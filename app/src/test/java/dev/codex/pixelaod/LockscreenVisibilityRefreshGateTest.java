package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class LockscreenVisibilityRefreshGateTest {
    @Test
    public void interactiveTraversalMarksDirtyWithoutSchedulingFullSnapshotWork() {
        LockscreenVisibilityRefreshGate gate = new LockscreenVisibilityRefreshGate();

        assertFalse(gate.markDirty(false));
        assertFalse(gate.markDirty(false));
        assertTrue(gate.isDirty());
        assertFalse(gate.isScheduled());
    }

    @Test
    public void aodActivationSchedulesOneRefreshForAllDeferredDecisions() {
        LockscreenVisibilityRefreshGate gate = new LockscreenVisibilityRefreshGate();

        gate.markDirty(false);
        gate.markDirty(false);
        assertTrue(gate.requestIfDirty(true));
        assertFalse(gate.requestIfDirty(true));
        assertTrue(gate.beginDispatch(true));
        assertFalse(gate.isDirty());
    }

    @Test
    public void repeatedDozingDecisionsCoalesceUntilThePostedRefreshRuns() {
        LockscreenVisibilityRefreshGate gate = new LockscreenVisibilityRefreshGate();

        assertTrue(gate.markDirty(true));
        assertFalse(gate.markDirty(true));
        assertFalse(gate.markDirty(true));
        assertTrue(gate.beginDispatch(true));
        assertFalse(gate.beginDispatch(true));
    }

    @Test
    public void dispatchThatRunsAfterWakeLeavesDirtyStateForNextAod() {
        LockscreenVisibilityRefreshGate gate = new LockscreenVisibilityRefreshGate();

        assertTrue(gate.markDirty(true));
        assertFalse(gate.beginDispatch(false));
        assertTrue(gate.isDirty());
        assertTrue(gate.requestIfDirty(true));
        assertTrue(gate.beginDispatch(true));
    }

    @Test
    public void publishingAnotherSnapshotConsumesPendingVisibilityWork() {
        LockscreenVisibilityRefreshGate gate = new LockscreenVisibilityRefreshGate();

        assertTrue(gate.markDirty(true));
        gate.onSnapshotPublished();
        assertFalse(gate.isDirty());
        assertFalse(gate.isScheduled());
        assertFalse(gate.beginDispatch(true));
    }
}
