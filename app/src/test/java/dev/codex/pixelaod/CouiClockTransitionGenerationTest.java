package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockTransitionGenerationTest {
    @Test
    public void staleLiveAodGenerationCannotWinAfterRetarget() {
        CouiClockTransitionGeneration generations = new CouiClockTransitionGeneration();

        long first = generations.begin();
        long second = generations.begin();

        assertFalse(generations.isCurrent(first));
        assertTrue(generations.isCurrent(second));
    }

    @Test
    public void invalidationCancelsPendingGeneration() {
        CouiClockTransitionGeneration generations = new CouiClockTransitionGeneration();

        long pending = generations.begin();
        generations.invalidate();

        assertFalse(generations.isCurrent(pending));
    }
}
