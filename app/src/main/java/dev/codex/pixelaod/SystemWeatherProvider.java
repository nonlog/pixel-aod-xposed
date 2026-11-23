package dev.codex.pixelaod;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Reads ColorOS / OPlus System Weather data from the native weather ContentProvider
 * and arbitrates between System Weather and Breezy Weather according to user settings.
 */
final class SystemWeatherProvider {
    private static final String URI_WEATHER_DATA = "content://com.oplusos.weather.service.provider.data";
    private static final String URI_INTRADAY_WEATHER = "content://com.oplusos.weather.service.provider.data/intraday_weather_info";

    private static final long QUERY_INTERVAL_MILLIS = 120_000L;
    private static final Object LOCK = new Object();

    private static PixelAodClockView.WeatherSnapshot cachedSnapshot;
    private static long lastQueryAttemptMillis;
    private static boolean queryInFlight;

    private static final String[] TEMP_COLUMNS = {
            "temperature", "current_temp", "temp", "real_feel_temp",
            "current_temperature", "now_temp", "curr_temp"
    };
    private static final String[] CODE_COLUMNS = {
            "weather_code", "weather_phenomena", "weather_id",
            "weather_type", "condition_code", "icon"
    };
    private static final String[] CONDITION_COLUMNS = {
            "weather_description", "weather_text", "weather_name",
            "condition", "weather"
    };
    private static final String[] SUNRISE_COLUMNS = {"sun_rise", "sunrise", "sunrise_time"};
    private static final String[] SUNSET_COLUMNS = {"sun_set", "sunset", "sunset_time"};

