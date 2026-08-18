package dev.codex.pixelaod;

/** Pure geometry rules for the COUI host's contextual At a Glance row. */
final class CouiClockContextualLayoutPolicy {
    private CouiClockContextualLayoutPolicy() {
    }

    /** COUI contextual forecast/alert/calendar is an AOD-only surface. */
    static boolean contextualSurfaceEnabled(boolean dozing) {
        return dozing;
    }

    static float contextualTop(float dateTopPx, int dateHeightPx,
            boolean weatherVisible, float weatherTopPx, int weatherHeightPx, float gapPx) {
        float infoBottom = dateTopPx + Math.max(0, dateHeightPx);
        if (weatherVisible) {
            infoBottom = Math.max(infoBottom,
                    weatherTopPx + Math.max(0, weatherHeightPx));
        }
        return infoBottom + Math.max(0f, gapPx);
    }

    static float lowerContentTop(float defaultTopPx, boolean contextualVisible,
            float contextualTopPx, int contextualMeasuredHeightPx,
            float contextualFallbackHeightPx, float gapPx) {
        if (!contextualVisible) {
            return defaultTopPx;
        }
        float height = Math.max(Math.max(0, contextualMeasuredHeightPx),
                Math.max(0f, contextualFallbackHeightPx));
        return Math.max(defaultTopPx,
                contextualTopPx + height + Math.max(0f, gapPx));
    }
}
