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
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

final class PixelLockscreenClockView extends FrameLayout {
    private static final Set<PixelLockscreenClockView> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final int CLOCK_COLOR = Color.rgb(232, 234, 237);
    private static final int INFO_COLOR = Color.rgb(218, 220, 224);
    private static final int LARGE_CLOCK_TEXT_DP = 150;
    private static final int LARGE_CLOCK_TOP_DP = 118;
    private static final int SMALL_CLOCK_TEXT_DP = 56;
    private static final int SMALL_CLOCK_TOP_DP = 74;
    private static final int EDGE_DP = 34;
    private static final int LARGE_INFO_TOP_DP = 36;
    private static final int SMALL_INFO_TOP_DP = 150;
    private static final int SMALL_NOTIFICATION_TOP_DP = 198;
    private static final int NOTIFICATION_ICON_SIZE_DP = 18;
    private static final int NOTIFICATION_ICON_SPACING_DP = 9;
    private static final int MAX_NOTIFICATION_ICONS = 5;
    private static final float CLOCK_LINE_SPACING = 0.70f;
    private static final float LARGE_CLOCK_LETTER_SPACING = -0.02f;
    private static final float SMALL_CLOCK_LETTER_SPACING = -0.025f;
    private static final int CLOCK_LOCKSCREEN_WEIGHT = 520;
    private static final int INFO_LOCKSCREEN_WEIGHT = 500;
    private static final long AOD_TRANSITION_ANIMATION_WINDOW_MS = 1800L;
    private static final StatusBarNotification[] EMPTY_NOTIFICATIONS = new StatusBarNotification[0];

    private static StatusBarNotification[] activeNotifications = EMPTY_NOTIFICATIONS;
    private static Context appContext;
    private static int modeLogCount;
    private static int nonKeyguardSuppressionLogCount;
    private static boolean hasVisibleLockscreenNotificationCards;
    private static boolean lockscreenSurfaceVisible;
    private static long pendingAodToLockscreenTransitionAt;
    private static String pendingAodToLockscreenTransitionSource = "";

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
    private final Runnable clockWeightTransitionStarter = new Runnable() {
        @Override
        public void run() {
            clockWeightTransitionPending = false;
            animateClockWeight(PixelAodClockView.aodClockWeight(getContext()),
                    PixelAodClockView.lockscreenClockWeight(getContext()));
        }
    };

    PixelLockscreenClockView(Context context) {
        super(context);
        appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        int lockscreenWeight = PixelAodClockView.lockscreenClockWeight(context);
        currentClockWeight = lockscreenWeight;
        Typeface clockTypeface = PixelAodClockView.sharedClockTypeface(context, lockscreenWeight);
        Typeface infoTypeface = PixelAodClockView.sharedInfoTypeface(context, INFO_LOCKSCREEN_WEIGHT);

        clockView = new TextView(context);
        clockView.setTextColor(resolveMaterialClockColor(context));
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                PixelAodClockView.scaledClockTextDp(context, LARGE_CLOCK_TEXT_DP));
        clockView.setTypeface(clockTypeface);
        PixelAodClockView.applySharedFontVariation(clockView, lockscreenWeight);
        clockView.setIncludeFontPadding(false);
        clockView.setElegantTextHeight(false);
        clockView.setGravity(Gravity.CENTER_HORIZONTAL);
        clockView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        clockView.setSingleLine(false);
        clockView.setLines(2);
        clockView.setLineSpacing(0f, CLOCK_LINE_SPACING);
        clockView.setLetterSpacing(LARGE_CLOCK_LETTER_SPACING);
        clockView.setFontFeatureSettings("tnum");
        clockView.setAlpha(0.98f);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
        addView(clockView, clockParams);

