package dev.codex.pixelaod;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

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

    @Test public void chargingUsesVendorLifecycle() throws Exception {
        String text = source("PowerSavingAodController");
        assertTrue(text.contains("callMethod(o,\"showClock\",0)"));
        assertTrue(text.contains("callMethod(o,\"setHideAlarm\")"));
        assertFalse(text.contains("PowerManager.wakeUp"));
        assertFalse(text.contains("AlarmManager"));
        assertFalse(text.contains("postDelayed"));
    }

    @Test public void notificationUsesNativePeekLifetime() throws Exception {
        String text = source("PixelPeekNotificationController");
        assertTrue(text.contains("startPowerSavingNotificationAod"));
        assertTrue(text.contains("endPowerSavingNotificationAod"));
        assertTrue(text.contains("onDetachedFromWindow"));
    }

    @Test public void nativeModeIsReadNotRewritten() throws Exception {
        String text = source("PowerSavingAodController");
        assertFalse(text.contains("Settings.Secure.put"));
        assertTrue(source("PowerSavingAodPolicy").contains("energy-saving"));
    }
}
