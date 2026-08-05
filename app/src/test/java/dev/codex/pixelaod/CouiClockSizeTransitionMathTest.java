package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CouiClockSizeTransitionMathTest {
    @Test
    public void eachGlyphMovesTowardItsOwnTargetInsteadOfScalingAsOneBlock() {
        CouiClockSizeTransitionMath.Element from =
                new CouiClockSizeTransitionMath.Element(100f, 200f, 56f, 1f);
        CouiClockSizeTransitionMath.Element topTarget =
                new CouiClockSizeTransitionMath.Element(300f, 400f, 150f, 1f);
        CouiClockSizeTransitionMath.Element bottomTarget =
                new CouiClockSizeTransitionMath.Element(300f, 700f, 150f, 1f);

        CouiClockSizeTransitionMath.Frame top =
                CouiClockSizeTransitionMath.frame(from, topTarget, 0.5f);
        CouiClockSizeTransitionMath.Frame bottom =
                CouiClockSizeTransitionMath.frame(from, bottomTarget, 0.5f);

        assertEquals(200f, top.centerX, 0.001f);
        assertEquals(300f, top.centerY, 0.001f);
        assertEquals(450f, bottom.centerY, 0.001f);
        assertEquals(103f / 56f, top.scaleFromSource, 0.001f);
    }

    @Test
    public void colonFadesOutAtTheStartOfCompactToLarge() {
        assertEquals(1f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0f), 0.001f);
        assertEquals(0.5f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0.11f), 0.001f);
        assertEquals(0f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0.22f), 0.001f);
        assertEquals(0f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0.8f), 0.001f);
    }

    @Test
    public void colonWaitsBeforeAppearingDuringLargeToCompact() {
        assertEquals(0f, CouiClockSizeTransitionMath.colonAlpha(0f, 1f, 0.51f), 0.001f);
        assertEquals(0.5f, CouiClockSizeTransitionMath.colonAlpha(0f, 1f, 0.63f), 0.001f);
        assertEquals(1f, CouiClockSizeTransitionMath.colonAlpha(0f, 1f, 0.74f), 0.001f);
    }

    @Test
    public void runtimeColonPathAppliesEasingInsideTheCouiTimingWindow() {
        assertEquals(0.25f, CouiClockSizeTransitionMath.colonAlpha(
                0f, 1f, 0.63f, progress -> progress * progress), 0.001f);
    }

    @Test
    public void trailingTrackingDoesNotMoveThePaintedGlyphCenter() {
        float referenceAdvance = 40f;
        float cellAdvanceWithTracking = 34f;

        assertEquals(120f, CouiClockSizeTransitionMath.glyphCenter(100f, referenceAdvance),
                0.001f);
        assertFalse(Math.abs(117f - CouiClockSizeTransitionMath.glyphCenter(
                100f, referenceAdvance)) < 0.001f);
        assertEquals(134f, 100f + cellAdvanceWithTracking, 0.001f);
    }

    @Test
    public void onlyLargeSmallChangesNeedTheGlyphTransaction() {
        assertTrue(CouiClockSizeTransitionMath.isSizeChange(false, true));
        assertTrue(CouiClockSizeTransitionMath.isSizeChange(true, false));
        assertFalse(CouiClockSizeTransitionMath.isSizeChange(false, false));
        assertFalse(CouiClockSizeTransitionMath.isSizeChange(true, true));
    }

    @Test
    public void sameSurfaceLargeSmallChangesUseTheGlyphTransaction() {
        assertTrue(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                true, true, false, true));
        assertTrue(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                false, false, true, false));
    }

    @Test
    public void crossSurfaceChangesDoNotUseTheGlyphTransaction() {
        assertFalse(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                true, false, false, true));
        assertFalse(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                false, true, true, false));
    }
}
