package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LiveUpdateMetricFormatPolicyTest {
    @Test
    public void interactiveTimerKeepsChronometerSeconds() {
        assertEquals("29:10", LiveUpdateMetricFormatPolicy.formatDurationSeconds(
                29 * 60L + 10L, true, false, false));
        assertTrue(LiveUpdateMetricFormatPolicy.shouldScheduleSecondTicks(false, false));
    }

    @Test
    public void ambientWithoutRamlessUsesAdaptiveCountdown() {
        assertEquals("30m", LiveUpdateMetricFormatPolicy.formatDurationSeconds(
                29 * 60L + 10L, true, true, false));
        assertEquals("29m", LiveUpdateMetricFormatPolicy.formatDurationSeconds(
                29 * 60L, true, true, false));
        assertEquals("<1m", LiveUpdateMetricFormatPolicy.formatDurationSeconds(
                42L, true, true, false));
        assertFalse(LiveUpdateMetricFormatPolicy.shouldScheduleSecondTicks(true, false));
    }

    @Test
    public void ambientWithRamlessCanKeepChronometer() {
        assertEquals("29:10", LiveUpdateMetricFormatPolicy.formatDurationSeconds(
                29 * 60L + 10L, true, true, true));
        assertTrue(LiveUpdateMetricFormatPolicy.shouldScheduleSecondTicks(true, true));
    }

    @Test
    public void adaptiveHoursAndCountUpAreStable() {
        assertEquals("1h 6m", LiveUpdateMetricFormatPolicy.formatAdaptiveCountdown(
                65 * 60L + 1L));
        assertEquals("1h 5m", LiveUpdateMetricFormatPolicy.formatAdaptiveCountUp(
                65 * 60L + 59L));
    }
}
