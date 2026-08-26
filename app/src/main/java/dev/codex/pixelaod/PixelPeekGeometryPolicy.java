package dev.codex.pixelaod;

/** Pure geometry for the transient Pixel-style incoming-notification card. */
final class PixelPeekGeometryPolicy {
    // On the current Pixel/COUI compact profile the notification icon row ends near 230 dp.
    // Keep the transient card below that entire cluster even if the vendor callback arrives
    // before the notification-row layout transaction has completed.
    static final float MIN_CARD_TOP_DP = 248f;
    static final float AMBIENT_CONTENT_GAP_DP = 18f;
    static final float CARD_MAX_HEIGHT_DP = 118f;
    static final float BOTTOM_SAFE_AREA_DP = 96f;

    private PixelPeekGeometryPolicy() {
    }

    static float resolveCardTopPx(float density, int hostHeightPx, float ambientBottomPx) {
        float safeDensity = density > 0f ? density : 1f;
        float minimumTop = MIN_CARD_TOP_DP * safeDensity;
        float desiredTop = Math.max(minimumTop,
                ambientBottomPx + AMBIENT_CONTENT_GAP_DP * safeDensity);
        if (hostHeightPx <= 0) {
            return desiredTop;
        }
        float maximumTop = hostHeightPx
                - (CARD_MAX_HEIGHT_DP + BOTTOM_SAFE_AREA_DP) * safeDensity;
        if (maximumTop < minimumTop) {
            maximumTop = minimumTop;
        }
        return Math.min(desiredTop, maximumTop);
    }
}
