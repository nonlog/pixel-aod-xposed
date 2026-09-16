package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class CouiUdfpsHdrLifecycleWiringTest {
    private static String source(String name) throws Exception {
        Path directory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null && !Files.exists(directory.resolve("settings.gradle"))) {
            directory = directory.getParent();
        }
        assertNotNull("repository root", directory);
        return new String(Files.readAllBytes(directory.resolve(
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
    public void hdrSurfaceTransactionsUseStateAndSurfaceDedupe() throws Exception {
        String method = section(source("CouiUdfpsController"),
                "private static void updatePressedHdr(ImageView pressedIcon, boolean pressed, boolean force)",
                "private static void setPressedIlluminationAlpha");
        assertTrue(method.contains("CouiUdfpsHdrUpdatePolicy.shouldApply"));
        assertTrue(method.contains("previous.surface.get() == surface"));
        assertTrue(method.contains("LAST_HDR_SURFACE_STATES.put"));
    }

    @Test
    public void hdrLayoutRefreshForcesNextFrameSurfaceReapply() throws Exception {
        String method = section(source("CouiUdfpsController"),
                "private static void configureHdrLayout", "private static void updatePressedHdr(ImageView pressedIcon, boolean pressed)");
        int layoutUpdate = method.indexOf("manager.updateViewLayout(pressedIcon, params)");
        int frameReapply = method.indexOf("pressedIcon.postOnAnimation(() -> updatePressedHdr(");
        assertTrue(layoutUpdate >= 0);
        assertTrue(frameReapply > layoutUpdate);
        assertTrue(method.contains("pressedIcon, isPressedTouchActive(pressedIcon), true"));
    }

    @Test
    public void hdrDisabledDropsModuleDedupeWithoutTouchingNativeBrightness() throws Exception {
        String method = section(source("CouiUdfpsController"),
                "private static void updatePressedHdr(ImageView pressedIcon, boolean pressed, boolean force)",
                "private static void setPressedIlluminationAlpha");
        assertTrue(method.contains("LAST_HDR_SURFACE_STATES.remove(pressedIcon)"));
        int disabled = method.indexOf("if (!enabled)");
        int transaction = method.indexOf("new SurfaceControl.Transaction()", disabled);
        assertTrue(disabled >= 0 && transaction > disabled);
        assertTrue(method.substring(disabled, transaction).contains("return;"));
    }

    @Test
    public void couiRendererDoesNotTakeOverAodScreenStateRequests() throws Exception {
        String controller = source("CouiUdfpsController");
        assertFalse(controller.contains("AODDisplayUtil"));
        assertFalse(controller.contains("requestScreenState"));
    }
}
