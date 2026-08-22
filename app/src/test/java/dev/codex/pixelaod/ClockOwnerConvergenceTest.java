package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class ClockOwnerConvergenceTest {
    @Test
    public void releaseRuntimeAlwaysBlocksLegacyPrimaryClockOwner() {
        assertTrue(ActiveClockRendererController.blocksLegacyPrimaryOwner());
    }

    @Test
    public void legacyClockRendererKeyIsNoLongerPartOfRuntimeSchema() {
        assertNull(PixelAodSettingsSchema.spec("clock_renderer"));
    }
    @Test
    public void legacyClockOwnerClassesAreRemoved() {
        assertClassMissing("dev.codex.pixelaod.ClockPluginHostController");
        assertClassMissing("dev.codex.pixelaod.PixelClockPluginHostView");
    }

    private static void assertClassMissing(String className) {
        try {
            Class.forName(className);
            fail("legacy clock owner class is still packaged: " + className);
        } catch (ClassNotFoundException expected) {
            // Expected: M8-S2 physically removes the legacy owner implementation.
        }
    }

}