package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClockPluginSceneMachineTest {
    @Test
    public void preservesLockscreenAcrossTransientUnlockedStateBeforeAod() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_LARGE, false, true, false, false, true, true);
        ClockPluginSceneMachine.Decision transientState = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_UNLOCKED, null, false, true, false, false, false, true);
        ClockPluginSceneMachine.Decision aod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null, false, true, false, false, false, true);

        assertEquals(ClockPluginSceneMachine.Scene.LOCKSCREEN_LARGE, transientState.scene);
        assertFalse(transientState.changed);
        assertEquals(ClockPluginSceneMachine.Scene.AOD_LARGE, aod.scene);
        assertTrue(aod.enteringAod);
    }

    @Test
    public void usesContentCompactnessWhenAodClockSizeIsUnavailable() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        ClockPluginSceneMachine.Decision decision = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, -1, false, true, false, true, false, true);

        assertEquals(ClockPluginSceneMachine.Scene.AOD_SMALL, decision.scene);
    }

    @Test
    public void preservesLockscreenUntilAodLifecyclePolicyIsReady() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL, false, true, false, false, true, true);
        ClockPluginSceneMachine.Decision earlyAod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null, false, false, true, false, false, true);
        ClockPluginSceneMachine.Decision readyAod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null, false, true, false, false, false, true);

        assertEquals(ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL, earlyAod.scene);
        assertFalse(earlyAod.changed);
        assertTrue(earlyAod.preparingAod);
        assertEquals(ClockPluginSceneMachine.Scene.AOD_LARGE, readyAod.scene);
        assertTrue(readyAod.enteringAod);
    }

    @Test
    public void hidesOnlyForConfirmedInteractiveUnlockOrDeniedAodPolicy() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL, false, true, false, false, true, true);
        ClockPluginSceneMachine.Decision unlocked = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_UNLOCKED, null, false, true, false, false, true, false);
        ClockPluginSceneMachine.Decision blockedAod = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null, false, false, false, false, false, true);

        assertEquals(ClockPluginSceneMachine.Scene.HIDDEN, unlocked.scene);
        assertEquals(ClockPluginSceneMachine.Scene.HIDDEN, blockedAod.scene);
    }

    @Test
    public void preservesLockscreenDuringAnimatedUnlockedState() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        ClockPluginSceneMachine.Decision animatedUnlock = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_UNLOCKED,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                true, true, false, false, true, false);

        assertEquals(ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL, animatedUnlock.scene);
        assertTrue(animatedUnlock.changed);
    }

    @Test
    public void preservesCommittedSceneDuringInteractiveAodLifecycleGap() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL, false, true, false, false, true, true);
        ClockPluginSceneMachine.Decision lifecycleGap = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, false, true, false, true, true);

        assertEquals(ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL, lifecycleGap.scene);
        assertFalse(lifecycleGap.changed);
        assertTrue(lifecycleGap.preparingAod);
    }

    @Test
    public void preservesCommittedAodDuringLifecycleReadinessGap() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, true, false, false, false, true);
        ClockPluginSceneMachine.Decision lifecycleGap = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, false, true, false, false, true);

        assertEquals(ClockPluginSceneMachine.Scene.AOD_LARGE, lifecycleGap.scene);
        assertFalse(lifecycleGap.changed);
        assertFalse(lifecycleGap.preparingAod);
    }

    @Test
    public void rejectsStaleKeyguardRenderWhileDisplayRemainsInAod() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, true, false, false, false, true, true);
        ClockPluginSceneMachine.Decision staleKeyguard = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                true, true, false, false, false, true, true);

        assertEquals(ClockPluginSceneMachine.Scene.AOD_LARGE, staleKeyguard.scene);
        assertFalse(staleKeyguard.changed);
        assertTrue(staleKeyguard.staleLockscreenRenderRejected);
    }

    @Test
    public void acceptsKeyguardRenderOnceWakeIsInteractive() {
        ClockPluginSceneMachine machine = new ClockPluginSceneMachine();

        machine.resolve(ClockPluginSceneMachine.UI_STATE_AOD, null,
                false, true, false, false, false, true, true);
        ClockPluginSceneMachine.Decision keyguard = machine.resolve(
                ClockPluginSceneMachine.UI_STATE_KEYGUARD,
                ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                true, true, false, false, true, true, true);

        assertEquals(ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL, keyguard.scene);
        assertTrue(keyguard.changed);
        assertFalse(keyguard.staleLockscreenRenderRejected);
    }
}
