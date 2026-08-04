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

    static int weatherAlertTop(Anchors anchors, float density) {
        return Math.max(firstEventTop(anchors, density),
                anchors.clockTopPx
                        + dp(PixelAodVisualStyle.SMALL_CLOCK_TEXT_DP
                        + PixelAodVisualStyle.COUI_COMPACT_CLOCK_TO_EVENT_GAP_DP, density));
    }

    static int weatherTop(Anchors anchors, float density) {
        return anchors.infoTopPx
                + dp(PixelAodVisualStyle.COUI_COMPACT_DATE_TO_WEATHER_TOP_OFFSET_DP, density);
    }

    static int firstEventTop(Anchors anchors, float density) {
        return weatherTop(anchors, density)
                + dp(PixelAodVisualStyle.COMPACT_INFO_TEXT_DP
                + PixelAodVisualStyle.COUI_COMPACT_INFO_TO_EVENT_GAP_DP, density);
    }

    static int mediaLeft(float density) {
        return dp(PixelAodVisualStyle.COUI_COMPACT_MEDIA_EDGE_DP, density);
    }

    static int mediaTop(int heightPx) {
        return Math.round(heightPx * PixelAodVisualStyle.COUI_COMPACT_MEDIA_TOP_FRACTION);
    }

    static int mediaTopForViewport(int heightPx, float density) {
        return heightPx > 0 ? mediaTop(heightPx)
                + dp(PixelAodVisualStyle.COUI_COMPACT_MEDIA_TOP_OFFSET_DP, density)
                : dp(PixelAodVisualStyle.Aod.SMALL_MEDIA_TOP_DP, density);
    }

    static int mediaTopAfterInfo(int defaultTopPx, int infoBottomPx, int gapPx) {
        return Math.max(defaultTopPx, infoBottomPx + Math.max(0, gapPx));
    }

    static Anchors anchors(int widthPx, int heightPx, int clockContentWidthPx,
            int infoContentWidthPx, float density) {
        int clockLeftPx = widthPx > 0
                ? clockLeft(widthPx, clockContentWidthPx, density)
                : dp(PixelAodVisualStyle.EDGE_DP
                - PixelAodVisualStyle.COMPACT_CLOCK_VISUAL_START_OFFSET_DP, density);
        int clockTopPx = heightPx > 0 ? clockTop(heightPx, density)
                : dp(PixelAodVisualStyle.SMALL_CLOCK_TOP_DP, density);
        int infoLeftPx;
        if (widthPx > 0) {
            int preferredInfoLeftPx = infoLeft(widthPx, infoContentWidthPx, density);
            int minimumInfoLeftPx = clockLeftPx + Math.max(0, clockContentWidthPx)
                    + dp(PixelAodVisualStyle.COUI_COMPACT_CLOCK_TO_INFO_GAP_DP, density);
            int maxInfoLeftPx = Math.max(0, widthPx - Math.max(0, infoContentWidthPx));
            infoLeftPx = Math.min(maxInfoLeftPx, Math.max(preferredInfoLeftPx, minimumInfoLeftPx));
        } else {
            infoLeftPx = dp(PixelAodVisualStyle.EDGE_DP, density);
        }
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
