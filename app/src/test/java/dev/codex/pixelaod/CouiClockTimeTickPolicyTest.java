package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CouiClockTimeTickPolicyTest {
    @Test
    public void acceptsClockAndDisplayLifecycleActions() {
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.TIME_TICK"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.TIME_SET"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.TIMEZONE_CHANGED"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.SCREEN_ON"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.SCREEN_OFF"));
        assertFalse(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.BATTERY_CHANGED"));
    }

    @Test
    public void refreshesOnlyWhenMinuteChanges() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(Long.MIN_VALUE, 123L));
        assertFalse(CouiClockTimeTickPolicy.shouldRefresh(123L, 123L));
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(123L, 124L));
    }
}
