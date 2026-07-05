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
    static final float AOD_CLOCK_ALPHA = 0.96f;
    static final float LOCKSCREEN_CLOCK_ALPHA = 0.98f;
    static final float INFO_ALPHA = 0.94f;
    static final float MEDIA_ALPHA = 0.82f;

    static final int LARGE_CLOCK_TEXT_DP = 150;
    static final int LARGE_CLOCK_TOP_DP = 144;
    static final int SMALL_CLOCK_TEXT_DP = 56;
    static final int SMALL_CLOCK_TOP_DP = 74;
    static final int EDGE_DP = 34;
    static final int COMPACT_CLOCK_VISUAL_START_OFFSET_DP = 7;
    static final int LARGE_INFO_TOP_DP = 100;
    static final int SMALL_INFO_TOP_DP = 150;
    static final int NOTIFICATION_LINE_TOP_DP = 198;

    static String aodProfile(int runtimeClockWeight) {
        return commonProfile(runtimeClockWeight, Aod.INFO_WEIGHT)
                + ",notificationIcon=" + Aod.NOTIFICATION_ICON_SIZE_DP
                + ",notificationSpacing=" + Aod.NOTIFICATION_ICON_SPACING_DP
                + ",mediaIcon=" + Aod.MEDIA_ICON_SIZE_DP
                + ",mediaSpacing=" + Aod.MEDIA_ICON_SPACING_DP
                + ",weatherIcon=" + Aod.WEATHER_ICON_SIZE_DP
                + ",weatherPadding=" + Aod.WEATHER_ICON_PADDING_DP
                + ",burnInX=" + Aod.BURN_IN_OFFSET_X_DP
                + ",burnInY=" + Aod.BURN_IN_OFFSET_Y_DP
                + ",batteryTop=" + Aod.BATTERY_TOP_DP;
    }

    static String lockscreenProfile(int runtimeClockWeight) {
        return commonProfile(runtimeClockWeight, Lockscreen.INFO_WEIGHT);
    }

    private static String commonProfile(int runtimeClockWeight, int infoWeight) {
        return "clockColor=" + color(CLOCK_COLOR_RED, CLOCK_COLOR_GREEN, CLOCK_COLOR_BLUE)
                + ",infoColor=" + color(INFO_COLOR_RED, INFO_COLOR_GREEN, INFO_COLOR_BLUE)
                + ",largeText=" + LARGE_CLOCK_TEXT_DP
                + ",smallText=" + SMALL_CLOCK_TEXT_DP
                + ",largeTop=" + LARGE_CLOCK_TOP_DP
                + ",smallTop=" + SMALL_CLOCK_TOP_DP
                + ",edge=" + EDGE_DP
                + ",compactOffset=" + COMPACT_CLOCK_VISUAL_START_OFFSET_DP
                + ",largeInfoTop=" + LARGE_INFO_TOP_DP
                + ",smallInfoTop=" + SMALL_INFO_TOP_DP
                + ",notificationTop=" + NOTIFICATION_LINE_TOP_DP
                + ",lineSpacing=" + CLOCK_LINE_SPACING
                + ",largeLetterSpacing=" + LARGE_CLOCK_LETTER_SPACING
                + ",compactLetterSpacing=" + COMPACT_CLOCK_LETTER_SPACING
                + ",infoLetterSpacing=" + INFO_LETTER_SPACING
                + ",aodClockAlpha=" + AOD_CLOCK_ALPHA
                + ",lockscreenClockAlpha=" + LOCKSCREEN_CLOCK_ALPHA
                + ",infoAlpha=" + INFO_ALPHA
                + ",mediaAlpha=" + MEDIA_ALPHA
                + ",runtimeClockWeight=" + runtimeClockWeight
                + ",infoWeight=" + infoWeight;
    }

    private static String color(int red, int green, int blue) {
        return "#" + hex(red) + hex(green) + hex(blue);
    }

    private static String hex(int value) {
        String hex = Integer.toHexString(value & 0xff).toUpperCase(java.util.Locale.US);
        return hex.length() == 1 ? "0" + hex : hex;
    }

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
