package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class OosProximityTransitionGateTest {
    @Test
    public void repeatedFarPollsDoNotCreateNewFarEdges() {
        OosProximityTransitionGate gate = new OosProximityTransitionGate();

        assertEquals(OosProximityTransitionGate.Transition.NONE, gate.update(false));
        assertEquals(OosProximityTransitionGate.Transition.NONE, gate.update(false));
        assertEquals(OosProximityTransitionGate.Transition.NONE, gate.update(false));
    }

    @Test
    public void onlyNearToFarCreatesAFarEdge() {
        OosProximityTransitionGate gate = new OosProximityTransitionGate();

        assertEquals(OosProximityTransitionGate.Transition.NEAR, gate.update(true));
        assertEquals(OosProximityTransitionGate.Transition.NONE, gate.update(true));
        assertEquals(OosProximityTransitionGate.Transition.FAR, gate.update(false));
        assertEquals(OosProximityTransitionGate.Transition.NONE, gate.update(false));
    }

    @Test
    public void resetStartsWithoutInventingAFarEdge() {
        OosProximityTransitionGate gate = new OosProximityTransitionGate();

        gate.update(true);
        gate.reset();

        assertEquals(OosProximityTransitionGate.Transition.NONE, gate.update(false));
        assertEquals(OosProximityTransitionGate.Transition.NEAR, gate.update(true));
    }
}
