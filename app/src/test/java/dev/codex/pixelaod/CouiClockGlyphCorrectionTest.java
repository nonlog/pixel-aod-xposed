package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CouiClockGlyphCorrectionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void zeroUsesReferenceNegativeTrimOnBothSides() {
        assertEquals(-5f,
                CouiClockGlyphCorrection.leftTrimOffset('0', 100f), EPSILON);
        assertEquals(-5f,
                CouiClockGlyphCorrection.rightSideExpansion('0', 100f), EPSILON);
    }

    @Test
    public void oneUsesReferenceAsymmetricTrimAndExpansion() {
        assertEquals(6f,
                CouiClockGlyphCorrection.leftTrimOffset('1', 100f), EPSILON);
        assertEquals(9f,
                CouiClockGlyphCorrection.rightSideExpansion('1', 100f), EPSILON);
    }

    @Test
    public void otherDigitsHaveNoCorrection() {
        assertEquals(0f,
                CouiClockGlyphCorrection.leftTrimOffset('8', 100f), EPSILON);
        assertEquals(0f,
                CouiClockGlyphCorrection.rightSideExpansion('8', 100f), EPSILON);
    }
}
