package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
}
