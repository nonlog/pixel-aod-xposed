package dev.codex.pixelaod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.ImageView;

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
    private static final AtomicBoolean NATIVE_AOD_SETTINGS_OBSERVER_REGISTERED =
            new AtomicBoolean(false);
    private static final AtomicBoolean SELECTED_USER_RECEIVER_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean TORCH_CALLBACK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean TORCH_REFRESH_RECEIVER_REGISTERED = new AtomicBoolean(false);
    private static volatile boolean vendorWakeTriggerAuthorityHooked;
    private static final String ACTION_SWITCH_FLASHLIGHT =
            "com.android.systemui.ACTION_SWITCH_FLASHLIGHT";
    private static final String ACTION_USER_SWITCHED = "android.intent.action.USER_SWITCHED";
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
    private static final String OPLUS_WAKE_UP_PROXIMITY_TASK =
            "com.oplus.systemui.aod.display.OplusWakeUpController$ProximityTask";
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
    private static final String WAKEFULNESS_LIFECYCLE =
            "com.android.systemui.keyguard.WakefulnessLifecycle";
    private static final String DOZE_PARAMETERS =
            "com.android.systemui.statusbar.phone.DozeParameters";
    private static final String SCREEN_OFF_ANIMATION_CONTROLLER =
            "com.android.systemui.statusbar.phone.ScreenOffAnimationController";
    private static final String DOZE_SERVICE_HOST =
            "com.android.systemui.statusbar.phone.DozeServiceHost";
    private static final String BATTERY_CONTROLLER_IMPL =
            "com.android.systemui.statusbar.policy.BatteryControllerImpl";
    private static final String DOZE_SUPPRESSOR =
            "com.android.systemui.doze.DozeSuppressor";
    private static final String KEYGUARD_STATE_CONTROLLER_IMPL =
            "com.android.systemui.statusbar.policy.KeyguardStateControllerImpl";
    private static final String KEYGUARD_TRANSITION_REPOSITORY_IMPL =
            "com.android.systemui.keyguard.data.repository.KeyguardTransitionRepositoryImpl";
    private static final String KEYGUARD_TRANSITION_STEP =
            "com.android.systemui.keyguard.shared.model.TransitionStep";
    private static final String KEYGUARD_SERVICE =
            "com.android.systemui.keyguard.KeyguardService";
    private static final String[] KEYGUARD_SERVICE_BINDER_CLASSES = {
            "com.android.systemui.keyguard.KeyguardService$3",
            "com.android.systemui.keyguard.KeyguardService$1"
    };
    private static final long KEYGUARD_SLEEP_ORIGIN_FRESH_MILLIS = 30_000L;
    private static final String KEYGUARD_NOTIFICATION_VISIBILITY_PROVIDER_IMPL =
            "com.android.systemui.statusbar.notification.interruption.KeyguardNotificationVisibilityProviderImpl";
    // Verified from CPH2573/OOS 16.0.9 SystemUI.apk, classes3.dex.
    private static final String OPLUS_CAPSULE_NOTIFICATION_CARD_VIEW =
            "com.oplus.systemui.notification.lockscreen.notification.CapsuleNotificationCardView";
    private static final String STATUS_BAR_ICON_VIEW =
            "com.android.systemui.statusbar.StatusBarIconView";
    private static final String NOTIF_FILTER =
            "com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifFilter";
    private static final String CUSTOM_TAG = "dev.codex.pixelaod.PIXEL_CLOCK";
    private static final String LOCKSCREEN_CUSTOM_TAG = "dev.codex.pixelaod.PIXEL_LOCKSCREEN_CLOCK";
    private static final int STATUS_EDGE_DP = 68;
    private static final long[] SCREEN_OFF_STOCK_SUPPRESSION_REASSERT_DELAYS_MILLIS = {
            0L, 160L, 620L
    };
    private static final long NATIVE_AOD_TICK_STOCK_SUPPRESSION_DEBOUNCE_MILLIS = 250L;
    private static final long NATIVE_AOD_TICK_STOCK_SUPPRESSION_RECHECK_DELAY_MILLIS = 56L;
    private static final long NATIVE_AOD_FRAME_KICK_MIN_INTERVAL_MILLIS = 1200L;
    private static final boolean ENABLE_EXPENSIVE_DEBUG_REAPPLY = false;
    private static final boolean ENABLE_EXPENSIVE_DEBUG_DUMPS = false;
    private static final boolean ENABLE_NOTIFICATION_VIEW_REFLECTION_DUMP = false;
    private static final boolean ENABLE_GLOBAL_STOCK_VIEW_METHOD_HOOKS = false;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<String> LOGGED_INSPECTION_CLASSES = java.util.Collections.synchronizedSet(new HashSet<>());
    private static final Set<Class<?>> HOOKED_KEYGUARD_SLEEP_BINDERS =
            Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> LOGGED_VIEW_TREE_KEYS = new HashSet<>();
    private static final Set<String> LOGGED_STOCK_SUPPRESSION_MISS_KEYS =
            Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> HOOKED_NOTIFICATION_VIEW_CLASSES = new HashSet<>();
    private static final Set<View> INSPECTED_PLUGIN_NOTIFICATION_VIEWS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<View> OBSERVED_STOCK_AOD_HOSTS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<View, String> STOCK_AOD_HOST_TRACES = new WeakHashMap<>();
    private static final Map<View, Long> LOCKSCREEN_HOST_TOUCH_TIMES = new WeakHashMap<>();
    private static final LinkedHashMap<String, StatusBarNotification> NOTIFICATION_CACHE = new LinkedHashMap<>();
    private static final NotificationSnapshotRefreshGate NOTIFICATION_SNAPSHOT_REFRESH_GATE =
            new NotificationSnapshotRefreshGate();
    private static final NotificationCapsuleIconPolicy NOTIFICATION_CAPSULE_ICON_POLICY =
            new NotificationCapsuleIconPolicy();
    private static final Pattern TEMPERATURE_PATTERN =
            Pattern.compile("-?\\d{1,2}\\s*(?:[°℃℉]|\\s?[CF]\\b)");
    private static final Pattern NOTIFICATION_RELATIVE_TIME_PATTERN =
            Pattern.compile("(?i)(?:\\bjust now\\b|\\b\\d+\\s*(?:min|mins|minute|minutes|hr|hrs|hour|hours)\\s+ago\\b|刚刚|\\d+\\s*(?:分钟|小时)前)");
    private static final Pattern NOTIFICATION_CLOCK_TIME_PATTERN =
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");
    private static NotificationListenerService lastNotificationListener;
    private static volatile boolean pendingSleepFromUnlocked;
    private static volatile boolean pendingSleepOriginAuthoritative;
    private static volatile long pendingSleepOriginLatchedAt = Long.MIN_VALUE;
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
    private static final FodNativeTimeoutHideGate FOD_NATIVE_TIMEOUT_HIDE_GATE =
            new FodNativeTimeoutHideGate();
    private static final OosProximityTransitionGate OOS_PROXIMITY_TRANSITION_GATE =
            new OosProximityTransitionGate();
    private static final VendorScreenOffAnimationEligibility
            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY =
            new VendorScreenOffAnimationEligibility();
    private static final NativeKeyguardSceneEligibility NATIVE_KEYGUARD_SCENE_ELIGIBILITY =
            new NativeKeyguardSceneEligibility();
    private static final NativeDozeTransitionProgressAdapter NATIVE_DOZE_TRANSITION_PROGRESS =
            new NativeDozeTransitionProgressAdapter();
    private static final VendorAmbientSuppressionCapabilities VENDOR_AMBIENT_SUPPRESSION =
            new VendorAmbientSuppressionCapabilities();
    private static volatile WeakReference<Object> lastDozeParameters =
            new WeakReference<>(null);
    private static volatile WeakReference<Object> lastScreenOffAnimationController =
            new WeakReference<>(null);
    private static volatile long lastOosProximityFarAt;
    private static volatile String lastScreenOffStockSuppressionTrace;
    private static volatile long lastNativeAodTickStockSuppressionAt;
    private static volatile String lastNativeAodTickStockSuppressionTrace;

    private PixelAodHook() {
    }

    private static boolean isPixelClockOverlay(View view) {
        return view instanceof PixelAodClockView
                || view instanceof PixelLockscreenClockView
                || view instanceof CouiClockHostView;
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
        PixelAodFeatureFlags.initialize(appContext);
        ActiveClockRendererController.install(appContext, classLoader);
        PixelAodLifecycleHookInstaller.registerSettingsObserver(appContext);
        registerSelectedUserReceiver(appContext);
        boolean notificationIcons = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_NOTIFICATION_ICONS, true);
        boolean pixelFingerprintIcon = PixelAodUdfpsRuntimePolicy.replacementRequested(appContext);
        boolean weather = PixelAodSettings.getBoolean(appContext,
                PixelAodSettings.KEY_WEATHER, true);
        String aodDisplayMode = PixelAodSettings.getString(appContext,
                PixelAodSettings.KEY_AOD_DISPLAY_MODE,
                PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS);
        if (weather) {
            PixelAodContentState.ensureBreezyWeatherReceiver(appContext);
        }
        // Preserve the proven 0.1.380 registration order while making hook ownership explicit.
        PixelAodLifecycleHookInstaller.installScreenOffAnimationEligibility(classLoader);
        PixelAodLifecycleHookInstaller.installAmbientSuppressionCapabilities(classLoader);
        PixelAodLifecycleHookInstaller.installNativeKeyguardTransitionSemantics(classLoader);
        PixelAodLifecycleHookInstaller.installWakefulness(classLoader);
        PixelAodLifecycleHookInstaller.installKeyguardGoingAway(classLoader);
        PixelAodSurfaceHookInstaller.installClockLayout(appContext, classLoader);
        PixelAodLifecycleHookInstaller.installNativeAodRefresh(classLoader);
        PixelAodNotificationHookInstaller.installBaseViewHooks(classLoader);
        PixelAodLifecycleHookInstaller.installAodRecord(classLoader);
        PixelAodLifecycleHookInstaller.installEnergySavingObservers(classLoader);
        PixelAodUdfpsHookInstaller.install(classLoader);
        PixelAodLifecycleHookInstaller.installVendorProximityPauseSemantics(classLoader);
        PixelAodLifecycleHookInstaller.installVendorWakeTriggerSemantics(classLoader);
        PixelAodLifecycleHookInstaller.installAodTriggerDiagnostics(classLoader);
        PixelAodLifecycleHookInstaller.installPowerWakeTriggers();
        PixelAodLifecycleHookInstaller.installDreamDozeStateObserver();
        PixelAodSurfaceHookInstaller.installGlobalStockSuppression(
                ENABLE_GLOBAL_STOCK_VIEW_METHOD_HOOKS);
        PixelAodNotificationHookInstaller.installNotificationContent(
                appContext, classLoader, notificationIcons);
        PixelAodSurfaceHookInstaller.installShadeAndLockscreen(appContext, classLoader);
        PixelAodLog.log("skipped global stock clock draw suppression to avoid UI jank");
        PixelAodLog.log("installed Pixel AOD hooks moduleEnabled=" + moduleEnabled
                + " aodDisplayMode=" + aodDisplayMode
                + " notificationIcons=" + notificationIcons
                + " pixelFingerprintIcon=" + pixelFingerprintIcon
                + " udfpsRenderer=" + PixelAodFeatureFlags.startupUdfpsRenderer()
                + " clockRenderer=COUI_PORT"
                + " weather=" + weather);
    }

    static void registerSettingsObserver(Context context) {
        if (context == null || !SETTINGS_OBSERVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        try {
            ContentObserver observer = new ContentObserver(MAIN) {
                @Override
                public void onChange(boolean selfChange) {
                    onChange(selfChange, null);
                }

                @Override
                public void onChange(boolean selfChange, android.net.Uri uri) {
                    PixelAodSettings.refresh(appContext);
                    PixelAodLog.log("refreshed selected-user Pixel AOD settings from provider change"
                            + " user=" + PixelAodSettings.cachedUserIdForDiagnostics()
                            + " selfChange=" + selfChange
                            + " uri=" + uri);
                    PixelAodClockView.refreshAodPolicyFromSettings("settings-provider-change");
                    if (PixelAodUdfpsRuntimePolicy.usesCouiRenderer()) {
                        CouiUdfpsController.refreshLast(appContext, "settings-provider-change");
                    } else {
                        PixelFingerprintIconController.refreshLast(appContext, "settings-provider-change");
                    }
                }
            };
            boolean allUsers = false;
            try {
                Method registerForUser = android.content.ContentResolver.class.getDeclaredMethod(
                        "registerContentObserver", android.net.Uri.class, boolean.class,
                        ContentObserver.class, int.class);
                registerForUser.setAccessible(true);
                registerForUser.invoke(appContext.getContentResolver(),
                        PixelAodSettingsProvider.URI, true, observer, -1);
                allUsers = true;
            } catch (Throwable ignored) {
                appContext.getContentResolver().registerContentObserver(
                        PixelAodSettingsProvider.URI, true, observer);
            }
            PixelAodLog.i("registered Pixel AOD settings observer allUsers=" + allUsers);
        } catch (Throwable t) {
            PixelAodLog.log("failed to register Pixel AOD settings observer", t);
        }
        registerNativeAodSettingsObserver(appContext);
    }

    private static void registerSelectedUserReceiver(Context context) {
        if (context == null || !SELECTED_USER_RECEIVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        try {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context receiverContext, Intent intent) {
                    if (intent == null || !ACTION_USER_SWITCHED.equals(intent.getAction())) {
                        return;
                    }
                    int userId = intent.getIntExtra("android.intent.extra.user_handle",
                            SelectedUserScope.resolveSelectedUserId());
                    String source = "selected-user-switch#" + Math.max(0, userId);
                    PixelAodClockView.invalidateVendorAmbientSession(source);
                    PixelAodClockView.endVendorTransientAodPresentation(source);
                    PixelAodClockView.hideAllAodOverlays(source);
                    PixelAodContentState.resetSelectedUserContentState(source);
                    PixelAodSettings.onSelectedUserChanged(appContext, userId, source);
                    PixelLockscreenClockView.refreshAll(source);
                    ActiveClockRendererController.refreshSemanticData(source);
                    PixelAodLog.i("handled Pixel AOD selected-user switch user=" + userId);
                }
            };
            IntentFilter filter = new IntentFilter(ACTION_USER_SWITCHED);
            if (Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                appContext.registerReceiver(receiver, filter);
            }
            PixelAodLog.i("registered Pixel AOD selected-user switch receiver");
        } catch (Throwable t) {
            SELECTED_USER_RECEIVER_REGISTERED.set(false);
            PixelAodLog.log("failed to register Pixel AOD selected-user switch receiver", t);
        }
    }

    private static void registerNativeAodSettingsObserver(Context context) {
        if (context == null
                || !NATIVE_AOD_SETTINGS_OBSERVER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        try {
            ContentObserver observer = new ContentObserver(MAIN) {
                @Override
                public void onChange(boolean selfChange) {
                    onChange(selfChange, null);
                }

                @Override
                public void onChange(boolean selfChange, android.net.Uri uri) {
                    String source = "native-aod-setting-change#" + uri;
                    PixelAodClockView.refreshNativeAodEligibility(source);
                }
            };
            appContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(
                            NativeAodAvailabilityAdapter.OPLUS_AOD_AVAILABLE_SETTING),
                    false, observer);
            appContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(
                            NativeAodAvailabilityAdapter.OPLUS_AOD_ENABLED_SETTING),
                    false, observer);
            appContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor("user_setup_complete"), false, observer);
            appContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED),
                    false, observer);
            PixelAodLog.log("registered native AOD availability observers");
        } catch (Throwable t) {
            NATIVE_AOD_SETTINGS_OBSERVER_REGISTERED.set(false);
            PixelAodLog.log("failed to register native AOD availability observers", t);
        }
    }

    static void registerTorchStateCallback(Context context) {
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

    static void registerTorchRefreshReceiver(Context context) {
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
        if (!PrimaryDisplayPolicy.isPrimary(view)) {
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

    static void hookStockClockVisibilityAndAlphaSuppression() {
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
        if (!PrimaryDisplayPolicy.isPrimary(view)) {
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
            if (parent instanceof PixelAodClockView
                    || parent instanceof PixelLockscreenClockView
                    || parent instanceof CouiClockHostView) {
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

    static void hookLockscreenClockProbe(ClassLoader classLoader) {
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

    static void hookLockscreenClockProbeClass(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            ModernHookBridge.hookAfter(clazz, "onAttachedToWindow",
                    param -> MAIN.post(() -> inspectLockscreenClockCandidate(param.thisObject, className)));
            PixelAodLog.log("hooked lockscreen clock probe " + className);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook lockscreen clock probe " + className, t);
        }
    }

    static void hookClockLayout(Context context, ClassLoader classLoader) {
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

    static void hookNativeAodRefreshCallbacks(ClassLoader classLoader) {
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

    static void hookShadeWindowView(Context context, ClassLoader classLoader) {
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

    static void hookNotificationView(ClassLoader classLoader) {
        try {
            Class<?> notificationViewClass = ModernHookBridge.findClass(
                    "com.oplus.egview.widget.NotificationView", classLoader);
            hookNotificationViewClass(notificationViewClass, "SystemUI loader");
        } catch (Throwable t) {
            PixelAodLog.log("failed to find OPlus NotificationView updates from SystemUI loader", t);
        }
    }

    static void hookNotificationListenerService() {
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

    static void hookSystemUiNotificationListener(ClassLoader classLoader) {
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
            int cacheSize;
            synchronized (NOTIFICATION_CACHE) {
                NOTIFICATION_CACHE.put(sbn.getKey(), sbn);
                cacheSize = NOTIFICATION_CACHE.size();
            }
            scheduleCachedNotificationSnapshotRefresh(source);
            PixelAodLog.log("cached notification from " + source
                    + " pkg=" + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " count=" + cacheSize
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            maybeScheduleFlashlightNotificationRefresh(sbn, source, "posted");
        } catch (Throwable t) {
            PixelAodLog.log("failed to cache notification from " + source, t);
        }
    }

    private static void removeCachedNotification(StatusBarNotification sbn, String source) {
        try {
            int cacheSize;
            synchronized (NOTIFICATION_CACHE) {
                NOTIFICATION_CACHE.remove(sbn.getKey());
                cacheSize = NOTIFICATION_CACHE.size();
            }
            NOTIFICATION_CAPSULE_ICON_POLICY.removeFinalDrawable(sbn.getKey());
            scheduleCachedNotificationSnapshotRefresh(source);
            PixelAodLog.log("removed notification from " + source
                    + " pkg=" + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " count=" + cacheSize
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            maybeScheduleFlashlightNotificationRefresh(sbn, source, "removed");
        } catch (Throwable t) {
            PixelAodLog.log("failed to remove notification from " + source, t);
        }
    }

    private static void scheduleCachedNotificationSnapshotRefresh(String source) {
        long delayMillis = NOTIFICATION_SNAPSHOT_REFRESH_GATE.requestDelayMillis(
                SystemClock.uptimeMillis());
        if (delayMillis == NotificationSnapshotRefreshGate.NO_SCHEDULE) {
            return;
        }
        Runnable refresh = () -> {
            NOTIFICATION_SNAPSHOT_REFRESH_GATE.markDispatched(SystemClock.uptimeMillis());
            try {
                StatusBarNotification[] snapshot;
                synchronized (NOTIFICATION_CACHE) {
                    snapshot = NOTIFICATION_CACHE.values().toArray(new StatusBarNotification[0]);
                }
                PixelAodClockView.setActiveNotifications(snapshot,
                        source + "#coalesced-notification-snapshot");
            } catch (Throwable t) {
                PixelAodLog.log("failed to publish coalesced notification snapshot from "
                        + source, t);
            }
        };
        if (delayMillis <= 0L) {
            MAIN.post(refresh);
        } else {
            MAIN.postDelayed(refresh, delayMillis);
        }
    }

    /**
     * Captures the real StatusBarIconView at its next pre-draw boundary.  On this device
     * StatusBarIconView.setNotification() stores mNotification and synchronously updates the
     * ImageView drawable; pre-draw additionally waits for all work queued for that frame.
     */
    static void hookStatusBarNotificationIconCapture(ClassLoader classLoader) {
        try {
            Class<?> statusBarIconViewClass = ModernHookBridge.findClass(STATUS_BAR_ICON_VIEW,
                    classLoader);
            ModernHookBridge.hookAfter(statusBarIconViewClass, "setNotification", param -> {
                if (param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof StatusBarNotification)
                        || !(param.thisObject instanceof ImageView)) {
                    return;
                }
                StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                ImageView iconView = (ImageView) param.thisObject;
                NotificationCapsuleIconPolicy.CaptureToken token =
                        NOTIFICATION_CAPSULE_ICON_POLICY.beginFinalDrawableCapture(iconView,
                                sbn.getKey());
                if (token != null) {
                    captureFinalStatusBarNotificationIconAtPreDraw(iconView, token);
                }
            }, StatusBarNotification.class);
            PixelAodLog.log("hooked StatusBarIconView final notification drawable capture class="
                    + STATUS_BAR_ICON_VIEW);
        } catch (Throwable t) {
            PixelAodLog.log("StatusBarIconView final notification drawable capture unavailable", t);
        }
    }

    private static void captureFinalStatusBarNotificationIconAtPreDraw(ImageView iconView,
            NotificationCapsuleIconPolicy.CaptureToken token) {
        if (iconView == null || token == null) {
            return;
        }
        if (!iconView.isAttachedToWindow()) {
            iconView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    iconView.removeOnAttachStateChangeListener(this);
                    captureFinalStatusBarNotificationIconAtPreDraw(iconView, token);
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    iconView.removeOnAttachStateChangeListener(this);
                }
            });
            return;
        }
        ViewTreeObserver observer = iconView.getViewTreeObserver();
        if (!observer.isAlive()) {
            PixelAodLog.log("skipped OPlus capsule final icon capture reason=no-pre-draw-observer");
            return;
        }
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver currentObserver = iconView.getViewTreeObserver();
                if (currentObserver.isAlive()) {
                    currentObserver.removeOnPreDrawListener(this);
                } else {
                    observer.removeOnPreDrawListener(this);
                }
                captureFinalStatusBarNotificationIcon(iconView, token);
                return true;
            }
        });
    }

    private static void captureFinalStatusBarNotificationIcon(ImageView iconView,
            NotificationCapsuleIconPolicy.CaptureToken token) {
        if (iconView == null || token == null) {
            return;
        }
        try {
            if (!NOTIFICATION_CAPSULE_ICON_POLICY.acceptsFinalDrawableCapture(token)) {
                PixelAodLog.log("OPlus capsule final icon capture stale", () ->
                        "OPlus capsule final icon capture stale key=" + token.notificationKey);
                return;
            }
            Object currentNotification = ModernHookBridge.callMethod(iconView, "getNotification");
            if (!(currentNotification instanceof StatusBarNotification)
                    || !token.notificationKey.equals(
                            ((StatusBarNotification) currentNotification).getKey())) {
                PixelAodLog.log("OPlus capsule final icon capture stale", () ->
                        "OPlus capsule final icon capture stale key=" + token.notificationKey
                                + " reason=statusbar-key-changed");
                return;
            }
            Drawable isolated = rasterizeFinalStatusBarIcon(iconView);
            if (isolated == null) {
                PixelAodLog.log("OPlus capsule final icon capture miss", () ->
                        "OPlus capsule final icon capture miss key=" + token.notificationKey
                                + " reason=no-final-drawable");
                return;
            }
            boolean directUpdateNeeded = NOTIFICATION_CAPSULE_ICON_POLICY
                    .acceptFinalDrawableCapture(token, isolated);
            PixelAodLog.log("OPlus capsule final icon capture hit", () ->
                    "OPlus capsule final icon capture hit key=" + token.notificationKey
                            + " drawable=" + isolated.getClass().getName());
            if (directUpdateNeeded) {
                scheduleLateCapsuleIconDirectUpdate();
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to capture StatusBarIconView final notification drawable", t);
        }
    }

    static void hookOplusNotificationCapsuleIcons(ClassLoader classLoader) {
        try {
            Class<?> cardViewClass = ModernHookBridge.findClass(
                    OPLUS_CAPSULE_NOTIFICATION_CARD_VIEW, classLoader);
            Method bindMethod = null;
            for (Method method : cardViewClass.getDeclaredMethods()) {
                if ("bind".equals(method.getName()) && method.getParameterCount() == 1) {
                    bindMethod = method;
                    break;
                }
            }
            if (bindMethod == null) {
                throw new NoSuchMethodException(OPLUS_CAPSULE_NOTIFICATION_CARD_VIEW
                        + "#bind(one argument)");
            }
            ModernHookBridge.hookAfter(bindMethod, param -> {
                if (param.args == null || param.args.length != 1) {
                    return;
                }
                bindOplusCapsuleNotificationIcon(param.thisObject, param.args[0]);
            });
            PixelAodLog.log("hooked OPlus notification capsule direct icon binding class="
                    + OPLUS_CAPSULE_NOTIFICATION_CARD_VIEW);
        } catch (Throwable t) {
            PixelAodLog.log("OPlus notification capsule direct icon binding unavailable", t);
        }
    }

    private static void bindOplusCapsuleNotificationIcon(Object cardView, Object innerData) {
        String notificationKey = capsuleNotificationCardKey(innerData);
        ImageView iconView = capsuleNotificationCardIconView(cardView, innerData);
        if (TextUtils.isEmpty(notificationKey) || iconView == null) {
            return;
        }
        try {
            NotificationCapsuleIconPolicy.CapsuleBindingToken token =
                    NOTIFICATION_CAPSULE_ICON_POLICY.beginCapsuleIconBinding(iconView,
                            notificationKey);
            Object captured = NOTIFICATION_CAPSULE_ICON_POLICY.finalDrawableFor(notificationKey);
            if (!(captured instanceof Drawable)) {
                NOTIFICATION_CAPSULE_ICON_POLICY.noteCapsuleCacheMiss(token);
                PixelAodLog.log("OPlus capsule final icon cache miss", () ->
                        "OPlus capsule final icon cache miss key=" + notificationKey);
                return;
            }
            NOTIFICATION_CAPSULE_ICON_POLICY.noteCapsuleCacheMiss(token);
            applyFinalDrawableToCapsuleIcon(iconView, (Drawable) captured, notificationKey,
                    "card-bind");
        } catch (Throwable t) {
            PixelAodLog.log("failed OPlus capsule direct icon binding", t);
        }
    }

    private static String capsuleNotificationCardKey(Object innerData) {
        if (innerData == null) {
            return null;
        }
        try {
            Object iconData = ModernHookBridge.callMethod(innerData, "getIconData");
            if (iconData == null) {
                return null;
            }
            Object entry = ModernHookBridge.callMethod(iconData, "getEntry");
            if (entry == null) {
                return null;
            }
            Object sbn = ModernHookBridge.callMethod(entry, "getSbn");
            return sbn instanceof StatusBarNotification ? ((StatusBarNotification) sbn).getKey() : null;
        } catch (Throwable t) {
            PixelAodLog.log("failed to read OPlus capsule card notification key", t);
            return null;
        }
    }

    private static ImageView capsuleNotificationCardIconView(Object cardView, Object innerData) {
        if (cardView == null || innerData == null) {
            return null;
        }
        try {
            Object cardType = ModernHookBridge.callMethod(innerData, "getCardType");
            String cardTypeName = String.valueOf(cardType);
            Object target = "SINGLE_MESSAGE".equals(cardTypeName)
                    ? ModernHookBridge.callMethod(cardView, "getBottomRightBadge")
                    : ModernHookBridge.callMethod(cardView, "getIcon");
            return target instanceof ImageView ? (ImageView) target : null;
        } catch (Throwable t) {
            PixelAodLog.log("failed to read OPlus capsule card icon view", t);
            return null;
        }
    }

    private static Drawable copyCapsuleNotificationDrawable(View capsuleView, Drawable source) {
        if (source == null || capsuleView == null || capsuleView.getContext() == null) {
            return null;
        }
        Context context = capsuleView.getContext();
        Rect originalBounds = new Rect(source.getBounds());
        int width = originalBounds.width() > 0 ? originalBounds.width() : source.getIntrinsicWidth();
        int height = originalBounds.height() > 0 ? originalBounds.height() : source.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            source.setBounds(0, 0, width, height);
            source.draw(canvas);
            return new BitmapDrawable(context.getResources(), bitmap).mutate();
        } catch (Throwable t) {
            PixelAodLog.log("failed to snapshot OPlus notification capsule drawable", t);
            return null;
        } finally {
            source.setBounds(originalBounds);
        }
    }

    /** Rasterizes the actual ImageView draw so final per-instance state is retained. */
    private static Drawable rasterizeFinalStatusBarIcon(ImageView iconView) {
        if (iconView == null || iconView.getContext() == null || iconView.getDrawable() == null) {
            return null;
        }
        int width = iconView.getWidth();
        int height = iconView.getHeight();
        Drawable source = iconView.getDrawable();
        Rect bounds = source.getBounds();
        if (width <= 0) {
            width = bounds.width() > 0 ? bounds.width() : source.getIntrinsicWidth();
        }
        if (height <= 0) {
            height = bounds.height() > 0 ? bounds.height() : source.getIntrinsicHeight();
        }
        if (width <= 0 || height <= 0) {
            return null;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            iconView.draw(canvas);
            return new BitmapDrawable(iconView.getContext().getResources(), bitmap).mutate();
        } catch (Throwable t) {
            PixelAodLog.log("failed to rasterize final StatusBarIconView drawable", t);
            return null;
        }
    }

    private static void scheduleLateCapsuleIconDirectUpdate() {
        MAIN.post(() -> {
            for (NotificationCapsuleIconPolicy.CapsuleBindingToken token
                    : NOTIFICATION_CAPSULE_ICON_POLICY.takeQueuedLateCapsuleBindings()) {
                if (!NOTIFICATION_CAPSULE_ICON_POLICY.acceptsCapsuleBinding(token)) {
                    continue;
                }
                Object target = token.iconView.get();
                Object captured = NOTIFICATION_CAPSULE_ICON_POLICY
                        .finalDrawableFor(token.notificationKey);
                if (!(target instanceof ImageView) || !(captured instanceof Drawable)) {
                    continue;
                }
                applyFinalDrawableToCapsuleIcon((ImageView) target, (Drawable) captured,
                        token.notificationKey, "late-capture");
            }
        });
    }

    private static void applyFinalDrawableToCapsuleIcon(ImageView iconView, Drawable captured,
            String notificationKey, String source) {
        Drawable replacement = copyCapsuleNotificationDrawable(iconView, captured);
        if (replacement == null) {
            PixelAodLog.log("skipped OPlus capsule final icon update", () ->
                    "OPlus capsule final icon update skipped key=" + notificationKey
                            + " source=" + source + " reason=copy-failed");
            return;
        }
        // The captured bitmap already contains StatusBarIconView's final tint/filter/state.
        // OOS leaves a card-type-specific color filter on some CachingIconViews; retaining it
        // would tint the final bitmap a second time.
        iconView.clearColorFilter();
        iconView.setImageDrawable(replacement);
        PixelAodLog.log("applied OPlus capsule final icon", () ->
                "OPlus capsule final icon applied key=" + notificationKey + " source=" + source);
    }

    private static void hookRuntimeNotificationView(Class<?> notificationViewClass, String source) {
        try {
            hookNotificationViewClass(notificationViewClass, source);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook runtime OPlus NotificationView updates from " + source, t);
        }
    }

    static void hookNotificationViewClass(Class<?> notificationViewClass, String source) {
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
                    param -> {
                        String updateSource = "onActiveNotifications/" + source;
                        publishNotificationsFromArg(param, 0, updateSource);
                        suppressRuntimeNotificationViewAfterUpdate(param.thisObject, updateSource);
                    }, StatusBarNotification[].class);
            ModernHookBridge.hookAfter(notificationViewClass, "onReceiveNotification",
                    param -> {
                        String updateSource = "onReceiveNotification/" + source;
                        publishNotificationsFromArg(param, 0, updateSource);
                        suppressRuntimeNotificationViewAfterUpdate(param.thisObject, updateSource);
                    }, StatusBarNotification[].class, StatusBarNotification.class);
            ModernHookBridge.hookAfter(notificationViewClass, "onRemoveNotification",
                    param -> {
                        String updateSource = "onRemoveNotification/" + source;
                        publishNotificationsFromArg(param, 0, updateSource);
                        suppressRuntimeNotificationViewAfterUpdate(param.thisObject, updateSource);
                    }, StatusBarNotification[].class, StatusBarNotification.class);
            ModernHookBridge.hookAfter(notificationViewClass, "clearNotificationView",
                    param -> clearCachedNotifications("NotificationView#clearNotificationView"));
            if ("com.oplus.egview.widget.NotificationView".equals(
                    notificationViewClass.getName())) {
                ModernHookBridge.hookBefore(notificationViewClass, "onDraw", param -> {
                    if (!(param.thisObject instanceof View)) {
                        return;
                    }
                    View notificationView = (View) param.thisObject;
                    if (!PrimaryDisplayPolicy.isPrimary(notificationView)) {
                        return;
                    }
                    Context context = notificationView.getContext();
                    boolean interactive = context == null
                            || PixelAodClockView.isDeviceInteractive(context);
                    if (NativeAodNotificationDrawPolicy.shouldSuppress(
                            true,
                            hasAodAncestor(notificationView),
                            interactive,
                            PixelAodClockView.isAodActive(),
                            PixelAodClockView.isVendorAmbientSessionActive())) {
                        // This class is the OPlus persistent AOD icon row (mIconMap/mIconSize/
                        // mIconSpacing). Full notification pulse/card presentation uses separate
                        // views, so skipping only this draw prevents stock-icon leakage without
                        // taking ownership of vendor pulse UI or the ambient lifecycle.
                        param.setResult(null);
                    }
                }, Canvas.class);
                PixelAodLog.log("hooked OPlus native AOD notification onDraw gate class="
                        + notificationViewClass.getName());
            }
            PixelAodLog.log("hooked OPlus NotificationView notification updates from " + source);
        } catch (Throwable t) {
            synchronized (HOOKED_NOTIFICATION_VIEW_CLASSES) {
                HOOKED_NOTIFICATION_VIEW_CLASSES.remove(key);
            }
            PixelAodLog.log("failed to hook OPlus NotificationView updates from " + source, t);
        }
    }

    private static void suppressRuntimeNotificationViewAfterUpdate(Object candidate, String source) {
        if (!(candidate instanceof View)) {
            return;
        }
        View notificationView = (View) candidate;
        MAIN.post(() -> suppressRuntimeNotificationViewNow(
                notificationView, source + "#post"));
    }

    private static void suppressRuntimeNotificationViewNow(View notificationView, String source) {
        if (notificationView == null || !PrimaryDisplayPolicy.isPrimary(notificationView)) {
            return;
        }
        Context context = notificationView.getContext();
        if (context == null || PixelAodClockView.isDeviceInteractive(context)) {
            return;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                PixelAodClockView.evaluateAodPolicy(context, source + "#notification-view");
        boolean configuredEntryFallback = !decision.shouldSuppressStockAodViews
                && hasAodAncestor(notificationView)
                && PixelAodClockView.isContinuousAodConfiguredForEntry(
                        context, source + "#configured-entry-fallback");
        if (!decision.shouldSuppressStockAodViews && !configuredEntryFallback) {
            PixelAodLog.log("kept runtime OPlus NotificationView source=" + source
                    + " reason=stock-suppression-not-authorized"
                    + " marker=" + markerFor(notificationView)
                    + " trace=" + PixelAodClockView.peekAodTraceId());
            return;
        }
        String marker = markerFor(notificationView);
        hideView(notificationView, marker);
        PixelAodLog.log("suppressed runtime OPlus NotificationView after update source=" + source
                + " configuredEntryFallback=" + configuredEntryFallback
                + " marker=" + marker
                + " trace=" + PixelAodClockView.peekAodTraceId());
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
            NOTIFICATION_CAPSULE_ICON_POLICY.clearFinalDrawables();
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

    static void hookAodRecord(ClassLoader classLoader) {
        try {
            Class<?> recordClass = ModernHookBridge.findClass(AOD_RECORD, classLoader);
            int dispatchHooks = 0;
            dispatchHooks += hookAodRecordDispatch(recordClass, int.class) ? 1 : 0;
            dispatchHooks += hookAodRecordDispatch(recordClass, int.class, Object.class) ? 1 : 0;
            PixelAodLog.log("hooked " + AOD_RECORD
                    + " dispatch lifecycle hooks=" + dispatchHooks);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodRecord lifecycle", t);
        }
    }

    private static boolean hookAodRecordDispatch(Class<?> recordClass,
            Class<?>... parameterTypes) {
        try {
            Method method = ModernHookBridge.findMethod(recordClass, "dispatch", parameterTypes);
            ModernHookBridge.hookAfter(method, param -> {
                if (param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Integer)) {
                    return;
                }
                int event = (Integer) param.args[0];
                String source = "AodRecord#" + methodSignature(method) + "#event=" + event;
                handleAodRecordLifecycleEvent(event, source);
            });
            PixelAodLog.log("hooked AodRecord lifecycle dispatch " + methodSignature(method));
            return true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook AodRecord lifecycle dispatch "
                    + java.util.Arrays.toString(parameterTypes), t);
            return false;
        }
    }

    private static void handleAodRecordLifecycleEvent(int event, String source) {
        if (event == 1) { // DREAM_START in current OOS AodRecord.
            // The vendor lifecycle message was queued by dispatch() before this after-hook runs.
            // Post behind it so Pixel activation follows vendor onDreamingStarted processing,
            // preserving the stable transition timing instead of pre-empting it.
            MAIN.post(() -> {
                PixelAodClockView.beginVendorAmbientSession(source);
                refreshNotificationsFromLastListener(source);
                PixelAodClockView.setAodActive(true, source);
                reassertStockAodSuppressionAfterScreenOff(source + "#after-active");
                PixelAodClockView.tickAllInstances();
                PixelAodLog.log("AodRecord vendor ambient DREAM_START source=" + source
                        + " epoch=" + PixelAodClockView.currentVendorAmbientEpoch());
            });
            return;
        }
        if (event == 2) { // DREAM_STOP.
            runAtFrontOfMain(() -> handleAodRecordDreamStop(source));
            return;
        }
        if (event == 3) { // DREAM_DESTROY: terminal safety net, no transition replay.
            runAtFrontOfMain(() -> {
                if (PixelAodClockView.isVendorAmbientSessionActive()) {
                    PixelAodClockView.invalidateVendorAmbientSession(source);
                }
                PixelAodClockView.hideAllAodOverlays(source);
                PixelAodClockView.stopAllInstances();
                restoreAdjustedStatusViews();
                PixelAodLog.log("AodRecord vendor ambient DREAM_DESTROY source=" + source);
            });
        }
    }

    private static void handleAodRecordDreamStop(String transitionSource) {
        long ambientEpoch = PixelAodClockView.currentVendorAmbientEpoch();
        boolean directGone = PixelAodClockView.isDirectGoneHandoffActiveForEpoch(ambientEpoch);
        PixelAodClockView.invalidateVendorAmbientSession(transitionSource);
        if (directGone) {
            PixelLockscreenClockView.suppressForDirectGone(transitionSource);
            ActiveClockRendererController.suppressForDirectGone(transitionSource);
            PixelAodClockView.hideAllAodOverlays(transitionSource + "#direct-gone");
            PixelAodClockView.stopAllInstances();
            restoreAdjustedStatusViews();
            PixelAodLog.log("AodRecord vendor ambient DREAM_STOP direct-to-Gone"
                    + " source=" + transitionSource
                    + " ambientEpoch=" + ambientEpoch
                    + " trace=" + PixelAodClockView.peekAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(null) + "}");
            return;
        }
        PixelLockscreenClockView.prepareAodToLockscreenTransition(transitionSource);
        PixelAodClockView.hideAllAodOverlays(transitionSource);
        String transitionTrace = PixelAodClockView.peekAodTraceId();
        PixelAodLog.log("AodRecord vendor ambient DREAM_STOP source=" + transitionSource
                + " trace=" + transitionTrace
                + " state={" + PixelAodClockView.describeAodState(null) + "}");
        PixelAodClockView.stopAllInstances();
        restoreAdjustedStatusViews();
        if (PixelLockscreenClockView.shouldShowOnKnownContext()) {
            applyLockscreenClockReplacementFromLastHosts(transitionSource);
        } else {
            suppressSystemAodDuringLockscreenTransition(transitionSource);
            restoreHiddenStockViewsAfterTransition(transitionSource, transitionTrace);
        }
    }

    static void hookOplusEnergySavingHideObservers(ClassLoader classLoader) {
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
        // OplusWakeUpController#notifyHideCallback is a local wake/sensor/timeout callback fanout,
        // not a Dream/AOD terminal. It can fire while the display remains in DOZE_SUSPEND, so it
        // must never invalidate the ambient session or restore stock AOD views. DREAM_STOP and
        // DREAM_DESTROY from AodRecord are the terminal lifecycle seams.
        PixelAodLog.log("installed OPlus AOD energy-saving hide observers hooked=" + hooked);
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
            final String source = sourceClass + "#" + methodSignature(method);
            try {
                targetMethod.setAccessible(true);
                ModernHookBridge.hookBefore(targetMethod, param -> {
                    Context context = contextFromHookParam(param);
                    observeOplusEnergySavingHide(context, source);
                });
                hooked = true;
                PixelAodLog.log("hooked OPlus AOD energy-saving hide observer " + source);
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus AOD energy-saving hide observer "
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
        if (!decision.shouldApplyModuleAod) {
            PixelAodLog.log("skipped native AOD frame kick source=" + source
                    + " reason=policy"
                    + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
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

    static void hookOplusFingerprintAodDiagnostics(ClassLoader classLoader) {
        boolean couiRenderer = PixelAodUdfpsRuntimePolicy.usesCouiRenderer();
        if (couiRenderer) {
            // Startup-exclusive: COUI owns both the replacement drawable and its UiMech/async
            // lifecycle observation. Do not layer the legacy broad FOD diagnostic hooks on top:
            // they duplicate every optical callback and eagerly build large state/argument dumps
            // on the exact wake/authentication path that must stay frame-safe.
            CouiUdfpsController.install(classLoader);
            PixelAodLog.i("COUI UDFPS owns replacement path; legacy fingerprint carrier skipped");
            PixelAodLog.i("COUI UDFPS owns FOD callback observation; broad diagnostics skipped");
            return;
        } else {
            PixelFingerprintIconController.installImageViewMutationHooks();
            PixelFingerprintIconController.installVendorViewHooks(classLoader);
        }
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
                    if (!PixelAodUdfpsRuntimePolicy.usesCouiRenderer()) {
                        PixelFingerprintIconController.refresh(
                                systemUiContext, uiMech, source, false);
                    }
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
                        if ("onFpTouch".equals(targetMethod.getName())) {
                            // Explicit fingerprint interaction owns a new visible FOD cycle.
                            // Release the native-timeout latch before any carrier refresh runs.
                            clearFodNativeTimeoutHide(source + "#explicit-fp-touch");
                            if (!PixelAodUdfpsRuntimePolicy.usesCouiRenderer()) {
                                PixelFingerprintIconController.onFingerprintTouch(
                                        param.thisObject, param.args, source);
                            }
                        }
                        if (!PixelAodUdfpsRuntimePolicy.usesCouiRenderer()) {
                            PixelFingerprintIconController.refresh(
                                    context, param.thisObject, source, true);
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
        if (uiMech == null && PixelAodUdfpsRuntimePolicy.usesCouiRenderer()) {
            uiMech = CouiUdfpsController.lastUiMech();
        }
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

    static void rememberFingerprintAodInstance(String sourceClass, Object instance) {
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

    static void hookOplusVendorProximityPauseSemantics(ClassLoader classLoader) {
        boolean taskHooked = false;
        boolean unregisterHooked = false;
        try {
            Class<?> taskClass = ModernHookBridge.findClass(
                    OPLUS_WAKE_UP_PROXIMITY_TASK, classLoader);
            Method setNear = ModernHookBridge.findMethod(taskClass, "setNear", boolean.class);
            ModernHookBridge.hookAfter(setNear, param -> {
                if (param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Boolean)) {
                    return;
                }
                PixelAodClockView.observeRawProximityFromOos(
                        (Boolean) param.args[0],
                        "OplusWakeUpController$ProximityTask#setNear(boolean)",
                        "authority=vendor-dwell-request");
            });
            Method run = ModernHookBridge.findMethod(taskClass, "run");
            ModernHookBridge.hookAfter(run, param -> {
                Boolean near = invokeBooleanNoArg(param.thisObject, "getNear");
                if (near != null) {
                    observeCommittedOosProximity(
                            near,
                            "OplusWakeUpController$ProximityTask#run()",
                            "authority=vendor-dwell-commit");
                }
            });
            taskHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook OPlus vendor proximity dwell task", t);
        }
        try {
            Class<?> controllerClass = ModernHookBridge.findClass(
                    OPLUS_WAKE_UP_CONTROLLER_CANDIDATES[0], classLoader);
            Method unregister = ModernHookBridge.findMethod(
                    controllerClass, "unregisterProximitySensor");
            ModernHookBridge.hookAfter(unregister, param -> {
                OOS_PROXIMITY_TRANSITION_GATE.reset();
                lastOosProximityFarAt = 0L;
                PixelAodClockView.resetProximityFromOos(
                        "OplusWakeUpController#unregisterProximitySensor()");
            });
            unregisterHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook OPlus proximity lifecycle reset", t);
        }
        PixelAodLog.i("installed OPlus vendor proximity pause semantics"
                + " task=" + taskHooked
                + " unregister=" + unregisterHooked
                + " dwellOwner=OplusWakeUpController$ProximityTask");
    }

    static void hookOplusVendorWakeTriggerSemantics(ClassLoader classLoader) {
        boolean hooked = false;
        String authorityClass = "none";
        for (String className : OPLUS_WAKE_UP_CONTROLLER_CANDIDATES) {
            try {
                Class<?> controllerClass = ModernHookBridge.findClass(className, classLoader);
                Method notifyWakeUp = ModernHookBridge.findMethod(
                        controllerClass, "notifyWakeUpCallback", int.class);
                ModernHookBridge.hookAfter(notifyWakeUp, param -> {
                    if (param.args == null || param.args.length == 0
                            || !(param.args[0] instanceof Integer)) {
                        return;
                    }
                    PixelAodClockView.observeVendorWakeTriggerFromOos(
                            (Integer) param.args[0],
                            "OplusWakeUpController#notifyWakeUpCallback(int)");
                });
                hooked = true;
                authorityClass = className;
                break;
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook OPlus vendor wake-trigger authority class "
                        + className, t);
            }
        }
        vendorWakeTriggerAuthorityHooked = hooked;
        PixelAodLog.i("installed OPlus vendor wake-trigger authority"
                + " hooked=" + hooked
                + " class=" + authorityClass
                + " seam=notifyWakeUpCallback(int)"
                + " mapping=0:tap,1:tilt-pickup,2:motion");
    }

    static void hookOplusAodTriggerDiagnostics(ClassLoader classLoader) {
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
            if (vendorWakeTriggerAuthorityHooked
                    && "notifyWakeUpCallback".equals(method.getName())) {
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
                        observeCommittedOosProximity(
                                (Boolean) param.getResult(), source, detail);
                    } else if ("getProxNearForLuxAod".equals(targetMethod.getName())
                            && param.getResult() instanceof Boolean) {
                        PixelAodClockView.observeRawProximityFromOos(
                                (Boolean) param.getResult(), source, detail);
                    } else if (vendorWakeTriggerAuthorityHooked
                            && isDisplayTriggerType(triggerType)) {
                        PixelAodLog.log("observed subordinate OPlus wake-trigger seam"
                                + " source=" + source
                                + " type=" + triggerType
                                + " action=diagnostic-only"
                                + " authority=notifyWakeUpCallback(int)"
                                + " detail={" + detail + "}");
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

    private static void observeCommittedOosProximity(boolean near, String source, String detail) {
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
        PixelAodClockView.updateProximityFromOos(near, source, detail);
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

    static void hookPowerManagerWakeTriggers() {
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
                        if (vendorWakeTriggerAuthorityHooked
                                && isDisplayTriggerType(triggerType)) {
                            PixelAodLog.log("observed PowerManager wake trigger"
                                    + " source=" + source
                                    + " type=" + triggerType
                                    + " action=diagnostic-only"
                                    + " authority=OplusWakeUpController#notifyWakeUpCallback(int)"
                                    + " args=" + args);
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

    static void hookDreamServiceDozeScreenStateObserver() {
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
                // M9 vendor-delegated lifecycle: keep the proven presentation pre-arm used by
                // the LS -> AOD animation, but never rewrite the display state requested by the
                // vendor DreamService. OPlus/SystemUI is the display/Doze lifecycle authority.
                PixelAodClockView.beginPanelHandoffPresentation(context,
                        "DreamService#setDozeScreenState(OFF)");
                PixelAodLog.log("observed DreamService doze screen state"
                        + " source=DreamService#setDozeScreenState"
                        + " requested=OFF action=vendor-authoritative"
                        + " trace=" + PixelAodClockView.currentAodTraceId()
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
            }, int.class);
            PixelAodLog.log("hooked DreamService#setDozeScreenState lifecycle observer");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook DreamService#setDozeScreenState lifecycle observer",
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
        return "tap".equals(triggerType)
                || "pickup".equals(triggerType)
                || "motion".equals(triggerType);
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

    private static void observeOplusEnergySavingHide(Context context, String source) {
        Context checkContext = context != null ? context : systemUiContext;
        PixelAodClockView.invalidateVendorAmbientSession(source + "#vendor-terminal");
        PixelAodClockView.endVendorTransientAodPresentation(source);
        PixelAodClockView.hideAllAodOverlays(source + "#vendor-terminal");
        restoreAdjustedStatusViews();
        StockAodVisibilityController.restoreHiddenStockViews();
        OosAodLifecycleAdapter.AodPolicyDecision decision = checkContext != null
                ? PixelAodClockView.evaluateAodPolicy(
                checkContext, source + "#energy-saving-hide-observed")
                : null;
        PixelAodLog.log("observed OPlus AOD energy-saving hide"
                + " source=" + source
                + " action=vendor-authoritative"
                + " modulePolicyReason="
                + (decision != null ? decision.modulePolicyReason : "no-context")
                + " shouldApplyModuleAod="
                + (decision != null && decision.shouldApplyModuleAod)
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(checkContext) + "}");
    }

    static void hookVendorScreenOffAnimationEligibility(ClassLoader classLoader) {
        boolean dozeParametersHooked = false;
        boolean screenOffControllerHooked = false;
        try {
            Class<?> dozeParametersClass = ModernHookBridge.findClass(DOZE_PARAMETERS, classLoader);
            for (java.lang.reflect.Constructor<?> constructor
                    : dozeParametersClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    if (param.thisObject != null) {
                        lastDozeParameters = new WeakReference<>(param.thisObject);
                    }
                });
            }
            Method displayNeedsBlanking = ModernHookBridge.findMethod(
                    dozeParametersClass, "getDisplayNeedsBlanking");
            ModernHookBridge.hookAfter(displayNeedsBlanking, param -> {
                if (param.thisObject != null) {
                    lastDozeParameters = new WeakReference<>(param.thisObject);
                }
                if (param.getResult() instanceof Boolean) {
                    VendorScreenOffAnimationEligibility.Snapshot snapshot =
                            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.observeDisplayNeedsBlanking(
                                    (Boolean) param.getResult(),
                                    "DozeParameters#getDisplayNeedsBlanking");
                    PixelAodLog.log("native screen-off animation blanking " + snapshot.describe());
                }
            });
            Method shouldControlScreenOff = ModernHookBridge.findMethod(
                    dozeParametersClass, "shouldControlScreenOff");
            ModernHookBridge.hookAfter(shouldControlScreenOff, param -> {
                if (param.thisObject != null) {
                    lastDozeParameters = new WeakReference<>(param.thisObject);
                }
                if (param.getResult() instanceof Boolean) {
                    VendorScreenOffAnimationEligibility.Snapshot snapshot =
                            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.observeShouldControlScreenOff(
                                    (Boolean) param.getResult(),
                                    "DozeParameters#shouldControlScreenOff");
                    PixelAodLog.log("native screen-off animation control " + snapshot.describe());
                }
            });
            dozeParametersHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook native DozeParameters screen-off eligibility", t);
        }
        try {
            Class<?> controllerClass = ModernHookBridge.findClass(
                    SCREEN_OFF_ANIMATION_CONTROLLER, classLoader);
            for (java.lang.reflect.Constructor<?> constructor
                    : controllerClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    if (param.thisObject != null) {
                        lastScreenOffAnimationController = new WeakReference<>(param.thisObject);
                    }
                });
            }
            Method shouldAnimateDozingChange = ModernHookBridge.findMethod(
                    controllerClass, "shouldAnimateDozingChange");
            ModernHookBridge.hookAfter(shouldAnimateDozingChange, param -> {
                if (param.thisObject != null) {
                    lastScreenOffAnimationController = new WeakReference<>(param.thisObject);
                }
                if (param.getResult() instanceof Boolean) {
                    VendorScreenOffAnimationEligibility.Snapshot snapshot =
                            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY
                                    .observeShouldAnimateDozingChange(
                                            (Boolean) param.getResult(),
                                            "ScreenOffAnimationController#shouldAnimateDozingChange");
                    PixelAodLog.log("native screen-off animation dozing-change "
                            + snapshot.describe());
                }
            });
            screenOffControllerHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook native ScreenOffAnimationController eligibility", t);
        }
        PixelAodLog.i("installed native screen-off animation eligibility"
                + " dozeParameters=" + dozeParametersHooked
                + " screenOffController=" + screenOffControllerHooked);
    }

    static void hookVendorAmbientSuppressionCapabilities(ClassLoader classLoader) {
        boolean hostHooked = false;
        boolean batteryHooked = false;
        boolean suppressorSeedHooked = false;
        try {
            Class<?> hostClass = ModernHookBridge.findClass(DOZE_SERVICE_HOST, classLoader);
            for (java.lang.reflect.Constructor<?> constructor : hostClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param ->
                        seedVendorAmbientSuppressionFromHost(
                                param.thisObject, "DozeServiceHost#constructor"));
            }
            ModernHookBridge.hookAfter(hostClass, "setAlwaysOnSuppressed", param -> {
                if (param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Boolean)) {
                    return;
                }
                VendorAmbientSuppressionCapabilities.Snapshot snapshot =
                        VENDOR_AMBIENT_SUPPRESSION.observeAlwaysOnSuppressed(
                                (Boolean) param.args[0],
                                "DozeServiceHost#setAlwaysOnSuppressed");
                onVendorAmbientSuppressionChanged(snapshot,
                        "DozeServiceHost#setAlwaysOnSuppressed");
            }, boolean.class);
            hostHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook DozeServiceHost ambient suppression", t);
        }
        try {
            Class<?> batteryClass = ModernHookBridge.findClass(BATTERY_CONTROLLER_IMPL, classLoader);
            for (java.lang.reflect.Constructor<?> constructor
                    : batteryClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param ->
                        seedVendorAodPowerSaveFromBatteryController(
                                param.thisObject, "BatteryControllerImpl#constructor"));
            }
            Method setPowerSave = ModernHookBridge.findMethod(
                    batteryClass, "setPowerSave", boolean.class);
            ModernHookBridge.hookAfter(setPowerSave, param -> {
                VendorAmbientSuppressionCapabilities.Snapshot snapshot =
                        observeVendorAodPowerSaveFromBatteryController(
                                param.thisObject, "BatteryControllerImpl#setPowerSave");
                if (snapshot != null) {
                    onVendorAmbientSuppressionChanged(snapshot,
                            "BatteryControllerImpl#setPowerSave");
                }
            });
            batteryHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook BatteryControllerImpl AOD power-save suppression", t);
        }
        try {
            Class<?> suppressorClass = ModernHookBridge.findClass(DOZE_SUPPRESSOR, classLoader);
            for (java.lang.reflect.Constructor<?> constructor
                    : suppressorClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    Object host = null;
                    try {
                        host = ModernHookBridge.getObjectField(param.thisObject, "mDozeHost");
                    } catch (Throwable ignored) {
                    }
                    seedVendorAmbientSuppressionFromHost(
                            host, "DozeSuppressor#constructor");
                });
            }
            suppressorSeedHooked = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook DozeSuppressor suppression-state seed", t);
        }
        PixelAodLog.i("installed vendor ambient suppression capabilities"
                + " host=" + hostHooked
                + " battery=" + batteryHooked
                + " suppressorSeed=" + suppressorSeedHooked
                + " state={" + VENDOR_AMBIENT_SUPPRESSION.snapshot().describe() + "}");
    }

    private static void seedVendorAmbientSuppressionFromHost(Object host, String source) {
        if (host == null) {
            return;
        }
        Boolean alwaysOnSuppressed = readBooleanField(host, "mAlwaysOnSuppressed");
        if (alwaysOnSuppressed != null) {
            VENDOR_AMBIENT_SUPPRESSION.observeAlwaysOnSuppressed(
                    alwaysOnSuppressed, source + "#always-on");
        }
        try {
            Object batteryController = ModernHookBridge.getObjectField(host, "mBatteryController");
            seedVendorAodPowerSaveFromBatteryController(
                    batteryController, source + "#battery");
        } catch (Throwable ignored) {
        }
        PixelAodLog.log("seeded vendor ambient suppression source=" + source
                + " state={" + VENDOR_AMBIENT_SUPPRESSION.snapshot().describe() + "}");
    }

    private static void seedVendorAodPowerSaveFromBatteryController(
            Object batteryController, String source) {
        observeVendorAodPowerSaveFromBatteryController(batteryController, source);
    }

    private static VendorAmbientSuppressionCapabilities.Snapshot
            observeVendorAodPowerSaveFromBatteryController(Object batteryController, String source) {
        Boolean aodPowerSave = readBooleanField(batteryController, "mAodPowerSave");
        if (aodPowerSave == null) {
            return null;
        }
        return VENDOR_AMBIENT_SUPPRESSION.observeAodPowerSave(aodPowerSave, source);
    }

    private static void onVendorAmbientSuppressionChanged(
            VendorAmbientSuppressionCapabilities.Snapshot snapshot, String source) {
        PixelAodLog.i("vendor ambient suppression changed source=" + source
                + " state={" + snapshot.describe() + "}");
        PixelAodClockView.onVendorAmbientSuppressionChanged(source);
    }

    private static void beginVendorScreenOffAnimationTransition(String source) {
        VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.beginTransition(source);
        Object dozeParameters = lastDozeParameters.get();
        Boolean displayNeedsBlanking = invokeBooleanNoArg(
                dozeParameters, "getDisplayNeedsBlanking");
        if (displayNeedsBlanking != null) {
            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.observeDisplayNeedsBlanking(
                    displayNeedsBlanking, source + "#display-blanking-sample");
        }
        PixelAodLog.i("began native screen-off animation eligibility transition "
                + VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.snapshot().describe());
    }

    private static void refreshVendorScreenOffAnimationEligibility(String source) {
        Object dozeParameters = lastDozeParameters.get();
        Boolean displayNeedsBlanking = invokeBooleanNoArg(
                dozeParameters, "getDisplayNeedsBlanking");
        if (displayNeedsBlanking != null) {
            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.observeDisplayNeedsBlanking(
                    displayNeedsBlanking, source + "#display-blanking");
        }
        Boolean shouldControlScreenOff = invokeBooleanNoArg(
                dozeParameters, "shouldControlScreenOff");
        if (shouldControlScreenOff != null) {
            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.observeShouldControlScreenOff(
                    shouldControlScreenOff, source + "#control-screen-off");
        }
        Object screenOffController = lastScreenOffAnimationController.get();
        if (screenOffController == null && dozeParameters != null) {
            try {
                screenOffController = ModernHookBridge.getObjectField(
                        dozeParameters, "mScreenOffAnimationController");
                if (screenOffController != null) {
                    lastScreenOffAnimationController = new WeakReference<>(screenOffController);
                }
            } catch (Throwable ignored) {
            }
        }
        Boolean shouldAnimateDozingChange = invokeBooleanNoArg(
                screenOffController, "shouldAnimateDozingChange");
        if (shouldAnimateDozingChange != null) {
            VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.observeShouldAnimateDozingChange(
                    shouldAnimateDozingChange, source + "#animate-dozing-change");
        }
        PixelAodLog.i("refreshed native screen-off animation eligibility "
                + VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.snapshot().describe());
    }

    private static Boolean invokeBooleanNoArg(Object target, String methodName) {
        if (target == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(target);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Boolean readBooleanField(Object target, String fieldName) {
        if (target == null || TextUtils.isEmpty(fieldName)) {
            return null;
        }
        try {
            Object value = ModernHookBridge.getObjectField(target, fieldName);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static VendorAmbientSuppressionCapabilities.Snapshot vendorAmbientSuppressionSnapshot() {
        return VENDOR_AMBIENT_SUPPRESSION.snapshot();
    }

    static String describeVendorAmbientSuppression() {
        return VENDOR_AMBIENT_SUPPRESSION.snapshot().describe();
    }

    static boolean shouldAnimateVendorScreenOffPresentation() {
        return VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.allowsExistingMorph();
    }

    static boolean canConsumeVendorDozeTransitionProgress() {
        return NATIVE_DOZE_TRANSITION_PROGRESS.snapshot().canConsume(
                VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.allowsVendorProgress(), true);
    }

    static Float consumableVendorDozeTransitionAmbientFractionOrNull() {
        NativeDozeTransitionProgressAdapter.Snapshot snapshot =
                NATIVE_DOZE_TRANSITION_PROGRESS.snapshot();
        if (!snapshot.canConsume(
                VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.allowsVendorProgress(),
                SystemAnimationScalePolicy.animationsEnabled())) {
            return null;
        }
        return snapshot.ambientFraction;
    }

    static String describeVendorScreenOffAnimationEligibility() {
        return VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.snapshot().describe();
    }

    static String describeNativeDozeTransitionProgress() {
        return NATIVE_DOZE_TRANSITION_PROGRESS.snapshot().describe(
                VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY.allowsVendorProgress(),
                SystemAnimationScalePolicy.animationsEnabled());
    }
    static void hookWakefulnessLifecycle(ClassLoader classLoader) {
        hookKeyguardSleepOrigin(classLoader);
        try {
            Class<?> lifecycleClass = ModernHookBridge.findClass(
                    WAKEFULNESS_LIFECYCLE, classLoader);
            int hooks = 0;
            for (Method method : lifecycleClass.getDeclaredMethods()) {
                if (CouiClockWakefulnessRoutingPolicy.shouldArmAodExit(method.getName())) {
                    ModernHookBridge.hookBefore(method, param -> {
                        String source = CouiClockWakefulnessRoutingPolicy.aodExitArmSource();
                        PixelAodLog.log("COUI AOD exit continuity arm request rendererMode=COUI_PORT"
                                + " event=dispatchStartedWakingUp"
                                + " phase=before-wakefulness-observers"
                                + " source=" + source);
                        // WakefulnessLifecycle dispatch is a main-thread process-global callback
                        // in SystemUI. The active controller's main-thread path therefore arms
                        // the existing COUI host synchronously before observer/plugin work.
                        ActiveClockRendererController.prepareAodToLockscreenTransition(source);
                        clearPendingSleepOrigin(source + "#wake");
                    });
                    hooks++;
                    continue;
                }
                if (!"dispatchStartedGoingToSleep".equals(method.getName())) {
                    continue;
                }
                ModernHookBridge.hookBefore(method, param -> {
                    String source = "WakefulnessLifecycle#dispatchStartedGoingToSleep";
                    beginVendorScreenOffAnimationTransition(source);
                    Context context = systemUiContext;
                    boolean keyguardLocked = false;
                    boolean interactiveAtCallback = false;
                    if (context != null) {
                        interactiveAtCallback = PixelAodClockView.isDeviceInteractive(context);
                        try {
                            android.app.KeyguardManager keyguardManager =
                                    context.getSystemService(android.app.KeyguardManager.class);
                            keyguardLocked = keyguardManager != null
                                    && keyguardManager.isKeyguardLocked();
                        } catch (Throwable ignored) {
                        }
                    }
                    boolean recentInteractiveLockscreen = PixelLockscreenClockView
                            .wasRecentlyInteractiveLockscreenVisibleForAodEntry();
                    boolean lockscreenSurfaceVisible = PixelLockscreenClockView
                            .isLockscreenSurfaceVisible();
                    Boolean preKeyguardUnlocked = pendingSleepFromUnlockedOrNull();
                    boolean authoritativeSleepOrigin = preKeyguardUnlocked != null
                            && pendingSleepOriginAuthoritative;
                    boolean fromLockscreen = CouiClockAodEntryOriginPolicy.resolveFromLockscreen(
                            preKeyguardUnlocked,
                            keyguardLocked,
                            recentInteractiveLockscreen,
                            lockscreenSurfaceVisible,
                            interactiveAtCallback);
                    PixelAodLog.log("COUI AOD sleep origin rendererMode=COUI_PORT"
                            + " preKeyguardUnlocked=" + preKeyguardUnlocked
                            + " authoritative=" + authoritativeSleepOrigin
                            + " keyguardLocked=" + keyguardLocked
                            + " recentInteractiveLockscreen=" + recentInteractiveLockscreen
                            + " lockscreenSurfaceVisible=" + lockscreenSurfaceVisible
                            + " interactiveAtCallback=" + interactiveAtCallback
                            + " fromLockscreen=" + fromLockscreen);
                    PixelAodClockView.noteScreenOff(source, fromLockscreen);
                    if (!fromLockscreen) {
                        suppressKnownStockAodBeforeDream(source);
                        ActiveClockRendererController.prepareNonLockscreenAodEntry(source);
                    }
                });
                ModernHookBridge.hookAfter(method, param ->
                        refreshVendorScreenOffAnimationEligibility(
                                "WakefulnessLifecycle#dispatchStartedGoingToSleep#after-observers"));
                hooks++;
            }
            PixelAodLog.log("installed Wakefulness wake/sleep lifecycle hooks=" + hooks);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook Wakefulness wake/sleep lifecycle", t);
        }
    }

    static boolean nativeKeyguardSceneAllowsPresentation() {
        return NATIVE_KEYGUARD_SCENE_ELIGIBILITY.allowsPresentationFallbackTrue();
    }

    static boolean nativeKeyguardSceneSupportsNonLockscreenAodBypass() {
        return NATIVE_KEYGUARD_SCENE_ELIGIBILITY.snapshot().supportsNonLockscreenAodBypass();
    }

    static String describeNativeKeyguardSceneEligibility() {
        return NATIVE_KEYGUARD_SCENE_ELIGIBILITY.snapshot().describe();
    }

    static void hookNativeKeyguardTransitionSemantics(ClassLoader classLoader) {
        try {
            Class<?> repositoryClass = ModernHookBridge.findClass(
                    KEYGUARD_TRANSITION_REPOSITORY_IMPL, classLoader);
            Class<?> transitionStepClass = ModernHookBridge.findClass(
                    KEYGUARD_TRANSITION_STEP, classLoader);
            ModernHookBridge.hookAfter(repositoryClass, "emitTransition", param -> {
                if (param.args == null || param.args.length == 0) {
                    return;
                }
                observeNativeKeyguardTransitionStep(param.args[0],
                        "KeyguardTransitionRepositoryImpl#emitTransition");
            }, transitionStepClass, boolean.class);
            NATIVE_DOZE_TRANSITION_PROGRESS.markSeamAvailable(
                    "KeyguardTransitionRepositoryImpl#emitTransition");
            for (java.lang.reflect.Constructor<?> constructor
                    : repositoryClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    try {
                        Object step = ModernHookBridge.callMethod(
                                param.thisObject, "getCurrentTransitionStep");
                        observeNativeKeyguardTransitionStep(step,
                                "KeyguardTransitionRepositoryImpl#constructor");
                    } catch (Throwable t) {
                        PixelAodLog.log("failed to seed native Keyguard scene state", t);
                    }
                });
            }
            PixelAodLog.i("hooked native Keyguard transition semantics class="
                    + KEYGUARD_TRANSITION_REPOSITORY_IMPL);
        } catch (Throwable t) {
            PixelAodLog.log("native Keyguard transition semantics unavailable; using fallback", t);
        }
    }

    private static NativeKeyguardSceneEligibility.Snapshot observeNativeKeyguardTransitionStep(
            Object transitionStep, String source) {
        if (transitionStep == null) {
            return NATIVE_KEYGUARD_SCENE_ELIGIBILITY.snapshot();
        }
        try {
            Object from = ModernHookBridge.callMethod(transitionStep, "getFrom");
            Object to = ModernHookBridge.callMethod(transitionStep, "getTo");
            Object value = ModernHookBridge.callMethod(transitionStep, "getValue");
            Object phase = ModernHookBridge.callMethod(transitionStep, "getTransitionState");
            Object owner = ModernHookBridge.callMethod(transitionStep, "getOwnerName");
            String fromName = String.valueOf(from);
            String toName = String.valueOf(to);
            float transitionValue = value instanceof Number
                    ? ((Number) value).floatValue() : Float.NaN;
            String phaseName = String.valueOf(phase);
            String ownerName = String.valueOf(owner);
            NativeKeyguardSceneEligibility.Snapshot before =
                    NATIVE_KEYGUARD_SCENE_ELIGIBILITY.snapshot();
            NativeKeyguardSceneEligibility.Snapshot after =
                    NATIVE_KEYGUARD_SCENE_ELIGIBILITY.observe(
                            fromName,
                            toName,
                            transitionValue,
                            phaseName,
                            ownerName,
                            source);
            NativeDozeTransitionProgressAdapter.Snapshot progressAfter =
                    NATIVE_DOZE_TRANSITION_PROGRESS.observe(
                            fromName, toName, transitionValue, phaseName, ownerName, source);
            if (NativeKeyguardSceneEligibility.becameIneligible(
                    before.presentationAllowed, after.presentationAllowed)) {
                ActiveClockRendererController.suppressForNativeScene(source + "#ineligible");
            }
            if (NativeKeyguardSceneEligibility.becameEligible(
                    before.presentationAllowed, after.presentationAllowed)) {
                ActiveClockRendererController.resyncForNativeScene(source + "#eligible");
            }
            if (progressAfter.isRunningSample()) {
                PixelAodLog.log("native-doze-progress-sample", () ->
                        "native Doze transition progress sample {"
                                + progressAfter.describe(
                                        VENDOR_SCREEN_OFF_ANIMATION_ELIGIBILITY
                                                .allowsVendorProgress(),
                                        SystemAnimationScalePolicy.animationsEnabled())
                                + "}");
            }
            if (after.phase != NativeKeyguardSceneEligibility.Phase.RUNNING) {
                PixelAodLog.i("native Keyguard scene transition {" + after.describe() + "}"
                        + " screenOffEligibility={"
                        + describeVendorScreenOffAnimationEligibility() + "}"
                        + " dozeProgress={" + describeNativeDozeTransitionProgress() + "}"
                        + " systemAnimation={" + SystemAnimationScalePolicy.describe() + "}");
            }
            return after;
        } catch (Throwable t) {
            PixelAodLog.log("failed to observe native Keyguard transition step source=" + source, t);
            return NATIVE_KEYGUARD_SCENE_ELIGIBILITY.snapshot();
        }
    }

    static void hookKeyguardGoingAway(ClassLoader classLoader) {
        try {
            Class<?> controllerClass = ModernHookBridge.findClass(
                    KEYGUARD_STATE_CONTROLLER_IMPL, classLoader);
            ModernHookBridge.hookAfter(controllerClass, "notifyKeyguardGoingAway", param -> {
                if (param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Boolean)) {
                    return;
                }
                boolean goingAway = (Boolean) param.args[0];
                String source = "KeyguardStateControllerImpl#notifyKeyguardGoingAway(" + goingAway + ")";
                NativeDirectGoneHandoff.Snapshot handoff =
                        PixelAodClockView.observeNativeKeyguardGoingAway(goingAway, source);
                if (goingAway && handoff.active) {
                    PixelAodLog.i("native direct-to-Gone authoritative signal"
                            + " source=" + source
                            + " ambientEpoch=" + handoff.ambientEpoch
                            + " trace=" + PixelAodClockView.currentAodTraceId());
                    PixelLockscreenClockView.suppressForDirectGone(source);
                    ActiveClockRendererController.suppressForDirectGone(source);
                    PixelAodClockView.endVendorTransientAodPresentation(source + "#direct-gone");
                    PixelAodClockView.hideAllAodOverlays(source + "#direct-gone");
                } else if (!goingAway && PixelAodClockView.isVendorAmbientSessionActive()) {
                    // A cancelled going-away request while the same Dream remains valid returns
                    // lifecycle eligibility to that vendor ambient session. The next native
                    // ClockPlugin render remains authoritative for the actual visible scene.
                    PixelAodClockView.setAodActive(true, source + "#cancelled");
                    ActiveClockRendererController.refreshAll(source + "#cancelled");
                    reassertStockAodSuppressionAfterScreenOff(source + "#cancelled");
                }
            }, boolean.class);
            PixelAodLog.i("hooked native Keyguard going-away authority class="
                    + KEYGUARD_STATE_CONTROLLER_IMPL);
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook native Keyguard going-away authority", t);
        }
    }

    /**
     * Mirrors COUI 2.5's pre-Keyguard sleep-origin latch. KeyguardManager is already allowed to
     * report locked by dispatchStartedGoingToSleep on this OPlus build, so querying it there can
     * misclassify an unlocked desktop screen-off as a lockscreen-origin transition.
     */
    private static void hookKeyguardSleepOrigin(ClassLoader classLoader) {
        int discovered = 0;
        for (String className : KEYGUARD_SERVICE_BINDER_CLASSES) {
            try {
                hookKeyguardSleepBinderClass(ModernHookBridge.findClass(className, classLoader));
                discovered++;
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                PixelAodLog.log("failed static pre-Keyguard sleep binder hook class="
                        + className, t);
            }
        }
        try {
            Class<?> serviceClass = ModernHookBridge.findClass(KEYGUARD_SERVICE, classLoader);
            for (java.lang.reflect.Constructor<?> constructor : serviceClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    try {
                        Object binder = ModernHookBridge.getObjectField(param.thisObject, "mBinder");
                        if (binder != null) {
                            hookKeyguardSleepBinderClass(binder.getClass());
                        }
                    } catch (Throwable ignored) {
                    }
                });
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            PixelAodLog.log("failed dynamic pre-Keyguard sleep binder discovery", t);
        }
        PixelAodLog.log("pre-Keyguard sleep-origin hooks installed staticCandidates=" + discovered);
    }

    private static void hookKeyguardSleepBinderClass(Class<?> binderClass) {
        if (binderClass == null || !HOOKED_KEYGUARD_SLEEP_BINDERS.add(binderClass)) {
            return;
        }
        int hooks = 0;
        for (Method method : binderClass.getDeclaredMethods()) {
            String name = method.getName();
            if ("onStartedGoingToSleep".equals(name)) {
                ModernHookBridge.hookBefore(method,
                        param -> latchPreKeyguardSleepOrigin(param.thisObject, binderClass));
                hooks++;
            } else if ("onStartedWakingUp".equals(name)) {
                ModernHookBridge.hookBefore(method,
                        param -> clearPendingSleepOrigin("KeyguardService#onStartedWakingUp"));
                hooks++;
            }
        }
        PixelAodLog.log("pre-Keyguard sleep binder hooked class=" + binderClass.getName()
                + " methods=" + hooks);
    }

    private static void latchPreKeyguardSleepOrigin(Object binder, Class<?> binderClass) {
        try {
            Object service = ModernHookBridge.getObjectField(binder, "this$0");
            Object mediator = ModernHookBridge.getObjectField(service, "mKeyguardViewMediator");
            Object showingValue = ModernHookBridge.getObjectField(mediator, "mShowing");
            if (!(showingValue instanceof Boolean)) {
                return;
            }
            boolean showing = (Boolean) showingValue;
            pendingSleepFromUnlocked = !showing;
            pendingSleepOriginLatchedAt = SystemClock.uptimeMillis();
            pendingSleepOriginAuthoritative = true;
            PixelAodLog.log("pre-Keyguard sleep origin latched rendererMode=COUI_PORT"
                    + " unlocked=" + pendingSleepFromUnlocked
                    + " keyguardShowing=" + showing
                    + " binder=" + binderClass.getName());
            if (pendingSleepFromUnlocked) {
                String source = "KeyguardService#onStartedGoingToSleep";
                PixelAodLog.log("COUI early non-lockscreen AOD pre-arm request"
                        + " rendererMode=COUI_PORT"
                        + " phase=pre-keyguard-binder"
                        + " source=" + source);
                ActiveClockRendererController.prepareNonLockscreenAodEntryEarly(source);
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to latch pre-Keyguard sleep origin class="
                    + binderClass.getName(), t);
        }
    }

    private static Boolean pendingSleepFromUnlockedOrNull() {
        long latchedAt = pendingSleepOriginLatchedAt;
        if (latchedAt == Long.MIN_VALUE) {
            return null;
        }
        long age = SystemClock.uptimeMillis() - latchedAt;
        if (age < 0L || age > KEYGUARD_SLEEP_ORIGIN_FRESH_MILLIS) {
            clearPendingSleepOrigin("stale");
            return null;
        }
        return pendingSleepFromUnlocked;
    }

    private static void clearPendingSleepOrigin(String source) {
        boolean had = pendingSleepOriginLatchedAt != Long.MIN_VALUE;
        pendingSleepFromUnlocked = false;
        pendingSleepOriginAuthoritative = false;
        pendingSleepOriginLatchedAt = Long.MIN_VALUE;
        if (had) {
            PixelAodLog.log("cleared pre-Keyguard sleep origin source=" + source);
        }
    }

    static boolean isFodNativeTimeoutHideLatched(Context context) {
        if (context == null) {
            return false;
        }
        boolean interactive = PixelAodClockView.isDeviceInteractive(context);
        if (interactive) {
            clearFodNativeTimeoutHide("interactive");
            return false;
        }
        String currentTrace = PixelAodClockView.peekAodTraceId();
        boolean latched = FOD_NATIVE_TIMEOUT_HIDE_GATE.shouldPreserveNativeHide(
                currentTrace, false);
        String hiddenTrace = FOD_NATIVE_TIMEOUT_HIDE_GATE.hiddenTrace();
        if (!latched && !TextUtils.isEmpty(hiddenTrace)
                && !TextUtils.equals(hiddenTrace, currentTrace)) {
            clearFodNativeTimeoutHide("aod-trace-changed");
        }
        return latched;
    }

    static void clearFodNativeTimeoutHide(String source) {
        String hiddenTrace = FOD_NATIVE_TIMEOUT_HIDE_GATE.hiddenTrace();
        long hiddenAgeMs = FOD_NATIVE_TIMEOUT_HIDE_GATE.hiddenAgeMillis(SystemClock.uptimeMillis());
        if (!FOD_NATIVE_TIMEOUT_HIDE_GATE.clear()) {
            return;
        }
        PixelAodLog.log("cleared native FOD timeout-hide latch"
                + " source=" + source
                + " hiddenTrace=" + hiddenTrace
                + " hiddenAgeMs=" + hiddenAgeMs
                + " currentTrace=" + PixelAodClockView.peekAodTraceId());
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

    static void hookLockscreenVisibilityObservers(ClassLoader classLoader) {
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
                if (hidden && shouldCorrectOosLockscreenVisibility(sbn, ranking, source)) {
                    param.setResult(false);
                    hidden = false;
                }
                PixelAodClockView.updateLockscreenVisibilityFromProvider(
                        sbn, hidden, source);
            }, entryClass);
            PixelAodLog.log("hooked KeyguardNotificationVisibilityProvider OOS compatibility observer");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook KeyguardNotificationVisibilityProvider read-only visibility observer", t);
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
                PixelAodLog.log("skipped abstract keyguard NotifFilter visibility observer");
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
                if (hidden && shouldCorrectOosLockscreenVisibility(sbn, ranking, source)) {
                    param.setResult(false);
                    hidden = false;
                }
                PixelAodClockView.updateLockscreenVisibilityFromFilter(sbn, hidden, source);
            }, entryClass, long.class);
            PixelAodLog.log("hooked keyguard NotifFilter OOS compatibility fallback");
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook keyguard NotifFilter read-only visibility observer", t);
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

    private static boolean shouldCorrectOosLockscreenVisibility(StatusBarNotification sbn,
            Object ranking, String source) {
        Context context = systemUiContext;
        if (context == null
                || !PixelAodSettings.getBoolean(context,
                        PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true)
                || !AodNotificationPipeline.lockscreenNotificationsEnabled(context)
                || sbn == null
                || sbn.getNotification() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        boolean testNotification = AodNotificationPipeline.isTestNotification(sbn);
        boolean rankingSecret = rankingVisibilitySecret(ranking);
        int importance = rankingImportance(ranking);
        boolean mediaCandidate = AodNotificationPipeline.hasMediaSessionExtra(notification)
                || AodNotificationPipeline.isMediaIconCandidate(sbn);
        boolean eligible = AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                sbn.getPackageName(),
                testNotification,
                notification.getSmallIcon() != null,
                notification.category,
                notification.visibility,
                rankingSecret,
                mediaCandidate,
                importance);
        if (eligible) {
            PixelAodLog.log("correcting OOS lockscreen notification visibility pkg="
                    + sbn.getPackageName()
                    + " key=" + sbn.getKey()
                    + " source=" + source
                    + " importance=" + importance
                    + " category=" + notification.category
                    + " visibility=" + notification.visibility
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
        return eligible;
    }

    private static boolean rankingVisibilitySecret(Object ranking) {
        if (ranking == null) {
            return false;
        }
        try {
            Object override = ModernHookBridge.callMethod(ranking,
                    "getLockscreenVisibilityOverride");
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
            return AodNotificationPipeline.NotificationManagerImportance.UNKNOWN;
        }
        try {
            Object value = ModernHookBridge.callMethod(ranking, "getImportance");
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Throwable ignored) {
            // Best-effort only.
        }
        return AodNotificationPipeline.NotificationManagerImportance.UNKNOWN;
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
            int displayId = PrimaryDisplayPolicy.displayId(host);
            if (!PrimaryDisplayPolicy.isPrimaryDisplayId(displayId)) {
                PixelAodLog.log("ignored AOD outer root reason=non-primary-display"
                        + " displayId=" + displayId + " source=" + source);
                return;
            }
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
        if (str.contains("motion") || str.contains("significant") || str.contains("amd")) {
            return "motion";
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
            int displayId = PrimaryDisplayPolicy.displayId(host);
            if (!PrimaryDisplayPolicy.isPrimaryDisplayId(displayId)) {
                PixelAodLog.log("skipped Pixel AOD host reason=non-primary-display"
                        + " displayId=" + displayId + " source=" + source);
                return;
            }
            ViewGroup pixelHost = findPixelClockInjectionHost(host);
            lastStockHost = new WeakReference<>(host);
            observeStockAodHostLifecycle(host, source);
            lastPixelHost = new WeakReference<>(pixelHost);
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
            if (screenOff) {
                // OOS 16.0.9 does not deliver ACTION_SCREEN_OFF to SystemUI on this path.
                // Schedule the same three suppression passes from the confirmed native AOD
                // host only after it has established the current AOD trace.
                reassertStockAodSuppressionAfterScreenOff(source + "#host-ready");
            }
            if (screenOff || customizeNow) {
                refreshNotificationsFromLastListener(source);
                hideStockClockViews(host);
                adjustPluginStatusViews(context, host);
                if (ENABLE_EXPENSIVE_DEBUG_DUMPS && PixelAodLog.isDebugEnabled()) {
                    scheduleParentDebugDumps(host, source);
                }
            } else if (lockscreenVisible) {
                hideStockKeyguardClockViews(highestParentGroup(host));
            } else {
                restoreStockViews(source + "#host-interactive-exit");
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
            int displayId = PrimaryDisplayPolicy.displayId(host);
            if (!PrimaryDisplayPolicy.isPrimaryDisplayId(displayId)) {
                PixelAodLog.log("skipped Pixel lockscreen host reason=non-primary-display"
                        + " displayId=" + displayId + " source=" + source);
                return;
            }
            if (PixelAodClockView.isDirectGoneHandoffActive()) {
                PixelLockscreenClockView.suppressForDirectGone(source + "#direct-gone");
                ActiveClockRendererController.suppressForDirectGone(source + "#direct-gone");
                PixelAodLog.log("skipped Pixel lockscreen host during native direct-to-Gone"
                        + " source=" + source
                        + " host=" + hostSummary(host)
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                return;
            }
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
                    restoreStockViews(source + "#interactive-unlocked");
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
            boolean surfaceVisible = isLikelyLockscreenSurfaceVisible(context, host, host);
            PixelLockscreenClockView.setLockscreenSurfaceVisible(surfaceVisible,
                    source + "#persistent-host");
            if (surfaceVisible) {
                ActiveClockRendererController.prepareLockscreenEntry(
                        source + "#persistent-host");
                PixelLockscreenClockView.markInteractiveLockscreenSurface(context,
                        source + "#persistent-host");
                hideStockKeyguardClockViews(highestParentGroup(host));
            }
            PixelAodLog.log("prepared persistent COUI ClockPlugin lockscreen host from " + source
                    + " surfaceVisible=" + surfaceVisible
                    + " host=" + markerFor(host)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("prepare Pixel lockscreen host failed from " + source, t);
        }
    }

    static void reapplyLockscreenClockFromKnownHost(String source) {
        // COUI owns the persistent lockscreen host; legacy overlay reapply is retired.
    }

    static void reassertStockAodSuppressionAfterScreenOff(String source) {
        final String expectedTrace = PixelAodClockView.peekAodTraceId();
        final boolean epochScoped = PixelAodClockView.isVendorAmbientSessionActive();
        final long expectedEpoch = PixelAodClockView.currentVendorAmbientEpoch();
        if (TextUtils.isEmpty(expectedTrace)
                || TextUtils.equals(expectedTrace, lastScreenOffStockSuppressionTrace)) {
            return;
        }
        lastScreenOffStockSuppressionTrace = expectedTrace;
        for (long delayMillis : SCREEN_OFF_STOCK_SUPPRESSION_REASSERT_DELAYS_MILLIS) {
            String passSource = source + "#stock-suppression-reapply-" + delayMillis + "ms";
            MAIN.postDelayed(() -> {
                if (epochScoped
                        && !PixelAodClockView.isCurrentVendorAmbientEpoch(expectedEpoch)) {
                    PixelAodLog.log("skipped stale stock AOD suppression reapply source="
                            + passSource
                            + " expectedEpoch=" + expectedEpoch
                            + " currentEpoch=" + PixelAodClockView.currentVendorAmbientEpoch());
                    return;
                }
                refreshKnownAodHostVisibility(passSource, expectedTrace);
            }, delayMillis);
        }
        PixelAodLog.log("scheduled screen-off stock AOD suppression reapply source=" + source
                + " expectedTrace=" + expectedTrace
                + " delaysMs=" + java.util.Arrays.toString(
                SCREEN_OFF_STOCK_SUPPRESSION_REASSERT_DELAYS_MILLIS));
    }

    private static void suppressKnownStockAodBeforeDream(String source) {
        runAtFrontOfMain(() -> {
            ViewGroup stockHost = lastStockHost.get();
            Context context = stockHost != null ? stockHost.getContext() : systemUiContext;
            if (stockHost == null || context == null
                    || !PixelAodClockView.isContinuousAodConfiguredForEntry(
                    context, source + "#pre-dream-stock-suppression")) {
                PixelAodLog.log("skipped pre-dream stock AOD suppression source=" + source
                        + " stockHost=" + hostSummary(stockHost)
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                return;
            }
            hideStockClockViews(stockHost);
            hideStockKeyguardClockViews(highestParentGroup(stockHost));
            adjustPluginStatusViews(context, stockHost);
            PixelAodLog.log("suppressed known stock AOD before Dream source=" + source
                    + " stockHost=" + hostSummary(stockHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
        });
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
                    + " owner=COUI_PORT"
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

    private static void applyLockscreenClockReplacement(Context context, ViewGroup stockHost,
            ViewGroup pixelHost, String source) {
        ViewGroup displayOwner = pixelHost != null ? pixelHost : stockHost;
        int displayId = PrimaryDisplayPolicy.displayId(displayOwner);
        if (!PrimaryDisplayPolicy.isPrimaryDisplayId(displayId)) {
            PixelAodLog.log("lockscreen replacement skipped reason=non-primary-display"
                    + " displayId=" + displayId + " source=" + source);
            return;
        }
        boolean surfaceVisible = isLikelyLockscreenSurfaceVisible(context, stockHost, pixelHost);
        PixelLockscreenClockView.setLockscreenSurfaceVisible(surfaceVisible, source);
        if (!surfaceVisible) {
            PixelAodLog.log("lockscreen replacement skipped from " + source
                    + " surfaceVisible=false stockHost=" + hostSummary(stockHost)
                    + " pixelHost=" + hostSummary(pixelHost)
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            restoreStockViews(source + "#lockscreen-surface-hidden");
            return;
        }
        if (stockHost != null) {
            hideStockKeyguardClockViews(highestParentGroup(stockHost));
        }
        if (pixelHost != null) {
            hideStockKeyguardClockViews(highestParentGroup(pixelHost));
        }
        PixelAodLog.log("lockscreen replacement delegated to persistent COUI ClockPlugin host from "
                + source
                + " stockHost=" + hostSummary(stockHost)
                + " pixelHost=" + hostSummary(pixelHost)
                + " trace=" + PixelAodClockView.currentAodTraceId()
                + " state={" + PixelAodClockView.describeAodState(context) + "}");
    }

    private static boolean isLikelyLockscreenSurfaceVisible(Context context, ViewGroup stockHost,
            ViewGroup pixelHost) {
        return PixelLockscreenClockView.isSystemKeyguardLocked(context);
    }

    private static void adjustPluginStatusViews(Context context, ViewGroup root) {
        if (!PrimaryDisplayPolicy.isPrimary(root)) {
            return;
        }
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
                        restoreStockViews(source + "#parent-dump-interactive-exit");
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
        if (PixelAodClockView.isDirectGoneHandoffActive()) {
            PixelLockscreenClockView.suppressForDirectGone(source + "#direct-gone");
            ActiveClockRendererController.suppressForDirectGone(source + "#direct-gone");
            PixelAodLog.log("skipped remembered-host lockscreen replacement during native direct-to-Gone"
                    + " source=" + source
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
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
                (stockHost, pixelHost) -> deferStockAodRestoreIfRetiring(
                        stockHost, source + "#transition-restore-guard"),
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
        if (!PrimaryDisplayPolicy.isPrimary(root)) {
            return;
        }
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
        if (!PrimaryDisplayPolicy.isPrimary(root)) {
            return;
        }
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
        if (view instanceof CouiClockHostView) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        return containsPersistentClockPluginHost((ViewGroup) view, 0);
    }

    private static boolean containsPersistentClockPluginHost(ViewGroup root, int depth) {
        if (root instanceof CouiClockHostView) {
            return true;
        }
        if (depth >= 12) {
            return false;
        }
        int childCount = Math.min(root.getChildCount(), 120);
        for (int i = 0; i < childCount; i++) {
            View child = root.getChildAt(i);
            if (child instanceof CouiClockHostView) {
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

    private static void observeStockAodHostLifecycle(ViewGroup host, String source) {
        if (host == null || !isAodRootLayout(host) || isChargingUiView(host)) {
            return;
        }
        String trace = PixelAodClockView.currentAodTraceId();
        synchronized (STOCK_AOD_HOST_TRACES) {
            STOCK_AOD_HOST_TRACES.put(host, trace);
        }
        boolean firstObservation;
        synchronized (OBSERVED_STOCK_AOD_HOSTS) {
            firstObservation = OBSERVED_STOCK_AOD_HOSTS.add(host);
        }
        if (!firstObservation) {
            return;
        }
        host.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                String currentTrace = PixelAodClockView.currentAodTraceId();
                synchronized (STOCK_AOD_HOST_TRACES) {
                    STOCK_AOD_HOST_TRACES.put(v, currentTrace);
                }
                PixelAodLog.log("observed stock AOD host attach source=" + source
                        + " host=" + markerFor(v)
                        + " trace=" + currentTrace);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                String expectedTrace;
                synchronized (STOCK_AOD_HOST_TRACES) {
                    expectedTrace = STOCK_AOD_HOST_TRACES.remove(v);
                }
                synchronized (OBSERVED_STOCK_AOD_HOSTS) {
                    OBSERVED_STOCK_AOD_HOSTS.remove(v);
                }
                v.removeOnAttachStateChangeListener(this);
                if (TextUtils.isEmpty(expectedTrace)) {
                    expectedTrace = PixelAodClockView.peekAodTraceId();
                }
                PixelAodLog.log("observed stock AOD host detach source=" + source
                        + " host=" + markerFor(v)
                        + " expectedTrace=" + expectedTrace
                        + " currentTrace=" + PixelAodClockView.peekAodTraceId());
                restoreHiddenStockViewsAfterTransition(
                        source + "#stock-aod-host-detached", expectedTrace);
            }
        });
    }

    private static boolean deferStockAodRestoreIfRetiring(String source) {
        return deferStockAodRestoreIfRetiring(lastStockHost.get(), source);
    }

    private static boolean deferStockAodRestoreIfRetiring(ViewGroup stockHost, String source) {
        if (stockHost == null) {
            return false;
        }
        String hostTrace;
        synchronized (STOCK_AOD_HOST_TRACES) {
            hostTrace = STOCK_AOD_HOST_TRACES.get(stockHost);
        }
        String currentTrace = PixelAodClockView.peekAodTraceId();
        boolean sameAodTrace = !TextUtils.isEmpty(hostTrace)
                && TextUtils.equals(hostTrace, currentTrace);
        boolean attachedOrParented = stockHost.isAttachedToWindow()
                || stockHost.getParent() != null;
        Context context = stockHost.getContext();
        boolean interactive = PixelAodClockView.isDeviceInteractive(context);
        if (!StockAodExitRestoreGate.shouldDeferRestore(interactive,
                isAodRootLayout(stockHost), attachedOrParented, sameAodTrace)) {
            return false;
        }

        // UDFPS can flip PowerManager to interactive before OPlus detaches its AOD APK root.
        // Keep the retiring native AOD subtree suppressed until that concrete host leaves the
        // hierarchy; otherwise restoring its original NotificationView alpha/visibility can
        // expose stock notification glyphs for a scanout frame.
        hideStockClockViews(stockHost);
        adjustPluginStatusViews(context, stockHost);
        hideStockKeyguardClockViews(highestParentGroup(stockHost));
        PixelAodLog.log("deferred restoring stock AOD views source=" + source
                + " reason=current-aod-host-still-attached"
                + " stockHost=" + hostSummary(stockHost)
                + " hostTrace=" + hostTrace
                + " currentTrace=" + currentTrace
                + " state={" + PixelAodClockView.describeAodState(context) + "}");
        return true;
    }

    private static void restoreStockViews(String source) {
        if (deferStockAodRestoreIfRetiring(source)) {
            return;
        }
        restoreAdjustedStatusViews();
        StockAodVisibilityController.restoreHiddenStockViews();
    }

    static void restoreSystemViewsForLockscreen(String source) {
        MAIN.post(() -> {
            PixelLockscreenClockView.refreshAll(source);
            if (PixelLockscreenClockView.shouldShowOnKnownContext()) {
                restoreAdjustedStatusViews();
                PixelAodLog.log("kept stock keyguard clock hidden for Pixel lockscreen from " + source);
                return;
            }
            restoreStockViews(source + "#system-lockscreen");
            PixelAodLog.log("restored system lockscreen views from " + source);
        });
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
