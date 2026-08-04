package dev.codex.pixelaod;

/** Pure geometry model used by the explicit compact-to-large clock size morph. */
final class AodGeometryHandoff {
    private static final float ANIMATION_THRESHOLD_PX = 0.5f;

    private AodGeometryHandoff() {
    }

    static Offset offsetToPreserveScreenPosition(float sourceX, float sourceY,
            float targetX, float targetY) {
        return new Offset(sourceX - targetX, sourceY - targetY);
    }

    static Snapshot snapshot(Point clock, Point date, Point weather) {
        return new Snapshot(clock, date, weather);
    }

    static final class Point {
        static final Point INVALID = new Point(0f, 0f, false);

        final float x;
        final float y;
        final boolean valid;

        Point(float x, float y) {
            this(x, y, true);
        }

        private Point(float x, float y, boolean valid) {
            this.x = x;
            this.y = y;
            this.valid = valid;
        }
    }

    static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(Point.INVALID, Point.INVALID, Point.INVALID);

        final Point clock;
        final Point date;
        final Point weather;

        Snapshot(Point clock, Point date, Point weather) {
            this.clock = clock != null ? clock : Point.INVALID;
            this.date = date != null ? date : Point.INVALID;
            this.weather = weather != null ? weather : Point.INVALID;
        }

        boolean hasAnyPoint() {
            return clock.valid || date.valid || weather.valid;
        }
    }

    static final class Offset {
        final float x;
        final float y;

        Offset(float x, float y) {
            this.x = Float.isFinite(x) ? x : 0f;
            this.y = Float.isFinite(y) ? y : 0f;
        }

        boolean shouldAnimate() {
            return Math.abs(x) >= ANIMATION_THRESHOLD_PX
                    || Math.abs(y) >= ANIMATION_THRESHOLD_PX;
        }
    }
}
