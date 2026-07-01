package dev.codex.pixelaod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.text.TextUtils;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PixelAodHook {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean SETTINGS_OBSERVER_REGISTERED = new AtomicBoolean(false);
    private static final String CLOCK_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodClockLayout";
    private static final String AOD_ROOT_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodRootLayout";
    private static final String AOD_RECORD = "com.oplus.systemui.aod.AodRecord";
    private static final String AOD_DISPLAY_UTIL =
            "com.oplus.systemui.aod.display.AODDisplayUtil";
    private static final String AOD_SMOOTH_TRANSITION_CONTROLLER =
            "com.oplus.systemui.aod.display.SmoothTransitionController";
    private static final String SHADE_WINDOW_VIEW =
            "com.android.systemui.shade.NotificationShadeWindowView";
    private static final String KEYGUARD_STYLE_CLOCK =
            "com.oplus.systemui.keyguard.view.CustomOplusKeyguardStyleClock";
    private static final String KEYGUARD_CLOCK_VIEW_ROOT =
            "com.oplus.keyguard.clock.big.ui.view.ClockViewRoot";
    private static final String KEYGUARD_NOTIFICATION_VISIBILITY_PROVIDER_IMPL =
            "com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProviderImpl";
    private static final String NOTIF_FILTER =
            "com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifFilter";
    private static final String CUSTOM_TAG = "dev.codex.pixelaod.PIXEL_CLOCK";
    private static final String LOCKSCREEN_CUSTOM_TAG = "dev.codex.pixelaod.PIXEL_LOCKSCREEN_CLOCK";
    private static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    private static final int STATUS_EDGE_DP = 68;
    private static final int NOTIFICATION_FLAG_SILENT = 0x00020000;
    private static final long AOD_ENTRY_STATE_REWRITE_WINDOW_MILLIS = 10000L;
    private static final boolean ENABLE_EXPENSIVE_DEBUG_REAPPLY = false;
    private static final boolean ENABLE_EXPENSIVE_DEBUG_DUMPS = false;
    private static final boolean ENABLE_NOTIFICATION_VIEW_REFLECTION_DUMP = false;
    private static final boolean ENABLE_GLOBAL_STOCK_VIEW_METHOD_HOOKS = false;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<String> LOGGED_INSPECTION_CLASSES = java.util.Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> LOGGED_VIEW_TREE_KEYS = new HashSet<>();
    private static final Set<String> LOGGED_STOCK_SUPPRESSION_MISS_KEYS =
            Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> HOOKED_NOTIFICATION_VIEW_CLASSES = new HashSet<>();
    private static final Set<View> INSPECTED_PLUGIN_NOTIFICATION_VIEWS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<View, HiddenState> HIDDEN_STOCK_VIEWS = new WeakHashMap<>();
    private static final Map<View, AdjustedState> ADJUSTED_STATUS_VIEWS = new WeakHashMap<>();
    private static final Map<View, Long> LOCKSCREEN_HOST_TOUCH_TIMES = new WeakHashMap<>();
    private static final LinkedHashMap<String, StatusBarNotification> NOTIFICATION_CACHE = new LinkedHashMap<>();
    private static final Pattern TEMPERATURE_PATTERN =
            Pattern.compile("-?\\d{1,2}\\s*(?:[°℃℉]|\\s?[CF]\\b)");
    private static final Pattern NOTIFICATION_RELATIVE_TIME_PATTERN =
            Pattern.compile("(?i)(?:\\bjust now\\b|\\b\\d+\\s*(?:min|mins|minute|minutes|hr|hrs|hour|hours)\\s+ago\\b|刚刚|\\d+\\s*(?:分钟|小时)前)");
    private static final Pattern NOTIFICATION_CLOCK_TIME_PATTERN =
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");
    private static NotificationListenerService lastNotificationListener;
    private static WeakReference<ViewGroup> lastStockHost = new WeakReference<>(null);
    private static WeakReference<ViewGroup> lastPixelHost = new WeakReference<>(null);
    private static WeakReference<ViewGroup> lastShadeHost = new WeakReference<>(null);

    private PixelAodHook() {
    }

    static void install(Context context, ClassLoader classLoader) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        PixelAodSettings.refresh(context);
        registerSettingsObserver(context);
        boolean customAod = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_CUSTOM_AOD, true);
        boolean lockscreenClock = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_LOCKSCREEN_CLOCK, true);
        boolean notificationIcons = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_NOTIFICATION_ICONS, true);
        boolean lockscreenPolicy = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true);
        boolean weather = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_WEATHER, true);
        if (weather) {
            PixelAodClockView.ensureBreezyWeatherReceiver(context);
        }
        if (customAod) {
            hookClockLayout(context, classLoader);
            hookNotificationView(classLoader);
            hookAodRecord(classLoader);
            hookSkipDozeOffState(context, classLoader);
            if (ENABLE_GLOBAL_STOCK_VIEW_METHOD_HOOKS) {
                hookStockClockVisibilityAndAlphaSuppression();
            } else {
                PixelAodLog.log("skipped global stock View visibility/alpha hooks");
            }
        }
        if (notificationIcons || customAod || lockscreenClock) {
            hookNotificationListenerService();
            hookSystemUiNotificationListener(classLoader);
        }
        if (lockscreenPolicy) {
            hookLockscreenNotificationPolicy(classLoader);
        }
        if (customAod || lockscreenClock) {
            hookShadeWindowView(context, classLoader);
            hookLockscreenClockProbe(classLoader);
            PixelAodLog.log("skipped global stock clock draw suppression to avoid UI jank");
        }
        PixelAodLog.log("installed Pixel AOD hooks customAod=" + customAod
                + " lockscreenClock=" + lockscreenClock
                + " notificationIcons=" + notificationIcons
                + " lockscreenPolicy=" + lockscreenPolicy
                + " weather=" + weather);
    }

    private static void registerSettingsObserver(Context context) {
        if (context == null || !SETTINGS_OBSERVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        try {
            appContext.getContentResolver().registerContentObserver(
                    PixelAodSettingsProvider.URI,
                    true,
                    new ContentObserver(MAIN) {
                        @Override
                        public void onChange(boolean selfChange) {
                            onChange(selfChange, null);
                        }

                        @Override
                        public void onChange(boolean selfChange, android.net.Uri uri) {
                            PixelAodSettings.refresh(appContext);
                            PixelAodLog.log("refreshed Pixel AOD settings from provider change"
                                    + " selfChange=" + selfChange
                                    + " uri=" + uri);
                        }
                    });
            PixelAodLog.log("registered Pixel AOD settings observer");
        } catch (Throwable t) {
            PixelAodLog.log("failed to register Pixel AOD settings observer", t);
        }
    }

    private static void hookStockClockDrawSuppression() {
        try {
            ModernHookBridge.hookBefore(View.class, "draw", param -> {
                if (!(param.thisObject instanceof View)) {
                    return;
                }
                View view = (View) param.thisObject;
                if (shouldSuppressStockClockDraw(view)) {
                    param.setResult(null);
                }
            }, Canvas.class);
            PixelAodLog.log("hooked stock AOD/keyguard clock draw suppression");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook stock AOD/keyguard clock draw suppression", t);
        }
    }

    private static boolean shouldSuppressStockClockDraw(View view) {
        if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView
                || hasCustomClockAncestor(view)) {
            return false;
        }
        String className = view.getClass().getName();
        if (!isStockDrawSuppressionClassCandidate(className)) {
            return false;
        }
        Context context = view.getContext();
        if (context == null) {
            logStockSuppressionMiss("draw", view, "no-context");
            return false;
        }
        if (isChargingUiView(view)) {
            logStockSuppressionMiss("draw", view, "charging-ui");
            return false;
        }
        boolean interactive = PixelAodClockView.isDeviceInteractive(context);
        boolean keyguardLocked = PixelLockscreenClockView.isSystemKeyguardLocked(context);
        if (!PixelAodClockView.isAodActive()) {
            logStockSuppressionMiss("draw", view, "aod-inactive");
        }
        if (interactive && !keyguardLocked) {
            logStockSuppressionMiss("draw", view, "interactive-unlocked");
            return false;
        }
        String marker = markerFor(view);
        boolean suppress = false;
        if (isStockAodDrawCandidate(marker, view)
                || isStockKeyguardClockDrawCandidate(marker, view)
                || looksLikePluginBatteryView(marker)
                || looksLikePluginNotificationView(marker)) {
            suppress = true;
        }
        if (!suppress) {
            logStockSuppressionMiss("draw", view, "candidate-not-matched");
            return false;
        }
        view.setAlpha(0f);
        PixelAodLog.log("suppressed stock clock draw marker=" + marker
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(context) + "}");
        return true;
    }

    private static void hookStockClockVisibilityAndAlphaSuppression() {
        try {
            ModernHookBridge.hookBefore(View.class, "setVisibility", param -> {
                if (!(param.thisObject instanceof View)) {
                    return;
                }
                View view = (View) param.thisObject;
                if (shouldSuppressStockClockByHook(view)) {
                    int visibility = (int) param.args[0];
                    if (visibility != View.GONE) {
                        param.args[0] = View.GONE;
                    }
                }
            }, int.class);
            PixelAodLog.log("hooked stock AOD/keyguard clock setVisibility suppression");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook stock clock setVisibility suppression", t);
        }

        try {
            ModernHookBridge.hookBefore(View.class, "setAlpha", param -> {
                if (!(param.thisObject instanceof View)) {
                    return;
                }
                View view = (View) param.thisObject;
                if (shouldSuppressStockClockByHook(view)) {
                    float alpha = (float) param.args[0];
                    if (alpha != 0f) {
                        param.args[0] = 0f;
                    }
                }
            }, float.class);
            PixelAodLog.log("hooked stock AOD/keyguard clock setAlpha suppression");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook stock clock setAlpha suppression", t);
        }
    }

    private static boolean shouldSuppressStockClockByHook(View view) {
        if (!PixelAodClockView.isAodActive()) {
            logStockSuppressionMiss("hook", view, "aod-inactive");
            return false;
        }
        if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView
                || hasCustomClockAncestor(view)) {
            return false;
        }
        String className = view.getClass().getName();
        if (!isStockDrawSuppressionClassCandidate(className)) {
            return false;
        }
        Context context = view.getContext();
        if (context == null) {
            logStockSuppressionMiss("hook", view, "no-context");
            return false;
        }
        if (isChargingUiView(view)) {
            logStockSuppressionMiss("hook", view, "charging-ui");
            return false;
        }
        String marker = markerFor(view);
        boolean suppress = isStockAodDrawCandidate(marker, view)
                || looksLikePluginBatteryView(marker)
                || looksLikePluginNotificationView(marker);
        if (!suppress) {
            logStockSuppressionMiss("hook", view, "candidate-not-matched");
        }
        return suppress;
    }

    private static void logStockSuppressionMiss(String path, View view, String reason) {
        if (view == null || !PixelAodLog.isDebugEnabled()) {
            return;
        }
        String className = view.getClass().getName();
        String marker = markerFor(view);
        String key = path + '|' + reason + '|' + className + '|' + marker;
        synchronized (LOGGED_STOCK_SUPPRESSION_MISS_KEYS) {
            if (!LOGGED_STOCK_SUPPRESSION_MISS_KEYS.add(key)) {
                return;
            }
        }
        PixelAodLog.log("stock suppression miss path=" + path
                + " reason=" + reason
                + " class=" + className
                + " marker=" + marker
                + " shown=" + view.isShown()
                + " visibility=" + view.getVisibility()
                + " alpha=" + view.getAlpha()
                + " size=" + view.getWidth() + "x" + view.getHeight()
                + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
    }

    private static String hostSummary(ViewGroup host) {
        if (host == null) {
            return "null";
        }
        return markerFor(host) + " children=" + host.getChildCount()
                + " shown=" + host.isShown()
                + " visibility=" + host.getVisibility();
    }

    private static boolean isStockDrawSuppressionClassCandidate(String className) {
        if (className == null) {
            return false;
        }
        String name = className.toLowerCase(Locale.US);
        if (name.contains("pixelaod")
                || name.contains("media")
                || name.contains("music")
                || name.contains("charge")
                || name.contains("finger")
                || name.contains("biometric")
                || name.contains("udfps")) {
            return false;
        }
        return name.startsWith("com.oplus.egview.widget.batteryview")
                || name.startsWith("com.oplus.egview.widget.notificationview")
                || name.contains("aod")
                || name.contains("clock")
                || name.contains("timeview")
                || name.contains("dateview")
                || name.contains("datemessage")
                || name.contains("date_message")
                || name.contains("keyguardstatusview")
                || name.contains("keyguardclockswitch")
                || name.contains("keyguard")
                || name.contains("weather")
                || name.contains("temperature");
    }

    private static boolean hasCustomClockAncestor(View view) {
        ViewParent parent = view.getParent();
        int depth = 0;
        while (parent instanceof View && depth < 8) {
            if (parent instanceof PixelAodClockView || parent instanceof PixelLockscreenClockView) {
                return true;
            }
            parent = ((View) parent).getParent();
            depth++;
        }
        return false;
    }

    private static boolean isStockAodDrawCandidate(String marker, View view) {
        if (looksLikeSystemAodMediaView(marker)) {
            return false;
        }
        if (looksLikeOplusKeyguardBigClock(marker)) {
            return true;
        }
        if (looksLikeGenericStockAodVisual(marker, view)) {
            return true;
        }
        if (looksLikeStockAodWeatherOrExtra(marker, view instanceof TextView
                ? ((TextView) view).getText() : null)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            return looksLikeStockAodClockContainer(marker);
        }
        if (view instanceof TextView) {
            return looksLikeStockAodText(marker, ((TextView) view).getText());
        }
        return looksLikeStockAodClockLeaf(marker);
    }

    private static void hookLockscreenClockProbe(ClassLoader classLoader) {
        PixelAodLog.log("skipped global lockscreen/shade View probe to avoid UI jank");
        hookLockscreenClockProbeClass(classLoader, KEYGUARD_STYLE_CLOCK);
        hookLockscreenClockProbeClass(classLoader, KEYGUARD_CLOCK_VIEW_ROOT);
    }

    private static void hookLockscreenClockGlobalAttachProbe() {
        try {
            ModernHookBridge.hookAfter(View.class, "onAttachedToWindow", param -> {
                Object candidate = param.thisObject;
                if (!(candidate instanceof View)) {
                    return;
                }
                View view = (View) candidate;
                String className = candidate.getClass().getName();
                if (isShadeWindowClassName(className) && candidate instanceof ViewGroup) {
                    MAIN.post(() -> handleLockscreenHost(view.getContext(), (ViewGroup) candidate,
                            "View#onAttachedToWindow/" + className));
                    return;
                }
                if (!isLockscreenClockClassName(className)) {
                    return;
                }
                MAIN.post(() -> inspectLockscreenClockCandidate(candidate,
                        "View#onAttachedToWindow/" + className));
            });
            ModernHookBridge.hookAfter(View.class, "onVisibilityChanged", param -> {
                Object candidate = param.thisObject;
                if (!(candidate instanceof View)) {
                    return;
                }
                View view = (View) candidate;
                String className = view.getClass().getName();
                if (isShadeWindowClassName(className) && candidate instanceof ViewGroup) {
                    MAIN.post(() -> handleLockscreenHost(view.getContext(), (ViewGroup) candidate,
                            "View#onVisibilityChanged/" + className));
                    return;
                }
                if (!isLockscreenClockClassName(className)) {
                    return;
                }
                MAIN.post(() -> inspectLockscreenClockCandidate(view,
                        "View#onVisibilityChanged/" + className));
            }, View.class, int.class);
            PixelAodLog.log("hooked global lockscreen/shade attach+visibility probe");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook global lockscreen/shade probe", t);
        }
    }

    private static boolean isShadeWindowClassName(String className) {
        return SHADE_WINDOW_VIEW.equals(className)
                || className.contains("NotificationShadeWindowView");
    }

    private static boolean isLockscreenClockClassName(String className) {
        return KEYGUARD_STYLE_CLOCK.equals(className)
                || KEYGUARD_CLOCK_VIEW_ROOT.equals(className)
                || className.contains("CustomOplusKeyguardStyleClock")
                || className.contains("ClockViewRoot");
    }

    private static void hookLockscreenClockProbeClass(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            ModernHookBridge.hookAfter(clazz, "onAttachedToWindow",
                    param -> MAIN.post(() -> inspectLockscreenClockCandidate(param.thisObject, className)));
            PixelAodLog.log("hooked lockscreen clock probe " + className);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook lockscreen clock probe " + className, t);
        }
    }

    private static void hookClockLayout(Context context, ClassLoader classLoader) {
        try {
            Class<?> clockLayoutClass = ModernHookBridge.findClass(CLOCK_LAYOUT, classLoader);
            ModernHookBridge.hookAfter(clockLayoutClass, "initForAodApk",
                    param -> MAIN.post(() -> handleClockLayout(context, param.thisObject, "initForAodApk")));
            ModernHookBridge.hookAfter(clockLayoutClass, "onAttachedToWindow",
                    param -> MAIN.post(() -> handleClockLayout(context, param.thisObject, "onAttachedToWindow")));
            PixelAodLog.log("hooked " + CLOCK_LAYOUT + " init/attach");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodClockLayout", t);
        }
    }

    private static void hookShadeWindowView(Context context, ClassLoader classLoader) {
        try {
            Class<?> shadeClass = ModernHookBridge.findClass(SHADE_WINDOW_VIEW, classLoader);
            ModernHookBridge.hookAfter(shadeClass, "onAttachedToWindow", param -> {
                if (param.thisObject instanceof ViewGroup) {
                    MAIN.post(() -> handleLockscreenHost(context, (ViewGroup) param.thisObject,
                            "NotificationShadeWindowView#onAttachedToWindow"));
                }
            });
            PixelAodLog.log("hooked " + SHADE_WINDOW_VIEW + " attach");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook " + SHADE_WINDOW_VIEW, t);
        }
    }

    private static void hookOuterRootLayout(ClassLoader classLoader) {
        try {
            Class<?> rootLayoutClass = ModernHookBridge.findClass(AOD_ROOT_LAYOUT, classLoader);
            ModernHookBridge.hookAfter(rootLayoutClass, "onAttachedToWindow", param -> {
                if (param.thisObject instanceof ViewGroup) {
                    ViewGroup root = (ViewGroup) param.thisObject;
                    if (!isAodRootLayout(root) || isChargingUiView(root)) {
                        return;
                    }
                    MAIN.post(() -> handleOuterRootLayout(root, "AodRootLayout#onAttachedToWindow"));
                }
            });
            PixelAodLog.log("hooked " + AOD_ROOT_LAYOUT + "#onAttachedToWindow");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodRootLayout.onAttachedToWindow", t);
        }
    }

    private static void hookNotificationView(ClassLoader classLoader) {
        try {
            Class<?> notificationViewClass = ModernHookBridge.findClass(
                    "com.oplus.egview.widget.NotificationView", classLoader);
            hookNotificationViewClass(notificationViewClass, "SystemUI loader");
        } catch (Throwable t) {
            PixelAodLog.log("failed to find OPlus NotificationView updates from SystemUI loader", t);
        }
    }

    private static void hookNotificationListenerService() {
        try {
            ModernHookBridge.hookAfter(NotificationListenerService.class,
                    "onListenerConnected", param -> {
                        if (param.thisObject instanceof NotificationListenerService) {
                            rememberNotificationListener(param.thisObject,
                                    "NotificationListenerService#onListenerConnected");
                            publishNotificationsFromListener(
                                    (NotificationListenerService) param.thisObject,
                                    "NotificationListenerService#onListenerConnected");
                        }
                    });
            ModernHookBridge.hookAfter(NotificationListenerService.class,
                    "onNotificationPosted", param -> {
                        if (param.args != null && param.args.length > 0
                                && param.args[0] instanceof StatusBarNotification) {
                            rememberNotificationListener(param.thisObject,
                                    "NotificationListenerService#onNotificationPosted");
                            cacheNotification((StatusBarNotification) param.args[0],
                                    "onNotificationPosted");
                        }
                    }, StatusBarNotification.class);
            ModernHookBridge.hookAfter(NotificationListenerService.class,
                    "onNotificationRemoved", param -> {
                        if (param.args != null && param.args.length > 0
                                && param.args[0] instanceof StatusBarNotification) {
                            rememberNotificationListener(param.thisObject,
                                    "NotificationListenerService#onNotificationRemoved");
                            removeCachedNotification((StatusBarNotification) param.args[0],
                                    "onNotificationRemoved");
                        }
                    }, StatusBarNotification.class);
            PixelAodLog.log("hooked NotificationListenerService fallback notification cache");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook NotificationListenerService fallback notification cache", t);
        }
    }

    private static void hookSystemUiNotificationListener(ClassLoader classLoader) {
        try {
            Class<?> listenerClass = ModernHookBridge.findClass(
                    "com.android.systemui.statusbar.NotificationListener", classLoader);
            hookNotificationListenerClass(listenerClass, "SystemUI NotificationListener");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook SystemUI NotificationListener fallback", t);
        }
    }

    private static void hookNotificationListenerClass(Class<?> listenerClass, String source) {
        boolean hookedPosted = false;
        boolean hookedRemoved = false;
        try {
            ModernHookBridge.hookAfter(listenerClass, "onListenerConnected", param -> {
                if (param.thisObject instanceof NotificationListenerService) {
                    rememberNotificationListener(param.thisObject, source + "#onListenerConnected");
                    publishNotificationsFromListener(
                            (NotificationListenerService) param.thisObject,
                            source + "#onListenerConnected");
                }
            });
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook " + source + "#onListenerConnected", t);
        }
        for (Method method : listenerClass.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            if (!"onNotificationPosted".equals(name) && !"onNotificationRemoved".equals(name)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            int sbnIndex = firstParameterIndex(parameterTypes, StatusBarNotification.class);
            if (sbnIndex < 0) {
                continue;
            }
            int rankingIndex = firstRankingMapParameterIndex(parameterTypes);
            try {
                method.setAccessible(true);
                ModernHookBridge.hookAfter(method, param -> {
                    if (param.args == null || param.args.length <= sbnIndex
                            || !(param.args[sbnIndex] instanceof StatusBarNotification)) {
                        return;
                    }
                    String methodSource = source + "#" + name;
                    rememberNotificationListener(param.thisObject, methodSource);
                    if (rankingIndex >= 0) {
                        publishRankingFromArg(param, rankingIndex);
                    }
                    StatusBarNotification sbn = (StatusBarNotification) param.args[sbnIndex];
                    if ("onNotificationRemoved".equals(name)) {
                        removeCachedNotification(sbn, methodSource);
                    } else {
                        cacheNotification(sbn, methodSource);
                    }
                });
                if ("onNotificationRemoved".equals(name)) {
                    hookedRemoved = true;
                } else {
                    hookedPosted = true;
                }
                PixelAodLog.log("hooked " + source + "#" + methodSignature(method));
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook " + source + "#" + methodSignature(method), t);
            }
        }
        PixelAodLog.log("hooked " + source + " fallback notification cache posted="
                + hookedPosted + " removed=" + hookedRemoved);
    }

    private static int firstParameterIndex(Class<?>[] parameterTypes, Class<?> target) {
        if (parameterTypes == null) {
            return -1;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (target.isAssignableFrom(parameterTypes[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int firstRankingMapParameterIndex(Class<?>[] parameterTypes) {
        if (parameterTypes == null) {
            return -1;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if ("android.service.notification.NotificationListenerService$RankingMap"
                    .equals(parameterTypes[i].getName())) {
                return i;
            }
        }
        return -1;
    }

    private static String methodSignature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[i].getSimpleName());
        }
        return builder.append(')').toString();
    }

    private static void publishRankingFromArg(ModernHookBridge.HookParam param, int index) {
        try {
            if (param.args == null || param.args.length <= index
                    || !(param.args[index] instanceof NotificationListenerService.RankingMap)) {
                return;
            }
            PixelAodClockView.updateRankingMap((NotificationListenerService.RankingMap) param.args[index]);
        } catch (Throwable t) {
            PixelAodLog.log("failed to publish notification ranking map", t);
        }
    }

    private static void rememberNotificationListener(Object candidate, String source) {
        if (!(candidate instanceof NotificationListenerService)) {
            return;
        }
        lastNotificationListener = (NotificationListenerService) candidate;
        PixelAodLog.log("remembered notification listener from " + source
                + " class=" + candidate.getClass().getName()
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(null) + "}");
    }

    private static void refreshNotificationsFromLastListener(String source) {
        NotificationListenerService service = lastNotificationListener;
        if (service == null) {
            PixelAodLog.log("no cached notification listener for " + source
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        publishNotificationsFromListener(service, source);
    }

    private static void publishNotificationsFromListener(NotificationListenerService service, String source) {
        try {
            StatusBarNotification[] notifications = service.getActiveNotifications();
            if (notifications == null) {
                return;
            }
            try {
                PixelAodClockView.updateRankingMap(service.getCurrentRanking());
            } catch (Throwable t) {
                PixelAodLog.log("failed to capture current notification ranking from " + source, t);
            }
            synchronized (NOTIFICATION_CACHE) {
                NOTIFICATION_CACHE.clear();
                for (StatusBarNotification sbn : notifications) {
                    if (sbn != null) {
                        NOTIFICATION_CACHE.put(sbn.getKey(), sbn);
                    }
                }
            }
            PixelAodClockView.setActiveNotifications(notifications);
            PixelAodClockView.setMediaNotificationCandidates(notifications, source);
            PixelAodLog.log("captured active notifications from " + source
                    + " count=" + notifications.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to capture active notifications from " + source, t);
        }
    }

    private static void cacheNotification(StatusBarNotification sbn, String source) {
        try {
            StatusBarNotification[] snapshot;
            synchronized (NOTIFICATION_CACHE) {
                NOTIFICATION_CACHE.put(sbn.getKey(), sbn);
                snapshot = NOTIFICATION_CACHE.values().toArray(new StatusBarNotification[0]);
            }
            PixelAodClockView.setActiveNotifications(snapshot);
            PixelAodClockView.cacheMediaNotificationCandidate(sbn, source);
            PixelAodLog.log("cached notification from " + source
                    + " pkg=" + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " count=" + snapshot.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to cache notification from " + source, t);
        }
    }

    private static void removeCachedNotification(StatusBarNotification sbn, String source) {
        try {
            StatusBarNotification[] snapshot;
            synchronized (NOTIFICATION_CACHE) {
                NOTIFICATION_CACHE.remove(sbn.getKey());
                snapshot = NOTIFICATION_CACHE.values().toArray(new StatusBarNotification[0]);
            }
            PixelAodClockView.setActiveNotifications(snapshot);
            PixelAodClockView.removeMediaNotificationCandidate(sbn, source);
            PixelAodLog.log("removed notification from " + source
                    + " pkg=" + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " count=" + snapshot.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to remove notification from " + source, t);
        }
    }

    private static void hookRuntimeNotificationView(Class<?> notificationViewClass, String source) {
        try {
            hookNotificationViewClass(notificationViewClass, source);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook runtime OPlus NotificationView updates from " + source, t);
        }
    }

    private static void hookNotificationViewClass(Class<?> notificationViewClass, String source) {
        if (notificationViewClass == null) {
            return;
        }
        String key = notificationViewClass.getName() + "|loader=" + notificationViewClass.getClassLoader();
        synchronized (HOOKED_NOTIFICATION_VIEW_CLASSES) {
            if (!HOOKED_NOTIFICATION_VIEW_CLASSES.add(key)) {
                return;
            }
        }
        try {
            ModernHookBridge.hookAfter(notificationViewClass, "onActiveNotifications",
                    param -> publishNotificationsFromArg(param, 0, "onActiveNotifications/" + source),
                    StatusBarNotification[].class);
            ModernHookBridge.hookAfter(notificationViewClass, "onReceiveNotification",
                    param -> publishNotificationsFromArg(param, 0, "onReceiveNotification/" + source),
                    StatusBarNotification[].class, StatusBarNotification.class);
            ModernHookBridge.hookAfter(notificationViewClass, "onRemoveNotification",
                    param -> publishNotificationsFromArg(param, 0, "onRemoveNotification/" + source),
                    StatusBarNotification[].class, StatusBarNotification.class);
            ModernHookBridge.hookAfter(notificationViewClass, "clearNotificationView",
                    param -> clearCachedNotifications("NotificationView#clearNotificationView"));
            PixelAodLog.log("hooked OPlus NotificationView notification updates from " + source);
        } catch (Throwable t) {
            synchronized (HOOKED_NOTIFICATION_VIEW_CLASSES) {
                HOOKED_NOTIFICATION_VIEW_CLASSES.remove(key);
            }
            PixelAodLog.log("failed to hook OPlus NotificationView updates from " + source, t);
        }
    }

    private static void publishNotificationsFromArg(ModernHookBridge.HookParam param, int index, String source) {
        try {
            if (param.args == null || param.args.length <= index || !(param.args[index] instanceof StatusBarNotification[])) {
                return;
            }
            StatusBarNotification[] notifications = (StatusBarNotification[]) param.args[index];
            StatusBarNotification[] snapshot = mergeCachedNotifications(notifications);
            if (lastNotificationListener != null) {
                refreshNotificationsFromLastListener(source + "#merged-oplus-subset");
            } else {
                PixelAodClockView.setActiveNotifications(snapshot);
            }
            PixelAodLog.log("merged OPlus AOD notification subset from " + source
                    + " subset=" + notifications.length
                    + " cache=" + snapshot.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to publish AOD notifications from " + source, t);
        }
    }

    private static StatusBarNotification[] mergeCachedNotifications(StatusBarNotification[] notifications) {
        synchronized (NOTIFICATION_CACHE) {
            if (notifications != null) {
                for (StatusBarNotification sbn : notifications) {
                    if (sbn != null) {
                        NOTIFICATION_CACHE.put(sbn.getKey(), sbn);
                    }
                }
            }
            return NOTIFICATION_CACHE.values().toArray(new StatusBarNotification[0]);
        }
    }

    private static void replaceCachedNotifications(StatusBarNotification[] notifications) {
        synchronized (NOTIFICATION_CACHE) {
            NOTIFICATION_CACHE.clear();
            if (notifications == null) {
                return;
            }
            for (StatusBarNotification sbn : notifications) {
                if (sbn != null) {
                    NOTIFICATION_CACHE.put(sbn.getKey(), sbn);
                }
            }
        }
    }

    private static void clearCachedNotifications(String source) {
        try {
            if (lastNotificationListener != null) {
                refreshNotificationsFromLastListener(source);
                PixelAodLog.log("refreshed fallback native AOD notification icons from " + source
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(null) + "}");
                return;
            }
            synchronized (NOTIFICATION_CACHE) {
                NOTIFICATION_CACHE.clear();
            }
            PixelAodClockView.clearActiveNotifications();
            PixelAodLog.log("cleared fallback native AOD notification icons from " + source
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to clear cached notifications from " + source, t);
        }
    }

    private static void publishCachedNotifications(String source) {
        try {
            StatusBarNotification[] snapshot;
            synchronized (NOTIFICATION_CACHE) {
                snapshot = NOTIFICATION_CACHE.values().toArray(new StatusBarNotification[0]);
            }
            PixelAodClockView.setActiveNotifications(snapshot);
            PixelAodLog.log("kept fallback native AOD notification icons from "
                    + source + " count=" + snapshot.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to publish cached notifications from " + source, t);
        }
    }

    private static void hookAodRecord(ClassLoader classLoader) {
        try {
            Class<?> recordClass = ModernHookBridge.findClass(AOD_RECORD, classLoader);
            ModernHookBridge.hookAfter(recordClass, "createAndInitRootView", param -> {
                Object result = param.getResult();
                if (result instanceof ViewGroup) {
                    ViewGroup root = (ViewGroup) result;
                    PixelAodLog.log("AodRecord root=" + root.getClass().getName()
                            + " children=" + root.getChildCount());
                    MAIN.post(() -> handleOuterRootLayout(root, "AodRecord#createAndInitRootView"));
                }
            }, Context.class);
            ModernHookBridge.hookAfter(recordClass, "onDreamingStarted", param -> MAIN.post(() -> {
                refreshNotificationsFromLastListener("AodRecord#onDreamingStarted");
                PixelAodClockView.setAodActive(true, "AodRecord#onDreamingStarted");
                PixelAodClockView.tickAllInstances();
            }), boolean.class);
            ModernHookBridge.hookBefore(recordClass, "onDreamingStopped",
                    param -> runAtFrontOfMain(() -> {
                        String transitionSource = "AodRecord#onDreamingStopped";
                        PixelLockscreenClockView.prepareAodToLockscreenTransition(transitionSource);
                        PixelAodClockView.hideAllAodOverlays(transitionSource);
                        String transitionTrace = PixelAodClockView.peekAodTraceId();
                        PixelAodLog.log("AodRecord#onDreamingStopped trace=" + transitionTrace
                                + " state={" + PixelAodClockView.describeAodState(null) + "}");
                        PixelAodClockView.stopAllInstances();
                        restoreAdjustedStatusViews();
                        if (PixelLockscreenClockView.shouldShowOnKnownContext()) {
                            applyLockscreenClockReplacementFromLastHosts(transitionSource);
                        } else {
                            suppressSystemAodDuringLockscreenTransition(transitionSource);
                            restoreHiddenStockViewsAfterTransition(transitionSource, transitionTrace);
                        }
                    }));
            PixelAodLog.log("hooked " + AOD_RECORD + " lifecycle/root");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodRecord lifecycle", t);
        }
    }

    private static void hookLockscreenNotificationPolicy(ClassLoader classLoader) {
        hookKeyguardNotificationVisibilityProvider(classLoader);
        hookKeyguardNotifFilter(classLoader);
    }

    private static void hookKeyguardNotificationVisibilityProvider(ClassLoader classLoader) {
        try {
            Class<?> providerClass = ModernHookBridge.findClass(
                    KEYGUARD_NOTIFICATION_VISIBILITY_PROVIDER_IMPL, classLoader);
            Class<?> entryClass = ModernHookBridge.findClass(
                    "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                    classLoader);
            ModernHookBridge.hookAfter(providerClass, "shouldHideNotification", param -> {
                if (param.args == null || param.args.length == 0) {
                    return;
                }
                StatusBarNotification sbn = statusBarNotificationFromEntry(param.args[0]);
                Object ranking = rankingFromEntry(param.args[0]);
                boolean hidden = Boolean.TRUE.equals(param.getResult());
                if (hidden && isEligibleForLockscreenPolicyOverride(sbn, ranking,
                        "KeyguardNotificationVisibilityProvider")) {
                    param.setResult(false);
                    hidden = false;
                }
                PixelAodClockView.updateLockscreenVisibilityFromProvider(
                        sbn, hidden, "KeyguardNotificationVisibilityProvider");
            }, entryClass);
            PixelAodLog.log("hooked KeyguardNotificationVisibilityProvider lockscreen policy");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook KeyguardNotificationVisibilityProvider lockscreen policy", t);
        }
    }

    private static void hookKeyguardNotifFilter(ClassLoader classLoader) {
        try {
            Class<?> filterClass = ModernHookBridge.findClass(NOTIF_FILTER, classLoader);
            Class<?> entryClass = ModernHookBridge.findClass(
                    "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                    classLoader);
            Method method = filterClass.getDeclaredMethod("shouldFilterOut",
                    entryClass,
                    long.class);
            if (Modifier.isAbstract(method.getModifiers())) {
                PixelAodLog.log("skipped abstract keyguard NotifFilter lockscreen policy fallback");
                return;
            }
            ModernHookBridge.hookAfter(filterClass, "shouldFilterOut", param -> {
                if (param.args == null
                        || param.args.length == 0
                        || !looksLikeKeyguardNotificationFilter(param.thisObject)) {
                    return;
                }
                StatusBarNotification sbn = statusBarNotificationFromEntry(param.args[0]);
                Object ranking = rankingFromEntry(param.args[0]);
                String source = filterName(param.thisObject);
                boolean hidden = Boolean.TRUE.equals(param.getResult());
                if (hidden && isEligibleForLockscreenPolicyOverride(sbn, ranking, source)) {
                    param.setResult(false);
                    hidden = false;
                }
                PixelAodClockView.updateLockscreenVisibilityFromFilter(sbn, hidden, source);
            }, entryClass, long.class);
            PixelAodLog.log("hooked keyguard NotifFilter lockscreen policy fallback");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook keyguard NotifFilter lockscreen policy fallback", t);
        }
    }

    private static boolean looksLikeKeyguardNotificationFilter(Object filter) {
        String marker = filterName(filter).toLowerCase(Locale.US);
        return marker.contains("keyguard")
                || marker.contains("lockscreen")
                || marker.contains("lock_screen")
                || marker.contains("minimalism")
                || marker.contains("unseen")
                || marker.contains("dnd")
                || marker.contains("visualeffects");
    }

    private static String filterName(Object filter) {
        if (filter == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder(filter.getClass().getName());
        try {
            Object name = ModernHookBridge.callMethod(filter, "getName");
            if (name != null) {
                builder.append("/").append(name);
            }
        } catch (Throwable ignored) {
            // Name is best-effort diagnostics only.
        }
        return builder.toString();
    }

    private static StatusBarNotification statusBarNotificationFromEntry(Object entry) {
        if (entry == null) {
            return null;
        }
        try {
            Object value = ModernHookBridge.callMethod(entry, "getSbn");
            if (value instanceof StatusBarNotification) {
                return (StatusBarNotification) value;
            }
        } catch (Throwable ignored) {
            // OPlus variants may rename or inline accessors.
        }
        try {
            Object value = ModernHookBridge.getObjectField(entry, "mSbn");
            if (value instanceof StatusBarNotification) {
                return (StatusBarNotification) value;
            }
        } catch (Throwable ignored) {
            // Field fallback is best-effort.
        }
        return null;
    }

    private static Object rankingFromEntry(Object entry) {
        if (entry == null) {
            return null;
        }
        try {
            return ModernHookBridge.callMethod(entry, "getRanking");
        } catch (Throwable ignored) {
            // OPlus variants may rename or inline accessors.
        }
        try {
            return ModernHookBridge.getObjectField(entry, "mRanking");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isEligibleForLockscreenPolicyOverride(StatusBarNotification sbn,
            Object ranking, String source) {
        if (sbn == null || sbn.getNotification() == null
                || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        String pkg = sbn.getPackageName();
        boolean testNotification = MODULE_PACKAGE.equals(pkg)
                && TestNotificationReceiver.TEST_TAG.equals(sbn.getTag());
        if (MODULE_PACKAGE.equals(pkg) && !testNotification) {
            PixelAodLog.log("blocked lockscreen policy override pkg=" + pkg
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " reason=module-package-not-test-notification"
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            return false;
        }
        if ("android".equals(pkg) || "com.android.systemui".equals(pkg)) {
            PixelAodLog.log("blocked lockscreen policy override pkg=" + pkg
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " reason=system-package"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return false;
        }
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
            PixelAodLog.log("blocked lockscreen policy override pkg=" + pkg
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " reason=transport-category"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return false;
        }
        if (notification.visibility == Notification.VISIBILITY_SECRET) {
            PixelAodLog.log("blocked lockscreen policy override pkg=" + pkg
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " reason=secret-visibility"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return false;
        }
        if (rankingVisibilitySecret(ranking)) {
            PixelAodLog.log("blocked lockscreen policy override pkg=" + pkg
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " reason=ranking-secret"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return false;
        }
        int importance = rankingImportance(ranking);
        if (!testNotification && (importance == 0 || importance > 0 && importance < 3
                || (notification.flags & NOTIFICATION_FLAG_SILENT) != 0)) {
            PixelAodLog.log("blocked lockscreen policy override pkg=" + pkg
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " reason=low-importance-or-silent"
                    + " importance=" + importance
                    + " flags=0x" + Integer.toHexString(notification.flags)
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return false;
        }
        PixelAodLog.log("allowing lockscreen notification through OOS policy pkg="
                + pkg + " key=" + sbn.getKey() + " importance=" + importance
                + " source=" + source
                + " category=" + notification.category
                + " visibility=" + notification.visibility
                + " flags=0x" + Integer.toHexString(notification.flags)
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(null) + "}");
        return true;
    }

    private static boolean rankingVisibilitySecret(Object ranking) {
        if (ranking == null) {
            return false;
        }
        try {
            Object override = ModernHookBridge.callMethod(ranking, "getLockscreenVisibilityOverride");
            if (override instanceof Integer
                    && (Integer) override == Notification.VISIBILITY_SECRET) {
                return true;
            }
        } catch (Throwable ignored) {
            // Continue with channel visibility fallback.
        }
        try {
            Object channel = ModernHookBridge.callMethod(ranking, "getChannel");
            if (channel instanceof NotificationChannel
                    && ((NotificationChannel) channel).getLockscreenVisibility()
                    == Notification.VISIBILITY_SECRET) {
                return true;
            }
        } catch (Throwable ignored) {
            // Best-effort only.
        }
        return false;
    }

    private static int rankingImportance(Object ranking) {
        if (ranking == null) {
            return Integer.MIN_VALUE;
        }
        try {
            Object value = ModernHookBridge.callMethod(ranking, "getImportance");
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Throwable ignored) {
            // Best-effort only.
        }
        return Integer.MIN_VALUE;
    }

    private static void handleClockLayout(Context context, Object clockLayoutObject, String source) {
        try {
            if (!(clockLayoutObject instanceof ViewGroup)) {
                PixelAodLog.log("AodClockLayout is not ViewGroup from " + source + ": " + clockLayoutObject);
                return;
            }
            Object aodView = null;
            try {
                aodView = ModernHookBridge.getObjectField(clockLayoutObject, "mAodViewFromApk");
            } catch (Throwable ignored) {
                // Field may be unavailable early in attach; initForAodApk will run with the real host.
            }
            if (!(aodView instanceof ViewGroup)) {
                PixelAodLog.log("skip clock injection from " + source
                        + " because mAodViewFromApk is not ready; layout="
                        + clockLayoutObject.getClass().getName());
                return;
            }
            handleClockHost(context, (ViewGroup) aodView, "AodClockLayout#" + source);
        } catch (Throwable t) {
            PixelAodLog.log("customize AOD clock layout failed from " + source, t);
        }
    }

    private static void handleOuterRootLayout(ViewGroup host, String source) {
        try {
            if (!isAodRootLayout(host)) {
                PixelAodLog.log("ignored AOD outer root from " + source
                        + " reason=not-aod-root host=" + hostSummary(host)
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                return;
            }
            if (isChargingUiView(host)) {
                PixelAodLog.log("ignored AOD outer root from " + source
                        + " reason=charging-ui host=" + hostSummary(host)
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                return;
            }
            PixelAodLog.log("observed AOD outer root from " + source + " host="
                    + host.getClass().getName() + " children=" + host.getChildCount()
                    + " state={" + PixelAodClockView.describeAodState(host.getContext()) + "}");
            hideStockClockViews(host);
            adjustPluginStatusViews(host.getContext(), host);
            scheduleStockSuppressionReapply(host, source);
        } catch (Throwable t) {
            PixelAodLog.log("observe AOD outer root failed from " + source, t);
        }
    }

    private static void hookSkipDozeOffState(Context context, ClassLoader classLoader) {
        if (!PixelAodSettings.getBoolean(context, PixelAodSettings.KEY_SKIP_DOZE_OFF_STATE, false)) {
            return;
        }
        hookAodDisplayUtilScreenState(context, classLoader);
        try {
            ModernHookBridge.hookBefore(DreamService.class, "setDozeScreenState", param -> {
                if (!(param.thisObject instanceof DreamService) || param.args == null
                        || param.args.length == 0 || !(param.args[0] instanceof Integer)) {
                    return;
                }
                DreamService service = (DreamService) param.thisObject;
                rewriteAodEntryState(service, param.args, 0, "DreamService#setDozeScreenState");
            }, int.class);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook DreamService doze OFF skip", t);
        }
    }

    private static void hookAodDisplayUtilScreenState(Context context, ClassLoader classLoader) {
        try {
            Class<?> utilClass = ModernHookBridge.findClass(AOD_DISPLAY_UTIL, classLoader);
            ModernHookBridge.hookBefore(utilClass, "requestScreenState",
                    param -> rewriteAodEntryState(context, param.args, 0,
                            "AODDisplayUtil#requestScreenState(int,int,String)"),
                    int.class, int.class, String.class);
            ModernHookBridge.hookBefore(utilClass, "requestScreenState",
                    param -> rewriteAodEntryState(viewContextOr(context, param.args, 0),
                            param.args, 1, "AODDisplayUtil#requestScreenState(View,int,boolean)"),
                    View.class, int.class, boolean.class);
            ModernHookBridge.hookBefore(utilClass, "requestScreenStateWhileDreamingStart",
                    param -> rewriteAodEntryState(context, param.args, 0,
                            "AODDisplayUtil#requestScreenStateWhileDreamingStart"),
                    int.class, String.class, boolean.class);
            PixelAodLog.log("hooked " + AOD_DISPLAY_UTIL + " screen-state entry rewrite");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AODDisplayUtil screen-state entry rewrite", t);
        }
        try {
            Class<?> smoothClass = ModernHookBridge.findClass(AOD_SMOOTH_TRANSITION_CONTROLLER,
                    classLoader);
            ModernHookBridge.hookBefore(smoothClass, "requestScreenState",
                    param -> rewriteAodEntryState(context, param.args, 0,
                            "SmoothTransitionController#requestScreenState"),
                    int.class);
            PixelAodLog.log("hooked " + AOD_SMOOTH_TRANSITION_CONTROLLER
                    + " screen-state entry rewrite");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook SmoothTransitionController screen-state entry rewrite", t);
        }
    }

    private static Context viewContextOr(Context fallback, Object[] args, int index) {
        if (args != null && args.length > index && args[index] instanceof View) {
            Context context = ((View) args[index]).getContext();
            if (context != null) {
                return context;
            }
        }
        return fallback;
    }

    private static boolean shouldBypassStateRewrite(Object[] args) {
        if (args == null) {
            return false;
        }
        for (Object arg : args) {
            if (arg instanceof String) {
                String str = ((String) arg).toLowerCase(Locale.US);
                if (str.contains("prox")
                        || str.contains("pocket")
                        || str.contains("sensor")
                        || str.contains("near")
                        || str.contains("timeout")
                        || str.contains("power")
                        || str.contains("key")
                        || str.contains("fold")
                        || str.contains("lid")
                        || str.contains("close")
                        || str.contains("suspend")
                        || str.contains("sleep")
                        || str.contains("saver")
                        || str.contains("schedule")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAodAllowedBySystemSettings(Context context) {
        if (context == null) {
            return true;
        }
        try {
            android.content.ContentResolver resolver = context.getContentResolver();
            
            // Check global AOD switches
            int aodEnable = android.provider.Settings.Secure.getInt(resolver, "Setting_AodEnable", 1);
            int aodSwitchEnable = android.provider.Settings.Secure.getInt(resolver, "Setting_AodSwitchEnable", 1);
            int aodState = android.provider.Settings.Secure.getInt(resolver, "Setting_AodState", 1);
            if (aodEnable == 0 || aodSwitchEnable == 0 || aodState == 0) {
                return false;
            }

            int aodDisplayMode = android.provider.Settings.Secure.getInt(resolver, "aod_display_mode", 1);
            // If display mode is 0 (off), return false
            if (aodDisplayMode == 0) {
                return false;
            }

            int userSetTime = android.provider.Settings.Secure.getInt(resolver, "Setting_AodUserSetTime", 0);
            
            // Mode 3 is scheduled, or UserSetTime == 1 is scheduled
            if (aodDisplayMode == 3 || userSetTime == 1) {
                int beginHour = android.provider.Settings.Secure.getInt(resolver, "Setting_AodSetTimeBeginHour", 7);
                int beginMin = android.provider.Settings.Secure.getInt(resolver, "Setting_AodSetTimeBeginMin", 0);
                int endHour = android.provider.Settings.Secure.getInt(resolver, "Setting_AodSetTimeEndHour", 23);
                int endMin = android.provider.Settings.Secure.getInt(resolver, "Setting_AodSetTimeEndMin", 0);

                java.util.Calendar now = java.util.Calendar.getInstance();
                int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
                int currentMin = now.get(java.util.Calendar.MINUTE);

                int nowMinutes = currentHour * 60 + currentMin;
                int startMinutes = beginHour * 60 + beginMin;
                int endMinutes = endHour * 60 + endMin;

                if (startMinutes < endMinutes) {
                    // Schedule is within the same day, e.g. 07:00 to 23:00
                    if (nowMinutes < startMinutes || nowMinutes >= endMinutes) {
                        return false;
                    }
                } else if (startMinutes > endMinutes) {
                    // Schedule crosses midnight, e.g. 23:00 to 07:00
                    if (nowMinutes < startMinutes && nowMinutes >= endMinutes) {
                        return false;
                    }
                }
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to check AOD system settings", t);
        }
        return true;
    }

    private static void rewriteAodEntryState(Context context, Object[] args, int stateIndex,
            String source) {
        if (args == null || args.length <= stateIndex || !(args[stateIndex] instanceof Integer)) {
            return;
        }
        int requestedState = (Integer) args[stateIndex];
        if (requestedState != Display.STATE_OFF
                && requestedState != Display.STATE_DOZE_SUSPEND) {
            return;
        }
        if (context != null && PixelAodClockView.isDeviceInteractive(context)) {
            PixelAodLog.log("skipped AOD rewrite source=" + source
                    + " requestedState=" + requestedState
                    + " reason=interactive"
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }

        if (context != null && !isAodAllowedBySystemSettings(context)) {
            PixelAodLog.log("skipped AOD rewrite source=" + source
                    + " requestedState=" + requestedState
                    + " reason=system-settings"
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        if (requestedState == Display.STATE_DOZE_SUSPEND
                && PixelAodClockView.shouldKeepDozeScreenActive(context)) {
            args[stateIndex] = Display.STATE_DOZE;
            PixelAodLog.i("rewrote " + source
                    + " DOZE_SUSPEND->DOZE while custom AOD needs live frames"
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        if (shouldBypassStateRewrite(args)) {
            PixelAodLog.log("skipped AOD rewrite source=" + source
                    + " requestedState=" + requestedState
                    + " reason=bypass-keywords"
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }

        if (context != null) {
            PixelAodClockView.noteScreenOffIfUnset(source + "#off-request");
        }
        if (!PixelAodClockView.isInAodEntryTransitionWindow(
                AOD_ENTRY_STATE_REWRITE_WINDOW_MILLIS)) {
            PixelAodLog.log("skipped AOD rewrite source=" + source
                    + " requestedState=" + requestedState
                    + " reason=outside-entry-window"
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        args[stateIndex] = Display.STATE_DOZE;
        logAodStateRewrite(source, requestedState);
    }

    private static void logAodStateRewrite(String source, int requestedState) {
        String stateName = requestedState == Display.STATE_DOZE_SUSPEND
                ? "DOZE_SUSPEND"
                : "OFF";
        PixelAodLog.log("rewrote " + source + " " + stateName + "->DOZE during AOD entry"
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(null) + "}");
    }

    private static void inspectLockscreenClockCandidate(Object candidate, String source) {
        try {
            if (!(candidate instanceof View)) {
                PixelAodLog.log("lockscreen clock probe ignored non-view " + source
                        + " value=" + candidate);
                return;
            }
            View view = (View) candidate;
            String key = "lockscreenProbe|" + source + "|" + System.identityHashCode(view);
            synchronized (LOGGED_VIEW_TREE_KEYS) {
                if (!LOGGED_VIEW_TREE_KEYS.add(key)) {
                    return;
                }
            }
            StringBuilder builder = new StringBuilder("lockscreen clock probe from ")
                    .append(source)
                    .append(" view=").append(markerFor(view))
                    .append(" parentRoot=").append(markerFor(highestParentGroup(view)));
            if (PixelLockscreenClockView.shouldShowOnLockscreen(view.getContext())) {
                hideStockKeyguardClockViews(highestParentGroup(view));
                hideView(view, markerFor(view));
            }
            if (view instanceof ViewGroup) {
                appendViewTree(builder, view, 0, 4, 0);
            }
            logChunked(builder.toString());
        } catch (Throwable t) {
            PixelAodLog.log("failed to inspect lockscreen clock candidate " + source, t);
        }
    }

    private static void handleClockHost(Context context, ViewGroup host, String source) {
        try {
            ViewGroup pixelHost = findPixelClockInjectionHost(host);
            lastStockHost = new WeakReference<>(host);
            lastPixelHost = new WeakReference<>(pixelHost);
            injectPixelClock(context, pixelHost);
            injectPixelLockscreenClock(context, pixelHost);
            PixelLockscreenClockView.refreshAll(source);
            boolean screenOff = !PixelAodClockView.isDeviceInteractive(context);
            boolean lockscreenVisible = isLikelyLockscreenSurfaceVisible(context, host, pixelHost);
            PixelAodLog.log("AOD host snapshot source=" + source
                    + " screenOff=" + screenOff
                    + " lockscreenVisible=" + lockscreenVisible
                    + " stockHost=" + hostSummary(host)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            if (screenOff) {
                PixelAodClockView.noteScreenOffIfUnset(source + "#host-ready");
                PixelAodClockView.markRecentAodOverlayVisible(source + "#host-ready");
            }
            if (screenOff && !PixelAodClockView.shouldCustomizeAodNow(context)) {
                PixelAodClockView.setAodActive(true, source + "#host-ready");
            }
            boolean customizeNow = PixelAodClockView.shouldCustomizeAodNow(context);
            PixelAodLog.log("AOD host decision source=" + source
                    + " screenOff=" + screenOff
                    + " customizeNow=" + customizeNow
                    + " lockscreenVisible=" + lockscreenVisible
                    + " stockHost=" + hostSummary(host)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            if (screenOff || customizeNow) {
                refreshNotificationsFromLastListener(source);
                hideStockClockViews(host);
                adjustPluginStatusViews(context, host);
                if (ENABLE_EXPENSIVE_DEBUG_DUMPS && PixelAodLog.isDebugEnabled()) {
                    scheduleParentDebugDumps(host, source);
                }
            } else if (lockscreenVisible) {
                applyLockscreenClockReplacement(context, host, pixelHost, source);
            } else {
                restoreAdjustedStatusViews();
                restoreHiddenStockViews();
            }
            if (ENABLE_EXPENSIVE_DEBUG_REAPPLY && PixelAodLog.isDebugEnabled()) {
                scheduleReapply(context, host, pixelHost, source);
            }
            PixelAodLog.log("customized AOD clock host from " + source + " host="
                    + host.getClass().getName() + " pixelHost=" + markerFor(pixelHost)
                    + " hostChildren=" + host.getChildCount());
        } catch (Throwable t) {
            PixelAodLog.log("customize AOD clock host failed from " + source, t);
        }
    }

    private static void handleLockscreenHost(Context context, ViewGroup host, String source) {
        try {
            lastShadeHost = new WeakReference<>(host);
            boolean interactive = PixelAodClockView.isDeviceInteractive(context);
            boolean customizeNow = PixelAodClockView.shouldCustomizeAodNow(context);
            PixelAodLog.log("lockscreen host snapshot source=" + source
                    + " interactive=" + interactive
                    + " customizeNow=" + customizeNow
                    + " host=" + hostSummary(host)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            if (interactive && !customizeNow) {
                PixelAodClockView.hideAllAodOverlays(source + "#interactive-shade");
                if (!PixelLockscreenClockView.isSystemKeyguardLocked(context)) {
                    PixelLockscreenClockView.setLockscreenSurfaceVisible(false,
                            source + "#interactive-unlocked");
                    PixelLockscreenClockView.refreshAll(source + "#interactive-unlocked");
                    restoreAdjustedStatusViews();
                    restoreHiddenStockViews();
                    return;
                }
            }
            if (!shouldTouchLockscreenHost(host)) {
                PixelAodLog.log("skipped lockscreen host touch source=" + source
                        + " reason=throttled host=" + hostSummary(host)
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                return;
            }
            lastPixelHost = new WeakReference<>(host);
            if (lastStockHost.get() == null) {
                lastStockHost = new WeakReference<>(host);
            }
            disableClippingUpwards(host);
            injectPixelClock(context, host);
            injectPixelLockscreenClock(context, host);
            applyLockscreenClockReplacement(context, host, host, source);
            if (ENABLE_EXPENSIVE_DEBUG_REAPPLY && PixelAodLog.isDebugEnabled()) {
                scheduleLockscreenReapply(context, host);
            }
            PixelAodLog.log("prepared Pixel lockscreen host from " + source
                    + " host=" + markerFor(host)
                    + " children=" + host.getChildCount()
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("prepare Pixel lockscreen host failed from " + source, t);
        }
    }

    static void reapplyLockscreenClockFromKnownHost(String source) {
        if (!ENABLE_EXPENSIVE_DEBUG_REAPPLY) {
            PixelLockscreenClockView.refreshAll("known-host-" + source);
            return;
        }
        MAIN.post(() -> {
            try {
                ViewGroup pixelHost = lastPixelHost.get();
                ViewGroup stockHost = lastStockHost.get();
                ViewGroup shadeHost = lastShadeHost.get();
                ViewGroup host = pixelHost != null ? pixelHost : stockHost != null ? stockHost : shadeHost;
                if (host == null) {
                    return;
                }
                Context context = host.getContext();
                if (context == null || PixelAodClockView.shouldCustomizeAodNow(context)
                        || !PixelLockscreenClockView.isSystemKeyguardLocked(context)) {
                    return;
                }
                if (pixelHost == null && shadeHost != null) {
                    pixelHost = shadeHost;
                }
                applyLockscreenClockReplacement(context,
                        stockHost != null ? stockHost : host,
                        pixelHost != null ? pixelHost : host,
                        "known-host-" + source);
            } catch (Throwable t) {
                PixelAodLog.log("failed to reapply Pixel lockscreen clock from known host " + source, t);
            }
        });
    }

    static void refreshKnownAodHostVisibility(String source) {
        final String expectedTrace = PixelAodClockView.peekAodTraceId();
        MAIN.post(() -> {
            ViewGroup stockHost = lastStockHost.get();
            ViewGroup pixelHost = lastPixelHost.get();
            ViewGroup shadeHost = lastShadeHost.get();
            ViewGroup host = stockHost != null ? stockHost : pixelHost != null ? pixelHost : shadeHost;
            Context context = host != null ? host.getContext() : null;
            String currentTrace = PixelAodClockView.peekAodTraceId();
            String state = context != null ? PixelAodClockView.describeAodState(context) : "context=null";
            if (!TextUtils.isEmpty(expectedTrace)
                    && !TextUtils.equals(expectedTrace, currentTrace)) {
                PixelAodLog.log("skipped refreshing known AOD host visibility source=" + source
                        + " reason=trace-mismatch expectedTrace=" + expectedTrace
                        + " currentTrace=" + currentTrace
                        + " stockHost=" + hostSummary(stockHost)
                        + " pixelHost=" + hostSummary(pixelHost)
                        + " shadeHost=" + hostSummary(shadeHost)
                        + " state={" + state + "}");
                return;
            }
            PixelAodLog.log("refreshing known AOD host visibility source=" + source
                    + " stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " shadeHost=" + hostSummary(shadeHost)
                    + " host=" + hostSummary(host)
                    + " trace=" + currentTrace
                    + " state={" + state + "}");
            if (host == null || context == null) {
                return;
            }
            if (!PixelAodClockView.isAodActive()) {
                PixelAodLog.log("skipped refreshing known AOD host visibility source=" + source
                        + " reason=aod-inactive"
                        + " host=" + hostSummary(host)
                        + " trace=" + currentTrace
                        + " state={" + state + "}");
                return;
            }
            hideStockClockViews(host);
            hideStockKeyguardClockViews(highestParentGroup(host));
            adjustPluginStatusViews(context, host);
            PixelAodLog.log("refreshed known AOD host visibility source=" + source
                    + " host=" + hostSummary(host)
                    + " trace=" + currentTrace
                    + " state={" + state + "}");
        });
    }

    private static boolean shouldTouchLockscreenHost(ViewGroup host) {
        long now = android.os.SystemClock.uptimeMillis();
        synchronized (LOCKSCREEN_HOST_TOUCH_TIMES) {
            Long last = LOCKSCREEN_HOST_TOUCH_TIMES.get(host);
            if (last != null && now - last < 900L) {
                return false;
            }
            LOCKSCREEN_HOST_TOUCH_TIMES.put(host, now);
            return true;
        }
    }

    private static ViewGroup findPixelClockInjectionHost(ViewGroup pluginHost) {
        ViewGroup best = pluginHost;
        ViewParent parent = pluginHost.getParent();
        int depth = 0;
        while (parent instanceof ViewGroup && depth < 12) {
            ViewGroup group = (ViewGroup) parent;
            String marker = markerFor(group).toLowerCase(Locale.US);
            if (marker.contains("id/aod_off_layout")
                    || marker.contains("id/keyguard_style_clock")
                    || group.getWidth() >= pluginHost.getWidth()
                    && group.getHeight() > pluginHost.getHeight() + dp(pluginHost.getContext(), 240)) {
                best = group;
            }
            parent = group.getParent();
            depth++;
        }
        return best;
    }

    private static void injectPixelClock(Context context, ViewGroup host) {
        View existing = host.findViewWithTag(CUSTOM_TAG);
        if (existing instanceof PixelAodClockView) {
            existing.bringToFront();
            ((PixelAodClockView) existing).start();
            PixelAodLog.log("reused PixelAodClockView in " + host.getClass().getName()
                    + " visibility=" + existing.getVisibility());
            return;
        }

        PixelAodClockView clockView = new PixelAodClockView(context);
        clockView.setTag(CUSTOM_TAG);
        clockView.setPadding(0, 0, 0, 0);
        clockView.setElevation(dp(context, 24));
        clockView.setTranslationZ(dp(context, 24));
        disableClippingUpwards(host);
        host.addView(clockView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        clockView.bringToFront();
        clockView.start();
        PixelAodLog.log("injected PixelAodClockView into " + host.getClass().getName());
    }

    private static void injectPixelLockscreenClock(Context context, ViewGroup host) {
        View existing = host.findViewWithTag(LOCKSCREEN_CUSTOM_TAG);
        if (existing instanceof PixelLockscreenClockView) {
            existing.bringToFront();
            ((PixelLockscreenClockView) existing).start();
            return;
        }

        PixelLockscreenClockView clockView = new PixelLockscreenClockView(context);
        clockView.setTag(LOCKSCREEN_CUSTOM_TAG);
        clockView.setPadding(0, 0, 0, 0);
        clockView.setElevation(dp(context, 28));
        clockView.setTranslationZ(dp(context, 28));
        disableClippingUpwards(host);
        host.addView(clockView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        clockView.bringToFront();
        clockView.start();
        PixelAodLog.log("injected PixelLockscreenClockView into " + host.getClass().getName());
    }

    private static void scheduleReapply(Context context, ViewGroup stockHost, ViewGroup pixelHost,
            String source) {
        reapplyLater(context, stockHost, pixelHost, source, 650L);
        reapplyLater(context, stockHost, pixelHost, source, 2500L);
    }

    private static void scheduleStockSuppressionReapply(ViewGroup host, String source) {
        stockSuppressionReapplyLater(host, source, 1800L);
    }

    private static void stockSuppressionReapplyLater(ViewGroup host, String source, long delayMillis) {
        MAIN.postDelayed(() -> {
            try {
                if (host == null || PixelAodClockView.isDeviceInteractive(host.getContext())) {
                    return;
                }
                hideStockClockViews(host);
                adjustPluginStatusViews(host.getContext(), host);
                PixelAodLog.log("reapplied stock AOD suppression from " + source
                        + "+" + delayMillis + " children=" + host.getChildCount()
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(host.getContext()) + "}");
            } catch (Throwable t) {
                PixelAodLog.log("delayed stock AOD suppression failed", t);
            }
        }, delayMillis);
    }

    private static void scheduleLockscreenReapply(Context context, ViewGroup host) {
        lockscreenReapplyLater(context, host, 650L);
        lockscreenReapplyLater(context, host, 2000L);
    }

    private static void lockscreenReapplyLater(Context context, ViewGroup host, long delayMillis) {
        MAIN.postDelayed(() -> {
            try {
                if (PixelAodClockView.shouldCustomizeAodNow(context)
                        || !PixelAodClockView.isDeviceInteractive(context)) {
                    return;
                }
                applyLockscreenClockReplacement(context, host, host,
                        "NotificationShadeWindowView#delayed-" + delayMillis);
            } catch (Throwable t) {
                PixelAodLog.log("delayed lockscreen reapply failed", t);
            }
        }, delayMillis);
    }

    private static void reapplyLater(Context context, ViewGroup stockHost, ViewGroup pixelHost,
            String source, long delayMillis) {
        MAIN.postDelayed(() -> {
            try {
                boolean screenOff = !PixelAodClockView.isDeviceInteractive(context);
                boolean lockscreenVisible = isLikelyLockscreenSurfaceVisible(context, stockHost, pixelHost);
                PixelAodLog.log("delayed AOD reapply source=" + source
                        + " delayMillis=" + delayMillis
                        + " screenOff=" + screenOff
                        + " lockscreenVisible=" + lockscreenVisible
                        + " stockHost=" + hostSummary(stockHost)
                        + " pixelHost=" + hostSummary(pixelHost)
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                PixelLockscreenClockView.refreshAll("delayed-reapply");
                if (screenOff && !PixelAodClockView.shouldCustomizeAodNow(context)) {
                    PixelAodClockView.setAodActive(true, "delayed-host-ready");
                }
                if (!screenOff && !lockscreenVisible && !PixelAodClockView.shouldCustomizeAodNow(context)) {
                    PixelLockscreenClockView.setLockscreenSurfaceVisible(false, "delayed-reapply");
                    restoreAdjustedStatusViews();
                    restoreHiddenStockViews();
                    return;
                }
                if (lockscreenVisible && !screenOff) {
                    applyLockscreenClockReplacement(context, stockHost, pixelHost, "delayed-reapply");
                    return;
                }
                hideStockClockViews(stockHost);
                adjustPluginStatusViews(context, stockHost);
                View custom = pixelHost.findViewWithTag(CUSTOM_TAG);
                if (custom instanceof PixelAodClockView) {
                    custom.bringToFront();
                    ((PixelAodClockView) custom).start();
                } else {
                    injectPixelClock(context, pixelHost);
                }
            } catch (Throwable t) {
                PixelAodLog.log("delayed AOD reapply failed", t);
            }
        }, delayMillis);
    }

    private static void applyLockscreenClockReplacement(Context context, ViewGroup stockHost,
            ViewGroup pixelHost, String source) {
        boolean surfaceVisible = isLikelyLockscreenSurfaceVisible(context, stockHost, pixelHost);
        PixelLockscreenClockView.setLockscreenSurfaceVisible(surfaceVisible, source);
        if (!surfaceVisible) {
            PixelAodLog.log("lockscreen replacement skipped from " + source
                    + " surfaceVisible=false stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            restoreAdjustedStatusViews();
            restoreHiddenStockViews();
            return;
        }
        boolean hasNotificationCards = false;
        if (stockHost != null) {
            ViewGroup root = highestParentGroup(stockHost);
            hideStockKeyguardClockViews(root);
            hasNotificationCards |= hasVisibleLockscreenNotificationCards(root);
        }
        if (pixelHost != null) {
            ViewGroup root = highestParentGroup(pixelHost);
            hideStockKeyguardClockViews(root);
            hasNotificationCards |= hasVisibleLockscreenNotificationCards(root);
            View custom = pixelHost.findViewWithTag(LOCKSCREEN_CUSTOM_TAG);
            if (custom instanceof PixelLockscreenClockView) {
                custom.bringToFront();
                ((PixelLockscreenClockView) custom).start();
            } else {
                injectPixelLockscreenClock(context, pixelHost);
            }
        }
        PixelAodLog.log("lockscreen replacement applied from " + source
                + " surfaceVisible=" + surfaceVisible
                + " hasNotificationCards=" + hasNotificationCards
                + " stockHost=" + hostSummary(stockHost)
                + " pixelHost=" + hostSummary(pixelHost)
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(context) + "}");
        PixelLockscreenClockView.setVisibleLockscreenNotificationCards(hasNotificationCards, source);
    }

    private static boolean isLikelyLockscreenSurfaceVisible(Context context, ViewGroup stockHost,
            ViewGroup pixelHost) {
        return PixelLockscreenClockView.isSystemKeyguardLocked(context);
    }

    private static void adjustPluginStatusViews(Context context, ViewGroup root) {
        traverse(root, view -> {
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
                return false;
            }
            if (isSystemUiHeaderOrQsView(view)) {
                return false;
            }

            String marker = markerFor(view);
            if (looksLikeSystemAodMediaView(marker)) {
                return false;
            }

            if (looksLikePluginBatteryView(marker)) {
                hideView(view, marker);
                return false;
            }

            if (looksLikePluginNotificationView(marker)) {
                hookRuntimeNotificationView(view.getClass(), marker);
                boolean firstInspection;
                synchronized (INSPECTED_PLUGIN_NOTIFICATION_VIEWS) {
                    firstInspection = INSPECTED_PLUGIN_NOTIFICATION_VIEWS.add(view);
                }
                if (firstInspection) {
                    publishNotificationsFromView(view, marker);
                    if (ENABLE_NOTIFICATION_VIEW_REFLECTION_DUMP) {
                        logNotificationViewShape(view, marker);
                    }
                }
                hideView(view, marker);
                return false;
            }
            return true;
        });
    }

    private static void publishAtAGlanceExtraFromTree(ViewGroup root) {
        try {
            String extra = findAtAGlanceExtra(root);
            if (extra != null) {
                PixelAodClockView.setAtAGlanceExtra(extra);
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to publish At a Glance extra from stock AOD tree", t);
        }
    }

    private static String findAtAGlanceExtra(View view) {
        if (view == null || view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
            return null;
        }
        String marker = markerFor(view);
        String text = shortTextFor(view);
        String description = shortDescriptionFor(view);
        String candidate = chooseAtAGlanceCandidate(marker, text, description);
        if (candidate != null) {
            return candidate;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        int childCount = Math.min(group.getChildCount(), 80);
        for (int i = 0; i < childCount; i++) {
            String childCandidate = findAtAGlanceExtra(group.getChildAt(i));
            if (childCandidate != null) {
                return childCandidate;
            }
        }
        return null;
    }

    private static void publishNotificationsFromView(View view, String marker) {
        try {
            ArrayList<StatusBarNotification> notifications = new ArrayList<>();
            StringBuilder fieldSummary = new StringBuilder();
            appendNotificationsFromFields(view.getClass(), view, notifications, fieldSummary);
            if (!notifications.isEmpty()) {
                StatusBarNotification[] array = notifications.toArray(new StatusBarNotification[0]);
                StatusBarNotification[] snapshot = mergeCachedNotifications(array);
                if (lastNotificationListener != null) {
                    refreshNotificationsFromLastListener("runtime-NotificationView#merged-oplus-subset");
                } else {
                    PixelAodClockView.setActiveNotifications(snapshot);
                }
                PixelAodLog.log("merged AOD notifications from runtime view subset="
                        + array.length + " cache=" + snapshot.length
                        + " marker=" + marker + " fields=" + fieldSummary
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(null) + "}");
            } else {
                PixelAodLog.log("no StatusBarNotification objects found in runtime NotificationView "
                        + marker
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(null) + "}");
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to inspect runtime NotificationView notifications " + marker, t);
        }
    }

    private static void appendNotificationsFromFields(Class<?> clazz, Object owner,
            List<StatusBarNotification> out, StringBuilder fieldSummary) throws IllegalAccessException {
        Class<?> current = clazz;
        int fieldCount = 0;
        while (current != null && current != Object.class && fieldCount < 80) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (fieldCount++ >= 80) {
                    break;
                }
                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(owner);
                } catch (Throwable ignored) {
                    continue;
                }
                if (fieldSummary.length() < 1200) {
                    fieldSummary.append(field.getName())
                            .append('=')
                            .append(value == null ? "null" : value.getClass().getName());
                    if (value instanceof Collection) {
                        fieldSummary.append('#').append(((Collection<?>) value).size());
                    } else if (value instanceof Map) {
                        fieldSummary.append('#').append(((Map<?, ?>) value).size());
                    } else if (value != null && value.getClass().isArray()) {
                        fieldSummary.append('#').append(Array.getLength(value));
                    }
                    fieldSummary.append(';');
                }
                collectStatusBarNotifications(value, out, new HashSet<>(), 0);
            }
            current = current.getSuperclass();
        }
    }

    private static void collectStatusBarNotifications(Object value, List<StatusBarNotification> out,
            Set<Object> seen, int depth) {
        if (value == null || depth > 3 || seen.contains(value)) {
            return;
        }
        seen.add(value);
        if (value instanceof StatusBarNotification) {
            out.add((StatusBarNotification) value);
            return;
        }
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            int length = Math.min(Array.getLength(value), 32);
            for (int i = 0; i < length; i++) {
                collectStatusBarNotifications(Array.get(value, i), out, seen, depth + 1);
            }
            return;
        }
        if (value instanceof Iterable) {
            int emitted = 0;
            for (Object item : (Iterable<?>) value) {
                collectStatusBarNotifications(item, out, seen, depth + 1);
                if (++emitted >= 32) {
                    break;
                }
            }
            return;
        }
        if (value instanceof Map) {
            int emitted = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                collectStatusBarNotifications(entry.getKey(), out, seen, depth + 1);
                collectStatusBarNotifications(entry.getValue(), out, seen, depth + 1);
                if (++emitted >= 32) {
                    break;
                }
            }
            return;
        }
        String className = clazz.getName();
        if (depth < 3 && (className.contains("Notification") || className.contains("notification")
                || className.startsWith("com.oplus") || className.startsWith("com.android.systemui"))) {
            Field[] fields = clazz.getDeclaredFields();
            int emitted = 0;
            for (Field field : fields) {
                if (emitted++ >= 24) {
                    break;
                }
                try {
                    field.setAccessible(true);
                    collectStatusBarNotifications(field.get(value), out, seen, depth + 1);
                } catch (Throwable ignored) {
                    // Best-effort field scan.
                }
            }
        }
    }

    private static void movePluginNotificationView(Context context, View notificationView, String marker) {
        View moveTarget = notificationView;
        rememberAdjustedState(moveTarget);
        float targetX = dp(context, STATUS_EDGE_DP);
        float targetY = dp(context, PixelAodClockView.notificationLineTopDp());
        moveTarget.setTranslationX(targetX - moveTarget.getX());
        moveTarget.setTranslationY(targetY - moveTarget.getY());
        moveTarget.setAlpha(0.92f);
        moveTarget.bringToFront();
        PixelAodLog.log("moved AOD notification view " + marker
                + " targetX=" + Math.round(targetX)
                + " targetY=" + Math.round(targetY)
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(context) + "}");
    }

    private static void logNotificationViewShape(View view, String marker) {
        String key = "inspect|" + view.getClass().getName();
        if (!LOGGED_INSPECTION_CLASSES.add(key)) {
            return;
        }
        try {
            StringBuilder builder = new StringBuilder("NotificationView reflection ").append(marker);
            builder.append(" fields=");
            java.lang.reflect.Field[] fields = view.getClass().getDeclaredFields();
            for (int i = 0; i < Math.min(fields.length, 24); i++) {
                java.lang.reflect.Field field = fields[i];
                builder.append(field.getType().getSimpleName()).append(' ').append(field.getName()).append(';');
            }
            builder.append(" methods=");
            java.lang.reflect.Method[] methods = view.getClass().getDeclaredMethods();
            for (int i = 0; i < Math.min(methods.length, 32); i++) {
                java.lang.reflect.Method method = methods[i];
                builder.append(method.getName()).append('(');
                Class<?>[] types = method.getParameterTypes();
                for (int j = 0; j < types.length; j++) {
                    if (j > 0) {
                        builder.append(',');
                    }
                    builder.append(types[j].getSimpleName());
                }
                builder.append(");");
            }
            PixelAodLog.log(builder.toString());
        } catch (Throwable t) {
            PixelAodLog.log("failed to inspect NotificationView " + marker, t);
        }
    }

    private static void disableClippingUpwards(View view) {
        View current = view;
        int depth = 0;
        while (current instanceof ViewGroup && depth < 8) {
            ViewGroup group = (ViewGroup) current;
            group.setClipChildren(false);
            group.setClipToPadding(false);
            ViewParent parent = group.getParent();
            current = parent instanceof View ? (View) parent : null;
            depth++;
        }
    }

    private static void rememberAdjustedState(View view) {
        synchronized (ADJUSTED_STATUS_VIEWS) {
            if (!ADJUSTED_STATUS_VIEWS.containsKey(view)) {
                ADJUSTED_STATUS_VIEWS.put(view, new AdjustedState(
                        view.getTranslationX(),
                        view.getTranslationY(),
                        view.getTranslationZ(),
                        view.getAlpha(),
                        view.getLayerType()));
            }
        }
    }

    private static void scheduleDebugDumps(ViewGroup root, String source) {
        if (!ENABLE_EXPENSIVE_DEBUG_DUMPS || !PixelAodLog.isDebugEnabled()) {
            return;
        }
        debugDumpLater(root, source, 120L);
        debugDumpLater(root, source, 1200L);
    }

    private static void scheduleParentDebugDumps(ViewGroup root, String source) {
        if (!ENABLE_EXPENSIVE_DEBUG_DUMPS || !PixelAodLog.isDebugEnabled()) {
            return;
        }
        parentDebugDumpLater(root, source, 1600L);
        parentDebugDumpLater(root, source, 3600L);
    }

    private static void parentDebugDumpLater(ViewGroup root, String source, long delayMillis) {
        MAIN.postDelayed(() -> {
            try {
                if (!PixelAodClockView.shouldCustomizeAodNow(root.getContext())) {
                    if (PixelLockscreenClockView.shouldShowOnLockscreen(root.getContext())) {
                        applyLockscreenClockReplacement(root.getContext(), root, lastPixelHost.get(),
                                source + "+parent-dump");
                    } else {
                        restoreAdjustedStatusViews();
                        restoreHiddenStockViews();
                    }
                    return;
                }
                ViewGroup parentRoot = highestParentGroup(root);
                if (parentRoot == root) {
                    PixelAodLog.log("AOD parent dump skipped from " + source
                            + " because no ViewGroup parent is attached for " + root.getClass().getName());
                    return;
                }
                hideStockKeyguardClockViews(parentRoot);
            } catch (Throwable t) {
                PixelAodLog.log("AOD parent tree dump failed from " + source, t);
            }
        }, delayMillis);
    }

    private static void applyLockscreenClockReplacementFromLastHosts(String source) {
        ViewGroup stockHost = lastStockHost.get();
        ViewGroup pixelHost = lastPixelHost.get();
        Context context = pixelHost != null ? pixelHost.getContext()
                : stockHost != null ? stockHost.getContext() : null;
        if (context == null) {
            PixelLockscreenClockView.refreshAll(source);
            PixelAodLog.log("lockscreen replacement skipped without remembered hosts from " + source
                    + " stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        applyLockscreenClockReplacement(context, stockHost, pixelHost, source);
    }

    static void suppressSystemAodDuringLockscreenTransition(String source) {
        runAtFrontOfMain(() -> {
            PixelLockscreenClockView.refreshAll(source);
            ViewGroup stockHost = lastStockHost.get();
            ViewGroup pixelHost = lastPixelHost.get();
            Context context = pixelHost != null ? pixelHost.getContext()
                    : stockHost != null ? stockHost.getContext() : null;
            PixelAodLog.log("suppressing system AOD during lockscreen transition source=" + source
                    + " stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            if (stockHost != null) {
                hideStockClockViews(stockHost);
                hideStockKeyguardClockViews(highestParentGroup(stockHost));
            }
            if (pixelHost != null) {
                hideStockKeyguardClockViews(highestParentGroup(pixelHost));
            }
        });
    }

    private static void restoreHiddenStockViewsAfterTransition(String source, String expectedTrace) {
        MAIN.postDelayed(() -> {
            ViewGroup pixelHost = lastPixelHost.get();
            ViewGroup stockHost = lastStockHost.get();
            String currentTrace = PixelAodClockView.peekAodTraceId();
            if (!TextUtils.isEmpty(expectedTrace)
                    && !TextUtils.equals(expectedTrace, currentTrace)) {
                Context context = pixelHost != null ? pixelHost.getContext()
                        : stockHost != null ? stockHost.getContext() : null;
                String state = context != null ? PixelAodClockView.describeAodState(context)
                        : "context=null";
                PixelAodLog.log("skipped restoring stock AOD/keyguard views after transition from "
                        + source + " reason=trace-mismatch expectedTrace=" + expectedTrace
                        + " currentTrace=" + currentTrace
                        + " stockHost=" + hostSummary(stockHost)
                        + " pixelHost=" + hostSummary(pixelHost)
                        + " state={" + state + "}");
                return;
            }
            Context context = null;
            if (pixelHost != null) {
                context = pixelHost.getContext();
            } else if (stockHost != null) {
                context = stockHost.getContext();
            }
            if (PixelLockscreenClockView.shouldShowOnLockscreen(context)
                    || PixelAodClockView.shouldCustomizeAodNow(context)) {
                PixelAodLog.log("kept stock AOD/keyguard views hidden after transition from "
                        + source + " stockHost=" + hostSummary(stockHost)
                        + " pixelHost=" + hostSummary(pixelHost)
                        + " trace=" + currentTrace
                        + " expectedTrace=" + expectedTrace
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            PixelAodLog.log("restoring stock AOD/keyguard views after transition from " + source
                    + " stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + currentTrace
                    + " expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            restoreHiddenStockViews();
        }, 900L);
    }

    private static void runAtFrontOfMain(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN.postAtFrontOfQueue(runnable);
        }
    }

    private static void debugDumpLater(ViewGroup root, String source, long delayMillis) {
        MAIN.postDelayed(() -> {
            try {
                logViewTree(root, source + "+" + delayMillis + "ms");
            } catch (Throwable t) {
                PixelAodLog.log("AOD view tree dump failed from " + source, t);
            }
        }, delayMillis);
    }

    private static void logViewTree(ViewGroup root, String source) {
        String key = source + "|" + root.getClass().getName() + "|"
                + System.identityHashCode(root);
        synchronized (LOGGED_VIEW_TREE_KEYS) {
            if (!LOGGED_VIEW_TREE_KEYS.add(key)) {
                return;
            }
        }
        StringBuilder builder = new StringBuilder("AOD visible tree from ").append(source)
                .append(" root=").append(markerFor(root));
        appendViewTree(builder, root, 0, 8, 0);
        logChunked(builder.toString());
    }

    private static ViewGroup highestParentGroup(View view) {
        ViewGroup highest = view instanceof ViewGroup ? (ViewGroup) view : null;
        ViewParent parent = view.getParent();
        int depth = 0;
        while (parent instanceof ViewGroup && depth < 12) {
            highest = (ViewGroup) parent;
            parent = ((ViewGroup) parent).getParent();
            depth++;
        }
        return highest != null ? highest : (ViewGroup) view;
    }

    private static int appendViewTree(StringBuilder builder, View view, int depth, int maxDepth, int emitted) {
        if (emitted > 220) {
            return emitted;
        }
        builder.append('\n');
        for (int i = 0; i < depth; i++) {
            builder.append("  ");
        }
        builder.append(markerFor(view))
                .append(" visibility=").append(view.getVisibility())
                .append(" alpha=").append(view.getAlpha())
                .append(" shown=").append(view.isShown())
                .append(" size=").append(view.getWidth()).append('x').append(view.getHeight())
                .append(" xy=").append(Math.round(view.getX())).append(',').append(Math.round(view.getY()))
                .append(" trans=").append(Math.round(view.getTranslationX())).append(',')
                .append(Math.round(view.getTranslationY())).append(',')
                .append(Math.round(view.getTranslationZ()))
                .append(" screen=").append(screenLocationFor(view))
                .append(textMarkerFor(view));
        emitted++;
        if (depth >= maxDepth || !(view instanceof ViewGroup)) {
            return emitted;
        }
        ViewGroup group = (ViewGroup) view;
        int childCount = Math.min(group.getChildCount(), 40);
        for (int i = 0; i < childCount; i++) {
            emitted = appendViewTree(builder, group.getChildAt(i), depth + 1, maxDepth, emitted);
        }
        if (group.getChildCount() > childCount) {
            builder.append('\n');
            for (int i = 0; i <= depth; i++) {
                builder.append("  ");
            }
            builder.append("... ").append(group.getChildCount() - childCount).append(" more children");
        }
        return emitted;
    }

    private static void logChunked(String message) {
        int max = 3200;
        for (int start = 0; start < message.length(); start += max) {
            int end = Math.min(message.length(), start + max);
            PixelAodLog.log(message.substring(start, end));
        }
    }

    private static void hideStockClockViews(ViewGroup root) {
        final int[] stats = new int[2];
        traverse(root, view -> {
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
                return false;
            }
            if (isSystemUiHeaderOrQsView(view)) {
                return false;
            }

            String marker = markerFor(view);
            if (looksLikeSystemAodMediaView(marker)) {
                return false;
            }

            if (view instanceof ViewGroup) {
                if (looksLikeStockAodClockContainer(marker)
                        || looksLikeGenericStockAodVisual(marker, view)) {
                    if (containsSystemAodMediaView((ViewGroup) view)) {
                        stats[1]++;
                        PixelAodLog.log("preserved stock AOD container with media subtree " + marker
                                + " trace=" + PixelAodClockView.currentAodTraceId()
                                + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                        return true;
                    } else {
                        stats[0]++;
                        hideView(view, marker);
                        return false;
                    }
                }
                if (looksLikeStockAodWeatherOrExtra(marker, null)) {
                    stats[0]++;
                    hideView(view, marker);
                    return false;
                }
                return true;
            }

            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                if (looksLikeStockAodText(marker, textView.getText())
                        || looksLikeStockAodWeatherOrExtra(marker, textView.getText())
                        || looksLikeGenericStockAodVisual(marker, textView)) {
                    stats[0]++;
                    hideView(textView, marker);
                }
                return true;
            }

            if (looksLikeStockAodClockLeaf(marker)
                    || looksLikeStockAodWeatherOrExtra(marker, null)
                    || looksLikeGenericStockAodVisual(marker, view)) {
                stats[0]++;
                hideView(view, marker);
            }
            return true;
        });
        PixelAodLog.log("stock AOD hide pass root=" + markerFor(root)
                + " hidden=" + stats[0]
                + " preservedMediaSubtree=" + stats[1]
                + " children=" + root.getChildCount()
                + " state={" + PixelAodClockView.describeAodState(root.getContext()) + "}");
    }

    private static void hideStockKeyguardClockViews(ViewGroup root) {
        final int[] stats = new int[2];
        traverse(root, view -> {
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
                return false;
            }
            if (isSystemUiHeaderOrQsView(view)) {
                return false;
            }

            String marker = markerFor(view);
            if (looksLikeSystemAodMediaView(marker)) {
                return false;
            }

            if (view instanceof ViewGroup
                    && containsPixelLockscreenClock((ViewGroup) view)) {
                stats[1]++;
                return true;
            }

            if (looksLikeOplusKeyguardBigClock(marker)
                    || isStockKeyguardClockDrawCandidate(marker, view)) {
                stats[0]++;
                hideView(view, marker);
                return false;
            }
            return true;
        });
        PixelAodLog.log("stock keyguard hide pass root=" + markerFor(root)
                + " hidden=" + stats[0]
                + " preservedPixelClockSubtree=" + stats[1]
                + " children=" + root.getChildCount()
                + " state={" + PixelAodClockView.describeAodState(root.getContext()) + "}");
    }

    private static boolean hasVisibleLockscreenNotificationCards(ViewGroup root) {
        if (root == null) {
            return false;
        }
        final boolean[] found = {false};
        traverse(root, view -> {
            if (found[0]) {
                return false;
            }
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
                return false;
            }
            if (!view.isShown()) {
                return false;
            }
            String marker = markerFor(view);
            if (looksLikeVisibleLockscreenNotificationCard(marker, view)) {
                found[0] = true;
                PixelAodLog.log("detected visible lockscreen notification card " + marker
                        + " size=" + view.getWidth() + "x" + view.getHeight()
                        + " screen=" + screenLocationFor(view)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                return false;
            }
            return true;
        });
        return found[0];
    }

    static boolean hasExpandedSystemNotificationShadeContent(ViewGroup root) {
        if (root == null || !root.isShown()) {
            return false;
        }
        return hasVisibleShadeDismissButton(root);
    }

    static boolean hasVisibleLockscreenNotificationCardsIn(ViewGroup root) {
        return hasVisibleLockscreenNotificationCards(root);
    }

    static boolean hasExpandedLockscreenNotificationContentIn(ViewGroup root) {
        if (root == null || !root.isShown()) {
            return false;
        }
        final boolean[] found = {false};
        traverse(root, view -> {
            if (found[0]) {
                return false;
            }
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView
                    || !view.isShown()) {
                return false;
            }
            if (isInsideMediaNotificationSurface(view)) {
                return false;
            }
            if (looksLikeExpandedNotificationControl(view)
                    || looksLikeExpandedNotificationCard(markerFor(view), view)) {
                found[0] = true;
                PixelAodLog.log("detected expanded lockscreen notification content "
                        + markerFor(view) + " size=" + view.getWidth() + "x" + view.getHeight()
                        + " screen=" + screenLocationFor(view)
                        + textMarkerFor(view)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                return false;
            }
            return true;
        });
        return found[0];
    }

    static boolean hasVisibleKeyguardBouncer(ViewGroup root) {
        if (root == null || !root.isShown()) {
            return false;
        }
        final boolean[] found = {false};
        traverse(root, view -> {
            if (found[0]) {
                return false;
            }
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView
                    || !view.isShown()) {
                return false;
            }
            String marker = markerFor(view).toLowerCase(Locale.US);
            if (looksLikeVisibleKeyguardBouncer(marker, view)) {
                found[0] = true;
                PixelAodLog.log("detected visible keyguard bouncer " + marker
                        + " size=" + view.getWidth() + "x" + view.getHeight()
                        + " screen=" + screenLocationFor(view)
                        + textMarkerFor(view)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                return false;
            }
            return true;
        });
        return found[0];
    }

    private static boolean hasVisibleShadeDismissButton(ViewGroup root) {
        final boolean[] found = {false};
        traverse(root, view -> {
            if (found[0]) {
                return false;
            }
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
                return false;
            }
            if (!view.isShown() || view.getWidth() < dp(view.getContext(), 36)
                    || view.getHeight() < dp(view.getContext(), 36)) {
                return true;
            }
            String marker = markerFor(view).toLowerCase(Locale.US);
            if ((marker.contains("dismiss") || marker.contains("clear_all")
                    || marker.contains("clearall"))
                    && marker.contains("notification")) {
                found[0] = true;
                PixelAodLog.log("detected expanded notification shade dismiss control "
                        + marker + " size=" + view.getWidth() + "x" + view.getHeight()
                        + " screen=" + screenLocationFor(view)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                return false;
            }
            return true;
        });
        return found[0];
    }

    private static boolean looksLikeExpandedNotificationControl(View view) {
        String text = shortTextFor(view);
        if (text == null) {
            text = shortDescriptionFor(view);
        }
        if (text == null) {
            return false;
        }
        String normalized = text.trim().replace(" ", "").toLowerCase(Locale.US);
        return normalized.equals("showless")
                || normalized.equals("收起")
                || normalized.equals("折叠")
                || normalized.contains("showless")
                || normalized.contains("collapsenotifications")
                || normalized.contains("lessnotifications");
    }

    private static boolean looksLikeExpandedNotificationCard(String marker, View view) {
        if (looksLikeMediaNotificationSurface(marker)) {
            return false;
        }
        if (!looksLikeVisibleLockscreenNotificationCard(marker, view)) {
            return false;
        }
        int[] location = new int[2];
        try {
            view.getLocationOnScreen(location);
        } catch (Throwable ignored) {
            location[1] = Integer.MAX_VALUE;
        }
        int minExpandedHeight = dp(view.getContext(), 128);
        int topOverlapLimit = dp(view.getContext(), 430);
        return view.getHeight() >= minExpandedHeight && location[1] < topOverlapLimit;
    }

    private static boolean looksLikeMediaNotificationSurface(String marker) {
        String m = marker.toLowerCase(Locale.US);
        return m.contains("qsmediaplayer")
                || m.contains("mediacarousel")
                || m.contains("oplusmedia")
                || m.contains("keyguardmedia")
                || m.contains("seedling")
                || m.contains("fluid")
                || m.contains("capsule")
                || m.contains("livealert")
                || m.contains("mediahostview")
                || m.contains("media_carousel")
                || m.contains("media_container")
                || m.contains("media_view")
                || m.contains("mediaoutput")
                || m.contains("playback")
                || m.contains("album")
                || m.contains("artwork")
                || m.contains("nowplaying");
    }

    private static boolean isInsideMediaNotificationSurface(View view) {
        View current = view;
        int depth = 0;
        while (current != null && depth < 8) {
            if (looksLikeMediaNotificationSurface(markerFor(current))) {
                return true;
            }
            Object parent = current.getParent();
            if (!(parent instanceof View)) {
                return false;
            }
            current = (View) parent;
            depth++;
        }
        return false;
    }

    private static boolean containsVisibleStockKeyguardClock(ViewGroup root) {
        if (root == null) {
            return false;
        }
        final boolean[] found = {false};
        traverse(root, view -> {
            if (found[0]) {
                return false;
            }
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView) {
                return false;
            }
            if (isSystemUiHeaderOrQsView(view)) {
                return false;
            }
            if (view.getVisibility() != View.VISIBLE) {
                return false;
            }
            String marker = markerFor(view);
            if (looksLikeOplusKeyguardBigClock(marker)) {
                found[0] = true;
                PixelAodLog.log("detected stock keyguard clock " + marker
                        + " visibility=" + view.getVisibility()
                        + " shown=" + view.isShown()
                        + " size=" + view.getWidth() + "x" + view.getHeight()
                        + " screen=" + screenLocationFor(view)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                return false;
            }
            return true;
        });
        return found[0];
    }

    private static boolean containsVisibleLockscreenSurfaceChrome(ViewGroup root) {
        if (root == null || !root.isShown()) {
            return false;
        }
        final boolean[] found = {false};
        traverse(root, view -> {
            if (found[0]) {
                return false;
            }
            if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView
                    || !view.isShown()) {
                return false;
            }
            String marker = markerFor(view).toLowerCase(Locale.US);
            if (looksLikeVisibleLockscreenSurfaceChrome(marker, view)) {
                found[0] = true;
                PixelAodLog.log("detected lockscreen surface chrome " + marker
                        + " size=" + view.getWidth() + "x" + view.getHeight()
                        + " screen=" + screenLocationFor(view)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
                return false;
            }
            return true;
        });
        return found[0];
    }

    private static boolean looksLikeVisibleLockscreenSurfaceChrome(String marker, View view) {
        if (marker.contains("notificationshadewindowview")
                || marker.contains("notificationpanel")
                || marker.contains("notificationstackscrolllayout")
                || marker.contains("notification_stack_scroller")
                || marker.contains("statusbar")
                || marker.contains("status_bar")
                || marker.contains("quicksettings")
                || marker.contains("qs")) {
            return false;
        }
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return false;
        }
        return marker.contains("keyguard")
                || marker.contains("lock_icon")
                || marker.contains("lockicon")
                || marker.contains("bottomaffordance")
                || marker.contains("bottom_affordance")
                || marker.contains("quickaffordance")
                || marker.contains("keyguardmedia");
    }

    private static boolean containsPixelLockscreenClock(ViewGroup root) {
        if (root == null) {
            return false;
        }
        int childCount = Math.min(root.getChildCount(), 120);
        for (int i = 0; i < childCount; i++) {
            View child = root.getChildAt(i);
            if (child instanceof PixelLockscreenClockView) {
                return true;
            }
            if (child instanceof ViewGroup && containsPixelLockscreenClock((ViewGroup) child)) {
                return true;
            }
        }
        return false;
    }

    private static void hideView(View view, String marker) {
        if (looksLikeSystemAodMediaView(marker)) {
            PixelAodLog.log("preserved system AOD media view " + marker
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
            return;
        }
        boolean firstHide = false;
        synchronized (HIDDEN_STOCK_VIEWS) {
            if (!HIDDEN_STOCK_VIEWS.containsKey(view)) {
                HIDDEN_STOCK_VIEWS.put(view, new HiddenState(view.getVisibility(), view.getAlpha()));
                firstHide = true;
            }
        }
        view.setAlpha(0f);
        view.setVisibility(View.GONE);
        if (firstHide) {
            PixelAodLog.log("hid stock AOD view " + marker);
        }
    }

    static void restoreSystemViewsForLockscreen(String source) {
        MAIN.post(() -> {
            PixelLockscreenClockView.refreshAll(source);
            restoreAdjustedStatusViews();
            if (PixelLockscreenClockView.shouldShowOnKnownContext()) {
                PixelAodLog.log("kept stock keyguard clock hidden for Pixel lockscreen from " + source);
                return;
            }
            restoreHiddenStockViews();
            PixelAodLog.log("restored system lockscreen views from " + source);
        });
    }

    private static void restoreHiddenStockViews() {
        synchronized (HIDDEN_STOCK_VIEWS) {
            for (Map.Entry<View, HiddenState> entry : HIDDEN_STOCK_VIEWS.entrySet()) {
                View view = entry.getKey();
                HiddenState state = entry.getValue();
                if (view != null && state != null) {
                    try {
                        view.setVisibility(state.visibility);
                        view.setAlpha(state.alpha);
                    } catch (Throwable t) {
                        PixelAodLog.log("restore hidden stock AOD view failed", t);
                    }
                }
            }
            HIDDEN_STOCK_VIEWS.clear();
        }
        PixelAodLog.log("restored hidden stock AOD views");
    }

    private static void restoreAdjustedStatusViews() {
        synchronized (ADJUSTED_STATUS_VIEWS) {
            for (Map.Entry<View, AdjustedState> entry : ADJUSTED_STATUS_VIEWS.entrySet()) {
                View view = entry.getKey();
                AdjustedState state = entry.getValue();
                if (view != null && state != null) {
                    try {
                        view.setTranslationX(state.translationX);
                        view.setTranslationY(state.translationY);
                        view.setTranslationZ(state.translationZ);
                        view.setAlpha(state.alpha);
                        view.setLayerType(state.layerType, null);
                    } catch (Throwable t) {
                        PixelAodLog.log("restore adjusted AOD status view failed", t);
                    }
                }
            }
            ADJUSTED_STATUS_VIEWS.clear();
        }
        PixelAodLog.log("restored adjusted AOD status views");
    }

    private static boolean looksLikeStockAodClockContainer(String marker) {
        String m = marker.toLowerCase(Locale.US);
        if (m.startsWith("com.oplus.aodimpl.aodrootlayout")
                || looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")) {
            return false;
        }
        return m.contains("aodscenemusicdefaulttimeviewgroup")
                || m.contains("timeviewgroup")
                || m.contains("clockviewgroup")
                || m.contains("keyguardstatusview")
                || m.contains("keyguardclockswitch")
                || m.contains("keyguard_status_view")
                || m.contains("keyguard_clock_switch")
                || m.contains("date")
                || m.contains("weather")
                || m.contains("temperature")
                || m.contains("temp")
                || m.contains("datemessage")
                || m.contains("date_message");
    }

    private static boolean looksLikeGenericStockAodVisual(String marker, View view) {
        if (view == null) {
            return false;
        }
        String m = marker.toLowerCase(Locale.US);
        if (view instanceof PixelAodClockView || view instanceof PixelLockscreenClockView
                || looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif")
                || m.contains("battery") || m.contains("charging")
                || m.contains("media") || m.contains("music")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")) {
            return false;
        }
        if (!m.contains("aod") && !hasAodAncestor(view)) {
            return false;
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CharSequence text = textView.getText();
            if (looksLikeStockAodText(marker, text)
                    || looksLikeStockAodWeatherOrExtra(marker, text)) {
                return true;
            }
            float sp = textView.getTextSize() / textView.getResources().getDisplayMetrics().scaledDensity;
            return sp >= 48f;
        }
        int screenWidth = view.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = view.getResources().getDisplayMetrics().heightPixels;
        int width = view.getWidth();
        int height = view.getHeight();
        return width >= Math.round(screenWidth * 0.28f)
                && height >= Math.round(screenHeight * 0.06f)
                && height <= Math.round(screenHeight * 0.45f)
                && (m.contains("time") || m.contains("clock") || m.contains("date")
                || m.contains("weather") || m.contains("temp") || m.contains("view"));
    }

    private static boolean hasAodAncestor(View view) {
        View current = view;
        int depth = 0;
        while (current != null && depth < 10) {
            String marker = markerFor(current).toLowerCase(Locale.US);
            if (marker.contains("aodrootlayout")
                    || marker.contains("aodclock")
                    || marker.contains("aod_off_layout")
                    || marker.contains("com.oplus.egview")
                    || marker.contains("com.oplus.aod")) {
                return true;
            }
            Object parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
            depth++;
        }
        return false;
    }

    private static boolean looksLikeOplusKeyguardBigClock(String marker) {
        String m = marker.toLowerCase(Locale.US);
        if (looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif")
                || m.contains("media") || m.contains("music")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")
                || m.contains("bouncer") || m.contains("emergency")
                || m.contains("bottomaffordance") || m.contains("bottom_affordance")
                || m.contains("camera") || m.contains("flashlight") || m.contains("quickaffordance")
                || m.contains("carrier") || m.contains("quicksettings") || m.contains("quick_settings")
                || m.contains("statusbar") || m.contains("status_bar")) {
            return false;
        }
        return m.contains("customopluskeyguardstyleclock")
                || m.contains("com.oplus.keyguard.clock.big.")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.clockviewroot")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.clocktimeview")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.datemessageview")
                || m.contains("keyguard_style_clock")
                || m.contains("clockviewroot")
                || m.contains("clock_time_view")
                || m.contains("date_message_view")
                || m.contains("com.oplus.keyguard.personality.clocks:id/clock_time_parent")
                || m.contains("com.oplus.keyguard.personality.clocks:id/date_message_view_parent")
                || (m.contains("customoplus") && (m.contains("clock") || m.contains("time") || m.contains("date")));
    }

    private static boolean isStockKeyguardClockDrawCandidate(String marker, View view) {
        if (view instanceof ViewGroup) {
            return looksLikeOplusKeyguardClockContainer(marker);
        }
        if (view instanceof TextView) {
            return looksLikeOplusKeyguardClockText(marker);
        }
        return false;
    }

    private static boolean looksLikeOplusKeyguardClockContainer(String marker) {
        String m = marker.toLowerCase(Locale.US);
        if (looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif")
                || m.contains("media") || m.contains("music")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")
                || m.contains("bouncer") || m.contains("emergency")
                || m.contains("bottomaffordance") || m.contains("bottom_affordance")
                || m.contains("camera") || m.contains("flashlight") || m.contains("quickaffordance")
                || m.contains("carrier") || m.contains("quicksettings")
                || m.contains("statusbar") || m.contains("status_bar")) {
            return false;
        }
        return m.contains("customopluskeyguardstyleclock")
                || m.contains("keyguard_style_clock")
                || m.contains("clockviewroot")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.clocktimeview")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.bigclockdigitaltimeview")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.datemessageview")
                || m.contains("com.oplus.keyguard.clock.big.ui.view.extramessageview")
                || m.contains("com.oplus.keyguard.personality.clocks:id/clock_time_parent")
                || m.contains("com.oplus.keyguard.personality.clocks:id/date_message_view_parent");
    }

    private static boolean looksLikeOplusKeyguardClockText(String marker) {
        String m = marker.toLowerCase(Locale.US);
        return m.contains("com.oplus.keyguard.personality.clocks:id/visible_digital_time_view")
                || m.contains("com.oplus.keyguard.personality.clocks:id/invisible_digital_time_view");
    }

    private static boolean looksLikeLockscreenNotificationCard(String marker, View view) {
        String m = marker.toLowerCase(Locale.US);
        if (m.contains("qsmediaplayer")
                || m.contains("mediacarousel")
                || m.contains("oplusmedia")
                || m.contains("keyguardmedia")
                || m.contains("mediahostview")
                || m.contains("media_carousel")
                || m.contains("media_container")
                || m.contains("media_view")) {
            return true;
        }
        if (!m.contains("notification") && !m.contains("expandable")) {
            return false;
        }
        if (m.contains("notificationshadewindowview")
                || m.contains("notificationpanel")
                || m.contains("notificationstackscrolllayout")
                || m.contains("notification_stack_scroller")
                || m.contains("notificationicons")
                || m.contains("notification_icon")
                || m.contains("iconshelf")
                || m.contains("statusbar")
                || m.contains("status_bar")
                || m.contains("scrim")
                || m.contains("qs")) {
            return false;
        }
        int minWidth = dp(view.getContext(), 160);
        int minHeight = dp(view.getContext(), 44);
        if (view.getWidth() < minWidth || view.getHeight() < minHeight) {
            return false;
        }
        return m.contains("expandablenotificationrow")
                || m.contains("notificationrow")
                || m.contains("notification_row")
                || m.contains("notificationcontent")
                || m.contains("notification_content")
                || m.contains("notificationentry")
                || m.contains("notification_entry")
                || m.contains("notificationcard")
                || m.contains("notification_card")
                || m.contains("notificationmain")
                || m.contains("notification_main")
                || m.contains("latest_event_content")
                || m.contains("com.oplus.systemui.notification")
                || m.contains("com.android.systemui:id/notification");
    }

    private static boolean looksLikeVisibleLockscreenNotificationCard(String marker, View view) {
        return looksLikeLockscreenNotificationCard(marker, view)
                || looksLikeOplusLockscreenNotificationCardByContent(marker, view)
                || isNonEmptySeedling(marker, view);
    }

    private static boolean isNonEmptySeedling(String marker, View view) {
        String m = marker.toLowerCase(Locale.US);
        if (m.contains("seedling") || m.contains("fluid") || m.contains("capsule")) {
            if (view.getVisibility() == View.VISIBLE && view instanceof ViewGroup) {
                NotificationTextSignals signals = new NotificationTextSignals();
                collectNotificationTextSignals(view, 0, signals);
                // A media seedling or timer seedling will have some meaningful text (e.g. song name, timer)
                // Even if relativeTime is false, meaningfulTextCount > 0 will catch it.
                return signals.meaningfulTextCount > 0 || signals.clockTime || signals.relativeTime;
            }
        }
        return false;
    }

    private static boolean looksLikeOplusLockscreenNotificationCardByContent(String marker, View view) {
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        String m = marker.toLowerCase(Locale.US);
        if (m.contains("notificationshadewindowview")
                || m.contains("notificationpanel")
                || m.contains("notificationstackscrolllayout")
                || m.contains("notification_stack_scroller")
                || m.contains("notificationicons")
                || m.contains("notification_icon")
                || m.contains("iconshelf")
                || m.contains("statusbar")
                || m.contains("status_bar")
                || m.contains("keyguardstatus")
                || m.contains("status_view")
                || m.contains("clock")
                || m.contains("date")
                || m.contains("qs")
                || m.contains("scrim")
                || m.contains("bouncer")
                || m.contains("finger")
                || m.contains("biometric")
                || m.contains("udfps")
                || m.contains("carrier")
                || m.contains("bottomaffordance")
                || m.contains("bottom_affordance")
                || m.contains("camera")
                || m.contains("flashlight")
                || m.contains("quickaffordance")) {
            return false;
        }
        int minWidth = dp(view.getContext(), 260);
        int minHeight = dp(view.getContext(), 44);
        int maxHeight = dp(view.getContext(), 260);
        int screenWidth = view.getResources().getDisplayMetrics().widthPixels;
        if (view.getWidth() < Math.min(minWidth, Math.round(screenWidth * 0.52f))
                || view.getHeight() < minHeight
                || view.getHeight() > maxHeight) {
            return false;
        }
        NotificationTextSignals signals = new NotificationTextSignals();
        collectNotificationTextSignals(view, 0, signals);
        return signals.relativeTime && signals.meaningfulTextCount > 0
                || signals.clockTime && signals.meaningfulTextCount > 1 && view.getBackground() != null;
    }

    private static boolean looksLikeVisibleKeyguardBouncer(String marker, View view) {
        String m = marker.toLowerCase(Locale.US);
        if (m.contains("keyguardpin")
                || m.contains("keyguardsecurity")
                || m.contains("keyguardbouncer")
                || m.contains("bouncer")
                || m.contains("pinview")
                || m.contains("pin_view")
                || m.contains("pukview")
                || m.contains("passwordview")
                || m.contains("patternview")
                || m.contains("num_pad")
                || m.contains("numpad")
                || m.contains("passwordentry")
                || m.contains("lockscreenpin")) {
            return true;
        }
        String text = shortTextFor(view);
        if (text == null) {
            text = shortDescriptionFor(view);
        }
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.US);
        return normalized.equals("emergency call")
                || normalized.equals("紧急呼叫")
                || normalized.equals("紧急电话")
                || normalized.contains("enter pin")
                || normalized.contains("输入 pin")
                || normalized.contains("输入密码");
    }

    private static void collectNotificationTextSignals(View view, int depth,
            NotificationTextSignals signals) {
        if (view == null || depth > 4 || signals.visitedCount > 80) {
            return;
        }
        signals.visitedCount++;
        if (view instanceof TextView) {
            CharSequence value = ((TextView) view).getText();
            if (value != null) {
                String text = value.toString().replace('\n', ' ').replace('\r', ' ').trim();
                if (!text.isEmpty()) {
                    boolean relativeTime = NOTIFICATION_RELATIVE_TIME_PATTERN.matcher(text).find();
                    boolean clockTime = NOTIFICATION_CLOCK_TIME_PATTERN.matcher(text).find();
                    signals.relativeTime |= relativeTime;
                    signals.clockTime |= clockTime;
                    if (!relativeTime && !clockTime && isMeaningfulNotificationCardText(text)) {
                        signals.meaningfulTextCount++;
                    }
                }
            }
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        int childCount = Math.min(group.getChildCount(), 24);
        for (int i = 0; i < childCount; i++) {
            collectNotificationTextSignals(group.getChildAt(i), depth + 1, signals);
        }
    }

    private static boolean isMeaningfulNotificationCardText(String text) {
        String trimmed = text.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        if (trimmed.matches("[\\d\\s:/.\\-年月日,]+")) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.US);
        return !lower.equals("silent")
                && !lower.equals("clear all")
                && !lower.equals("manage")
                && !lower.equals("notification settings");
    }

    private static boolean looksLikePluginBatteryView(String marker) {
        return marker.toLowerCase(Locale.US).startsWith("com.oplus.egview.widget.batteryview");
    }

    private static boolean looksLikePluginNotificationView(String marker) {
        return marker.toLowerCase(Locale.US).startsWith("com.oplus.egview.widget.notificationview");
    }

    private static boolean isAodRootLayout(View view) {
        if (view == null) {
            return false;
        }
        String className = view.getClass().getName().toLowerCase(Locale.US);
        return className.contains("aodrootlayout");
    }

    private static boolean isChargingUiView(View view) {
        View current = view;
        int depth = 0;
        while (current != null && depth < 8) {
            String className = current.getClass().getName().toLowerCase(Locale.US);
            if (className.startsWith("com.oplus.charge.")
                    || className.startsWith("com.oplus.systemui.charge.")
                    || className.contains(".charge.")
                    || className.contains(".charging.")) {
                return true;
            }
            Object parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
            depth++;
        }
        return false;
    }

    private static boolean looksLikeSystemAodMediaView(String marker) {
        String m = marker.toLowerCase(Locale.US);
        if (m.contains("timeview") || m.contains("clock") || m.contains("date")) {
            return false;
        }
        if (m.contains("id/aod_media_container")
                || m.contains("id/media_view_container")
                || m.contains("aod_media_container")
                || m.contains("media_view_container")
                || m.contains("com.android.systemui:id/aod_media")
                || m.contains("com.android.systemui:id/media_view")) {
            return true;
        }
        return m.contains("aod_media")
                || m.contains("mediahost")
                || m.contains("mediacontainer")
                || m.contains("mediapanel")
                || m.contains("mediaplayer")
                || m.contains("mediaoutput")
                || m.contains("playback")
                || m.contains("album")
                || m.contains("artwork")
                || m.contains("nowplaying")
                || (m.contains("music") && !m.contains("aodscenemusicdefaulttimeviewgroup"));
    }

    private static boolean containsSystemAodMediaView(ViewGroup root) {
        int childCount = Math.min(root.getChildCount(), 80);
        for (int i = 0; i < childCount; i++) {
            View child = root.getChildAt(i);
            if (looksLikeSystemAodMediaView(markerFor(child))) {
                return true;
            }
            if (child instanceof ViewGroup && containsSystemAodMediaView((ViewGroup) child)) {
                return true;
            }
        }
        return false;
    }

    private static void traverse(View view, ViewVisitor visitor) {
        boolean visitChildren = visitor.visit(view);
        if (!visitChildren || !(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            traverse(group.getChildAt(i), visitor);
        }
    }

    private static boolean looksLikeStockAodText(String marker, CharSequence text) {
        String m = marker.toLowerCase(Locale.US);
        if (looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif") || m.contains("battery") || m.contains("charging")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")) {
            return false;
        }
        if (m.contains("time") || m.contains("clock") || m.contains("date")) {
            return true;
        }
        if (text == null) {
            return false;
        }
        String value = text.toString().trim();
        return value.matches("^[0-9]{1,2}[:\uFF1A][0-9]{2}$") || value.matches("^[0-9]{1,2}$");
    }

    private static boolean looksLikeStockAodClockLeaf(String marker) {
        String m = marker.toLowerCase(Locale.US);
        if (looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif") || m.contains("battery") || m.contains("charging")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")) {
            return false;
        }
        return m.contains("timeview") || m.contains("dateview") || m.contains("clockview")
                || m.contains("aodtime") || m.contains("aoddate");
    }

    private static boolean looksLikeStockAodWeatherOrExtra(String marker, CharSequence text) {
        String m = marker.toLowerCase(Locale.US);
        if (looksLikeSystemAodMediaView(marker)
                || m.contains("notification") || m.contains("notif")
                || m.contains("battery") || m.contains("charging")
                || m.contains("media") || m.contains("music")
                || m.contains("finger") || m.contains("biometric") || m.contains("udfps")) {
            return false;
        }
        if (m.contains("weather")
                || m.contains("temperature")
                || m.contains("tempview")
                || m.contains("extramessage")
                || m.contains("extra_message")
                || m.contains("ataglance")
                || m.contains("at_a_glance")
                || m.contains("aodextra")
                || m.contains("aod_extra")) {
            return true;
        }
        if (text == null) {
            return false;
        }
        String value = text.toString().trim();
        return value.matches("^-?\\d{1,2}\\s*[°℃℉CF]?$")
                || value.matches("^[-+]?\\d{1,2}\\s*[°℃℉]?\\s+[\\p{L}\\p{M}\\s]{2,18}$");
    }

    private static String chooseAtAGlanceCandidate(String marker, String text, String description) {
        String m = marker.toLowerCase(Locale.US);
        if (looksLikeAtAGlanceClockOnlyView(m)
                || m.contains("notification") || m.contains("notif")
                || m.contains("battery") || m.contains("charging")
                || m.contains("media") || m.contains("music")) {
            return null;
        }
        String textCandidate = normalizeAtAGlanceCandidate(text);
        String descriptionCandidate = normalizeAtAGlanceCandidate(description);
        String extractedTextTemperature = extractTemperature(textCandidate);
        String extractedDescriptionTemperature = extractTemperature(descriptionCandidate);
        boolean likelyWeatherView = m.contains("extra") || m.contains("weather")
                || m.contains("temperature") || m.contains("temp");
        if (extractedTextTemperature != null) {
            logAtAGlanceCandidate(marker, extractedTextTemperature);
            return extractedTextTemperature;
        }
        if (extractedDescriptionTemperature != null) {
            logAtAGlanceCandidate(marker, extractedDescriptionTemperature);
            return extractedDescriptionTemperature;
        }
        if (looksLikeTemperature(textCandidate)) {
            logAtAGlanceCandidate(marker, textCandidate);
            return textCandidate;
        }
        if (looksLikeTemperature(descriptionCandidate)) {
            logAtAGlanceCandidate(marker, descriptionCandidate);
            return descriptionCandidate;
        }
        if (likelyWeatherView && looksLikeWeatherPhrase(descriptionCandidate)) {
            logAtAGlanceCandidate(marker, descriptionCandidate);
            return descriptionCandidate;
        }
        if (likelyWeatherView && looksLikeWeatherPhrase(textCandidate)) {
            logAtAGlanceCandidate(marker, textCandidate);
            return textCandidate;
        }
        return null;
    }

    private static boolean looksLikeAtAGlanceClockOnlyView(String marker) {
        return marker.contains("clocktime")
                || marker.contains("clock_time")
                || marker.contains("timeview")
                || marker.contains("time_view")
                || marker.contains("id/time")
                || marker.contains("id/clock")
                || marker.contains("clockviewroot");
    }

    private static String normalizeAtAGlanceCandidate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (trimmed.isEmpty() || trimmed.length() > 24) {
            return null;
        }
        return trimmed;
    }

    private static String extractTemperature(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = TEMPERATURE_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group().replace(" ", "");
        }
        return null;
    }

    private static boolean looksLikeTemperature(String value) {
        return value != null && value.matches("^-?\\d{1,2}\\s*[°℃℉CF]?$");
    }

    private static boolean looksLikeWeatherPhrase(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.US);
        return lower.matches("^[\\p{L}\\p{M}\\s]{3,24}$")
                && (lower.contains("cloud") || lower.contains("rain") || lower.contains("snow")
                || lower.contains("sun") || lower.contains("clear") || lower.contains("fog")
                || lower.contains("mist") || lower.contains("storm") || lower.contains("overcast")
                || lower.contains("wind"));
    }

    private static void logAtAGlanceCandidate(String marker, String value) {
        PixelAodLog.log("captured stock AOD At a Glance candidate value="
                + value + " marker=" + marker
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(null) + "}");
    }

    private static boolean isSystemUiHeaderOrQsView(View view) {
        if (view == null) {
            return false;
        }
        String marker = markerFor(view).toLowerCase(Locale.US);
        if (marker.contains("statusbar") || marker.contains("status_bar")
                || marker.contains("quicksettings") || marker.contains("quick_settings")
                || marker.contains(".qs.") || marker.contains("/qs") || marker.contains("_qs") || marker.contains("qs_")
                || marker.contains("qsheader") || marker.contains("shadeheader") || marker.contains("shade_header")
                || marker.contains("quickstatusheader") || marker.contains("splitshadeheader")
                || marker.contains("policy.clock")
                || marker.contains("bouncer") || marker.contains("emergency") || marker.contains("carrier")) {
            return true;
        }
        ViewParent parent = view.getParent();
        int depth = 0;
        while (parent instanceof View && depth < 12) {
            String name = parent.getClass().getName().toLowerCase(Locale.US);
            if (name.contains(".qs.") || name.contains("quicksettings") || name.contains("quick_settings")
                    || name.contains("statusbar") || name.contains("status_bar")
                    || name.contains("qsheader") || name.contains("shadeheader") || name.contains("shade_header")
                    || name.contains("quickstatusheader") || name.contains("splitshadeheader")
                    || name.contains("bouncer") || name.contains("emergency") || name.contains("carrier")) {
                return true;
            }
            int id = ((View) parent).getId();
            if (id != View.NO_ID) {
                try {
                    String idName = ((View) parent).getResources().getResourceName(id).toLowerCase(Locale.US);
                    if (idName.contains("/qs") || idName.contains("_qs") || idName.contains("qs_")
                            || idName.contains("quicksettings") || idName.contains("quick_settings")
                            || idName.contains("statusbar") || idName.contains("status_bar")
                            || idName.contains("qsheader") || idName.contains("shadeheader") || idName.contains("shade_header")
                            || idName.contains("quickstatusheader") || idName.contains("splitshadeheader")
                            || idName.contains("bouncer") || idName.contains("emergency") || idName.contains("carrier")) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            parent = ((View) parent).getParent();
            depth++;
        }
        return false;
    }

    private static String markerFor(View view) {
        StringBuilder builder = new StringBuilder(view.getClass().getName());
        Object tag = view.getTag();
        if (tag != null) {
            builder.append(" tag=").append(tag);
        }
        int id = view.getId();
        if (id != View.NO_ID) {
            try {
                builder.append(" id=").append(view.getResources().getResourceName(id));
            } catch (Throwable ignored) {
                builder.append(" id=0x").append(Integer.toHexString(id));
            }
        }
        Object parent = view.getParent();
        if (parent != null) {
            builder.append(" parent=").append(parent.getClass().getName());
        }
        return builder.toString();
    }

    private static String shortTextFor(View view) {
        if (!(view instanceof TextView)) {
            return null;
        }
        CharSequence text = ((TextView) view).getText();
        return text != null ? text.toString() : null;
    }

    private static String shortDescriptionFor(View view) {
        CharSequence description = view.getContentDescription();
        return description != null ? description.toString() : null;
    }

    private static String screenLocationFor(View view) {
        try {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            return location[0] + "," + location[1];
        } catch (Throwable ignored) {
            return "?,?";
        }
    }

    private static String textMarkerFor(View view) {
        StringBuilder builder = new StringBuilder();
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.length() > 0) {
                builder.append(" text=").append(shortValue(text));
            }
        }
        CharSequence description = view.getContentDescription();
        if (description != null && description.length() > 0) {
            builder.append(" desc=").append(shortValue(description));
        }
        return builder.toString();
    }

    private static String shortValue(CharSequence value) {
        String text = value.toString().replace('\n', ' ').replace('\r', ' ').trim();
        if (text.length() > 40) {
            text = text.substring(0, 40) + "...";
        }
        return "'" + text + "'";
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private interface ViewVisitor {
        boolean visit(View view);
    }

    private static final class HiddenState {
        final int visibility;
        final float alpha;

        HiddenState(int visibility, float alpha) {
            this.visibility = visibility;
            this.alpha = alpha;
        }
    }

    private static final class AdjustedState {
        final float translationX;
        final float translationY;
        final float translationZ;
        final float alpha;
        final int layerType;

        AdjustedState(float translationX, float translationY, float translationZ, float alpha, int layerType) {
            this.translationX = translationX;
            this.translationY = translationY;
            this.translationZ = translationZ;
            this.alpha = alpha;
            this.layerType = layerType;
        }
    }

    private static final class NotificationTextSignals {
        boolean relativeTime;
        boolean clockTime;
        int meaningfulTextCount;
        int visitedCount;
    }
}


