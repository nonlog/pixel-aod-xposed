package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Runtime wiring for alarm/call/bouncer -> lockscreen first-frame weight normalization. */
public final class CouiClockTransientReturnWiringTest {
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
    public void nativeTransitionPreloadsBeforeFinishedEligibilityResync() throws Exception {
        String hook = section(source("PixelAodHook"),
                "private static NativeKeyguardSceneEligibility.Snapshot observeNativeKeyguardTransitionStep",
                "static void hookKeyguardGoingAway");
        int preload = hook.indexOf("shouldPreloadLockscreenReturn(after)");
        int eligible = hook.indexOf("NativeKeyguardSceneEligibility.becameEligible(");
        assertTrue(preload >= 0);
        assertTrue(preload < eligible);
        assertTrue(hook.contains("ActiveClockRendererController.prepareNativeLockscreenReturn"));
    }

    @Test
    public void controllerUsesForcedLockscreenMappingInsteadOfTransientUiState() throws Exception {
        String method = section(source("CouiClockPluginHostController"),
                "static void prepareNativeLockscreenReturn", "static void suppressForDirectGone");
        assertTrue(method.contains("readRenderState(plugin, false)"));
        assertTrue(method.contains("forcedLockscreenEntry"));
        assertTrue(method.contains("record.host.preloadLockscreenReturn"));
    }

    @Test
    public void hiddenHostIsRestyledSynchronouslyWithoutOwningNativeVisibility() throws Exception {
        String method = section(source("CouiClockHostView"),
                "void preloadLockscreenReturn", "/** Begins an AOD entry");
        assertTrue(method.contains("cancelScheduledTargetApply()"));
        assertTrue(method.contains("cancelRunningPropertyAnimations()"));
        assertTrue(method.contains("applyTargets(false, 0L)"));
        assertFalse(method.contains("setVisibility("));
    }
}
