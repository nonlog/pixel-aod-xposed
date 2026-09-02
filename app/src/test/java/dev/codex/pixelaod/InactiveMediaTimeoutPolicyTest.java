package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InactiveMediaTimeoutPolicyTest {
    private static final long TEN_MINUTES = 10L * 60L * 1000L;

    @Test
    public void tenMinuteBoundaryIsExclusive() {
        long startedAt = 1_000_000L;
        assertTrue(InactiveMediaTimeoutPolicy.isWithinTimeout(
                startedAt, startedAt + TEN_MINUTES - 1L, TEN_MINUTES));
        assertFalse(InactiveMediaTimeoutPolicy.isWithinTimeout(
                startedAt, startedAt + TEN_MINUTES, TEN_MINUTES));
        assertEquals(startedAt + TEN_MINUTES,
                InactiveMediaTimeoutPolicy.deadline(startedAt, TEN_MINUTES));
    }

    @Test
    public void elapsedDeepSleepTimeCountsTowardExpiry() {
        long startedAt = 20_000L;
        long nowAfterTwentySixMinutes = startedAt + 26L * 60L * 1000L;
        assertEquals(0L, InactiveMediaTimeoutPolicy.remainingDelay(
                startedAt, nowAfterTwentySixMinutes, TEN_MINUTES));
        assertFalse(InactiveMediaTimeoutPolicy.isWithinTimeout(
                startedAt, nowAfterTwentySixMinutes, TEN_MINUTES));
    }
}
