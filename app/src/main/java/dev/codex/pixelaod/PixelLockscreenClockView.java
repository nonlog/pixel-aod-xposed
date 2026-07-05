package dev.codex.pixelaod;

import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

final class PixelLockscreenClockView extends FrameLayout {
    private static final Set<PixelLockscreenClockView> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final int CLOCK_COLOR = Color.rgb(PixelAodVisualStyle.CLOCK_COLOR_RED,
            PixelAodVisualStyle.CLOCK_COLOR_GREEN, PixelAodVisualStyle.CLOCK_COLOR_BLUE);
    private static final int INFO_COLOR = Color.rgb(PixelAodVisualStyle.INFO_COLOR_RED,
            PixelAodVisualStyle.INFO_COLOR_GREEN, PixelAodVisualStyle.INFO_COLOR_BLUE);
    private static final int LARGE_CLOCK_TEXT_DP = PixelAodVisualStyle.LARGE_CLOCK_TEXT_DP;
    private static final int LARGE_CLOCK_TOP_DP = PixelAodVisualStyle.LARGE_CLOCK_TOP_DP;
    private static final int SMALL_CLOCK_TEXT_DP = PixelAodVisualStyle.SMALL_CLOCK_TEXT_DP;
    private static final int SMALL_CLOCK_TOP_DP = PixelAodVisualStyle.SMALL_CLOCK_TOP_DP;
    private static final int EDGE_DP = PixelAodVisualStyle.EDGE_DP;
    private static final int COMPACT_CLOCK_VISUAL_START_OFFSET_DP =
            PixelAodVisualStyle.COMPACT_CLOCK_VISUAL_START_OFFSET_DP;
    private static final int LARGE_INFO_TOP_DP = PixelAodVisualStyle.LARGE_INFO_TOP_DP;
    private static final int SMALL_INFO_TOP_DP = PixelAodVisualStyle.SMALL_INFO_TOP_DP;
    private static final int SMALL_NOTIFICATION_TOP_DP =
            PixelAodVisualStyle.NOTIFICATION_LINE_TOP_DP;
    private static final float CLOCK_LINE_SPACING = PixelAodVisualStyle.CLOCK_LINE_SPACING;
    private static final float LARGE_CLOCK_LETTER_SPACING =
            PixelAodVisualStyle.LARGE_CLOCK_LETTER_SPACING;
    private static final float SMALL_CLOCK_LETTER_SPACING =
            PixelAodVisualStyle.COMPACT_CLOCK_LETTER_SPACING;
    private static final int CLOCK_LOCKSCREEN_WEIGHT =
            PixelAodVisualStyle.Lockscreen.CLOCK_WEIGHT;
    private static final int INFO_LOCKSCREEN_WEIGHT = PixelAodVisualStyle.Lockscreen.INFO_WEIGHT;
    private static final long AOD_TRANSITION_ANIMATION_WINDOW_MS = 1800L;
    private static final long RECENT_AOD_FALLBACK_WINDOW_MS = 120_000L;
    private static final long LOCKSCREEN_TO_AOD_ANIMATION_WINDOW_MS = 2500L;
    private static final long MIN_LOCKSCREEN_VISIBLE_FOR_AOD_ANIMATION_MS = 450L;
    private static final long NOTIFICATION_LAYOUT_CHECK_INTERVAL_MS = 80L;
    private static final long BOUNCER_CHECK_INTERVAL_MS = 900L;
    private static final boolean ANIMATE_AOD_POSITION_TO_LOCKSCREEN = true;
    private static final StatusBarNotification[] EMPTY_NOTIFICATIONS = new StatusBarNotification[0];

    private static StatusBarNotification[] activeNotifications = EMPTY_NOTIFICATIONS;
    private static Context appContext;
    private static boolean hasVisibleLockscreenNotificationCards;
    private static boolean lockscreenSurfaceVisible;
    private static long pendingAodToLockscreenTransitionAt;
    private static String pendingAodToLockscreenTransitionSource = "";
    private static float pendingAodToLockscreenTranslationX;
    private static float pendingAodToLockscreenTranslationY;
    private static long interactiveLockscreenVisibleSince;
    private static long recentInteractiveLockscreenVisibleAt;

