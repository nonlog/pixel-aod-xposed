package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.json.JSONObject;
import org.junit.Test;

public final class BreezyWeatherForecastTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final long NOW = Instant.parse("2026-08-05T10:00:00Z").toEpochMilli();

    @Test
    public void dataEligibilityUsesTheDeviceLocalTomorrowWithoutAHardCodedHour() {
        long beforeSix = Instant.parse("2026-08-05T09:59:00Z").toEpochMilli();
        long sixPm = Instant.parse("2026-08-05T10:00:00Z").toEpochMilli();
        long beforeMidnight = Instant.parse("2026-08-05T15:59:59Z").toEpochMilli();
        long midnight = Instant.parse("2026-08-05T16:00:00Z").toEpochMilli();

        assertTrue(complete(LocalDate.of(2026, 8, 6), beforeSix - 1_000L)
                .isEligible(beforeSix, SINGAPORE));
        assertTrue(complete(LocalDate.of(2026, 8, 6), sixPm - 1_000L)
                .isEligible(sixPm, SINGAPORE));
        assertTrue(complete(LocalDate.of(2026, 8, 6), beforeMidnight - 1_000L)
                .isEligible(beforeMidnight, SINGAPORE));
        assertFalse(complete(LocalDate.of(2026, 8, 6), midnight - 1_000L)
                .isEligible(midnight, SINGAPORE));
    }

    @Test
    public void usesDeviceLocalTomorrowAndReevaluatesAcrossTimeZones() {
        BreezyWeatherForecast singaporeTomorrow = complete(LocalDate.of(2026, 8, 6), NOW);
        long instant = Instant.parse("2026-08-05T16:00:00Z").toEpochMilli();

        // At the same instant UTC is still Aug 5 (tomorrow = Aug 6), while Singapore has
        // crossed midnight to Aug 6 (tomorrow = Aug 7). The forecast must follow device-local
        // calendar semantics rather than a fixed UTC date.
        assertTrue(singaporeTomorrow.isEligible(instant, ZoneId.of("UTC")));
        assertFalse(singaporeTomorrow.isEligible(instant, SINGAPORE));
    }

    @Test
    public void requiresFreshSourceCompleteIconHighAndLow() {
        BreezyWeatherForecast complete = complete(LocalDate.of(2026, 8, 6), NOW);
        assertTrue(complete.isSourceFresh(NOW + BreezyWeatherForecast.MAX_SOURCE_AGE_MILLIS));
        assertFalse(complete.isSourceFresh(NOW + BreezyWeatherForecast.MAX_SOURCE_AGE_MILLIS + 1));
        assertFalse(BreezyWeatherForecast.forFields("location", complete.forecastDate,
                BreezyWeatherForecast.UNKNOWN_WEATHER_CODE, "", 31, 25, NOW).isComplete());
        assertFalse(BreezyWeatherForecast.forFields("location", complete.forecastDate,
                800, "", Double.NaN, 25, NOW).isComplete());
        assertFalse(BreezyWeatherForecast.forFields("location", complete.forecastDate,
                800, "", 31, Double.NaN, NOW).isComplete());
    }

    @Test
    public void prefersWholeDayIconThenDaytimeAndNeverNightOnly() {
        BreezyWeatherForecast nightOnly = BreezyWeatherForecast.forFields("location",
                LocalDate.of(2026, 8, 6), BreezyWeatherForecast.UNKNOWN_WEATHER_CODE, "", 31, 25,
                NOW);
        assertFalse(nightOnly.hasUsableIcon());

        BreezyWeatherForecast daytime = BreezyWeatherForecast.forDaytimeFields("location",
                LocalDate.of(2026, 8, 6), 801, "", 31, 25, NOW);
        assertEquals(801, daytime.weatherCode);
        assertTrue(daytime.daytimeIcon);

        BreezyWeatherForecast wholeDay = BreezyWeatherForecast.forFields("location",
                LocalDate.of(2026, 8, 6), 804, "", 31, 25, NOW);
        assertEquals(804, wholeDay.weatherCode);
        assertFalse(wholeDay.daytimeIcon);
    }

    @Test
    public void providerParsingUsesDaytimeIconAndRejectsMissingDayCondition() throws Exception {
        BreezyWeatherForecast missingDayCondition = BreezyWeatherForecast.fromJson(new JSONObject(
                "{\"date\":\"2026-08-06\",\"day\":{},"
                        + "\"night\":{\"weatherCode\":802,"
                        + "\"temperature\":{\"temperature\":{\"value\":25,\"unit\":\"C\"}}}}"),
                "location", NOW, SINGAPORE);
        assertEquals(BreezyWeatherForecast.UNKNOWN_WEATHER_CODE,
                missingDayCondition.weatherCode);
        assertFalse(missingDayCondition.isComplete());

        BreezyWeatherForecast daytime = BreezyWeatherForecast.fromJson(new JSONObject(
                "{\"date\":\"2026-08-06\",\"day\":{\"weatherCode\":801,"
                        + "\"temperature\":{\"temperature\":{\"value\":31,\"unit\":\"C\"}}},"
                        + "\"night\":{\"weatherCode\":802,"
                        + "\"temperature\":{\"temperature\":{\"value\":25,\"unit\":\"C\"}}}}"),
                "location", NOW, SINGAPORE);
        assertEquals(801, daytime.weatherCode);
        assertTrue(daytime.daytimeIcon);
        assertEquals(31, daytime.roundedTemperature(daytime.highCelsius));
        assertEquals(25, daytime.roundedTemperature(daytime.lowCelsius));
        assertTrue(daytime.isComplete());
    }

    @Test
    public void textOnlyForecastUsesOnlyDeterministicallyRecognizedDaytimeCondition() throws Exception {
        BreezyWeatherForecast clear = BreezyWeatherForecast.fromJson(new JSONObject(
                "{\"date\":\"2026-08-06\",\"day\":{\"weatherText\":\"Clear\","
                        + "\"temperature\":{\"temperature\":{\"value\":31,\"unit\":\"C\"}}},"
                        + "\"night\":{\"temperature\":{\"temperature\":{\"value\":25,\"unit\":\"C\"}}}}"),
                "location", NOW, SINGAPORE);
        assertEquals(800, clear.weatherCode);
        assertTrue(clear.isComplete());

        BreezyWeatherForecast unknown = BreezyWeatherForecast.fromJson(new JSONObject(
                "{\"date\":\"2026-08-06\",\"day\":{\"weatherText\":\"unrecognized condition\","
                        + "\"temperature\":{\"temperature\":{\"value\":31,\"unit\":\"C\"}}},"
                        + "\"night\":{\"temperature\":{\"temperature\":{\"value\":25,\"unit\":\"C\"}}}}"),
                "location", NOW, SINGAPORE);
        assertEquals(BreezyWeatherForecast.UNKNOWN_WEATHER_CODE, unknown.weatherCode);
        assertFalse(unknown.hasUsableIcon());
        assertFalse(unknown.isComplete());
    }

    @Test
    public void actualBreezyNightOnlyRecordIsIncompleteAndNeverUsesNightIcon() throws Exception {
        BreezyWeatherForecast nightOnly = BreezyWeatherForecast.fromJson(new JSONObject(
                "{\"date\":\"2026-08-06\",\"night\":{\"weatherCode\":802,"
                        + "\"temperature\":{\"temperature\":{\"value\":25,\"unit\":\"C\"}}}}"),
                "location", NOW, SINGAPORE);

        assertEquals(BreezyWeatherForecast.UNKNOWN_WEATHER_CODE, nightOnly.weatherCode);
        assertFalse(nightOnly.hasUsableIcon());
        assertFalse(nightOnly.isComplete());
    }

    @Test
    public void formatsRoundedCelsiusHighThenLowAndIgnoresPayloadTimestampForDisplayEquality() {
        BreezyWeatherForecast first = BreezyWeatherForecast.forFields("one",
                LocalDate.of(2026, 8, 6), 800, "clear", 30.6, 24.4, NOW);
        BreezyWeatherForecast refreshed = BreezyWeatherForecast.forFields("one",
                LocalDate.of(2026, 8, 6), 800, "clear", 30.6, 24.4, NOW + 1_000L);

        assertEquals("Tomorrow 31\u00b0 / 24\u00b0", first.formatText("Tomorrow"));
        assertEquals("明天 31\u00b0 / 24\u00b0", first.formatText("明天"));
        assertTrue(first.sameDisplay(refreshed));

        BreezyWeatherForecast textOnlyRefresh = BreezyWeatherForecast.forFields("one",
                LocalDate.of(2026, 8, 6), 800, "different source wording", 30.6, 24.4,
                NOW + 2_000L);
        assertTrue(first.sameDisplay(textOnlyRefresh));
    }

    @Test
    public void keepsLocationChangeAsARealForecastChange() {
        BreezyWeatherForecast first = complete(LocalDate.of(2026, 8, 6), NOW);
        BreezyWeatherForecast replacement = BreezyWeatherForecast.forFields("other",
                first.forecastDate, first.weatherCode, first.conditionText,
                first.highCelsius, first.lowCelsius, NOW);
        assertFalse(first.sameDisplay(replacement));
    }

    private static BreezyWeatherForecast complete(LocalDate date, long sourceAt) {
        return BreezyWeatherForecast.forFields("location", date, 800, "Clear", 31, 25, sourceAt);
    }
}
