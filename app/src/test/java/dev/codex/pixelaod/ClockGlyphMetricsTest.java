package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ClockGlyphMetricsTest {
    @Test
    public void keepsEachClockCellAtTheLockscreenAdvanceDuringWeightAnimation() {
        float referenceDigitAdvance = 1172f;
        float animatedDigitAdvance = 1178f;
        float spacing = -20f;

        assertEquals(1152f,
                ClockGlyphMetrics.cellAdvance(referenceDigitAdvance, spacing, false),
                0.001f);
        assertEquals(-3f,
                ClockGlyphMetrics.centerOffset(referenceDigitAdvance, animatedDigitAdvance),
                0.001f);
    }

    @Test
    public void centersNarrowAnimatedColonInsideItsFixedReferenceCell() {
        assertEquals(28f, ClockGlyphMetrics.centerOffset(405f, 349f), 0.001f);
        assertEquals(405f, ClockGlyphMetrics.cellAdvance(405f, -20f, true), 0.001f);
    }
}
