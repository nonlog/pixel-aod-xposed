package dev.codex.pixelaod;

/** Pure interpolation rules shared by the COUI-style per-glyph clock transition. */
final class CouiClockSizeTransitionMath {
    static final float COLON_ALPHA_DURATION_FRACTION = 0.22f;
    static final float COLON_ENTER_DELAY_FRACTION = 0.52f;

    private CouiClockSizeTransitionMath() {
    }

    static boolean isSizeChange(boolean fromCompact, boolean toCompact) {
        return fromCompact != toCompact;
    }

    static boolean shouldRunActualSizeTransition(boolean sceneRequestsSizeChange,
            boolean actualFromCompact, boolean actualToCompact) {
        return sceneRequestsSizeChange && isSizeChange(actualFromCompact, actualToCompact);
    }

    static boolean isSameSurfaceSizeChange(boolean fromLockscreen, boolean toLockscreen,
            boolean fromCompact, boolean toCompact) {
        return fromLockscreen == toLockscreen && isSizeChange(fromCompact, toCompact);
    }

    static boolean isPresentationMorphCurrent(long scheduledGeneration,
            long currentGeneration) {
        return scheduledGeneration == currentGeneration;
    }

    static Frame frame(Element from, Element to, float progress) {
        float bounded = clamp01(progress);
        float size = lerp(from.textSizePx, to.textSizePx, bounded);
        float sourceSize = Math.max(1f, from.textSizePx);
        return new Frame(
                lerp(from.centerX, to.centerX, bounded),
                lerp(from.centerY, to.centerY, bounded),
                size / sourceSize,
                lerp(from.alpha, to.alpha, bounded));
    }

    /**
     * Information rows must change their text metrics directly. Scaling the whole TextView also
     * scales fixed-size compound drawables (the weather icon), which creates the visible
     * shrink-then-expand rebound when the real target view takes over.
     */
    static InfoFrame informationFrame(Element from, Element to, float progress) {
        float bounded = clamp01(progress);
        return new InfoFrame(
                lerp(from.centerX, to.centerX, bounded),
                lerp(from.centerY, to.centerY, bounded),
                lerp(from.textSizePx, to.textSizePx, bounded),
                lerp(from.alpha, to.alpha, bounded));
    }

    /**
     * Keeps a {@code FixedAdvanceSpan} text corridor intact while its visible size changes.
     * Rebuilding a replacement span at every in-between size rounds every glyph cell separately;
     * scaling the source corridor about its painted centre keeps the characters continuous.
     */
    static float fixedCellTextScale(float sourceTextSizePx, float frameTextSizePx) {
        return Math.max(1f, frameTextSizePx) / Math.max(1f, sourceTextSizePx);
    }

    static int interpolatedDimension(int from, int to, float progress) {
        return Math.round(lerp(from, to, clamp01(progress)));
    }

    static float interpolatedFloat(float from, float to, float progress) {
        return lerp(from, to, clamp01(progress));
    }

    /**
     * Re-eases an in-flight motion from its current visual progress to a new endpoint.
     *
     * <p>The size layer's normal path is an ease-out. Calling {@code ValueAnimator.reverse()}
     * while continuing to evaluate that original ease-out backwards makes the first half of a
     * reversal look abnormally slow: the animation starts in the original curve's near-flat
     * tail.  Treat every redirection as a fresh segment instead.  The driver can still reverse
     * in place, but the visual motion begins at the exact current frame and gets a new ease-out
     * toward the requested endpoint.</p>
     */
    static float redirectedMotionProgress(float driverProgress,
            float segmentStartDriver, float segmentEndDriver,
            float segmentStartMotion, float segmentEndMotion, Easing easing) {
        float driverDistance = segmentEndDriver - segmentStartDriver;
        if (Math.abs(driverDistance) < 0.0001f) {
            return clamp01(segmentEndMotion);
        }
        float segmentProgress = clamp01(
                (driverProgress - segmentStartDriver) / driverDistance);
        Easing safeEasing = easing != null ? easing : value -> value;
        float eased = clamp01(safeEasing.apply(segmentProgress));
        return clamp01(lerp(segmentStartMotion, segmentEndMotion, eased));
    }

    static float paintedBoundsCenter(float paintedStart, float paintedEnd) {
        return (paintedStart + paintedEnd) / 2f;
    }

    static float replacementSpanPaintOrigin(float cellStart, float referenceGlyphAdvance,
            float animatedGlyphAdvance) {
        return cellStart + ((referenceGlyphAdvance - animatedGlyphAdvance) / 2f);
    }

