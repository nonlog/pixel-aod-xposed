package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PowerSavingAodPolicyTest {
    @Test
    public void chargingIsEnergySavingOnly() {
        assertTrue(PowerSavingAodPolicy.isChargingHoldRequested(
                true, "energy-saving", true, true));
        assertFalse(PowerSavingAodPolicy.isChargingHoldRequested(
                true, "all-day", true, true));
        assertFalse(PowerSavingAodPolicy.isChargingHoldRequested(
                true, "energy-saving", true, false));
    }

    @Test
    public void runtimeGates() {
        assertTrue(PowerSavingAodPolicy.shouldKeepChargingVisible(true, false, false, false, true));
        assertFalse(PowerSavingAodPolicy.shouldKeepChargingVisible(true, true, false, false, true));
        assertFalse(PowerSavingAodPolicy.shouldKeepChargingVisible(true, false, true, false, true));
        assertFalse(PowerSavingAodPolicy.shouldKeepChargingVisible(true, false, false, true, true));
        assertFalse(PowerSavingAodPolicy.shouldKeepChargingVisible(true, false, false, false, false));
    }

    @Test
    public void notificationNeedsNativePeek() {
        assertTrue(PowerSavingAodPolicy.isNotificationEnhancementConfigured(
                true, "energy-saving", true, true));
        assertFalse(PowerSavingAodPolicy.isNotificationEnhancementConfigured(
                true, "energy-saving", true, false));
        assertFalse(PowerSavingAodPolicy.isNotificationEnhancementConfigured(
                true, "scheduled", true, true));
    }

    @Test
    public void nativeNotificationWindowCanLeadDisplayState() {
        assertTrue(PowerSavingAodPolicy.hasVendorTransientPresentationWindow(true, false));
        assertTrue(PowerSavingAodPolicy.hasVendorTransientPresentationWindow(false, true));
        assertFalse(PowerSavingAodPolicy.hasVendorTransientPresentationWindow(false, false));
    }

    @Test
    public void updateBudgetExtensionIsLimitedToExplicitWindows() {
        assertTrue(PowerSavingAodPolicy.shouldExtendNativeEnergySavingUpdateBudget(
                true, false, false));
        assertTrue(PowerSavingAodPolicy.shouldExtendNativeEnergySavingUpdateBudget(
                false, true, true));
        assertFalse(PowerSavingAodPolicy.shouldExtendNativeEnergySavingUpdateBudget(
                false, true, false));
        assertFalse(PowerSavingAodPolicy.shouldExtendNativeEnergySavingUpdateBudget(
                false, false, true));
    }

    @Test
    public void restoreNativeHideOnlyInEnergySaving() {
        assertTrue(PowerSavingAodPolicy.shouldReapplyNativeHide(
                true, "energy-saving", true, false));
        assertFalse(PowerSavingAodPolicy.shouldReapplyNativeHide(
                false, "energy-saving", true, false));
        assertFalse(PowerSavingAodPolicy.shouldReapplyNativeHide(
                true, "all-day", true, false));
        assertFalse(PowerSavingAodPolicy.shouldReapplyNativeHide(
                true, "energy-saving", true, true));
    }
}
