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
    public void nonBouncerTransitionsRemainUnderExistingSceneGate() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "GONE", 0.2f, "RUNNING", "owner", "gone")));
        assertFalse(CouiBouncerHostVisibilityPolicy.nativeHostOwns(gate.observe(
                "LOCKSCREEN", "DOZING", 0.2f, "RUNNING", "owner", "dozing")));
    }
}