    static float paintedGlyphCenter(float cellStart, float referenceGlyphAdvance,
            float animatedGlyphAdvance, float paintedLeft, float paintedRight) {
        return replacementSpanPaintOrigin(cellStart, referenceGlyphAdvance,
                animatedGlyphAdvance) + paintedBoundsCenter(paintedLeft, paintedRight);
    }

    /**
     * Stable horizontal owner for one clock digit, matching a COUI DigitalTimeView slot and the
     * module's own FixedAdvanceSpan contract. Variable-font ink is allowed to change inside the
     * slot; the slot itself must never be re-anchored from the glyph's changing painted bounds.
     */
    static float fixedGlyphCellCenter(float cellStart, float referenceGlyphAdvance) {
        return cellStart + (Math.max(0f, referenceGlyphAdvance) / 2f);
    }

    static float paintedBaselineCenter(float baseline, float paintedTop, float paintedBottom) {
        return baseline + paintedBoundsCenter(paintedTop, paintedBottom);
    }

    /**
     * Mirrors TextView's CENTER_VERTICAL baseline rule. Android only centres a text layout when
     * the font line is shorter than the available box; a taller line is pinned to the top. Using
     * an unconditional centred baseline moves the painted glyphs down when a compact overlay box
     * is shorter than the font metrics.
     */
    static float centeredTextBaseline(float boxHeight, float ascent, float descent) {
        float safeHeight = Math.max(0f, boxHeight);
        float lineHeight = Math.max(0f, descent - ascent);
        float verticalOffset = lineHeight < safeHeight
                ? (float) Math.floor((safeHeight - lineHeight) / 2f)
                : 0f;
        return verticalOffset - ascent;
    }

    /** Returns the painted-content center inside a gravity-centered clone box. */
    static float visualContentOffset(float boxSize, float contentAdvance,
            float paintedStart, float paintedEnd) {
        float centeredContentStart = (Math.max(0f, boxSize)
                - Math.max(0f, contentAdvance)) / 2f;
        return centeredContentStart + paintedBoundsCenter(paintedStart, paintedEnd);
    }

    /** Positions a clone so its painted-content center, rather than its outer box, hits target. */
    static float positionForVisualCenter(float targetCenter, float visualContentOffset) {
        return targetCenter - visualContentOffset;
    }

    static float colonAlpha(float fromAlpha, float toAlpha, float linearProgress) {
        return colonAlpha(fromAlpha, toAlpha, linearProgress, progress -> progress);
    }

    static float colonAlpha(float fromAlpha, float toAlpha, float linearProgress,
            Easing easing) {
        float progress = clamp01(linearProgress);
        Easing safeEasing = easing != null ? easing : value -> value;
        if (fromAlpha > toAlpha) {
            float fade = clamp01(progress / COLON_ALPHA_DURATION_FRACTION);
            return lerp(fromAlpha, toAlpha, clamp01(safeEasing.apply(fade)));
        }
        if (toAlpha > fromAlpha) {
            float fade = clamp01((progress - COLON_ENTER_DELAY_FRACTION)
                    / COLON_ALPHA_DURATION_FRACTION);
            return lerp(fromAlpha, toAlpha, clamp01(safeEasing.apply(fade)));
        }
        return fromAlpha;
    }

    static int interpolatedWeight(int fromWeight, int toWeight, float progress) {
        return Math.round(lerp(fromWeight, toWeight, clamp01(progress)));
    }

    private static float lerp(float from, float to, float progress) {
        return from + ((to - from) * progress);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    interface Easing {
        float apply(float progress);
    }

    static final class Element {
        final float centerX;
        final float centerY;
        final float textSizePx;
        final float alpha;

        Element(float centerX, float centerY, float textSizePx, float alpha) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.textSizePx = Math.max(1f, textSizePx);
            this.alpha = clamp01(alpha);
        }
    }

    static final class Frame {
        final float centerX;
        final float centerY;
        final float scaleFromSource;
        final float alpha;

        Frame(float centerX, float centerY, float scaleFromSource, float alpha) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.scaleFromSource = scaleFromSource;
            this.alpha = alpha;
        }
    }

    /** A position-and-metrics frame for date, weather, and contextual information rows. */
    static final class InfoFrame {
        final float centerX;
        final float centerY;
        final float textSizePx;
        final float alpha;

        InfoFrame(float centerX, float centerY, float textSizePx, float alpha) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.textSizePx = Math.max(1f, textSizePx);
            this.alpha = alpha;
        }
    }
}
