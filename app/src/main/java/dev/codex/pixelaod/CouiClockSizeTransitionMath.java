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

    static boolean isSameSurfaceSizeChange(boolean fromLockscreen, boolean toLockscreen,
            boolean fromCompact, boolean toCompact) {
        return fromLockscreen == toLockscreen && isSizeChange(fromCompact, toCompact);
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

    static float glyphCenter(float cellStart, float referenceGlyphAdvance) {
        return cellStart + (Math.max(0f, referenceGlyphAdvance) / 2f);
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
}
