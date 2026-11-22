package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Guards the bounded ROM TextAnimator cache policy used by the persistent COUI host. */
public final class CouiClockMorphPerformanceWiringTest {
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
    public void romTypefaceCacheIsBoundedToOneHighRefreshTransition() throws Exception {
        String runtime = source("CouiClockRomTextAnimatorRuntime");
        assertTrue(runtime.contains("FONT_CACHE_MAX_ENTRIES = 96"));
        assertFalse(runtime.contains("FONT_CACHE_MAX_ENTRIES = 384"));
    }

    @Test
    public void persistentHostDoesNotRunOffscreenMorphPrewarm() throws Exception {
        String host = source("CouiClockHostView");
        assertFalse(host.contains("morphRuntime.prewarmAsync(motionInterpolator)"));
    }
}
