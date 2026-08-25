package dev.codex.pixelaod;

/**
 * Literal, host-independent geometry contract for the COUI clock surfaces.
 */
public final class CouiClockGeometryPolicy {
    public static final float WDTH = 100f;
    public static final float ROND = 100f;
    public static final float GRAD = 0f;
    public static final float SLNT = 0f;

    public static final float INFO_CENTER_RATIO = .75f;
    public static final float LOCKSCREEN_INFO_X_DP = -36f;
    public static final float AOD_INFO_X_DP = -34f;
    public static final float INFO_Y_RATIO = .118f;
    public static final float INFO_Y_OFFSET_DP = 33f;
    public static final float DATE_WEATHER_GAP_DP = 1f;
    public static final float COMPACT_CLOCK_INFO_MIN_GAP_DP = 12f;
    public static final float WEATHER_ICON_SLOT_DP = 22f;
    public static final float WEATHER_ICON_CONTENT_INSET_DP = 2f;
    public static final float WEATHER_ICON_GAP_DP = 4f;
    public static final float PARTIAL_CONTENT_TOP_RATIO = .255f;
    public static final float PARTIAL_CONTENT_X_DP = 32f;
    public static final float MEDIA_TO_NOTIFICATION_GAP_DP = 28f;
    public static final float NOTIFICATION_ICON_SIZE_DP = 18f;
    public static final float NOTIFICATION_ICON_GAP_DP = 15f;
    public static final int MAX_NOTIFICATION_ICONS = 7;
    public static final int BURN_IN_X_PERIOD_MINUTES = 83;
    public static final int BURN_IN_Y_PERIOD_MINUTES = 521;
    public static final float BATTERY_BURN_IN_X_SCALE = .75f;
    public static final float BATTERY_BURN_IN_Y_SCALE = .5f;
    public static final float BATTERY_BOTTOM_MARGIN_DP = 64f;

    public enum Surface {
        LS_LARGE,
        AOD_LARGE,
        LS_SMALL,
        AOD_SMALL,
        LS_IMMERSED
    }

    public static final class SurfaceTarget {
        public final float baseWidthRatio;
        public final float topRatio;
        public final float topDp;
        public final float scale;
        public final float centerRatio;
        public final float centerDp;
        public final float weight;
        public final float opsz;
        public final float trackingFactor;
        public final float textRatio;
        public final float infoYRatio;
        public final float wdth;
        public final float rond;
        public final float grad;
        public final float slnt;
        public final boolean burnInEnabled;

        private SurfaceTarget(float baseWidthRatio, float topRatio, float topDp,
                float scale, float centerRatio, float centerDp, float weight,
                float opsz, float trackingFactor, float textRatio, float infoYRatio,
                boolean burnInEnabled) {
            this.baseWidthRatio = baseWidthRatio;
            this.topRatio = topRatio;
            this.topDp = topDp;
            this.scale = scale;
            this.centerRatio = centerRatio;
            this.centerDp = centerDp;
            this.weight = weight;
            this.opsz = opsz;
            this.trackingFactor = trackingFactor;
            this.textRatio = textRatio;
            this.infoYRatio = infoYRatio;
            this.wdth = WDTH;
            this.rond = ROND;
            this.grad = GRAD;
            this.slnt = SLNT;
            this.burnInEnabled = burnInEnabled;
        }
    }

    public static final SurfaceTarget LS_LARGE = new SurfaceTarget(
            .47f, .215f, -10f, 1.0f, 0f, 0f, 450f, 144f, -.07f,
            0f, 0f, false);
    public static final SurfaceTarget AOD_LARGE = new SurfaceTarget(
            .47f, .215f, -24f, .9f, 0f, 0f, 100f, 144f, -.06f,
            0f, 0f, true);
    public static final SurfaceTarget LS_SMALL = new SurfaceTarget(
            0f, .105f, 25f, .36170214f, .25f, 8f, 500f, 96f, -.09f,
            0f, 0f, false);
    public static final SurfaceTarget AOD_SMALL = new SurfaceTarget(
            0f, .105f, 25f, .36170214f, .25f, 10f, 180f, 96f, -.09f,
            0f, 0f, true);
    public static final SurfaceTarget LS_IMMERSED = new SurfaceTarget(
            0f, .072f, 30f, .32978722f, .25f, 8f, 500f, 96f, -.09f,
            .155f, .09f, false);

    static float resolveCompactInformationStart(float centeredStart, float clockRight,
            float minimumGap, float maximumStart) {
        float requiredStart = Math.max(centeredStart, clockRight + Math.max(0f, minimumGap));
        float safeMaximumStart = Math.max(centeredStart, maximumStart);
        return Math.min(requiredStart, safeMaximumStart);
    }

    private CouiClockGeometryPolicy() {
    }

    public static SurfaceTarget target(Surface surface) {
        if (surface == null) {
            throw new IllegalArgumentException("surface must not be null");
        }
        switch (surface) {
            case LS_LARGE:
                return LS_LARGE;
            case AOD_LARGE:
                return AOD_LARGE;
            case LS_SMALL:
                return LS_SMALL;
            case AOD_SMALL:
                return AOD_SMALL;
            case LS_IMMERSED:
                return LS_IMMERSED;
            default:
                throw new IllegalArgumentException("Unsupported surface: " + surface);
        }
    }
}
