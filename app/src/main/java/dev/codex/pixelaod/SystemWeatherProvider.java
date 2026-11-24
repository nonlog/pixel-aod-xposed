package dev.codex.pixelaod;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads ColorOS / OPlus / OnePlus System Weather data across both CN and Global ROMs:
 * 1) OnePlus / OPlus Weather2 /data ContentProviders (Global opexp + CN provider.data)
 * 2) ColorOS Weather Service weather_info/-10000, attent_city -> weather_info/<id>, and intraday_weather_info
 * 3) Stock OPlus lockscreen clock ExtraMsgView mirrored weather text (coui-ref fallback)
 */
final class SystemWeatherProvider {
    private static final String[] OP_DATA_URIS = {
            "content://com.oplus.weather.provider.opexp.data/data",
            "content://com.oplus.weather.provider.data/data",
            "content://com.coloros.weather.provider.data/data",
            "content://com.oneplus.weather.ContentProvider/data"
    };

    private static final String[] WEATHER_SERVICE_AUTHORITIES = {
            "com.oplusos.weather.service.provider.data",
            "com.oplus.weather.service.provider.data",
            "com.coloros.weather.service.provider.data"
    };

    private static final long QUERY_INTERVAL_MILLIS = 120_000L;
    private static final Object LOCK = new Object();

    private static PixelAodClockView.WeatherSnapshot cachedSnapshot;
    private static PixelAodClockView.WeatherSnapshot mirroredSnapshot;
    private static long lastQueryAttemptMillis;
    private static boolean queryInFlight;

    private static final String[] TEMP_COLUMNS = {
            "current_temp", "temp", "temperature", "temprature",
            "real_feel_temp", "current_temperature", "now_temp", "curr_temp"
    };
    private static final String[] CODE_COLUMNS = {
            "weather_id", "type", "weather_code", "weather_phenomena",
            "weather_type", "condition_code", "icon"
    };
    private static final String[] CONDITION_COLUMNS = {
            "current_weather", "info", "weather_description",
            "weather_text", "weather_name", "condition", "weather", "day_weather"
    };
    private static final String[] SUNRISE_COLUMNS = {"sunrise_time", "sun_rise", "sunrise"};
    private static final String[] SUNSET_COLUMNS = {"sunset_time", "sun_set", "sunset"};
    private static final String[] IS_DAY_COLUMNS = {"is_day", "isDay"};

    private static final Pattern MIRRORED_TEMP_PATTERN =
            Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*(?:℃|°C|°F|℉|°)");

    private static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

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

    static void updateMirroredLockscreenWeather(CharSequence text) {
        PixelAodClockView.WeatherSnapshot parsed =
                parseMirroredLockscreenWeather(text, System.currentTimeMillis());
        if (parsed == null || !parsed.hasDisplayableWeather()) {
            return;
        }
        synchronized (LOCK) {
            mirroredSnapshot = parsed;
        }
    }

    static String[] parseMirroredLockscreenWeatherParts(CharSequence text) {
        if (text == null) {
            return null;
        }
        String raw = text.toString().trim();
        if (raw.isEmpty()) {
            return null;
        }
        Matcher matcher = MIRRORED_TEMP_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        String tempText = formatSystemTemperature(matcher.group(1));
        if (isEmpty(tempText)) {
            return null;
        }
        String condition = (raw.substring(0, matcher.start()) + " " + raw.substring(matcher.end()))
                .replaceAll("[·•|/\\-,，、]+", " ")
                .trim();
        int owmCode = mapConditionTextToOwm(condition);
        return new String[]{tempText, condition, String.valueOf(owmCode)};
    }

    static PixelAodClockView.WeatherSnapshot parseMirroredLockscreenWeather(
            CharSequence text, long nowMillis) {
        String[] parts = parseMirroredLockscreenWeatherParts(text);
        if (parts == null) {
            return null;
        }
        int owmCode = parseIntOrMin(parts[2]);
        return new PixelAodClockView.WeatherSnapshot(parts[0], owmCode, parts[1], nowMillis, 0L, 0L);
    }

