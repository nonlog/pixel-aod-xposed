package dev.codex.pixelaod;

import android.content.Context;

/** Owns registration of AOD/process lifecycle hooks while implementations remain in PixelAodHook. */
final class PixelAodLifecycleHookInstaller {
    private PixelAodLifecycleHookInstaller() {
    }

    static void registerSettingsObserver(Context context) {
        PixelAodHook.registerSettingsObserver(context);
    }

    static void installWakefulness(ClassLoader classLoader) {
        PixelAodHook.hookWakefulnessLifecycle(classLoader);
    }

    static void installNativeAodRefresh(ClassLoader classLoader) {
        PixelAodHook.hookNativeAodRefreshCallbacks(classLoader);
    }

    static void installAodRecord(ClassLoader classLoader) {
        PixelAodHook.hookAodRecord(classLoader);
    }

    static void installEnergySavingGuards(ClassLoader classLoader) {
        PixelAodHook.hookOplusEnergySavingHideGuards(classLoader);
    }

    static void installAodTriggerDiagnostics(ClassLoader classLoader) {
        PixelAodHook.hookOplusAodTriggerDiagnostics(classLoader);
    }

    static void installPowerWakeTriggers() {
        PixelAodHook.hookPowerManagerWakeTriggers();
    }

    static void installDreamDozeState() {
        PixelAodHook.hookDreamServiceDozeScreenState();
    }
}
