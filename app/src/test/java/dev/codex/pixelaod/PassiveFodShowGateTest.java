package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PassiveFodShowGateTest {
    @Test
    public void suppressesSteadyAodShowFromRecentProximityFar() {
        assertTrue(PassiveFodShowGate.shouldSuppress(
                15_000L, 20L, -1L));
    }

    @Test
    public void allowsInitialAodEntryShow() {
        assertFalse(PassiveFodShowGate.shouldSuppress(
                2_000L, 20L, -1L));
    }

    @Test
    public void allowsRecentExplicitWakeTrigger() {
        assertFalse(PassiveFodShowGate.shouldSuppress(
                15_000L, 20L, 300L));
    }

    @Test
    public void allowsShowNotCausedByRecentProximityFar() {
        assertFalse(PassiveFodShowGate.shouldSuppress(
                15_000L, 2_000L, -1L));
    }

    @Test
    public void suppressesDelayedShowInsidePassiveProximitySession() {
        assertTrue(PassiveFodShowGate.shouldSuppress(
                15_000L, 1_000L, -1L));
    }

    @Test
    public void recognizesAllObservedFodShowInvocations() {
        assertTrue(PassiveFodShowGate.isFodShowInvocation("notifyShowAodIcon", new Object[0]));
        assertTrue(PassiveFodShowGate.isFodShowInvocation("showUdfpsOverlay", new Object[] { 4 }));
        assertTrue(PassiveFodShowGate.isFodShowInvocation("fpIconShow", new Object[0]));
        assertTrue(PassiveFodShowGate.isFodShowInvocation(
                "showFingerprintIconTemporarily", new Object[0]));
        assertTrue(PassiveFodShowGate.isFodShowInvocation(
                "setFpIconVisibilityInAOD", new Object[] { true }));
        assertTrue(PassiveFodShowGate.isFodShowInvocation(
                "setFingerprintIconShow", new Object[] { true }));
        assertTrue(PassiveFodShowGate.isFodShowInvocation(
                "showOrHideFingerprintIconTemporarily", new Object[] { true }));
        assertFalse(PassiveFodShowGate.isFodShowInvocation(
                "setFpIconVisibilityInAOD", new Object[] { false }));
        assertFalse(PassiveFodShowGate.isFodShowInvocation(
                "showOrHideFingerprintIconTemporarily", new Object[] { false }));
        assertFalse(PassiveFodShowGate.isFodShowInvocation("notifyHideAodIcon", new Object[0]));
    }
}
