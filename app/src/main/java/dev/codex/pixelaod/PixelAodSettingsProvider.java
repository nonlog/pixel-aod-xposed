package dev.codex.pixelaod;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

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
        SharedPreferences prefs = context.getSharedPreferences(PixelAodSettings.PREFS,
                Context.MODE_PRIVATE);
        putBoolean(cursor, PixelAodSettings.KEY_CUSTOM_AOD,
                prefs.getBoolean(PixelAodSettings.KEY_CUSTOM_AOD, true));
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
        return 0;
    }

    private static void putBoolean(MatrixCursor cursor, String key, boolean value) {
        cursor.addRow(new Object[]{key, "boolean", value ? "true" : "false"});
    }

    private static void putFloat(MatrixCursor cursor, String key, float value) {
        cursor.addRow(new Object[]{key, "float", Float.toString(value)});
    }
}
