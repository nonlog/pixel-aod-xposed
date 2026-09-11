package dev.codex.pixelaod;

/** Pure policy for matching the system UDFPS glyph to the native AOD reveal surface. */
final class AodFingerprintRevealSyncPolicy {
    static final float COMPLETE_PROGRESS = 0.995f;

    private AodFingerprintRevealSyncPolicy() {
    }

    static boolean shouldSynchronize(boolean moduleEnabled, boolean customAodEnabled,
            boolean replacementRequested, boolean interactive, boolean nativeAodShowing,
            boolean aodLayoutAttached) {
        return moduleEnabled
                && customAodEnabled
                && !replacementRequested
                && !interactive
                && nativeAodShowing
                && aodLayoutAttached;
    }

    static float revealProgress(boolean aodLayoutShown, float aodLayoutAlpha,
            boolean maskShown, float maskAlpha) {
        if (!aodLayoutShown) {
            return 0f;
        }
        float progress = clamp01(aodLayoutAlpha);
        if (maskShown) {
            progress = Math.min(progress, 1f - clamp01(maskAlpha));
        }
        return clamp01(progress);
    }

    static float synchronizedIconAlpha(float nativeBrightnessAlpha, float revealProgress) {
        return clamp01(nativeBrightnessAlpha) * clamp01(revealProgress);
    }

    static boolean isComplete(float revealProgress) {
        return revealProgress >= COMPLETE_PROGRESS;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, value));
    }
}
