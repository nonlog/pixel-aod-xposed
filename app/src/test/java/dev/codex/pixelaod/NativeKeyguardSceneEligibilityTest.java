package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class NativeKeyguardSceneEligibilityTest {
    @Test
    public void unknownStatePreservesExistingFallback() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot snapshot = gate.snapshot();

        assertFalse(snapshot.hasAuthoritativeDecision());
        assertTrue(snapshot.allowsPresentationFallbackTrue());
    }

    @Test
    public void lockscreenToAodStaysEligibleAcrossNativeTransition() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot started = gate.observe(
                "LOCKSCREEN", "AOD", 0.0f, "STARTED", "owner", "start");
        NativeKeyguardSceneEligibility.Snapshot running = gate.observe(
                "LOCKSCREEN", "AOD", 0.45f, "RUNNING", "owner", "running");
        NativeKeyguardSceneEligibility.Snapshot finished = gate.observe(
                "LOCKSCREEN", "AOD", 1.0f, "FINISHED", "owner", "finish");

        assertTrue(started.presentationAllowed);
        assertTrue(running.presentationAllowed);
        assertTrue(finished.presentationAllowed);
        assertTrue(running.isDozePresentationTransition());
    }

    @Test
    public void enteringBouncerSuppressesPixelImmediately() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot started = gate.observe(
                "LOCKSCREEN", "PRIMARY_BOUNCER", 0.0f, "STARTED", "owner", "start");

        assertFalse(started.presentationAllowed);
        assertFalse(started.allowsPresentationFallbackTrue());
    }

    @Test
    public void leavingBouncerWaitsForFinishedLockscreenState() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot running = gate.observe(
                "PRIMARY_BOUNCER", "LOCKSCREEN", 0.7f, "RUNNING", "owner", "running");
        NativeKeyguardSceneEligibility.Snapshot finished = gate.observe(
                "PRIMARY_BOUNCER", "LOCKSCREEN", 1.0f, "FINISHED", "owner", "finish");

        assertFalse(running.presentationAllowed);
        assertTrue(finished.presentationAllowed);
    }

    @Test
    public void canceledBouncerEntryReturnsToLockscreenEligibility() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();
        gate.observe("LOCKSCREEN", "PRIMARY_BOUNCER", 0.4f,
                "RUNNING", "owner", "running");

        NativeKeyguardSceneEligibility.Snapshot canceled = gate.observe(
                "LOCKSCREEN", "PRIMARY_BOUNCER", 0.4f, "CANCELED", "owner", "cancel");

        assertTrue(canceled.presentationAllowed);
    }

    @Test
    public void occludedAndGoneAreAuthoritativeSuppressionScenes() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(gate.observe("LOCKSCREEN", "OCCLUDED", 1.0f,
                "FINISHED", "owner", "occluded").presentationAllowed);
        assertFalse(gate.observe("LOCKSCREEN", "GONE", 1.0f,
                "FINISHED", "owner", "gone").presentationAllowed);
    }

    @Test
    public void undefinedNativeSceneDoesNotOverrideFallback() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot snapshot = gate.observe(
                "UNDEFINED", "UNDEFINED", 1.0f, "FINISHED", "owner", "undefined");

        assertNull(snapshot.presentationAllowed);
        assertTrue(snapshot.allowsPresentationFallbackTrue());
    }

    @Test
    public void dozingIsEligibleAndProgressIsClampedForDiagnostics() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot snapshot = gate.observe(
                "LOCKSCREEN", "DOZING", 1.4f, "RUNNING", "owner", "running");

        assertTrue(snapshot.presentationAllowed);
        assertTrue(snapshot.value == 1.0f);
        assertTrue(snapshot.isDozePresentationTransition());
    }
    @Test
    public void decisionEdgesSuppressOnceAndRequireResyncAfterIneligibleScene() {
        assertTrue(NativeKeyguardSceneEligibility.becameIneligible(true, false));
        assertTrue(NativeKeyguardSceneEligibility.becameIneligible(null, false));
        assertFalse(NativeKeyguardSceneEligibility.becameIneligible(false, false));

        assertTrue(NativeKeyguardSceneEligibility.becameEligible(false, true));
        assertFalse(NativeKeyguardSceneEligibility.becameEligible(null, true));
        assertFalse(NativeKeyguardSceneEligibility.becameEligible(true, true));
    }

    @Test
    public void finishedGoneStateAllowsOnlyScopedNonLockscreenPrearmBypass() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot gone = gate.observe(
                "LOCKSCREEN", "GONE", 1.0f, "FINISHED", "owner", "gone");

        assertFalse(gone.presentationAllowed);
        assertTrue(gone.supportsNonLockscreenAodBypass());
    }

    @Test
    public void goneToDozingAllowsScopedBypassWhileNativePresentationIsStillIneligible() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        NativeKeyguardSceneEligibility.Snapshot running = gate.observe(
                "GONE", "DOZING", 0.45f, "RUNNING", "owner", "sleep");

        assertFalse(running.presentationAllowed);
        assertTrue(running.supportsNonLockscreenAodBypass());
    }

    @Test
    public void bouncerAndOccludedNeverQualifyForNonLockscreenBypass() {
        NativeKeyguardSceneEligibility gate = new NativeKeyguardSceneEligibility();

        assertFalse(gate.observe("LOCKSCREEN", "PRIMARY_BOUNCER", 0.2f,
                "RUNNING", "owner", "bouncer").supportsNonLockscreenAodBypass());
        assertFalse(gate.observe("GONE", "OCCLUDED", 0.2f,
                "RUNNING", "owner", "occluded").supportsNonLockscreenAodBypass());
    }
}