    private static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; }

    private SystemWeatherProvider() {
    }

    static final int SOURCE_NONE = 0;
    static final int SOURCE_BREEZY = 1;
    static final int SOURCE_SYSTEM = 2;

    static int selectWeatherSource(String sourceMode, boolean breezyFresh, boolean systemFresh) {
        String normalized = normalizeSourceMode(sourceMode);
        if (PixelAodSettingsSchema.WEATHER_DATA_SOURCE_SYSTEM.equals(normalized)) {
            return systemFresh ? SOURCE_SYSTEM : SOURCE_NONE;
        }
        if (PixelAodSettingsSchema.WEATHER_DATA_SOURCE_BREEZY.equals(normalized)) {
            return breezyFresh ? SOURCE_BREEZY : SOURCE_NONE;
        }
        if (breezyFresh) {
            return SOURCE_BREEZY;
        }
        if (systemFresh) {
            return SOURCE_SYSTEM;
        }
        return SOURCE_NONE;
    }

    static PixelAodClockView.WeatherSnapshot selectWeatherSnapshot(
            String sourceMode,
            PixelAodClockView.WeatherSnapshot breezy,
            PixelAodClockView.WeatherSnapshot system,
            long nowMillis) {
        boolean breezyFresh = breezy != null && breezy.isFresh(nowMillis);
        boolean systemFresh = system != null && system.isFresh(nowMillis);
        int selected = selectWeatherSource(sourceMode, breezyFresh, systemFresh);
        if (selected == SOURCE_BREEZY) {
            return breezy;
        }
        if (selected == SOURCE_SYSTEM) {
            return system;
        }
        return PixelAodClockView.WeatherSnapshot.empty();
    }

    static String normalizeSourceMode(String sourceMode) {
        if (PixelAodSettingsSchema.WEATHER_DATA_SOURCE_SYSTEM.equals(sourceMode)) {
            return PixelAodSettingsSchema.WEATHER_DATA_SOURCE_SYSTEM;
        }
        if (PixelAodSettingsSchema.WEATHER_DATA_SOURCE_BREEZY.equals(sourceMode)) {
            return PixelAodSettingsSchema.WEATHER_DATA_SOURCE_BREEZY;
        }
        return PixelAodSettingsSchema.WEATHER_DATA_SOURCE_AUTO;
    }

    static PixelAodClockView.WeatherSnapshot getCachedOrRefresh(
            Context context, Runnable onUpdated) {
        long now = System.currentTimeMillis();
        PixelAodClockView.WeatherSnapshot current;
        boolean shouldQuery = false;
        synchronized (LOCK) {
            current = (cachedSnapshot != null && cachedSnapshot.isFresh(now))
                    ? cachedSnapshot
                    : PixelAodClockView.WeatherSnapshot.empty();
            if (!queryInFlight && (now - lastQueryAttemptMillis >= QUERY_INTERVAL_MILLIS
                    || (!current.hasDisplayableWeather() && now - lastQueryAttemptMillis >= 15_000L))) {
                queryInFlight = true;
                lastQueryAttemptMillis = now;
                shouldQuery = true;
            }
        }
        if (shouldQuery && context != null) {
            final Context appCtx = context.getApplicationContext() != null
                    ? context.getApplicationContext()
                    : context;
            Thread worker = new Thread(() -> {
                PixelAodClockView.WeatherSnapshot fetched = querySystemWeather(appCtx);
                boolean changed = false;
                synchronized (LOCK) {
                    queryInFlight = false;
                    if (fetched != null && fetched.hasDisplayableWeather()) {
                        changed = cachedSnapshot == null || !cachedSnapshot.sameDisplay(fetched);
                        cachedSnapshot = fetched;
                    }
                }
                if (changed && onUpdated != null) {
                    onUpdated.run();
                }
            }, "PixelAodSystemWeather");
            worker.setDaemon(true);
            worker.start();
        }
        return current;
    }

    static PixelAodClockView.WeatherSnapshot querySystemWeather(Context context) {
        if (context == null) {
            return PixelAodClockView.WeatherSnapshot.empty();
        }
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            return PixelAodClockView.WeatherSnapshot.empty();
        }
        PixelAodClockView.WeatherSnapshot snapshot = queryUri(resolver, Uri.parse(URI_WEATHER_DATA), "location=?", new String[]{"1"});
        if (snapshot != null && snapshot.hasDisplayableWeather()) {
            return snapshot;
        }
        snapshot = queryUri(resolver, Uri.parse(URI_WEATHER_DATA), null, null);
        if (snapshot != null && snapshot.hasDisplayableWeather()) {
            return snapshot;
        }
        snapshot = queryUri(resolver, Uri.parse(URI_INTRADAY_WEATHER), null, null);
        if (snapshot != null && snapshot.hasDisplayableWeather()) {
            return snapshot;
        }
        return PixelAodClockView.WeatherSnapshot.empty();
    }

    private static PixelAodClockView.WeatherSnapshot queryUri(
            ContentResolver resolver, Uri uri, String selection, String[] selectionArgs) {
        try (Cursor cursor = resolver.query(uri, null, selection, selectionArgs, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            String tempRaw = firstColumnString(cursor, TEMP_COLUMNS);
            String codeRaw = firstColumnString(cursor, CODE_COLUMNS);
            String condition = firstColumnString(cursor, CONDITION_COLUMNS);
            long sunrise = firstColumnMillis(cursor, SUNRISE_COLUMNS);
            long sunset = firstColumnMillis(cursor, SUNSET_COLUMNS);

            String tempText = formatSystemTemperature(tempRaw);
            int parsedCode = parseIntOrMin(codeRaw);
            int owmCode = mapOplusWeatherCodeToOwm(parsedCode, condition);
            if (isEmpty(tempText) && owmCode == Integer.MIN_VALUE && isEmpty(condition)) {
                return null;
            }
            return new PixelAodClockView.WeatherSnapshot(
                    tempText,
                    owmCode,
                    condition,
                    System.currentTimeMillis(),
                    sunrise,
                    sunset);
        } catch (Throwable t) {
            PixelAodLog.log("system weather query failed for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    static String formatSystemTemperature(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return "";
        }
        String cleaned = trimmed.replace("℃", "").replace("°C", "").replace("°F", "").replace("°", "").trim();
        try {
            double val = Double.parseDouble(cleaned);
            if (Double.isNaN(val) || Double.isInfinite(val) || val < -100.0 || val > 150.0) {
                return "";
            }
            return Math.round(val) + "°";
        } catch (NumberFormatException ignored) {
            return trimmed.contains("°") ? trimmed : "";
        }
    }

    static int mapOplusWeatherCodeToOwm(int rawCode, String conditionText) {
        if (rawCode >= 200 && rawCode <= 804) {
            return rawCode;
        }
        switch (rawCode) {
            case 0:
                return 800;
            case 1:
                return 801;
            case 2:
                return 804;
            case 3:
                return 520;
            case 4:
            case 5:
                return 211;
            case 6:
                return 611;
            case 7:
            case 21:
            case 301:
                return 500;
            case 8:
            case 22:
                return 501;
            case 9:
            case 10:
            case 11:
            case 12:
            case 23:
            case 24:
            case 25:
                return 502;
            case 13:
            case 14:
            case 26:
            case 302:
                return 600;
            case 15:
            case 16:
            case 17:
            case 27:
            case 28:
                return 601;
            case 18:
            case 32:
            case 49:
            case 57:
            case 58:
                return 741;
            case 19:
                return 511;
            case 20:
            case 29:
            case 30:
            case 31:
                return 771;
            case 53:
            case 54:
            case 55:
            case 56:
                return 721;
            default:
                break;
        }
        if (conditionText != null && !conditionText.trim().isEmpty()) {
            String lower = conditionText.toLowerCase(Locale.ROOT);
            if (lower.contains("雷") || lower.contains("thunder")) {
                return 211;
            }
            if (lower.contains("雨夹雪") || lower.contains("冰雹") || lower.contains("冻雨")
                    || lower.contains("sleet") || lower.contains("hail")) {
                return 611;
            }
            if (lower.contains("雪") || lower.contains("snow")) {
                return 600;
            }
            if (lower.contains("雨") || lower.contains("rain") || lower.contains("shower") || lower.contains("drizzle")) {
                return 500;
            }
            if (lower.contains("霾") || lower.contains("haze")) {
                return 721;
            }
            if (lower.contains("雾") || lower.contains("fog") || lower.contains("mist")) {
                return 741;
            }
            if (lower.contains("风") || lower.contains("沙") || lower.contains("尘")
                    || lower.contains("wind") || lower.contains("dust")) {
                return 771;
            }
            if (lower.contains("阴") || lower.contains("overcast")) {
                return 804;
            }
            if (lower.contains("云") || lower.contains("cloud")) {
                return 801;
            }
            if (lower.contains("晴") || lower.contains("clear") || lower.contains("sunny")) {
                return 800;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static String firstColumnString(Cursor cursor, String[] names) {
        for (String name : names) {
            int idx = cursor.getColumnIndex(name);
            if (idx >= 0 && !cursor.isNull(idx)) {
                String value = cursor.getString(idx);
                if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private static long firstColumnMillis(Cursor cursor, String[] names) {
        for (String name : names) {
            int idx = cursor.getColumnIndex(name);
            if (idx >= 0 && !cursor.isNull(idx)) {
                try {
                    long val = cursor.getLong(idx);
                    if (val > 0L) {
                        return val < 10_000_000_000L ? val * 1000L : val;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return 0L;
    }

    private static int parseIntOrMin(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }
}
