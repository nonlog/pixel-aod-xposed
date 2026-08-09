package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

public final class PixelAodDebugLogGateTest {
    @Test
    public void boundsDebugMessagesWithinOneWindowAndReportsSuppression() {
        DebugLogGate gate = new DebugLogGate(1_000L, 3);

        assertTrue(gate.acquire("first", 0L, 0L).emit);
        assertTrue(gate.acquire("second", 1L, 0L).emit);
        assertTrue(gate.acquire("third", 2L, 0L).emit);
        assertFalse(gate.acquire("fourth", 3L, 0L).emit);

        DebugLogGate.Decision nextWindow = gate.acquire("next", 1_000L, 0L);
        assertTrue(nextWindow.emit);
        assertEquals(1, nextWindow.suppressedBefore);
    }

    @Test
    public void suppressesTheSameHotCategoryUntilItsIntervalExpires() {
        DebugLogGate gate = new DebugLogGate(1_000L, 10);

        assertTrue(gate.acquire("clock-paint", 100L, 250L).emit);
        assertFalse(gate.acquire("clock-paint", 200L, 250L).emit);
        assertFalse(gate.acquire("clock-paint", 349L, 250L).emit);
        assertTrue(gate.acquire("clock-paint", 350L, 250L).emit);
    }

    @Test
    public void disabledDebugLoggingDoesNotBuildLazyMessages() {
        AtomicInteger builds = new AtomicInteger();
        PixelAodLog.setDebugEnabled(false);

        PixelAodLog.log("AOD policy decision", () -> {
            builds.incrementAndGet();
            return "expensive state";
        });

        assertEquals(0, builds.get());
    }
}
