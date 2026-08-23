package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VendorAmbientSuppressionCapabilitiesTest {
    @Test
    public void unknownSignalsStayTypedUnknownAndFailOpenAtConsumerBoundary() {
        VendorAmbientSuppressionCapabilities adapter =
                new VendorAmbientSuppressionCapabilities();

        VendorAmbientSuppressionCapabilities.Snapshot snapshot = adapter.snapshot();

        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN, snapshot.baseAod);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.notificationPulse);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.contextualPresentation);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.wakeGestures);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.authenticationPulse);
        assertFalse(snapshot.baseAodDenied());
        assertFalse(snapshot.notificationPulseDenied());
    }

    @Test
    public void ambientDisplaySuppressionDeniesOnlyProvenBaseAodCapability() {
        VendorAmbientSuppressionCapabilities adapter =
                new VendorAmbientSuppressionCapabilities();
        adapter.observeAodPowerSave(false, "battery-seed");

        VendorAmbientSuppressionCapabilities.Snapshot snapshot =
                adapter.observeAlwaysOnSuppressed(true, "DozeServiceHost#setAlwaysOnSuppressed");

        assertEquals(VendorAmbientSuppressionCapabilities.Decision.DENY, snapshot.baseAod);
        assertEquals(VendorAmbientSuppressionCapabilities.Reason.AMBIENT_DISPLAY_SUPPRESSED,
                snapshot.baseAodReason);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.notificationPulse);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.contextualPresentation);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.wakeGestures);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.authenticationPulse);
    }

    @Test
    public void aodPowerSaveDeniesBaseAodAndNativeNotificationPulse() {
        VendorAmbientSuppressionCapabilities adapter =
                new VendorAmbientSuppressionCapabilities();
        adapter.observeAlwaysOnSuppressed(false, "host-seed");

        VendorAmbientSuppressionCapabilities.Snapshot snapshot =
                adapter.observeAodPowerSave(true, "BatteryControllerImpl#setPowerSave");

        assertTrue(snapshot.baseAodDenied());
        assertTrue(snapshot.notificationPulseDenied());
        assertEquals(VendorAmbientSuppressionCapabilities.Reason.AOD_POWER_SAVE,
                snapshot.baseAodReason);
        assertEquals(VendorAmbientSuppressionCapabilities.Reason.AOD_POWER_SAVE,
                snapshot.notificationPulseReason);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.wakeGestures);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.authenticationPulse);
    }

    @Test
    public void suppressionClearedDoesNotInventLifecycleItOnlyMarksKnownCapabilitiesUnsuppressed() {
        VendorAmbientSuppressionCapabilities adapter =
                new VendorAmbientSuppressionCapabilities();
        adapter.observeAlwaysOnSuppressed(true, "suppressed");
        adapter.observeAodPowerSave(true, "power-save");

        adapter.observeAlwaysOnSuppressed(false, "ambient-clear");
        VendorAmbientSuppressionCapabilities.Snapshot snapshot =
                adapter.observeAodPowerSave(false, "power-clear");

        assertEquals(VendorAmbientSuppressionCapabilities.Decision.ALLOW, snapshot.baseAod);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.notificationPulse);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.contextualPresentation);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.wakeGestures);
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN,
                snapshot.authenticationPulse);
    }

    @Test
    public void partialSeedCannotClaimBaseAodAllowed() {
        VendorAmbientSuppressionCapabilities adapter =
                new VendorAmbientSuppressionCapabilities();

        VendorAmbientSuppressionCapabilities.Snapshot hostOnly =
                adapter.observeAlwaysOnSuppressed(false, "host-seed");
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.UNKNOWN, hostOnly.baseAod);

        VendorAmbientSuppressionCapabilities.Snapshot complete =
                adapter.observeAodPowerSave(false, "battery-seed");
        assertEquals(VendorAmbientSuppressionCapabilities.Decision.ALLOW, complete.baseAod);
    }
}
