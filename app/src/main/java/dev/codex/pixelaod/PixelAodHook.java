package dev.codex.pixelaod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
    private static final AtomicBoolean TORCH_CALLBACK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean TORCH_REFRESH_RECEIVER_REGISTERED = new AtomicBoolean(false);
    private static final String ACTION_SWITCH_FLASHLIGHT =
            "com.android.systemui.ACTION_SWITCH_FLASHLIGHT";
    private static final String CLOCK_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodClockLayout";
    private static final String AOD_ROOT_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodRootLayout";
    private static final String AOD_RECORD = "com.oplus.systemui.aod.AodRecord";
    private static final String AOD_UPDATE_MANAGER =
            "com.oplus.systemui.aod.aodclock.off.AodUpdateManager";
    private static final String[] OPLUS_WAKE_UP_CONTROLLER_CANDIDATES = {
            "com.oplus.systemui.aod.display.OplusWakeUpController",
            "com.oplus.systemui.aod.OplusWakeUpController",
            "com.oplus.systemui.aod.controller.OplusWakeUpController",
            "com.oplus.systemui.keyguard.OplusWakeUpController",
            "com.oplus.keyguard.OplusWakeUpController"
    };
    private static final String[] OPLUS_WAKE_CALLBACK_CANDIDATES = {
            "com.oplus.systemui.aod.display.OplusWakeUpController$AodSingleClickWakeUpCallback",
            "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder$AodSingleClickWakeUpCallback",
            "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController$PanoramicAodSingleClickWakeUpCallback"
    };
    private static final String OPLUS_BIOMETRIC_AUTH_CONTROLLER =
            "com.oplus.systemui.biometrics.OplusBiometricAuthController";
    private static final String OPLUS_ON_SCREEN_FINGERPRINT_UI_MECH =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech";
    private static final String[] FOD_AOD_DIAGNOSTIC_METHOD_NAMES = {
            "loadAnimDrawables",
            "restoreIconDrawable",
            "restoreIconDrawableDark",
            "updateFpIconColor",
            "updateFpColor",
            "updateFpIconState",
            "hideUdfpsOverlay",
            "setFpIconVisibilityInAOD",
            "setVisibilityInAOD",
            "showOrHideFingerprintIconTemporarily",
            "showUdfpsOverlay",
            "hidePressAnimImmediately",
            "fpIconHide",
            "fpIconShow",
            "hideFingerprintIcon",
            "hideFingerprintIconTemporarily",
            "notifyHideAodIcon",
            "notifyShowAodIcon",
            "setOnDozeState",
            "setOnDreamingStart",
            "onDreamingStart",
            "onDreamingStopped",
            "onScreenTurnedOff",
            "onScreenTurnedOn",
            "startToAnimInDream",
            "onFpTouch",
            "setTouchDownNow",
            "updateFpIconAlpha",
            "setFingerprintIconShow",
            "showFingerprintIconTemporarily",
            "stopOpticalAnimation",
            "stopPressedAnimation"
    };
    private static final String[] FOD_AOD_ASYNC_RUNNABLE_CLASSES = {
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$1",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$fpIconShow$2",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$restoreIconDrawable$1",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$touchEvent$2",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateFpColor$1"
    };
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
    private static final long AOD_ENERGY_HIDE_REASSERT_DELAY_MILLIS = 80L;
    private static final long[] AOD_NATIVE_TIMEOUT_REASSERT_DELAYS_MILLIS = {
            80L, 450L, 1200L
    };
    private static final long[] SCREEN_OFF_STOCK_SUPPRESSION_REASSERT_DELAYS_MILLIS = {
            0L, 160L, 620L
    };
    private static final long NATIVE_AOD_TICK_STOCK_SUPPRESSION_DEBOUNCE_MILLIS = 250L;
    private static final long NATIVE_AOD_TICK_STOCK_SUPPRESSION_RECHECK_DELAY_MILLIS = 56L;
    private static final long NATIVE_AOD_FRAME_KICK_MIN_INTERVAL_MILLIS = 1200L;
    private static final long FOD_ONLY_NATIVE_HIDE_SKIP_WINDOW_MILLIS = 300L;
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
    private static volatile Context systemUiContext;
    private static volatile WeakReference<Object> lastBiometricAuthController =
            new WeakReference<>(null);
    private static volatile WeakReference<Object> lastOnScreenFingerprintUiMech =
            new WeakReference<>(null);
    private static volatile WeakReference<Object> lastNativeAodClockLayout =
            new WeakReference<>(null);
    private static volatile Method nativeAodClockRefreshMethod;
    private static volatile Object[] nativeAodClockRefreshArgs = new Object[0];
    private static volatile boolean nativeAodFrameKickInProgress;
    private static volatile long lastNativeAodFrameKickAt;
    private static volatile long lastFodOnlyNativeTimeoutHideSuppressionMs;
    private static volatile String lastFodOnlyNativeTimeoutHideTrace;
    private static final OosProximityTransitionGate OOS_PROXIMITY_TRANSITION_GATE =
            new OosProximityTransitionGate();
    private static volatile long lastOosProximityFarAt;
    private static volatile String lastScreenOffStockSuppressionTrace;
    private static volatile long lastNativeAodTickStockSuppressionAt;
    private static volatile String lastNativeAodTickStockSuppressionTrace;

    private PixelAodHook() {
    }

    static void removeLegacyClockOverlays(String source) {
        if (!ClockPluginHostController.hasValidatedHost()) {
            return;
        }
        removeLegacyClockOverlay(lastStockHost.get(), CUSTOM_TAG, PixelAodClockView.class, source);
        removeLegacyClockOverlay(lastStockHost.get(), LOCKSCREEN_CUSTOM_TAG,
                PixelLockscreenClockView.class, source);
        removeLegacyClockOverlay(lastPixelHost.get(), CUSTOM_TAG, PixelAodClockView.class, source);
        removeLegacyClockOverlay(lastPixelHost.get(), LOCKSCREEN_CUSTOM_TAG,
                PixelLockscreenClockView.class, source);
        removeLegacyClockOverlay(lastShadeHost.get(), CUSTOM_TAG, PixelAodClockView.class, source);
        removeLegacyClockOverlay(lastShadeHost.get(), LOCKSCREEN_CUSTOM_TAG,
                PixelLockscreenClockView.class, source);
    }

    /**
     * Once a ClockPlugin host has produced a real frame it owns both module clock scenes.  The
     * legacy NotificationShadeWindowView overlays remain only as a startup fallback.
     */
    private static boolean refreshPersistentClockPluginHost(String source) {
        if (!ClockPluginHostController.hasValidatedHost()) {
            return false;
        }
        removeLegacyClockOverlays(source + "#remove-legacy");
        return true;
    }

    private static void removeLegacyClockOverlay(ViewGroup host, String tag,
            Class<?> expectedType, String source) {
        if (host == null) {
            return;
        }
        View overlay = host.findViewWithTag(tag);
        if (overlay == null || !expectedType.isInstance(overlay)
                || !(overlay.getParent() instanceof ViewGroup)) {
            return;
        }
        ((ViewGroup) overlay.getParent()).removeView(overlay);
        PixelAodLog.log("removed legacy Pixel clock overlay source=" + source
                + " tag=" + tag + " host=" + hostSummary(host));
    }

    private static boolean isPixelClockOverlay(View view) {
        return view instanceof PixelClockPluginHostView
                || view instanceof PixelAodClockView
                || view instanceof PixelLockscreenClockView;
    }

    static void install(Context context, ClassLoader classLoader) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        systemUiContext = appContext;
        PixelAodSettings.refresh(appContext);
        boolean moduleEnabled = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_MODULE_ENABLED, true);
        if (!moduleEnabled) {
            PixelAodLog.log("Pixel AOD module disabled by setting; hooks not installed");
            return;
        }
        registerSettingsObserver(appContext);
        boolean notificationIcons = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_NOTIFICATION_ICONS, true);
        boolean pixelFingerprintIcon = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, false);
        boolean lockscreenPolicy = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true);
        boolean weather = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_WEATHER, true);
        String aodDisplayMode = PixelAodSettings.getString(appContext,
                PixelAodSettings.KEY_AOD_DISPLAY_MODE,
                PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS);
        if (weather) {
            PixelAodClockView.ensureBreezyWeatherReceiver(appContext);
        }
        ClockPluginHostController.install(appContext, classLoader);
        hookClockLayout(appContext, classLoader);
        hookNativeAodRefreshCallbacks(classLoader);
        hookNotificationView(classLoader);
        hookAodRecord(classLoader);
        hookOplusEnergySavingHideGuards(classLoader);
        hookOplusFingerprintAodDiagnostics(classLoader);
        hookOplusAodTriggerDiagnostics(classLoader);
        hookPowerManagerWakeTriggers();
        hookDreamServiceDozeScreenState();
        if (ENABLE_GLOBAL_STOCK_VIEW_METHOD_HOOKS) {
            hookStockClockVisibilityAndAlphaSuppression();
        } else {
            PixelAodLog.log("skipped global stock View visibility/alpha hooks");
        }
        if (notificationIcons) {
            hookNotificationListenerService();
            hookSystemUiNotificationListener(classLoader);
            registerTorchStateCallback(appContext);
            registerTorchRefreshReceiver(appContext);
        }
        if (lockscreenPolicy) {
            hookLockscreenNotificationPolicy(classLoader);
        }
        hookShadeWindowView(appContext, classLoader);
        hookLockscreenClockProbe(classLoader);
        PixelAodLog.log("skipped global stock clock draw suppression to avoid UI jank");
        PixelAodLog.log("installed Pixel AOD hooks moduleEnabled=" + moduleEnabled
                + " aodDisplayMode=" + aodDisplayMode
                + " notificationIcons=" + notificationIcons
                + " pixelFingerprintIcon=" + pixelFingerprintIcon
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
                            PixelAodClockView.refreshAodPolicyFromSettings("settings-provider-change");
                            PixelFingerprintIconController.refreshLast(
                                    appContext, "settings-provider-change");
                        }
                    });
            PixelAodLog.log("registered Pixel AOD settings observer");
        } catch (Throwable t) {
            PixelAodLog.log("failed to register Pixel AOD settings observer", t);
        }
    }

    private static void registerTorchStateCallback(Context context) {
        if (context == null || !TORCH_CALLBACK_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            CameraManager cameraManager = context.getSystemService(CameraManager.class);
            if (cameraManager == null) {
                PixelAodLog.log("failed to register torch callback reason=no-camera-manager");
                return;
            }
            cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    onTorchStateChanged("onTorchModeChanged", cameraId, enabled);
                }

                @Override
                public void onTorchModeUnavailable(String cameraId) {
                    onTorchStateChanged("onTorchModeUnavailable", cameraId, false);
                }
            }, MAIN);
            PixelAodLog.log("registered torch callback for AOD notification refresh");
        } catch (Throwable t) {
            TORCH_CALLBACK_REGISTERED.set(false);
            PixelAodLog.log("failed to register torch callback for AOD notification refresh", t);
        }
    }

    private static void registerTorchRefreshReceiver(Context context) {
        if (context == null || !TORCH_REFRESH_RECEIVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        try {
            IntentFilter filter = new IntentFilter(ACTION_SWITCH_FLASHLIGHT);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context receiveContext, Intent intent) {
                    String action = intent != null ? intent.getAction() : null;
                    scheduleLiveAlertNotificationRefresh(
                            "flashlight-broadcast#" + action,
                            "systemui-flashlight-action");
                }
            };
            if (Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                appContext.registerReceiver(receiver, filter);
            }
            PixelAodLog.log("registered flashlight action receiver for AOD notification refresh");
        } catch (Throwable t) {
            TORCH_REFRESH_RECEIVER_REGISTERED.set(false);
            PixelAodLog.log("failed to register flashlight action receiver for AOD notification refresh",
                    t);
        }
    }

    private static void onTorchStateChanged(String source, String cameraId, boolean enabled) {
        PixelAodLog.log("observed torch state source=" + source
                + " cameraId=" + cameraId
                + " enabled=" + enabled
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(systemUiContext) + "}");
        scheduleTorchNotificationRefresh(source, cameraId, enabled, 40L);
        scheduleTorchNotificationRefresh(source, cameraId, enabled, 280L);
        scheduleTorchNotificationRefresh(source, cameraId, enabled, 900L);
    }

    private static void scheduleTorchNotificationRefresh(String source, String cameraId,
            boolean enabled, long delayMillis) {
        MAIN.postDelayed(() -> {
            String refreshSource = "torch-state-" + (enabled ? "on" : "off")
                    + "#" + source + "#" + delayMillis + "ms";
            refreshNotificationsFromLastListener(refreshSource);
            PixelAodClockView.forceRefreshNotificationIcons(refreshSource);
            requestNativeAodFrameRefreshKickForLiveAlert(refreshSource, delayMillis);
            PixelAodLog.log("requested AOD notification refresh from torch state"
                    + " cameraId=" + cameraId
                    + " enabled=" + enabled
                    + " delayMs=" + delayMillis
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(systemUiContext) + "}");
        }, delayMillis);
    }

    private static void scheduleLiveAlertNotificationRefresh(String source, String reason) {
        scheduleLiveAlertNotificationRefresh(source, reason, 40L);
        scheduleLiveAlertNotificationRefresh(source, reason, 280L);
        scheduleLiveAlertNotificationRefresh(source, reason, 900L);
    }

    private static void scheduleLiveAlertNotificationRefresh(String source, String reason,
            long delayMillis) {
        MAIN.postDelayed(() -> {
            String refreshSource = "live-alert#" + source + "#" + delayMillis + "ms";
            refreshNotificationsFromLastListener(refreshSource);
            PixelAodClockView.forceRefreshNotificationIcons(refreshSource);
            requestNativeAodFrameRefreshKickForLiveAlert(refreshSource, delayMillis);
            PixelAodLog.log("requested AOD notification refresh from live alert"
                    + " source=" + source
                    + " reason=" + reason
                    + " delayMs=" + delayMillis
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(systemUiContext) + "}");
        }, delayMillis);
    }

    private static void requestNativeAodFrameRefreshKickForLiveAlert(String source,
            long delayMillis) {
        if (delayMillis < 280L) {
            return;
        }
        requestNativeAodFrameRefreshKick(source);
    }

    private static void maybeScheduleFlashlightNotificationRefresh(StatusBarNotification sbn,
            String source, String action) {
        if (!AodNotificationPipeline.isOosFlashlightLiveAlert(sbn)) {
            return;
        }
        scheduleLiveAlertNotificationRefresh(
                "flashlight-notification-" + action + "#" + source,
                "flashlight-notification-" + action);
    }

    private static void maybeScheduleBlackScreenGestureNotificationRefresh(String source,
            String args, String triggerType) {
        String combined = (TextUtils.isEmpty(source) ? "" : source) + " "
                + (TextUtils.isEmpty(args) ? "" : args);
        if (!combined.toLowerCase(Locale.US).contains("oplusblackscreengestureevent")) {
            return;
        }
        if (PixelAodClockView.isDeviceInteractive(systemUiContext)) {
            return;
        }
        scheduleLiveAlertNotificationRefresh(
                "black-screen-gesture#" + triggerType + "#" + source,
                "oplus-black-screen-gesture");
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
        if (isPixelClockOverlay(view) || hasCustomClockAncestor(view)) {
            return false;
        }
        String className = view.getClass().getName();
        if (!isStockDrawSuppressionClassCandidate(className)) {
            return false;
        }
        if (containsPersistentClockPluginHost(view)) {
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
        if (isPixelClockOverlay(view) || hasCustomClockAncestor(view)) {
            return false;
        }
        String className = view.getClass().getName();
        if (!isStockDrawSuppressionClassCandidate(className)) {
            return false;
        }
        if (containsPersistentClockPluginHost(view)) {
            return false;
        }
        Context context = view.getContext();
        if (context == null) {
            logStockSuppressionMiss("hook", view, "no-context");
            return false;
        }
        if (!PixelAodClockView.isAodActive()
                && !PixelAodClockView.isBriefAodDisplayActive(context)) {
            logStockSuppressionMiss("hook", view, "aod-inactive");
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
            if (parent instanceof PixelClockPluginHostView
                    || parent instanceof PixelAodClockView
                    || parent instanceof PixelLockscreenClockView) {
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

    private static void hookNativeAodRefreshCallbacks(ClassLoader classLoader) {
        boolean hooked = false;
        try {
            Class<?> clockLayoutClass = ModernHookBridge.findClass(CLOCK_LAYOUT, classLoader);
            hooked |= hookNativeAodRefreshMethods(clockLayoutClass, "AodClockLayout",
                    "performAodUpdate", "refreshAodTime");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodClockLayout native AOD refresh callbacks", t);
        }
        try {
            Class<?> updateManagerClass = ModernHookBridge.findClass(AOD_UPDATE_MANAGER, classLoader);
            hooked |= hookNativeAodRefreshMethods(updateManagerClass, "AodUpdateManager",
                    "setExactTimeForAlarm", "updateCounterOrSetHideAlarm");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodUpdateManager native AOD refresh callbacks", t);
        }
        PixelAodLog.log("installed native AOD refresh callbacks hooked=" + hooked);
    }

    private static boolean hookNativeAodRefreshMethods(Class<?> clazz, String sourceClass,
            String... methodNames) {
        boolean hooked = false;
        Set<String> names = new HashSet<>();
        Collections.addAll(names, methodNames);
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers()) || !names.contains(method.getName())) {
                continue;
            }
            final Method targetMethod = method;
            final String source = sourceClass + "#" + methodSignature(method);
            final boolean reassertStockAodSuppression = "AodClockLayout".equals(sourceClass)
                    && "performAodUpdate".equals(targetMethod.getName());
            try {
                targetMethod.setAccessible(true);
                rememberNativeAodClockRefreshMethod(sourceClass, targetMethod);
                ModernHookBridge.hookAfter(targetMethod, param -> {
                    rememberNativeAodClockRefreshTarget(sourceClass, targetMethod,
                            param.thisObject, param.args);
                    MAIN.post(() -> handleNativeAodRefreshCallback(
                            source, reassertStockAodSuppression));
                });
                hooked = true;
                PixelAodLog.log("hooked native AOD refresh callback " + source);
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook native AOD refresh callback " + source, t);
            }
        }
        return hooked;
    }

    private static void handleNativeAodRefreshCallback(String source,
            boolean reassertStockAodSuppression) {
        PixelAodClockView.refreshAllForNativeAodTick(source);
        if (reassertStockAodSuppression) {
            reassertStockAodSuppressionAfterNativeTick(source);
        }
    }

    private static void reassertStockAodSuppressionAfterNativeTick(String source) {
        String expectedTrace = PixelAodClockView.peekAodTraceId();
        if (TextUtils.isEmpty(expectedTrace)) {
            PixelAodLog.log("skipped native AOD tick stock suppression source=" + source
                    + " reason=no-trace");
            return;
        }
        long now = SystemClock.uptimeMillis();
        long ageMs = now - lastNativeAodTickStockSuppressionAt;
        if (TextUtils.equals(expectedTrace, lastNativeAodTickStockSuppressionTrace)
                && lastNativeAodTickStockSuppressionAt > 0L
                && ageMs >= 0L
                && ageMs < NATIVE_AOD_TICK_STOCK_SUPPRESSION_DEBOUNCE_MILLIS) {
            PixelAodLog.log("skipped native AOD tick stock suppression source=" + source
                    + " reason=debounced"
                    + " ageMs=" + ageMs
                    + " trace=" + expectedTrace);
            return;
        }
        lastNativeAodTickStockSuppressionAt = now;
        lastNativeAodTickStockSuppressionTrace = expectedTrace;
        String passSource = source + "#native-tick-stock-suppression";
        refreshKnownAodHostVisibility(passSource, expectedTrace);
        MAIN.postDelayed(() -> {
            String currentTrace = PixelAodClockView.peekAodTraceId();
            if (!OosAodLifecycleAdapter.matchesExpectedTrace(expectedTrace, currentTrace)) {
                PixelAodLog.log("skipped native AOD tick stock suppression recheck source="
                        + source
                        + " reason=trace-mismatch"
                        + " expectedTrace=" + expectedTrace
                        + " currentTrace=" + currentTrace);
                return;
            }
            refreshKnownAodHostVisibility(passSource + "-recheck-"
                    + NATIVE_AOD_TICK_STOCK_SUPPRESSION_RECHECK_DELAY_MILLIS + "ms",
                    expectedTrace);
        }, NATIVE_AOD_TICK_STOCK_SUPPRESSION_RECHECK_DELAY_MILLIS);
        PixelAodLog.log("scheduled native AOD tick stock suppression source=" + source
                + " trace=" + expectedTrace
                + " recheckDelayMs=" + NATIVE_AOD_TICK_STOCK_SUPPRESSION_RECHECK_DELAY_MILLIS
                + " state={" + PixelAodClockView.describeAodState(systemUiContext) + "}");
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

    private static String simpleClassName(String className) {
        int index = className.lastIndexOf('.');
        return index >= 0 ? className.substring(index + 1) : className;
    }

    private static Object defaultReturnValue(Class<?> type) {
        if (type == null || type == void.class || !type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
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
            PixelAodClockView.setActiveNotifications(notifications, source);
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
            PixelAodClockView.setActiveNotifications(snapshot, source);
            PixelAodClockView.cacheMediaNotificationCandidate(sbn, source);
            PixelAodLog.log("cached notification from " + source
                    + " pkg=" + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " count=" + snapshot.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            maybeScheduleFlashlightNotificationRefresh(sbn, source, "posted");
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
            PixelAodClockView.setActiveNotifications(snapshot, source);
            PixelAodClockView.removeMediaNotificationCandidate(sbn, source);
            PixelAodLog.log("removed notification from " + source
                    + " pkg=" + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " count=" + snapshot.length
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            maybeScheduleFlashlightNotificationRefresh(sbn, source, "removed");
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
                PixelAodClockView.setActiveNotifications(snapshot, source);
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
            PixelAodClockView.setActiveNotifications(snapshot, source);
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

    private static void hookOplusEnergySavingHideGuards(ClassLoader classLoader) {
        boolean hooked = false;
        try {
            Class<?> recordClass = ModernHookBridge.findClass(AOD_RECORD, classLoader);
            hooked |= hookEnergySavingHideMethods(recordClass, "onEnergySavingNotifyHide",
                    "AodRecord");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodRecord energy-saving hide guard", t);
        }
        try {
            Class<?> updateManagerClass = ModernHookBridge.findClass(AOD_UPDATE_MANAGER, classLoader);
            hooked |= hookEnergySavingHideMethods(updateManagerClass,
                    "notifyHideAodFromEnergySavingDirectly", "AodUpdateManager");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodUpdateManager energy-saving hide guard", t);
        }
        for (String className : OPLUS_WAKE_UP_CONTROLLER_CANDIDATES) {
            try {
                Class<?> controllerClass = ModernHookBridge.findClass(className, classLoader);
                hooked |= hookEnergySavingHideMethods(controllerClass, "hideByTimeoutReceiver",
                        simpleClassName(className));
                hooked |= hookEnergySavingHideMethods(controllerClass, "notifyHideCallback",
                        simpleClassName(className));
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus energy-saving hide guard class "
                        + className, t);
            }
        }
        PixelAodLog.log("installed OPlus AOD energy-saving hide guards hooked=" + hooked);
    }

    private static boolean hookEnergySavingHideMethods(Class<?> clazz, String methodName,
            String sourceClass) {
        boolean hooked = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers())
                    || !methodName.equals(method.getName())) {
                continue;
            }
            final Method targetMethod = method;
            final Class<?> returnType = method.getReturnType();
            final String source = sourceClass + "#" + methodSignature(method);
            try {
                targetMethod.setAccessible(true);
                ModernHookBridge.hookBefore(targetMethod, param -> {
                    Context context = contextFromHookParam(param);
                    if (!shouldSuppressOplusEnergySavingHide(context, source)) {
                        return;
                    }
                    param.setResult(defaultReturnValue(returnType));
                    reassertPixelAodAfterEnergySavingHide(context, source);
                });
                ModernHookBridge.hookAfter(targetMethod, param ->
                        maybeReassertPixelAodAfterNativeTimeoutHide(
                                contextFromHookParam(param), source));
                hooked = true;
                PixelAodLog.log("hooked OPlus AOD energy-saving hide guard " + source);
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus AOD energy-saving hide guard "
                        + source, t);
            }
        }
        return hooked;
    }

    static void requestNativeAodFrameRefreshKick(String source) {
        MAIN.post(() -> runNativeAodFrameRefreshKick(source));
    }

    private static void runNativeAodFrameRefreshKick(String source) {
        Context context = systemUiContext;
        if (context == null) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=no-context trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        if (PixelAodClockView.isDeviceInteractive(context)) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=interactive trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        if (!PixelAodClockView.isAodActive()) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=aod-inactive trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                PixelAodClockView.evaluateAodPolicy(context, source + "#frame-kick");
        if (!decision.shouldApplyModuleAod || !decision.shouldKeepNativeDozeAlive) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=policy"
                    + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
                    + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        Method method = nativeAodClockRefreshMethod;
        Object receiver = lastNativeAodClockLayout.get();
        if (method == null || receiver == null) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=no-native-target method=" + method
                    + " receiver=" + (receiver != null ? receiver.getClass().getName() : "null")
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        long now = SystemClock.uptimeMillis();
        long ageMs = now - lastNativeAodFrameKickAt;
        if (lastNativeAodFrameKickAt > 0L && ageMs >= 0L
                && ageMs < NATIVE_AOD_FRAME_KICK_MIN_INTERVAL_MILLIS) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=throttled ageMs=" + ageMs
                    + " minIntervalMs=" + NATIVE_AOD_FRAME_KICK_MIN_INTERVAL_MILLIS
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        if (nativeAodFrameKickInProgress) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=in-progress trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return;
        }
        Object[] args = nativeAodFrameKickArgs(method);
        try {
            nativeAodFrameKickInProgress = true;
            lastNativeAodFrameKickAt = now;
            method.invoke(receiver, args);
            PixelAodLog.log("requested native AOD frame kick source=" + source
                    + " method=" + methodSignature(method)
                    + " args=" + summarizeArgs(args, 4)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed native AOD frame kick source=" + source
                    + " method=" + methodSignature(method)
                    + " args=" + summarizeArgs(args, 4)
                    + " trace=" + PixelAodClockView.currentAodTraceId(), t);
        } finally {
            nativeAodFrameKickInProgress = false;
        }
    }

    private static void rememberNativeAodClockRefreshMethod(String sourceClass, Method method) {
        if (!"AodClockLayout".equals(sourceClass) || !canInvokeNativeAodClockRefresh(method)) {
            return;
        }
        Method current = nativeAodClockRefreshMethod;
        if (current != null
                && nativeAodClockRefreshPriority(current) >= nativeAodClockRefreshPriority(method)) {
            return;
        }
        nativeAodClockRefreshMethod = method;
        nativeAodClockRefreshArgs = defaultNativeAodFrameKickArgs(method);
        PixelAodLog.log("remembered native AOD refresh method method="
                + methodSignature(method)
                + " priority=" + nativeAodClockRefreshPriority(method));
    }

    private static void rememberNativeAodClockRefreshTarget(String sourceClass, Method method,
            Object receiver, Object[] args) {
        if (!"AodClockLayout".equals(sourceClass)
                || receiver == null
                || !canInvokeNativeAodClockRefresh(method)) {
            return;
        }
        lastNativeAodClockLayout = new WeakReference<>(receiver);
        nativeAodClockRefreshMethod = method;
        nativeAodClockRefreshArgs = compatibleNativeAodFrameKickArgs(method, args)
                ? args.clone()
                : defaultNativeAodFrameKickArgs(method);
    }

    private static void rememberNativeAodClockLayout(Object receiver) {
        if (receiver != null) {
            lastNativeAodClockLayout = new WeakReference<>(receiver);
        }
    }

    private static boolean canInvokeNativeAodClockRefresh(Method method) {
        if (method == null) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 0
                || (parameterTypes.length == 1
                && (parameterTypes[0] == boolean.class || parameterTypes[0] == Boolean.class));
    }

    private static int nativeAodClockRefreshPriority(Method method) {
        if (method == null) {
            return 0;
        }
        if ("performAodUpdate".equals(method.getName())) {
            return 3;
        }
        if ("refreshAodTime".equals(method.getName())) {
            return 2;
        }
        return 1;
    }

    private static Object[] nativeAodFrameKickArgs(Method method) {
        Object[] args = nativeAodClockRefreshArgs;
        if (compatibleNativeAodFrameKickArgs(method, args)) {
            return args.clone();
        }
        return defaultNativeAodFrameKickArgs(method);
    }

    private static boolean compatibleNativeAodFrameKickArgs(Method method, Object[] args) {
        if (method == null || args == null) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != args.length) {
            return false;
        }
        if (parameterTypes.length == 0) {
            return true;
        }
        return parameterTypes[0] == boolean.class || parameterTypes[0] == Boolean.class;
    }

    private static Object[] defaultNativeAodFrameKickArgs(Method method) {
        if (method == null || method.getParameterTypes().length == 0) {
            return new Object[0];
        }
        return new Object[] { Boolean.TRUE };
    }

    private static void hookOplusFingerprintAodDiagnostics(ClassLoader classLoader) {
        PixelFingerprintIconController.installImageViewMutationHooks();
        PixelFingerprintIconController.installVendorViewHooks(classLoader);
        boolean hooked = false;
        hooked |= hookFingerprintAodDiagnosticClass(classLoader, OPLUS_BIOMETRIC_AUTH_CONTROLLER);
        hooked |= hookFingerprintAodDiagnosticClass(classLoader, OPLUS_ON_SCREEN_FINGERPRINT_UI_MECH);
        for (String className : FOD_AOD_ASYNC_RUNNABLE_CLASSES) {
            hooked |= hookFingerprintAodAsyncRunnableClass(classLoader, className);
        }
        PixelAodLog.log("installed FOD AOD diagnostics hooked=" + hooked);
    }

    private static boolean hookFingerprintAodAsyncRunnableClass(
            ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            boolean hooked = false;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!"run".equals(method.getName()) || method.getParameterCount() != 0
                        || Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                final Method targetMethod = method;
                final String source = className + "#run()";
                targetMethod.setAccessible(true);
                ModernHookBridge.hookAfter(targetMethod, param -> {
                    Object uiMech;
                    try {
                        uiMech = ModernHookBridge.getObjectField(param.thisObject, "this$0");
                    } catch (Throwable ignored) {
                        uiMech = null;
                    }
                    if (uiMech == null) {
                        PixelAodLog.log("fingerprint async refresh skipped source=" + source
                                + " reason=outer-ui-mech-unavailable");
                        return;
                    }
                    rememberFingerprintAodInstance("OnScreenFingerprintUiMech", uiMech);
                    PixelFingerprintIconController.refresh(
                            systemUiContext, uiMech, source, false);
                });
                hooked = true;
                PixelAodLog.log("hooked FOD AOD async refresh " + source);
            }
            if (!hooked) {
                PixelAodLog.log("FOD AOD async refresh class has no run method class="
                        + className);
            }
            return hooked;
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("FOD AOD async refresh class not found class=" + className);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook FOD AOD async refresh class=" + className, t);
        }
        return false;
    }

    private static boolean hookFingerprintAodDiagnosticClass(
            ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            return hookFingerprintAodDiagnosticMethods(clazz, simpleClassName(className));
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("FOD AOD diagnostic class not found class=" + className);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook FOD AOD diagnostic class " + className, t);
        }
        return false;
    }

    private static boolean hookFingerprintAodDiagnosticMethods(Class<?> clazz, String sourceClass) {
        boolean hooked = false;
        StringBuilder candidates = new StringBuilder();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!isFingerprintAodDiagnosticMethod(method)) {
                continue;
            }
            if (candidates.length() > 0) {
                candidates.append('|');
            }
            candidates.append(methodSignature(method));
            final Method targetMethod = method;
            final Class<?> returnType = method.getReturnType();
            final String source = sourceClass + "#" + methodSignature(method);
            try {
                targetMethod.setAccessible(true);
                if (PassiveFodShowGate.isPotentialFodShowMethod(targetMethod.getName())) {
                    ModernHookBridge.hookBefore(targetMethod, param -> {
                        Context context = contextFromHookParam(param);
                        if (!PassiveFodShowGate.isFodShowInvocation(
                                targetMethod.getName(), param.args)
                                || !shouldSuppressPassiveFodShow(context, source)) {
                            return;
                        }
                        param.setResult(defaultReturnValue(returnType));
                        dismissExistingPassiveFod(param.thisObject, source);
                    });
                }
                ModernHookBridge.hookAfter(targetMethod, param -> {
                    Context context = contextFromHookParam(param);
                    rememberFingerprintAodInstance(sourceClass, param.thisObject);
                    if ("OnScreenFingerprintUiMech".equals(sourceClass)) {
                        PixelFingerprintIconController.refresh(
                                context, param.thisObject, source, true);
                        if ("onFpTouch".equals(targetMethod.getName())) {
                            PixelFingerprintIconController.onFingerprintTouch(
                                    param.thisObject, param.args, source);
                        }
                    }
                    PixelAodLog.log("FOD AOD diagnostic source=" + source
                            + " args=" + summarizeArgs(param.args, 6)
                            + " result=" + summarizeValue(param.getResult())
                            + " trace=" + PixelAodClockView.currentAodTraceId()
                            + " state={" + PixelAodClockView.describeAodState(context) + "}");
                });
                hooked = true;
                PixelAodLog.log("hooked FOD AOD diagnostic " + source);
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook FOD AOD diagnostic " + source, t);
            }
        }
        PixelAodLog.log("FOD AOD diagnostic candidates class=" + clazz.getName()
                + " hooked=" + hooked
                + " methods=" + (candidates.length() > 0 ? candidates : "none"));
        return hooked;
    }

    private static boolean shouldSuppressPassiveFodShow(Context context, String source) {
        Context checkContext = context != null ? context : systemUiContext;
        if (checkContext == null
                || PixelAodClockView.isDeviceInteractive(checkContext)
                || !PixelAodClockView.isAodActive()) {
            return false;
        }
        long now = SystemClock.uptimeMillis();
        long proximityFarAgeMs = lastOosProximityFarAt > 0L
                && now >= lastOosProximityFarAt
                ? now - lastOosProximityFarAt : -1L;
        long traceAgeMs = PixelAodClockView.currentAodTraceAgeMillis();
        long explicitWakeAgeMs = PixelAodClockView.recentExplicitWakeTriggerAgeMillis();
        if (!PassiveFodShowGate.shouldSuppress(
                traceAgeMs, proximityFarAgeMs, explicitWakeAgeMs)) {
            return false;
        }
        PixelAodLog.i("suppressed passive OPlus FOD show"
                + " source=" + source
                + " proximityFarAgeMs=" + proximityFarAgeMs
                + " explicitWakeAgeMs=" + explicitWakeAgeMs
                + " traceAgeMs=" + traceAgeMs
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
        return true;
    }

    private static void dismissExistingPassiveFod(Object sourceInstance, String source) {
        Object uiMech = sourceInstance != null
                && OPLUS_ON_SCREEN_FINGERPRINT_UI_MECH.equals(sourceInstance.getClass().getName())
                ? sourceInstance : lastOnScreenFingerprintUiMech.get();
        boolean dismissed = invokeFirstNoArgFodMethod(uiMech, "OnScreenFingerprintUiMech",
                source, PixelAodClockView.currentAodTraceId(),
                "notifyHideAodIcon", "hideFingerprintIconTemporarily", "hideFingerprintIcon",
                "fpIconHide");
        if (!dismissed) {
            Object controller = sourceInstance != null
                    && OPLUS_BIOMETRIC_AUTH_CONTROLLER.equals(sourceInstance.getClass().getName())
                    ? sourceInstance : lastBiometricAuthController.get();
            dismissed = invokeFirstNoArgFodMethod(controller, "OplusBiometricAuthController",
                    source, PixelAodClockView.currentAodTraceId(), "hideUdfpsOverlay");
        }
        PixelAodLog.i("dismissed passive OPlus FOD after suppressed show"
                + " source=" + source
                + " dismissed=" + dismissed
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private static boolean isFingerprintAodDiagnosticMethod(Method method) {
        if (method == null || Modifier.isAbstract(method.getModifiers())) {
            return false;
        }
        for (String name : FOD_AOD_DIAGNOSTIC_METHOD_NAMES) {
            if (name.equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void rememberFingerprintAodInstance(String sourceClass, Object instance) {
        if (instance == null || TextUtils.isEmpty(sourceClass)) {
            return;
        }
        if ("OplusBiometricAuthController".equals(sourceClass)) {
            lastBiometricAuthController = new WeakReference<>(instance);
            return;
        }
        if ("OnScreenFingerprintUiMech".equals(sourceClass)) {
            lastOnScreenFingerprintUiMech = new WeakReference<>(instance);
        }
    }

    private static void hookOplusAodTriggerDiagnostics(ClassLoader classLoader) {
        boolean hooked = false;
        for (String className : OPLUS_WAKE_UP_CONTROLLER_CANDIDATES) {
            try {
                Class<?> controllerClass = ModernHookBridge.findClass(className, classLoader);
                hooked |= hookAodTriggerDiagnosticMethods(controllerClass, simpleClassName(className));
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus AOD trigger diagnostics class "
                        + className, t);
            }
        }
        for (String className : OPLUS_WAKE_CALLBACK_CANDIDATES) {
            try {
                Class<?> callbackClass = ModernHookBridge.findClass(className, classLoader);
                hooked |= hookAodTriggerDiagnosticMethods(callbackClass, simpleClassName(className));
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus AOD trigger callback class "
                        + className, t);
            }
        }
        PixelAodLog.log("installed OPlus AOD trigger diagnostics hooked=" + hooked);
    }

    private static boolean hookAodTriggerDiagnosticMethods(Class<?> clazz, String sourceClass) {
        boolean hooked = false;
        StringBuilder candidates = new StringBuilder();
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers())
                    || !isAodTriggerDiagnosticMethod(method)) {
                continue;
            }
            if (candidates.length() > 0) {
                candidates.append('|');
            }
            candidates.append(methodSignature(method));
            final Method targetMethod = method;
            final String source = sourceClass + "#" + methodSignature(method);
            try {
                targetMethod.setAccessible(true);
                ModernHookBridge.hookAfter(targetMethod, param -> {
                    String args = summarizeArgs(param.args, 4);
                    String triggerType = classifyAodTriggerEvent(source, args);
                    if (TextUtils.isEmpty(triggerType)) {
                        return;
                    }
                    if (isPassiveAodTriggerProbe(targetMethod) && isDisplayTriggerType(triggerType)) {
                        PixelAodLog.log("skipped passive OPlus AOD trigger probe "
                                + source + " type=" + triggerType
                                + " args=" + args
                                + " result=" + summarizeValue(param.getResult()));
                        return;
                    }
                    String detail = "args=" + args
                            + ",result=" + summarizeValue(param.getResult());
                    if ("getProxNear".equals(targetMethod.getName())
                            && param.getResult() instanceof Boolean) {
                        boolean near = (Boolean) param.getResult();
                        OosProximityTransitionGate.Transition transition =
                                OOS_PROXIMITY_TRANSITION_GATE.update(near);
                        if (transition == OosProximityTransitionGate.Transition.NEAR) {
                            lastOosProximityFarAt = 0L;
                        } else if (transition == OosProximityTransitionGate.Transition.FAR) {
                            lastOosProximityFarAt = SystemClock.uptimeMillis();
                        }
                        if (transition != OosProximityTransitionGate.Transition.NONE) {
                            PixelAodLog.log("OOS proximity suppression edge"
                                    + " transition=" + transition
                                    + " near=" + near
                                    + " source=" + source);
                        }
                        PixelAodClockView.updateProximityFromOos(
                                near, source, detail);
                    } else {
                        PixelAodClockView.noteNativeTrigger(triggerType, source, detail);
                    }
                    maybeScheduleBlackScreenGestureNotificationRefresh(source, args, triggerType);
                });
                hooked = true;
                PixelAodLog.log("hooked OPlus AOD trigger diagnostic " + source);
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus AOD trigger diagnostic " + source, t);
            }
        }
        PixelAodLog.log("OPlus AOD trigger diagnostic candidates class=" + clazz.getName()
                + " hooked=" + hooked
                + " methods=" + (candidates.length() > 0 ? candidates : "none"));
        return hooked;
    }

    private static boolean isAodTriggerDiagnosticMethod(Method method) {
        if (method == null) {
            return false;
        }
        String name = method.getName();
        String lowerName = name.toLowerCase(Locale.US);
        String methodTriggerType = classifyAodTriggerKeyword(name);
        if (isPassiveAodTriggerProbe(method) && isDisplayTriggerType(methodTriggerType)) {
            return false;
        }
        return lowerName.contains("notifywakeupcallback")
                || lowerName.contains("onwakeup")
                || lowerName.contains("onclick")
                || lowerName.contains("ondoubleclick")
                || lowerName.contains("ongesture")
                || !TextUtils.isEmpty(methodTriggerType);
    }

    private static void hookPowerManagerWakeTriggers() {
        boolean hooked = false;
        try {
            for (Method method : android.os.PowerManager.class.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())
                        || !"wakeUp".equals(method.getName())) {
                    continue;
                }
                final Method targetMethod = method;
                final String source = "PowerManager#" + methodSignature(method);
                try {
                    targetMethod.setAccessible(true);
                    ModernHookBridge.hookBefore(targetMethod, param -> {
                        String args = summarizeArgs(param.args, 6);
                        String triggerType = classifyAodTriggerEvent(source, args);
                        if (TextUtils.isEmpty(triggerType)) {
                            return;
                        }
                        PixelAodClockView.noteNativeTrigger(triggerType, source,
                                "args=" + args);
                    });
                    hooked = true;
                    PixelAodLog.log("hooked PowerManager wake trigger " + source);
                } catch (Throwable t) {
                    PixelAodLog.log("failed to hook PowerManager wake trigger "
                            + source, t);
                }
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to install PowerManager wake trigger hooks", t);
        }
        PixelAodLog.log("installed PowerManager wake trigger hooks hooked=" + hooked);
    }

    private static void hookDreamServiceDozeScreenState() {
        try {
            ModernHookBridge.hookBefore(DreamService.class, "setDozeScreenState", param -> {
                if (param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Integer)) {
                    return;
                }
                int requestedState = (Integer) param.args[0];
                if (requestedState != Display.STATE_OFF) {
                    return;
                }
                Context context = contextFromHookParam(param);
                PixelAodClockView.beginPanelHandoffPresentation(context,
                        "DreamService#setDozeScreenState(OFF)");
                if (!shouldRewriteDozeScreenOffToDoze(context,
                        "DreamService#setDozeScreenState(OFF)")) {
                    return;
                }
                param.args[0] = Display.STATE_DOZE;
                PixelAodLog.log("rewrote DreamService doze screen state"
                        + " source=DreamService#setDozeScreenState"
                        + " requested=OFF replacement=DOZE"
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
            }, int.class);
            PixelAodLog.log("hooked DreamService#setDozeScreenState AOD keepalive guard");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook DreamService#setDozeScreenState AOD keepalive guard",
                    t);
        }
    }

    private static String classifyAodTriggerEvent(String source, String args) {
        String combined = (TextUtils.isEmpty(source) ? "" : source) + " "
                + (TextUtils.isEmpty(args) ? "" : args);
        String triggerType = classifyAodTriggerKeyword(combined);
        if (!TextUtils.isEmpty(triggerType)) {
            return triggerType;
        }
        String lowerSource = TextUtils.isEmpty(source)
                ? "" : source.toLowerCase(Locale.US);
        if (lowerSource.contains("notifywakeupcallback")
                || lowerSource.contains("onwakeup")) {
            return "tap";
        }
        return "";
    }

    private static boolean isPassiveAodTriggerProbe(Method method) {
        if (method == null) {
            return false;
        }
        String lowerName = method.getName().toLowerCase(Locale.US);
        return lowerName.startsWith("get")
                || lowerName.startsWith("is")
                || lowerName.startsWith("set")
                || lowerName.startsWith("register")
                || lowerName.startsWith("unregister")
                || lowerName.startsWith("access$get");
    }

    private static boolean isDisplayTriggerType(String triggerType) {
        return "tap".equals(triggerType) || "pickup".equals(triggerType);
    }

    private static Context contextFromHookParam(ModernHookBridge.HookParam param) {
        if (param != null) {
            Context context = contextFromObject(param.thisObject);
            if (context != null) {
                return context;
            }
            if (param.args != null) {
                for (Object arg : param.args) {
                    context = contextFromObject(arg);
                    if (context != null) {
                        return context;
                    }
                }
            }
        }
        return systemUiContext;
    }

    private static Context contextFromObject(Object value) {
        if (value instanceof Context) {
            return (Context) value;
        }
        if (value instanceof View) {
            return ((View) value).getContext();
        }
        if (value instanceof DreamService) {
            return (DreamService) value;
        }
        return null;
    }

    private static boolean shouldRewriteDozeScreenOffToDoze(Context context, String source) {
        Context checkContext = context != null ? context : systemUiContext;
        String state = PixelAodClockView.describeAodState(checkContext);
        if (checkContext == null) {
            PixelAodLog.log("allowed DreamService doze OFF source=" + source
                    + " reason=no-context state={" + state + "}");
            return false;
        }
        if (PixelAodClockView.isDeviceInteractive(checkContext)) {
            PixelAodLog.log("allowed DreamService doze OFF source=" + source
                    + " reason=interactive state={" + state + "}");
            return false;
        }
        if (PixelAodClockView.isProximityNear()) {
            PixelAodLog.log("allowed DreamService doze OFF source=" + source
                    + " reason=proximity-near state={" + state + "}");
            return false;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                PixelAodClockView.evaluateAodPolicy(checkContext, source);
        if (!decision.modulePolicyAllowsDisplay
                || !decision.shouldApplyModuleAod
                || !decision.shouldKeepNativeDozeAlive) {
            PixelAodLog.log("allowed DreamService doze OFF source=" + source
                    + " reason=policy"
                    + " modulePolicyReason=" + decision.modulePolicyReason
                    + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
                    + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                    + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
            return false;
        }
        PixelAodLog.log("blocked DreamService doze OFF source=" + source
                + " reason=" + decision.keepNativeDozeReason
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
        return true;
    }

    private static boolean shouldSuppressOplusEnergySavingHide(Context context, String source) {
        Context checkContext = context != null ? context : systemUiContext;
        String state = PixelAodClockView.describeAodState(checkContext);
        if (checkContext == null) {
            PixelAodLog.log("allowed OPlus AOD energy-saving hide source=" + source
                    + " reason=no-context state={" + state + "}");
            return false;
        }
        if (PixelAodClockView.isDeviceInteractive(checkContext)) {
            PixelAodLog.log("allowed OPlus AOD energy-saving hide source=" + source
                    + " reason=interactive state={" + state + "}");
            return false;
        }
        if (!isAodAllowedBySystemSettings(checkContext)) {
            PixelAodLog.log("allowed OPlus AOD energy-saving hide source=" + source
                    + " reason=system-settings state={" + state + "}");
            return false;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                PixelAodClockView.evaluateAodPolicy(checkContext, source + "#energy-saving-hide");
        if (!decision.modulePolicyAllowsDisplay) {
            PixelAodLog.log("allowed OPlus AOD energy-saving hide source=" + source
                    + " reason=" + decision.modulePolicyReason
                    + " shouldAllowNativeHideCallbacks="
                    + decision.shouldAllowNativeHideCallbacks
                    + " state={"
                    + PixelAodClockView.describeAodState(checkContext) + "}");
            return false;
        }
        if (decision.shouldAllowNativeHideCallbacks) {
            if (shouldUseFodOnlyNativeTimeoutHide(checkContext, source, decision)
                    && dispatchFodOnlyNativeTimeoutHide(checkContext, source)) {
                PixelAodLog.i("suppressed OPlus AOD native-timeout hide via FOD-only path"
                        + " source=" + source
                        + " reason=" + decision.nativeHideCallbackReason
                        + " keepDozeReason=" + decision.keepNativeDozeReason
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
                return true;
            }
            PixelAodLog.log("allowed OPlus AOD energy-saving hide source=" + source
                    + " reason=" + decision.nativeHideCallbackReason
                    + " shouldKeepNativeDozeAlive="
                    + decision.shouldKeepNativeDozeAlive
                    + " state={"
                    + PixelAodClockView.describeAodState(checkContext) + "}");
            return false;
        }
        if (!decision.shouldKeepNativeDozeAlive) {
            PixelAodLog.log("allowed OPlus AOD energy-saving hide source=" + source
                    + " reason=" + decision.keepNativeDozeReason
                    + " shouldAllowNativeHideCallbacks="
                    + decision.shouldAllowNativeHideCallbacks
                    + " state={" + state + "}");
            return false;
        }
        PixelAodLog.i("suppressed OPlus AOD energy-saving hide source=" + source
                + " reason=" + decision.nativeHideCallbackReason
                + " keepDozeReason=" + decision.keepNativeDozeReason
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + state + "}");
        return true;
    }

    private static boolean shouldUseFodOnlyNativeTimeoutHide(Context context, String source,
            OosAodLifecycleAdapter.AodPolicyDecision decision) {
        return context != null
                && decision != null
                && decision.shouldKeepNativeDozeAlive
                && "native-timeout-callback".equals(decision.nativeHideCallbackReason)
                && isNativeAodTimeoutHideSource(source);
    }

    private static boolean dispatchFodOnlyNativeTimeoutHide(Context context, String source) {
        String trace = PixelAodClockView.peekAodTraceId();
        Object uiMech = lastOnScreenFingerprintUiMech.get();
        boolean dispatched = invokeFirstNoArgFodMethod(uiMech, "OnScreenFingerprintUiMech",
                source, trace,
                "notifyHideAodIcon",
                "hideFingerprintIconTemporarily",
                "hideFingerprintIcon",
                "fpIconHide");
        if (!dispatched) {
            PixelAodLog.log("allowed OPlus AOD native-timeout hide source=" + source
                    + " reason=fod-only-unavailable"
                    + " trace=" + trace
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            return false;
        }
        lastFodOnlyNativeTimeoutHideSuppressionMs = SystemClock.uptimeMillis();
        lastFodOnlyNativeTimeoutHideTrace = trace;
        PixelAodClockView.markRecentAodOverlayVisible(source + "#fod-only-native-timeout-hide");
        return true;
    }

    private static boolean invokeFirstNoArgFodMethod(Object target, String targetName,
            String source, String trace, String... methodNames) {
        if (target == null || methodNames == null || methodNames.length == 0) {
            return false;
        }
        for (String methodName : methodNames) {
            Method method = findNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                continue;
            }
            try {
                method.setAccessible(true);
                Object result = method.invoke(target);
                PixelAodLog.log("FOD-only native-timeout hide invoked"
                        + " target=" + targetName
                        + " method=" + methodSignature(method)
                        + " source=" + source
                        + " result=" + summarizeValue(result)
                        + " trace=" + trace
                        + " state={" + PixelAodClockView.describeAodState(systemUiContext) + "}");
                return true;
            } catch (Throwable t) {
                PixelAodLog.log("failed FOD-only native-timeout hide"
                        + " target=" + targetName
                        + " method=" + methodName
                        + " source=" + source
                        + " trace=" + trace, t);
            }
        }
        PixelAodLog.log("FOD-only native-timeout hide target had no usable method"
                + " target=" + targetName
                + " source=" + source
                + " trace=" + trace
                + " class=" + target.getClass().getName());
        return false;
    }

    private static Method findNoArgMethod(Class<?> clazz, String methodName) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (methodName.equals(method.getName())
                        && method.getParameterTypes().length == 0
                        && !Modifier.isAbstract(method.getModifiers())) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static void reassertPixelAodAfterEnergySavingHide(Context context, String source) {
        Context checkContext = context != null ? context : systemUiContext;
        PixelAodClockView.markRecentAodOverlayVisible(source + "#suppressed-hide");
        MAIN.post(() -> {
            PixelAodClockView.setAodActive(true, source + "#suppressed-hide");
            PixelAodClockView.tickAllInstances();
            refreshKnownAodHostVisibility(source + "#suppressed-hide");
        });
        MAIN.postDelayed(() -> {
            PixelAodClockView.tickAllInstances();
            refreshKnownAodHostVisibility(source + "#suppressed-hide-delayed");
            PixelAodLog.log("reasserted Pixel AOD after OPlus energy-saving hide source="
                    + source
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
        }, AOD_ENERGY_HIDE_REASSERT_DELAY_MILLIS);
    }

    private static void maybeReassertPixelAodAfterNativeTimeoutHide(
            Context context, String source) {
        if (!isNativeAodTimeoutHideSource(source)) {
            return;
        }
        Context checkContext = context != null ? context : systemUiContext;
        String expectedTrace = PixelAodClockView.peekAodTraceId();
        if (wasFodOnlyNativeTimeoutHideJustSuppressed(expectedTrace)) {
            PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                    + " reason=fod-only-native-timeout-suppressed"
                    + " expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
            return;
        }
        if (checkContext == null) {
            PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                    + " reason=no-context expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            return;
        }
        if (PixelAodClockView.isDeviceInteractive(checkContext)) {
            PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                    + " reason=interactive expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
            return;
        }
        if (PixelAodClockView.isProximityNear()) {
            PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                    + " reason=proximity-near expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
            return;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                PixelAodClockView.evaluateAodPolicy(
                        checkContext, source + "#native-timeout-after");
        if (!decision.modulePolicyAllowsDisplay
                || !decision.shouldApplyModuleAod
                || !decision.shouldKeepNativeDozeAlive) {
            PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                    + " reason=policy"
                    + " modulePolicyReason=" + decision.modulePolicyReason
                    + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
                    + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                    + " expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
            return;
        }
        PixelAodLog.log("scheduling Pixel AOD native-timeout reassert source=" + source
                + " expectedTrace=" + expectedTrace
                + " reason=" + decision.nativeHideCallbackReason
                + " keepDozeReason=" + decision.keepNativeDozeReason
                + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
        reassertPixelAodAfterNativeTimeoutHide(checkContext, source, expectedTrace);
    }

    private static boolean wasFodOnlyNativeTimeoutHideJustSuppressed(String expectedTrace) {
        long ageMs = SystemClock.uptimeMillis() - lastFodOnlyNativeTimeoutHideSuppressionMs;
        return ageMs >= 0
                && ageMs <= FOD_ONLY_NATIVE_HIDE_SKIP_WINDOW_MILLIS
                && OosAodLifecycleAdapter.matchesExpectedTrace(
                expectedTrace, lastFodOnlyNativeTimeoutHideTrace);
    }

    private static boolean isNativeAodTimeoutHideSource(String source) {
        return source != null
                && (source.contains("notifyHideCallback")
                || source.contains("AodRecord#onEnergySavingNotifyHide"));
    }

    private static void reassertPixelAodAfterNativeTimeoutHide(
            Context context, String source, String expectedTrace) {
        runPixelAodNativeTimeoutReassert(context, source, expectedTrace, "immediate");
        for (long delayMillis : AOD_NATIVE_TIMEOUT_REASSERT_DELAYS_MILLIS) {
            MAIN.postDelayed(() -> runPixelAodNativeTimeoutReassert(
                    context, source, expectedTrace, "delayed-" + delayMillis),
                    delayMillis);
        }
    }

    private static void runPixelAodNativeTimeoutReassert(Context context, String source,
            String expectedTrace, String pass) {
        MAIN.post(() -> {
            String currentTrace = PixelAodClockView.peekAodTraceId();
            if (!OosAodLifecycleAdapter.matchesExpectedTrace(expectedTrace, currentTrace)) {
                PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                        + " pass=" + pass
                        + " reason=trace-mismatch expectedTrace=" + expectedTrace
                        + " currentTrace=" + currentTrace
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            if (PixelAodClockView.isDeviceInteractive(context)) {
                PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                        + " pass=" + pass
                        + " reason=interactive trace=" + currentTrace
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            if (PixelAodClockView.isProximityNear()) {
                PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                        + " pass=" + pass
                        + " reason=proximity-near trace=" + currentTrace
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            OosAodLifecycleAdapter.AodPolicyDecision decision =
                    PixelAodClockView.evaluateAodPolicy(
                            context, source + "#native-timeout-reassert-" + pass);
            if (!decision.modulePolicyAllowsDisplay
                    || !decision.shouldApplyModuleAod
                    || !decision.shouldKeepNativeDozeAlive) {
                PixelAodLog.log("skipped Pixel AOD native-timeout reassert source=" + source
                        + " pass=" + pass
                        + " reason=policy"
                        + " modulePolicyReason=" + decision.modulePolicyReason
                        + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
                        + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                        + " trace=" + currentTrace
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            PixelAodClockView.markRecentAodOverlayVisible(
                    source + "#native-timeout-reassert-" + pass);
            PixelAodClockView.setAodActive(true, source + "#native-timeout-reassert-" + pass);
            PixelAodClockView.tickAllInstances();
            refreshKnownAodHostVisibility(source + "#native-timeout-reassert-" + pass);
            PixelAodLog.log("reasserted Pixel AOD after native timeout hide source=" + source
                    + " pass=" + pass
                    + " trace=" + currentTrace
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
        });
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
                String source = "KeyguardNotificationVisibilityProvider";
                boolean hidden = Boolean.TRUE.equals(param.getResult());
                if (!hidden && shouldForceHideSilentNotificationOnLockscreen(sbn, ranking, source)) {
                    param.setResult(true);
                    hidden = true;
                }
                if (hidden && isEligibleForLockscreenPolicyOverride(sbn, ranking,
                        source)) {
                    param.setResult(false);
                    hidden = false;
                }
                PixelAodClockView.updateLockscreenVisibilityFromProvider(
                        sbn, hidden, source);
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
                if (!hidden && shouldForceHideSilentNotificationOnLockscreen(sbn, ranking, source)) {
                    param.setResult(true);
                    hidden = true;
                }
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
                || (notification.flags & AodNotificationPipeline.NOTIFICATION_FLAG_SILENT) != 0)) {
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

    private static boolean shouldForceHideSilentNotificationOnLockscreen(StatusBarNotification sbn,
            Object ranking, String source) {
        if (!PixelAodClockView.isLockscreenPolicyEnabled()) {
            return false;
        }
        Context context = systemUiContext;
        if (context == null || !PixelLockscreenClockView.isSystemKeyguardLocked(context)) {
            return false;
        }
        if (sbn == null || sbn.getNotification() == null
                || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        String pkg = sbn.getPackageName();
        boolean testNotification = MODULE_PACKAGE.equals(pkg)
                && TestNotificationReceiver.TEST_TAG.equals(sbn.getTag());
        if (MODULE_PACKAGE.equals(pkg) && !testNotification) {
            return false;
        }
        if ("android".equals(pkg) || "com.android.systemui".equals(pkg)) {
            return false;
        }
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)
                || notification.visibility == Notification.VISIBILITY_SECRET
                || rankingVisibilitySecret(ranking)) {
            return false;
        }
        int importance = rankingImportance(ranking);
        String hiddenReason = PixelAodClockView.lockscreenPolicySilentHiddenReason(sbn, importance);
        if (testNotification || hiddenReason == null) {
            return false;
        }
        PixelAodLog.log("forcing lockscreen silent-notification hide pkg=" + pkg
                + " key=" + sbn.getKey()
                + " source=" + source
                + " reason=" + hiddenReason
                + " importance=" + importance
                + " flags=0x" + Integer.toHexString(notification.flags)
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(context) + "}");
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
            rememberNativeAodClockLayout(clockLayoutObject);
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

    private static String classifyAodTriggerKeyword(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String str = value.toLowerCase(Locale.US);
        if (str.contains("prox") || str.contains("near") || str.contains("far")) {
            return "proximity";
        }
        if (str.contains("pocket")) {
            return "pocket";
        }
        if (str.contains("pickup") || str.contains("pick_up") || str.contains("pick-up")
                || str.contains("raise") || str.contains("lift")) {
            return "pickup";
        }
        if (str.contains("tap") || str.contains("touch") || str.contains("gesture")) {
            return "tap";
        }
        if (str.contains("sensor")) {
            return "sensor";
        }
        return "";
    }

    private static String summarizeArgs(Object[] args, int maxArgs) {
        if (args == null || args.length == 0) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(args.length, Math.max(0, maxArgs));
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(';');
            }
            builder.append(i).append('=').append(summarizeValue(args[i]));
        }
        if (args.length > limit) {
            builder.append(";more=").append(args.length - limit);
        }
        return builder.toString();
    }

    private static String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "String(" + truncateForLog((String) value, 48) + ")";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return value.getClass().getSimpleName() + "(" + value + ")";
        }
        if (value instanceof View) {
            return "View(" + markerFor((View) value) + ")";
        }
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().getSimpleName() + "Array("
                    + Array.getLength(value) + ")";
        }
        return clazz.getSimpleName();
    }

    private static String truncateForLog(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
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
                if (!containsPersistentClockPluginHost(view)) {
                    hideView(view, markerFor(view));
                }
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
            boolean persistentHost = refreshPersistentClockPluginHost(source);
            if (!persistentHost) {
                injectPixelClock(context, pixelHost);
                injectPixelLockscreenClock(context, pixelHost);
            }
            PixelLockscreenClockView.refreshAll(source);
            boolean screenOff = !PixelAodClockView.isDeviceInteractive(context);
            boolean lockscreenVisible = isLikelyLockscreenSurfaceVisible(context, host, pixelHost);
            PixelAodLog.log("AOD host snapshot source=" + source
                    + " screenOff=" + screenOff
                    + " lockscreenVisible=" + lockscreenVisible
                    + " stockHost=" + hostSummary(host)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            OosAodLifecycleAdapter.AodPolicyDecision decision =
                    PixelAodClockView.evaluateAodPolicy(context, source + "#host-ready");
            boolean lifecycleCustomizeNow = decision.lifecycleWantsPixelOverlay;
            boolean moduleAodPolicyAllows = !screenOff || decision.modulePolicyAllowsDisplay;
            boolean customizeNow = lifecycleCustomizeNow && moduleAodPolicyAllows;
            if (screenOff) {
                PixelAodClockView.noteScreenOffIfUnset(source + "#host-ready");
                if (moduleAodPolicyAllows) {
                    PixelAodClockView.markRecentAodOverlayVisible(source + "#host-ready");
                }
            }
            if (screenOff && moduleAodPolicyAllows && !lifecycleCustomizeNow) {
                PixelAodClockView.setAodActive(true, source + "#host-ready");
            }
            PixelAodLog.log("AOD host decision source=" + source
                    + " screenOff=" + screenOff
                    + " customizeNow=" + customizeNow
                    + " lifecycleCustomizeNow=" + lifecycleCustomizeNow
                    + " moduleAodPolicyAllows=" + moduleAodPolicyAllows
                    + " shouldDrawPixelOverlay=" + decision.shouldDrawPixelOverlay
                    + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                    + " shouldSuppressStockAodViews=" + decision.shouldSuppressStockAodViews
                    + " shouldAllowNativeHideCallbacks="
                    + decision.shouldAllowNativeHideCallbacks
                    + " reasons={draw=" + decision.drawReason
                    + ",stock=" + decision.stockSuppressionReason
                    + ",nativeHide=" + decision.nativeHideCallbackReason + "}"
                    + " lockscreenVisible=" + lockscreenVisible
                    + " stockHost=" + hostSummary(host)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            if (screenOff && !moduleAodPolicyAllows) {
                restoreAdjustedStatusViews();
                hideStockClockViews(host);
                adjustPluginStatusViews(context, host);
                hideStockKeyguardClockViews(highestParentGroup(host));
                scheduleStockSuppressionReapply(host, source + "#module-aod-policy");
                PixelAodLog.log("suppressed stock AOD without Pixel overlay source=" + source
                        + " reason=" + decision.stockSuppressionReason
                        + " stockHost=" + hostSummary(host)
                        + " pixelHost=" + hostSummary(pixelHost)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            if (screenOff || customizeNow) {
                refreshNotificationsFromLastListener(source);
                hideStockClockViews(host);
                adjustPluginStatusViews(context, host);
                if (ENABLE_EXPENSIVE_DEBUG_DUMPS && PixelAodLog.isDebugEnabled()) {
                    scheduleParentDebugDumps(host, source);
                }
            } else if (lockscreenVisible) {
                if (persistentHost) {
                    hideStockKeyguardClockViews(highestParentGroup(host));
                } else {
                    applyLockscreenClockReplacement(context, host, pixelHost, source);
                }
            } else {
                restoreAdjustedStatusViews();
                restoreHiddenStockViews();
            }
            if (!persistentHost && ENABLE_EXPENSIVE_DEBUG_REAPPLY && PixelAodLog.isDebugEnabled()) {
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
            if (refreshPersistentClockPluginHost(source)) {
                boolean surfaceVisible = isLikelyLockscreenSurfaceVisible(context, host, host);
                PixelLockscreenClockView.setLockscreenSurfaceVisible(surfaceVisible,
                        source + "#persistent-host");
                if (surfaceVisible) {
                    PixelLockscreenClockView.markInteractiveLockscreenSurface(context,
                            source + "#persistent-host");
                    hideStockKeyguardClockViews(highestParentGroup(host));
                }
                PixelAodLog.log("prepared persistent ClockPlugin lockscreen host from " + source
                        + " surfaceVisible=" + surfaceVisible
                        + " host=" + markerFor(host)
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
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
        if (refreshPersistentClockPluginHost("known-host-" + source)) {
            return;
        }
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
                if (context == null
                        || PixelAodClockView.shouldApplyModuleAodNow(
                        context, "known-host-" + source)
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

    static void reassertStockAodSuppressionAfterScreenOff(String source) {
        final String expectedTrace = PixelAodClockView.peekAodTraceId();
        if (TextUtils.isEmpty(expectedTrace)
                || TextUtils.equals(expectedTrace, lastScreenOffStockSuppressionTrace)) {
            return;
        }
        lastScreenOffStockSuppressionTrace = expectedTrace;
        for (long delayMillis : SCREEN_OFF_STOCK_SUPPRESSION_REASSERT_DELAYS_MILLIS) {
            String passSource = source + "#stock-suppression-reapply-" + delayMillis + "ms";
            MAIN.postDelayed(() -> refreshKnownAodHostVisibility(passSource, expectedTrace),
                    delayMillis);
        }
        PixelAodLog.log("scheduled screen-off stock AOD suppression reapply source=" + source
                + " expectedTrace=" + expectedTrace
                + " delaysMs=" + java.util.Arrays.toString(
                SCREEN_OFF_STOCK_SUPPRESSION_REASSERT_DELAYS_MILLIS));
    }

    static void refreshKnownAodHostVisibility(String source) {
        refreshKnownAodHostVisibility(source, PixelAodClockView.peekAodTraceId());
    }

    private static void refreshKnownAodHostVisibility(String source, String expectedTrace) {
        MAIN.post(() -> {
            ViewGroup stockHost = lastStockHost.get();
            ViewGroup pixelHost = lastPixelHost.get();
            ViewGroup shadeHost = lastShadeHost.get();
            ViewGroup host = stockHost != null ? stockHost : pixelHost != null ? pixelHost : shadeHost;
            Context context = host != null ? host.getContext() : null;
            String currentTrace = PixelAodClockView.peekAodTraceId();
            String state = context != null ? PixelAodClockView.describeAodState(context) : "context=null";
            if (!OosAodLifecycleAdapter.matchesExpectedTrace(expectedTrace, currentTrace)) {
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
            boolean persistentHost = refreshPersistentClockPluginHost(
                    source + "#refresh-known-aod-host");
            OosAodLifecycleAdapter.AodPolicyDecision decision =
                    PixelAodClockView.evaluateAodPolicy(context, source + "#known-host");
            if (!decision.shouldSuppressStockAodViews) {
                PixelAodLog.log("skipped refreshing known AOD host visibility source=" + source
                        + " reason=policy"
                        + " shouldSuppressStockAodViews="
                        + decision.shouldSuppressStockAodViews
                        + " stockReason=" + decision.stockSuppressionReason
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
                    + " persistentHost=" + persistentHost
                    + " stockReason=" + decision.stockSuppressionReason
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
        if (refreshPersistentClockPluginHost("inject-aod-clock")) {
            return;
        }
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
        if (refreshPersistentClockPluginHost("inject-lockscreen-clock")) {
            return;
        }
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
        stockSuppressionReapplyLater(host, source, 1800L,
                PixelAodClockView.peekAodTraceId());
    }

    private static void stockSuppressionReapplyLater(ViewGroup host, String source,
            long delayMillis, String expectedTrace) {
        StockAodVisibilityController.scheduleStockSuppressionReapply(
                MAIN,
                host,
                source,
                delayMillis,
                expectedTrace,
                (context, targetHost) -> {
                    hideStockClockViews(targetHost);
                    adjustPluginStatusViews(context, targetHost);
                },
                PixelAodHook::hostSummary);
    }

    private static void scheduleLockscreenReapply(Context context, ViewGroup host) {
        lockscreenReapplyLater(context, host, 650L);
        lockscreenReapplyLater(context, host, 2000L);
    }

    private static void lockscreenReapplyLater(Context context, ViewGroup host, long delayMillis) {
        MAIN.postDelayed(() -> {
            try {
                if (PixelAodClockView.shouldApplyModuleAodNow(
                        context, "NotificationShadeWindowView#delayed-" + delayMillis)
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
                OosAodLifecycleAdapter.AodPolicyDecision decision =
                        PixelAodClockView.evaluateAodPolicy(context, source + "#delayed-reapply");
                boolean lifecycleCustomizeNow = decision.lifecycleWantsPixelOverlay;
                boolean moduleAodPolicyAllows = !screenOff || decision.modulePolicyAllowsDisplay;
                boolean customizeNow = lifecycleCustomizeNow && moduleAodPolicyAllows;
                PixelAodLog.log("delayed AOD reapply source=" + source
                        + " delayMillis=" + delayMillis
                        + " screenOff=" + screenOff
                        + " lockscreenVisible=" + lockscreenVisible
                        + " customizeNow=" + customizeNow
                        + " lifecycleCustomizeNow=" + lifecycleCustomizeNow
                        + " moduleAodPolicyAllows=" + moduleAodPolicyAllows
                        + " shouldDrawPixelOverlay=" + decision.shouldDrawPixelOverlay
                        + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                        + " shouldSuppressStockAodViews=" + decision.shouldSuppressStockAodViews
                        + " shouldAllowNativeHideCallbacks="
                        + decision.shouldAllowNativeHideCallbacks
                        + " reasons={draw=" + decision.drawReason
                        + ",stock=" + decision.stockSuppressionReason
                        + ",nativeHide=" + decision.nativeHideCallbackReason + "}"
                        + " stockHost=" + hostSummary(stockHost)
                        + " pixelHost=" + hostSummary(pixelHost)
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                PixelLockscreenClockView.refreshAll("delayed-reapply");
                if (refreshPersistentClockPluginHost(source + "#delayed-reapply")) {
                    if (screenOff) {
                        if (stockHost != null) {
                            hideStockClockViews(stockHost);
                            hideStockKeyguardClockViews(highestParentGroup(stockHost));
                            adjustPluginStatusViews(context, stockHost);
                        }
                    } else if (lockscreenVisible) {
                        if (stockHost != null) {
                            hideStockKeyguardClockViews(highestParentGroup(stockHost));
                        }
                        if (pixelHost != null) {
                            hideStockKeyguardClockViews(highestParentGroup(pixelHost));
                        }
                    } else {
                        restoreAdjustedStatusViews();
                        restoreHiddenStockViews();
                    }
                    return;
                }
                if (screenOff && !moduleAodPolicyAllows) {
                    restoreAdjustedStatusViews();
                    hideStockClockViews(stockHost);
                    if (stockHost != null) {
                        hideStockKeyguardClockViews(highestParentGroup(stockHost));
                        scheduleStockSuppressionReapply(stockHost, source + "#module-aod-policy");
                    }
                    PixelAodLog.log("kept stock AOD suppressed during delayed reapply source=" + source
                            + " reason=" + decision.stockSuppressionReason
                            + " delayMillis=" + delayMillis
                            + " stockHost=" + hostSummary(stockHost)
                            + " pixelHost=" + hostSummary(pixelHost)
                            + " state={" + PixelAodClockView.describeAodState(context) + "}");
                    return;
                }
                if (screenOff && moduleAodPolicyAllows && !lifecycleCustomizeNow) {
                    PixelAodClockView.setAodActive(true, "delayed-host-ready");
                }
                if (!screenOff && !lockscreenVisible && !customizeNow) {
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
        if (refreshPersistentClockPluginHost(source + "#lockscreen-replacement")) {
            if (stockHost != null) {
                hideStockKeyguardClockViews(highestParentGroup(stockHost));
            }
            if (pixelHost != null) {
                hideStockKeyguardClockViews(highestParentGroup(pixelHost));
            }
            PixelAodLog.log("lockscreen replacement delegated to persistent ClockPlugin host from "
                    + source
                    + " stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
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
            if (isPixelClockOverlay(view)) {
                return false;
            }
            if (containsPersistentClockPluginHost(view)) {
                return true;
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
        if (view == null || isPixelClockOverlay(view)) {
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
                    PixelAodClockView.setActiveNotifications(snapshot,
                            "runtime-NotificationView#" + marker);
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
        StockAodVisibilityController.rememberAdjustedState(view);
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
                if (!PixelAodClockView.shouldApplyModuleAodNow(
                        root.getContext(), source + "#parent-dump")) {
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
        StockAodVisibilityController.scheduleRestoreAfterTransition(
                MAIN,
                source,
                expectedTrace,
                900L,
                new StockAodVisibilityController.HostLookup() {
                    @Override
                    public ViewGroup stockHost() {
                        return lastStockHost.get();
                    }

                    @Override
                    public ViewGroup pixelHost() {
                        return lastPixelHost.get();
                    }
                },
                PixelAodHook::hostSummary);
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
            if (isPixelClockOverlay(view)) {
                return false;
            }
            if (containsPersistentClockPluginHost(view)) {
                stats[1]++;
                return true;
            }
            if (isSystemUiHeaderOrQsView(view)) {
                return false;
            }

            String marker = markerFor(view);
            if (looksLikeSystemAodMediaView(marker)) {
                stats[0]++;
                StockAodVisibilityController.hideView(view, marker, false);
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
            if (isPixelClockOverlay(view)) {
                return false;
            }
            if (containsPersistentClockPluginHost(view)) {
                stats[1]++;
                return true;
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

    /**
     * The persistent ClockPlugin host is a child of OPlus' own ClockViewRoot.  Never hide,
     * alpha, or short-circuit draw on that root, otherwise the replacement child is hidden too.
     */
    private static boolean containsPersistentClockPluginHost(View view) {
        if (view instanceof PixelClockPluginHostView) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        return containsPersistentClockPluginHost((ViewGroup) view, 0);
    }

    private static boolean containsPersistentClockPluginHost(ViewGroup root, int depth) {
        if (root instanceof PixelClockPluginHostView) {
            return true;
        }
        if (depth >= 12) {
            return false;
        }
        int childCount = Math.min(root.getChildCount(), 120);
        for (int i = 0; i < childCount; i++) {
            View child = root.getChildAt(i);
            if (child instanceof PixelClockPluginHostView) {
                return true;
            }
            if (child instanceof ViewGroup
                    && containsPersistentClockPluginHost((ViewGroup) child, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static void hideView(View view, String marker) {
        if (containsPersistentClockPluginHost(view)) {
            return;
        }
        StockAodVisibilityController.hideView(view, marker, looksLikeSystemAodMediaView(marker));
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
        StockAodVisibilityController.restoreHiddenStockViews();
    }

    private static void restoreAdjustedStatusViews() {
        StockAodVisibilityController.restoreAdjustedStatusViews();
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
        // Native OOS media cards sit over the large lockscreen clock — treat them as
        // compact triggers on the lockscreen. AOD uses a separate module media row and
        // does not use this probe for clock size.
        if (looksLikeMediaNotificationSurface(marker)
                || m.contains("qsmediaplayer")
                || m.contains("mediacarousel")
                || m.contains("oplusmedia")
                || m.contains("keyguardmedia")
                || m.contains("mediahostview")
                || m.contains("media_carousel")
                || m.contains("media_container")
                || m.contains("media_view")
                || m.contains("media_player")
                || m.contains("nowplaying")) {
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
                // Media/timer seedlings with text occupy lockscreen space → compact there.
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

    private static final class NotificationTextSignals {
        boolean relativeTime;
        boolean clockTime;
        int meaningfulTextCount;
        int visitedCount;
    }
}


