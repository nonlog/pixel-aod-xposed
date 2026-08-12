package dev.codex.pixelaod;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Locale;

final class PixelAodVisualStyle {
    private static final String PROFILE_REVISION = "7.2";

    // Clock and media use one deeper COUI-like accent rather than two near-identical tints.
    static final int CLOCK_COLOR_RED = 183;
    static final int CLOCK_COLOR_GREEN = 222;
    static final int CLOCK_COLOR_BLUE = 225;
    static final int INFO_COLOR_RED = 219;
    static final int INFO_COLOR_GREEN = 253;
    static final int INFO_COLOR_BLUE = 254;
    static final int MEDIA_EMPHASIS_COLOR_RED = 183;
    static final int MEDIA_EMPHASIS_COLOR_GREEN = 222;
    static final int MEDIA_EMPHASIS_COLOR_BLUE = 225;

    static final float CLOCK_LINE_SPACING = 0.70f;
    static final float LARGE_CLOCK_LETTER_SPACING = -0.02f;
    // COUI lays out the compact clock as five individual glyphs. Its two digit-to-digit
    // gaps are tighter than the two gaps beside the colon; these values are measured from
    // PixelClockHostView.lineTargets() in the OOS 16.0.9 COUI implementation.
    static final float COMPACT_CLOCK_DIGIT_TRACKING_EM = -0.09f;
    static final float COMPACT_CLOCK_COLON_TRACKING_EM = -0.049500003f;
    static final float COMPACT_CLOCK_LETTER_SPACING = COMPACT_CLOCK_COLON_TRACKING_EM;
    static final float INFO_LETTER_SPACING = 0.01f;
    static final float AOD_CLOCK_ALPHA = 1f;
    static final float LOCKSCREEN_CLOCK_ALPHA = 0.98f;
    static final float INFO_ALPHA = 1f;

    static final int LARGE_CLOCK_TEXT_DP = 150;
    static final int LARGE_CLOCK_TOP_DP = 184;
    static final int SMALL_CLOCK_TEXT_DP = 56;
    static final int SMALL_CLOCK_TOP_DP = 90;
    static final int SMALL_INFO_TOP_DP = 99;
    static final int EDGE_DP = 34;
    static final int COMPACT_CLOCK_VISUAL_START_OFFSET_DP = 7;
    // Pixel Small keeps time edge-anchored and starts the information column just right of the
    // screen midpoint. Actual text widths then decide whether extra clearance is required.
    static final int PIXEL_SMALL_INFO_COLUMN_OFFSET_DP = 4;
    static final int COUI_COMPACT_CLOCK_TO_INFO_GAP_DP = 16;
    static final int COUI_COMPACT_CLOCK_TO_EVENT_GAP_DP = 12;
    // Keep a readable gap between the compact date and weather, then lift the contextual row
    // away from OOS notification cards without letting it overlap the current-weather line.
    // Keep the date top, current-weather bottom, and contextual-row anchor unchanged while the
    // compact date/current-weather type moves to the requested 16 dp.
    static final int COUI_COMPACT_DATE_TO_WEATHER_TOP_OFFSET_DP = 27;
    static final int COUI_COMPACT_INFO_TO_EVENT_GAP_DP = 4;
    static final int COMPACT_CONTEXTUAL_TO_NOTIFICATION_GAP_DP = 12;
    static final int COUI_COMPACT_MEDIA_EDGE_DP = 32;
    static final int COUI_COMPACT_MEDIA_FALLBACK_HEIGHT_DP = 40;
    static final long COUI_WEIGHT_MORPH_MILLIS = 550L;
    static final int LARGE_INFO_TOP_DP = 100;
    static final int NOTIFICATION_LINE_TOP_DP = 198;
    static final int CALENDAR_DATE_TO_EVENT_TOP_OFFSET_DP = 22;
    static final int CALENDAR_DATE_TO_NOTIFICATION_TOP_OFFSET_DP = 50;
    static final int CALENDAR_DATE_TO_SECOND_EVENT_TOP_OFFSET_DP = 50;
    static final int CALENDAR_DATE_TO_NOTIFICATION_WITH_TWO_EVENTS_TOP_OFFSET_DP = 78;
    static final int COMPACT_DATE_TO_EVENT_TOP_OFFSET_DP = 34;
    static final int COMPACT_DATE_TO_NOTIFICATION_TOP_OFFSET_DP = 85;
    static final int COMPACT_DATE_TO_SECOND_EVENT_TOP_OFFSET_DP = 57;
    static final int COMPACT_DATE_TO_NOTIFICATION_WITH_TWO_EVENTS_TOP_OFFSET_DP = 96;
    // Keep Small notification anchors stable while the time/info group moves down 16 dp.
    // Row-bottom clearance still wins when localized content or contextual cards need more room.
    static final int COMPACT_DATE_TO_NOTIFICATION_WITHOUT_EVENT_TOP_OFFSET_DP = 72;
    static final int CALENDAR_ICON_SPACING_DP = 2;
    static final int CALENDAR_APPLICATION_ICON_LEADING_OFFSET_DP = 6;
    static final int NOTIFICATION_ROW_LEADING_OFFSET_DP = 2;
    static final int LARGE_INFO_ROW_GAP_DP = 6;
    static final int LARGE_INFO_TEXT_DP = 16;
    static final int COMPACT_INFO_TEXT_DP = 16;
    static final int COMPACT_AUXILIARY_INFO_TEXT_DP = 14;

