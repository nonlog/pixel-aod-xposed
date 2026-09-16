package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiUdfpsHdrUpdatePolicyTest {
    @Test
    public void firstSurfaceUpdateAlwaysApplies() {
        assertTrue(CouiUdfpsHdrUpdatePolicy.shouldApply(
                false, false, false, false, false));
    }

    @Test
    public void identicalStateOnSameSurfaceIsSkipped() {
        assertFalse(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, true, true, true, false));
        assertFalse(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, false, true, false, false));
    }

    @Test
    public void pressTransitionAppliesOnSameSurface() {
        assertTrue(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, false, true, true, false));
        assertTrue(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, true, true, false, false));
    }

    @Test
    public void recreatedSurfaceReappliesEvenWhenPressStateIsUnchanged() {
        assertTrue(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, true, false, true, false));
        assertTrue(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, false, false, false, false));
    }

    @Test
    public void forcedLayoutRefreshReappliesOnSameSurface() {
        assertTrue(CouiUdfpsHdrUpdatePolicy.shouldApply(
                true, true, true, true, true));
    }
}
