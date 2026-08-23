package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NativeDozeTransitionProgressAdapterTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void seamStartsUnavailableAndCannotBeConsumed() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();

        NativeDozeTransitionProgressAdapter.Snapshot snapshot = adapter.snapshot();

        assertFalse(snapshot.seamAvailable);
        assertFalse(snapshot.reliable);
        assertFalse(snapshot.continuousObserved);
        assertFalse(snapshot.canConsume(true, true));
    }

    @Test
    public void lockscreenToDozingNormalizesEnteringAmbientProgress() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");

        NativeDozeTransitionProgressAdapter.Snapshot snapshot = adapter.observe(
                "LOCKSCREEN", "DOZING", 0.25f, "RUNNING", "owner", "step");

        assertTrue(snapshot.reliable);
        assertTrue(snapshot.continuousObserved);
        assertEquals(NativeDozeTransitionProgressAdapter.Direction.ENTERING_AMBIENT,
                snapshot.direction);
        assertEquals(0.25f, snapshot.transitionProgress, EPSILON);
        assertEquals(0.25f, snapshot.ambientFraction, EPSILON);
    }

    @Test
    public void aodToLockscreenNormalizesAmbientFractionBackTowardZero() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");

        NativeDozeTransitionProgressAdapter.Snapshot snapshot = adapter.observe(
                "AOD", "LOCKSCREEN", 0.25f, "RUNNING", "owner", "step");

        assertTrue(snapshot.reliable);
        assertTrue(snapshot.continuousObserved);
        assertEquals(NativeDozeTransitionProgressAdapter.Direction.LEAVING_AMBIENT,
                snapshot.direction);
        assertEquals(0.25f, snapshot.transitionProgress, EPSILON);
        assertEquals(0.75f, snapshot.ambientFraction, EPSILON);
    }

    @Test
    public void onlyOrdinaryLockscreenAmbientHandoffsAreReliable() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");

        assertFalse(adapter.observe("GONE", "DOZING", 0.5f,
                "RUNNING", "owner", "step").reliable);
        assertFalse(adapter.observe("LOCKSCREEN", "PRIMARY_BOUNCER", 0.5f,
                "RUNNING", "owner", "step").reliable);
        assertFalse(adapter.observe("DOZING", "AOD", 0.5f,
                "RUNNING", "owner", "step").reliable);
    }

    @Test
    public void canceledOrInvalidSamplesAreNeverReliable() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");

        assertFalse(adapter.observe("LOCKSCREEN", "AOD", 0.4f,
                "CANCELED", "owner", "step").reliable);
        assertFalse(adapter.observe("LOCKSCREEN", "AOD", Float.NaN,
                "RUNNING", "owner", "step").reliable);
    }

    @Test
    public void canceledTransitionClearsContinuousCapability() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");
        assertTrue(adapter.observe("LOCKSCREEN", "DOZING", 0.5f,
                "RUNNING", "owner", "step").continuousObserved);

        NativeDozeTransitionProgressAdapter.Snapshot canceled = adapter.observe(
                "LOCKSCREEN", "DOZING", 0.5f, "CANCELED", "owner", "step");
        NativeDozeTransitionProgressAdapter.Snapshot finished = adapter.observe(
                "LOCKSCREEN", "DOZING", 1.0f, "FINISHED", "owner", "step");

        assertFalse(canceled.continuousObserved);
        assertFalse(finished.continuousObserved);
        assertFalse(finished.canConsume(true, true));
    }

    @Test
    public void nativeProgressIsClampedButNeverFabricated() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");

        NativeDozeTransitionProgressAdapter.Snapshot above = adapter.observe(
                "LOCKSCREEN", "AOD", 1.4f, "RUNNING", "owner", "step");
        assertEquals(1.0f, above.transitionProgress, EPSILON);
        assertEquals(1.0f, above.ambientFraction, EPSILON);

        NativeDozeTransitionProgressAdapter.Snapshot below = adapter.observe(
                "AOD", "LOCKSCREEN", -0.2f, "RUNNING", "owner", "step");
        assertEquals(0.0f, below.transitionProgress, EPSILON);
        assertEquals(1.0f, below.ambientFraction, EPSILON);
    }

    @Test
    public void consumptionRequiresReliableSignalNativePermissionAndAnimations() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");
        NativeDozeTransitionProgressAdapter.Snapshot snapshot = adapter.observe(
                "LOCKSCREEN", "DOZING", 0.5f, "RUNNING", "owner", "step");

        assertFalse(snapshot.canConsume(false, true));
        assertFalse(snapshot.canConsume(true, false));
        assertTrue(snapshot.canConsume(true, true));
    }

    @Test
    public void endpointOnlyTransitionNeverClaimsContinuousCapability() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");

        NativeDozeTransitionProgressAdapter.Snapshot snapshot = adapter.observe(
                "DOZING", "LOCKSCREEN", 1.0f, "FINISHED", "owner", "step");

        assertTrue(snapshot.reliable);
        assertFalse(snapshot.continuousObserved);
        assertEquals(0.0f, snapshot.ambientFraction, EPSILON);
        assertFalse(snapshot.canConsume(true, true));
    }

    @Test
    public void currentOosStyleScreenOffControlDenialKeepsProgressObserveOnly() {
        NativeDozeTransitionProgressAdapter adapter = new NativeDozeTransitionProgressAdapter();
        adapter.markSeamAvailable("hook");
        NativeDozeTransitionProgressAdapter.Snapshot progress = adapter.observe(
                "LOCKSCREEN", "DOZING", 0.5f, "RUNNING", "owner", "step");

        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.beginTransition("sleep");
        gate.observeDisplayNeedsBlanking(false, "blanking");
        gate.observeShouldControlScreenOff(false, "control");
        gate.observeShouldAnimateDozingChange(true, "dozing-change");

        assertTrue(progress.reliable);
        assertFalse(gate.allowsVendorProgress());
        assertFalse(progress.canConsume(gate.allowsVendorProgress(), true));
    }
}
