package dev.codex.pixelaod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

public final class BreezyWeatherRelayReceiver extends BroadcastReceiver {
    static final String ACTION_RELAY = "dev.codex.pixelaod.BREEZY_WEATHER_RELAY";
    static final String ACTION_REQUEST_RELAY = "dev.codex.pixelaod.REQUEST_BREEZY_WEATHER_RELAY";
    static final String EXTRA_RECEIVED_AT = "dev.codex.pixelaod.extra.RECEIVED_AT";
    private static final String PREFS = "pixel_aod_weather";
    private static final String KEY_RECEIVED_AT = "received_at";
    private static final String KEY_WEATHER_JSON = "weather_json";
    private static final String KEY_WEATHER_GZ = "weather_gz";
    private static final String KEY_CURRENT_CONDITION = "current_condition";
    private static final String KEY_CONDITION = "condition";
    private static final String KEY_WEATHER_TEXT = "weather_text";
    private static final String KEY_CURRENT_TEMP = "current_temp";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_TEMP = "temp";
    private static final String KEY_CURRENT_CONDITION_CODE = "current_condition_code";
    private static final String KEY_WEATHER_CODE = "weather_code";
    private static final String KEY_CONDITION_CODE = "condition_code";
    private static final String TAG = "PixelAodBreezyRelay";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        try {
            if (ACTION_REQUEST_RELAY.equals(intent.getAction())) {
                replayCachedWeather(context);
                return;
            }
            persistWeatherExtras(context, intent);
            Intent relay = new Intent(ACTION_RELAY)
                    .setPackage("com.android.systemui")
                    .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                relay.putExtras(extras);
            }
            relay.putExtra(EXTRA_RECEIVED_AT, System.currentTimeMillis());
            context.sendBroadcast(relay);
            Log.i(TAG, "relayed Breezy weather action=" + intent.getAction());
        } catch (Throwable t) {
            Log.w(TAG, "failed to relay Breezy weather", t);
        }
    }

    private static void replayCachedWeather(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long receivedAt = prefs.getLong(KEY_RECEIVED_AT, 0L);
        if (receivedAt <= 0L) {
            Log.i(TAG, "no cached Breezy weather to replay");
            return;
        }
        Intent relay = new Intent(ACTION_RELAY)
                .setPackage("com.android.systemui")
                .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        putStringIfPresent(relay, "WeatherJson", prefs.getString(KEY_WEATHER_JSON, null));
        String weatherGz = prefs.getString(KEY_WEATHER_GZ, null);
        if (weatherGz != null && !weatherGz.isEmpty()) {
            relay.putExtra("WeatherGz", Base64.decode(weatherGz, Base64.NO_WRAP));
        }
        putStringIfPresent(relay, "currentCondition", prefs.getString(KEY_CURRENT_CONDITION, null));
        putStringIfPresent(relay, "condition", prefs.getString(KEY_CONDITION, null));
        putStringIfPresent(relay, "weatherText", prefs.getString(KEY_WEATHER_TEXT, null));
        putDoubleIfPresent(relay, "currentTemp", prefs, KEY_CURRENT_TEMP);
        putDoubleIfPresent(relay, "temperature", prefs, KEY_TEMPERATURE);
        putDoubleIfPresent(relay, "temp", prefs, KEY_TEMP);
        putIntIfPresent(relay, "currentConditionCode", prefs, KEY_CURRENT_CONDITION_CODE);
        putIntIfPresent(relay, "weatherCode", prefs, KEY_WEATHER_CODE);
        putIntIfPresent(relay, "conditionCode", prefs, KEY_CONDITION_CODE);
        relay.putExtra(EXTRA_RECEIVED_AT, receivedAt);
        context.sendBroadcast(relay);
        Log.i(TAG, "replayed cached Breezy weather receivedAt=" + receivedAt);
    }

    private static void persistWeatherExtras(Context context, Intent intent) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putLong(KEY_RECEIVED_AT, System.currentTimeMillis());
        putString(editor, KEY_WEATHER_JSON, intent.getStringExtra("WeatherJson"));
        byte[] weatherGz = intent.getByteArrayExtra("WeatherGz");
        putString(editor, KEY_WEATHER_GZ,
                weatherGz != null && weatherGz.length > 0
                        ? Base64.encodeToString(weatherGz, Base64.NO_WRAP)
                        : null);
        putString(editor, KEY_CURRENT_CONDITION, intent.getStringExtra("currentCondition"));
        putString(editor, KEY_CONDITION, intent.getStringExtra("condition"));
        putString(editor, KEY_WEATHER_TEXT, intent.getStringExtra("weatherText"));
        putDouble(editor, KEY_CURRENT_TEMP, intent, "currentTemp");
        putDouble(editor, KEY_TEMPERATURE, intent, "temperature");
        putDouble(editor, KEY_TEMP, intent, "temp");
        putInt(editor, KEY_CURRENT_CONDITION_CODE, intent, "currentConditionCode");
        putInt(editor, KEY_WEATHER_CODE, intent, "weatherCode");
        putInt(editor, KEY_CONDITION_CODE, intent, "conditionCode");
        editor.apply();
    }

    private static void putString(SharedPreferences.Editor editor, String key, String value) {
        if (value == null || value.isEmpty()) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
    }

    private static void putDouble(SharedPreferences.Editor editor, String key, Intent intent,
            String extraName) {
        if (intent.hasExtra(extraName)) {
            editor.putLong(key, Double.doubleToLongBits(intent.getDoubleExtra(extraName, Double.NaN)));
        } else {
            editor.remove(key);
        }
    }

    private static void putInt(SharedPreferences.Editor editor, String key, Intent intent,
            String extraName) {
        if (intent.hasExtra(extraName)) {
            editor.putInt(key, intent.getIntExtra(extraName, Integer.MIN_VALUE));
        } else {
            editor.remove(key);
        }
    }

    private static void putStringIfPresent(Intent intent, String extraName, String value) {
        if (value != null && !value.isEmpty()) {
            intent.putExtra(extraName, value);
        }
    }

    private static void putDoubleIfPresent(Intent intent, String extraName,
            SharedPreferences prefs, String key) {
        if (prefs.contains(key)) {
            intent.putExtra(extraName, Double.longBitsToDouble(prefs.getLong(key, 0L)));
        }
    }

    private static void putIntIfPresent(Intent intent, String extraName,
            SharedPreferences prefs, String key) {
        if (prefs.contains(key)) {
            intent.putExtra(extraName, prefs.getInt(key, Integer.MIN_VALUE));
        }
    }
}
