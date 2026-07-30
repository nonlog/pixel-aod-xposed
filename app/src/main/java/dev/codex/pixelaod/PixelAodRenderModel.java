package dev.codex.pixelaod;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class PixelAodRenderModel {
    private static final int MAX_AT_A_GLANCE_EXTRA_LENGTH = 24;

    final Calendar calendar;
    final String clockText;
    final String dateText;
    final PixelAodClockView.WeatherSnapshot weather;
    final String batteryText;
    final boolean batteryCharging;

    private PixelAodRenderModel(Calendar calendar, String clockText, String dateText,
            PixelAodClockView.WeatherSnapshot weather, String batteryText,
            boolean batteryCharging) {
        this.calendar = calendar;
        this.clockText = clockText;
        this.dateText = dateText;
        this.weather = weather != null ? weather : PixelAodClockView.WeatherSnapshot.empty();
        this.batteryText = batteryText != null ? batteryText : "";
        this.batteryCharging = batteryCharging;
    }

    static PixelAodRenderModel forAod(Context context, boolean compactClock,
            PixelAodClockView.WeatherSnapshot weather, String batteryText,
            boolean batteryCharging) {
        Calendar calendar = Calendar.getInstance();
        return new PixelAodRenderModel(calendar, formatClockText(context, calendar, compactClock),
                formatDateWithWeather(calendar, weather),
                weather, batteryText, batteryCharging);
    }

    static PixelAodRenderModel forLockscreen(Context context, boolean compactClock,
            PixelAodClockView.WeatherSnapshot weather) {
        Calendar calendar = Calendar.getInstance();
        return new PixelAodRenderModel(calendar, formatClockText(context, calendar, compactClock),
                formatDateWithWeather(calendar, weather), weather, "", false);
    }

    static String formatDateWithWeather(Calendar calendar,
            PixelAodClockView.WeatherSnapshot weather) {
        Locale locale = Locale.getDefault();
        String pattern = locale.getLanguage().equals(Locale.CHINESE.getLanguage())
                ? "M\u6708d\u65e5 EEEE" : "EEE, MMM d";
        String date = new SimpleDateFormat(pattern, locale).format(calendar.getTime());
        if (weather != null && !TextUtils.isEmpty(weather.temperatureText)) {
            return date + " \u00b7 " + weather.temperatureText;
        }
        return date;
    }

    private static String formatClockText(Context context, Calendar calendar, boolean compactClock) {
        boolean is24Hour = context == null || DateFormat.is24HourFormat(context);
        int hour = calendar.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) {
            hour = 12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        if (compactClock) {
            return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        }
        return String.format(Locale.getDefault(), "%02d\n%02d", hour, minute);
    }

    static String normalizeAtAGlanceExtra(String extra) {
        if (extra == null) {
            return "";
        }
        String normalized = extra.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() > MAX_AT_A_GLANCE_EXTRA_LENGTH) {
            normalized = normalized.substring(0, MAX_AT_A_GLANCE_EXTRA_LENGTH).trim();
        }
        return normalized;
    }
}
