package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PowerSavingAodWiringTest {
    private static String source(String name) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        return new String(Files.readAllBytes(root.resolve(
                "app/src/main/java/dev/codex/pixelaod/" + name + ".java")),
                StandardCharsets.UTF_8);
    }

    private static String section(String text, String from, String to) {
        int start = text.indexOf(from);
        assertTrue("missing start: " + from, start >= 0);
        int end = text.indexOf(to, start + from.length());
        assertTrue("missing end: " + to, end > start);
        return text.substring(start, end);
    }

    @Test
    public void chargingPreservesVendorTimerAndInterceptsActualControllerTimeout() throws Exception {
        String controller = source("PowerSavingAodController");
        String alarmHook = section(controller, "private static void hookUpdateManager",
                "private static void registerBatteryReceiver");
        assertTrue(alarmHook.contains("hookAfter"));
        assertFalse(alarmHook.contains("setResult"));
        assertFalse(alarmHook.contains("shouldKeepPowerSavingAodVisibleForCharging"));

        String hook = source("PixelAodHook");
        assertTrue(hook.contains("BaseAodClockLayoutController"));
        assertTrue(hook.contains("PanoramicAodController"));
        assertTrue(hook.contains("onEnergySavingNotifyHide"));
        assertFalse(hook.contains("notifyHideAodFromEnergySavingDirectly"));
    }

    @Test
    public void chargingTimeoutHidesOnlyFingerprintWithoutPanelOffRequest() throws Exception {
        String hook = source("PixelAodHook");
        String method = section(hook,
                "static boolean hideFingerprintOnlyForPowerSavingChargingTimeout",
                "static boolean isFodNativeTimeoutHideLatched");
        assertTrue(method.contains("FOD_NATIVE_TIMEOUT_HIDE_GATE.markHidden"));
        assertTrue(method.contains("callMethod(uiMech, \"setVisibilityInAOD\", 1)"));
        assertFalse(method.contains("notifyHideAodIcon"));
        assertFalse(method.contains("requestScreenState"));
    }

    @Test
    public void notificationUsesAttachedNativePeekAsTransientAuthority() throws Exception {
        String peek = source("PixelPeekNotificationController");
        assertTrue(peek.contains("hasActiveNativeNotificationWindow"));
        assertTrue(peek.contains("state.nativeAttached = true"));
        assertTrue(peek.contains("startPowerSavingNotificationAod"));
        assertTrue(peek.contains("endPowerSavingNotificationAod"));

        String clock = source("PixelAodClockView");
        String start = section(clock, "private static boolean startVendorTransientAodPresentation",
                "private static boolean isGenericOplusWakeCallback");
        assertTrue(start.contains("nativeNotificationWindow"));
        assertTrue(start.contains("!ambientSessionActive && !nativeNotificationWindow"));
        assertTrue(start.contains("!nativeNotificationWindow && vendorSuppression.notificationPulseDenied()"));
        String lifecycle = section(clock, "private static AodLifecycleState currentAodLifecycleState",
                "static final class AodLifecycleState");
        assertTrue(lifecycle.contains("PixelPeekNotificationController.hasActiveNativeNotificationWindow()"));
        assertTrue(lifecycle.contains("vendorTransientSurfaceAvailable"));
    }

    @Test
    public void extensionDoesNotCreateItsOwnPowerOrTimeoutOwner() throws Exception {
        String controller = source("PowerSavingAodController");
        assertTrue(controller.contains("callMethod(clockLayout, \"showClock\", 0)"));
        assertTrue(controller.contains("callMethod(updateManager, \"setHideAlarm\")"));
        assertFalse(controller.contains("PowerManager.wakeUp"));
        assertFalse(controller.contains("AlarmManager"));
        assertFalse(controller.contains("postDelayed"));
        assertFalse(controller.contains("Settings.Secure.put"));
    }
}
