package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CouiCompactLayoutTest {
    @Test
    public void matchesTheReferenceCoordinatesOnTheOnePlus12Canvas() {
        float density = 4f;

        assertEquals(400, CouiCompactLayout.clockCenterX(1440, density));
        // COUI PixelClockHostView: height * 0.105 + 25 dp.
        assertEquals(433, CouiCompactLayout.clockTop(3168, density));
        assertEquals(944, CouiCompactLayout.infoCenterX(1440, density));
        // Our Google Sans Flex text bounds sit 36 px lower than COUI's grouped information
        // view on a 1440 x 3168 / 4x canvas, so the visual groups share one vertical centre.
        assertEquals(470, CouiCompactLayout.infoTop(3168, density));
        assertEquals(128, CouiCompactLayout.mediaLeft(density));
        assertEquals(840, CouiCompactLayout.mediaTopForViewport(3168, density));
    }

    @Test
    public void keepsClockAndInformationContentCenteredOnTheirAnchors() {
        float density = 4f;

        assertEquals(240, CouiCompactLayout.clockLeft(1440, 320, density));
        assertEquals(744, CouiCompactLayout.infoLeft(1440, 400, density));
    }

    @Test
    public void reservesClearanceAroundTheClockForTheLongestCompactContent() {
        float density = 4f;
        int clockContentWidthPx = 640;
        CouiCompactLayout.Anchors anchors = CouiCompactLayout.anchors(
                1440, 3168, clockContentWidthPx, 580, density);

        int clockRightPx = anchors.clockLeftPx + clockContentWidthPx;
        int weatherAlertTopPx = CouiCompactLayout.weatherAlertTop(anchors, density);
        int minimumHorizontalGapPx = Math.round(16 * density);
        int minimumVerticalGapPx = Math.round(6 * density);
        int clockBottomPx = anchors.clockTopPx
                + Math.round(PixelAodVisualStyle.SMALL_CLOCK_TEXT_DP * density);

        assertEquals(784, anchors.infoLeftPx);
        assertEquals(686, weatherAlertTopPx);
        assertTrue(anchors.infoLeftPx - clockRightPx >= minimumHorizontalGapPx);
        assertTrue(weatherAlertTopPx - clockBottomPx >= minimumVerticalGapPx);
        assertEquals(578, CouiCompactLayout.weatherTop(anchors, density));
    }

    @Test
    public void alignsTheMeasuredCompactInformationGroupWithTheClockGlyphs() {
        float density = 4f;
        // Bounds captured from the user's OnePlus 12 recording at 20:57. The clock's glyph
        // box begins 32 px below its layout anchor and is 163 px high. The date/weather group
        // begins 4 px above its anchor and spans 160 px.
        float clockCenter = CouiCompactLayout.clockTop(3168, density) + 32f + 163f / 2f;
        float informationCenter = CouiCompactLayout.infoTop(3168, density) - 4f + 160f / 2f;

        assertEquals(clockCenter, informationCenter, 1f);
    }

    @Test
    public void keepsMediaAtTheCouiBaselineUnlessInformationNeedsTheSpace() {
        assertEquals(840, CouiCompactLayout.mediaTopAfterInfo(840, 810, 24));
        assertEquals(932, CouiCompactLayout.mediaTopAfterInfo(840, 908, 24));
        assertEquals(14, PixelAodVisualStyle.COMPACT_AUXILIARY_INFO_TEXT_DP);
    }

    @Test
    public void givesCompactNotificationsCouiBreathingRoomBelowDateAndWeather() {
        float density = 4f;
        int defaultNotificationTop = CouiCompactLayout.infoTop(3168, density)
                + Math.round(PixelAodVisualStyle
                .COMPACT_DATE_TO_NOTIFICATION_WITHOUT_EVENT_TOP_OFFSET_DP * density);
        // The current date/weather pair needs only 702 px for its measured bottom plus gap;
        // retain the COUI target instead of collapsing the icon row against the weather.
        assertEquals(822, AodInfoStackLayout.topAfterVisibleRow(defaultNotificationTop, 678, 24));
    }

    @Test
    public void fallsBackToTheExistingDpCoordinatesUntilTheViewportIsMeasured() {
        CouiCompactLayout.Anchors anchors = CouiCompactLayout.anchors(0, 0, 320, 400, 4f);

        assertEquals(108, anchors.clockLeftPx);
        assertEquals(296, anchors.clockTopPx);
        assertEquals(136, anchors.infoLeftPx);
        assertEquals(600, anchors.infoTopPx);
        assertEquals(936, CouiCompactLayout.mediaTopForViewport(0, 4f));
    }
}
