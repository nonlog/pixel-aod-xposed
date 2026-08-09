package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

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
}
