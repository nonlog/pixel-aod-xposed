package dev.codex.pixelaod;

final class ClockGlyphMetrics {
    private ClockGlyphMetrics() {
    }

    static float cellAdvance(float referenceGlyphAdvance, float letterSpacingPixels,
            boolean lineEnd) {
        return referenceGlyphAdvance + (lineEnd ? 0f : letterSpacingPixels);
    }

    static float centerOffset(float referenceGlyphAdvance, float animatedGlyphAdvance) {
        return (referenceGlyphAdvance - animatedGlyphAdvance) / 2f;
    }
}
