package dev.codex.pixelaod;

/** Literal digit corrections used by the COUI compact/immersed line layout. */
final class CouiClockGlyphCorrection {
    private CouiClockGlyphCorrection() {
    }

    static float leftTrimOffset(char digit, float lineWidth) {
        if (digit == '0') {
            return -lineWidth * 0.05f;
        }
        if (digit == '1') {
            return lineWidth * 0.06f;
        }
        return 0f;
    }

    static float rightSideExpansion(char digit, float lineWidth) {
        if (digit == '0') {
            return -lineWidth * 0.05f;
        }
        if (digit == '1') {
            return lineWidth * 0.09f;
        }
        return 0f;
    }
}
