package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** COUI-derived visual invariants for the clock and information surfaces. */
public final class PixelAodVisualStyleTest {
    @Test
    public void usesALighterInformationWeightOnAod() {
        assertEquals(500, PixelAodVisualStyle.Lockscreen.INFO_WEIGHT);
        assertEquals(450, PixelAodVisualStyle.Aod.INFO_WEIGHT);
    }

    @Test
    public void matchesTheCouiAodBatteryTextSize() {
        assertEquals(16, PixelAodVisualStyle.Aod.BATTERY_TEXT_DP);
    }

    @Test
    public void usesOneEmphasisColorForTheClockAndMediaLine() {
        assertEquals(PixelAodVisualStyle.MEDIA_EMPHASIS_COLOR_RED,
                PixelAodVisualStyle.CLOCK_COLOR_RED);
        assertEquals(PixelAodVisualStyle.MEDIA_EMPHASIS_COLOR_GREEN,
                PixelAodVisualStyle.CLOCK_COLOR_GREEN);
        assertEquals(PixelAodVisualStyle.MEDIA_EMPHASIS_COLOR_BLUE,
                PixelAodVisualStyle.CLOCK_COLOR_BLUE);
    }

    @Test
    public void mediaProfileMatchesTheRenderedAodMediaRow() {
        assertEquals(18, PixelAodVisualStyle.Aod.MEDIA_TITLE_TEXT_DP);
        assertEquals(15, PixelAodVisualStyle.Aod.MEDIA_ARTIST_TEXT_DP);
        assertEquals(18, PixelAodVisualStyle.Aod.MEDIA_ICON_SIZE_DP);
        assertEquals(6, PixelAodVisualStyle.Aod.MEDIA_ICON_SPACING_DP);
        assertEquals(4, PixelAodVisualStyle.Aod.MEDIA_SUBTITLE_TOP_GAP_DP);
        assertEquals(500, PixelAodVisualStyle.Aod.MEDIA_TITLE_WEIGHT);
    }

    @Test
    public void balancesTheSmallSceneAwayFromTopBiometricsAndLowAodRows() {
        assertEquals(90, PixelAodVisualStyle.SMALL_CLOCK_TOP_DP);
        assertEquals(99, PixelAodVisualStyle.SMALL_INFO_TOP_DP);
        assertEquals(218, PixelAodVisualStyle.Aod.SMALL_MEDIA_TOP_DP);
        assertEquals(72,
                PixelAodVisualStyle.COMPACT_DATE_TO_NOTIFICATION_WITHOUT_EVENT_TOP_OFFSET_DP);
        assertEquals(12, PixelAodVisualStyle.COUI_COMPACT_CLOCK_TO_EVENT_GAP_DP);
        assertEquals(12,
                PixelAodVisualStyle.COMPACT_CONTEXTUAL_TO_NOTIFICATION_GAP_DP);
        assertEquals(85, PixelAodVisualStyle.COMPACT_DATE_TO_NOTIFICATION_TOP_OFFSET_DP);
    }

    @Test
    public void tightensCompactClockSpacingToAlignWithTheNotificationColumn() {
        assertEquals(-0.09f, PixelAodVisualStyle.COMPACT_CLOCK_DIGIT_TRACKING_EM, 0.0001f);
        assertEquals(-0.049500003f, PixelAodVisualStyle.COMPACT_CLOCK_COLON_TRACKING_EM,
                0.0001f);
    }
}
