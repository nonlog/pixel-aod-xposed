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

    @Test
    public void powerPolicyDenialDoesNotRearmCarrierWhileScreenIsNonInteractive() {
        OosAodLifecycleAdapter.PowerPolicyDecision automaticLowBattery =
                OosAodLifecycleAdapter.evaluatePowerPolicy(
                        false, true, true, false, 12, 15);
        OosAodLifecycleAdapter.PowerPolicyDecision manualPowerSaver =
                OosAodLifecycleAdapter.evaluatePowerPolicy(
                        true, true, false, false, 98, 15);

        assertFalse(automaticLowBattery.allowsDisplay);
        assertFalse(manualPowerSaver.allowsDisplay);
        assertTrue(OosAodLifecycleAdapter.isPowerPolicyDenial(
                automaticLowBattery.reason));
        assertTrue(OosAodLifecycleAdapter.isPowerPolicyDenial(
                manualPowerSaver.reason));
        assertFalse(PixelFingerprintIconPolicy.shouldRefreshAfterAodPolicy(
                false, automaticLowBattery.allowsDisplay, true));
        assertFalse(PixelFingerprintIconPolicy.shouldRefreshAfterAodPolicy(
                false, manualPowerSaver.allowsDisplay, true));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.STYLE_EXISTING_NATIVE,
                PixelFingerprintIconPolicy.refreshMode(
                        false, automaticLowBattery.allowsDisplay, true, true));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.SKIP,
                PixelFingerprintIconPolicy.refreshMode(
                        false, automaticLowBattery.allowsDisplay, true, false));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.STYLE_EXISTING_NATIVE,
                PixelFingerprintIconPolicy.refreshMode(
                        false, manualPowerSaver.allowsDisplay, true, true));
    }

    @Test
    public void allowedRecentAodOverlayStillRefreshesForProximityRecovery() {
        OosAodLifecycleAdapter.PowerPolicyDecision allowed =
                OosAodLifecycleAdapter.evaluatePowerPolicy(
                        false, true, false, false, 80, 15);

        assertTrue(allowed.allowsDisplay);
        assertFalse(OosAodLifecycleAdapter.isPowerPolicyDenial(allowed.reason));
        assertTrue(PixelFingerprintIconPolicy.shouldRefreshAfterAodPolicy(
                false, allowed.allowsDisplay, false));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.REFRESH_CARRIER,
                PixelFingerprintIconPolicy.refreshMode(
                        false, allowed.allowsDisplay, false, true));
        assertFalse(PassiveFodShowGate.shouldSuppress(15_000L, 20L, -1L));
    }

    @Test
    public void nativeTimeoutHideOutranksContinuousAodCarrierRefresh() {
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.SKIP,
                PixelFingerprintIconPolicy.refreshMode(
                        false, true, false, true, true));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.SKIP,
                PixelFingerprintIconPolicy.refreshMode(
                        false, true, false, false, true));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.REFRESH_CARRIER,
                PixelFingerprintIconPolicy.refreshMode(
                        true, true, false, true, true));
        assertEquals(PixelFingerprintIconPolicy.RefreshMode.REFRESH_CARRIER,
                PixelFingerprintIconPolicy.refreshMode(
                        false, true, false, true, false));
    }

    @Test
    public void interactiveLockscreenIsNotBlockedByAodPowerPolicyGate() {
        assertTrue(PixelFingerprintIconPolicy.shouldRefreshAfterAodPolicy(
                true, false, true));
    }

    @Test
    public void deniedNonInteractivePolicyLeavesNativeHideCallbackAllowed() {
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                OosAodLifecycleAdapter.evaluatePolicy(
                        "AodRecord#onEnergySavingNotifyHide", "trace", null,
                        new OosAodLifecycleAdapter.ModulePolicy(
                                false, true, false, false, "power-save-mode",
                                "continuous", true, false),
                        false, false);

        assertFalse(decision.modulePolicyAllowsDisplay);
        assertFalse(decision.shouldKeepNativeDozeAlive);
        assertTrue(decision.shouldAllowNativeHideCallbacks);
        assertFalse(PixelFingerprintIconPolicy.shouldRefreshAfterAodPolicy(
                false, decision.modulePolicyAllowsDisplay,
                OosAodLifecycleAdapter.isPowerPolicyDenial(decision.modulePolicyReason)));
        assertFalse(OosAodLifecycleAdapter.shouldReassertAodAfterPolicy(
                false, decision.modulePolicyAllowsDisplay, decision.shouldApplyModuleAod,
                decision.shouldKeepNativeDozeAlive, decision.modulePolicyReason));
    }

    @Test
    public void allowedAodCanReassertOnlyWhileNativeDozeIsKept() {
        assertTrue(OosAodLifecycleAdapter.shouldReassertAodAfterPolicy(
                false, true, true, true, "power-policy-allowed"));
        assertFalse(OosAodLifecycleAdapter.shouldReassertAodAfterPolicy(
                false, true, true, false, "power-policy-allowed"));
        assertFalse(OosAodLifecycleAdapter.shouldReassertAodAfterPolicy(
                true, true, true, true, "power-policy-allowed"));
        assertFalse(OosAodLifecycleAdapter.shouldReassertAodAfterPolicy(
                false, false, false, false, "low-battery"));
    }
}
