package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OosAodHandoffProfileTest {
    @Test
    public void oos1609AndLaterLeaveBurnInToNativeClockPlugin() {
        assertTrue(OosAodHandoffProfile.usesSystemManagedBurnIn("CPH2573_16.0.9.400(EX01)"));
        assertTrue(OosAodHandoffProfile.usesSystemManagedBurnIn("CPH2573_16.0.10.100(EX01)"));
    }

    @Test
    public void olderOrUnknownBuildsKeepLegacyModuleFallback() {
        assertFalse(OosAodHandoffProfile.usesSystemManagedBurnIn("CPH2573_16.0.8.900(EX01)"));
        assertFalse(OosAodHandoffProfile.usesSystemManagedBurnIn(null));
        assertFalse(OosAodHandoffProfile.usesSystemManagedBurnIn("unknown"));
    }
}
