package dev.codex.pixelaod;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class BreezyWeatherRelayReceiverTest {
    private static byte[] gzip(String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    @Test
    public void failedQueryWithoutValidCacheDoesNotBecomeAuthoritativeEmptySnapshot() {
        BreezyWeatherSnapshot.RelayState state = BreezyWeatherSnapshot.relayState(null, false);
        assertTrue(state.queryFailed);
        assertFalse(state.available);
        assertFalse(BreezyWeatherSnapshot.shouldApplyRelaySnapshot(
                state.available, state.synced));
    }

    @Test
    public void successfulEmptySnapshotRemainsAuthoritativeAndCanClearAlerts() {
        BreezyWeatherSnapshot.RelayState state = BreezyWeatherSnapshot.relayState(
                BreezyWeatherSnapshot.queried("loc", Collections.emptyList(),
                        Collections.emptyList(), 1_000L),
                true);
        assertTrue(state.available);
        assertTrue(state.synced);
        assertTrue(BreezyWeatherSnapshot.shouldApplyRelaySnapshot(
                state.available, state.synced));
    }

    @Test
    public void decompressBoundedGzipReadsNormalPayload() throws Exception {
        String payload = "[{\"currentTemp\":22,\"currentCondition\":\"Sunny\"}]";
        assertEquals(payload, BreezyWeatherRelayReceiver.decompressBoundedGzip(gzip(payload)));
    }

    @Test
    public void decompressBoundedGzipRejectsOversizedCompressedPayload() {
        byte[] oversized = new byte[BreezyWeatherRelayReceiver.MAX_WEATHER_GZ_BYTES + 1];
        assertNull(BreezyWeatherRelayReceiver.decompressBoundedGzip(oversized));
    }

    @Test
    public void decompressBoundedGzipRejectsOversizedExpandedPayload() throws Exception {
        char[] chars = new char[BreezyWeatherRelayReceiver.MAX_WEATHER_GZ_CHARS + 2048];
        Arrays.fill(chars, 'a');
        byte[] bomb = gzip(new String(chars));
        assertNull(BreezyWeatherRelayReceiver.decompressBoundedGzip(bomb));
    }
}
