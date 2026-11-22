package dev.codex.pixelaod;

/** COUI 2.7 width-based design metrics for lockscreen/AOD geometry. */
final class CouiDesignMetricsPolicy {
    static final float REFERENCE_DENSITY_DPI = 538f;
    static final float REFERENCE_DISPLAY_WIDTH_PX = 1272f;
    static final float DENSITY_DEFAULT_DPI = 160f;

    private CouiDesignMetricsPolicy() {
    }

    static float pixelsPerDesignDp(int designWidthPx, int fallbackWidthPx) {
        int width = designWidthPx > 0 ? designWidthPx : fallbackWidthPx;
        if (width <= 0) {
            width = Math.round(REFERENCE_DISPLAY_WIDTH_PX);
        }
        return (REFERENCE_DENSITY_DPI / DENSITY_DEFAULT_DPI)
                * (width / REFERENCE_DISPLAY_WIDTH_PX);
    }

    static float toPixels(float value, int designWidthPx, int fallbackWidthPx) {
        return value * pixelsPerDesignDp(designWidthPx, fallbackWidthPx);
    }
}
