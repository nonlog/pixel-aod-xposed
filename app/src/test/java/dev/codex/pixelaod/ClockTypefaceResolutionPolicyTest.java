package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClockTypefaceResolutionPolicyTest {
    @Test
    public void bundledVariableFontUsesExactVariationInstances() {
        assertEquals(ClockTypefaceResolutionPolicy.Source.EXACT_VARIATION,
                ClockTypefaceResolutionPolicy.weightedSource(true));
        assertEquals(ClockTypefaceResolutionPolicy.Source.DERIVED_WEIGHT,
                ClockTypefaceResolutionPolicy.weightedSource(false));
    }

    @Test
    public void aodHandoffReappliesTypefaceWhenLogicalWeightIsUnchanged() {
        assertTrue("The visible clock must re-submit its Typeface at the AOD handoff boundary",
                ClockTypefaceResolutionPolicy.shouldApplyTypeface(
                        300, 300, true));
    }

    @Test
    public void sharedClockTypefaceUsesCouiRoundedAxis() {
        assertTrue(ClockTypefaceResolutionPolicy.sharedClockVariationSettings(280)
                .contains("'ROND' 100"));
    }

    @Test
    public void oos1609UsesStableSingleLayerAodHandoff() {
        assertFalse(OosAodHandoffProfile.usesStableSingleLayerAodHandoff(
                "CPH2573_16.0.5.400(EX01)"));
        assertTrue(OosAodHandoffProfile.usesStableSingleLayerAodHandoff(
                "CPH2573_16.0.9.400(EX01)"));
        assertTrue(OosAodHandoffProfile.usesStableSingleLayerAodHandoff(
                "CPH2573_16.0.10.100(EX01)"));
    }

    @Test
    public void oos1609LetsTheSystemOwnBurnInTranslation() {
        assertFalse(OosAodHandoffProfile.usesSystemManagedBurnIn(
                "CPH2573_16.0.5.400(EX01)"));
        assertTrue(OosAodHandoffProfile.usesSystemManagedBurnIn(
                "CPH2573_16.0.9.400(EX01)"));
    }

    @Test
    public void oos1609DoesNotDelayNonLockscreenAodReplacement() {
        assertEquals(810L, OosAodHandoffProfile.nonLockscreenRevealDelayMillis(
                "CPH2573_16.0.5.400(EX01)"));
        assertEquals(0L, OosAodHandoffProfile.nonLockscreenRevealDelayMillis(
                "CPH2573_16.0.9.400(EX01)"));
    }
}
