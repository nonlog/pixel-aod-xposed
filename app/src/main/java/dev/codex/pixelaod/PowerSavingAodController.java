package dev.codex.pixelaod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** Extends only the native OPlus Power Saving AOD lifecycle for user-selected exceptions. */
final class PowerSavingAodController {
    private static final String CLOCK_LAYOUT =
            "com.oplus.systemui.aod.aodclock.off.AodClockLayout";
    private static final String UPDATE_MANAGER =
            "com.oplus.systemui.aod.aodclock.off.AodUpdateManager";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile Context appContext;
    private static volatile boolean batteryStateKnown;
    private static volatile boolean plugged;
    private static volatile boolean nativeTimeoutHeld;
    private static volatile WeakReference<Object> lastClockLayout = new WeakReference<>(null);
    private static volatile WeakReference<Object> lastUpdateManager = new WeakReference<>(null);

    private PowerSavingAodController() {
    }

    static void install(Context context, ClassLoader loader) {
        if (context == null || loader == null || !INSTALLED.compareAndSet(false, true)) {
            return;
        }
        appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        hookClockLayout(loader);
        hookUpdateManager(loader);
        registerBatteryReceiver(appContext);
        onPolicyChanged("install");
    }

    static boolean isPlugged() {
        return plugged;
    }

    static boolean isChargingHoldRequested(Context context) {
        Context ctx = context != null ? context : appContext;
        if (ctx == null) {
            return false;
        }
        NativeAodAvailabilityAdapter.Decision nativeAod = NativeAodAvailabilityAdapter.read(
                ctx, PixelAodClockView.isVendorAmbientSessionActive());
        return PowerSavingAodPolicy.isChargingHoldRequested(
                PixelAodSettings.getBoolean(ctx,
                        PixelAodSettings.KEY_POWER_SAVING_CHARGING_AOD, true),
                nativeAod.displayMode, nativeAod.configuredEligible, plugged);
    }

    static void onPolicyChanged(String source) {
        runOnMain(() -> reconcile(source));
    }

    static void requestNotificationShow(String source) {
        runOnMain(() -> {
            Context ctx = appContext;
            if (ctx == null) {
                return;
            }
            NativeAodAvailabilityAdapter.Decision nativeAod = NativeAodAvailabilityAdapter.read(
                    ctx, PixelAodClockView.isVendorAmbientSessionActive());
            if (!PowerSavingAodPolicy.isEnergySavingMode(nativeAod.displayMode)
                    || !nativeAod.configuredEligible
                    || PixelAodClockView.isDeviceInteractive(ctx)) {
                return;
            }
            showNative("notification#" + source);
        });
    }

    /** Called only at the vendor's actual energy-saving timeout callback. */
    static boolean shouldHoldNativeEnergySavingTimeout(Context context, String source) {
        Context ctx = context != null ? context : appContext;
        return ctx != null && PixelAodClockView.shouldKeepPowerSavingAodVisibleForCharging(
                ctx, "energy-saving-timeout#" + source);
    }

    static void onNativeEnergySavingTimeoutHeld(String source) {
        nativeTimeoutHeld = true;
        PixelAodLog.i("held native Power Saving AOD timeout while charging source=" + source);
    }

    static void onNativeDozeScreenOffHeld(String source) {
        nativeTimeoutHeld = true;
        PixelAodLog.i("held native Power Saving AOD screen-off while charging source=" + source);
    }

    private static void hookClockLayout(ClassLoader loader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(CLOCK_LAYOUT, loader);
            ModernHookBridge.hookAfter(clazz, "onAttachedToWindow", param -> {
                lastClockLayout = new WeakReference<>(param.thisObject);
                reconcile("clock-layout-attach");
            });
            ModernHookBridge.hookAfter(clazz, "showClock",
                    param -> lastClockLayout = new WeakReference<>(param.thisObject), int.class);
        } catch (Throwable t) {
            PixelAodLog.log("failed Power Saving AOD clock hooks", t);
        }
    }

    private static void hookUpdateManager(ClassLoader loader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(UPDATE_MANAGER, loader);
            // Keep the vendor alarm intact. It is also the authoritative deadline used to retire
            // transient AOD affordances such as the fingerprint icon. Charging is handled later,
            // at the exact energy-saving display callback, rather than deleting this timer.
            ModernHookBridge.hookAfter(clazz, "setHideAlarm",
                    param -> lastUpdateManager = new WeakReference<>(param.thisObject));
        } catch (Throwable t) {
            PixelAodLog.log("failed Power Saving AOD hide-alarm observer", t);
        }
    }

    private static void registerBatteryReceiver(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                handleBattery(intent, "battery");
            }
        };
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent sticky;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sticky = context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                sticky = context.registerReceiver(receiver, filter);
            }
            if (sticky != null) {
                handleBattery(sticky, "battery-sticky");
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed Power Saving AOD battery observer", t);
        }
    }

    private static void handleBattery(Intent intent, String source) {
        if (intent == null) {
            return;
        }
        boolean next = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        boolean changed = !batteryStateKnown || plugged != next;
        batteryStateKnown = true;
        plugged = next;
        if (!changed) {
            return;
        }
        PixelAodLog.i("Power Saving AOD charging edge plugged=" + next + " source=" + source);
        reconcile(source);
    }

    private static void reconcile(String source) {
        Context ctx = appContext;
        if (ctx == null) {
            return;
        }
        boolean timeoutHeld = nativeTimeoutHeld;
        boolean hold = PixelAodClockView.shouldKeepPowerSavingAodVisibleForCharging(
                ctx, "reconcile#" + source);
        if (hold) {
            showNative(source);
            PixelAodClockView.refreshNativeAodEligibility("charging-hold#" + source);
            return;
        }
        // Eligibility alone must not reset the vendor deadline. Only restore native hide
        // scheduling after a timeout actually fired and we suppressed its full-AOD hide.
        nativeTimeoutHeld = false;
        NativeAodAvailabilityAdapter.Decision nativeAod = NativeAodAvailabilityAdapter.read(
                ctx, PixelAodClockView.isVendorAmbientSessionActive());
        if (PowerSavingAodPolicy.shouldReapplyNativeHide(timeoutHeld,
                nativeAod.displayMode, nativeAod.configuredEligible,
                PixelAodClockView.isDeviceInteractive(ctx))) {
            reapplyNativeHide(source);
        }
    }

    private static void showNative(String source) {
        Object clockLayout = lastClockLayout.get();
        if (clockLayout == null) {
            PixelAodLog.log("Power Saving AOD waiting for native clock layout source=" + source);
            return;
        }
        try {
            ModernHookBridge.callMethod(clockLayout, "showClock", 0);
            PixelAodLog.i("requested native Power Saving AOD show source=" + source);
        } catch (Throwable t) {
            PixelAodLog.log("failed native Power Saving AOD show", t);
        }
    }

    private static void reapplyNativeHide(String source) {
        Object updateManager = lastUpdateManager.get();
        if (updateManager == null) {
            PixelAodLog.log("Power Saving native hide restore deferred source=" + source);
            return;
        }
        try {
            ModernHookBridge.callMethod(updateManager, "setHideAlarm");
            PixelAodLog.i("restored native Power Saving AOD hide source=" + source);
        } catch (Throwable t) {
            PixelAodLog.log("failed native Power Saving AOD hide restore", t);
        }
    }

    private static void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN.post(runnable);
        }
    }
}