    static PixelAodClockView.WeatherSnapshot getCachedOrRefresh(
            Context context, Runnable onUpdated) {
        long now = System.currentTimeMillis();
        PixelAodClockView.WeatherSnapshot current;
        boolean shouldQuery = false;
        synchronized (LOCK) {
            if (cachedSnapshot != null && cachedSnapshot.isFresh(now)) {
                current = cachedSnapshot;
            } else if (mirroredSnapshot != null && mirroredSnapshot.isFresh(now)) {
                current = mirroredSnapshot;
            } else {
                current = PixelAodClockView.WeatherSnapshot.empty();
            }
            boolean hasProviderCache = cachedSnapshot != null && cachedSnapshot.isFresh(now);
            if (!queryInFlight && (now - lastQueryAttemptMillis >= QUERY_INTERVAL_MILLIS
                    || (!hasProviderCache && now - lastQueryAttemptMillis >= 15_000L))) {
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
            return fallbackMirroredSnapshot();
        }
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            return fallbackMirroredSnapshot();
        }
        for (String uriStr : OP_DATA_URIS) {
            PixelAodClockView.WeatherSnapshot snapshot =
                    queryOpDataUri(resolver, Uri.parse(uriStr));
            if (snapshot != null && snapshot.hasDisplayableWeather()) {
                return snapshot;
            }
        }
        for (String authority : WEATHER_SERVICE_AUTHORITIES) {
            PixelAodClockView.WeatherSnapshot snapshot =
                    queryWeatherServiceAuthority(resolver, authority);
            if (snapshot != null && snapshot.hasDisplayableWeather()) {
                return snapshot;
            }
        }
        return fallbackMirroredSnapshot();
    }