    static String aodProfile(Context context, int runtimeClockWeight) {
        return commonProfile(context, runtimeClockWeight, Aod.INFO_WEIGHT)
                + ",notificationIcon=" + Aod.NOTIFICATION_ICON_SIZE_DP
                + ",notificationSpacing=" + Aod.NOTIFICATION_ICON_SPACING_DP
                + ",mediaIcon=" + Aod.MEDIA_ICON_SIZE_DP
                + ",mediaIconSpacing=" + Aod.MEDIA_ICON_SPACING_DP
                + ",mediaTitleText=" + Aod.MEDIA_TITLE_TEXT_DP
                + ",mediaArtistText=" + Aod.MEDIA_ARTIST_TEXT_DP
                + ",mediaSubtitleGap=" + Aod.MEDIA_SUBTITLE_TOP_GAP_DP
                + ",weatherIcon=" + Aod.WEATHER_ICON_SIZE_DP
                + ",weatherPadding=" + Aod.WEATHER_ICON_PADDING_DP
                + ",batteryText=" + Aod.BATTERY_TEXT_DP
                + ",burnInX=" + Aod.BURN_IN_OFFSET_X_DP
                + ",burnInY=" + Aod.BURN_IN_OFFSET_Y_DP
                + ",batteryTop=" + Aod.BATTERY_TOP_DP
                + ",chargeBolt=" + Aod.CHARGE_BOLT_WIDTH_DP + "x" + Aod.CHARGE_BOLT_HEIGHT_DP;
    }

    static String lockscreenProfile(Context context, int runtimeClockWeight) {
        return commonProfile(context, runtimeClockWeight, Lockscreen.INFO_WEIGHT);
    }

    private static String commonProfile(Context context, int runtimeClockWeight, int infoWeight) {
        return "profileRevision=" + PROFILE_REVISION
                + runtimeDisplayProfile(context)
                + ",clockColor=" + color(CLOCK_COLOR_RED, CLOCK_COLOR_GREEN, CLOCK_COLOR_BLUE)
                + ",infoColor=" + color(INFO_COLOR_RED, INFO_COLOR_GREEN, INFO_COLOR_BLUE)
                + ",mediaColor=" + color(MEDIA_EMPHASIS_COLOR_RED,
                MEDIA_EMPHASIS_COLOR_GREEN, MEDIA_EMPHASIS_COLOR_BLUE)
                + ",largeText=" + LARGE_CLOCK_TEXT_DP
                + ",smallText=" + SMALL_CLOCK_TEXT_DP
                + ",largeTop=" + LARGE_CLOCK_TOP_DP
                + ",smallTop=" + SMALL_CLOCK_TOP_DP
                + ",edge=" + EDGE_DP
                + ",compactOffset=" + COMPACT_CLOCK_VISUAL_START_OFFSET_DP
                + ",pixelSmallClockLeft=" + (EDGE_DP - COMPACT_CLOCK_VISUAL_START_OFFSET_DP)
                + ",pixelSmallInfoColumnOffset=" + PIXEL_SMALL_INFO_COLUMN_OFFSET_DP
                + ",largeInfoTop=" + LARGE_INFO_TOP_DP
                + ",smallInfoTop=" + SMALL_INFO_TOP_DP
                + ",notificationTop=" + NOTIFICATION_LINE_TOP_DP
                + ",calendarDateToEvent=" + CALENDAR_DATE_TO_EVENT_TOP_OFFSET_DP
                + ",calendarDateToNotification="
                + CALENDAR_DATE_TO_NOTIFICATION_TOP_OFFSET_DP
                + ",largeInfoText=" + LARGE_INFO_TEXT_DP
                + ",compactInfoText=" + COMPACT_INFO_TEXT_DP
                + ",compactAuxiliaryInfoText=" + COMPACT_AUXILIARY_INFO_TEXT_DP
                + ",lineSpacing=" + CLOCK_LINE_SPACING
                + ",largeLetterSpacing=" + LARGE_CLOCK_LETTER_SPACING
                + ",compactLetterSpacing=" + COMPACT_CLOCK_LETTER_SPACING
                + ",infoLetterSpacing=" + INFO_LETTER_SPACING
                + ",aodClockAlpha=" + AOD_CLOCK_ALPHA
                + ",lockscreenClockAlpha=" + LOCKSCREEN_CLOCK_ALPHA
                + ",infoAlpha=" + INFO_ALPHA
                + ",runtimeClockWeight=" + runtimeClockWeight
                + ",infoWeight=" + infoWeight;
    }

