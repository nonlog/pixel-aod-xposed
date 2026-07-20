package dev.codex.pixelaod;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NotificationIconDisplayPlanTest {
    @Test
    public void keepsFiveIconsWithoutOverflow() {
        NotificationIconDisplayPlan plan = NotificationIconDisplayPlan.fromEligibleIconKeys(
                Arrays.asList("a", "b", "c", "d", "e"), 5);

        assertEquals(5, plan.totalIconCount());
        assertEquals(5, plan.visibleIconCount());
        assertEquals(0, plan.overflowCount());
        assertFalse(plan.hasOverflow());
    }

    @Test
    public void showsFiveIconsAndOneOverflowForSixEligibleIcons() {
        NotificationIconDisplayPlan plan = NotificationIconDisplayPlan.fromEligibleIconKeys(
                Arrays.asList("a", "b", "c", "d", "e", "f"), 5);

        assertEquals(6, plan.totalIconCount());
        assertEquals(5, plan.visibleIconCount());
        assertEquals(1, plan.overflowCount());
        assertTrue(plan.hasOverflow());
    }

    @Test
    public void countsOverflowAfterDeduplication() {
        NotificationIconDisplayPlan plan = NotificationIconDisplayPlan.fromEligibleIconKeys(
                Arrays.asList("a", "a", "b", "c", "d", "e", "f", "f", null), 5);

        assertEquals(6, plan.totalIconCount());
        assertEquals(5, plan.visibleIconCount());
        assertEquals(1, plan.overflowCount());
    }

    @Test
    public void supportsEmptyAndLargeNotificationSets() {
        NotificationIconDisplayPlan empty = NotificationIconDisplayPlan.fromEligibleIconKeys(
                Collections.emptyList(), 5);
        NotificationIconDisplayPlan large = NotificationIconDisplayPlan.fromEligibleIconKeys(
                Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i"), 5);

        assertEquals(0, empty.visibleIconCount());
        assertEquals(0, empty.overflowCount());
        assertEquals(5, large.visibleIconCount());
        assertEquals(4, large.overflowCount());
    }
}
