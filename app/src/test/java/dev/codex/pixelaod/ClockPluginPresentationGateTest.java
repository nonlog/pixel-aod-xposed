package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClockPluginPresentationGateTest {
    @Test
    public void skipsAnUnchangedClockPluginRender() {
        ClockPluginPresentationGate gate = new ClockPluginPresentationGate();
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();
        ClockPluginSceneMachine.Decision aod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, true, false, false, false, true);

        assertTrue(gate.shouldPresent(aod, false));
        assertFalse(gate.shouldPresent(aod, false));
    }

    @Test
    public void permitsARealSceneOrLifecycleChange() {
        ClockPluginPresentationGate gate = new ClockPluginPresentationGate();
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();
        ClockPluginSceneMachine.Decision lockscreen = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                false, true, false, false, true, true);
        ClockPluginSceneMachine.Decision aod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, true, false, true, false, true);

        assertTrue(gate.shouldPresent(lockscreen, false));
        assertTrue(gate.shouldPresent(aod, false));
    }

    @Test
    public void permitsAnExplicitlyForcedPresentation() {
        ClockPluginPresentationGate gate = new ClockPluginPresentationGate();
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();
        ClockPluginSceneMachine.Decision aod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, true, false, false, false, true);

        assertTrue(gate.shouldPresent(aod, false));
        assertTrue(gate.shouldPresent(aod, true));
    }
}
