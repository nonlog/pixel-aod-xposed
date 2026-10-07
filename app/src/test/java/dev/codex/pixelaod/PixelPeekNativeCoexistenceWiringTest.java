package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class PixelPeekNativeCoexistenceWiringTest {
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

    @Test
    public void nativePeekIsExcludedFromGenericStockAodSuppression() throws Exception {
        String hook = source("PixelAodHook");
        assertTrue(hook.contains("looksLikeNativeOplusPeekView"));
        assertTrue(hook.contains("containsNativeOplusPeekView"));
        assertTrue(hook.contains("preserved stock AOD container with native Peek subtree"));
        assertTrue(hook.contains("looksLikeNativeOplusPeekView(marker)"));
    }

    @Test
    public void customPeekFailsOpenToNativeDrawing() throws Exception {
        String peek = source("PixelPeekNotificationController");
        assertTrue(peek.contains("KEY_PIXEL_NOTIFICATION_PEEK"));
        assertTrue(peek.contains("Pixel peek preserving native presentation"));
        assertTrue(peek.contains("PixelPeekPresentationPolicy.shouldSuppressNativeDraw"));
        assertTrue(peek.contains("state.nativeAttached"));
    }
}
