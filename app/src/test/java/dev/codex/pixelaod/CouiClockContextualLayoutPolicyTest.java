package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockContextualLayoutPolicyTest {
    @Test
    public void contextualSurfaceIsAodOnly() {
        assertTrue(CouiClockContextualLayoutPolicy.contextualSurfaceEnabled(true));
        assertFalse(CouiClockContextualLayoutPolicy.contextualSurfaceEnabled(false));
    }

    @Test
    public void largeContextualRowCentersOnDisplay() {
        assertEquals(410f, CouiClockContextualLayoutPolicy.largeContextualStart(
                1000f, 180f), 0.001f);
        assertEquals(0f, CouiClockContextualLayoutPolicy.largeContextualStart(
                120f, 180f), 0.001f);
    }

    @Test
    public void largeContextualRowCentersOnlyDisplayedForecast() {
        assertEquals(410f, CouiClockContextualLayoutPolicy.largeContextualStartForDisplayedCard(
                1000f, 180f, 24f, true), 0.001f);
        assertEquals(24f, CouiClockContextualLayoutPolicy.largeContextualStartForDisplayedCard(
                1000f, 180f, 24f, false), 0.001f);
    }

    @Test
    public void largeContextualRowUsesVisibleContentInsteadOfExpandedRowWidth() {
        assertEquals(180f, CouiClockContextualLayoutPolicy.visibleContentWidth(
                140f, true, 40f, 840f), 0.001f);
        assertEquals(140f, CouiClockContextualLayoutPolicy.visibleContentWidth(
                140f, false, 40f, 840f), 0.001f);
        assertEquals(840f, CouiClockContextualLayoutPolicy.visibleContentWidth(
                900f, true, 40f, 840f), 0.001f);
    }

    @Test
    public void contextualRowStartsBelowVisibleWeather() {
        assertEquals(176f, CouiClockContextualLayoutPolicy.contextualTop(
                100f, 32, true, 138f, 32, 6f), 0.001f);
    }

    @Test
    public void contextualRowStartsBelowDateWhenWeatherHidden() {
        assertEquals(138f, CouiClockContextualLayoutPolicy.contextualTop(
                100f, 32, false, 0f, 0, 6f), 0.001f);
    }

    @Test
    public void hiddenContextualCardNeverMovesExistingContentAnchor() {
        assertEquals(300f, CouiClockContextualLayoutPolicy.lowerContentTop(
                300f, false, 220f, 80, 56f, 12f), 0.001f);
    }

    @Test
    public void visibleContextualCardKeepsExistingAnchorWhenItFitsAboveIt() {
        assertEquals(300f, CouiClockContextualLayoutPolicy.lowerContentTop(
                300f, true, 220f, 40, 40f, 12f), 0.001f);
    }

    @Test
    public void visibleContextualCardPushesContentOnlyWhenClearanceRequiresIt() {
        assertEquals(336f, CouiClockContextualLayoutPolicy.lowerContentTop(
                300f, true, 240f, 84, 56f, 12f), 0.001f);
    }
}
