package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.Test;

public final class ForecastDisplayWindowTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    @Test
    public void defaultsUseTwentyOneHundredThroughBeforeTwentyThreeThirty() {
        ForecastDisplayWindow window = ForecastDisplayWindow.defaults();

        assertFalse(window.contains(time("2026-08-05T12:59:59Z"), SINGAPORE));
        assertTrue(window.contains(time("2026-08-05T13:00:00Z"), SINGAPORE));
        assertTrue(window.contains(time("2026-08-05T15:29:59Z"), SINGAPORE));
        assertFalse(window.contains(time("2026-08-05T15:30:00Z"), SINGAPORE));
    }

    @Test
    public void acceptsCrossMidnightWindowsAndSchedulesTheirNearestBoundary() {
        ForecastDisplayWindow window = ForecastDisplayWindow.fromSettings("21:00", "01:00");

        assertFalse(window.contains(time("2026-08-05T12:59:59Z"), SINGAPORE));
        assertTrue(window.contains(time("2026-08-05T13:00:00Z"), SINGAPORE));
        assertTrue(window.contains(time("2026-08-05T16:59:59Z"), SINGAPORE));
        assertFalse(window.contains(time("2026-08-05T17:00:00Z"), SINGAPORE));
        assertEquals(time("2026-08-05T17:00:00Z"),
                window.nextBoundaryMillis(time("2026-08-05T15:00:00Z"), SINGAPORE));
        assertEquals(time("2026-08-05T16:00:00Z"),
                AtAGlanceWeatherPolicy.nextForecastBoundary(window,
                        time("2026-08-05T15:00:00Z"), SINGAPORE, true));
    }

    @Test
    public void malformedOrEqualSettingsFallBackToTheWholeDefaultPair() {
        ForecastDisplayWindow malformed = ForecastDisplayWindow.fromSettings("20:00", "bad");
        ForecastDisplayWindow equal = ForecastDisplayWindow.fromSettings("21:00", "21:00");

        assertEquals(LocalTime.of(21, 0), malformed.start);
        assertEquals(LocalTime.of(23, 30), malformed.end);
        assertEquals(LocalTime.of(21, 0), equal.start);
        assertEquals(LocalTime.of(23, 30), equal.end);
    }

    @Test
    public void policySeparatesForecastDataEligibilityFromConfiguredTimeEligibility() {
        long sourceAt = time("2026-08-05T12:00:00Z");
        BreezyWeatherForecast forecast = BreezyWeatherForecast.forFields("location",
                LocalDate.of(2026, 8, 6), 800, "Clear", 31, 25, sourceAt);
        ForecastDisplayWindow window = ForecastDisplayWindow.defaults();

        assertTrue(forecast.isEligible(time("2026-08-05T12:59:59Z"), SINGAPORE));
        assertFalse(AtAGlanceWeatherPolicy.forecastEligible(forecast,
                time("2026-08-05T12:59:59Z"), SINGAPORE, true, window));
        assertTrue(AtAGlanceWeatherPolicy.forecastEligible(forecast,
                time("2026-08-05T13:00:00Z"), SINGAPORE, true, window));
    }

    private static long time(String instant) {
        return Instant.parse(instant).toEpochMilli();
    }
}
