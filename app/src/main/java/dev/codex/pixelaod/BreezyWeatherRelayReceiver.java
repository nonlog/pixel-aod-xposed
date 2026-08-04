package dev.codex.pixelaod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public final class BreezyWeatherRelayReceiver extends BroadcastReceiver {
    static final String ACTION_RELAY = "dev.codex.pixelaod.BREEZY_WEATHER_RELAY";
    static final String ACTION_REQUEST_RELAY = "dev.codex.pixelaod.REQUEST_BREEZY_WEATHER_RELAY";
    static final String EXTRA_RECEIVED_AT = "dev.codex.pixelaod.extra.RECEIVED_AT";
    static final String EXTRA_SUNRISE = "dev.codex.pixelaod.extra.SUNRISE";
    static final String EXTRA_SUNSET = "dev.codex.pixelaod.extra.SUNSET";
    static final String EXTRA_ALERT_JSON = "dev.codex.pixelaod.extra.WEATHER_ALERT_JSON";
    static final String EXTRA_ALERTS_SYNCED = "dev.codex.pixelaod.extra.WEATHER_ALERTS_SYNCED";
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
    private static final String KEY_SUNRISE = "sunrise_millis";
    private static final String KEY_SUNSET = "sunset_millis";
    private static final String KEY_ALERT_JSON = "alert_json";
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
            if (hasWeatherPayload(intent)) {
                persistWeatherExtras(context, intent);
            }
            BreezyWeatherAlert.QueryResult alertResult = queryWeatherAlerts(context);
            BreezyWeatherAlert relayedAlert = alertResult.queried
                    ? persistWeatherAlert(context, alertResult.alert)
                    : readPersistedWeatherAlert(context);
            Intent relay = new Intent(ACTION_RELAY)
                    .setPackage("com.android.systemui")
                    .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                relay.putExtras(extras);
            }
            appendWeatherAlert(relay, relayedAlert);
            // Re-extract sunrise/sunset from persisted JSON so the clock view
            // gets authoritative day/night boundaries.
            long[] sunTimes = readSunTimesFromPrefs(context);
            if (sunTimes != null) {
                if (sunTimes[0] > 0L) {
                    relay.putExtra(EXTRA_SUNRISE, sunTimes[0]);
                }
                if (sunTimes[1] > 0L) {
                    relay.putExtra(EXTRA_SUNSET, sunTimes[1]);
                }
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
        BreezyWeatherAlert.QueryResult alertResult = queryWeatherAlerts(context);
        BreezyWeatherAlert relayedAlert = alertResult.queried
                ? persistWeatherAlert(context, alertResult.alert)
                : readPersistedWeatherAlert(context);
        long receivedAt = prefs.getLong(KEY_RECEIVED_AT, 0L);
        if (receivedAt <= 0L && !alertResult.queried
                && relayedAlert == BreezyWeatherAlert.empty()) {
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
        if (prefs.contains(KEY_SUNRISE)) {
            relay.putExtra(EXTRA_SUNRISE, prefs.getLong(KEY_SUNRISE, 0L));
        }
        if (prefs.contains(KEY_SUNSET)) {
            relay.putExtra(EXTRA_SUNSET, prefs.getLong(KEY_SUNSET, 0L));
        }
        appendWeatherAlert(relay, relayedAlert);
        relay.putExtra(EXTRA_RECEIVED_AT,
                receivedAt > 0L ? receivedAt : System.currentTimeMillis());
        context.sendBroadcast(relay);
        Log.i(TAG, "replayed cached Breezy weather receivedAt=" + receivedAt);
    }

    private static void persistWeatherExtras(Context context, Intent intent) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putLong(KEY_RECEIVED_AT, System.currentTimeMillis());
        String json = intent.getStringExtra("WeatherJson");
        putString(editor, KEY_WEATHER_JSON, json);
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
        long[] sunTimes = extractSunTimes(intent, json, weatherGz);
        if (sunTimes != null) {
            if (sunTimes[0] > 0L) {
                editor.putLong(KEY_SUNRISE, sunTimes[0]);
            } else {
                editor.remove(KEY_SUNRISE);
            }
            if (sunTimes[1] > 0L) {
                editor.putLong(KEY_SUNSET, sunTimes[1]);
            } else {
                editor.remove(KEY_SUNSET);
            }
        }
        editor.apply();
    }

    private static boolean hasWeatherPayload(Intent intent) {
        return intent != null && (intent.hasExtra("WeatherJson")
                || intent.hasExtra("WeatherGz")
                || intent.hasExtra("currentCondition")
                || intent.hasExtra("condition")
                || intent.hasExtra("weatherText")
                || intent.hasExtra("currentTemp")
                || intent.hasExtra("temperature")
                || intent.hasExtra("temp"));
    }

    private static BreezyWeatherAlert.QueryResult queryWeatherAlerts(Context context) {
        if (!PixelAodSettings.getBoolean(context, PixelAodSettings.KEY_WEATHER, true)
                || !PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_WEATHER_ALERTS, false)) {
            return BreezyWeatherAlert.QueryResult.queried(BreezyWeatherAlert.empty());
        }
        return BreezyWeatherAlertProvider.queryCurrent(context, System.currentTimeMillis());
    }

    private static BreezyWeatherAlert persistWeatherAlert(Context context, BreezyWeatherAlert alert) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        BreezyWeatherAlert observed = BreezyWeatherAlert.observeForDisplay(alert,
                BreezyWeatherAlert.fromRelayJson(prefs.getString(KEY_ALERT_JSON, null)),
                System.currentTimeMillis());
        SharedPreferences.Editor editor = prefs.edit();
        putString(editor, KEY_ALERT_JSON, observed.toRelayJson());
        editor.apply();
        return observed;
    }

    private static BreezyWeatherAlert readPersistedWeatherAlert(Context context) {
        if (context == null) {
            return BreezyWeatherAlert.empty();
        }
        return BreezyWeatherAlert.fromRelayJson(context.getSharedPreferences(PREFS,
                Context.MODE_PRIVATE).getString(KEY_ALERT_JSON, null));
    }

    private static void appendWeatherAlert(Intent relay, BreezyWeatherAlert alert) {
        if (relay == null) {
            return;
        }
        relay.putExtra(EXTRA_ALERTS_SYNCED, true);
        putStringIfPresent(relay, EXTRA_ALERT_JSON,
                alert != null ? alert.toRelayJson() : null);
    }

    private static long[] extractSunTimes(Intent intent, String json, byte[] weatherGz) {
        long[] fromIntent = readSunTimesFromIntent(intent);
        if (fromIntent[0] > 0L && fromIntent[1] > 0L) {
            return fromIntent;
        }
        long[] fromJson = readSunTimesFromJson(json, weatherGz);
        if (fromJson[0] > 0L) {
            fromIntent[0] = fromJson[0];
        }
        if (fromJson[1] > 0L) {
            fromIntent[1] = fromJson[1];
        }
        return fromIntent;
    }

    private static long[] readSunTimesFromIntent(Intent intent) {
        long[] result = new long[]{0L, 0L};
        if (intent == null) {
            return result;
        }
        if (intent.hasExtra(EXTRA_SUNRISE)) {
            result[0] = intent.getLongExtra(EXTRA_SUNRISE, 0L);
        } else if (intent.hasExtra("sunrise")) {
            result[0] = intent.getLongExtra("sunrise", 0L);
        } else if (intent.hasExtra("sunRise")) {
            result[0] = intent.getLongExtra("sunRise", 0L);
        }
        if (intent.hasExtra(EXTRA_SUNSET)) {
            result[1] = intent.getLongExtra(EXTRA_SUNSET, 0L);
        } else if (intent.hasExtra("sunset")) {
            result[1] = intent.getLongExtra("sunset", 0L);
        } else if (intent.hasExtra("sunSet")) {
            result[1] = intent.getLongExtra("sunSet", 0L);
        }
        
        if (result[0] > 0L && result[0] < 100000000000L) {
            result[0] *= 1000L;
        }
        if (result[1] > 0L && result[1] < 100000000000L) {
            result[1] *= 1000L;
        }
        
        return result;
    }

    private static long[] readSunTimesFromJson(String json, byte[] weatherGz) {
        long[] result = new long[]{0L, 0L};
        String target = json;
        if ((target == null || target.isEmpty()) && weatherGz != null && weatherGz.length > 0) {
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(weatherGz));
                 InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    builder.append(buffer, 0, read);
                }
                String jsonStr = builder.toString();
                try {
                    org.json.JSONArray array = new org.json.JSONArray(jsonStr);
                    if (array.length() > 0) {
                        org.json.JSONObject first = array.optJSONObject(0);
                        if (first != null) {
                            target = first.toString();
                        }
                    }
                } catch (Throwable t) {
                    target = jsonStr;
                }
            } catch (Throwable t) {
                return result;
            }
        }
        if (target == null || target.isEmpty()) {
            return result;
        }
        try {
            org.json.JSONObject object = new org.json.JSONObject(target);
            long sunriseSec = readSunSecondsFromObject(object, "sunRise", "sunrise");
            long sunsetSec = readSunSecondsFromObject(object, "sunSet", "sunset");
            if (sunriseSec <= 0L || sunsetSec <= 0L) {
                org.json.JSONObject hourly = object.optJSONObject("hourlyForecast");
                if (hourly == null) {
                    org.json.JSONArray arrays = object.optJSONArray("hourlyForecast");
                    if (arrays != null && arrays.length() > 0) {
                        hourly = arrays.optJSONObject(0);
                    }
                }
                if (hourly != null) {
                    if (sunriseSec <= 0L) {
                        sunriseSec = readSunSecondsFromObject(hourly, "sunRise", "sunrise");
                    }
                    if (sunsetSec <= 0L) {
                        sunsetSec = readSunSecondsFromObject(hourly, "sunSet", "sunset");
                    }
                }
            }
            if (sunriseSec <= 0L || sunsetSec <= 0L) {
                org.json.JSONArray daily = object.optJSONArray("dailyForecast");
                if (daily != null) {
                    for (int i = 0; i < daily.length(); i++) {
                        org.json.JSONObject day = daily.optJSONObject(i);
                        if (day == null) continue;
                        if (sunriseSec <= 0L) {
                            sunriseSec = readSunSecondsFromObject(day, "sunRise", "sunrise");
                        }
                        if (sunsetSec <= 0L) {
                            sunsetSec = readSunSecondsFromObject(day, "sunSet", "sunset");
                        }
                        if (sunriseSec > 0L && sunsetSec > 0L) break;
                    }
                }
            }
            if (sunriseSec > 0L) {
                result[0] = (sunriseSec < 100000000000L) ? sunriseSec * 1000L : sunriseSec;
            }
            if (sunsetSec > 0L) {
                result[1] = (sunsetSec < 100000000000L) ? sunsetSec * 1000L : sunsetSec;
            }
        } catch (Throwable t) {
            Log.w(TAG, "failed to extract sunrise/sunset from Breezy weather JSON", t);
        }
        return result;
    }

    private static long readSunSecondsFromObject(org.json.JSONObject object, String camelKey, String lowerKey) {
        if (object == null) {
            return 0L;
        }
        if (object.has(camelKey)) {
            return object.optLong(camelKey, 0L);
        }
        if (object.has(lowerKey)) {
            return object.optLong(lowerKey, 0L);
        }
        return 0L;
    }

    private static long[] readSunTimesFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long[] result = new long[]{0L, 0L};
        if (prefs.contains(KEY_SUNRISE)) {
            result[0] = prefs.getLong(KEY_SUNRISE, 0L);
        }
        if (prefs.contains(KEY_SUNSET)) {
            result[1] = prefs.getLong(KEY_SUNSET, 0L);
        }
        return result;
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
