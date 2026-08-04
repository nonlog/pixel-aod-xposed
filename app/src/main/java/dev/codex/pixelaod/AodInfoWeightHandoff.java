package dev.codex.pixelaod;

/**
 * Maps the clock's live weight progress into a compensated range for the optically smaller
 * date and weather text.  This has no separate animator: every frame follows the clock.
 */
final class AodInfoWeightHandoff {
    private AodInfoWeightHandoff() {
    }

    static int synchronizedInfoWeight(int clockWeight, int aodClockWeight,
            int lockscreenClockWeight) {
        int lowerClockWeight = Math.min(aodClockWeight, lockscreenClockWeight);
        int upperClockWeight = Math.max(aodClockWeight, lockscreenClockWeight);
        int minimumInfoWeight = PixelAodVisualStyle.Aod.DATE_WEATHER_MIN_WEIGHT;
        int maximumInfoWeight = PixelAodVisualStyle.Aod.DATE_WEATHER_MAX_WEIGHT;
        if (upperClockWeight <= lowerClockWeight) {
            return Math.round((minimumInfoWeight + maximumInfoWeight) / 2f);
        }
        float progress = (clockWeight - lowerClockWeight)
                / (float) (upperClockWeight - lowerClockWeight);
        progress = Math.max(0f, Math.min(1f, progress));
        return Math.round(minimumInfoWeight
                + ((maximumInfoWeight - minimumInfoWeight) * progress));
    }
}
