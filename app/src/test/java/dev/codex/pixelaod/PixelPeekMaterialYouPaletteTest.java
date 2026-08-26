package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class PixelPeekMaterialYouPaletteTest {
    @Test
    public void currentTealPaletteKeepsNeutralSurfaceWithSubtlePrimaryTint() {
        int neutral = 0xff2c3131;
        int primary = 0xff003738;

        int color = PixelPeekMaterialYouPalette.blendAndLimit(neutral, primary, 0.10f, 56);

        assertEquals(0xff283232, color);
    }

    @Test
    public void brightPaletteIsLimitedForAodUse() {
        int color = PixelPeekMaterialYouPalette.blendAndLimit(
                0xff707070, 0xff00ffff, 0.25f, 56);

        int red = (color >>> 16) & 0xff;
        int green = (color >>> 8) & 0xff;
        int blue = color & 0xff;
        assertTrue(Math.max(red, Math.max(green, blue)) <= 56);
    }

    @Test
    public void zeroTintReturnsNeutralWhenAlreadyWithinAodBudget() {
        assertEquals(0xff283030, PixelPeekMaterialYouPalette.blendAndLimit(
                0xff283030, 0xff00ffff, 0f, 56));
    }
}
