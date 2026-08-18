package dev.codex.pixelaod;

/** Pure gate for the vendor pressed-icon animation overrides. */
final class CouiUdfpsPressedAnimationPolicy {
    private CouiUdfpsPressedAnimationPolicy() {
    }

    static boolean shouldSuppress(boolean replacementEnabled, boolean hdrEnabled) {
        // COUI suppresses the vendor pressed animation whenever the replacement icon feature is
        // active. HDR only selects the press visual implementation; it does not control this hook.
        return replacementEnabled;
    }

    static Boolean overrideHasPressedAnimation(boolean replacementEnabled, boolean hdrEnabled) {
        return shouldSuppress(replacementEnabled, hdrEnabled) ? Boolean.FALSE : null;
    }

    static Float overrideScalePressedAnim(boolean replacementEnabled, boolean hdrEnabled) {
        return shouldSuppress(replacementEnabled, hdrEnabled) ? Float.valueOf(1f) : null;
    }
}