        dateView = new TextView(context);
        dateView.setTextColor(resolveMaterialInfoColor(context));
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        dateView.setTypeface(infoTypeface);
        PixelAodClockView.applySharedFontVariation(dateView, INFO_LOCKSCREEN_WEIGHT);
        dateView.setIncludeFontPadding(false);
        dateView.setGravity(Gravity.START);
        dateView.setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
        dateView.setSingleLine(true);
        dateView.setAlpha(0.94f);
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
        removeCallbacks(clockWeightTransitionStarter);
        clockWeightTransitionPending = false;
        stop();
        INSTANCES.remove(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!shouldShowOnLockscreen(getContext())) {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            if (nonKeyguardSuppressionLogCount < 12) {
                nonKeyguardSuppressionLogCount++;
                PixelAodLog.log("suppressed Pixel lockscreen clock outside keyguard");
            }
            return;
        }
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
    }

    static void setActiveNotifications(StatusBarNotification[] notifications) {
        synchronized (PixelLockscreenClockView.class) {
            activeNotifications = notifications != null ? notifications.clone() : EMPTY_NOTIFICATIONS;
        }
        refreshAll("notifications");
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
        }
        refreshAll(source);
    }

    static void setLockscreenSurfaceVisible(boolean visible, String source) {
        boolean changed;
        synchronized (PixelLockscreenClockView.class) {
            changed = lockscreenSurfaceVisible != visible;
            lockscreenSurfaceVisible = visible;
        }
        if (changed) {
            PixelAodLog.log("Pixel lockscreen surface visible=" + visible
                    + " source=" + source);
        }
        refreshAll(source);
    }

    static void prepareAodToLockscreenTransition(String source) {
        synchronized (PixelLockscreenClockView.class) {
            pendingAodToLockscreenTransitionAt = android.os.SystemClock.uptimeMillis();
            pendingAodToLockscreenTransitionSource = source;
        }
        PixelAodLog.log("prepared Pixel lockscreen clock weight transition source="
                + source);
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
        if (context == null || !PixelAodClockView.isDeviceInteractive(context)) {
            return false;
        }
        return isSystemKeyguardLocked(context);
    }

    static boolean isSystemKeyguardLocked(Context context) {
        if (context == null || !PixelAodClockView.isDeviceInteractive(context)) {
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

    private void updatePresentation(String source) {
        boolean visible = shouldShowOnLockscreen(getContext());
        boolean firstVisibleFrame = visible && getVisibility() != View.VISIBLE;
        boolean animateWeight = visible
                && (firstVisibleFrame || consumeRecentAodToLockscreenTransition(source));
        if (animateWeight) {
            setClockWeight(PixelAodClockView.aodClockWeight(getContext()));
        }
        setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        List<StatusBarNotification> notifications = currentNotifications();
        boolean hasCards = hasVisibleLockscreenNotificationCards();
        boolean compact = !notifications.isEmpty() || hasCards;
        applyClockMode(compact);
        applyMaterialColors();
        updateTime();
        rebuildNotificationIcons(hasCards ? Collections.emptyList() : notifications);
        if (animateWeight) {
            beginClockWeightTransition(source);
        } else if (!clockWeightTransitionPending) {
            setClockWeight(PixelAodClockView.lockscreenClockWeight(getContext()));
        }
        if (modeLogCount < 12) {
            modeLogCount++;
            PixelAodLog.log("Pixel lockscreen clock visible compact="
                    + compactClock + " notifications=" + notifications.size()
                    + " source=" + source);
        }
    }

    private static boolean consumeRecentAodToLockscreenTransition(String updateSource) {
        synchronized (PixelLockscreenClockView.class) {
            long markedAt = pendingAodToLockscreenTransitionAt;
            if (markedAt <= 0L) {
                return false;
            }
            long age = android.os.SystemClock.uptimeMillis() - markedAt;
            if (age < 0L || age > AOD_TRANSITION_ANIMATION_WINDOW_MS) {
                pendingAodToLockscreenTransitionAt = 0L;
                pendingAodToLockscreenTransitionSource = "";
                return false;
            }
            String source = pendingAodToLockscreenTransitionSource;
            pendingAodToLockscreenTransitionAt = 0L;
            pendingAodToLockscreenTransitionSource = "";
            PixelAodLog.log("consumed Pixel lockscreen clock weight transition from="
                    + source + " update=" + updateSource + " ageMs=" + age);
            return true;
        }
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        boolean is24Hour = DateFormat.is24HourFormat(getContext());
        int hour = calendar.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) {
            hour = 12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        if (compactClock) {
            clockView.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        } else {
            clockView.setText(String.format(Locale.getDefault(), "%02d\n%02d", hour, minute));
        }
        Locale locale = Locale.getDefault();
        dateView.setText(PixelAodClockView.formatDateWithWeather(calendar));
        PixelAodClockView.applyWeatherIcon(dateView,
                PixelAodClockView.currentFreshWeather(getContext()),
                resolveMaterialInfoColor(getContext()));
    }

    private void applyClockMode(boolean compact) {
        if (compactClock == compact) {
            return;
        }
        compactClock = compact;
        FrameLayout.LayoutParams clockParams = (FrameLayout.LayoutParams) clockView.getLayoutParams();
        FrameLayout.LayoutParams dateParams = (FrameLayout.LayoutParams) dateView.getLayoutParams();
        FrameLayout.LayoutParams notificationParams =
                (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        if (compact) {
            clockView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                    PixelAodClockView.scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP));
            clockView.setGravity(Gravity.START);
            clockView.setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
            clockView.setSingleLine(true);
            clockView.setLines(1);
            clockView.setLineSpacing(0f, 1f);
            clockView.setLetterSpacing(SMALL_CLOCK_LETTER_SPACING);
            clockView.setFontFeatureSettings("pnum");
            clockParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            clockParams.gravity = Gravity.TOP | Gravity.START;
            clockParams.leftMargin = dp(EDGE_DP);
            clockParams.topMargin = dp(SMALL_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            dateParams.topMargin = dp(SMALL_INFO_TOP_DP);
            notificationParams.topMargin = dp(SMALL_NOTIFICATION_TOP_DP);
        } else {
            clockView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                    PixelAodClockView.scaledClockTextDp(getContext(), LARGE_CLOCK_TEXT_DP));
            clockView.setGravity(Gravity.CENTER_HORIZONTAL);
            clockView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            clockView.setSingleLine(false);
            clockView.setLines(2);
            clockView.setLineSpacing(0f, CLOCK_LINE_SPACING);
            clockView.setLetterSpacing(LARGE_CLOCK_LETTER_SPACING);
            clockView.setFontFeatureSettings("tnum");
            clockParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            clockParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            clockParams.leftMargin = 0;
            clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            dateParams.topMargin = dp(LARGE_INFO_TOP_DP);
            notificationParams.topMargin = dp(SMALL_NOTIFICATION_TOP_DP);
        }
        clockView.setLayoutParams(clockParams);
        dateView.setLayoutParams(dateParams);
        notificationIconRow.setLayoutParams(notificationParams);
    }

    private void rebuildNotificationIcons(List<StatusBarNotification> notifications) {
        notificationIconRow.removeAllViews();
        if (notifications.isEmpty()) {
            notificationIconRow.setVisibility(View.GONE);
            return;
        }
        notificationIconRow.setVisibility(View.VISIBLE);
        int emitted = 0;
        HashSet<String> seenPackages = new HashSet<>();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null || !seenPackages.add(sbn.getPackageName())) {
                continue;
            }
            Drawable drawable = PixelAodClockView.loadSmallIconDrawable(getContext(), sbn);
            if (drawable == null) {
                continue;
            }
            ImageView iconView = new ImageView(getContext());
            iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconView.setImageDrawable(drawable);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(NOTIFICATION_ICON_SIZE_DP), dp(NOTIFICATION_ICON_SIZE_DP));
            if (emitted > 0) {
                params.leftMargin = dp(NOTIFICATION_ICON_SPACING_DP);
            }
            notificationIconRow.addView(iconView, params);
            emitted++;
            if (emitted >= MAX_NOTIFICATION_ICONS) {
                break;
            }
        }
        notificationIconRow.setVisibility(emitted > 0 ? View.VISIBLE : View.GONE);
    }

    private void applyMaterialColors() {
        int clockColor = resolveMaterialClockColor(getContext());
        int infoColor = resolveMaterialInfoColor(getContext());
        clockView.setTextColor(clockColor);
        dateView.setTextColor(infoColor);
        PixelAodClockView.applyWeatherIcon(dateView,
                PixelAodClockView.currentFreshWeather(getContext()), infoColor);
    }

    private void animateClockWeight(int fromWeight, int toWeight) {
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
        }
        setClockWeight(fromWeight);
        clockWeightAnimator = ValueAnimator.ofInt(fromWeight, toWeight);
        clockWeightAnimator.setDuration(700L);
        clockWeightAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f));
        clockWeightAnimator.addUpdateListener(animation -> {
            Object value = animation.getAnimatedValue();
            if (value instanceof Integer) {
                setClockWeight((Integer) value);
            }
        });
        clockWeightAnimator.start();
    }

    private void beginClockWeightTransition(String source) {
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
        }
        removeCallbacks(clockWeightTransitionStarter);
        clockWeightTransitionPending = true;
        int fromWeight = PixelAodClockView.aodClockWeight(getContext());
        int toWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        setClockWeight(fromWeight);
        postDelayed(clockWeightTransitionStarter, 140L);
        PixelAodLog.log("scheduled visible Pixel lockscreen weight transition source="
                + source + " from=" + fromWeight + " to=" + toWeight);
    }

    private void setClockWeight(int weight) {
        if (currentClockWeight == weight) {
            return;
        }
        currentClockWeight = weight;
        PixelAodClockView.applySharedFontVariation(clockView, weight);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                clockView.setTypeface(PixelAodClockView.sharedClockTypeface(getContext(), weight));
            } catch (Throwable ignored) {
                // Font variation is the primary path; static typeface fallback is best-effort.
            }
        }
    }

    private static int resolveMaterialClockColor(Context context) {
        return resolveSystemColor(context, "system_accent1_100", CLOCK_COLOR);
    }

    private static int resolveMaterialInfoColor(Context context) {
        int color = resolveSystemColor(context, "system_accent1_200", INFO_COLOR);
        return Color.argb(230, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int resolveSystemColor(Context context, String name, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            int id = context.getResources().getIdentifier(name, "color", "android");
            if (id != 0) {
                return context.getColor(id);
            }
        } catch (Throwable ignored) {
            // Dynamic color resources are available on modern Android only.
        }
        return fallback;
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
}
