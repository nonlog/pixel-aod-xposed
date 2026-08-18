package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class CouiUdfpsPressedAnimationPolicyTest {
    @Test
    public void replacementHdrPathSuppressesVendorPressedAnimation() {
        assertTrue(CouiUdfpsPressedAnimationPolicy.shouldSuppress(true, true));
        assertEquals(Boolean.FALSE,
                CouiUdfpsPressedAnimationPolicy.overrideHasPressedAnimation(true, true));
        assertEquals(Float.valueOf(1f),
                CouiUdfpsPressedAnimationPolicy.overrideScalePressedAnim(true, true));
    }

    @Test
    public void replacementSuppressesVendorAnimationRegardlessOfHdrMode() {
        assertFalse(CouiUdfpsPressedAnimationPolicy.shouldSuppress(false, true));
        assertTrue(CouiUdfpsPressedAnimationPolicy.shouldSuppress(true, false));
        assertNull(CouiUdfpsPressedAnimationPolicy.overrideHasPressedAnimation(false, true));
        assertEquals(Boolean.FALSE,
                CouiUdfpsPressedAnimationPolicy.overrideHasPressedAnimation(true, false));
        assertEquals(Float.valueOf(1f),
                CouiUdfpsPressedAnimationPolicy.overrideScalePressedAnim(true, false));
    }
}
