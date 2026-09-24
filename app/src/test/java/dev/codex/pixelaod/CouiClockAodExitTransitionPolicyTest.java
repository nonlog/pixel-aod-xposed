package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockAodExitTransitionPolicyTest {
    @Test
    public void renderDrivenInteractiveTransientHideRetargetsValidLockscreenScene() {
        assertEquals(CouiClockAodExitTransitionPolicy.Decision.PRESENT_LOCKSCREEN,
                CouiClockAodExitTransitionPolicy.decide(
                        true,
                        true,
                        true,
                        CouiClockPluginPresentationMapper.Action.HIDE,
                        CouiClockPresentationModel.Scene.SMALL));
    }

    @Test
    public void alreadyNonDozingPresentationKeepsNormalHide() {
        assertEquals(CouiClockAodExitTransitionPolicy.Decision.NORMAL,
                CouiClockAodExitTransitionPolicy.decide(
                        true,
                        false,
                        true,
                        CouiClockPluginPresentationMapper.Action.HIDE,
                        CouiClockPresentationModel.Scene.SMALL));
    }

    @Test
    public void loadOrRefreshDoesNotOverrideTransientHide() {
        assertEquals(CouiClockAodExitTransitionPolicy.Decision.NORMAL,
                CouiClockAodExitTransitionPolicy.decide(
                        false,
                        true,
                        true,
                        CouiClockPluginPresentationMapper.Action.HIDE,
                        CouiClockPresentationModel.Scene.SMALL));
    }

    @Test
    public void nonInteractiveDozingStateDoesNotOverrideTransientHide() {
        assertEquals(CouiClockAodExitTransitionPolicy.Decision.NORMAL,
                CouiClockAodExitTransitionPolicy.decide(
                        true,
                        true,
                        false,
                        CouiClockPluginPresentationMapper.Action.HIDE,
                        CouiClockPresentationModel.Scene.SMALL));
    }

    @Test
    public void invalidLockscreenSceneDoesNotOverrideTransientHide() {
        assertEquals(CouiClockAodExitTransitionPolicy.Decision.NORMAL,
                CouiClockAodExitTransitionPolicy.decide(
                        true,
                        true,
                        true,
                        CouiClockPluginPresentationMapper.Action.HIDE,
                        null));
    }

    @Test
    public void nonHideActionDoesNotOverrideEvenWhenAllExitSignalsArePresent() {
        assertEquals(CouiClockAodExitTransitionPolicy.Decision.NORMAL,
                CouiClockAodExitTransitionPolicy.decide(
                        true,
                        true,
                        true,
                        CouiClockPluginPresentationMapper.Action.PRESENT,
                        CouiClockPresentationModel.Scene.LARGE));
    }

    @Test
    public void renderDrivenAnimationRequiresRawUiStateAnimation() {
        assertTrue(CouiClockAodExitTransitionPolicy.animationAllowed(true, true));
        assertFalse(CouiClockAodExitTransitionPolicy.animationAllowed(true, false));
        assertFalse(CouiClockAodExitTransitionPolicy.animationAllowed(false, true));
    }
    @Test
    public void finishedDozingToLockscreenRequestsStaleHostRepair() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertTrue(CouiClockAodExitTransitionPolicy.shouldRepairFinishedNativeExit(
                gate.observe("DOZING", "LOCKSCREEN", 1.0f, "FINISHED",
                        "owner", "doze-finish")));
        assertTrue(CouiClockAodExitTransitionPolicy.shouldRepairFinishedNativeExit(
                gate.observe("AOD", "LOCKSCREEN", 1.0f, "FINISHED",
                        "owner", "aod-finish")));
    }

    @Test
    public void nativeExitRepairNeverStealsRunningAnimationOrOtherReturns() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(CouiClockAodExitTransitionPolicy.shouldRepairFinishedNativeExit(
                gate.observe("DOZING", "LOCKSCREEN", 0.0f, "STARTED",
                        "owner", "start")));
        assertFalse(CouiClockAodExitTransitionPolicy.shouldRepairFinishedNativeExit(
                gate.observe("DOZING", "LOCKSCREEN", 0.7f, "RUNNING",
                        "owner", "running")));
        assertFalse(CouiClockAodExitTransitionPolicy.shouldRepairFinishedNativeExit(
                gate.observe("DOZING", "LOCKSCREEN", 0.7f, "CANCELED",
                        "owner", "cancel")));
        assertFalse(CouiClockAodExitTransitionPolicy.shouldRepairFinishedNativeExit(
                gate.observe("OCCLUDED", "LOCKSCREEN", 1.0f, "FINISHED",
                        "owner", "alarm-return")));
    }

}
