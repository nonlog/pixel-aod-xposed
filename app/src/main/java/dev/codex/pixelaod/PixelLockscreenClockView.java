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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.Layout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
    private static final int LARGE_INFO_TEXT_DP = PixelAodVisualStyle.LARGE_INFO_TEXT_DP;
    private static final int LARGE_INFO_ROW_GAP_DP = PixelAodVisualStyle.LARGE_INFO_ROW_GAP_DP;
    private static final int SMALL_INFO_TOP_DP = PixelAodVisualStyle.SMALL_INFO_TOP_DP;
    private static final int COMPACT_INFO_TEXT_DP = PixelAodVisualStyle.COMPACT_INFO_TEXT_DP;
    private static final int COMPACT_AUXILIARY_INFO_TEXT_DP =
            PixelAodVisualStyle.COMPACT_AUXILIARY_INFO_TEXT_DP;
    private static final int COMPACT_DATE_TO_EVENT_TOP_OFFSET_DP =
            PixelAodVisualStyle.COMPACT_DATE_TO_EVENT_TOP_OFFSET_DP;
    private static final int SMALL_NOTIFICATION_TOP_DP =
            PixelAodVisualStyle.NOTIFICATION_LINE_TOP_DP;
    private static final int NOTIFICATION_ROW_LEADING_OFFSET_DP =
            PixelAodVisualStyle.NOTIFICATION_ROW_LEADING_OFFSET_DP;
    private static final int NOTIFICATION_ICON_SIZE_DP =
            PixelAodVisualStyle.Aod.NOTIFICATION_ICON_SIZE_DP;
    private static final int NOTIFICATION_ICON_SPACING_DP =
            PixelAodVisualStyle.Aod.NOTIFICATION_ICON_SPACING_DP;
    private static final int MAX_NOTIFICATION_ICONS = 5;
    private static final float CLOCK_LINE_SPACING = PixelAodVisualStyle.CLOCK_LINE_SPACING;
    private static final float LARGE_CLOCK_LETTER_SPACING =
            PixelAodVisualStyle.LARGE_CLOCK_LETTER_SPACING;
    private static final float SMALL_CLOCK_LETTER_SPACING =
            PixelAodVisualStyle.COMPACT_CLOCK_LETTER_SPACING;
    private static final float LOCKSCREEN_CLOCK_ALPHA =
            PixelAodVisualStyle.LOCKSCREEN_CLOCK_ALPHA;
    private static final float INFO_ALPHA = PixelAodVisualStyle.INFO_ALPHA;
    private static final int CLOCK_LOCKSCREEN_WEIGHT =
            PixelAodVisualStyle.Lockscreen.CLOCK_WEIGHT;
    private static final int INFO_LOCKSCREEN_WEIGHT = PixelAodVisualStyle.Lockscreen.INFO_WEIGHT;
    private static final long AOD_TRANSITION_ANIMATION_WINDOW_MS = 1800L;
    private static final long RECENT_AOD_FALLBACK_WINDOW_MS = 120_000L;
    private static final long LOCKSCREEN_TO_AOD_ANIMATION_WINDOW_MS = 2500L;
    private static final long MIN_LOCKSCREEN_VISIBLE_FOR_AOD_ANIMATION_MS = 450L;
    private static final long NOTIFICATION_LAYOUT_CHECK_INTERVAL_MS = 80L;
    private static final long BOUNCER_CHECK_INTERVAL_MS = 900L;
    /** COUI applyTargets default duration + PathInterpolator(0.2, 0, 0, 1). */
    private static final long SIZE_MORPH_MILLIS = 550L;
    private static final PathInterpolator SIZE_MORPH_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final StatusBarNotification[] EMPTY_NOTIFICATIONS = new StatusBarNotification[0];

    private static StatusBarNotification[] activeNotifications = EMPTY_NOTIFICATIONS;
    private static Context appContext;
    private static boolean hasVisibleLockscreenNotificationCards;
    private static boolean lockscreenSurfaceVisible;
    private static long pendingAodToLockscreenTransitionAt;
    private static String pendingAodToLockscreenTransitionSource = "";
    private static long interactiveLockscreenVisibleSince;
    private static long recentInteractiveLockscreenVisibleAt;

    private final TextView clockView;
    private final TextView dateView;
    private final TextView weatherView;
    private final LinearLayout contextualRow;
    private final ImageView contextualIconView;
    private final TextView contextualView;
    private final LinearLayout notificationIconRow;
    private TextView notificationOverflowView;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (clockPluginManaged) {
                refreshClockPluginLockscreenContent("broadcast");
            } else {
                updatePresentation("broadcast");
            }
        }
    };
    private boolean started;
    private boolean clockPluginManaged;
    private boolean clockPluginGlyphTransitionActive;
    private boolean compactClock;
    private ValueAnimator clockWeightAnimator;
    private String activeClockWeightTransitionSource = "";
    private int currentClockWeight = CLOCK_LOCKSCREEN_WEIGHT;
    private int currentInfoWeight = -1;
    private boolean clockWeightTransitionPending;
    private boolean showingClockPluginAodNotificationIcons;
    private String lastClockPluginNotificationIconSignature = "";
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
            if (controller == null) {
                continue;
            }
            PlaybackState state = controller.getPlaybackState();
            if (state != null) {
                int st = state.getState();
                // Only actively playing media. PAUSED alone must not stick compact after the
                // user dismisses the OOS media card (logs: clockSizeState=1 LARGE while we
                // still forced SMALL via paused MediaSession).
                if (st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
                        || st == PlaybackState.STATE_FAST_FORWARDING || st == PlaybackState.STATE_REWINDING
                        || st == PlaybackState.STATE_SKIPPING_TO_NEXT
                        || st == PlaybackState.STATE_SKIPPING_TO_PREVIOUS) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasPlayingMediaOnAnyInstance() {
        for (PixelLockscreenClockView view : INSTANCES) {
            if (view != null && view.hasPlayingMediaLocally()) {
                return true;
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
        Typeface infoTypeface = PixelAodClockView.sharedInfoTypeface(context, lockscreenWeight);

        clockView = new TextView(context);
        clockView.setTextColor(resolveMaterialClockColor(context));
        PixelAodClockView.applySharedClockTextStyle(clockView, context, lockscreenWeight,
                PixelAodClockView.scaledClockTextDp(context, LARGE_CLOCK_TEXT_DP), false);
        PixelAodLog.log("applied Pixel lockscreen clock style source=init weight=" + lockscreenWeight
                + " variation=" + PixelAodClockView.sharedClockFontVariationSettings(lockscreenWeight)
                + " typeface=builder"
                + " visualProfile={"
                + PixelAodVisualStyle.lockscreenProfile(context, lockscreenWeight) + "}");
        clockView.setAlpha(LOCKSCREEN_CLOCK_ALPHA);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
        addView(clockView, clockParams);

        dateView = PixelAodClockView.makeInfoLine(context, infoTypeface,
                lockscreenWeight, LARGE_INFO_TEXT_DP, Gravity.START);
        FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        dateParams.leftMargin = dp(EDGE_DP);
        dateParams.topMargin = dp(LARGE_INFO_TOP_DP);
        addView(dateView, dateParams);

        weatherView = PixelAodClockView.makeInfoLine(context, infoTypeface,
                lockscreenWeight, LARGE_INFO_TEXT_DP, Gravity.START);
        weatherView.setEllipsize(TextUtils.TruncateAt.END);
        weatherView.setVisibility(View.GONE);
        FrameLayout.LayoutParams weatherParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        weatherParams.leftMargin = dp(EDGE_DP);
        weatherParams.topMargin = dp(LARGE_INFO_TOP_DP + LARGE_INFO_TEXT_DP + LARGE_INFO_ROW_GAP_DP);
        addView(weatherView, weatherParams);

        contextualRow = new LinearLayout(context);
        contextualRow.setOrientation(LinearLayout.HORIZONTAL);
        contextualRow.setGravity(Gravity.CENTER_VERTICAL);
        contextualRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        contextualRow.setAlpha(INFO_ALPHA);
        contextualRow.setVisibility(View.GONE);
        contextualRow.setMinimumHeight(dp(LARGE_INFO_TEXT_DP));
        FrameLayout.LayoutParams contextualParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        contextualParams.leftMargin = dp(EDGE_DP);
        contextualParams.rightMargin = dp(EDGE_DP);
        contextualParams.topMargin = dp(LARGE_INFO_TOP_DP + LARGE_INFO_ROW_GAP_DP);
        addView(contextualRow, contextualParams);

        contextualIconView = new ImageView(context);
        contextualIconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        contextualIconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        contextualRow.addView(contextualIconView, new LinearLayout.LayoutParams(
                dp(LARGE_INFO_TEXT_DP), dp(LARGE_INFO_TEXT_DP)));

        contextualView = PixelAodClockView.makeInfoLine(context, infoTypeface,
                lockscreenWeight, LARGE_INFO_TEXT_DP, Gravity.START);
        contextualView.setEllipsize(TextUtils.TruncateAt.END);
        contextualView.setMaxLines(1);
        LinearLayout.LayoutParams contextualTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        contextualTextParams.leftMargin = dp(PixelAodVisualStyle.CALENDAR_ICON_SPACING_DP);
        contextualRow.addView(contextualView, contextualTextParams);

        notificationIconRow = new LinearLayout(context);
        notificationIconRow.setOrientation(LinearLayout.HORIZONTAL);
        notificationIconRow.setGravity(Gravity.CENTER_VERTICAL);
        notificationIconRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        notificationIconRow.setAlpha(INFO_ALPHA);
        FrameLayout.LayoutParams notificationParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        notificationParams.leftMargin = dp(EDGE_DP);
        notificationParams.topMargin = dp(SMALL_NOTIFICATION_TOP_DP);
        addView(notificationIconRow, notificationParams);

        setInfoWeight(lockscreenWeight);
        updatePresentation("init");
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        INSTANCES.add(this);
        start();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateInfoGroupLayout();
        if (showingClockPluginAodNotificationIcons) {
            applyNotificationRowPosition();
        }
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
        if (clockPluginManaged) {
            super.dispatchDraw(canvas);
            return;
        }
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
        if (clockPluginManaged) {
            refreshClockPluginLockscreenContent("start");
        } else {
            updatePresentation("start");
        }
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

    void setClockPluginManaged(boolean managed, String source) {
        if (clockPluginManaged == managed) {
            return;
        }
        clockPluginManaged = managed;
        if (managed) {
            resetTransitionState();
            setVisibility(View.INVISIBLE);
            if (isAttachedToWindow()) {
                start();
                refreshClockPluginLockscreenContent(source + "#managed");
            }
        } else if (isAttachedToWindow()) {
            updatePresentation(source + "#legacy");
        }
        PixelAodLog.log("configured ClockPlugin lockscreen layer managed=" + managed
                + " source=" + source + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    void presentClockPluginLockscreen(boolean compact, String source) {
        presentClockPluginLockscreen(compact, source,
                PixelAodClockView.lockscreenClockWeight(getContext()), false);
    }

    /**
     * @param fromWeight when {@code animateWeightFromAod} is true, animate wght from this
     *                   value to the configured lockscreen weight (AOD→lockscreen handoff).
     */
    void presentClockPluginLockscreen(boolean compact, String source, int fromWeight,
            boolean animateWeightFromAod) {
        if (!clockPluginManaged) {
            return;
        }
        if (!started) {
            start();
        }
        if (isBouncerVisible()) {
            setVisibility(View.INVISIBLE);
            return;
        }
        boolean wasVisible = getVisibility() == View.VISIBLE && getAlpha() > 0.01f;
        boolean sizeChanged = compact != compactClock;
        // Glyph content bounds + textSize (not MATCH_PARENT view box). Video v2: full-view
        // width made scale/pivot wrong so digits flew off-screen mid-morph.
        ViewMorphSnapshot fromClock = null;
        ViewMorphSnapshot fromDate = null;
        ViewMorphSnapshot fromWeather = null;
        if (sizeChanged && wasVisible && !clockPluginGlyphTransitionActive) {
            fromClock = snapshotTextContentMorph(clockView);
            if (!fromClock.valid) {
                fromClock = estimateClockMorphSnapshot(compactClock);
            }
            fromDate = snapshotTextContentMorph(dateView);
            fromWeather = snapshotTextContentMorph(weatherView);
        }
        resetTransitionState();
        clearViewMorphTransforms(clockView);
        clearViewMorphTransforms(dateView);
        clearViewMorphTransforms(weatherView);
        setScaleX(1f);
        setScaleY(1f);
        setVisibility(View.VISIBLE);
        if (!wasVisible && !hasPendingAodToLockscreenTransition()) {
            PixelAodClockView.beginContextualSurfaceEntry(source + "#ClockPlugin-visible");
        }
        markInteractiveLockscreenSurface(getContext(), source + "#ClockPlugin");
        setExpandedNotificationSuppressed(false);
        int lockscreenWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        applyClockMode(compact);
        applyMaterialColors();
        updateTime();
        clearClockPluginAodNotificationIcons(source + "#lockscreen");
        setTranslationX(0f);
        setTranslationY(0f);
        // OnPreDraw: apply start transform before the first target frame is drawn (no post flash).
        if (sizeChanged && wasVisible && fromClock != null && fromClock.valid) {
            startTextContentMorph(clockView, fromClock, source + "#size-morph-clock");
            if (fromDate != null && fromDate.valid) {
                startTextContentMorph(dateView, fromDate, source + "#size-morph-date");
            }
            if (fromWeather != null && fromWeather.valid) {
                startTextContentMorph(weatherView, fromWeather,
                        source + "#size-morph-weather");
            }
        }
        int aodWeight = PixelAodClockView.aodClockWeight(getContext());
        int weightStart = fromWeight > 0 ? fromWeight : currentClockWeight;
        // Also reverse-morph when the LS layer is already at AOD weight (early-aod-weight path
        // never flips host scene to AOD, so animateWeightFromAod may be false).
        boolean weightLooksLikeAod = Math.abs(currentClockWeight - aodWeight) <= 16
                && Math.abs(currentClockWeight - lockscreenWeight) > 8;
        if ((animateWeightFromAod || weightLooksLikeAod)
                && weightStart > 0
                && Math.abs(weightStart - lockscreenWeight) > 8) {
            beginLockscreenWeightRestoreTransition(weightStart, lockscreenWeight, source);
        } else {
            setClockWeight(lockscreenWeight);
            setInfoWeight(INFO_LOCKSCREEN_WEIGHT);
        }
        PixelAodLog.log("presented ClockPlugin lockscreen layer trace="
                + PixelAodClockView.currentAodTraceId()
                + " source=" + source
                + " compact=" + compactClock
                + " weight=" + currentClockWeight
                + " sizeChanged=" + sizeChanged
                + " sizeMorph=" + (sizeChanged && wasVisible)
                + " animateFromAod=" + animateWeightFromAod
                + " fromWeight=" + fromWeight);
    }

    void refreshClockPluginLockscreenContent(String source) {
        if (!clockPluginManaged || getVisibility() != View.VISIBLE) {
            return;
        }
        applyMaterialColors();
        updateTime();
        if (showingClockPluginAodNotificationIcons) {
            applyNotificationRowPosition();
            rebuildNotificationIcons(currentNotifications(), source + "#aod-handoff");
        } else {
            rebuildNotificationIcons(Collections.emptyList(), source + "#lockscreen");
        }
        PixelAodLog.log("refreshed ClockPlugin lockscreen layer trace="
                + PixelAodClockView.currentAodTraceId() + " source=" + source);
    }

    /**
     * Start LS→AOD weight morph on the visible lockscreen surface (icons + weight).
     * AOD layer continues from the live intermediate weight — never re-park at full LS weight.
     */
    void beginClockPluginAodWeightTransition(String source) {
        if (!clockPluginManaged) {
            return;
        }
        if (getVisibility() != View.VISIBLE) {
            PixelAodLog.log("skipped persistent ClockPlugin lockscreen weight handoff source="
                    + source + " reason=layer-not-visible"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        // Defense in depth: non-lockscreen doze must never start 340→AOD on the LS layer
        // (logs: early-aod-weight finished during non-ls reveal while hostScene=LOCKSCREEN_SMALL).
        // Use screen-off latch || recent stamp — surface hide must not kill LS→AOD morph.
        if (!PixelAodClockView.shouldAnimateLockscreenToAodWeight()) {
            PixelAodLog.log("skipped persistent ClockPlugin lockscreen weight handoff source="
                    + source + " reason=not-lockscreen-to-aod-weight"
                    + " recent={" + describeRecentInteractiveLockscreenForAodEntry() + "}"
                    + " screenOffFromLs="
                    + PixelAodClockView.wasScreenOffFromInteractiveLockscreen()
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        showingClockPluginAodNotificationIcons = true;
        applyNotificationRowPosition();
        rebuildNotificationIcons(currentNotifications(), source + "#aod-handoff");
        runWeightTransition(currentClockWeight,
                PixelAodClockView.aodClockWeight(getContext()), source + "#ls-to-aod");
    }

    /**
     * Stop any in-flight weight morph but keep the current intermediate weight
     * (used when ownership of the morph transfers to the AOD layer).
     */
    void cancelClockPluginWeightTransitionKeepCurrent(String source) {
        if (!clockPluginManaged) {
            return;
        }
        if (clockWeightAnimator == null) {
            return;
        }
        int kept = currentClockWeight;
        clockWeightAnimator.cancel();
        clockWeightAnimator = null;
        clockWeightTransitionPending = false;
        lastClockTransitionStartedAt = 0L;
        // Re-apply kept weight in case cancel listener changed it for aod-to-ls snaps.
        setClockWeight(kept);
        PixelAodLog.log("cancelled lockscreen weight keep-current source=" + source
                + " weight=" + kept
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private void beginLockscreenWeightRestoreTransition(int fromWeight, int toWeight,
            String source) {
        runWeightTransition(fromWeight, toWeight, source + "#aod-to-ls");
    }

    private void runWeightTransition(int fromWeight, int toWeight, String source) {
        if (clockWeightTransitionPending && clockWeightAnimator != null) {
            boolean replacingAodToLockscreenRestore = source != null
                    && source.contains("#ls-to-aod")
                    && activeClockWeightTransitionSource.contains("#aod-to-ls");
            if (replacingAodToLockscreenRestore) {
                PixelAodLog.log("replaced persistent ClockPlugin lockscreen weight restore source="
                        + source + " previousSource=" + activeClockWeightTransitionSource
                        + " currentWeight=" + currentClockWeight
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                clockWeightAnimator.cancel();
            } else {
                PixelAodLog.log("kept persistent ClockPlugin lockscreen weight handoff source="
                        + source + " currentWeight=" + currentClockWeight
                        + " reason=already-running"
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                return;
            }
        }
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
            clockWeightAnimator = null;
        }
        clockWeightTransitionPending = true;
        activeClockWeightTransitionSource = source != null ? source : "";
        lastClockTransitionStartedAt = android.os.SystemClock.uptimeMillis();
        if (fromWeight == toWeight) {
            setClockWeight(toWeight);
            clockWeightTransitionPending = false;
            activeClockWeightTransitionSource = "";
            lastClockTransitionStartedAt = 0L;
            PixelAodLog.log("skipped persistent ClockPlugin lockscreen weight handoff source="
                    + source + " reason=equal-weight weight=" + toWeight
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        final boolean restoreToLockscreen = source != null && source.contains("aod-to-ls");
        setClockWeight(fromWeight);
        clockWeightAnimator = ValueAnimator.ofFloat(0f, 1f);
        clockWeightAnimator.setDuration(PixelAodVisualStyle.COUI_WEIGHT_MORPH_MILLIS);
        clockWeightAnimator.setInterpolator(SIZE_MORPH_INTERPOLATOR);
        final boolean[] cancelled = {false};
        clockWeightAnimator.addUpdateListener(animation -> {
            float progress = (Float) animation.getAnimatedValue();
            int weight = Math.round(fromWeight + ((toWeight - fromWeight) * progress));
            setClockWeight(weight);
        });
        clockWeightAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cancelled[0] = true;
                clockWeightTransitionPending = false;
                activeClockWeightTransitionSource = "";
                lastClockTransitionStartedAt = 0L;
                clockWeightAnimator = null;
                // If aod-to-ls is interrupted while the user is still on lockscreen, finish at
                // lockscreen weight so the clock does not stick at AOD weight (logs 20:55).
                boolean interactive = PixelAodClockView.isDeviceInteractive(getContext());
                if (restoreToLockscreen
                        && interactive
                        && getVisibility() == View.VISIBLE) {
                    setClockWeight(toWeight);
                    PixelAodLog.log("cancelled persistent ClockPlugin lockscreen weight handoff source="
                            + source + " fromWeight=" + fromWeight + " toWeight=" + toWeight
                            + " snappedToLockscreen=true"
                            + " trace=" + PixelAodClockView.currentAodTraceId());
                } else {
                    PixelAodLog.log("cancelled persistent ClockPlugin lockscreen weight handoff source="
                            + source + " fromWeight=" + fromWeight + " toWeight=" + toWeight
                            + " snappedToLockscreen=false interactive=" + interactive
                            + " trace=" + PixelAodClockView.currentAodTraceId());
                }
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (cancelled[0]) {
                    return;
                }
                setClockWeight(toWeight);
                clockWeightTransitionPending = false;
                activeClockWeightTransitionSource = "";
                lastClockTransitionStartedAt = 0L;
                clockWeightAnimator = null;
                // aod-to-ls finished on lockscreen: arm LS→AOD weight morph for the next
                // screen-off even if markInteractive / noteScreenOff lag (logs aod-2f-3df142c).
                if (restoreToLockscreen) {
                    PixelAodClockView.noteLockscreenSessionForAodWeight(source + "#aod-to-ls-end");
                }
                PixelAodLog.log("finished persistent ClockPlugin lockscreen weight handoff source="
                        + source + " toWeight=" + toWeight
                        + " trace=" + PixelAodClockView.currentAodTraceId());
            }
        });
        clockWeightAnimator.start();
        PixelAodLog.log("started persistent ClockPlugin lockscreen weight handoff source="
                + source + " fromWeight=" + fromWeight + " toWeight=" + toWeight
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    /**
     * Glyph-content geometry in parent coordinates + local pivot inside the TextView.
     * Using the full MATCH_PARENT box for large clock made scale ≈ tiny and pivot wrong.
     */
    private static final class ViewMorphSnapshot {
        float centerX;
        float centerY;
        float width;
        float height;
        /** Content-center X/Y in the TextView's local coordinates (for setPivot). */
        float pivotX;
        float pivotY;
        float textSize;
        boolean valid;
    }

    private static void clearViewMorphTransforms(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
    }

    /**
     * Measure the drawn glyph box (not the MATCH_PARENT hit box) so small "22:17" and large
     * two-line "22\\n17" morph between real digit bounds.
     */
    private ViewMorphSnapshot snapshotTextContentMorph(TextView tv) {
        ViewMorphSnapshot snap = new ViewMorphSnapshot();
        if (tv == null) {
            return snap;
        }
        float localLeft;
        float localTop;
        float contentW;
        float contentH;
        Layout layout = tv.getLayout();
        if (layout != null && layout.getLineCount() > 0) {
            float left = Float.MAX_VALUE;
            float right = Float.MIN_VALUE;
            for (int i = 0; i < layout.getLineCount(); i++) {
                left = Math.min(left, layout.getLineLeft(i));
                right = Math.max(right, layout.getLineRight(i));
            }
            contentW = Math.max(1f, right - left);
            contentH = Math.max(1f, (float) layout.getHeight());
            localLeft = tv.getTotalPaddingLeft() + left;
            localTop = tv.getTotalPaddingTop();
        } else {
            CharSequence cs = tv.getText();
            String text = cs != null ? cs.toString() : "";
            boolean multi = text.indexOf('\n') >= 0;
            float maxLineW = 1f;
            if (multi) {
                for (String line : text.split("\n")) {
                    maxLineW = Math.max(maxLineW, tv.getPaint().measureText(line));
                }
            } else {
                maxLineW = Math.max(1f, tv.getPaint().measureText(text));
            }
            contentW = maxLineW;
            contentH = Math.max(1f, tv.getTextSize() * (multi ? 2.15f : 1.2f));
            int vw = tv.getWidth();
            if (vw > contentW * 1.4f) {
                // Centered large clock (MATCH_PARENT).
                localLeft = (vw - contentW) / 2f;
            } else {
                localLeft = tv.getTotalPaddingLeft();
            }
            localTop = tv.getTotalPaddingTop();
            if (tv.getWidth() <= 0 || tv.getHeight() <= 0) {
                return snap;
            }
        }
        snap.width = contentW;
        snap.height = contentH;
        snap.pivotX = localLeft + contentW / 2f;
        snap.pivotY = localTop + contentH / 2f;
        snap.centerX = tv.getLeft() + snap.pivotX + tv.getTranslationX();
        snap.centerY = tv.getTop() + snap.pivotY + tv.getTranslationY();
        snap.textSize = tv.getTextSize();
        snap.valid = contentW > 1f && contentH > 1f && snap.textSize > 1f;
        return snap;
    }

    /** Fallback when the clock has not been measured yet. */
    private ViewMorphSnapshot estimateClockMorphSnapshot(boolean compact) {
        ViewMorphSnapshot snap = new ViewMorphSnapshot();
        float parentW = getWidth() > 0
                ? getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        if (compact) {
            float textPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    PixelAodClockView.scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP),
                    getResources().getDisplayMetrics());
            snap.width = textPx * 3.2f;
            snap.height = textPx * 1.15f;
            snap.pivotX = snap.width / 2f;
            snap.pivotY = snap.height / 2f;
            float parentH = getHeight() > 0
                    ? getHeight()
                    : getResources().getDisplayMetrics().heightPixels;
            snap.centerX = CouiCompactLayout.clockCenterX(Math.round(parentW),
                    Math.round(snap.width), getResources().getDisplayMetrics().density);
            snap.centerY = CouiCompactLayout.clockTop(Math.round(parentH),
                    getResources().getDisplayMetrics().density) + snap.pivotY;
            snap.textSize = textPx;
        } else {
            float textPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    PixelAodClockView.scaledClockTextDp(getContext(), LARGE_CLOCK_TEXT_DP),
                    getResources().getDisplayMetrics());
            // Two-line large clock content ≈ digit width, not full screen width.
            snap.width = textPx * 1.35f;
            snap.height = textPx * 2.15f;
            snap.pivotX = snap.width / 2f;
            snap.pivotY = snap.height / 2f;
            snap.centerX = parentW / 2f;
            snap.centerY = dp(LARGE_CLOCK_TOP_DP) + snap.pivotY;
            snap.textSize = textPx;
        }
        snap.valid = snap.width > 1f && snap.height > 1f;
        return snap;
    }

    /**
     * Target layout is already applied. Before the first draw, park the TextView so its
     * glyph box matches the previous content center/size, then animate to identity.
     */
    private void startTextContentMorph(TextView view, ViewMorphSnapshot from, String source) {
        if (view == null || from == null || !from.valid) {
            return;
        }
        final TextView target = view;
        final ViewMorphSnapshot fromSnap = from;
        ViewTreeObserver observer = target.getViewTreeObserver();
        if (!observer.isAlive()) {
            target.post(() -> runTextContentMorph(target, fromSnap, source));
            return;
        }
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver obs = target.getViewTreeObserver();
                if (obs.isAlive()) {
                    obs.removeOnPreDrawListener(this);
                }
                runTextContentMorph(target, fromSnap, source);
                return true;
            }
        });
        target.invalidate();
    }

    private void runTextContentMorph(TextView target, ViewMorphSnapshot fromSnap, String source) {
        ViewMorphSnapshot to = snapshotTextContentMorph(target);
        if (!to.valid) {
            clearViewMorphTransforms(target);
            return;
        }
        // Prefer textSize ratio so digits grow/shrink continuously (1-line ↔ 2-line layout).
        float scale = fromSnap.textSize > 1f && to.textSize > 1f
                ? (fromSnap.textSize / to.textSize)
                : ((fromSnap.width / to.width) + (fromSnap.height / to.height)) * 0.5f;
        if (scale < 0.25f) {
            scale = 0.25f;
        } else if (scale > 4.5f) {
            scale = 4.5f;
        }
        // Pivot at target glyph center; translation moves that pivot from fromCenter → toCenter.
        float startTx = fromSnap.centerX - to.centerX;
        float startTy = fromSnap.centerY - to.centerY;
        target.animate().cancel();
        target.setPivotX(to.pivotX);
        target.setPivotY(to.pivotY);
        target.setScaleX(scale);
        target.setScaleY(scale);
        target.setTranslationX(startTx);
        target.setTranslationY(startTy);
        target.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(SIZE_MORPH_MILLIS)
                .setInterpolator(SIZE_MORPH_INTERPOLATOR)
                .withEndAction(() -> {
                    clearViewMorphTransforms(target);
                    PixelAodLog.log("finished lockscreen size morph source=" + source
                            + " toCx=" + to.centerX
                            + " toCy=" + to.centerY
                            + " toW=" + to.width
                            + " toH=" + to.height
                            + " toTextSize=" + to.textSize
                            + " trace=" + PixelAodClockView.currentAodTraceId());
                })
                .start();
        PixelAodLog.log("started lockscreen size morph source=" + source
                + " fromCx=" + fromSnap.centerX
                + " fromCy=" + fromSnap.centerY
                + " fromW=" + fromSnap.width
                + " fromH=" + fromSnap.height
                + " fromTextSize=" + fromSnap.textSize
                + " toCx=" + to.centerX
                + " toCy=" + to.centerY
                + " toW=" + to.width
                + " toH=" + to.height
                + " toTextSize=" + to.textSize
                + " pivotX=" + to.pivotX
                + " pivotY=" + to.pivotY
                + " startScale=" + scale
                + " startTx=" + startTx
                + " startTy=" + startTy
                + " durationMs=" + SIZE_MORPH_MILLIS
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    int clockPluginWeight() {
        return currentClockWeight;
    }

    CouiClockSizeTransitionLayer.SceneSnapshot captureClockPluginSizeTransition(
            CouiClockSizeTransitionLayer transitionLayer, ViewGroup coordinateRoot) {
        return transitionLayer.capture(coordinateRoot, clockView, dateView, weatherView,
                contextualRow, contextualIconView, contextualView, currentClockWeight,
                currentInfoWeight);
    }

    void setClockPluginGlyphTransitionActive(boolean active) {
        clockPluginGlyphTransitionActive = active;
    }

    int clockPluginInfoWeight() {
        return currentInfoWeight;
    }

    String clockPluginDiagnosticState() {
        return "{compact=" + compactClock
                + ",weight=" + currentClockWeight
                + ",infoWeight=" + currentInfoWeight
                + ",transitionPending=" + clockWeightTransitionPending
                + ",layer=" + PixelAodClockView.describeViewForHandoff(this)
                + ",clock=" + PixelAodClockView.describeClockTextView(clockView)
                + ',' + PixelAodClockView.describeViewForHandoff(clockView)
                + '}';
    }

    boolean isClockPluginWeightTransitionRunning() {
        return clockWeightTransitionPending
                && clockWeightAnimator != null
                && clockWeightAnimator.isRunning();
    }

    void restoreClockPluginLockscreenWeight(String source) {
        if (!clockPluginManaged) {
            return;
        }
        int fromWeight = currentClockWeight;
        if (clockWeightAnimator != null) {
            // Cancel without treating this as a finished aod-to-ls snap path.
            clockWeightAnimator.cancel();
            clockWeightAnimator = null;
        }
        clockWeightTransitionPending = false;
        lastClockTransitionStartedAt = 0L;
        int targetWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        clearClockPluginAodNotificationIcons(source + "#restore-lockscreen");
        // cancel-early-aod-interactive used to setClockWeight(340) instantly after early-aod-weight
        // parked the layer at ~160 — that is the AOD→LS snap regression with notifications.
        if (fromWeight > 0 && Math.abs(fromWeight - targetWeight) > 8) {
            beginLockscreenWeightRestoreTransition(fromWeight, targetWeight,
                    source + "#restore-animated");
            PixelAodLog.log("restored persistent ClockPlugin lockscreen weight source=" + source
                    + " animated=true fromWeight=" + fromWeight
                    + " toWeight=" + targetWeight
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        } else {
            setClockWeight(targetWeight);
            PixelAodLog.log("restored persistent ClockPlugin lockscreen weight source=" + source
                    + " animated=false weight=" + targetWeight
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
    }

    void setClockPluginLayerVisible(boolean visible) {
        if (!clockPluginManaged) {
            return;
        }
        if (!visible) {
            resetTransitionState();
            animate().cancel();
            setScaleX(1f);
            setScaleY(1f);
            clearViewMorphTransforms(clockView);
            clearViewMorphTransforms(dateView);
            clearClockPluginAodNotificationIcons("ClockPlugin-layer-hidden");
        }
        setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    static void setActiveNotifications(StatusBarNotification[] notifications) {
        synchronized (PixelLockscreenClockView.class) {
            activeNotifications = notifications != null ? notifications.clone() : EMPTY_NOTIFICATIONS;
        }
        refreshVisibleNotificationPresentation("notifications");
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
        boolean clearedRecentInteractive = false;
        synchronized (PixelLockscreenClockView.class) {
            changed = lockscreenSurfaceVisible != visible;
            lockscreenSurfaceVisible = visible;
            if (!visible) {
                // Do NOT clear recent-interactive stamps on every surface hide.
                // Screen-off also reports surfaceVisible=false (sometimes while still
                // briefly interactive), and wiping the stamps made LS→AOD think it was a
                // non-lockscreen entry (instant weight, no morph).
                // Only clear on unlock: interactive + keyguard no longer locked.
                boolean unlockedInteractive = appContext != null
                        && PixelAodClockView.isDeviceInteractive(appContext)
                        && !isSystemKeyguardLockedRaw(appContext);
                if (unlockedInteractive) {
                    interactiveLockscreenVisibleSince = 0L;
                    recentInteractiveLockscreenVisibleAt = 0L;
                    clearedRecentInteractive = true;
                }
            }
        }
        if (clearedRecentInteractive) {
            PixelAodClockView.clearLockscreenSessionForAodWeight(source + "#surface-unlock");
        }
        if (changed) {
            PixelAodLog.log("Pixel lockscreen surface visible=" + visible
                    + " source=" + source
                    + " clearedRecentInteractive=" + clearedRecentInteractive);
            refreshAll(source);
        }
    }

    static boolean isLockscreenSurfaceVisible() {
        synchronized (PixelLockscreenClockView.class) {
            return lockscreenSurfaceVisible;
        }
    }

    static boolean wasRecentlyInteractiveLockscreenVisibleForAodEntry() {
        synchronized (PixelLockscreenClockView.class) {
            long now = android.os.SystemClock.uptimeMillis();
            long age = now - recentInteractiveLockscreenVisibleAt;
            long visibleFor = recentInteractiveLockscreenVisibleAt - interactiveLockscreenVisibleSince;
            return recentInteractiveLockscreenVisibleAt > 0L
                    && interactiveLockscreenVisibleSince > 0L
                    && age >= 0L
                    && age <= LOCKSCREEN_TO_AOD_ANIMATION_WINDOW_MS
                    && visibleFor >= MIN_LOCKSCREEN_VISIBLE_FOR_AOD_ANIMATION_MS;
        }
    }

    static String describeRecentInteractiveLockscreenForAodEntry() {
        synchronized (PixelLockscreenClockView.class) {
            long now = android.os.SystemClock.uptimeMillis();
            long age = recentInteractiveLockscreenVisibleAt > 0L
                    ? now - recentInteractiveLockscreenVisibleAt
                    : -1L;
            long visibleFor = recentInteractiveLockscreenVisibleAt > 0L
                    && interactiveLockscreenVisibleSince > 0L
                    ? recentInteractiveLockscreenVisibleAt - interactiveLockscreenVisibleSince
                    : -1L;
            return "surfaceVisible=" + lockscreenSurfaceVisible
                    + ",interactiveVisibleAgeMs=" + age
                    + ",interactiveVisibleForMs=" + visibleFor;
        }
    }

    static void prepareAodToLockscreenTransition(String source) {
        synchronized (PixelLockscreenClockView.class) {
            pendingAodToLockscreenTransitionAt = android.os.SystemClock.uptimeMillis();
            pendingAodToLockscreenTransitionSource = source;
        }
        PixelAodLog.log("prepared Pixel lockscreen clock weight-only transition trace="
                + PixelAodClockView.currentAodTraceId()
                + " source=" + source
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

    private static void refreshVisibleNotificationPresentation(String source) {
        mainHandler().post(() -> {
            int refreshed = 0;
            for (PixelLockscreenClockView view : INSTANCES) {
                if (view == null
                        || !NotificationPresentationGate.shouldRefreshLockscreen(
                        view.isAttachedToWindow(),
                        view.getVisibility() == View.VISIBLE,
                        view.isShown(),
                        view.getAlpha())) {
                    continue;
                }
                refreshed++;
                view.updatePresentation(source);
            }
            if (refreshed > 0) {
                PixelAodHook.reapplyLockscreenClockFromKnownHost(source);
            }
        });
    }

    static void markInteractiveLockscreenSurface(Context context, String source) {
        if (context == null
                || !PixelAodClockView.isDeviceInteractive(context)
                || !isSystemKeyguardLockedRaw(context)) {
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        boolean started;
        synchronized (PixelLockscreenClockView.class) {
            started = interactiveLockscreenVisibleSince <= 0L
                    || now - recentInteractiveLockscreenVisibleAt > 700L;
            if (started) {
                interactiveLockscreenVisibleSince = now;
            }
            recentInteractiveLockscreenVisibleAt = now;
        }
        PixelAodClockView.noteLockscreenSessionForAodWeight(source + "#mark-interactive");
        if (started) {
            PixelAodLog.log("marked interactive Pixel lockscreen surface source=" + source
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
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
        if (clockPluginManaged) {
            refreshClockPluginLockscreenContent(source);
            return;
        }
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
        if (firstVisibleFrame && !hasTransition) {
            PixelAodClockView.beginContextualSurfaceEntry(source + "#lockscreen-visible");
        } else if (firstVisibleFrame) {
            PixelAodLog.log("preserved contextual surface entry for AOD-to-lockscreen handoff"
                    + " source=" + source + " entry="
                    + PixelAodClockView.currentContextualSurfaceEntry());
        }
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
                || hasLiveLockscreenNotificationCards(firstVisibleFrame);
        // Standalone fallback has no vendor ClockPlugin state. Follow actual visible lockscreen
        // card presence; raw notification or MediaSession activity must not pin Small after the
        // card has collapsed away from the clock area.
        int resolvedClockSize = ClockPluginLockscreenSizePolicy.resolve(null,
                hasCards, !notifications.isEmpty(), playingMedia);
        boolean compact = resolvedClockSize == ClockPluginSceneMachine.CLOCK_SIZE_SMALL;
        applyClockMode(compact);
        applyMaterialColors();
        updateTime();
        rebuildNotificationIcons(Collections.emptyList());
        if (hasTransition) {
            beginClockWeightTransition(source);
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
        markInteractiveLockscreenSurface(getContext(), "lockscreen-dispatch-draw");
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
                + " source=" + pendingAodToLockscreenTransitionSource);
                pendingAodToLockscreenTransitionAt = 0L;
                pendingAodToLockscreenTransitionSource = "";
                return null;
            }
            String source = pendingAodToLockscreenTransitionSource;
            pendingAodToLockscreenTransitionAt = 0L;
            pendingAodToLockscreenTransitionSource = "";
            PixelAodLog.log("consumed Pixel lockscreen clock weight-only transition trace="
                    + PixelAodClockView.currentAodTraceId()
                    + " from=" + source
                    + " update=" + updateSource
                    + " ageMs=" + age
                    + " state={" + PixelAodClockView.describeAodState(appContext, false, -1) + "}");
            return new TransitionInfo(source);
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
        return new TransitionInfo("recent-aod");
    }

    private void updateTime() {
        PixelAodRenderModel model = PixelAodRenderModel.forLockscreen(getContext(), compactClock,
                PixelAodClockView.currentFreshWeather(getContext()));
        PixelAodClockView.applySharedClockText(
                clockView, getContext(), model.clockText, compactClock);
        PixelAodClockView.applySharedInfoText(dateView, getContext(), model.dateText);
        PixelAodClockView.applySharedInfoText(weatherView, getContext(), model.weatherText);
        weatherView.setVisibility(TextUtils.isEmpty(model.weatherText) ? View.GONE : View.VISIBLE);
        PixelAodClockView.applyWeatherLeadingIcon(weatherView,
                model.weather, PixelAodClockView.resolveMaterialClockColor(getContext()));
        ContextualAtAGlanceSelector.Selection contextual = PixelAodClockView.selectContextualCard(
                getContext(), getVisibility() == View.VISIBLE && isShown()
                        && getAlpha() > 0.01f, false, "lockscreen-update");
        FrameLayout.LayoutParams previousNotificationParams =
                (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        int previousNotificationTopPx = previousNotificationParams.topMargin;
        boolean contextualChanged = ContextualAtAGlancePresentation.apply(
                getContext(), contextualRow, contextualIconView, contextualView,
                contextual.card, PixelAodClockView.resolveMaterialInfoColor(getContext()),
                PixelAodClockView.resolveMaterialClockColor(getContext()),
                contextualTextSizeDp(contextual.card),
                currentInfoWeight > 0 ? currentInfoWeight : INFO_LOCKSCREEN_WEIGHT,
                "lockscreen-update");
        FrameLayout.LayoutParams contextualParams =
                (FrameLayout.LayoutParams) contextualRow.getLayoutParams();
        boolean calendarApplicationIcon = contextual.card.kind
                == ContextualAtAGlanceCard.Kind.CALENDAR_EVENT
                && ContextualAtAGlanceCalendarIcon.usesApplicationIcon(getContext());
        ContextualAtAGlanceCalendarIcon.applyRowMargins(contextualParams, getContext(), EDGE_DP,
                calendarApplicationIcon);
        contextualRow.setLayoutParams(contextualParams);
        updateInfoGroupLayout();
        FrameLayout.LayoutParams notificationParams =
                (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        notificationParams.topMargin = notificationRowTopPx();
        notificationIconRow.setLayoutParams(notificationParams);
        if (contextualChanged) {
            ContextualAtAGlancePresentation.animateLowerRows(notificationIconRow, null,
                    previousNotificationTopPx, notificationParams.topMargin, 0, 0);
        }
        ContextualAtAGlanceCalendarIcon.applyGeometry(contextualIconView, getContext(),
                contextualTextSizeDp(contextual.card),
                calendarApplicationIcon);
        updateInfoGroupLayout();
        applyNotificationRowPosition();
    }

    private void applyClockMode(boolean compact) {
        boolean changed = compactClock != compact;
        compactClock = compact;
        FrameLayout.LayoutParams clockParams = (FrameLayout.LayoutParams) clockView.getLayoutParams();
        FrameLayout.LayoutParams notificationParams =
                (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        if (compact) {
            PixelAodClockView.applySharedClockTextStyle(clockView, getContext(), currentClockWeight,
                    PixelAodClockView.scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP), true);
            clockParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            clockParams.gravity = Gravity.TOP | Gravity.START;
            CouiCompactLayout.Anchors anchors = compactAnchors();
            clockParams.leftMargin = anchors.clockLeftPx;
            clockParams.topMargin = anchors.clockTopPx;
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, COMPACT_INFO_TEXT_DP);
            weatherView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, COMPACT_INFO_TEXT_DP);
            int contextualTextSize = contextualTextSizeDp(
                    ContextualAtAGlancePresentation.current(contextualRow));
            contextualView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, contextualTextSize);
            updateContextualSlotIconGeometry(contextualTextSize);
            dateView.setVisibility(View.VISIBLE);
        } else {
            PixelAodClockView.applySharedClockTextStyle(clockView, getContext(), currentClockWeight,
                    PixelAodClockView.scaledClockTextDp(getContext(), LARGE_CLOCK_TEXT_DP), false);
            clockParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            clockParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            clockParams.leftMargin = 0;
            clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, LARGE_INFO_TEXT_DP);
            weatherView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, LARGE_INFO_TEXT_DP);
            int contextualTextSize = contextualTextSizeDp(
                    ContextualAtAGlancePresentation.current(contextualRow));
            contextualView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, contextualTextSize);
            updateContextualSlotIconGeometry(contextualTextSize);
            dateView.setVisibility(View.VISIBLE);
        }
        clockView.setLayoutParams(clockParams);
        updateInfoGroupLayout();
        notificationParams.topMargin = notificationRowTopPx();
        notificationIconRow.setLayoutParams(notificationParams);
        PixelAodClockView.syncNotificationOverflowStyle(
                notificationOverflowView, dateView, notificationIconRow);
        PixelAodLog.log("applied Pixel lockscreen clock mode trace="
                + PixelAodClockView.currentAodTraceId()
                + " compact=" + compact
                + " changed=" + changed
                + " weight=" + currentClockWeight
                + " clockTop=" + clockParams.topMargin
                + " dateTop="
                + ((FrameLayout.LayoutParams) dateView.getLayoutParams()).topMargin
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

    /** Source geometry is used only by the explicit compact-to-large clock size morph. */
    AodGeometryHandoff.Snapshot snapshotClockPluginTextCentersOnScreen() {
        return AodGeometryHandoff.snapshot(textCenterOnScreen(clockView),
                textCenterOnScreen(dateView), textCenterOnScreen(weatherView));
    }

    private static AodGeometryHandoff.Point textCenterOnScreen(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE
                || view.getWidth() <= 0 || view.getHeight() <= 0) {
            return AodGeometryHandoff.Point.INVALID;
        }
        int[] location = new int[2];
        try {
            view.getLocationOnScreen(location);
            return new AodGeometryHandoff.Point(location[0] + view.getWidth() / 2f,
                    location[1] + view.getHeight() / 2f);
        } catch (Throwable ignored) {
            return AodGeometryHandoff.Point.INVALID;
        }
    }

    private void updateInfoGroupLayout() {
        if (dateView == null || weatherView == null || contextualRow == null) {
            return;
        }
        int gapPx = dp(compactClock ? PixelAodVisualStyle.COUI_COMPACT_INFO_TO_EVENT_GAP_DP
                : LARGE_INFO_ROW_GAP_DP);
        int infoLeftPx;
        int dateTopPx;
        int compactWeatherTopPx = 0;
        int contextualMinimumTopPx;
        if (compactClock) {
            CouiCompactLayout.Anchors anchors = compactAnchors();
            FrameLayout.LayoutParams clockParams =
                    (FrameLayout.LayoutParams) clockView.getLayoutParams();
            if (clockParams.leftMargin != anchors.clockLeftPx
                    || clockParams.topMargin != anchors.clockTopPx) {
                clockParams.leftMargin = anchors.clockLeftPx;
                clockParams.topMargin = anchors.clockTopPx;
                clockView.setLayoutParams(clockParams);
            }
            infoLeftPx = anchors.infoLeftPx;
            dateTopPx = anchors.infoTopPx;
            float density = getResources().getDisplayMetrics().density;
            compactWeatherTopPx = CouiCompactLayout.weatherTop(anchors, density);
            contextualMinimumTopPx = CouiCompactLayout.weatherAlertTop(anchors, density);
        } else {
            infoLeftPx = dp(EDGE_DP);
            dateTopPx = dp(LARGE_INFO_TOP_DP);
            contextualMinimumTopPx = dp(LARGE_INFO_TOP_DP
                    + PixelAodVisualStyle.CALENDAR_DATE_TO_EVENT_TOP_OFFSET_DP);
        }
        ClockInfoGroupLayout.Result layout = ClockInfoGroupLayout.layout(
                compactClock, infoLeftPx, dateTopPx,
                infoLineWidthPx(dateView), infoLineHeightPx(dateView,
                        compactClock ? COMPACT_INFO_TEXT_DP : LARGE_INFO_TEXT_DP),
                weatherView.getVisibility() == View.VISIBLE, infoLineHeightPx(weatherView,
                        compactClock ? COMPACT_INFO_TEXT_DP : LARGE_INFO_TEXT_DP),
                compactWeatherTopPx, gapPx, contextualMinimumTopPx);
        FrameLayout.LayoutParams dateParams = (FrameLayout.LayoutParams) dateView.getLayoutParams();
        if (dateParams.leftMargin != layout.dateLeftPx || dateParams.topMargin != layout.dateTopPx) {
            dateParams.leftMargin = layout.dateLeftPx;
            dateParams.topMargin = layout.dateTopPx;
            dateView.setLayoutParams(dateParams);
        }
        FrameLayout.LayoutParams weatherParams = (FrameLayout.LayoutParams) weatherView.getLayoutParams();
        if (weatherParams.leftMargin != layout.weatherLeftPx
                || weatherParams.topMargin != layout.weatherTopPx) {
            weatherParams.leftMargin = layout.weatherLeftPx;
            weatherParams.topMargin = layout.weatherTopPx;
            weatherView.setLayoutParams(weatherParams);
        }
        FrameLayout.LayoutParams contextualParams =
                (FrameLayout.LayoutParams) contextualRow.getLayoutParams();
        if (contextualParams.topMargin != layout.contextualTopPx) {
            contextualParams.topMargin = layout.contextualTopPx;
            contextualRow.setLayoutParams(contextualParams);
        }
    }

    private static int infoLineWidthPx(TextView view) {
        return Math.max(1, PixelAodClockView.estimatedTextContentWidthPx(view));
    }

    private static int infoLineHeightPx(TextView view, int fallbackTextDp) {
        if (view == null) {
            return Math.max(1, fallbackTextDp);
        }
        android.graphics.Paint.FontMetrics metrics = view.getPaint().getFontMetrics();
        int paintedHeight = Math.max(1, Math.round(metrics.descent - metrics.ascent));
        return Math.max(Math.max(1, view.getMeasuredHeight()), paintedHeight
                + view.getTotalPaddingTop() + view.getTotalPaddingBottom());
    }

    private CouiCompactLayout.Anchors compactAnchors() {
        return CouiCompactLayout.anchors(getWidth(), getHeight(),
                PixelAodClockView.estimatedTextContentWidthPx(clockView),
                Math.max(PixelAodClockView.estimatedTextContentWidthPx(dateView),
                        Math.max(PixelAodClockView.estimatedTextContentWidthPx(weatherView),
                                PixelAodClockView.estimatedTextContentWidthPx(contextualView))),
                getResources().getDisplayMetrics().density);
    }

    private void rebuildNotificationIcons(List<StatusBarNotification> notifications) {
        rebuildNotificationIcons(notifications, "legacy");
    }

    private void rebuildNotificationIcons(List<StatusBarNotification> notifications, String source) {
        List<StatusBarNotification> snapshot = notifications != null
                ? notifications : Collections.emptyList();
        if (snapshot.isEmpty()) {
            if (notificationIconRow.getChildCount() == 0
                    && notificationIconRow.getVisibility() == View.GONE
                    && TextUtils.isEmpty(lastClockPluginNotificationIconSignature)) {
                return;
            }
            lastClockPluginNotificationIconSignature = "";
            notificationIconRow.removeAllViews();
            notificationIconRow.setVisibility(View.GONE);
            PixelAodLog.log("cleared Pixel lockscreen AOD handoff notification icons trace="
                    + PixelAodClockView.currentAodTraceId() + " source=" + source);
            return;
        }

        String signature = AodNotificationPipeline.notificationSignature(
                snapshot.toArray(new StatusBarNotification[0]))
                + "|mediaPackages=" + activeMediaPackageSignature();
        if (TextUtils.equals(lastClockPluginNotificationIconSignature, signature)) {
            return;
        }
        lastClockPluginNotificationIconSignature = signature;
        notificationIconRow.removeAllViews();
        notificationOverflowView = null;
        HashSet<String> seenIconKeys = new HashSet<>();
        int skippedMedia = 0;
        int loadFailures = 0;
        ArrayList<String> loadedIconKeys = new ArrayList<>();
        ArrayList<Drawable> loadedIcons = new ArrayList<>();
        for (StatusBarNotification sbn : snapshot) {
            if (sbn == null || sbn.getNotification() == null) {
                continue;
            }
            if (isNotificationForActiveMedia(sbn)) {
                skippedMedia++;
                continue;
            }
            String dedupeKey = AodNotificationPipeline.notificationIconDedupeKey(sbn);
            if (!seenIconKeys.add(dedupeKey)) {
                continue;
            }
            Drawable drawable = PixelAodClockView.loadSmallIconDrawable(getContext(), sbn);
            if (drawable == null) {
                loadFailures++;
                continue;
            }
            loadedIconKeys.add(dedupeKey);
            loadedIcons.add(drawable);
        }
        NotificationIconDisplayPlan displayPlan =
                NotificationIconDisplayPlan.fromEligibleIconKeys(
                        loadedIconKeys, MAX_NOTIFICATION_ICONS);
        int emitted = displayPlan.visibleIconCount();
        for (int index = 0; index < emitted; index++) {
            ImageView iconView = new ImageView(getContext());
            iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconView.setImageDrawable(loadedIcons.get(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(NOTIFICATION_ICON_SIZE_DP), dp(NOTIFICATION_ICON_SIZE_DP));
            if (index > 0) {
                params.leftMargin = dp(NOTIFICATION_ICON_SPACING_DP);
            }
            notificationIconRow.addView(iconView, params);
        }
        notificationOverflowView = PixelAodClockView.addNotificationOverflowText(
                notificationIconRow, displayPlan.overflowCount(), emitted > 0,
                PixelAodClockView.sharedInfoTypeface(getContext(), INFO_LOCKSCREEN_WEIGHT),
                INFO_LOCKSCREEN_WEIGHT,
                compactClock ? COMPACT_INFO_TEXT_DP : LARGE_INFO_TEXT_DP);
        notificationIconRow.setVisibility(
                displayPlan.totalIconCount() > 0 ? View.VISIBLE : View.GONE);
        PixelAodLog.log("rebuilt Pixel lockscreen AOD handoff notification icons trace="
                + PixelAodClockView.currentAodTraceId()
                + " source=" + source
                + " input=" + snapshot.size()
                + " emitted=" + emitted
                + " eligible=" + displayPlan.totalIconCount()
                + " overflow=" + displayPlan.overflowCount()
                + " skippedMedia=" + skippedMedia
                + " loadFailures=" + loadFailures
                + " iconOrder=" + TextUtils.join(",", loadedIconKeys)
                + " compact=" + compactClock
                + " weight=" + currentClockWeight
                + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
    }

    private void clearClockPluginAodNotificationIcons(String source) {
        showingClockPluginAodNotificationIcons = false;
        applyNotificationRowPosition();
        rebuildNotificationIcons(Collections.emptyList(), source);
    }

    private int notificationRowTopPx() {
        int baseTop = showingClockPluginAodNotificationIcons
                ? PixelAodClockView.aodNotificationTopPxForHandoff(getContext(), compactClock,
                getHeight())
                : dp(SMALL_NOTIFICATION_TOP_DP);
        if (!ContextualAtAGlancePresentation.current(contextualRow).isVisible()) {
            return baseTop;
        }
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) contextualRow.getLayoutParams();
        int height = contextualRow.getHeight() > 0
                ? contextualRow.getHeight() : dp(COMPACT_AUXILIARY_INFO_TEXT_DP);
        return Math.max(baseTop, AodInfoStackLayout.rowBottom(params.topMargin, height,
                dp(LARGE_INFO_ROW_GAP_DP)));
    }

    /** Forecast keeps its compact auxiliary geometry in both clock sizes to avoid text reflow. */
    private int contextualTextSizeDp(ContextualAtAGlanceCard card) {
        if (card != null && card.kind == ContextualAtAGlanceCard.Kind.WEATHER_FORECAST) {
            return COMPACT_AUXILIARY_INFO_TEXT_DP;
        }
        return compactClock ? COMPACT_AUXILIARY_INFO_TEXT_DP : LARGE_INFO_TEXT_DP;
    }

    private static boolean hasPendingAodToLockscreenTransition() {
        synchronized (PixelLockscreenClockView.class) {
            long age = android.os.SystemClock.uptimeMillis() - pendingAodToLockscreenTransitionAt;
            return pendingAodToLockscreenTransitionAt > 0L
                    && age >= 0L && age <= AOD_TRANSITION_ANIMATION_WINDOW_MS;
        }
    }

    private void updateContextualSlotIconGeometry(int textSizeDp) {
        boolean applicationIcon = ContextualAtAGlancePresentation.current(contextualRow).kind
                == ContextualAtAGlanceCard.Kind.CALENDAR_EVENT
                && ContextualAtAGlanceCalendarIcon.usesApplicationIcon(getContext());
        ContextualAtAGlanceCalendarIcon.applyGeometry(contextualIconView, getContext(),
                textSizeDp, applicationIcon);
    }

    private void applyNotificationRowPosition() {
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        params.topMargin = notificationRowTopPx();
        notificationIconRow.setLayoutParams(params);
        float translationX = showingClockPluginAodNotificationIcons
                ? -dp(NOTIFICATION_ROW_LEADING_OFFSET_DP) : 0f;
        notificationIconRow.setTranslationX(translationX);
        PixelAodLog.log("aligned lockscreen notification handoff row"
                + " handoff=" + showingClockPluginAodNotificationIcons
                + " compact=" + compactClock
                + " topPx=" + params.topMargin
                + " translationX=" + translationX
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private boolean isNotificationForActiveMedia(StatusBarNotification sbn) {
        if (AodNotificationPipeline.isMediaIconCandidate(sbn)) {
            return true;
        }
        String packageName = sbn != null ? sbn.getPackageName() : "";
        for (MediaController controller : mediaControllers) {
            if (controller != null && TextUtils.equals(packageName, controller.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private String activeMediaPackageSignature() {
        HashSet<String> seenPackages = new HashSet<>();
        ArrayList<String> packages = new ArrayList<>();
        for (MediaController controller : mediaControllers) {
            if (controller == null || TextUtils.isEmpty(controller.getPackageName())
                    || !seenPackages.add(controller.getPackageName())) {
                continue;
            }
            packages.add(controller.getPackageName());
        }
        Collections.sort(packages);
        return TextUtils.join(",", packages);
    }

    private void applyMaterialColors() {
        int clockColor = PixelAodClockView.resolveMaterialClockColor(getContext());
        clockView.setTextColor(clockColor);
        dateView.setTextColor(clockColor);
        weatherView.setTextColor(clockColor);
        contextualView.setTextColor(PixelAodClockView.resolveMaterialInfoColor(getContext()));
        PixelAodClockView.applyWeatherLeadingIcon(weatherView,
                PixelAodClockView.currentFreshWeather(getContext()), clockColor);
    }

    private void beginClockWeightTransition(String source) {
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
        final boolean animateWeight = fromWeight != toWeight;
        if (!animateWeight) {
            setClockWeight(toWeight);
            setTranslationX(0f);
            setTranslationY(0f);
            clockWeightTransitionPending = false;
            clockWeightAnimator = null;
            PixelAodLog.log("skipped Pixel lockscreen transition trace="
                    + PixelAodClockView.currentAodTraceId()
                    + " source=" + source
                    + " reason=no-weight-change"
                    + " weight=" + toWeight
                    + " x=0 y=0"
                    + " state={" + PixelAodClockView.describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            return;
        }
        setClockWeight(fromWeight);
        setTranslationX(0f);
        setTranslationY(0f);
        clockWeightAnimator = ValueAnimator.ofFloat(0f, 1f);
        clockWeightAnimator.setDuration(PixelAodVisualStyle.COUI_WEIGHT_MORPH_MILLIS);
        clockWeightAnimator.setInterpolator(SIZE_MORPH_INTERPOLATOR);
        final boolean[] cancelled = {false};
        clockWeightAnimator.addUpdateListener(animation -> {
            Object animated = animation.getAnimatedValue();
            if (animated instanceof Float) {
                float progress = (Float) animated;
                if (animateWeight) {
                    int weight = Math.round(fromWeight + ((toWeight - fromWeight) * progress));
                    setClockWeight(weight);
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
                + " position=disabled"
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
        setClockWeight(weight, false);
    }

    private void setClockWeight(int weight, boolean handoffBoundary) {
        setInfoWeight(weight);
        if (!ClockTypefaceResolutionPolicy.shouldApplyTypeface(
                currentClockWeight, weight, handoffBoundary)) {
            return;
        }
        currentClockWeight = weight;
        PixelAodClockView.applySharedClockTypeface(clockView, getContext(), weight);
        PixelAodClockView.applySharedClockLetterSpacing(clockView, compactClock);
        if (PixelAodLog.isDebugEnabled()) {
            PixelAodLog.log("clock paint snapshot", () ->
                    "clock paint snapshot layer=lockscreen requestedWeight=" + weight
                            + " state=" + PixelAodClockView.describeClockTextView(clockView));
        }
    }

    private void setInfoWeight(int weight) {
        int synchronizedWeight = AodInfoWeightHandoff.synchronizedInfoWeight(weight,
                PixelAodClockView.aodClockWeight(getContext()),
                PixelAodClockView.lockscreenClockWeight(getContext()));
        if (currentInfoWeight == synchronizedWeight) {
            return;
        }
        currentInfoWeight = synchronizedWeight;
        PixelAodClockView.applySharedClockTypeface(dateView, getContext(), synchronizedWeight);
        PixelAodClockView.applySharedClockTypeface(weatherView, getContext(), synchronizedWeight);
        PixelAodClockView.applySharedClockTypeface(contextualView, getContext(), synchronizedWeight);
        // A weight change alters measured text/ink bounds. Keep the date/weather group and the
        // downstream notification anchor in the same geometry transaction.
        if (contextualRow != null && notificationIconRow != null) {
            updateInfoGroupLayout();
            applyNotificationRowPosition();
        }
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

        TransitionInfo(String source) {
            this.source = source;
        }
    }
}
