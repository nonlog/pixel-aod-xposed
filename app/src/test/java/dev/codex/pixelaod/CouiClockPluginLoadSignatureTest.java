package dev.codex.pixelaod;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockPluginLoadSignatureTest {
    @SuppressWarnings("unused")
    private static final class FakeClockPlugin {
        boolean loadPluginReal(String logicalPackage) {
            return true;
        }

        boolean loadPluginReal(String logicalPackage, int displayId) {
            return true;
        }

        boolean loadPluginReal(String logicalPackage, boolean force, Object extras) {
            return true;
        }

        boolean loadPluginReal(int displayId) {
            return false;
        }

        boolean loadPluginReal() {
            return false;
        }

        boolean otherMethod(String logicalPackage) {
            return false;
        }
    }

    @Test
    public void matchesSingleAndMultiArgLeadingStringLoadPluginReal() throws Exception {
        Method oneArg = FakeClockPlugin.class.getDeclaredMethod("loadPluginReal", String.class);
        Method twoArg = FakeClockPlugin.class.getDeclaredMethod(
                "loadPluginReal", String.class, int.class);
        Method threeArg = FakeClockPlugin.class.getDeclaredMethod(
                "loadPluginReal", String.class, boolean.class, Object.class);
        Method intOnly = FakeClockPlugin.class.getDeclaredMethod("loadPluginReal", int.class);
        Method zeroArg = FakeClockPlugin.class.getDeclaredMethod("loadPluginReal");
        Method unrelated = FakeClockPlugin.class.getDeclaredMethod("otherMethod", String.class);

        assertTrue(CouiClockPluginHostController.matchesLoadPluginRealSignature(oneArg));
        assertTrue(CouiClockPluginHostController.matchesLoadPluginRealSignature(twoArg));
        assertTrue(CouiClockPluginHostController.matchesLoadPluginRealSignature(threeArg));
        assertFalse(CouiClockPluginHostController.matchesLoadPluginRealSignature(intOnly));
        assertFalse(CouiClockPluginHostController.matchesLoadPluginRealSignature(zeroArg));
        assertFalse(CouiClockPluginHostController.matchesLoadPluginRealSignature(unrelated));
        assertFalse(CouiClockPluginHostController.matchesLoadPluginRealSignature(null));
    }

    @Test
    public void recognizesBigClockLogicalPackageAcrossArities() {
        assertTrue(CouiClockPluginHostController.isBigClockLoad(
                new Object[]{"com.oplus.keyguard.clock.big"}));
        assertTrue(CouiClockPluginHostController.isBigClockLoad(
                new Object[]{"com.oplus.keyguard.clock.big", 0}));
        assertTrue(CouiClockPluginHostController.isBigClockLoad(
                new Object[]{"com.oplus.keyguard.clock.big", true, "extra"}));
        assertFalse(CouiClockPluginHostController.isBigClockLoad(
                new Object[]{"com.oplus.keyguard.clock.other"}));
        assertFalse(CouiClockPluginHostController.isBigClockLoad(new Object[0]));
        assertFalse(CouiClockPluginHostController.isBigClockLoad(null));
    }
}
