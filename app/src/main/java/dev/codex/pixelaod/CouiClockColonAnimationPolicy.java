package dev.codex.pixelaod;

/** Pure timing contract for the independently animated morph colon alpha. */
final class CouiClockColonAnimationPolicy {
    private CouiClockColonAnimationPolicy() {
    }

    static long alphaStartDelay(float targetAlpha, float currentAlpha, long durationMillis) {
        return targetAlpha > currentAlpha
                ? (long) (durationMillis * CouiClockPresentationModel.COLON_START_FRACTION)
                : 0L;
    }

    static long alphaDuration(long durationMillis) {
        return Math.max(1L,
                (long) (durationMillis * CouiClockPresentationModel.COLON_DURATION_FRACTION));
    }
}
