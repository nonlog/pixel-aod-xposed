package dev.codex.pixelaod;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

/** Reads only the current active alert from Breezy Weather's opt-in public provider. */
final class BreezyWeatherAlertProvider {
    static final String READ_PROVIDER_PERMISSION = "org.breezyweather.READ_PROVIDER";
    private static final Uri CURRENT_WEATHER_URI = Uri.parse(
            "content://org.breezyweather.provider.weather/weather"
                    + "?withDaily=false&withHourly=false&withMinutely=false"
                    + "&withAlerts=true&withNormals=false");
    private static final String CURRENT_POSITION_SELECTION = "id = CURRENT_POSITION";

    private BreezyWeatherAlertProvider() {
    }

    static BreezyWeatherAlert.QueryResult queryCurrent(Context context, long nowMillis) {
        if (context == null || context.checkSelfPermission(READ_PROVIDER_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return BreezyWeatherAlert.QueryResult.notQueried();
        }
        try (Cursor cursor = context.getContentResolver().query(CURRENT_WEATHER_URI, null,
                CURRENT_POSITION_SELECTION, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return BreezyWeatherAlert.QueryResult.queried(BreezyWeatherAlert.empty());
            }
            int weatherColumn = cursor.getColumnIndex("weather");
            if (weatherColumn < 0 || cursor.isNull(weatherColumn)) {
                return BreezyWeatherAlert.QueryResult.queried(BreezyWeatherAlert.empty());
            }
            return BreezyWeatherAlert.QueryResult.queried(
                    BreezyWeatherAlert.fromProviderPayload(cursor.getBlob(weatherColumn), nowMillis));
        } catch (Throwable ignored) {
            // Breezy may be absent, updating its provider, or have revoked its runtime permission.
            return BreezyWeatherAlert.QueryResult.notQueried();
        }
    }
}
