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

    @Test
    public void smallGeometryUsesPrelayoutStableRowHeights() throws Exception {
        String host = source("CouiClockHostView");
        assertTrue(host.contains("stableCompactDateRowHeightPx()"));
        assertTrue(host.contains("stableCompactWeatherRowHeightPx(boolean reserveWeatherSlot)"));
        String prime = section(host,
                "private void primeContextualGeometryTarget",
                "private int stableCompactDateRowHeightPx");
        assertTrue(prime.contains("int dateHeight = stableCompactDateRowHeightPx()"));
        assertTrue(prime.contains("stableCompactWeatherRowHeightPx(reserveWeatherSlot)"));
    }

    @Test
    public void smallAodForecastAnchorReservesWeatherSlotInPrimeAndNormalTargetPaths()
            throws Exception {
        String host = source("CouiClockHostView");
        String prime = section(host,
                "private void primeContextualGeometryTarget",
                "private int stableCompactDateRowHeightPx");
        String normal = section(host,
                "private void applyInformationTargets",
                "private void updateStableLargeForecastCard");
        assertTrue(prime.contains("reserveSmallAodWeatherSlot"));
        assertTrue(prime.contains("stableCompactWeatherRowHeightPx(reserveWeatherSlot)"));
        assertTrue(normal.contains("reserveSmallAodWeatherSlot"));
        assertTrue(normal.contains("stableCompactWeatherRowHeightPx(reserveWeatherSlot)"));
    }

    @Test
    public void hiddenSmallContextualLetsMediaReclaimTheSlot() throws Exception {
        String host = source("CouiClockHostView");
        String content = section(host,
                "private void applyContentTargets",
                "private void applyContentViewTarget");
        assertTrue(content.contains("Scene.SMALL"));
        assertTrue(content.contains("compactContentTopWithoutContextual"));
    }

    @Test
    public void smallAodEntryKeepsContextualRowTransparentUntilFinalEndpoint() throws Exception {
        String host = source("CouiClockHostView");
        String refresh = section(host,
                "private void refreshContextualFromExistingAdapters",
                "private void revealDeferredSmallContextualAfterEntry");
        assertTrue(refresh.contains("deferSmallAodContextualReveal"));
        assertTrue(refresh.contains("contextualGroup.setAlpha(0f)"));
        String reveal = section(host,
                "private void revealDeferredSmallContextualAfterEntry",
                "private void onContextualDisplayedContentChanged");
        assertTrue(reveal.contains("primeContextualGeometryTarget(displayedCard)"));
        assertTrue(reveal.contains("contextualGroup.animate().alpha(1f)"));
    }
}
