package dev.codex.pixelaod;

final class PixelAodVisualStyle {
    static final int CLOCK_COLOR_RED = 232;
    static final int CLOCK_COLOR_GREEN = 234;
    static final int CLOCK_COLOR_BLUE = 237;
    static final int INFO_COLOR_RED = 218;
    static final int INFO_COLOR_GREEN = 220;
    static final int INFO_COLOR_BLUE = 224;

    static final float CLOCK_LINE_SPACING = 0.70f;
    static final float LARGE_CLOCK_LETTER_SPACING = -0.02f;
    static final float COMPACT_CLOCK_LETTER_SPACING = -0.025f;
    static final float INFO_LETTER_SPACING = 0.01f;

    static final int LARGE_CLOCK_TEXT_DP = 150;
    static final int LARGE_CLOCK_TOP_DP = 144;
    static final int SMALL_CLOCK_TEXT_DP = 56;
    static final int SMALL_CLOCK_TOP_DP = 74;
    static final int EDGE_DP = 34;
    static final int COMPACT_CLOCK_VISUAL_START_OFFSET_DP = 7;
    static final int LARGE_INFO_TOP_DP = 100;
    static final int SMALL_INFO_TOP_DP = 150;
    static final int NOTIFICATION_LINE_TOP_DP = 198;

    static final class Aod {
        static final int LARGE_MEDIA_TOP_DP = 132;
        static final int LARGE_MEDIA_WITH_NOTIFICATIONS_TOP_DP = 224;
        static final int SMALL_MEDIA_TOP_DP = 234;
        static final int NOTIFICATION_ICON_SIZE_DP = 14;
        static final int NOTIFICATION_ICON_SPACING_DP = 8;
        static final int MEDIA_ICON_SIZE_DP = 13;
        static final int MEDIA_ICON_SPACING_DP = 8;
        static final int BATTERY_TOP_DP = 720;
        static final int CLOCK_WEIGHT = 280;
        static final int INFO_WEIGHT = 500;
        static final int WEATHER_ICON_SIZE_DP = 15;
        static final int WEATHER_ICON_PADDING_DP = 6;
        static final int BURN_IN_OFFSET_X_DP = 8;
        static final int BURN_IN_OFFSET_Y_DP = 12;

        private Aod() {
        }
    }

    static final class Lockscreen {
        static final int CLOCK_WEIGHT = 520;
        static final int INFO_WEIGHT = 500;

        private Lockscreen() {
        }
    }

    private PixelAodVisualStyle() {
    }
}
