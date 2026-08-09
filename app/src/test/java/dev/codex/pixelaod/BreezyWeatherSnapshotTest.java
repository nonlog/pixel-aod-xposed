package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

public final class BreezyWeatherSnapshotTest {
    private static final long SOURCE_AT = Instant.parse("2026-08-05T08:00:00Z").toEpochMilli();
    private static final long QUERY_AT = Instant.parse("2026-08-05T10:00:00Z").toEpochMilli();

    @Test
    public void forecastUsesProviderTimestampAndRepeatedQueriesCannotRefreshStaleSource() {
        BreezyWeatherSnapshot first = BreezyWeatherSnapshot.fromProviderJson(
                providerJson(SOURCE_AT), "loc", QUERY_AT);
        BreezyWeatherSnapshot repeated = BreezyWeatherSnapshot.fromProviderJson(
                providerJson(SOURCE_AT), "loc", SOURCE_AT + BreezyWeatherForecast.MAX_SOURCE_AGE_MILLIS
                        + 1L);

        assertEquals(SOURCE_AT, first.forecastFor(java.time.LocalDate.of(2026, 8, 6))
                .sourceUpdatedAtMillis);
        assertEquals(SOURCE_AT, repeated.forecastFor(java.time.LocalDate.of(2026, 8, 6))
                .sourceUpdatedAtMillis);
        assertFalse(repeated.forecastFor(java.time.LocalDate.of(2026, 8, 6))
                .isSourceFresh(SOURCE_AT + BreezyWeatherForecast.MAX_SOURCE_AGE_MILLIS + 1L));
    }

    @Test
    public void malformedJsonCompressedJsonAndFatalSchemaAreQueryFailures() throws IOException {
        assertNull(BreezyWeatherSnapshot.fromProviderJson("{not-json", "loc", QUERY_AT));
        assertNull(BreezyWeatherSnapshot.fromProviderPayload(new byte[]{1, 2, 3}, "loc",
                QUERY_AT));
        assertNull(BreezyWeatherSnapshot.fromProviderPayload(gzip("not-json"), "loc", QUERY_AT));
        assertNull(BreezyWeatherSnapshot.fromProviderJson(
                "{\"timestamp\":1754380800,\"alerts\":\"not-an-array\"}", "loc", QUERY_AT));
        assertNull(BreezyWeatherSnapshot.fromRelayJson("{not-json"));
    }

    @Test
    public void validEmptyProviderResponseRemainsSuccessfulAndFailuresKeepAlertGrace() {
        BreezyWeatherSnapshot empty = BreezyWeatherSnapshot.fromProviderJson(
                "{\"refreshTime\":1754380800000,\"alerts\":[],\"daily\":[]}",
                "loc", QUERY_AT);
        assertTrue(empty.sourceQuerySucceeded);
        assertTrue(empty.activeAlerts.isEmpty());

        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("id", "loc", "Storm", QUERY_AT - 1L,
                0L, 2);
        BreezyWeatherSnapshot confirmed = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.emptyList(), QUERY_AT);
        BreezyWeatherSnapshot failed = BreezyWeatherSnapshot.failedUsing(confirmed);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();

        assertEquals(alert, store.select(failed.activeAlerts, failed,
                QUERY_AT + 59L * 60L * 1000L, 1L, true));
        assertEquals(BreezyWeatherAlert.empty(), store.select(failed.activeAlerts, failed,
                QUERY_AT + BreezyWeatherSnapshot.MAX_ALERT_SOURCE_AGE_MILLIS + 1L, 2L, true));
    }

    private static String providerJson(long sourceAtMillis) {
        return "{\"refreshTime\":" + sourceAtMillis
                + ",\"locationId\":\"loc\",\"alerts\":[],\"daily\":[{"
                + "\"date\":\"2026-08-06\",\"day\":{\"weatherCode\":800,"
                + "\"temperature\":{\"temperature\":{\"value\":31,\"unit\":\"C\"}}},"
                + "\"night\":{\"temperature\":{\"temperature\":{\"value\":25,\"unit\":\"C\"}}}}]}";
    }

    private static byte[] gzip(String value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}
