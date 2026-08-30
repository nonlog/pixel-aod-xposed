package dev.codex.pixelaod;

/** Pure geometry rules for the COUI host's contextual At a Glance row. */
final class CouiClockContextualLayoutPolicy {
    private CouiClockContextualLayoutPolicy() {
    }

    /** COUI contextual forecast/alert/calendar is an AOD-only surface. */
    static boolean contextualSurfaceEnabled(boolean dozing) {
        return dozing;
    }

    static float largeContextualStart(float containerWidthPx, float visibleContentWidthPx) {
        float container = Math.max(0f, containerWidthPx);
        float content = Math.max(0f, visibleContentWidthPx);
        return Math.max(0f, (container - content) / 2f);
    }

    /** Large only centers the rendered weather forecast; other contextual kinds keep S25 X. */
    static float largeContextualStartForDisplayedCard(float containerWidthPx,
            float visibleContentWidthPx, float defaultStartPx, boolean displayedForecast) {
        return displayedForecast
                ? largeContextualStart(containerWidthPx, visibleContentWidthPx)
                : defaultStartPx;
    }

    static float visibleContentWidth(float textWidthPx, boolean iconVisible,
            float iconSpanPx, float maximumRowWidthPx) {
        float maximum = Math.max(0f, maximumRowWidthPx);
        float icon = iconVisible ? Math.max(0f, iconSpanPx) : 0f;
        float maximumText = Math.max(0f, maximum - icon);
        float text = Math.min(Math.max(0f, textWidthPx), maximumText);
        return Math.min(maximum, icon + text);
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
