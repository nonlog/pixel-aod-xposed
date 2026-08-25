package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockNotificationOverflowPolicyTest {
    @Test
    public void zeroToFiveIconsHaveNoOverflow() {
        for (int count = 0; count <= 5; count++) {
            CouiClockNotificationOverflowPolicy.Plan plan =
                    CouiClockNotificationOverflowPolicy.forCount(count);
            assertEquals(count, plan.visibleCount());
            assertEquals(0, plan.hiddenCount());
            assertFalse(plan.hasOverflow());
            assertEquals("", plan.overflowText());
        }
    }

    @Test
    public void sixIconsShowFiveAndPlusOne() {
        CouiClockNotificationOverflowPolicy.Plan plan =
                CouiClockNotificationOverflowPolicy.forCount(6);

        assertEquals(5, plan.visibleCount());
        assertEquals(1, plan.hiddenCount());
        assertTrue(plan.hasOverflow());
        assertEquals("+1", plan.overflowText());
    }

    @Test
    public void largerCountsUseExactHiddenCountInOverflowLabel() {
        CouiClockNotificationOverflowPolicy.Plan plan =
                CouiClockNotificationOverflowPolicy.forCount(12);

        assertEquals(5, plan.visibleCount());
        assertEquals(7, plan.hiddenCount());
        assertEquals("+7", plan.overflowText());
    }
}