    private static PixelAodClockView.WeatherSnapshot fallbackMirroredSnapshot() {
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            if (mirroredSnapshot != null && mirroredSnapshot.isFresh(now)) {
                return mirroredSnapshot;
            }
        }
        return PixelAodClockView.WeatherSnapshot.empty();
    }

    private static PixelAodClockView.WeatherSnapshot queryWeatherServiceAuthority(
            ContentResolver resolver, String authority) {
        Uri currentLocUri = Uri.parse("content://" + authority + "/weather_info/-10000");
        PixelAodClockView.WeatherSnapshot snapshot = queryWeatherInfoUri(
                resolver, currentLocUri, "weather_index=?", new String[]{"0"});
        if (snapshot != null && snapshot.hasDisplayableWeather()) {
            return snapshot;
        }
        snapshot = queryWeatherInfoUri(resolver, currentLocUri, null, null);
        if (snapshot != null && snapshot.hasDisplayableWeather()) {
            return snapshot;
        }
        long activeCityRowId = queryActiveAttendCityRowId(
                resolver, Uri.parse("content://" + authority + "/attent_city"));
        if (activeCityRowId > 0L) {
            Uri cityWeatherUri = Uri.parse("content://" + authority + "/weather_info/" + activeCityRowId);
            snapshot = queryWeatherInfoUri(
                    resolver, cityWeatherUri, "weather_index=?", new String[]{"0"});
            if (snapshot != null && snapshot.hasDisplayableWeather()) {
                return snapshot;
            }
            snapshot = queryWeatherInfoUri(resolver, cityWeatherUri, null, null);
            if (snapshot != null && snapshot.hasDisplayableWeather()) {
                return snapshot;
            }
        }
        Uri intradayUri = Uri.parse("content://" + authority + "/intraday_weather_info");
        snapshot = queryWeatherInfoUri(resolver, intradayUri, null, null);
        if (snapshot != null && snapshot.hasDisplayableWeather()) {
            return snapshot;
        }
        return null;
    }

    private static long queryActiveAttendCityRowId(ContentResolver resolver, Uri attendCityUri) {
        try (Cursor cursor = resolver.query(attendCityUri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return -1L;
            }
            int idIdx = cursor.getColumnIndex("_id");
            int locIdx = cursor.getColumnIndex("location");
            int curIdx = cursor.getColumnIndex("current");
            long firstId = -1L;
            do {
                long rowId = idIdx >= 0 && !cursor.isNull(idIdx) ? cursor.getLong(idIdx) : -1L;
                if (rowId <= 0L) {
                    continue;
                }
                if (firstId <= 0L) {
                    firstId = rowId;
                }
                boolean isLoc = locIdx >= 0 && !cursor.isNull(locIdx) && cursor.getInt(locIdx) == 1;
                boolean isCur = curIdx >= 0 && !cursor.isNull(curIdx) && cursor.getInt(curIdx) == 1;
                if (isLoc || isCur) {
                    return rowId;
                }
            } while (cursor.moveToNext());
            return firstId;
        } catch (Throwable t) {
            PixelAodLog.log("system weather attent_city query failed for " + attendCityUri + ": " + t.getMessage());
            return -1L;
        }
    }

    private static PixelAodClockView.WeatherSnapshot queryOpDataUri(
            ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
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
            int owmCode = mapOnePlusOpWeatherCodeToOwm(parsedCode, condition);
            if (isEmpty(tempText) && owmCode == Integer.MIN_VALUE && isEmpty(condition)) {
                return null;
            }
            if (sunrise <= 0L || sunset <= 0L) {
                String isDayRaw = firstColumnString(cursor, IS_DAY_COLUMNS);
                if ("true".equalsIgnoreCase(isDayRaw) || "1".equals(isDayRaw)) {
                    long[] synthetic = syntheticSunTimesForDayState(true, System.currentTimeMillis());
                    sunrise = synthetic[0];
                    sunset = synthetic[1];
                } else if ("false".equalsIgnoreCase(isDayRaw) || "0".equals(isDayRaw)) {
                    long[] synthetic = syntheticSunTimesForDayState(false, System.currentTimeMillis());
                    sunrise = synthetic[0];
                    sunset = synthetic[1];
                }
            }
            return new PixelAodClockView.WeatherSnapshot(
                    tempText, owmCode, condition, System.currentTimeMillis(), sunrise, sunset);
        } catch (Throwable t) {
            PixelAodLog.log("system weather op-data query failed for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    private static PixelAodClockView.WeatherSnapshot queryWeatherInfoUri(
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
            int owmCode = mapOplusWeatherInfoCodeToOwm(parsedCode, condition);
            if (isEmpty(tempText) && owmCode == Integer.MIN_VALUE && isEmpty(condition)) {
                return null;
            }
            return new PixelAodClockView.WeatherSnapshot(
                    tempText, owmCode, condition, System.currentTimeMillis(), sunrise, sunset);
        } catch (Throwable t) {
            PixelAodLog.log("system weather query failed for " + uri + ": " + t.getMessage());
            return null;
        }
    }

    static long[] syntheticSunTimesForDayState(boolean isDay, long nowMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMillis);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (isDay) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            long sunrise = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            long sunset = cal.getTimeInMillis();
            return new long[]{sunrise, sunset};
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 58);
            long sunrise = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            long sunset = cal.getTimeInMillis();
            return new long[]{sunrise, sunset};
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
        String cleaned = trimmed.replace("℃", "").replace("℉", "")
                .replace("°C", "").replace("°F", "").replace("°", "").trim();
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

    static int mapOnePlusOpWeatherCodeToOwm(int rawCode, String conditionText) {
        int fromText = mapConditionTextToOwm(conditionText);
        if (fromText != Integer.MIN_VALUE) {
            return fromText;
        }
        if (rawCode >= 200 && rawCode <= 804) {
            return rawCode;
        }
        switch (rawCode) {
            case 1001: return 800;
            case 1002: return 801;
            case 1003: return 802;
            case 1004: return 804;
            case 1005:
            case 1006: return 500;
            case 1007: return 501;
            case 1008: return 502;
            case 1009: return 211;
            case 1010: return 600;
            case 1011: return 611;
            case 1012: return 741;
            case 1013: return 721;
            case 1014: return 771;
            default: return mapOplusWeatherInfoCodeToOwm(rawCode, conditionText);
        }
    }

    static int mapOplusWeatherInfoCodeToOwm(int rawCode, String conditionText) {
        int fromText = mapConditionTextToOwm(conditionText);
        if (fromText != Integer.MIN_VALUE) {
            return fromText;
        }
        if (rawCode >= 200 && rawCode <= 804) {
            return rawCode;
        }
        switch (rawCode) {
            case 1: return 800;
            case 2: return 801;
            case 3: return 804;
            case 4: return 520;
            case 5: return 211;
            case 6:
            case 7: return 611;
            case 8:
            case 25: return 500;
            case 9:
            case 26: return 501;
            case 10: case 11: case 12: case 13: case 27: case 28: case 29: return 502;
            case 14: case 15: case 30: case 34: return 600;
            case 16: case 17: case 18: case 31: case 32: return 601;
            case 19: case 33: case 35: case 40: return 741;
            case 20: return 511;
            case 21: case 22: case 23: case 24: return 771;
            case 36: case 37: case 38: case 39: return 721;
            default: return mapOplusWeatherCodeToOwm(rawCode, conditionText);
        }
    }

    static int mapOplusWeatherCodeToOwm(int rawCode, String conditionText) {
        if (rawCode >= 200 && rawCode <= 804) {
            return rawCode;
        }
        switch (rawCode) {
            case 0: return 800;
            case 1: return 801;
            case 2: return 804;
            case 3: return 520;
            case 4: case 5: return 211;
            case 6: return 611;
            case 7: case 21: case 301: return 500;
            case 8: case 22: return 501;
            case 9: case 10: case 11: case 12: case 23: case 24: case 25: return 502;
            case 13: case 14: case 26: case 302: return 600;
            case 15: case 16: case 17: case 27: case 28: return 601;
            case 18: case 32: case 49: case 57: case 58: return 741;
            case 19: return 511;
            case 20: case 29: case 30: case 31: return 771;
            case 53: case 54: case 55: case 56: return 721;
            default: break;
        }
        return mapConditionTextToOwm(conditionText);
    }

    static int mapConditionTextToOwm(String conditionText) {
        if (conditionText == null || conditionText.trim().isEmpty()) {
            return Integer.MIN_VALUE;
        }
        String lower = conditionText.toLowerCase(Locale.ROOT);
        if (lower.contains("雷") || lower.contains("thunder")) return 211;
        if (lower.contains("雨夹雪") || lower.contains("冰雹") || lower.contains("冻雨")
                || lower.contains("sleet") || lower.contains("hail") || lower.contains("freezing")) return 611;
        if (lower.contains("雪") || lower.contains("snow")) return 600;
        if (lower.contains("雨") || lower.contains("rain") || lower.contains("shower") || lower.contains("drizzle")) return 500;
        if (lower.contains("霾") || lower.contains("haze")) return 721;
        if (lower.contains("雾") || lower.contains("fog") || lower.contains("mist")) return 741;
        if (lower.contains("风") || lower.contains("沙") || lower.contains("尘")
                || lower.contains("wind") || lower.contains("dust") || lower.contains("sand")) return 771;
        if (lower.contains("阴") || lower.contains("overcast")) return 804;
        if (lower.contains("云") || lower.contains("cloud")) return 801;
        if (lower.contains("晴") || lower.contains("clear") || lower.contains("sunny")) return 800;
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
