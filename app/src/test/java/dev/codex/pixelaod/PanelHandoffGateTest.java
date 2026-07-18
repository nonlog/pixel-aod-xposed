package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PanelHandoffGateTest {
    @Test
    public void rejectsStaleCallbackAfterGenerationReplacement() {
        // Given
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult stale = gate.openOrExtend("trace-1", 0L);
        PanelHandoffGate.OpenResult current = gate.openOrExtend("trace-2", 10L);

        // When
        boolean staleCompleted = gate.completeIfCurrent(
                stale.traceId, stale.generation, 110L);

        // Then
        assertFalse(staleCompleted);
        assertTrue(gate.shouldBlockPresentation("trace-2"));
        assertTrue(gate.completeIfCurrent(
                current.traceId, current.generation, 110L));
    }

    @Test
    public void coalescesDuplicateRequestAndExtendsItsDeadline() {
        // Given
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult opened = gate.openOrExtend("trace-1", 0L);

        // When
        PanelHandoffGate.OpenResult extended = gate.openOrExtend("trace-1", 50L);

        // Then
        assertEquals(opened.generation, extended.generation);
        assertEquals(0L, extended.openedAtMillis);
        assertEquals(150L, extended.deadlineMillis);
        assertTrue(extended.extended);
        assertFalse(gate.completeIfCurrent("trace-1", opened.generation, 149L));
        assertTrue(gate.completeIfCurrent("trace-1", opened.generation, 150L));
    }

    @Test
    public void cancelsCurrentGenerationImmediately() {
        // Given
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult opened = gate.openOrExtend("trace-1", 0L);

        // When
        boolean cancelled = gate.cancel();

        // Then
        assertTrue(cancelled);
        assertFalse(gate.shouldBlockPresentation("trace-1"));
        assertFalse(gate.completeIfCurrent(
                opened.traceId, opened.generation, 100L));
    }

    @Test
    public void completesCurrentGenerationAtItsDeadlineOnlyOnce() {
        // Given
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult opened = gate.openOrExtend("trace-1", 4L);

        // When
        boolean completed = gate.completeIfCurrent(
                opened.traceId, opened.generation, 104L);

        // Then
        assertTrue(completed);
        assertFalse(gate.shouldBlockPresentation("trace-1"));
        assertFalse(gate.completeIfCurrent(
                opened.traceId, opened.generation, 104L));
    }

    @Test
    public void rejectsCallbackForWrongTraceEvenWhenGenerationMatches() {
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult opened = gate.openOrExtend("trace-1", 0L);

        assertFalse(gate.completeIfCurrent("trace-other", opened.generation, 100L));
        assertTrue(gate.shouldBlockPresentation("trace-1"));
    }

    @Test
    public void doesNotReopenCompletedTrace() {
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult opened = gate.openOrExtend("trace-1", 0L);
        assertTrue(gate.completeIfCurrent("trace-1", opened.generation, 100L));

        PanelHandoffGate.OpenResult duplicate = gate.openOrExtend("trace-1", 200L);

        assertFalse(duplicate.accepted);
        assertFalse(gate.shouldBlockPresentation("trace-1"));
    }

    @Test
    public void staleCancellationCannotCancelReplacementGeneration() {
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult stale = gate.openOrExtend("trace-1", 0L);
        PanelHandoffGate.OpenResult current = gate.openOrExtend("trace-2", 10L);

        assertFalse(gate.cancelIfCurrent(stale.traceId, stale.generation));
        assertTrue(gate.shouldBlockPresentation("trace-2"));
        assertTrue(gate.cancelIfCurrent(current.traceId, current.generation));
        assertFalse(gate.shouldBlockPresentation("trace-2"));
    }

    @Test
    public void doesNotReopenOlderCompletedTraceAfterNewerTraceCompletes() {
        PanelHandoffGate gate = new PanelHandoffGate(100L);
        PanelHandoffGate.OpenResult first = gate.openOrExtend("trace-1", 0L);
        assertTrue(gate.completeIfCurrent("trace-1", first.generation, 100L));
        PanelHandoffGate.OpenResult second = gate.openOrExtend("trace-2", 200L);
        assertTrue(gate.completeIfCurrent("trace-2", second.generation, 300L));

        PanelHandoffGate.OpenResult lateFirst = gate.openOrExtend("trace-1", 400L);

        assertFalse(lateFirst.accepted);
        assertFalse(gate.shouldBlockPresentation("trace-1"));
    }
}
