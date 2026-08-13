package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CouiCompactLayoutTest {
    @Test
    public void usesPixelSmallEdgeAndColumnAnchorsOnTheOnePlus12Canvas() {
        float density = 4f;

        assertEquals(108, CouiCompactLayout.clockLeft(1440, 320, density));
        assertEquals(128, CouiCompactLayout.paintedLeadingEdge(density));
        assertEquals(128, CouiCompactLayout.contextualLayoutLeft(density));
        assertEquals(104, CouiCompactLayout.contextualLayoutLeft(density,
                ContextualAtAGlanceCalendarIcon.APPLICATION_ICON_LEADING_OFFSET_DP));
        assertEquals(132, CouiCompactLayout.notificationLayoutLeft(density));
        assertEquals(268, CouiCompactLayout.clockCenterX(1440, 320, density));
        assertEquals(360, CouiCompactLayout.clockTop(3168, density));
        assertEquals(736, CouiCompactLayout.infoLeft(1440, 400, density));
        assertEquals(396, CouiCompactLayout.infoTop(3168, density));
        assertEquals(128, CouiCompactLayout.mediaLeft(density));
        assertEquals(872, CouiCompactLayout.mediaTopForViewport(3168, density));
    }

    @Test
    public void alignsSmallPaintedEdgesAfterPerElementOpticalInsets() {
        float density = 4f;
        int targetPaintedEdgePx = CouiCompactLayout.paintedLeadingEdge(density);
        int clockPaintedEdgePx = CouiCompactLayout.clockLeft(1440, 320, density)
                + Math.round(PixelAodVisualStyle.COMPACT_CLOCK_GLYPH_LEADING_INSET_DP * density);
        int contextualPaintedEdgePx = CouiCompactLayout.contextualLayoutLeft(density)
                + Math.round(PixelAodVisualStyle.COMPACT_CONTEXTUAL_ICON_LEADING_INSET_DP
                * density);
        int notificationPaintedEdgePx = CouiCompactLayout.notificationLayoutLeft(density)
                - Math.round(PixelAodVisualStyle.NOTIFICATION_ROW_LEADING_OFFSET_DP * density)
                + Math.round(PixelAodVisualStyle.COMPACT_NOTIFICATION_GLYPH_LEADING_INSET_DP
                * density);

        assertEquals(targetPaintedEdgePx, clockPaintedEdgePx);
        assertEquals(targetPaintedEdgePx, contextualPaintedEdgePx);
        assertEquals(targetPaintedEdgePx, notificationPaintedEdgePx);
    }

    @Test
    public void reservesClearanceAroundTheClockForLongLocalizedContent() {
        float density = 4f;
        int clockContentWidthPx = 640;
        CouiCompactLayout.Anchors anchors = CouiCompactLayout.anchors(
                1440, 3168, clockContentWidthPx, 580, density);

        int clockRightPx = anchors.clockLeftPx + clockContentWidthPx;
        int weatherAlertTopPx = CouiCompactLayout.weatherAlertTop(anchors, density);
        int minimumHorizontalGapPx = Math.round(16 * density);
        int minimumVerticalGapPx = Math.round(12 * density);
        int clockBottomPx = anchors.clockTopPx
                + Math.round(PixelAodVisualStyle.SMALL_CLOCK_TEXT_DP * density);

        assertEquals(812, anchors.infoLeftPx);
        assertEquals(632, weatherAlertTopPx);
        assertTrue(anchors.infoLeftPx - clockRightPx >= minimumHorizontalGapPx);
        assertTrue(weatherAlertTopPx - clockBottomPx >= minimumVerticalGapPx);
        assertEquals(504, CouiCompactLayout.weatherTop(anchors, density));
    }

    @Test
    public void keepsMeasuredSmallInformationOpticallyAlignedWithClockGlyphs() {
        float density = 4f;
        // Preserve the measured Google Sans Flex ink-center relationship while moving the whole
        // small scene slightly down from the first Pixel-style top anchor.
        float clockCenter = CouiCompactLayout.clockTop(3168, density) + 32f + 163f / 2f;
        float informationCenter = CouiCompactLayout.infoTop(3168, density) - 4f + 160f / 2f;

        assertEquals(clockCenter, informationCenter, 2f);
    }

    @Test
    public void keepsMediaAtTheSmallBaselineUnlessInformationNeedsTheSpace() {
        assertEquals(872, CouiCompactLayout.mediaTopAfterInfo(872, 810, 24));
        assertEquals(932, CouiCompactLayout.mediaTopAfterInfo(840, 908, 24));
        assertEquals(14, PixelAodVisualStyle.COMPACT_AUXILIARY_INFO_TEXT_DP);
    }

    @Test
    public void keepsSmallNotificationsBelowDateAndWeather() {
        float density = 4f;
        int defaultNotificationTop = CouiCompactLayout.infoTop(3168, density)
                + Math.round(PixelAodVisualStyle
                .COMPACT_DATE_TO_NOTIFICATION_WITHOUT_EVENT_TOP_OFFSET_DP * density);
        assertEquals(702, AodInfoStackLayout.topAfterVisibleRow(defaultNotificationTop, 678, 24));
    }

    @Test
    public void keepsContextualAlertSeparatedFromNotificationIcons() {
        float density = 4f;
        CouiCompactLayout.Anchors anchors = CouiCompactLayout.anchors(
                1440, 3168, 320, 400, density);
        int alertTopPx = CouiCompactLayout.weatherAlertTop(anchors, density);
        int alertBottomPx = alertTopPx
                + Math.round(PixelAodVisualStyle.COMPACT_AUXILIARY_INFO_TEXT_DP * density);
        int defaultNotificationTopPx = anchors.infoTopPx
                + Math.round(PixelAodVisualStyle
                .COMPACT_DATE_TO_NOTIFICATION_WITHOUT_EVENT_TOP_OFFSET_DP * density);
        int notificationTopPx = AodInfoStackLayout.topAfterVisibleRow(
                defaultNotificationTopPx, alertBottomPx,
                Math.round(PixelAodVisualStyle
                .COMPACT_CONTEXTUAL_TO_NOTIFICATION_GAP_DP * density));

        assertEquals(632, alertTopPx);
        assertEquals(736, notificationTopPx);
        assertEquals(48, notificationTopPx - alertBottomPx);
    }

    @Test
    public void keepsStableDpAnchorsBeforeViewportMeasurement() {
        CouiCompactLayout.Anchors anchors = CouiCompactLayout.anchors(0, 0, 320, 400, 4f);

        assertEquals(108, anchors.clockLeftPx);
        assertEquals(360, anchors.clockTopPx);
        assertEquals(136, anchors.infoLeftPx);
        assertEquals(396, anchors.infoTopPx);
        assertEquals(872, CouiCompactLayout.mediaTopForViewport(0, 4f));
    }
}
