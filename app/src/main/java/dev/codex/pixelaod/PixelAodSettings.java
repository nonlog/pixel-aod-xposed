package dev.codex.pixelaod;

import android.content.Context;
import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

public final class PixelAodSettings {
    public static final String PREFS = "pixel_aod_settings";
    public static final String KEY_CUSTOM_AOD = "custom_aod";
    public static final String KEY_LOCKSCREEN_CLOCK = "lockscreen_clock";
    public static final String KEY_WEATHER = "weather";
    public static final String KEY_NOTIFICATION_ICONS = "notification_icons";
    public static final String KEY_LOCKSCREEN_NOTIFICATION_POLICY = "lockscreen_notification_policy";
    public static final String KEY_DEBUG_LOGGING = "debug_logging";
    public static final String KEY_CLOCK_SCALE = "clock_scale";
    public static final String KEY_AOD_WEIGHT = "aod_weight";
    public static final String KEY_LOCKSCREEN_WEIGHT = "lockscreen_weight";
    public static final float DEFAULT_CLOCK_SCALE = 1.0f;
    public static final float DEFAULT_AOD_WEIGHT = 280f;
    public static final float DEFAULT_LOCKSCREEN_WEIGHT = 520f;
    private static final Map<String, String> CACHE = new HashMap<>();
    private static long lastLoadMillis;
    private static final long CACHE_TTL_MILLIS = 2_000L;

    private PixelAodSettings() {
    }

    public static boolean getBoolean(Context context, String key, boolean fallback) {
        String value = getValue(context, key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public static float getFloat(Context context, String key, float fallback) {
        String value = getValue(context, key);
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int getIntFromFloat(Context context, String key, float fallback,
            int min, int max) {
        int value = Math.round(getFloat(context, key, fallback));
        return Math.max(min, Math.min(max, value));
    }

    public static void refresh(Context context) {
        load(context, true);
    }

    private static String getValue(Context context, String key) {
        load(context, false);
        synchronized (CACHE) {
            return CACHE.get(key);
        }
    }

    private static void load(Context context, boolean force) {
        if (context == null) {
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        synchronized (CACHE) {
            if (!force && now - lastLoadMillis < CACHE_TTL_MILLIS) {
                return;
            }
            lastLoadMillis = now;
        }
        HashMap<String, String> values = new HashMap<>();
        try (Cursor cursor = context.getContentResolver().query(
                PixelAodSettingsProvider.URI, null, null, null, null)) {
            if (cursor == null) {
                return;
            }
            int keyIndex = cursor.getColumnIndex("key");
            int valueIndex = cursor.getColumnIndex("value");
            while (cursor.moveToNext()) {
                if (keyIndex >= 0 && valueIndex >= 0) {
                    values.put(cursor.getString(keyIndex), cursor.getString(valueIndex));
                }
            }
        } catch (Throwable ignored) {
            return;
        }
        synchronized (CACHE) {
            CACHE.clear();
            CACHE.putAll(values);
        }
    }
}
