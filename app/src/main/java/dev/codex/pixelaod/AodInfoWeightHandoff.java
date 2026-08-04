package dev.codex.pixelaod;

/** Pure timing model for continuing a live lockscreen information-weight morph on AOD. */
final class AodInfoWeightHandoff {
    private AodInfoWeightHandoff() {
    }

    static boolean needsAnimation(int startWeight, int targetWeight) {
        return Math.abs(startWeight - targetWeight) > 1;
    }

    static long remainingDurationMillis(int startWeight, int targetWeight, int originWeight,
            long fullDurationMillis) {
        if (!needsAnimation(startWeight, targetWeight) || fullDurationMillis <= 0L) {
            return 0L;
        }
        int totalDistance = Math.abs(originWeight - targetWeight);
        if (totalDistance <= 1) {
            return fullDurationMillis;
        }
        float remainingFraction = Math.min(1f,
                Math.abs(startWeight - targetWeight) / (float) totalDistance);
        return Math.max(1L, Math.round(fullDurationMillis * remainingFraction));
    }
}
