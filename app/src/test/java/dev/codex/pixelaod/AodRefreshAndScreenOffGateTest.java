package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AodRefreshAndScreenOffGateTest {
    @Test
    public void coalescesNestedAodFrameRequestsUntilThePendingFrameRuns() {
        AodFrameRefreshGate gate = new AodFrameRefreshGate();

        assertTrue(gate.request(1_000L));
        assertFalse(gate.request(1_001L));

        gate.markFrameDispatched(1_016L);
        assertFalse(gate.request(1_200L));
        assertTrue(gate.request(1_266L));
    }

    @Test
    public void permitsTheNextFrameAfterTheRefreshCooldown() {
        AodFrameRefreshGate gate = new AodFrameRefreshGate();

        assertTrue(gate.request(1_000L));
        gate.markFrameDispatched(1_016L);

        assertTrue(gate.request(1_266L));
    }
}
