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

    /** Keeps information glyph ink on the cell's fixed origin during a weight handoff. */
    static float fixedOriginOffset(float referenceGlyphAdvance, float animatedGlyphAdvance) {
        return 0f;
    }

    /**
     * A replacement span stores its reference cell in pixels.  During a size transaction the
     * Paint changes before the text is re-spanned, so keep that stored cell in the same scale as
     * the current Paint instead of leaving the glyph to move inside an old-size cell.
     */
    static float scaledForTextSize(float valuePx, float referenceTextSizePx,
            float currentTextSizePx) {
        if (referenceTextSizePx <= 0f || currentTextSizePx <= 0f) {
            return valuePx;
        }
        return valuePx * (currentTextSizePx / referenceTextSizePx);
    }
}
