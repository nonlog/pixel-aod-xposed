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
        PixelAodSettings.normalizeAlwaysEnabledPreferences(prefs);
        for (PixelAodSettingsSchema.SettingSpec spec : PixelAodSettingsSchema.allSpecs()) {
            putSetting(cursor, prefs, spec);
        }
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
        PixelAodSettingsSchema.SettingSpec spec = PixelAodSettingsSchema.spec(key);
        if (writeKnownSetting(editor, spec, key, rawValue)) {
            return commitUpdate(context, editor);
        }
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
        return commitUpdate(context, editor);
    }

    private static void putSetting(MatrixCursor cursor, SharedPreferences prefs,
            PixelAodSettingsSchema.SettingSpec spec) {
        if (spec == null) {
            return;
        }
        if (spec.type == PixelAodSettingsSchema.Type.BOOLEAN) {
            putBoolean(cursor, spec.key, spec.alwaysEnabled
                    || prefs.getBoolean(spec.key, spec.defaultBoolean()));
            return;
        }
        if (spec.type == PixelAodSettingsSchema.Type.FLOAT) {
            putFloat(cursor, spec.key, prefs.getFloat(spec.key, spec.defaultFloat()));
            return;
        }
        putString(cursor, spec.key, prefs.getString(spec.key, spec.defaultString()));
    }

    private static boolean writeKnownSetting(SharedPreferences.Editor editor,
            PixelAodSettingsSchema.SettingSpec spec, String key, Object rawValue) {
        if (spec == null) {
            return false;
        }
        if (spec.alwaysEnabled) {
            editor.putBoolean(key, true);
            return true;
        }
        if (spec.type == PixelAodSettingsSchema.Type.BOOLEAN) {
            editor.putBoolean(key, rawValue instanceof Boolean
                    ? (Boolean) rawValue
                    : Boolean.parseBoolean(String.valueOf(rawValue)));
            return true;
        }
        if (spec.type == PixelAodSettingsSchema.Type.FLOAT) {
            editor.putFloat(key, parseFloat(rawValue, spec.defaultFloat()));
            return true;
        }
        editor.putString(key, String.valueOf(rawValue));
        return true;
    }

    private static float parseFloat(Object rawValue, float fallback) {
        if (rawValue instanceof Float) {
            return (Float) rawValue;
        }
        if (rawValue instanceof Double) {
            return ((Double) rawValue).floatValue();
        }
        if (rawValue instanceof Number) {
            return ((Number) rawValue).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(rawValue));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int commitUpdate(Context context, SharedPreferences.Editor editor) {
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

    private static void putString(MatrixCursor cursor, String key, String value) {
        cursor.addRow(new Object[]{key, "string", value});
    }
}
