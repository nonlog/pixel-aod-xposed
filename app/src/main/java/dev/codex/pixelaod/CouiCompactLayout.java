package dev.codex.pixelaod;

/** Pixel coordinates for the compact COUI Expressive clock scene. */
final class CouiCompactLayout {
    private CouiCompactLayout() {
    }

    static int clockCenterX(int widthPx, float density) {
        return Math.round(widthPx * PixelAodVisualStyle.COUI_COMPACT_CLOCK_CENTER_X_FRACTION)
                + dp(PixelAodVisualStyle.COUI_COMPACT_CLOCK_CENTER_X_OFFSET_DP, density);
    }

    static int clockTop(int heightPx, float density) {
        return Math.round(heightPx * PixelAodVisualStyle.COUI_COMPACT_CLOCK_TOP_FRACTION)
                + dp(PixelAodVisualStyle.COUI_COMPACT_CLOCK_TOP_OFFSET_DP, density);
    }

    static int clockLeft(int widthPx, int contentWidthPx, float density) {
        return Math.max(0, clockCenterX(widthPx, density) - Math.max(0, contentWidthPx) / 2);
    }

    static int infoCenterX(int widthPx, float density) {
        return Math.round(widthPx * PixelAodVisualStyle.COUI_COMPACT_INFO_CENTER_X_FRACTION)
                + dp(PixelAodVisualStyle.COUI_COMPACT_INFO_CENTER_X_OFFSET_DP, density);
    }

    static int infoTop(int heightPx, float density) {
        return Math.round(heightPx * PixelAodVisualStyle.COUI_COMPACT_INFO_TOP_FRACTION)
                + dp(PixelAodVisualStyle.COUI_COMPACT_INFO_TOP_OFFSET_DP, density);
    }

    static int infoLeft(int widthPx, int contentWidthPx, float density) {
        return Math.max(0, infoCenterX(widthPx, density) - Math.max(0, contentWidthPx) / 2);
    }

    static int mediaLeft(float density) {
        return dp(PixelAodVisualStyle.COUI_COMPACT_MEDIA_EDGE_DP, density);
    }

    static int mediaTop(int heightPx) {
        return Math.round(heightPx * PixelAodVisualStyle.COUI_COMPACT_MEDIA_TOP_FRACTION);
    }

    static int mediaTopForViewport(int heightPx, float density) {
        return heightPx > 0 ? mediaTop(heightPx)
                : dp(PixelAodVisualStyle.Aod.SMALL_MEDIA_TOP_DP, density);
    }

    static Anchors anchors(int widthPx, int heightPx, int clockContentWidthPx,
            int infoContentWidthPx, float density) {
        int clockLeftPx = widthPx > 0
                ? clockLeft(widthPx, clockContentWidthPx, density)
                : dp(PixelAodVisualStyle.EDGE_DP
                - PixelAodVisualStyle.COMPACT_CLOCK_VISUAL_START_OFFSET_DP, density);
        int clockTopPx = heightPx > 0 ? clockTop(heightPx, density)
                : dp(PixelAodVisualStyle.SMALL_CLOCK_TOP_DP, density);
        int infoLeftPx = widthPx > 0 ? infoLeft(widthPx, infoContentWidthPx, density)
                : dp(PixelAodVisualStyle.EDGE_DP, density);
        int infoTopPx = heightPx > 0 ? infoTop(heightPx, density)
                : dp(PixelAodVisualStyle.SMALL_INFO_TOP_DP, density);
        return new Anchors(clockLeftPx, clockTopPx, infoLeftPx, infoTopPx);
    }

    static final class Anchors {
        final int clockLeftPx;
        final int clockTopPx;
        final int infoLeftPx;
        final int infoTopPx;

        Anchors(int clockLeftPx, int clockTopPx, int infoLeftPx, int infoTopPx) {
            this.clockLeftPx = clockLeftPx;
            this.clockTopPx = clockTopPx;
            this.infoLeftPx = infoLeftPx;
            this.infoTopPx = infoTopPx;
        }
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }
}
