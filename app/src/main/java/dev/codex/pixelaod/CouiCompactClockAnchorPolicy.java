package dev.codex.pixelaod;

/** Stable COUI 2.7 compact-clock centering independent of the current time digits. */
final class CouiCompactClockAnchorPolicy {
    static final String ANCHOR_DIGIT = "8";
    private static final int DIGIT_COUNT = 4;

    private CouiCompactClockAnchorPolicy() {
    }

    static float centeredStart(float centerX, float anchorDigitAdvance, float colonAdvance,
            float tracking, float colonTracking) {
        float stableWidth = Math.max(0f, anchorDigitAdvance) * DIGIT_COUNT;
        float contentWidth = stableWidth
                + Math.max(0f, colonAdvance)
                + tracking * 2f
                + colonTracking * 2f;
        return centerX - contentWidth / 2f;
    }
}
