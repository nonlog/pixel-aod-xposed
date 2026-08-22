package dev.codex.pixelaod;

import android.content.Context;

/**
 * Runtime lifecycle/state facade shared by COUI presentation and SystemUI adapters.
 *
 * <p>M8 first removes callers' dependency on the legacy View class. The proven state machine stays
 * behind this facade until a later implementation move can be demonstrated as behavior-neutral.</p>
 */
final class PixelAodRuntimeState {
    private PixelAodRuntimeState() {
    }

    static boolean isDeviceInteractive(Context context) {
        return PixelAodClockView.isDeviceInteractive(context);
    }

    static String currentAodTraceId() {
        return PixelAodClockView.currentAodTraceId();
    }

    static String peekAodTraceId() {
        return PixelAodClockView.peekAodTraceId();
    }

    static String describeAodState(Context context) {
        return PixelAodClockView.describeAodState(context);
    }

    static String describeAodState(Context context, boolean compact, int weight) {
        return PixelAodClockView.describeAodState(context, compact, weight);
    }

    static boolean wasScreenOffFromInteractiveLockscreen() {
        return PixelAodClockView.wasScreenOffFromInteractiveLockscreen();
    }

    static void noteLockscreenSessionForAodWeight(String source) {
        PixelAodClockView.noteLockscreenSessionForAodWeight(source);
    }

    static void clearLockscreenSessionForAodWeight(String source) {
        PixelAodClockView.clearLockscreenSessionForAodWeight(source);
    }

    static boolean shouldAnimateLockscreenToAodWeight() {
        return PixelAodClockView.shouldAnimateLockscreenToAodWeight();
    }

    static boolean isInAodEntryTransitionWindow(long windowMillis) {
        return PixelAodClockView.isInAodEntryTransitionWindow(windowMillis);
    }

    static boolean shouldBridgeLockscreenDuringAodEntry(Context context, long windowMillis) {
        return PixelAodClockView.shouldBridgeLockscreenDuringAodEntry(context, windowMillis);
    }

    static PixelAodClockView.BurnInOffset consumeRecentBurnInOffset(long windowMillis) {
        return PixelAodClockView.consumeRecentBurnInOffset(windowMillis);
    }
}
