package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
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

    @Test
    public void oplusAllDayModeAllowsScreenOffPrearm() {
        NativeAodAvailabilityAdapter.Decision decision = modeDecision(
                1, 0, 0, 7, 0, 23, 30, 9 * 60, false);
        assertEquals("all-day", decision.displayMode);
        assertTrue(decision.prearmEligible);
        assertTrue(decision.scheduleWindowEligible);
        assertFalse(decision.continuousEligible);
    }

    @Test
    public void oplusEnergySavingModeWaitsForRealVendorAmbientLifecycle() {
        NativeAodAvailabilityAdapter.Decision idle = modeDecision(
                0, 0, 1, 7, 0, 23, 30, 9 * 60, false);
        NativeAodAvailabilityAdapter.Decision active = modeDecision(
                0, 0, 1, 7, 0, 23, 30, 9 * 60, true);
        assertEquals("energy-saving", idle.displayMode);
        assertFalse(idle.prearmEligible);
        assertFalse(idle.continuousEligible);
        assertTrue(active.continuousEligible);
    }

    @Test
    public void oplusScheduledModePrearmsOnlyInsideNativeSchedule() {
        NativeAodAvailabilityAdapter.Decision inside = modeDecision(
                0, 1, 0, 22, 0, 7, 0, 23 * 60, false);
        NativeAodAvailabilityAdapter.Decision outside = modeDecision(
                0, 1, 0, 22, 0, 7, 0, 12 * 60, false);
        assertEquals("scheduled", inside.displayMode);
        assertTrue(inside.scheduleWindowEligible);
        assertTrue(inside.prearmEligible);
        assertFalse(outside.scheduleWindowEligible);
        assertFalse(outside.prearmEligible);
        assertEquals("native-aod-schedule-inactive", outside.reason);
    }

    @Test
    public void scheduleWindowSupportsCrossMidnightAndSameTimeAllDay() {
        assertTrue(NativeAodAvailabilityAdapter.isWithinSchedule(22, 0, 7, 0, 23 * 60));
        assertTrue(NativeAodAvailabilityAdapter.isWithinSchedule(22, 0, 7, 0, 6 * 60 + 59));
        assertFalse(NativeAodAvailabilityAdapter.isWithinSchedule(22, 0, 7, 0, 12 * 60));
        assertTrue(NativeAodAvailabilityAdapter.isWithinSchedule(7, 0, 7, 0, 12 * 60));
    }

    private static NativeAodAvailabilityAdapter.Decision decision(
            Integer available, Integer enabled, boolean deviceProvisioned,
            boolean userSetup, boolean lifecycle) {
        return NativeAodAvailabilityAdapter.evaluate(
                available, enabled, null, null, deviceProvisioned, userSetup,
                lifecycle, 0, "test");
    }

    private static NativeAodAvailabilityAdapter.Decision modeDecision(
            int alwaysDisplay, int timingSet, int energySavingSet,
            int startHour, int startMinute, int endHour, int endMinute,
            int minuteOfDay, boolean lifecycle) {
        return NativeAodAvailabilityAdapter.evaluate(
                1, 1, null, null, true, true, lifecycle, 0,
                alwaysDisplay, timingSet, energySavingSet,
                startHour, startMinute, endHour, endMinute, minuteOfDay, "test");
    }
}
