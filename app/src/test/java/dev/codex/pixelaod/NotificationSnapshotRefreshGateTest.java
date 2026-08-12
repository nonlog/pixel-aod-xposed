package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class NotificationSnapshotRefreshGateTest {
    @Test
    public void mergesCallbacksThatArriveBeforeThePendingSnapshotRuns() {
        NotificationSnapshotRefreshGate gate = new NotificationSnapshotRefreshGate();

        assertEquals(0L, gate.requestDelayMillis(1_000L));
        assertEquals(NotificationSnapshotRefreshGate.NO_SCHEDULE,
                gate.requestDelayMillis(1_001L));

        gate.markDispatched(1_010L);
        assertEquals(NotificationSnapshotRefreshGate.MIN_INTERVAL_MILLIS - 10L,
                gate.requestDelayMillis(1_020L));
    }

    @Test
    public void allowsTheNextSnapshotAfterTheCooldown() {
        NotificationSnapshotRefreshGate gate = new NotificationSnapshotRefreshGate();

        assertEquals(0L, gate.requestDelayMillis(1_000L));
        gate.markDispatched(1_010L);

        assertEquals(0L, gate.requestDelayMillis(
                1_010L + NotificationSnapshotRefreshGate.MIN_INTERVAL_MILLIS));
    }
}
