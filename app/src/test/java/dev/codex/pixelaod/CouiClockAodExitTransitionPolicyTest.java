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
}
