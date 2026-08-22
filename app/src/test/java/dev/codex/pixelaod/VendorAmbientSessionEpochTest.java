package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VendorAmbientSessionEpochTest {
    @Test
    public void invalidateMakesPriorAmbientWorkStale() {
        VendorAmbientSessionEpoch epoch = new VendorAmbientSessionEpoch();
        long first = epoch.begin("first").epoch;
        assertTrue(epoch.isCurrent(first));

        long invalidated = epoch.invalidate("terminal").epoch;
        assertTrue(invalidated > first);
        assertFalse(epoch.isCurrent(first));
        assertFalse(epoch.isCurrent(invalidated));
    }

    @Test
    public void newVendorSessionOwnsANewEpoch() {
        VendorAmbientSessionEpoch epoch = new VendorAmbientSessionEpoch();
        long first = epoch.begin("first").epoch;
        epoch.invalidate("terminal");
        long second = epoch.begin("second").epoch;

        assertTrue(second > first);
        assertFalse(epoch.isCurrent(first));
        assertTrue(epoch.isCurrent(second));
    }
}
