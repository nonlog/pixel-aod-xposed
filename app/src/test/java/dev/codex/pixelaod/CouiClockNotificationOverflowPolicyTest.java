package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockNotificationOverflowPolicyTest {
    @Test
    public void atOrBelowNativeCapacityHasNoOverflow() {
        for (int count = 0; count <= 3; count++) {
            CouiClockNotificationOverflowPolicy.Plan plan =
                    CouiClockNotificationOverflowPolicy.forCount(count, 3);
            assertEquals(count, plan.visibleCount());
            assertEquals(0, plan.hiddenCount());
            assertFalse(plan.hasOverflow());
        }
    }

    @Test
    public void aboveNativeCapacityKeepsCapacityAndUsesOverflowDot() {
        CouiClockNotificationOverflowPolicy.Plan plan =
                CouiClockNotificationOverflowPolicy.forCount(4, 3);
        assertEquals(3, plan.visibleCount());
        assertEquals(1, plan.hiddenCount());
        assertTrue(plan.hasOverflow());
    }

    @Test
    public void capacityIsAnInputRatherThanModuleFiveIconConstant() {
        CouiClockNotificationOverflowPolicy.Plan plan =
                CouiClockNotificationOverflowPolicy.forCount(12, 7);
        assertEquals(7, plan.visibleCount());
        assertEquals(5, plan.hiddenCount());
        assertTrue(plan.hasOverflow());
    }
}