package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CouiClockRomTextAnimatorFallbackTest {
    @Test
    public void parsesWeightAndOpticalSizeAxesFromVariationString() {
        assertEquals(450f, CouiClockRomTextAnimatorRuntime.parseAxis(
                CouiClockFontPolicy.LARGE_VARIATION, "wght", 0f), 0.001f);
        assertEquals(144f, CouiClockRomTextAnimatorRuntime.parseAxis(
                CouiClockFontPolicy.LARGE_VARIATION, "opsz", 0f), 0.001f);
        assertEquals(100f, CouiClockRomTextAnimatorRuntime.parseAxis(
                CouiClockFontPolicy.AOD_LARGE_VARIATION, "wght", 0f), 0.001f);
        assertEquals(180f, CouiClockRomTextAnimatorRuntime.parseAxis(
                CouiClockFontPolicy.AOD_SMALL_VARIATION, "wght", 0f), 0.001f);
        assertEquals(96f, CouiClockRomTextAnimatorRuntime.parseAxis(
                CouiClockFontPolicy.AOD_SMALL_VARIATION, "opsz", 0f), 0.001f);
        assertEquals(400f, CouiClockRomTextAnimatorRuntime.parseAxis(
                null, "wght", 400f), 0.001f);
    }

    @Test
    public void quantizesVariationAxesToBoundedSteps() {
        assertEquals("'wght' 100, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0",
                CouiClockRomTextAnimatorRuntime.quantizeVariation(101.2f, 144f));
        assertEquals("'wght' 275, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 120, 'slnt' 0",
                CouiClockRomTextAnimatorRuntime.quantizeVariation(274.8f, 120.1f));
        assertEquals("'wght' 450, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0",
                CouiClockRomTextAnimatorRuntime.quantizeVariation(450f, 96f));
    }

    @Test
    public void interpolatesArgbEndpointsAndMidpoint() {
        assertEquals(0xFF000000, CouiClockRomTextAnimatorRuntime.interpolateArgb(
                0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, CouiClockRomTextAnimatorRuntime.interpolateArgb(
                0xFF000000, 0xFFFFFFFF, 1f));
        assertEquals(0xFF808080, CouiClockRomTextAnimatorRuntime.interpolateArgb(
                0xFF000000, 0xFFFFFFFF, 0.5f));
    }
}
