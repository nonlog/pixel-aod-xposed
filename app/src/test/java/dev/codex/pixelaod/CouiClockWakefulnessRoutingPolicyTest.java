package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockWakefulnessRoutingPolicyTest {
    @Test
    public void onlyProcessGlobalWakeEventArmsCouiAodExit() {
        assertTrue(CouiClockWakefulnessRoutingPolicy.shouldArmAodExit(
                "dispatchStartedWakingUp"));
        assertFalse(CouiClockWakefulnessRoutingPolicy.shouldArmAodExit(
                "dispatchStartedGoingToSleep"));
        assertFalse(CouiClockWakefulnessRoutingPolicy.shouldArmAodExit(
                "onDreamingStopped"));
        assertFalse(CouiClockWakefulnessRoutingPolicy.shouldArmAodExit(
                "ACTION_SCREEN_ON"));
        assertFalse(CouiClockWakefulnessRoutingPolicy.shouldArmAodExit(null));
    }

    @Test
    public void armSourceIdentifiesTheGlobalWakefulnessCallback() {
        assertEquals(
                "WakefulnessLifecycle#dispatchStartedWakingUp",
                CouiClockWakefulnessRoutingPolicy.aodExitArmSource());
    }
}
