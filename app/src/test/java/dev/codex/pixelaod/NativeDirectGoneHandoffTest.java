package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NativeDirectGoneHandoffTest {
    @Test
    public void armsOnlyWhenGoingAwayStartsInsideActiveAmbientSession() {
        NativeDirectGoneHandoff gate = new NativeDirectGoneHandoff();

        assertFalse(gate.observeKeyguardGoingAway(true, 7L, false, "ordinary-unlock").active);
        assertTrue(gate.observeKeyguardGoingAway(true, 8L, true, "ambient-direct-unlock").active);
        assertTrue(gate.isActiveFor(8L));
        assertFalse(gate.isActiveFor(7L));
    }

    @Test
    public void survivesAmbientStopUntilNativeGoingAwayClears() {
        NativeDirectGoneHandoff gate = new NativeDirectGoneHandoff();
        gate.observeKeyguardGoingAway(true, 12L, true, "direct-unlock");

        assertTrue(gate.isActive());
        assertTrue(gate.observeKeyguardGoingAway(false, 13L, false, "gone-or-cancelled").active == false);
        assertFalse(gate.isActive());
    }

    @Test
    public void nextAmbientSessionClearsStaleDirectGoneLatch() {
        NativeDirectGoneHandoff gate = new NativeDirectGoneHandoff();
        gate.observeKeyguardGoingAway(true, 22L, true, "direct-unlock");

        gate.resetForNewAmbientSession("new-ambient");

        assertFalse(gate.isActive());
        assertFalse(gate.isActiveFor(22L));
    }
}
