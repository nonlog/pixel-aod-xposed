package dev.codex.pixelaod;

/** Pure COUI font, morph-style, and fallback glyph-set policy. */
final class CouiClockFontPolicy {
    static final String FONT_ASSET_PATH = "fonts/GoogleSansFlex-Variable.ttf";
    static final String TABULAR_NUMBERS = "tnum";
    static final String LARGE_VARIATION =
            "'wght' 450, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0";
    static final String SMALL_VARIATION =
            "'wght' 500, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0";
    static final String AOD_LARGE_VARIATION =
            "'wght' 100, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0";
    static final String AOD_SMALL_VARIATION =
            "'wght' 180, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0";

    enum GlyphMode {
        MORPHING_LARGE,
        FOUR_SET_CROSSFADE
    }

    enum FallbackSet {
        LOCKSCREEN_LARGE,
        LOCKSCREEN_SMALL,
        AOD_LARGE,
        AOD_SMALL
    }

    private CouiClockFontPolicy() {
    }

    static String variationFor(CouiClockPresentationModel.Scene scene, boolean dozing) {
        CouiClockPresentationModel.Scene normalized = scene == null
                ? CouiClockPresentationModel.Scene.LARGE : scene;
        if (dozing) {
            return normalized == CouiClockPresentationModel.Scene.LARGE
                    ? AOD_LARGE_VARIATION : AOD_SMALL_VARIATION;
        }
        return normalized == CouiClockPresentationModel.Scene.LARGE
                ? LARGE_VARIATION : SMALL_VARIATION;
    }

    static GlyphMode glyphMode(CouiClockPresentationModel.Scene scene, boolean dozing,
            boolean runtimeAvailable) {
        return runtimeAvailable
                ? GlyphMode.MORPHING_LARGE : GlyphMode.FOUR_SET_CROSSFADE;
    }

    static boolean fallbackSetVisible(FallbackSet set,
            CouiClockPresentationModel.Scene scene, boolean dozing) {
        if (set == null) {
            return false;
        }
        boolean large = scene == CouiClockPresentationModel.Scene.LARGE;
        switch (set) {
            case LOCKSCREEN_LARGE:
                return !dozing && large;
            case LOCKSCREEN_SMALL:
                return !dozing && !large;
            case AOD_LARGE:
                return dozing && large;
            case AOD_SMALL:
                return dozing && !large;
            default:
                return false;
        }
    }

    static MorphStyleSpec morphStyle(CouiClockPresentationModel.Scene scene, boolean dozing,
            int color, boolean animate, long durationMillis) {
        return new MorphStyleSpec(variationFor(scene, dozing), color, animate,
                durationMillis);
    }

    static final class MorphStyleSpec {
        private final String variation;
        private final int color;
        private final boolean animate;
        private final long durationMillis;

        MorphStyleSpec(String variation, int color, boolean animate, long durationMillis) {
            this.variation = variation;
            this.color = color;
            this.animate = animate;
            this.durationMillis = durationMillis;
        }

        String variation() {
            return variation;
        }

        int color() {
            return color;
        }

        boolean animate() {
            return animate;
        }

        long durationMillis() {
            return durationMillis;
        }

        float interpolatorX1() {
            return CouiClockPresentationModel.MOTION_X1;
        }

        float interpolatorY1() {
            return CouiClockPresentationModel.MOTION_Y1;
        }

        float interpolatorX2() {
            return CouiClockPresentationModel.MOTION_X2;
        }

        float interpolatorY2() {
            return CouiClockPresentationModel.MOTION_Y2;
        }
    }
}
