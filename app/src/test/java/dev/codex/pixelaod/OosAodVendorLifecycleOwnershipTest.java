package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OosAodVendorLifecycleOwnershipTest {
    @Test
    public void allowedContinuousPixelPresentationNeverKeepsVendorDozeAlive() {
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                OosAodLifecycleAdapter.evaluatePolicy(
                        "test#continuous-aod", "trace", activeAodState(),
                        new OosAodLifecycleAdapter.ModulePolicy(
                                true, true, true, false,
                                "all-checks-passed", "continuous", true, false),
                        false, false);

        assertTrue(decision.modulePolicyAllowsDisplay);
        assertTrue(decision.shouldApplyModuleAod);
        assertTrue(decision.shouldDrawPixelOverlay);
        assertFalse(decision.shouldKeepNativeDozeAlive);
        assertTrue(decision.shouldAllowNativeHideCallbacks);
    }

    @Test
    public void triggerOnlyPresentationLivesOnlyInsideArmedVendorAodScene() {
        assertTrue(OosAodLifecycleAdapter.shouldPresentVendorTransientScene(
                true, false, true));
        assertFalse(OosAodLifecycleAdapter.shouldPresentVendorTransientScene(
                false, false, true));
        assertFalse(OosAodLifecycleAdapter.shouldPresentVendorTransientScene(
                true, true, true));
        assertFalse(OosAodLifecycleAdapter.shouldPresentVendorTransientScene(
                true, false, false));
    }

    private static PixelAodClockView.AodLifecycleState activeAodState() {
        return new PixelAodClockView.AodLifecycleState(
                1_000L,
                true,
                900L,
                950L,
                950L,
                0L,
                false,
                "trace",
                "test",
                950L,
                3,
                1,
                950L,
                true,
                false,
                true,
                false,
                false,
                true,
                true,
                "none",
                "none",
                "",
                0L,
                false,
                "none",
                "none",
                "",
                0L,
                "none",
                "none",
                "none",
                "trace",
                "",
                "none",
                "none",
                "none",
                false,
                false,
                0,
                0,
                0,
                0L);
    }
}
