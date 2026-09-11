package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FodNativeTimeoutHideGateTest {
    @Test
    public void timeoutHidePersistsForSameAodTrace() {
        FodNativeTimeoutHideGate gate = new FodNativeTimeoutHideGate();
        assertTrue(gate.markHidden("trace-a", 100L));
        assertTrue(gate.shouldPreserveNativeHide("trace-a", false));
        assertFalse(gate.shouldPreserveNativeHide("trace-b", false));
    }

    @Test
    public void explicitReleaseCanClearOnlyMatchingTrace() {
        FodNativeTimeoutHideGate gate = new FodNativeTimeoutHideGate();
        assertTrue(gate.markHidden("trace-a", 100L));
        assertFalse(gate.clearIfTrace("trace-b"));
        assertTrue(gate.shouldPreserveNativeHide("trace-a", false));
        assertTrue(gate.clearIfTrace("trace-a"));
        assertFalse(gate.shouldPreserveNativeHide("trace-a", false));
    }
}
