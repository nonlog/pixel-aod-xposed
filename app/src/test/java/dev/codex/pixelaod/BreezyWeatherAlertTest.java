package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BreezyWeatherAlertTest {
    @Test
    public void choosesTheHighestSeverityActiveAlert() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert minor = BreezyWeatherAlert.forFields(
                "Minor rain", 1_749_999_000_000L, 1_750_001_000_000L, 1);
        BreezyWeatherAlert severe = BreezyWeatherAlert.forFields(
                "Severe thunderstorm", 1_749_999_000_000L, 1_750_001_000_000L, 3);
        BreezyWeatherAlert alert = BreezyWeatherAlert.selectActive(now, minor, severe);

        assertEquals("Severe thunderstorm", alert.headline);
        assertEquals(3, alert.severity);
        assertTrue(alert.isActive(now));
    }

    @Test
    public void ignoresFutureAndExpiredAlerts() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert future = BreezyWeatherAlert.forFields(
                "Future", 1_750_001_000_000L, 0L, 4);
        BreezyWeatherAlert expired = BreezyWeatherAlert.forFields(
                "Expired", 0L, 1_749_999_000_000L, 4);
        BreezyWeatherAlert alert = BreezyWeatherAlert.selectActive(now, future, expired);

        assertFalse(alert.isActive(now));
        assertEquals("", alert.headline);
    }

    @Test
    public void keepsTheFirstObservedTimeWhenTheSameAlertIsRefreshed() {
        long observedAt = 1_750_000_000_000L;
        BreezyWeatherAlert source = BreezyWeatherAlert.forFields(
                "Thunderstorm", observedAt - 1_000L, observedAt + 86_400_000L, 3);
        BreezyWeatherAlert stored = BreezyWeatherAlert.observeForDisplay(source,
                BreezyWeatherAlert.empty(), observedAt);
        BreezyWeatherAlert refreshed = BreezyWeatherAlert.observeForDisplay(source, stored,
                observedAt + 9L * 60L * 1000L);

        assertEquals(observedAt, refreshed.observedAtMillis);
        assertTrue(refreshed.isActive(observedAt + 9L * 60L * 1000L));
        assertFalse(refreshed.isActive(observedAt + 10L * 60L * 1000L));
    }

    @Test
    public void doesNotRestartTheClockForAnAlreadyOldAlert() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert staleSource = BreezyWeatherAlert.forFields(
                "Old warning", now - 2L * 60L * 60L * 1000L, now + 86_400_000L, 2);

        BreezyWeatherAlert observed = BreezyWeatherAlert.observeForDisplay(
                staleSource, BreezyWeatherAlert.empty(), now);

        assertEquals("", observed.headline);
        assertFalse(observed.isActive(now));
    }
}
