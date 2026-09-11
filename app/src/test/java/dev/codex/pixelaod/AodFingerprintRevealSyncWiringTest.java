package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AodFingerprintRevealSyncWiringTest {
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

    @Test
    public void couiStartupInstallsSystemGlyphRevealSync() throws Exception {
        String hook = source("PixelAodHook");
        assertTrue(hook.contains("AodFingerprintRevealSyncController.install(classLoader)"));
    }

    @Test
    public void synchronizationObservesNativeRevealAndOnlyMutatesVisualAlpha() throws Exception {
        String controller = source("AodFingerprintRevealSyncController");
        assertTrue(controller.contains("AodClockLayout"));
        assertTrue(controller.contains("setVisibleWithSetupAnimate"));
        assertTrue(controller.contains("workshop_aod_anim_mock"));
        assertTrue(controller.contains("OnScreenFingerprintIcon"));
        assertTrue(controller.contains("setBrightnessAlpha"));
        assertTrue(controller.contains("updateFpIconAlpha"));
        assertTrue(controller.contains("onFpTouch"));
        assertTrue(controller.contains("icon.setAlpha(target)"));
        assertTrue(controller.contains("postOnAnimation"));

        assertFalse(controller.contains("PowerManager"));
        assertFalse(controller.contains("requestScreenState"));
        assertFalse(controller.contains("setVisibilityInAOD"));
        assertFalse(controller.contains("setVisibility(View.VISIBLE"));
        assertFalse(controller.contains("postDelayed"));
        assertFalse(controller.contains("AlarmManager"));
    }
}
