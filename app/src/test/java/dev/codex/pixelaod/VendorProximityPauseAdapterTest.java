package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VendorProximityPauseAdapterTest {
    @Test
    public void rawNearStartsPausingWithoutHidingPresentation() {
        VendorProximityPauseAdapter adapter = new VendorProximityPauseAdapter();

        VendorProximityPauseAdapter.Snapshot snapshot =
                adapter.observeRawNear(true, "ProximityTask#setNear");

        assertEquals(VendorProximityPauseAdapter.Phase.PAUSING, snapshot.phase);
        assertTrue(snapshot.phaseChanged());
        assertFalse(snapshot.blocksPresentation());
        assertTrue(snapshot.blocksNotificationPulse());
    }

    @Test
    public void farBeforeVendorCommitCancelsPausingWithoutVisibleHide() {
        VendorProximityPauseAdapter adapter = new VendorProximityPauseAdapter();
        adapter.observeRawNear(true, "near");

        VendorProximityPauseAdapter.Snapshot snapshot =
                adapter.observeRawNear(false, "far-before-commit");

        assertEquals(VendorProximityPauseAdapter.Phase.ACTIVE, snapshot.phase);
        assertFalse(snapshot.blocksPresentation());
        assertFalse(snapshot.blocksNotificationPulse());
    }

    @Test
    public void vendorDelayedCommitMovesPausingToPaused() {
        VendorProximityPauseAdapter adapter = new VendorProximityPauseAdapter();
        adapter.observeRawNear(true, "near");

        VendorProximityPauseAdapter.Snapshot snapshot =
                adapter.observeCommittedNear(true, "ProximityTask#run");

        assertEquals(VendorProximityPauseAdapter.Phase.PAUSED, snapshot.phase);
        assertTrue(snapshot.blocksPresentation());
        assertTrue(snapshot.blocksNotificationPulse());
    }

    @Test
    public void rawFarAfterPausedWaitsForVendorCommitBeforeResumingPresentation() {
        VendorProximityPauseAdapter adapter = new VendorProximityPauseAdapter();
        adapter.observeRawNear(true, "near");
        adapter.observeCommittedNear(true, "near-commit");

        VendorProximityPauseAdapter.Snapshot rawFar =
                adapter.observeRawNear(false, "far");
        assertEquals(VendorProximityPauseAdapter.Phase.PAUSED, rawFar.phase);
        assertTrue(rawFar.blocksPresentation());

        VendorProximityPauseAdapter.Snapshot committedFar =
                adapter.observeCommittedNear(false, "far-commit");
        assertEquals(VendorProximityPauseAdapter.Phase.ACTIVE, committedFar.phase);
        assertFalse(committedFar.blocksPresentation());
        assertFalse(committedFar.blocksNotificationPulse());
    }

    @Test
    public void committedFarCannotBeOverriddenByStaleRawNear() {
        VendorProximityPauseAdapter adapter = new VendorProximityPauseAdapter();
        adapter.observeRawNear(true, "near");

        VendorProximityPauseAdapter.Snapshot snapshot =
                adapter.observeCommittedNear(false, "authoritative-far");

        assertEquals(VendorProximityPauseAdapter.Phase.ACTIVE, snapshot.phase);
        assertFalse(snapshot.rawNear);
        assertFalse(snapshot.blocksPresentation());
    }

    @Test
    public void resetFailsOpenFromBothPausingAndPaused() {
        VendorProximityPauseAdapter adapter = new VendorProximityPauseAdapter();
        adapter.observeRawNear(true, "near");
        VendorProximityPauseAdapter.Snapshot pausingReset = adapter.reset("lifecycle-reset");
        assertEquals(VendorProximityPauseAdapter.Phase.ACTIVE, pausingReset.phase);
        assertFalse(pausingReset.blocksNotificationPulse());

        adapter.observeRawNear(true, "near-again");
        adapter.observeCommittedNear(true, "near-commit");
        VendorProximityPauseAdapter.Snapshot pausedReset = adapter.reset("unregister");
        assertEquals(VendorProximityPauseAdapter.Phase.ACTIVE, pausedReset.phase);
        assertFalse(pausedReset.blocksPresentation());
    }
}
