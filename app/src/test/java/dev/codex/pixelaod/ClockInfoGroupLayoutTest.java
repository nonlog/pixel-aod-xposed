package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression coverage for the date, current-weather, and contextual information group. */
public final class ClockInfoGroupLayoutTest {
    @Test
    public void largeClockPlacesWeatherAfterDateAndCentersTheirVisibleRows() {
        ClockInfoGroupLayout.Result result = ClockInfoGroupLayout.layout(
                false, 136, 400,
                240, 64,
                true, 40,
                0,
                24, 488);

        assertEquals(136, result.dateLeftPx);
        assertEquals(400, result.dateTopPx);
        assertEquals(136 + 240 + 24, result.weatherLeftPx);
        assertEquals(412, result.weatherTopPx);
        assertEquals(464, result.infoBottomPx);
    }

    @Test
    public void contextualRowStartsAfterActualLargeInformationBottomAndMinimumAnchor() {
        ClockInfoGroupLayout.Result result = ClockInfoGroupLayout.layout(
                false, 136, 400,
                240, 64,
                true, 96,
                0,
                24, 488);

        assertEquals(384, result.weatherTopPx);
        assertEquals(480, result.infoBottomPx);
        assertEquals(504, result.contextualTopPx);
        assertTrue(result.contextualTopPx >= result.infoBottomPx + 24);
    }

    @Test
    public void compactClockPreservesSeparateDateAndWeatherAnchors() {
        ClockInfoGroupLayout.Result result = ClockInfoGroupLayout.layout(
                true, 784, 470,
                180, 80,
                true, 80,
                578,
                24, 606);

        assertEquals(784, result.dateLeftPx);
        assertEquals(470, result.dateTopPx);
        assertEquals(784, result.weatherLeftPx);
        assertEquals(578, result.weatherTopPx);
        assertEquals(682, result.contextualTopPx);
    }

    @Test
    public void lockscreenAndAodGetTheSameResultForTheSameInputs() {
        ClockInfoGroupLayout.Result lockscreen = ClockInfoGroupLayout.layout(
                false, 136, 400, 220, 64, true, 64, 0, 24, 488);
        ClockInfoGroupLayout.Result aod = ClockInfoGroupLayout.layout(
                false, 136, 400, 220, 64, true, 64, 0, 24, 488);

        assertEquals(lockscreen.dateLeftPx, aod.dateLeftPx);
        assertEquals(lockscreen.weatherLeftPx, aod.weatherLeftPx);
        assertEquals(lockscreen.weatherTopPx, aod.weatherTopPx);
        assertEquals(lockscreen.infoBottomPx, aod.infoBottomPx);
        assertEquals(lockscreen.contextualTopPx, aod.contextualTopPx);
    }
}
