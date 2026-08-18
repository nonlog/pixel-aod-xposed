package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class CouiClockAppliedTargetPolicyTest {
    @Test
    public void identicalGlyphTargetIsStableAcrossNonAnimatingRefresh() {
        CouiClockAppliedTargetPolicy.Glyph first =
                CouiClockAppliedTargetPolicy.glyph(12.5f, 48f, 0.92f, 1f);
        CouiClockAppliedTargetPolicy.Glyph same =
                CouiClockAppliedTargetPolicy.glyph(12.5f, 48f, 0.92f, 1f);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
    }

    @Test
    public void positionScaleOrAlphaChangeProducesNewGlyphTarget() {
        CouiClockAppliedTargetPolicy.Glyph base =
                CouiClockAppliedTargetPolicy.glyph(12.5f, 48f, 0.92f, 1f);
        assertNotEquals(base, CouiClockAppliedTargetPolicy.glyph(13f, 48f, 0.92f, 1f));
        assertNotEquals(base, CouiClockAppliedTargetPolicy.glyph(12.5f, 49f, 0.92f, 1f));
        assertNotEquals(base, CouiClockAppliedTargetPolicy.glyph(12.5f, 48f, 1f, 1f));
        assertNotEquals(base, CouiClockAppliedTargetPolicy.glyph(12.5f, 48f, 0.92f, 0f));
    }

    @Test
    public void identicalInformationTargetDoesNotNeedToCancelRunningMotion() {
        CouiClockAppliedTargetPolicy.Information first =
                CouiClockAppliedTargetPolicy.information(30f, 240f, 1f);
        CouiClockAppliedTargetPolicy.Information same =
                CouiClockAppliedTargetPolicy.information(30f, 240f, 1f);
        assertEquals(first, same);
        assertNotEquals(first, CouiClockAppliedTargetPolicy.information(30f, 241f, 1f));
    }
}
