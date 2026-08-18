package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiUdfpsPressedVisualPolicyTest {
    @Test
    public void primaryDrawableIsPressedOnlyForLiveTouchWithHdrEnabled() {
        assertTrue(CouiUdfpsPressedVisualPolicy.primaryDrawablePressed(true, true));
        assertFalse(CouiUdfpsPressedVisualPolicy.primaryDrawablePressed(true, false));
        assertFalse(CouiUdfpsPressedVisualPolicy.primaryDrawablePressed(false, true));
        assertFalse(CouiUdfpsPressedVisualPolicy.primaryDrawablePressed(false, false));
    }

    @Test
    public void replacementIlluminationIsOpaqueOnlyDuringLiveHdrTouch() {
        assertEquals(255,
                CouiUdfpsPressedVisualPolicy.illuminationAlpha(true, true));
        assertEquals(0,
                CouiUdfpsPressedVisualPolicy.illuminationAlpha(false, true));
        assertEquals(0,
                CouiUdfpsPressedVisualPolicy.illuminationAlpha(true, false));
        assertEquals(0,
                CouiUdfpsPressedVisualPolicy.illuminationAlpha(false, false));
    }

    @Test
    public void idleKeepsVendorCarrierOwnershipButRemovesOurIllumination() {
        CouiUdfpsPressedVisualPolicy.VisualState state =
                CouiUdfpsPressedVisualPolicy.resolve(false, true, true);

        assertFalse(state.primaryDrawablePressed());
        assertEquals(0, state.moduleIlluminationAlpha());
        assertTrue(state.preserveVendorCarrierVisibility());
    }

    @Test
    public void liveHdrTouchKeepsVendorCarrierAndLightsOurIllumination() {
        CouiUdfpsPressedVisualPolicy.VisualState state =
                CouiUdfpsPressedVisualPolicy.resolve(true, true, true);

        assertTrue(state.primaryDrawablePressed());
        assertEquals(255, state.moduleIlluminationAlpha());
        assertTrue(state.preserveVendorCarrierVisibility());
    }

    @Test
    public void stablePressedCarrierIsInvisibleWhenIdleAndRestoresOriginalAlphaOnTouch() {
        assertEquals(0f,
                CouiUdfpsPressedVisualPolicy.pressedCarrierViewAlpha(false, 0.73f), 0f);
        assertEquals(0.73f,
                CouiUdfpsPressedVisualPolicy.pressedCarrierViewAlpha(true, 0.73f), 0f);
        assertEquals(1f,
                CouiUdfpsPressedVisualPolicy.pressedCarrierViewAlpha(true, 2f), 0f);
    }
}
