package dev.codex.pixelaod;

final class ClockTypefaceResolutionPolicy {
    enum Source {
        EXACT_VARIATION,
        DERIVED_WEIGHT
    }

    private ClockTypefaceResolutionPolicy() {
    }

    static Source weightedSource(boolean bundledVariableFont) {
        return bundledVariableFont ? Source.EXACT_VARIATION : Source.DERIVED_WEIGHT;
    }

    static String strategyName(boolean bundledVariableFont) {
        return bundledVariableFont
                ? "shared-file-exact-variation-cache"
                : "system-family-derived-weight-cache";
    }

    static boolean shouldApplyTypeface(int currentWeight, int requestedWeight,
            boolean handoffBoundary) {
        return handoffBoundary || currentWeight != requestedWeight;
    }

    static String sharedClockVariationSettings(int weight) {
        int normalizedWeight = Math.max(100, Math.min(500, weight));
        return "'opsz' 144, 'wght' " + normalizedWeight
                + ", 'GRAD' 0, 'ROND' 100, 'wdth' 100";
    }
}
