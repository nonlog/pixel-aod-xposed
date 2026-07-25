package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PixelFingerprintIconPolicyTest {
    @Test
    public void usesAodOutlineForEveryObservedScreenOffState() {
        assertTrue(PixelFingerprintIconPolicy.useAodStyle(true, false, false));
        assertTrue(PixelFingerprintIconPolicy.useAodStyle(false, true, false));
        assertTrue(PixelFingerprintIconPolicy.useAodStyle(false, false, true));
        assertFalse(PixelFingerprintIconPolicy.useAodStyle(false, false, false));
        assertTrue(PixelFingerprintIconPolicy.useAodStyle(
                false, false, false, false));
        assertFalse(PixelFingerprintIconPolicy.useAodStyle(
                true, false, false, false));
    }

    @Test
    public void fadesLockscreenBackgroundOutBeforeAod() {
        assertEquals(1f, PixelFingerprintIconPolicy.lockscreenLayerAlpha(0f), 0f);
        assertEquals(0.5f, PixelFingerprintIconPolicy.lockscreenLayerAlpha(0.5f), 0f);
        assertEquals(0f, PixelFingerprintIconPolicy.lockscreenLayerAlpha(1f), 0f);
        assertEquals(1f, PixelFingerprintIconPolicy.lockscreenBackgroundAlpha(
                true, 0f), 0f);
        assertEquals(0f, PixelFingerprintIconPolicy.lockscreenBackgroundAlpha(
                false, 0f), 0f);
    }

    @Test
    public void replacesOnlyPrimaryCarrier() {
        assertTrue(PixelFingerprintIconPolicy.shouldReplaceCarrier(true));
        assertFalse(PixelFingerprintIconPolicy.shouldReplaceCarrier(false));
    }

    @Test
    public void keepsNativePressedLayerVisibleOnlyWhileFingerIsDown() {
        assertFalse(PixelFingerprintIconPolicy.shouldShowNativePressedLayer(false));
        assertTrue(PixelFingerprintIconPolicy.shouldShowNativePressedLayer(true));
    }

    @Test
    public void routesExistingFingerprintViewThroughItsOwnerHandler() {
        assertEquals(PixelFingerprintIconPolicy.DispatchTarget.VIEW_HANDLER,
                PixelFingerprintIconPolicy.dispatchTarget(true));
        assertEquals(PixelFingerprintIconPolicy.DispatchTarget.MAIN_DISCOVERY,
                PixelFingerprintIconPolicy.dispatchTarget(false));
    }

    @Test
    public void makesResolvedSurfaceColorOpaqueWithoutChangingRgb() {
        assertEquals(0xff123456,
                PixelFingerprintIconPolicy.opaqueColor(0x40123456));
    }

    @Test
    public void identifiesCompetingCouiFingerprintDrawable() {
        assertTrue(PixelFingerprintIconPolicy.isCompetingDrawableClass(
                "one.dot.couiexpressive.hooks.systemui.StockUdfpsIconHook$StockFingerprintDrawable"));
        assertFalse(PixelFingerprintIconPolicy.isCompetingDrawableClass(
                "dev.codex.pixelaod.PixelFingerprintDrawable"));
        assertFalse(PixelFingerprintIconPolicy.isCompetingDrawableClass(
                "android.graphics.drawable.AnimatedVectorDrawable"));
        assertFalse(PixelFingerprintIconPolicy.isCompetingDrawableClass(null));
    }

    @Test
    public void replacesWhenEnabledEvenIfCouiDrawableIsActive() {
        String couiDrawable =
                "one.dot.couiexpressive.hooks.systemui.StockUdfpsIconHook$StockFingerprintDrawable";

        assertTrue(PixelFingerprintIconPolicy.shouldUsePixelIcon(true,
                "android.graphics.drawable.AnimatedVectorDrawable"));
        assertFalse(PixelFingerprintIconPolicy.shouldUsePixelIcon(false,
                "android.graphics.drawable.AnimatedVectorDrawable"));
        assertTrue(PixelFingerprintIconPolicy.shouldUsePixelIcon(true, couiDrawable));
    }
}
