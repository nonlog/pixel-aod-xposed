package dev.codex.pixelaod;

/**
 * Pure layout rules for the date, current-weather, and contextual At a Glance group.
 *
 * <p>Large clocks present date and weather as one horizontal information group. Compact
 * clocks retain COUI's separate right-side anchors. In both modes the contextual row starts
 * below the actual information-group bottom while retaining its established minimum anchor.</p>
 */
final class ClockInfoGroupLayout {
    private ClockInfoGroupLayout() {
    }

    static Result layout(boolean compact, int infoLeftPx, int dateTopPx, int dateWidthPx,
            int dateHeightPx, boolean weatherVisible, int weatherHeightPx,
            int compactWeatherTopPx, int gapPx, int contextualMinimumTopPx) {
        int safeInfoLeft = Math.max(0, infoLeftPx);
        int safeDateTop = Math.max(0, dateTopPx);
        int safeDateWidth = Math.max(0, dateWidthPx);
        int safeDateHeight = Math.max(0, dateHeightPx);
        int safeGap = Math.max(0, gapPx);

        int weatherLeftPx = safeInfoLeft;
        int weatherTopPx = safeDateTop;
        int infoBottomPx = safeDateTop + safeDateHeight;
        if (weatherVisible) {
            int safeWeatherHeight = Math.max(0, weatherHeightPx);
            weatherLeftPx = compact ? safeInfoLeft : safeInfoLeft + safeDateWidth + safeGap;
            weatherTopPx = compact ? Math.max(0, compactWeatherTopPx)
                    : safeDateTop + Math.round((safeDateHeight - safeWeatherHeight) / 2f);
            infoBottomPx = Math.max(infoBottomPx, weatherTopPx + safeWeatherHeight);
        }
        int contextualTopPx = Math.max(Math.max(0, contextualMinimumTopPx),
                infoBottomPx + safeGap);
        return new Result(safeInfoLeft, safeDateTop, weatherLeftPx, weatherTopPx,
                infoBottomPx, contextualTopPx);
    }

    static final class Result {
        final int dateLeftPx;
        final int dateTopPx;
        final int weatherLeftPx;
        final int weatherTopPx;
        final int infoBottomPx;
        final int contextualTopPx;

        Result(int dateLeftPx, int dateTopPx, int weatherLeftPx, int weatherTopPx,
                int infoBottomPx, int contextualTopPx) {
            this.dateLeftPx = dateLeftPx;
            this.dateTopPx = dateTopPx;
            this.weatherLeftPx = weatherLeftPx;
            this.weatherTopPx = weatherTopPx;
            this.infoBottomPx = infoBottomPx;
            this.contextualTopPx = contextualTopPx;
        }
    }
}
