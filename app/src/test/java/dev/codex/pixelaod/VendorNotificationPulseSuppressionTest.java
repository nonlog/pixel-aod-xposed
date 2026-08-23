package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VendorNotificationPulseSuppressionTest {
    private static OosAodLifecycleAdapter.ModulePolicy allowedModulePolicy() {
        return new OosAodLifecycleAdapter.ModulePolicy(
                true, true, true, false,
                "continuous-native-aod", "continuous", true, false);
    }

    @Test
    public void nativeAodPowerSaveCapabilityBlocksPostedPulseCandidate() {
        OosAodLifecycleAdapter.NotificationPulseObservation observation =
                OosAodLifecycleAdapter.evaluateNotificationPulseObservation(
                        "SystemUI NotificationListener#onNotificationPosted",
                        1, 1, -1, allowedModulePolicy(), false, true);

        assertTrue(observation.policyBlocked);
        assertFalse(observation.policyNativePulseCompatible);
        assertEquals("vendor-suppression-blocked", observation.policyLabel);
        assertEquals("vendor-aod-power-save", observation.policyReason);
        assertEquals("block-native-pulse", observation.policyAction);
    }

    @Test
    public void unknownOrClearTypedCapabilityLeavesExistingPulsePolicyUntouched() {
        OosAodLifecycleAdapter.NotificationPulseObservation observation =
                OosAodLifecycleAdapter.evaluateNotificationPulseObservation(
                        "SystemUI NotificationListener#onNotificationPosted",
                        1, 1, -1, allowedModulePolicy(), false, false);

        assertFalse(observation.policyBlocked);
        assertTrue(observation.policyNativePulseCompatible);
        assertEquals("native-pulse-compatible", observation.policyLabel);
    }

    @Test
    public void vendorPulseSuppressionDoesNotPromoteSnapshotIntoExplicitPulseDecision() {
        OosAodLifecycleAdapter.NotificationPulseObservation observation =
                OosAodLifecycleAdapter.evaluateNotificationPulseObservation(
                        "SystemUI NotificationListener#onListenerConnected",
                        1, 1, -1, allowedModulePolicy(), false, true);

        assertFalse(observation.policyBlocked);
        assertFalse(observation.policyNativePulseCompatible);
        assertEquals("observe-only", observation.policyLabel);
        assertEquals("snapshot-not-explicit-post", observation.policyReason);
    }
}
