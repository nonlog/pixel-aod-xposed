package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

public final class BreezyWeatherRelayReceiverTest {
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
    public void decompressWeatherGzDecodesValidPayloadAndRejectsOversizedOutput() throws Exception {
        String payload = "[{\"currentTemp\":24,\"currentCondition\":\"Sunny\"}]";
        byte[] compressed = gzip(payload);
        assertEquals(payload, BreezyWeatherRelayReceiver.decompressWeatherGz(compressed, 1024));
        assertNull(BreezyWeatherRelayReceiver.decompressWeatherGz(compressed, 10));
        assertNull(BreezyWeatherRelayReceiver.decompressWeatherGz(null, 1024));
        assertNull(BreezyWeatherRelayReceiver.decompressWeatherGz(new byte[]{1, 2, 3}, 1024));
    }

    private static byte[] gzip(String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }
}
