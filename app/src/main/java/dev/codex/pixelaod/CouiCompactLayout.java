package dev.codex.pixelaod;

/**
 * Layout math for the small clock scene.
 *
 * <p>The class keeps its historical name to avoid a broad transition-layer refactor, but the
 * primary anchors are no longer the OOS/COUI 25%/75% viewport fractions. Pixel Small uses an
 * edge-anchored clock plus a width-aware right information column, so longer localized content
 * can consume available space without shifting the clock itself.</p>
 */
final class CouiCompactLayout {
    private CouiCompactLayout() {
    }

    static int clockTop(int heightPx, float density) {
        return dp(PixelAodVisualStyle.SMALL_CLOCK_TOP_DP, density);
    }

    static int clockLeft(int widthPx, int contentWidthPx, float density) {
        return dp(PixelAodVisualStyle.COMPACT_PAINTED_LEADING_EDGE_DP
                - PixelAodVisualStyle.COMPACT_CLOCK_GLYPH_LEADING_INSET_DP, density);
    }

    static int paintedLeadingEdge(float density) {
        return dp(PixelAodVisualStyle.COMPACT_PAINTED_LEADING_EDGE_DP, density);
    }

    /** Painted clock edge for the current COUI glyph target, including any AOD burn-in X. */
    static float paintedLeadingEdgeForClockTarget(float firstDigitTargetX, float density) {
        return firstDigitTargetX
                + PixelAodVisualStyle.COMPACT_CLOCK_GLYPH_LEADING_INSET_DP * density;
    }

    /** Layout X that makes the contextual icon paint on the supplied clock edge. */
    static float contextualLayoutLeftForPaintedEdge(float paintedEdgePx, float density,
            int applicationIconLeadingOffsetDp) {
        return paintedEdgePx
                - (PixelAodVisualStyle.COMPACT_CONTEXTUAL_ICON_LEADING_INSET_DP
                + Math.max(0, applicationIconLeadingOffsetDp)) * density;
    }

    /**
     * COUI host notification rows have no legacy post-layout translation. Align their first
     * painted glyph directly to the current clock target edge.
     */
    static float notificationLayoutLeftForPaintedEdge(float paintedEdgePx, float density) {
        return paintedEdgePx
                - PixelAodVisualStyle.COMPACT_NOTIFICATION_GLYPH_LEADING_INSET_DP * density;
    }

    static int contextualLayoutLeft(float density) {
        return contextualLayoutLeft(density, 0);
    }

    static int contextualLayoutLeft(float density, int applicationIconLeadingOffsetDp) {
        return dp(PixelAodVisualStyle.COMPACT_PAINTED_LEADING_EDGE_DP
                - PixelAodVisualStyle.COMPACT_CONTEXTUAL_ICON_LEADING_INSET_DP
                - Math.max(0, applicationIconLeadingOffsetDp), density);
    }

    static int notificationLayoutLeft(float density) {
        // notificationIconRow is translated left after layout. Compensate for that translation
        // and the measured glyph inset so the final painted icon edge lands on the same optical
        // edge as the clock and contextual forecast icon.
        return dp(PixelAodVisualStyle.COMPACT_PAINTED_LEADING_EDGE_DP
                + PixelAodVisualStyle.NOTIFICATION_ROW_LEADING_OFFSET_DP
                - PixelAodVisualStyle.COMPACT_NOTIFICATION_GLYPH_LEADING_INSET_DP, density);
    }

    static int clockCenterX(int widthPx, int contentWidthPx, float density) {
        return clockLeft(widthPx, contentWidthPx, density) + Math.max(0, contentWidthPx) / 2;
    }

    static int infoTop(int heightPx, float density) {
        return dp(PixelAodVisualStyle.SMALL_INFO_TOP_DP, density);
    }

    static int infoLeft(int widthPx, int contentWidthPx, float density) {
        if (widthPx <= 0) {
            return dp(PixelAodVisualStyle.EDGE_DP, density);
        }
        int preferredLeftPx = widthPx / 2
                + dp(PixelAodVisualStyle.PIXEL_SMALL_INFO_COLUMN_OFFSET_DP, density);
        int preferredMaxPx = Math.max(0, widthPx
                - dp(PixelAodVisualStyle.EDGE_DP, density) - Math.max(0, contentWidthPx));
        return Math.max(0, Math.min(preferredLeftPx, preferredMaxPx));
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

    static int mediaTopForViewport(int heightPx, float density) {
        return dp(PixelAodVisualStyle.Aod.SMALL_MEDIA_TOP_DP, density);
    }

    static int mediaTopAfterInfo(int defaultTopPx, int infoBottomPx, int gapPx) {
        return Math.max(defaultTopPx, infoBottomPx + Math.max(0, gapPx));
    }

    static Anchors anchors(int widthPx, int heightPx, int clockContentWidthPx,
            int infoContentWidthPx, float density) {
        int clockLeftPx = clockLeft(widthPx, clockContentWidthPx, density);
        int clockTopPx = clockTop(heightPx, density);
        int infoLeftPx;
        if (widthPx > 0) {
            int preferredInfoLeftPx = infoLeft(widthPx, infoContentWidthPx, density);
            int minimumInfoLeftPx = clockLeftPx + Math.max(0, clockContentWidthPx)
                    + dp(PixelAodVisualStyle.COUI_COMPACT_CLOCK_TO_INFO_GAP_DP, density);
            int preferredMaxInfoLeftPx = Math.max(0, widthPx
                    - dp(PixelAodVisualStyle.EDGE_DP, density)
                    - Math.max(0, infoContentWidthPx));
            int hardMaxInfoLeftPx = Math.max(0, widthPx - Math.max(0, infoContentWidthPx));
            int maxInfoLeftPx = minimumInfoLeftPx <= preferredMaxInfoLeftPx
                    ? preferredMaxInfoLeftPx : hardMaxInfoLeftPx;
            infoLeftPx = Math.min(maxInfoLeftPx,
                    Math.max(preferredInfoLeftPx, minimumInfoLeftPx));
        } else {
            infoLeftPx = dp(PixelAodVisualStyle.EDGE_DP, density);
        }
        int infoTopPx = infoTop(heightPx, density);
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
