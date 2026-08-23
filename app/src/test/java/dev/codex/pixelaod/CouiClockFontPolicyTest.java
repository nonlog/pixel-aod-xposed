package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockFontPolicyTest {
    @Test
    public void usesExactReferenceVariationsForAllVisualSurfaces() {
        assertEquals("'wght' 450, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0",
                CouiClockFontPolicy.variationFor(CouiClockPresentationModel.Scene.LARGE,
                        false));
        assertEquals("'wght' 500, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0",
                CouiClockFontPolicy.variationFor(CouiClockPresentationModel.Scene.SMALL,
                        false));
        assertEquals("'wght' 100, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0",
                CouiClockFontPolicy.variationFor(CouiClockPresentationModel.Scene.LARGE,
                        true));
        assertEquals("'wght' 180, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0",
                CouiClockFontPolicy.variationFor(CouiClockPresentationModel.Scene.SMALL,
                        true));
    }

    @Test
    public void informationVariationKeepsDefaultTextSquareButAllowsRoundedDateWeather() {
        assertEquals("'wght' 500, 'wdth' 100, 'ROND' 0, 'GRAD' 0, 'opsz' 18",
                CouiClockFontPolicy.informationVariation(500, false));
        assertEquals("'wght' 500, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 18",
                CouiClockFontPolicy.informationVariation(500, true));
    }

    @Test
    public void morphStyleCarriesVariationColorAnimationAndDuration() {
        CouiClockFontPolicy.MorphStyleSpec style = CouiClockFontPolicy.morphStyle(
                CouiClockPresentationModel.Scene.LARGE, false, 0x00112233, true, 550L);

        assertEquals(CouiClockFontPolicy.variationFor(
                CouiClockPresentationModel.Scene.LARGE, false), style.variation());
        assertEquals(0x00112233, style.color());
        assertTrue(style.animate());
        assertEquals(550L, style.durationMillis());
        assertEquals(0.2f, style.interpolatorX1(), 0f);
        assertEquals(1f, style.interpolatorY2(), 0f);
    }

    @Test
    public void runtimeUsesSingleMorphingLargeSetForEveryVisualState() {
        assertEquals(CouiClockFontPolicy.GlyphMode.MORPHING_LARGE,
                CouiClockFontPolicy.glyphMode(
                        CouiClockPresentationModel.Scene.LARGE, false, true));
        assertEquals(CouiClockFontPolicy.GlyphMode.MORPHING_LARGE,
                CouiClockFontPolicy.glyphMode(
                        CouiClockPresentationModel.Scene.SMALL, false, true));
        assertEquals(CouiClockFontPolicy.GlyphMode.MORPHING_LARGE,
                CouiClockFontPolicy.glyphMode(
                        CouiClockPresentationModel.Scene.LARGE, true, true));
        assertEquals(CouiClockFontPolicy.GlyphMode.MORPHING_LARGE,
                CouiClockFontPolicy.glyphMode(
                        CouiClockPresentationModel.Scene.SMALL, true, true));
    }

    @Test
    public void morphRuntimeKeepsStableMetricCellsAcrossDozeWeightChanges() {
        assertEquals(CouiClockFontPolicy.FallbackSet.LOCKSCREEN_LARGE,
                CouiClockFontPolicy.metricSetFor(
                        CouiClockPresentationModel.Scene.LARGE, true, true));
        assertEquals(CouiClockFontPolicy.FallbackSet.LOCKSCREEN_SMALL,
                CouiClockFontPolicy.metricSetFor(
                        CouiClockPresentationModel.Scene.SMALL, true, true));
        assertEquals(CouiClockFontPolicy.FallbackSet.LOCKSCREEN_SMALL,
                CouiClockFontPolicy.metricSetFor(
                        CouiClockPresentationModel.Scene.IMMERSED, true, true));
    }

    @Test
    public void fallbackRendererStillUsesSurfaceSpecificMetricSets() {
        assertEquals(CouiClockFontPolicy.FallbackSet.AOD_LARGE,
                CouiClockFontPolicy.metricSetFor(
                        CouiClockPresentationModel.Scene.LARGE, true, false));
        assertEquals(CouiClockFontPolicy.FallbackSet.AOD_SMALL,
                CouiClockFontPolicy.metricSetFor(
                        CouiClockPresentationModel.Scene.SMALL, true, false));
        assertEquals(CouiClockFontPolicy.FallbackSet.LOCKSCREEN_SMALL,
                CouiClockFontPolicy.metricSetFor(
                        CouiClockPresentationModel.Scene.SMALL, false, false));
    }

    @Test
    public void runtimeUnavailableUsesFourSetFallbackVisibility() {
        assertEquals(CouiClockFontPolicy.GlyphMode.FOUR_SET_CROSSFADE,
                CouiClockFontPolicy.glyphMode(
                        CouiClockPresentationModel.Scene.LARGE, false, false));
        assertTrue(CouiClockFontPolicy.fallbackSetVisible(
                CouiClockFontPolicy.FallbackSet.LOCKSCREEN_LARGE,
                CouiClockPresentationModel.Scene.LARGE, false));
        assertFalse(CouiClockFontPolicy.fallbackSetVisible(
                CouiClockFontPolicy.FallbackSet.LOCKSCREEN_LARGE,
                CouiClockPresentationModel.Scene.SMALL, false));
        assertTrue(CouiClockFontPolicy.fallbackSetVisible(
                CouiClockFontPolicy.FallbackSet.AOD_SMALL,
                CouiClockPresentationModel.Scene.SMALL, true));
    }
}
