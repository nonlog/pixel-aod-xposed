package dev.codex.pixelaod;

import android.os.Bundle;

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

    @SuppressWarnings("unused")
    private static final class FakeKeyguardPlugin {
        void setUp(int userId, int scope) {
        }

        void setUp(int scope) {
        }

        Bundle sendCommand(String command, Bundle bundle) {
            return null;
        }

        Bundle sendCommand(String command) {
            return null;
        }

        static void reload$default(FakeKeyguardPlugin plugin, int userId) {
        }

        void reload$default(int userId) {
        }
    }

    @Test
    public void matchesColorOs17KeyguardPluginSignatures() throws Exception {
        Method setUpTwoArg = FakeKeyguardPlugin.class.getDeclaredMethod("setUp", int.class, int.class);
        Method setUpOneArg = FakeKeyguardPlugin.class.getDeclaredMethod("setUp", int.class);
        Method sendCmdTwoArg = FakeKeyguardPlugin.class.getDeclaredMethod(
                "sendCommand", String.class, Bundle.class);
        Method sendCmdOneArg = FakeKeyguardPlugin.class.getDeclaredMethod("sendCommand", String.class);
        Method reloadStatic = FakeKeyguardPlugin.class.getDeclaredMethod(
                "reload$default", FakeKeyguardPlugin.class, int.class);
        Method reloadInstance = FakeKeyguardPlugin.class.getDeclaredMethod("reload$default", int.class);

        assertTrue(CouiClockPluginHostController.matchesKeyguardPluginSetUpSignature(setUpTwoArg));
        assertFalse(CouiClockPluginHostController.matchesKeyguardPluginSetUpSignature(setUpOneArg));
        assertTrue(CouiClockPluginHostController.matchesKeyguardPluginSendCommandSignature(sendCmdTwoArg));
        assertFalse(CouiClockPluginHostController.matchesKeyguardPluginSendCommandSignature(sendCmdOneArg));
        assertTrue(CouiClockPluginHostController.matchesKeyguardPluginReloadSignature(
                reloadStatic, FakeKeyguardPlugin.class));
        assertFalse(CouiClockPluginHostController.matchesKeyguardPluginReloadSignature(
                reloadInstance, FakeKeyguardPlugin.class));
    }
}
