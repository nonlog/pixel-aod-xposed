package dev.codex.pixelaod;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;

public final class PixelAodSettingsProvider extends ContentProvider {
    static final String AUTHORITY = "dev.codex.pixelaod.settings";
    static final Uri URI = Uri.parse("content://" + AUTHORITY + "/preferences");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "type", "value"});
        Context context = getContext();
        if (context == null) {
            return cursor;
        }
        SharedPreferences prefs = PixelAodSettings.getSharedPreferences(context);
        putBoolean(cursor, PixelAodSettings.KEY_CUSTOM_AOD,
                prefs.getBoolean(PixelAodSettings.KEY_CUSTOM_AOD, true));
        putBoolean(cursor, PixelAodSettings.KEY_SKIP_DOZE_OFF_STATE,
                prefs.getBoolean(PixelAodSettings.KEY_SKIP_DOZE_OFF_STATE, false));
        putBoolean(cursor, PixelAodSettings.KEY_LOCKSCREEN_CLOCK,
                prefs.getBoolean(PixelAodSettings.KEY_LOCKSCREEN_CLOCK, true));
        putBoolean(cursor, PixelAodSettings.KEY_WEATHER,
                prefs.getBoolean(PixelAodSettings.KEY_WEATHER, true));
        putBoolean(cursor, PixelAodSettings.KEY_NOTIFICATION_ICONS,
                prefs.getBoolean(PixelAodSettings.KEY_NOTIFICATION_ICONS, true));
        putBoolean(cursor, PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY,
                prefs.getBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true));
        putBoolean(cursor, PixelAodSettings.KEY_DEBUG_LOGGING,
                prefs.getBoolean(PixelAodSettings.KEY_DEBUG_LOGGING, false));
        putFloat(cursor, PixelAodSettings.KEY_CLOCK_SCALE,
                prefs.getFloat(PixelAodSettings.KEY_CLOCK_SCALE,
                        PixelAodSettings.DEFAULT_CLOCK_SCALE));
        putFloat(cursor, PixelAodSettings.KEY_AOD_WEIGHT,
                prefs.getFloat(PixelAodSettings.KEY_AOD_WEIGHT,
                        PixelAodSettings.DEFAULT_AOD_WEIGHT));
        putFloat(cursor, PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                prefs.getFloat(PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                        PixelAodSettings.DEFAULT_LOCKSCREEN_WEIGHT));
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.dev.codex.pixelaod.preference";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Context context = getContext();
        if (context == null || values == null) {
            return 0;
        }
        String key = values.getAsString("key");
        if (TextUtils.isEmpty(key)) {
            return 0;
        }
        Object rawValue = values.get("value");
        if (rawValue == null) {
            return 0;
        }
        SharedPreferences.Editor editor = PixelAodSettings.getSharedPreferences(context).edit();
        if (rawValue instanceof Boolean) {
            editor.putBoolean(key, (Boolean) rawValue);
        } else if (rawValue instanceof Integer) {
            editor.putInt(key, (Integer) rawValue);
        } else if (rawValue instanceof Long) {
            editor.putLong(key, (Long) rawValue);
        } else if (rawValue instanceof Float) {
            editor.putFloat(key, (Float) rawValue);
        } else if (rawValue instanceof Double) {
            editor.putFloat(key, ((Double) rawValue).floatValue());
        } else {
            String value = String.valueOf(rawValue);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                editor.putBoolean(key, Boolean.parseBoolean(value));
            } else {
                try {
                    if (value.contains(".")) {
                        editor.putFloat(key, Float.parseFloat(value));
                    } else {
                        editor.putInt(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException ignored) {
                    editor.putString(key, value);
                }
            }
        }
        if (!editor.commit()) {
            return 0;
        }
        PixelAodSettings.refresh(context);
        context.getContentResolver().notifyChange(URI, null);
        return 1;
    }

    private static void putBoolean(MatrixCursor cursor, String key, boolean value) {
        cursor.addRow(new Object[]{key, "boolean", value ? "true" : "false"});
    }

    private static void putFloat(MatrixCursor cursor, String key, float value) {
        cursor.addRow(new Object[]{key, "float", Float.toString(value)});
    }
}
