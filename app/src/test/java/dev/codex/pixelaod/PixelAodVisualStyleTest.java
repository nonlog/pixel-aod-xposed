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
    public void tightensCompactClockSpacingToAlignWithTheNotificationColumn() {
        assertEquals(-0.09f, PixelAodVisualStyle.COMPACT_CLOCK_DIGIT_TRACKING_EM, 0.0001f);
        assertEquals(-0.049500003f, PixelAodVisualStyle.COMPACT_CLOCK_COLON_TRACKING_EM,
                0.0001f);
    }
}
