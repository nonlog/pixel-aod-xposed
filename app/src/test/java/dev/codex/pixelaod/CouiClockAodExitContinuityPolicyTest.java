package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CouiClockAodExitContinuityPolicyTest {
    @Test
    public void pendingDozeExitKeepsReplacementOwnerDuringTransientHide() {
        assertEquals(CouiClockAodExitContinuityPolicy.Decision.KEEP_REPLACEMENT,
                CouiClockAodExitContinuityPolicy.decide(
                        true,
                        true,
                        CouiClockPluginPresentationMapper.Action.HIDE));
    }

    @Test
    public void lockscreenPresentationCompletesPendingDozeExit() {
        assertTrue(CouiClockAodExitContinuityPolicy.completesHandoff(
                true,
                false,
                CouiClockPluginPresentationMapper.Action.PRESENT));
    }

    @Test
    public void ordinaryUnlockStillHidesReplacement() {
        assertEquals(CouiClockAodExitContinuityPolicy.Decision.HIDE_REPLACEMENT,
                CouiClockAodExitContinuityPolicy.decide(
                        false,
                        false,
                        CouiClockPluginPresentationMapper.Action.HIDE));
    }

    @Test
    public void unarmedDozingHideCannotCreateAReplacementHold() {
        assertEquals(CouiClockAodExitContinuityPolicy.Decision.HIDE_REPLACEMENT,
                CouiClockAodExitContinuityPolicy.decide(
                        false,
                        true,
                        CouiClockPluginPresentationMapper.Action.HIDE));
    }
}
