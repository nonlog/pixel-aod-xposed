package dev.codex.pixelaod;

final class ClockGlyphMetrics {
    private ClockGlyphMetrics() {
    }

    static float cellAdvance(float referenceGlyphAdvance, float letterSpacingPixels,
            boolean lineEnd) {
        return referenceGlyphAdvance + (lineEnd ? 0f : letterSpacingPixels);
    }

    static float compactTrackingPixels(float textSizePx, char glyph, char nextGlyph,
            boolean lineEnd) {
        if (lineEnd) {
            return 0f;
        }
        float trackingEm = glyph == ':' || nextGlyph == ':'
                ? PixelAodVisualStyle.COMPACT_CLOCK_COLON_TRACKING_EM
                : PixelAodVisualStyle.COMPACT_CLOCK_DIGIT_TRACKING_EM;
        return textSizePx * trackingEm;
    }

    static float infoTrackingPixels(float textSizePx, boolean lineEnd) {
        return lineEnd ? 0f : textSizePx * PixelAodVisualStyle.INFO_LETTER_SPACING;
    }

    static float centerOffset(float referenceGlyphAdvance, float animatedGlyphAdvance) {
        return (referenceGlyphAdvance - animatedGlyphAdvance) / 2f;
    }
}
