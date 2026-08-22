package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CouiUdfpsOwnershipPolicyTest {
    @Test
    public void systemIconModeDoesNotOwnVendorVisualsButCanOwnSuccessRipple() {
        assertFalse(CouiUdfpsOwnershipPolicy.ownsReplacementVisuals(true, false));
        assertTrue(CouiUdfpsOwnershipPolicy.ownsSuccessRipple(true, true));
        assertFalse(CouiUdfpsOwnershipPolicy.mayMutateVendorVisuals(false, false));
    }

    @Test
    public void trackedReplacementCanBeRestoredOnceAfterToggleOff() {
        assertTrue(CouiUdfpsOwnershipPolicy.mayMutateVendorVisuals(false, true));
    }

    @Test
    public void systemIconModeSuppressesOnlyNativeUnlockRipple() {
        assertTrue(CouiUdfpsOwnershipPolicy.suppressStockRipple(true, true,
                false, true));
        assertFalse(CouiUdfpsOwnershipPolicy.suppressStockRipple(true, true,
                false, false));
    }

    @Test
    public void missingCustomRippleTargetNeverSuppressesNativeRipple() {
        assertFalse(CouiUdfpsOwnershipPolicy.suppressStockRipple(true, false,
                false, true));
        assertFalse(CouiUdfpsOwnershipPolicy.suppressStockRipple(false, true,
                false, true));
    }

    @Test
    public void replacementModeRetainsHistoricalRippleSuppression() {
        assertTrue(CouiUdfpsOwnershipPolicy.suppressStockRipple(true, true,
                true, false));
    }

    @Test
    public void releasePrimaryGlyphOwnershipIsExplicit() {
        assertTrue(PixelAodUdfpsRuntimePolicy.primaryGlyphOwnedBySystem(false));
        assertFalse(PixelAodUdfpsRuntimePolicy.primaryGlyphOwnedBySystem(true));
    }
}
