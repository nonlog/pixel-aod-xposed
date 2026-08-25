package dev.codex.pixelaod;

import android.content.Context;

/** Owns registration of AOD/process lifecycle hooks while implementations remain in PixelAodHook. */
final class PixelAodLifecycleHookInstaller {
    private PixelAodLifecycleHookInstaller() {
    }

    static void registerSettingsObserver(Context context) {
        PixelAodHook.registerSettingsObserver(context);
    }

    static void installScreenOffAnimationEligibility(ClassLoader classLoader) {
        PixelAodHook.hookVendorScreenOffAnimationEligibility(classLoader);
    }

    static void installAmbientSuppressionCapabilities(ClassLoader classLoader) {
        PixelAodHook.hookVendorAmbientSuppressionCapabilities(classLoader);
    }

    static void installNativeSmartspacePassThrough(ClassLoader classLoader) {
        PixelAodHook.hookNativeSmartspaceContextualPassThrough(classLoader);
    }

    static void installNativeLiveAlertAodPassThrough(ClassLoader classLoader) {
        PixelAodHook.hookNativeLiveAlertAodPassThrough(classLoader);
    }

    static void installSelectiveBiometricPulseSemantics(ClassLoader classLoader) {
        PixelAodHook.hookSelectiveBiometricPulseSemantics(classLoader);
    }

    static void installNativeKeyguardTransitionSemantics(ClassLoader classLoader) {
        PixelAodHook.hookNativeKeyguardTransitionSemantics(classLoader);
    }

    static void installWakefulness(ClassLoader classLoader) {
        PixelAodHook.hookWakefulnessLifecycle(classLoader);
    }

    static void installKeyguardGoingAway(ClassLoader classLoader) {
        PixelAodHook.hookKeyguardGoingAway(classLoader);
    }

    static void installNativeAodRefresh(ClassLoader classLoader) {
        PixelAodHook.hookNativeAodRefreshCallbacks(classLoader);
    }

    static void installAodRecord(ClassLoader classLoader) {
        PixelAodHook.hookAodRecord(classLoader);
    }

    static void installEnergySavingObservers(ClassLoader classLoader) {
        PixelAodHook.hookOplusEnergySavingHideObservers(classLoader);
    }

    static void installVendorProximityPauseSemantics(ClassLoader classLoader) {
        PixelAodHook.hookOplusVendorProximityPauseSemantics(classLoader);
    }

    static void installVendorWakeTriggerSemantics(ClassLoader classLoader) {
        PixelAodHook.hookOplusVendorWakeTriggerSemantics(classLoader);
    }

    static void installAodTriggerDiagnostics(ClassLoader classLoader) {
        PixelAodHook.hookOplusAodTriggerDiagnostics(classLoader);
    }

    static void installPowerWakeTriggers() {
        PixelAodHook.hookPowerManagerWakeTriggers();
    }

    static void installDreamDozeStateObserver() {
        PixelAodHook.hookDreamServiceDozeScreenStateObserver();
    }
}
