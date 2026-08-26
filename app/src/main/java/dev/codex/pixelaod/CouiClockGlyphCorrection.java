package dev.codex.pixelaod;

/** Literal digit corrections used by the COUI compact/immersed line layout. */
final class CouiClockGlyphCorrection {
    private CouiClockGlyphCorrection() {
    }

    static float leftTrimOffset(char digit, float lineWidth) {
        int numericDigit = Character.digit(digit, 10);
        if (numericDigit == 0) {
            return -lineWidth * 0.05f;
        }
        if (numericDigit == 1) {
            return lineWidth * 0.06f;
        }
        return 0f;
    }

    static float rightSideExpansion(char digit, float lineWidth) {
        int numericDigit = Character.digit(digit, 10);
        if (numericDigit == 0) {
            return -lineWidth * 0.05f;
        }
        if (numericDigit == 1) {
            return lineWidth * 0.09f;
        }
        return 0f;
    }
}
