package dev.codex.pixelaod;

/** Ownership boundary for the M7 system-icon + custom-success-ripple UDFPS mode. */
final class CouiUdfpsOwnershipPolicy {
    private CouiUdfpsOwnershipPolicy() {
    }

    static boolean ownsReplacementVisuals(boolean couiRenderer, boolean replacementEnabled) {
        return couiRenderer && replacementEnabled;
    }

    static boolean ownsSuccessRipple(boolean couiRenderer, boolean successRippleEnabled) {
        return couiRenderer && successRippleEnabled;
    }

    static boolean mayMutateVendorVisuals(boolean replacementOwned,
            boolean trackedReplacementState) {
        return replacementOwned || trackedReplacementState;
    }

    static boolean suppressStockRipple(boolean customSuccessOwned, boolean targetAvailable,
            boolean replacementOwned, boolean unlockedRippleMethod) {
        if (!customSuccessOwned || !targetAvailable) {
            return false;
        }
        // Replacement mode keeps the historical COUI behavior. System-icon mode preserves the
        // native dwell/press feedback and suppresses only the native unlock-success ripple.
        return replacementOwned || unlockedRippleMethod;
    }
}
