package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Guards the native-FINISHED safety net for notification-peek AOD wake races. */
public final class CouiClockAodExitFallbackWiringTest {
    private static String source(String name) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("settings.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("repository root", root);
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
    public void nativeFinishedExitHasDedicatedRepairAfterStartedPreload() throws Exception {
        String hook = section(source("PixelAodHook"),
                "private static NativeKeyguardSceneEligibility.Snapshot observeNativeKeyguardTransitionStep",
                "static void hookKeyguardGoingAway");
        int preload = hook.indexOf("shouldPreloadLockscreenReturn(after)");
        int repair = hook.indexOf("shouldRepairFinishedNativeExit(after)");
        assertTrue(preload >= 0);
        assertTrue(repair > preload);
        assertTrue(hook.contains(
                "ActiveClockRendererController.repairFinishedAodToLockscreen"));
    }

    @Test
    public void repairOnlyTouchesStillDozingHostAndPrefersRememberedLockscreenScene()
            throws Exception {
        String method = section(source("CouiClockPluginHostController"),
                "static void repairFinishedAodToLockscreen",
                "static void prepareNonLockscreenAodEntry");
        assertTrue(method.contains("boolean hostDozing = record.host.presentation().dozing()"));
        assertTrue(method.contains("hasPendingAodToLockscreenAnimation"));
        assertTrue(method.contains("shouldRepairStaleHost"));
        int remembered = method.indexOf(
                "CouiClockPresentationModel.Scene scene = record.lastLockscreenScene");
        int tracker = method.indexOf("readRenderState(plugin, false)");
        assertTrue(remembered >= 0);
        assertTrue(tracker > remembered);
        assertTrue(method.contains("DEFAULT_AOD_CONTENT"));
        assertTrue(method.contains("record.host.preloadLockscreenReturn"));
    }
}
