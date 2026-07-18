package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ProximityAuthorityGateTest {
    @Test
    public void ignoresNoisyRawSensorSamples() {
        ProximityAuthorityGate gate = new ProximityAuthorityGate();

        assertFalse(gate.update(ProximityAuthorityGate.Source.RAW_SENSOR, true));
        assertFalse(gate.isNear());
        assertFalse(gate.update(ProximityAuthorityGate.Source.RAW_SENSOR, false));
        assertFalse(gate.isNear());
    }

    @Test
    public void appliesOnlyOosNativeProximityState() {
        ProximityAuthorityGate gate = new ProximityAuthorityGate();

        assertTrue(gate.update(ProximityAuthorityGate.Source.OOS_NATIVE, true));
        assertTrue(gate.isNear());
        assertFalse(gate.update(ProximityAuthorityGate.Source.OOS_NATIVE, true));
        assertTrue(gate.update(ProximityAuthorityGate.Source.OOS_NATIVE, false));
        assertFalse(gate.isNear());
    }

    @Test
    public void resetClearsConfirmedNearState() {
        ProximityAuthorityGate gate = new ProximityAuthorityGate();
        gate.update(ProximityAuthorityGate.Source.OOS_NATIVE, true);

        assertTrue(gate.reset());
        assertFalse(gate.isNear());
        assertFalse(gate.reset());
    }
}
