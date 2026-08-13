package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FodNativeTimeoutHideGateTest {
    @Test
    public void timeoutHideIsBoundToOneNonInteractiveAodTrace() {
        FodNativeTimeoutHideGate gate = new FodNativeTimeoutHideGate();

        assertFalse(gate.markHidden("", 100L));
        assertTrue(gate.markHidden("aod-42", 1_000L));
        assertTrue(gate.shouldPreserveNativeHide("aod-42", false));
        assertFalse(gate.shouldPreserveNativeHide("aod-43", false));
        assertFalse(gate.shouldPreserveNativeHide("aod-42", true));
        assertEquals(250L, gate.hiddenAgeMillis(1_250L));
    }

    @Test
    public void failedDispatchOrExplicitInteractionCanReleaseTheLatch() {
        FodNativeTimeoutHideGate gate = new FodNativeTimeoutHideGate();
        assertTrue(gate.markHidden("aod-42", 1_000L));

        assertFalse(gate.clearIfTrace("aod-41"));
        assertTrue(gate.shouldPreserveNativeHide("aod-42", false));
        assertTrue(gate.clearIfTrace("aod-42"));
        assertFalse(gate.shouldPreserveNativeHide("aod-42", false));

        assertTrue(gate.markHidden("aod-43", 2_000L));
        assertTrue(gate.clear());
        assertFalse(gate.clear());
    }
}