    private static String runtimeDisplayProfile(Context context) {
        if (context == null) {
            return ",densityDpi=unknown,density=unknown,scaledDensity=unknown,fontScale=unknown,"
                    + "widthDp=unknown,heightDp=unknown,smallestDp=unknown";
        }
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        return ",densityDpi=" + metrics.densityDpi
                + ",density=" + decimal(metrics.density)
                + ",scaledDensity=" + decimal(metrics.scaledDensity)
                + ",fontScale=" + decimal(config.fontScale)
                + ",widthDp=" + config.screenWidthDp
                + ",heightDp=" + config.screenHeightDp
                + ",smallestDp=" + config.smallestScreenWidthDp;
    }

    private static String color(int red, int green, int blue) {
        return "#" + hex(red) + hex(green) + hex(blue);
    }

    private static String hex(int value) {
        String hex = Integer.toHexString(value & 0xff).toUpperCase(java.util.Locale.US);
        return hex.length() == 1 ? "0" + hex : hex;
    }

    private static String decimal(float value) {
        return String.format(Locale.US, "%.2f", value);
    }

    static final class Aod {
        static final int LARGE_MEDIA_TOP_DP = 132;
        static final int LARGE_MEDIA_WITH_NOTIFICATIONS_TOP_DP = 224;
        static final int SMALL_MEDIA_TOP_DP = 218;
        static final int NOTIFICATION_ICON_SIZE_DP = 14;
        static final int NOTIFICATION_ICON_SPACING_DP = 8;
        static final int MEDIA_TITLE_TEXT_DP = 18;
        static final int MEDIA_TITLE_WEIGHT = 500;
        static final int MEDIA_ARTIST_TEXT_DP = 15;
        static final int MEDIA_ICON_SIZE_DP = 18;
        static final int MEDIA_ICON_SPACING_DP = 6;
        static final int MEDIA_SUBTITLE_TOP_GAP_DP = 4;
        static final int BATTERY_TOP_DP = 720;
        static final int BATTERY_TEXT_DP = 16;
        static final int CHARGE_BOLT_WIDTH_DP = 9;
        static final int CHARGE_BOLT_HEIGHT_DP = 13;
        static final int CLOCK_WEIGHT = 280;
        /** Slightly lighter than lockscreen information text, matching the AOD clock weight. */
        static final int INFO_WEIGHT = 450;
        /** Compensated output range for the optically smaller date and weather text. */
        static final int DATE_WEATHER_MIN_WEIGHT = 400;
        static final int DATE_WEATHER_MAX_WEIGHT = 500;
        /** Forecast auxiliary text needs extra optical weight on the dim AOD surface. */
        static final int WEATHER_FORECAST_WEIGHT_COMPENSATION = 36;
        static final int WEATHER_ICON_SIZE_DP = 15;
        static final int WEATHER_ICON_PADDING_DP = 6;
        static final int BURN_IN_OFFSET_X_DP = 8;
        static final int BURN_IN_OFFSET_Y_DP = 12;

        private Aod() {
        }
    }

    static final class Lockscreen {
        static final int CLOCK_WEIGHT = 520;
        /** The handoff eases this into AOD's lighter information weight. */
        static final int INFO_WEIGHT = 500;

        private Lockscreen() {
        }
    }

    private PixelAodVisualStyle() {
    }
}
