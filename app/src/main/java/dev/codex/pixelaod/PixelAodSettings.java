package dev.codex.pixelaod;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public final class PixelAodSettings {
    public static final String PREFS = PixelAodSettingsSchema.PREFS;
    public static final String KEY_MODULE_ENABLED = PixelAodSettingsSchema.KEY_MODULE_ENABLED;
    public static final String KEY_CUSTOM_AOD = PixelAodSettingsSchema.KEY_CUSTOM_AOD;
    public static final String KEY_LOCKSCREEN_CLOCK = PixelAodSettingsSchema.KEY_LOCKSCREEN_CLOCK;
    public static final String KEY_AOD_DISPLAY_MODE = PixelAodSettingsSchema.KEY_AOD_DISPLAY_MODE;
    public static final String KEY_WEATHER = PixelAodSettingsSchema.KEY_WEATHER;
    public static final String KEY_WEATHER_ALERTS = PixelAodSettingsSchema.KEY_WEATHER_ALERTS;
    public static final String KEY_WEATHER_FORECAST =
            PixelAodSettingsSchema.KEY_WEATHER_FORECAST;
    public static final String KEY_WEATHER_FORECAST_START_TIME =
            PixelAodSettingsSchema.KEY_WEATHER_FORECAST_START_TIME;
    public static final String KEY_WEATHER_FORECAST_END_TIME =
            PixelAodSettingsSchema.KEY_WEATHER_FORECAST_END_TIME;
    public static final String KEY_WEATHER_ICON_PACK = PixelAodSettingsSchema.KEY_WEATHER_ICON_PACK;
    public static final String KEY_CALENDAR_EVENTS = PixelAodSettingsSchema.KEY_CALENDAR_EVENTS;
    public static final String KEY_CALENDAR_ICON_PACKAGE =
            PixelAodSettingsSchema.KEY_CALENDAR_ICON_PACKAGE;
    public static final String KEY_NOTIFICATION_ICONS =
            PixelAodSettingsSchema.KEY_NOTIFICATION_ICONS;
    public static final String KEY_PIXEL_FINGERPRINT_ICON =
            PixelAodSettingsSchema.KEY_PIXEL_FINGERPRINT_ICON;
    public static final String KEY_LOCKSCREEN_NOTIFICATION_POLICY =
            PixelAodSettingsSchema.KEY_LOCKSCREEN_NOTIFICATION_POLICY;
    public static final String KEY_DEBUG_LOGGING = PixelAodSettingsSchema.KEY_DEBUG_LOGGING;
    public static final String KEY_AOD_WEIGHT = PixelAodSettingsSchema.KEY_AOD_WEIGHT;
    public static final String KEY_LOCKSCREEN_WEIGHT = PixelAodSettingsSchema.KEY_LOCKSCREEN_WEIGHT;
    public static final String KEY_FORCE_ENGLISH_DATE =
            PixelAodSettingsSchema.KEY_FORCE_ENGLISH_DATE;
    public static final String KEY_DISABLE_BURN_IN_OFFSET =
            PixelAodSettingsSchema.KEY_DISABLE_BURN_IN_OFFSET;
    public static final String KEY_POCKET_MODE = PixelAodSettingsSchema.KEY_POCKET_MODE;
    public static final String KEY_AOD_SCHEDULE_ENABLED =
            PixelAodSettingsSchema.KEY_AOD_SCHEDULE_ENABLED;
    public static final String KEY_AOD_SCHEDULE_START_TIME =
            PixelAodSettingsSchema.KEY_AOD_SCHEDULE_START_TIME;
    public static final String KEY_AOD_SCHEDULE_END_TIME =
            PixelAodSettingsSchema.KEY_AOD_SCHEDULE_END_TIME;
    public static final String KEY_LANGUAGE = PixelAodSettingsSchema.KEY_LANGUAGE;
    public static final String AOD_DISPLAY_MODE_CONTINUOUS =
            PixelAodSettingsSchema.AOD_DISPLAY_MODE_CONTINUOUS;
    public static final String AOD_DISPLAY_MODE_TRIGGER_ONLY =
            PixelAodSettingsSchema.AOD_DISPLAY_MODE_TRIGGER_ONLY;
    public static final String LANGUAGE_SYSTEM = PixelAodSettingsSchema.LANGUAGE_SYSTEM;
    public static final String LANGUAGE_CHINESE = PixelAodSettingsSchema.LANGUAGE_CHINESE;
    public static final String LANGUAGE_ENGLISH = PixelAodSettingsSchema.LANGUAGE_ENGLISH;
    public static final float DEFAULT_AOD_WEIGHT = PixelAodSettingsSchema.DEFAULT_AOD_WEIGHT;
    public static final float DEFAULT_LOCKSCREEN_WEIGHT =
            PixelAodSettingsSchema.DEFAULT_LOCKSCREEN_WEIGHT;
    private static final Map<String, String> CACHE = new HashMap<>();
    private static long lastLoadMillis;
    private static final long CACHE_TTL_MILLIS = 2_000L;

    private PixelAodSettings() {
    }

    public static boolean getBoolean(Context context, String key, boolean fallback) {
        if (isAlwaysEnabledKey(key)) {
            return true;
        }
        String value = getValue(context, key);
        return value == null ? defaultBoolean(key, fallback) : Boolean.parseBoolean(value);
    }

    public static boolean isAlwaysEnabledKey(String key) {
        return PixelAodSettingsSchema.isAlwaysEnabledKey(key);
    }

    public static boolean requiresSystemUiRestart(String key) {
        return PixelAodSettingsSchema.requiresSystemUiRestart(key);
    }

    public static boolean defaultBoolean(String key, boolean fallback) {
        return PixelAodSettingsSchema.booleanDefault(key, fallback);
    }

    public static String defaultString(String key, String fallback) {
        return PixelAodSettingsSchema.stringDefault(key, fallback);
    }

    public static float defaultFloat(String key, float fallback) {
        return PixelAodSettingsSchema.floatDefault(key, fallback);
    }

    public static boolean normalizeAlwaysEnabledPreferences(SharedPreferences prefs) {
        if (prefs == null) {
            return false;
        }
        boolean changed = false;
        SharedPreferences.Editor editor = null;
        if (!prefs.getBoolean(KEY_NOTIFICATION_ICONS,
                defaultBoolean(KEY_NOTIFICATION_ICONS, true))) {
            editor = prefs.edit();
            editor.putBoolean(KEY_NOTIFICATION_ICONS, true);
            changed = true;
        }
        if (!prefs.getBoolean(KEY_POCKET_MODE, defaultBoolean(KEY_POCKET_MODE, true))) {
            if (editor == null) {
                editor = prefs.edit();
            }
            editor.putBoolean(KEY_POCKET_MODE, true);
            changed = true;
        }
        return !changed || (editor != null && editor.commit());
    }

    public static String getString(Context context, String key, String fallback) {
        String value = getValue(context, key);
        return value == null ? defaultString(key, fallback) : value;
    }

    public static float getFloat(Context context, String key, float fallback) {
        String value = getValue(context, key);
        if (value == null) {
            return defaultFloat(key, fallback);
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return defaultFloat(key, fallback);
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

    public static SharedPreferences getSharedPreferences(Context context) {
        Context storage = storageContext(context);
        return storage.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
        PixelAodLog.setDebugEnabled(Boolean.parseBoolean(
                values.getOrDefault(KEY_DEBUG_LOGGING,
                        Boolean.toString(defaultBoolean(KEY_DEBUG_LOGGING, false)))));
    }

    private static Context storageContext(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 24) {
            return context;
        }
        Context directBootContext = context.createDeviceProtectedStorageContext();
        if (directBootContext == null) {
            return context;
        }
        try {
            directBootContext.moveSharedPreferencesFrom(context, PREFS);
        } catch (Throwable ignored) {
            // The credential-encrypted store may be locked during early SystemUI startup.
        }
        return directBootContext;
    }
}
