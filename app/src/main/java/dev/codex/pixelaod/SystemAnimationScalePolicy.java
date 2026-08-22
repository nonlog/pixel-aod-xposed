package dev.codex.pixelaod;

import android.animation.ValueAnimator;

/**
 * One accessibility-aware timing policy for module-owned presentation motion.
 *
 * <p>Framework Animator implementations already apply {@link ValueAnimator#getDurationScale()}
 * internally. Callers must therefore keep their existing baseline setDuration() values and use
 * {@link #scaledNonAnimatorDelayMillis(long)} only for module-owned Handler/View delays that must
 * stay synchronized with an Animator. Multiplying both would double-scale motion.</p>
 */
final class SystemAnimationScalePolicy {
    static final float DEFAULT_SCALE = 1f;
    private static final float DEFAULT_EPSILON = 0.0001f;

    private SystemAnimationScalePolicy() {
    }

    static Snapshot current() {
        float scale = DEFAULT_SCALE;
        try {
            scale = ValueAnimator.getDurationScale();
        } catch (Throwable ignored) {
            // Preserve the proven 1x presentation path if the runtime input is unavailable.
        }
        return fromRawScale(scale);
    }

    static boolean animationsEnabled() {
        return current().animationsEnabled;
    }

    static boolean shouldAnimate(boolean requested) {
        return requested && animationsEnabled();
    }

    static long scaledNonAnimatorDelayMillis(long baselineMillis) {
        return current().scaledNonAnimatorDelayMillis(baselineMillis);
    }

    static String describe() {
        return current().describe();
    }

    static Snapshot fromRawScale(float rawScale) {
        float scale = sanitizeScale(rawScale);
        return new Snapshot(scale);
    }

    private static float sanitizeScale(float rawScale) {
        if (Float.isNaN(rawScale) || Float.isInfinite(rawScale) || rawScale < 0f) {
            return DEFAULT_SCALE;
        }
        return rawScale;
    }

    static final class Snapshot {
        final float scale;
        final boolean animationsEnabled;
        final boolean defaultScale;

        Snapshot(float scale) {
            this.scale = scale;
            this.animationsEnabled = scale > 0f;
            this.defaultScale = Math.abs(scale - DEFAULT_SCALE) <= DEFAULT_EPSILON;
        }

        /**
         * Baseline passed to framework Animator APIs. Android applies {@code scale} itself.
         */
        long frameworkAnimatorDurationMillis(long baselineMillis) {
            return Math.max(0L, baselineMillis);
        }

        /**
         * Wall-clock delay for a module-owned non-Animator callback paired with an Animator.
         */
        long scaledNonAnimatorDelayMillis(long baselineMillis) {
            if (baselineMillis <= 0L || !animationsEnabled) {
                return 0L;
            }
            if (defaultScale) {
                return baselineMillis;
            }
            double scaled = baselineMillis * (double) scale;
            if (scaled >= Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            return Math.max(1L, Math.round(scaled));
        }

        String describe() {
            return "scale=" + scale
                    + ",enabled=" + animationsEnabled
                    + ",default=" + defaultScale;
        }
    }
}