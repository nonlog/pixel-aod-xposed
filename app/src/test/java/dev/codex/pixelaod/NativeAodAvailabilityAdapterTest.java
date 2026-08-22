package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NativeAodAvailabilityAdapterTest {
    @Test
    public void nativeAvailableEnabledAndVendorLifecycleAreAllRequired() {
        assertTrue(decision(1, 1, true, true, true).continuousEligible);
        assertFalse(decision(0, 1, true, true, true).continuousEligible);
        assertFalse(decision(1, 0, true, true, true).continuousEligible);
        NativeAodAvailabilityAdapter.Decision configuredButNoLifecycle =
                decision(1, 1, true, true, false);
        assertTrue(configuredButNoLifecycle.configuredEligible);
        assertFalse(configuredButNoLifecycle.continuousEligible);
    }

    @Test
    public void provisioningAndSelectedUserSetupAreRequired() {
        assertFalse(decision(1, 1, false, true, true).continuousEligible);
        assertFalse(decision(1, 1, true, false, true).continuousEligible);
    }

    @Test
    public void frameworkAmbientConfigurationIsFallbackWhenVendorSettingsAreUnknown() {
        NativeAodAvailabilityAdapter.Decision decision =
                NativeAodAvailabilityAdapter.evaluate(
                        null, null, true, true, true, true, true, 0, "test");
        assertTrue(decision.continuousEligible);
    }

    @Test
    public void validVendorLifecycleIsConservativeFallbackOnlyWhenConfigIsUnknown() {
        NativeAodAvailabilityAdapter.Decision unknownConfig =
                NativeAodAvailabilityAdapter.evaluate(
                        null, null, null, null, true, true, true, 0, "test");
        NativeAodAvailabilityAdapter.Decision explicitDisabled =
                NativeAodAvailabilityAdapter.evaluate(
                        null, 0, null, null, true, true, true, 0, "test");
        assertTrue(unknownConfig.continuousEligible);
        assertFalse(explicitDisabled.continuousEligible);
    }

    private static NativeAodAvailabilityAdapter.Decision decision(
            Integer available, Integer enabled, boolean deviceProvisioned,
            boolean userSetup, boolean lifecycle) {
        return NativeAodAvailabilityAdapter.evaluate(
                available, enabled, null, null, deviceProvisioned, userSetup,
                lifecycle, 0, "test");
    }
}
