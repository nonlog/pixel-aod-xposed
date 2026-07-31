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
}
