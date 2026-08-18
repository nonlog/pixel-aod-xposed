package dev.codex.pixelaod;

/**
 * Exact-value target snapshots used to preserve an already-running COUI property animation.
 *
 * <p>The COUI 2.5 host remembers the last target assigned to each glyph/information view and
 * returns before calling {@code ViewPropertyAnimator.cancel()} when a later data refresh computes
 * the same target. Without this guard an unrelated time/weather/media refresh can cancel the
 * position leg of a lockscreen/AOD transition while the font morph continues.</p>
 */
final class CouiClockAppliedTargetPolicy {
    private CouiClockAppliedTargetPolicy() {
    }

    static Glyph glyph(float x, float y, float scale, float alpha) {
        return new Glyph(x, y, scale, alpha);
    }

    static Information information(float x, float y, float alpha) {
        return new Information(x, y, alpha);
    }

    static final class Glyph {
        final float x;
        final float y;
        final float scale;
        final float alpha;

        Glyph(float x, float y, float scale, float alpha) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.alpha = alpha;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Glyph)) return false;
            Glyph value = (Glyph) other;
            return Float.compare(x, value.x) == 0
                    && Float.compare(y, value.y) == 0
                    && Float.compare(scale, value.scale) == 0
                    && Float.compare(alpha, value.alpha) == 0;
        }

        @Override
        public int hashCode() {
            int result = Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(scale);
            result = 31 * result + Float.floatToIntBits(alpha);
            return result;
        }
    }

    static final class Information {
        final float x;
        final float y;
        final float alpha;

        Information(float x, float y, float alpha) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Information)) return false;
            Information value = (Information) other;
            return Float.compare(x, value.x) == 0
                    && Float.compare(y, value.y) == 0
                    && Float.compare(alpha, value.alpha) == 0;
        }

        @Override
        public int hashCode() {
            int result = Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(alpha);
            return result;
        }
    }
}
