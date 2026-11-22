package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CouiCompactClockAnchorPolicyTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void stableAnchorMatchesCoui27FourEightReferenceWidth() {
        assertEquals(165f, CouiCompactClockAnchorPolicy.centeredStart(
                360f, 100f, 20f, -10f, -5f), EPSILON);
    }

    @Test
    public void opticalDigitCorrectionsDoNotMoveTheOverallCenterAnchor() {
        float zeroLeft = CouiClockGlyphCorrection.leftTrimOffset('0', 100f);
        float oneRight = CouiClockGlyphCorrection.rightSideExpansion('1', 100f);
        assertEquals(-5f, zeroLeft, EPSILON);
        assertEquals(9f, oneRight, EPSILON);
        assertEquals(165f, CouiCompactClockAnchorPolicy.centeredStart(
                360f, 100f, 20f, -10f, -5f), EPSILON);
    }

    @Test
    public void anchorDigitRemainsEightLikeCoui27() {
        assertEquals("8", CouiCompactClockAnchorPolicy.ANCHOR_DIGIT);
    }
}
