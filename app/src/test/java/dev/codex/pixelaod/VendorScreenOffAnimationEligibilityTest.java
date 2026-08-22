package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VendorScreenOffAnimationEligibilityTest {
    @Test
    public void unknownNativeStatePreservesExistingStableMorphButDoesNotAuthorizeProgress() {
        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.beginTransition("sleep");

        assertTrue(gate.allowsExistingMorph());
        assertFalse(gate.allowsVendorProgress());
    }

    @Test
    public void explicitDisplayBlankingBlocksExistingMorphAndProgress() {
        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.beginTransition("sleep");
        gate.observeDisplayNeedsBlanking(true, "DozeParameters#getDisplayNeedsBlanking");
        gate.observeShouldControlScreenOff(true, "DozeParameters#shouldControlScreenOff");
        gate.observeShouldAnimateDozingChange(true,
                "ScreenOffAnimationController#shouldAnimateDozingChange");

        assertFalse(gate.allowsExistingMorph());
        assertFalse(gate.allowsVendorProgress());
    }

    @Test
    public void nativeScreenOffControlDenialDoesNotVetoExistingMorphButBlocksProgress() {
        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.beginTransition("sleep");
        gate.observeDisplayNeedsBlanking(false, "blanking");
        gate.observeShouldControlScreenOff(false, "control");

        assertTrue(gate.allowsExistingMorph());
        assertFalse(gate.allowsVendorProgress());
    }

    @Test
    public void explicitDozingAnimationDenialBlocksExistingMorph() {
        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.beginTransition("sleep");
        gate.observeDisplayNeedsBlanking(false, "blanking");
        gate.observeShouldControlScreenOff(true, "control");
        gate.observeShouldAnimateDozingChange(false, "dozing-change");

        assertFalse(gate.allowsExistingMorph());
        assertFalse(gate.allowsVendorProgress());
    }

    @Test
    public void vendorProgressRequiresAllPositiveNativeSignals() {
        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.beginTransition("sleep");
        gate.observeDisplayNeedsBlanking(false, "blanking");
        gate.observeShouldControlScreenOff(true, "control");
        assertFalse(gate.allowsVendorProgress());

        gate.observeShouldAnimateDozingChange(true, "dozing-change");

        assertTrue(gate.allowsExistingMorph());
        assertTrue(gate.allowsVendorProgress());
    }

    @Test
    public void newTransitionClearsSceneSpecificSignalsButRetainsBlankingCapability() {
        VendorScreenOffAnimationEligibility gate = new VendorScreenOffAnimationEligibility();
        gate.observeDisplayNeedsBlanking(false, "blanking");
        gate.beginTransition("first");
        gate.observeShouldControlScreenOff(false, "control");
        gate.observeShouldAnimateDozingChange(false, "dozing-change");
        assertFalse(gate.allowsExistingMorph());
        assertFalse(gate.allowsVendorProgress());

        VendorScreenOffAnimationEligibility.Snapshot next = gate.beginTransition("second");

        assertTrue(next.allowsExistingMorph);
        assertFalse(next.allowsVendorProgress);
        assertFalse(Boolean.TRUE.equals(next.displayNeedsBlanking));
    }
}
