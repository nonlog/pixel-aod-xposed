package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Guards the first visible frame of the contextual weather row during Small AOD entry. */
public final class CouiClockContextualGeometryWiringTest {
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
    public void displayedContentCallbackPrimesGeometryBeforeRequestingAnotherLayout() throws Exception {
        String method = section(source("CouiClockHostView"),
                "private void onContextualDisplayedContentChanged()",
                "private void primeContextualGeometryTarget");
        int prime = method.indexOf("primeContextualGeometryTarget(displayedCard)");
        int requestLayout = method.indexOf("contextualGroup.requestLayout()");
        int postedTargets = method.indexOf("scheduleApplyTargets(false)");
        assertTrue(prime >= 0);
        assertTrue(prime < requestLayout);
        assertTrue(prime < postedTargets);
    }

    @Test
    public void smallPrimeCommitsBothAodLeadingEdgeAndStackY() throws Exception {
        String method = section(source("CouiClockHostView"),
                "private void primeContextualGeometryTarget",
                "private void applyContextualIconGeometry");
        assertTrue(method.contains("CouiClockPresentationModel.Scene.SMALL"));
        assertTrue(method.contains("CouiCompactLayout.couiHostContentLeft"));
        assertTrue(method.contains("CouiClockContextualLayoutPolicy.contextualTop"));
        assertTrue(method.contains("compactAodVerticalOffsetPx()"));
        assertTrue(method.contains("contextualGroup.setTranslationX"));
        assertTrue(method.contains("contextualGroup.setTranslationY"));
    }
}
