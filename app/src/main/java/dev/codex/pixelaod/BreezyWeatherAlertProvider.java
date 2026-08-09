package dev.codex.pixelaod;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

/** Reads the current-position Breezy row for the relay path. */
final class BreezyWeatherAlertProvider {
    static final String READ_PROVIDER_PERMISSION = "org.breezyweather.READ_PROVIDER";
    private static final Uri CURRENT_WEATHER_URI = Uri.parse(
            "content://org.breezyweather.provider.weather/weather"
                    + "?withDaily=true&withHourly=false&withMinutely=false"
                    + "&withAlerts=true&withNormals=false");
    private static final String CURRENT_POSITION_SELECTION = "id = CURRENT_POSITION";

    private BreezyWeatherAlertProvider() {
    }

    static BreezyWeatherAlert.QueryResult queryCurrent(Context context, long nowMillis) {
        BreezyWeatherSnapshot.QueryResult result = querySnapshot(context, nowMillis);
        if (!result.queried) {
            return BreezyWeatherAlert.QueryResult.notQueried();
        }
        return BreezyWeatherAlert.QueryResult.queried(
                BreezyWeatherAlert.selectActive(nowMillis, BreezyWeatherAlert.empty(),
                        result.snapshot.activeAlerts.isEmpty()
                                ? BreezyWeatherAlert.empty() : result.snapshot.activeAlerts.get(0)));
    }

    static BreezyWeatherSnapshot.QueryResult querySnapshot(Context context, long nowMillis) {
        if (context == null || context.checkSelfPermission(READ_PROVIDER_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return BreezyWeatherSnapshot.QueryResult.notQueried(null);
        }
        try (Cursor cursor = context.getContentResolver().query(CURRENT_WEATHER_URI, null,
                CURRENT_POSITION_SELECTION, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return BreezyWeatherSnapshot.QueryResult.queried(
                        BreezyWeatherSnapshot.queried("current-position",
                                java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                                nowMillis));
            }
            int weatherColumn = cursor.getColumnIndex("weather");
            String locationId = cursorLocationId(cursor);
            if (weatherColumn < 0 || cursor.isNull(weatherColumn)) {
                return BreezyWeatherSnapshot.QueryResult.notQueried(null);
            }
            BreezyWeatherSnapshot snapshot = BreezyWeatherSnapshot.fromProviderPayload(
                    cursor.getBlob(weatherColumn), locationId, nowMillis);
            return snapshot != null
                    ? BreezyWeatherSnapshot.QueryResult.queried(snapshot)
                    : BreezyWeatherSnapshot.QueryResult.notQueried(null);
        } catch (Throwable ignored) {
            // Breezy may be absent, updating its provider, or have revoked its runtime permission.
            return BreezyWeatherSnapshot.QueryResult.notQueried(null);
        }
    }

    private static String cursorLocationId(Cursor cursor) {
        for (String column : new String[]{"locationId", "location_id", "placeId", "place_id",
                "cityId", "city_id", "id"}) {
            int index = cursor.getColumnIndex(column);
            if (index >= 0 && !cursor.isNull(index)) {
                String value = cursor.getString(index);
                if (value != null && !value.trim().isEmpty()
                        && !"CURRENT_POSITION".equalsIgnoreCase(value.trim())) {
                    return value.trim();
                }
            }
        }
        String latitude = cursorString(cursor, "latitude", "lat");
        String longitude = cursorString(cursor, "longitude", "lon", "lng");
        String name = cursorString(cursor, "locationName", "city", "name");
        if (!latitude.isEmpty() || !longitude.isEmpty() || !name.isEmpty()) {
            return latitude + "," + longitude + "," + name;
        }
        return "current-position";
    }

    private static String cursorString(Cursor cursor, String... columns) {
        for (String column : columns) {
            int index = cursor.getColumnIndex(column);
            if (index >= 0 && !cursor.isNull(index)) {
                String value = cursor.getString(index);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return "";
    }
}