    private final TextView clockView;
    private final TextView dateView;
    private final LinearLayout notificationIconRow;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePresentation("broadcast");
        }
    };
    private boolean started;
    private boolean compactClock;
    private ValueAnimator clockWeightAnimator;
    private int currentClockWeight = CLOCK_LOCKSCREEN_WEIGHT;
    private boolean clockWeightTransitionPending;
    private boolean expandedNotificationSuppressed;
    private boolean lastLiveLockscreenNotificationCards;
    private boolean lastExpandedLockscreenNotificationContent;
    private boolean lastBouncerVisible;
    private long lastNotificationLayoutCheckAt;
    private long lastBouncerCheckAt;
    private long lastClockTransitionStartedAt;

    private MediaSessionManager mediaSessionManager;
    private final List<MediaController> mediaControllers = new ArrayList<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener activeSessionsChangedListener =
            this::updateMediaControllers;
    private final MediaController.Callback mediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            refreshAll("media_state");
        }
    };

    private void updateMediaControllers(List<MediaController> controllers) {
        mainHandler().post(() -> {
            for (MediaController controller : mediaControllers) {
                try { controller.unregisterCallback(mediaControllerCallback); } catch (Throwable ignored) {}
            }
            mediaControllers.clear();
            if (controllers != null) {
                mediaControllers.addAll(controllers);
                for (MediaController controller : mediaControllers) {
                    try { controller.registerCallback(mediaControllerCallback); } catch (Throwable ignored) {}
                }
            }
            refreshAll("media_sessions");
            PixelAodHook.reapplyLockscreenClockFromKnownHost("media_sessions");
        });
    }

    private boolean hasPlayingMediaLocally() {
        for (MediaController controller : mediaControllers) {
            PlaybackState state = controller.getPlaybackState();
            if (state != null) {
                int st = state.getState();
                if (st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
                        || st == PlaybackState.STATE_FAST_FORWARDING || st == PlaybackState.STATE_REWINDING
                        || st == PlaybackState.STATE_SKIPPING_TO_NEXT || st == PlaybackState.STATE_SKIPPING_TO_PREVIOUS) {
                    return true;
                }
            }
        }
        return false;
    }

    PixelLockscreenClockView(Context context) {
        super(context);
        appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        int lockscreenWeight = PixelAodClockView.lockscreenClockWeight(context);
        currentClockWeight = lockscreenWeight;
        Typeface infoTypeface = PixelAodClockView.sharedInfoTypeface(context, INFO_LOCKSCREEN_WEIGHT);

        clockView = new TextView(context);
        clockView.setTextColor(resolveMaterialClockColor(context));
        PixelAodClockView.applySharedClockTextStyle(clockView, context, lockscreenWeight,
                PixelAodClockView.scaledClockTextDp(context, LARGE_CLOCK_TEXT_DP), false);
        PixelAodLog.log("applied Pixel lockscreen clock style source=init weight=" + lockscreenWeight
                + " variation=" + PixelAodClockView.sharedClockFontVariationSettings(lockscreenWeight)
                + " typeface=builder"
                + " visualProfile={"
                + PixelAodVisualStyle.lockscreenProfile(lockscreenWeight) + "}");
        clockView.setAlpha(0.98f);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
        addView(clockView, clockParams);

        dateView = PixelAodClockView.makeInfoLine(context, infoTypeface,
                INFO_LOCKSCREEN_WEIGHT, 16, Gravity.START);
        FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        dateParams.leftMargin = dp(EDGE_DP);
        dateParams.topMargin = dp(LARGE_INFO_TOP_DP);
        addView(dateView, dateParams);

        notificationIconRow = new LinearLayout(context);
        notificationIconRow.setOrientation(LinearLayout.HORIZONTAL);
        notificationIconRow.setGravity(Gravity.CENTER_VERTICAL);
        notificationIconRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        notificationIconRow.setAlpha(0.94f);
        FrameLayout.LayoutParams notificationParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        notificationParams.leftMargin = dp(EDGE_DP);
        notificationParams.topMargin = dp(SMALL_NOTIFICATION_TOP_DP);
        addView(notificationIconRow, notificationParams);

        updatePresentation("init");
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        INSTANCES.add(this);
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        resetTransitionState();
        stop();
        INSTANCES.remove(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!shouldShowOnLockscreen(getContext())) {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
                PixelAodLog.log("suppressed Pixel lockscreen clock outside keyguard trace="
                        + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            }
            return;
        }
        if (isBouncerVisible()) {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            return;
        }
        boolean expandedNotificationContent = hasExpandedLockscreenNotificationContent(false);
        if (expandedNotificationContent) {
            setExpandedNotificationSuppressed(true);
            return;
        } else if (expandedNotificationSuppressed) {
            setExpandedNotificationSuppressed(false);
            updatePresentation("expanded-notification-collapsed");
        }
        boolean liveCards = hasLiveLockscreenNotificationCards(false);
        if (liveCards) {
            if (!compactClock) {
                applyClockMode(true);
                updateTime();
            }
            notificationIconRow.removeAllViews();
            notificationIconRow.setVisibility(View.GONE);
        }
        markInteractiveLockscreenVisibleIfNeeded();
        maybeStartDrawTimeAodTransition();
        super.dispatchDraw(canvas);
    }

    void start() {
        if (!started) {
            started = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            try {
                getContext().registerReceiver(receiver, filter);
            } catch (Throwable t) {
                PixelAodLog.log("failed to register Pixel lockscreen receiver", t);
            }
            try {
                mediaSessionManager = (MediaSessionManager) getContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
                if (mediaSessionManager != null) {
                    mediaSessionManager.addOnActiveSessionsChangedListener(activeSessionsChangedListener, null);
                    updateMediaControllers(mediaSessionManager.getActiveSessions(null));
                }
            } catch (Throwable t) {
                PixelAodLog.log("failed to register media session listener", t);
            }
        }
        updatePresentation("start");
    }

    void stop() {
        if (!started) {
            return;
        }
        started = false;
        try {
            getContext().unregisterReceiver(receiver);
        } catch (Throwable t) {
            PixelAodLog.log("failed to unregister Pixel lockscreen receiver", t);
        }
        try {
            if (mediaSessionManager != null) {
                mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener);
            }
            updateMediaControllers(Collections.emptyList());
        } catch (Throwable t) {
            PixelAodLog.log("failed to unregister media session listener", t);
        }
    }

    static void setActiveNotifications(StatusBarNotification[] notifications) {
        synchronized (PixelLockscreenClockView.class) {
            activeNotifications = notifications != null ? notifications.clone() : EMPTY_NOTIFICATIONS;
        }
        refreshAll("notifications");
        PixelAodHook.reapplyLockscreenClockFromKnownHost("notifications");
    }

    static void setVisibleLockscreenNotificationCards(boolean hasCards, String source) {
        boolean changed;
        synchronized (PixelLockscreenClockView.class) {
            changed = hasVisibleLockscreenNotificationCards != hasCards;
            hasVisibleLockscreenNotificationCards = hasCards;
        }
        if (changed) {
            PixelAodLog.log("Pixel lockscreen visible notification cards="
                    + hasCards + " source=" + source);
            refreshAll(source);
        }
    }

    static void setLockscreenSurfaceVisible(boolean visible, String source) {
        boolean changed;
        synchronized (PixelLockscreenClockView.class) {
            changed = lockscreenSurfaceVisible != visible;
            lockscreenSurfaceVisible = visible;
            if (!visible) {
                interactiveLockscreenVisibleSince = 0L;
                recentInteractiveLockscreenVisibleAt = 0L;
            }
        }
        if (changed) {
            PixelAodLog.log("Pixel lockscreen surface visible=" + visible
                    + " source=" + source);
            refreshAll(source);
        }
    }

    static void prepareAodToLockscreenTransition(String source) {
        PixelAodClockView.BurnInOffset offset = PixelAodClockView.currentBurnInOffset();
        synchronized (PixelLockscreenClockView.class) {
            pendingAodToLockscreenTransitionAt = android.os.SystemClock.uptimeMillis();
            pendingAodToLockscreenTransitionSource = source;
            pendingAodToLockscreenTranslationX = offset.translationX;
            pendingAodToLockscreenTranslationY = offset.translationY;
        }
        PixelAodLog.log("prepared Pixel lockscreen clock weight transition trace="
                + PixelAodClockView.currentAodTraceId()
                + " source=" + source
                + " x=" + Math.round(offset.translationX)
                + " y=" + Math.round(offset.translationY)
                + " state={" + PixelAodClockView.describeAodState(appContext, false, -1) + "}");
    }

    static void refreshAll(String source) {
        mainHandler().post(() -> {
            for (PixelLockscreenClockView view : INSTANCES) {
                if (view != null) {
                    view.updatePresentation(source);
                }
            }
        });
    }

    static boolean shouldShowOnLockscreen(Context context) {
        return shouldShowOnLockscreen(context, "direct");
    }

    static boolean isSystemKeyguardLocked(Context context) {
        if (context == null || !PixelAodClockView.isDeviceInteractive(context)) {
            return false;
        }
        return isSystemKeyguardLockedRaw(context);
    }

    private static boolean shouldShowOnLockscreen(Context context, String source) {
        if (context == null) {
            PixelAodLog.log("lockscreen visibility decision source=" + source
                    + " visible=false reason=no-context trace=" + PixelAodClockView.currentAodTraceId());
            return false;
        }
        boolean interactive = PixelAodClockView.isDeviceInteractive(context);
        boolean locked = isSystemKeyguardLockedRaw(context);
        boolean bridgeCandidate = !interactive && locked
                && PixelAodClockView.isInAodEntryTransitionWindow(
                AOD_TRANSITION_ANIMATION_WINDOW_MS);
        boolean bridge = !interactive && shouldBridgeLockscreenToAod(context);
        boolean visible = interactive ? locked : bridge;
        String reason;
        if (interactive) {
            reason = locked ? "interactive-keyguard-locked" : "interactive-unlocked";
        } else if (bridge) {
            reason = "bridge-to-aod";
        } else if (bridgeCandidate) {
            reason = "bridge-blocked-by-aod-policy";
        } else if (!locked) {
            reason = "noninteractive-unlocked";
        } else {
            reason = "noninteractive-outside-aod-window";
        }
        if (!"direct".equals(source)) {
            PixelAodLog.log("lockscreen visibility decision source=" + source
                    + " visible=" + visible
                    + " reason=" + reason
                    + " interactive=" + interactive
                    + " locked=" + locked
                    + " bridge=" + bridge
                    + " bridgeCandidate=" + bridgeCandidate
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
        }
        return visible;
    }

    private static boolean shouldBridgeLockscreenToAod(Context context) {
        return isSystemKeyguardLockedRaw(context)
                && PixelAodClockView.shouldBridgeLockscreenDuringAodEntry(
                        context, AOD_TRANSITION_ANIMATION_WINDOW_MS);
    }

    private static boolean isSystemKeyguardLockedRaw(Context context) {
        if (context == null) {
            return false;
        }
        try {
            KeyguardManager keyguardManager =
                    (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return keyguardManager != null && keyguardManager.isKeyguardLocked();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean shouldShowOnKnownContext() {
        return shouldShowOnLockscreen(appContext);
    }

    static boolean shouldAnimateLockscreenToAodTransition() {
        synchronized (PixelLockscreenClockView.class) {
            long now = android.os.SystemClock.uptimeMillis();
            long age = now - recentInteractiveLockscreenVisibleAt;
            long visibleFor = recentInteractiveLockscreenVisibleAt - interactiveLockscreenVisibleSince;
            return lockscreenSurfaceVisible
                    && recentInteractiveLockscreenVisibleAt > 0L
                    && interactiveLockscreenVisibleSince > 0L
                    && age >= 0L
                    && age <= LOCKSCREEN_TO_AOD_ANIMATION_WINDOW_MS
                    && visibleFor >= MIN_LOCKSCREEN_VISIBLE_FOR_AOD_ANIMATION_MS;
        }
    }

    private void updatePresentation(String source) {
        boolean shouldShow = shouldShowOnLockscreen(getContext(), source);
        boolean bouncerVisible = isBouncerVisible();
        boolean visible = shouldShow && !bouncerVisible;
        boolean firstVisibleFrame = visible && getVisibility() != View.VISIBLE;
        TransitionInfo transition = visible ? consumeRecentAodToLockscreenTransition(source) : null;
        if (transition == null && visible) {
            transition = consumeRecentAodFallbackTransition(source);
        }
        boolean hasTransition = visible && transition != null;
        int aodWeight = PixelAodClockView.aodClockWeight(getContext());
        int lockscreenWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        boolean animateWeight = hasTransition && aodWeight != lockscreenWeight;
        PixelAodLog.log("lockscreen presentation decision trace=" + PixelAodClockView.currentAodTraceId()
                + " source=" + source
                + " shouldShow=" + shouldShow
                + " bouncerVisible=" + bouncerVisible
                + " visible=" + visible
                + " firstVisibleFrame=" + firstVisibleFrame
                + " transition=" + (transition != null ? transition.source : "none")
                + " hasTransition=" + hasTransition
                + " animateWeight=" + animateWeight
                + " aodWeight=" + aodWeight
                + " lockscreenWeight=" + lockscreenWeight
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
        if (!clockWeightTransitionPending) {
            setClockWeight(animateWeight ? aodWeight : lockscreenWeight);
        }
        setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            clearInteractiveLockscreenVisibleIfNeeded();
            resetTransitionState();
            setExpandedNotificationSuppressed(false);
            PixelAodLog.log("lockscreen presentation hidden trace=" + PixelAodClockView.currentAodTraceId()
                    + " source=" + source
                    + " reason=" + (bouncerVisible ? "bouncer-visible" : "should-not-show")
                    + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            return;
        }
        markInteractiveLockscreenVisibleIfNeeded();
        boolean playingMedia = hasPlayingMediaLocally();
        if (hasExpandedLockscreenNotificationContent(firstVisibleFrame)) {
            setExpandedNotificationSuppressed(true);
            PixelAodLog.log("lockscreen presentation suppressed trace=" + PixelAodClockView.currentAodTraceId()
                    + " source=" + source
                    + " reason=expanded-notification-content"
                    + " firstVisibleFrame=" + firstVisibleFrame
                    + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            return;
        }
        setExpandedNotificationSuppressed(false);
        List<StatusBarNotification> notifications = currentNotifications();
        boolean hasCards = hasVisibleLockscreenNotificationCards()
                || hasLiveLockscreenNotificationCards(false);
        boolean compact = !notifications.isEmpty() || hasCards || playingMedia;
        applyClockMode(compact);
        applyMaterialColors();
        updateTime();
        rebuildNotificationIcons(Collections.emptyList());
        if (hasTransition) {
            beginClockTransition(source, transition);
        } else if (!clockWeightTransitionPending) {
            setTranslationX(0f);
            setTranslationY(0f);
        }
        PixelAodLog.log("lockscreen presentation visible trace=" + PixelAodClockView.currentAodTraceId()
                + " source=" + source
                + " compact=" + compactClock
                + " notifications=" + notifications.size()
                + " hasCards=" + hasCards
                + " playingMedia=" + playingMedia
                + " firstVisibleFrame=" + firstVisibleFrame
                + " transition=" + (transition != null ? transition.source : "none")
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
    }

    private boolean isBouncerVisible() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastBouncerCheckAt < BOUNCER_CHECK_INTERVAL_MS) {
            return lastBouncerVisible;
        }
        lastBouncerCheckAt = now;
        View root = getRootView();
        lastBouncerVisible = root instanceof ViewGroup
                && PixelAodClockView.isDeviceInteractive(getContext())
                && PixelAodHook.hasVisibleKeyguardBouncer((ViewGroup) root);
        return lastBouncerVisible;
    }

    private boolean hasLiveLockscreenNotificationCards(boolean force) {
        refreshNotificationLayoutState(force);
        return lastLiveLockscreenNotificationCards;
    }

    private void markInteractiveLockscreenVisibleIfNeeded() {
        Context context = getContext();
        if (PixelAodClockView.isDeviceInteractive(context) && isSystemKeyguardLockedRaw(context)) {
            long now = android.os.SystemClock.uptimeMillis();
            synchronized (PixelLockscreenClockView.class) {
                if (interactiveLockscreenVisibleSince <= 0L
                        || now - recentInteractiveLockscreenVisibleAt > 700L) {
                    interactiveLockscreenVisibleSince = now;
                }
                recentInteractiveLockscreenVisibleAt = now;
            }
        }
    }

    private void clearInteractiveLockscreenVisibleIfNeeded() {
        Context context = getContext();
        if (PixelAodClockView.isDeviceInteractive(context) && !isSystemKeyguardLockedRaw(context)) {
            synchronized (PixelLockscreenClockView.class) {
                interactiveLockscreenVisibleSince = 0L;
                recentInteractiveLockscreenVisibleAt = 0L;
            }
        }
    }

    private boolean hasExpandedLockscreenNotificationContent(boolean force) {
        refreshNotificationLayoutState(force);
        return lastExpandedLockscreenNotificationContent;
    }

    private void refreshNotificationLayoutState(boolean force) {
        long now = android.os.SystemClock.uptimeMillis();
        if (!force && now - lastNotificationLayoutCheckAt < NOTIFICATION_LAYOUT_CHECK_INTERVAL_MS) {
            return;
        }
        lastNotificationLayoutCheckAt = now;
        View root = getRootView();
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            lastExpandedLockscreenNotificationContent =
                    PixelAodHook.hasExpandedLockscreenNotificationContentIn(group);
            lastLiveLockscreenNotificationCards =
                    lastExpandedLockscreenNotificationContent
                            || PixelAodHook.hasVisibleLockscreenNotificationCardsIn(group);
        } else {
            lastExpandedLockscreenNotificationContent = false;
            lastLiveLockscreenNotificationCards = false;
        }
    }

    private void setExpandedNotificationSuppressed(boolean suppressed) {
        boolean changed = expandedNotificationSuppressed != suppressed;
        expandedNotificationSuppressed = suppressed;
        if (suppressed && getVisibility() != View.GONE) {
            setVisibility(View.GONE);
        }
        clockView.setVisibility(suppressed ? View.GONE : View.VISIBLE);
        dateView.setVisibility(suppressed ? View.GONE : View.VISIBLE);
        notificationIconRow.setVisibility(View.GONE);
        if (changed) {
            PixelAodLog.log("Pixel lockscreen clock expanded-notification suppression=" + suppressed
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            if (suppressed) {
                scheduleSuppressionRefresh(300L);
                scheduleSuppressionRefresh(900L);
            }
        }
    }

    private void scheduleSuppressionRefresh(long delayMillis) {
        mainHandler().postDelayed(() -> {
            if (started) {
                updatePresentation("expanded-notification-suppression+" + delayMillis);
            }
        }, delayMillis);
    }

    private void maybeStartDrawTimeAodTransition() {
        if (clockWeightTransitionPending || getVisibility() != View.VISIBLE) {
            return;
        }
        PixelAodClockView.BurnInOffset offset =
                PixelAodClockView.consumeRecentBurnInOffset(RECENT_AOD_FALLBACK_WINDOW_MS);
        if (offset == null) {
            return;
        }
        PixelAodLog.log("consumed recent Pixel AOD fallback transition trace="
                + PixelAodClockView.currentAodTraceId()
                + " update=dispatchDraw"
                + " x=" + Math.round(offset.translationX)
                + " y=" + Math.round(offset.translationY)
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
        beginClockTransition("dispatchDraw", new TransitionInfo(
                "recent-aod-draw", offset.translationX, offset.translationY));
    }

    private static TransitionInfo consumeRecentAodToLockscreenTransition(String updateSource) {
        synchronized (PixelLockscreenClockView.class) {
            long markedAt = pendingAodToLockscreenTransitionAt;
            if (markedAt <= 0L) {
                PixelAodLog.log("lockscreen transition consume miss trace="
                        + PixelAodClockView.currentAodTraceId()
                        + " update=" + updateSource
                        + " reason=no-pending-transition");
                return null;
            }
            long age = android.os.SystemClock.uptimeMillis() - markedAt;
            if (age < 0L || age > AOD_TRANSITION_ANIMATION_WINDOW_MS) {
                PixelAodLog.log("lockscreen transition consume miss trace="
                        + PixelAodClockView.currentAodTraceId()
                        + " update=" + updateSource
                        + " reason=expired ageMs=" + age
                        + " source=" + pendingAodToLockscreenTransitionSource
                        + " translationX=" + Math.round(pendingAodToLockscreenTranslationX)
                        + " translationY=" + Math.round(pendingAodToLockscreenTranslationY));
                pendingAodToLockscreenTransitionAt = 0L;
                pendingAodToLockscreenTransitionSource = "";
                pendingAodToLockscreenTranslationX = 0f;
                pendingAodToLockscreenTranslationY = 0f;
                return null;
            }
            String source = pendingAodToLockscreenTransitionSource;
            float translationX = pendingAodToLockscreenTranslationX;
            float translationY = pendingAodToLockscreenTranslationY;
            pendingAodToLockscreenTransitionAt = 0L;
            pendingAodToLockscreenTransitionSource = "";
            pendingAodToLockscreenTranslationX = 0f;
            pendingAodToLockscreenTranslationY = 0f;
            PixelAodLog.log("consumed Pixel lockscreen clock weight transition trace="
                    + PixelAodClockView.currentAodTraceId()
                    + " from=" + source
                    + " update=" + updateSource
                    + " ageMs=" + age
                    + " x=" + Math.round(translationX)
                    + " y=" + Math.round(translationY)
                    + " state={" + PixelAodClockView.describeAodState(appContext, false, -1) + "}");
            return new TransitionInfo(source, translationX, translationY);
        }
    }

    private static TransitionInfo consumeRecentAodFallbackTransition(String updateSource) {
        PixelAodClockView.BurnInOffset offset =
                PixelAodClockView.consumeRecentBurnInOffset(RECENT_AOD_FALLBACK_WINDOW_MS);
        if (offset == null) {
            PixelAodLog.log("lockscreen fallback transition miss trace="
                    + PixelAodClockView.currentAodTraceId()
                    + " update=" + updateSource
                    + " reason=no-recent-burn-in-offset"
                    + " state={" + PixelAodClockView.describeAodState(appContext, false, -1) + "}");
            return null;
        }
        PixelAodLog.log("consumed recent Pixel AOD fallback transition trace="
                + PixelAodClockView.currentAodTraceId()
                + " update=" + updateSource
                + " x=" + Math.round(offset.translationX)
                + " y=" + Math.round(offset.translationY)
                + " state={" + PixelAodClockView.describeAodState(appContext, false, -1) + "}");
        return new TransitionInfo("recent-aod", offset.translationX, offset.translationY);
    }

    private void updateTime() {
        PixelAodRenderModel model = PixelAodRenderModel.forLockscreen(getContext(), compactClock,
                PixelAodClockView.currentFreshWeather(getContext()));
        clockView.setText(model.clockText);
        dateView.setText(model.dateText);
        PixelAodClockView.applyWeatherIcon(dateView,
                model.weather, resolveMaterialInfoColor(getContext()));
    }

    private void applyClockMode(boolean compact) {
        boolean changed = compactClock != compact;
        compactClock = compact;
        FrameLayout.LayoutParams clockParams = (FrameLayout.LayoutParams) clockView.getLayoutParams();
        FrameLayout.LayoutParams dateParams = (FrameLayout.LayoutParams) dateView.getLayoutParams();
        FrameLayout.LayoutParams notificationParams =
                (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        if (compact) {
            PixelAodClockView.applySharedClockTextStyle(clockView, getContext(), currentClockWeight,
                    PixelAodClockView.scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP), true);
            clockParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            clockParams.gravity = Gravity.TOP | Gravity.START;
            clockParams.leftMargin = dp(EDGE_DP - COMPACT_CLOCK_VISUAL_START_OFFSET_DP);
            clockParams.topMargin = dp(SMALL_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            dateView.setVisibility(View.VISIBLE);
            dateParams.topMargin = dp(SMALL_INFO_TOP_DP);
            notificationParams.topMargin = dp(SMALL_NOTIFICATION_TOP_DP);
        } else {
            PixelAodClockView.applySharedClockTextStyle(clockView, getContext(), currentClockWeight,
                    PixelAodClockView.scaledClockTextDp(getContext(), LARGE_CLOCK_TEXT_DP), false);
            clockParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            clockParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            clockParams.leftMargin = 0;
            clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            dateView.setVisibility(View.VISIBLE);
            dateParams.topMargin = dp(LARGE_INFO_TOP_DP);
            notificationParams.topMargin = dp(SMALL_NOTIFICATION_TOP_DP);
        }
        clockView.setLayoutParams(clockParams);
        dateView.setLayoutParams(dateParams);
        notificationIconRow.setLayoutParams(notificationParams);
        PixelAodLog.log("applied Pixel lockscreen clock mode trace="
                + PixelAodClockView.currentAodTraceId()
                + " compact=" + compact
                + " changed=" + changed
                + " weight=" + currentClockWeight
                + " clockTop=" + clockParams.topMargin
                + " dateTop=" + dateParams.topMargin
                + " notificationTop=" + notificationParams.topMargin
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
        if (changed) {
            PixelAodLog.log("applied Pixel lockscreen clock style source=mode-change compact="
                    + compact + " weight=" + currentClockWeight
                    + " variation=" + PixelAodClockView.sharedClockFontVariationSettings(currentClockWeight)
                    + " typeface=builder"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
    }

    private void rebuildNotificationIcons(List<StatusBarNotification> notifications) {
        notificationIconRow.removeAllViews();
        notificationIconRow.setVisibility(View.GONE);
        PixelAodLog.log("rebuilt Pixel lockscreen notification icons trace="
                + PixelAodClockView.currentAodTraceId()
                + " count=" + (notifications != null ? notifications.size() : 0)
                + " compact=" + compactClock
                + " weight=" + currentClockWeight
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
    }

    private void applyMaterialColors() {
        int clockColor = PixelAodClockView.resolveMaterialClockColor(getContext());
        int infoColor = PixelAodClockView.resolveMaterialInfoColor(getContext());
        clockView.setTextColor(clockColor);
        dateView.setTextColor(infoColor);
        PixelAodClockView.applyWeatherIcon(dateView,
                PixelAodClockView.currentFreshWeather(getContext()), infoColor);
    }

    private void beginClockTransition(String source, TransitionInfo transition) {
        if (clockWeightTransitionPending && clockWeightAnimator != null
                && clockWeightAnimator.isRunning()) {
            PixelAodLog.log("ignored duplicate Pixel lockscreen transition trace="
                    + PixelAodClockView.currentAodTraceId()
                    + " source=" + source
                    + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            return;
        }
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
            clockWeightAnimator = null;
        }
        clockWeightTransitionPending = true;
        lastClockTransitionStartedAt = android.os.SystemClock.uptimeMillis();
        int fromWeight = PixelAodClockView.aodClockWeight(getContext());
        int toWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        float sanitizedTranslationX = ANIMATE_AOD_POSITION_TO_LOCKSCREEN && transition != null
                ? transition.translationX : 0f;
        float sanitizedTranslationY = ANIMATE_AOD_POSITION_TO_LOCKSCREEN && transition != null
                ? transition.translationY : 0f;
        if (Float.isNaN(sanitizedTranslationX) || Float.isInfinite(sanitizedTranslationX)) {
            sanitizedTranslationX = 0f;
        }
        if (Float.isNaN(sanitizedTranslationY) || Float.isInfinite(sanitizedTranslationY)) {
            sanitizedTranslationY = 0f;
        }
        final float fromTranslationX = sanitizedTranslationX;
        final float fromTranslationY = sanitizedTranslationY;
        final boolean animateWeight = fromWeight != toWeight;
        final boolean animatePosition =
                Math.abs(fromTranslationX) >= 0.5f || Math.abs(fromTranslationY) >= 0.5f;
        if (!animateWeight && !animatePosition) {
            setClockWeight(toWeight);
            setTranslationX(0f);
            setTranslationY(0f);
            clockWeightTransitionPending = false;
            clockWeightAnimator = null;
            PixelAodLog.log("skipped Pixel lockscreen transition trace="
                    + PixelAodClockView.currentAodTraceId()
                    + " source=" + source
                    + " reason=no-weight-or-position-change"
                    + " weight=" + toWeight
                    + " x=0 y=0"
                    + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            return;
        }
        setClockWeight(fromWeight);
        setTranslationX(fromTranslationX);
        setTranslationY(fromTranslationY);
        clockWeightAnimator = ValueAnimator.ofFloat(0f, 1f);
        clockWeightAnimator.setDuration(700L);
        clockWeightAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f));
        final boolean[] cancelled = {false};
        clockWeightAnimator.addUpdateListener(animation -> {
            Object animated = animation.getAnimatedValue();
            if (animated instanceof Float) {
                float progress = (Float) animated;
                if (animateWeight) {
                    int weight = Math.round(fromWeight + ((toWeight - fromWeight) * progress));
                    setClockWeight(weight);
                }
                if (animatePosition) {
                    setTranslationX(fromTranslationX * (1f - progress));
                    setTranslationY(fromTranslationY * (1f - progress));
                }
            }
        });
        clockWeightAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cancelled[0] = true;
                clockWeightTransitionPending = false;
                lastClockTransitionStartedAt = 0L;
                clockWeightAnimator = null;
                PixelAodLog.log("cancelled Pixel lockscreen transition trace="
                        + PixelAodClockView.currentAodTraceId()
                        + " source=" + source
                        + " fromWeight=" + fromWeight
                        + " toWeight=" + toWeight
                        + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (cancelled[0]) {
                    return;
                }
                setClockWeight(toWeight);
                setTranslationX(0f);
                setTranslationY(0f);
                clockWeightTransitionPending = false;
                lastClockTransitionStartedAt = 0L;
                clockWeightAnimator = null;
                PixelAodLog.log("finished Pixel lockscreen transition trace="
                        + PixelAodClockView.currentAodTraceId()
                        + " source=" + source
                        + " toWeight=" + toWeight
                        + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            }
        });
        clockWeightAnimator.start();
        PixelAodLog.log("started Pixel lockscreen transition trace="
                + PixelAodClockView.currentAodTraceId()
                + " source=" + source
                + " fromWeight=" + fromWeight
                + " fromVariation=" + PixelAodClockView.sharedClockFontVariationSettings(fromWeight)
                + " toWeight=" + toWeight
                + " toVariation=" + PixelAodClockView.sharedClockFontVariationSettings(toWeight)
                + " typeface=builder"
                + " x=" + Math.round(fromTranslationX)
                + " y=" + Math.round(fromTranslationY)
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
    }

    private void resetTransitionState() {
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
            clockWeightAnimator = null;
        }
        clockWeightTransitionPending = false;
        lastClockTransitionStartedAt = 0L;
        setTranslationX(0f);
        setTranslationY(0f);
        PixelAodLog.log("reset Pixel lockscreen transition state trace="
                + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
    }

    private void setClockWeight(int weight) {
        if (currentClockWeight == weight) {
            return;
        }
        currentClockWeight = weight;
        PixelAodClockView.applySharedClockTypeface(clockView, getContext(), weight);
        PixelAodClockView.applySharedClockLetterSpacing(clockView, getContext(), weight, compactClock);
    }

    private static int resolveMaterialClockColor(Context context) {
        return PixelAodClockView.resolveMaterialClockColor(context);
    }

    private static int resolveMaterialInfoColor(Context context) {
        return PixelAodClockView.resolveMaterialInfoColor(context);
    }

    private static List<StatusBarNotification> currentNotifications() {
        StatusBarNotification[] snapshot;
        synchronized (PixelLockscreenClockView.class) {
            snapshot = activeNotifications;
        }
        ArrayList<StatusBarNotification> list = new ArrayList<>();
        if (snapshot == null) {
            return list;
        }
        for (StatusBarNotification sbn : snapshot) {
            if (sbn != null && sbn.getNotification() != null) {
                list.add(sbn);
            }
        }
        return list;
    }

    private static boolean hasVisibleLockscreenNotificationCards() {
        synchronized (PixelLockscreenClockView.class) {
            return hasVisibleLockscreenNotificationCards;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static Handler mainHandler() {
        return MainHandlerHolder.MAIN;
    }

    private static final class MainHandlerHolder {
        static final Handler MAIN = new Handler(Looper.getMainLooper());
    }

    private static final class TransitionInfo {
        final String source;
        final float translationX;
        final float translationY;

        TransitionInfo(String source, float translationX, float translationY) {
            this.source = source;
            this.translationX = translationX;
            this.translationY = translationY;
        }
    }
}
