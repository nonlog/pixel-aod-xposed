package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CouiBouncerHostVisibilityPolicyTest {
    @Test
    public void lockscreenToBouncerMotionDelegatesVisibilityToNativeHost() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "PRIMARY_BOUNCER", 0.0f, "STARTED", "owner", "start")));
        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "PRIMARY_BOUNCER", 0.6f, "RUNNING", "owner", "running")));
        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "PRIMARY_BOUNCER", 1.0f, "FINISHED", "owner", "finish")));
    }

    @Test
    public void bouncerExitCancellationDoesNotCreateModuleVisibilityGate() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "PRIMARY_BOUNCER", "LOCKSCREEN", 0.7f, "CANCELED", "owner", "cancel")));
    }

    @Test
    public void settledLockscreenReturnsOwnershipToNormalModulePipeline() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "PRIMARY_BOUNCER", "LOCKSCREEN", 1.0f, "FINISHED", "owner", "finish")));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "PRIMARY_BOUNCER", 0.0f, "CANCELED", "owner", "cancel-entry")));
    }

    @Test
    public void alternateBouncerUsesSameNativeHostOwnership() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "ALTERNATE_BOUNCER", 0.3f, "RUNNING", "owner", "running")));
    }

    @Test
    public void lockscreenToOccludedPreservesClockUnderNativeHost() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "OCCLUDED", 0.0f, "STARTED", "owner", "alarm-start")));
        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "OCCLUDED", 0.6f, "RUNNING", "owner", "alarm-running")));
        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "OCCLUDED", 1.0f, "FINISHED", "owner", "alarm-visible")));
    }

    @Test
    public void occludedToLockscreenRevealsPreservedClockBeforeFinished() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "OCCLUDED", "LOCKSCREEN", 0.0f, "STARTED", "owner", "call-end-start")));
        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "OCCLUDED", "LOCKSCREEN", 0.7f, "RUNNING", "owner", "call-end-running")));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "OCCLUDED", "LOCKSCREEN", 1.0f, "FINISHED", "owner", "call-end-finish")));
    }

    @Test
    public void canceledOcclusionTransitionReturnsOwnershipToSettledScene() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "OCCLUDED", 0.4f, "CANCELED", "owner", "cancel-entry")));
        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "OCCLUDED", "LOCKSCREEN", 0.4f, "CANCELED", "owner", "cancel-exit")));
    }

    @Test
    public void nonLockscreenCanceledGoneEdgeKeepsPreparedHostOwnedByNativeRoot() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();
        NativeKeyguardSceneEligibility.Snapshot canceled = gate.observe(
                "LOCKSCREEN", "GONE", 0.0f, "CANCELED", "owner", "rapid-screen-off");

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwnsNonLockscreenAodEntryEdge(
                canceled, true, false));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwnsNonLockscreenAodEntryEdge(
                canceled, false, false));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwnsNonLockscreenAodEntryEdge(
                canceled, true, true));
    }

    @Test
    public void nonLockscreenGoneToDozingOwnsOnlyStartedEdge() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();
        NativeKeyguardSceneEligibility.Snapshot started = gate.observe(
                "GONE", "DOZING", 0.0f, "STARTED", "owner", "sleep-start");
        NativeKeyguardSceneEligibility.Snapshot running = gate.observe(
                "GONE", "DOZING", 0.2f, "RUNNING", "owner", "sleep-running");
        NativeKeyguardSceneEligibility.Snapshot finished = gate.observe(
                "GONE", "DOZING", 1.0f, "FINISHED", "owner", "sleep-finish");

        assertTrue(CouiBouncerHostVisibilityPolicy.nativeHostOwnsNonLockscreenAodEntryEdge(
                started, true, false));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwnsNonLockscreenAodEntryEdge(
                running, true, false));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwnsNonLockscreenAodEntryEdge(
                finished, true, false));
    }

    @Test
    public void unrelatedTransitionsRemainUnderExistingSceneGate() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "GONE", 0.2f, "RUNNING", "owner", "gone")));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "DOZING", 0.2f, "RUNNING", "owner", "dozing")));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "PRIMARY_BOUNCER", "OCCLUDED", 0.2f, "RUNNING", "owner", "cross-transient")));
    }
}
