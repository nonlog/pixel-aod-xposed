package dev.codex.pixelaod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Additive, versioned relay payload for current-location forecast and all active alerts. */
final class BreezyWeatherSnapshot {
    static final int RELAY_VERSION = 1;
    static final long MAX_ALERT_SOURCE_AGE_MILLIS = 60L * 60L * 1000L;

    final String activeLocationId;
    final List<BreezyWeatherAlert> activeAlerts;
    final List<BreezyWeatherForecast> forecasts;
    final long lastSuccessfulSourceAtMillis;
    final boolean sourceQuerySucceeded;

    private BreezyWeatherSnapshot(String activeLocationId, List<BreezyWeatherAlert> activeAlerts,
            List<BreezyWeatherForecast> forecasts, long lastSuccessfulSourceAtMillis,
            boolean sourceQuerySucceeded) {
        this.activeLocationId = activeLocationId != null ? activeLocationId.trim() : "";
        this.activeAlerts = immutableAlerts(activeAlerts);
        this.forecasts = immutableForecasts(forecasts);
        this.lastSuccessfulSourceAtMillis = Math.max(0L, lastSuccessfulSourceAtMillis);
        this.sourceQuerySucceeded = sourceQuerySucceeded;
    }

    static BreezyWeatherSnapshot empty() {
        return new BreezyWeatherSnapshot("", Collections.emptyList(), Collections.emptyList(), 0L,
                false);
    }

    static BreezyWeatherSnapshot queried(String activeLocationId,
            List<BreezyWeatherAlert> activeAlerts, List<BreezyWeatherForecast> forecasts,
            long successfulAtMillis) {
        return new BreezyWeatherSnapshot(activeLocationId, activeAlerts, forecasts,
                successfulAtMillis, true);
    }

    static BreezyWeatherSnapshot failedUsing(BreezyWeatherSnapshot cached) {
        if (cached == null) {
            return empty();
        }
        return new BreezyWeatherSnapshot(cached.activeLocationId, cached.activeAlerts,
                cached.forecasts, cached.lastSuccessfulSourceAtMillis, false);
    }

    static RelayState relayState(BreezyWeatherSnapshot snapshot, boolean sourceQueried) {
        return new RelayState(snapshot != null, sourceQueried, !sourceQueried);
    }

    static boolean shouldApplyRelaySnapshot(boolean snapshotAvailable, boolean snapshotSynced) {
        // New broadcasts use availability; older successful broadcasts only had synced.
        return snapshotAvailable || snapshotSynced;
    }

    static final class RelayState {
        final boolean available;
        final boolean synced;
        final boolean queryFailed;

        RelayState(boolean available, boolean synced, boolean queryFailed) {
            this.available = available;
            this.synced = synced;
            this.queryFailed = queryFailed;
        }
    }

    boolean isAlertSourceFresh(long nowMillis) {
        long age = nowMillis - lastSuccessfulSourceAtMillis;
        return lastSuccessfulSourceAtMillis > 0L && age >= 0L
                && age <= MAX_ALERT_SOURCE_AGE_MILLIS;
    }

    BreezyWeatherForecast forecastFor(java.time.LocalDate date) {
        if (date == null) {
            return BreezyWeatherForecast.empty();
        }
        for (BreezyWeatherForecast forecast : forecasts) {
            if (date.equals(forecast.forecastDate)) {
                return forecast;
            }
        }
        return BreezyWeatherForecast.empty();
    }

    boolean sameForecastDisplay(BreezyWeatherSnapshot other) {
        if (other == null || activeLocationId.equals(other.activeLocationId)) {
            BreezyWeatherForecast left = firstDisplayableForecast();
            BreezyWeatherForecast right = other != null ? other.firstDisplayableForecast()
                    : BreezyWeatherForecast.empty();
            return left.sameDisplay(right);
        }
        return false;
    }

    boolean sameContextualDisplay(BreezyWeatherSnapshot other) {
        if (other == null || !activeLocationId.equals(other.activeLocationId)) {
            return false;
        }
        Set<String> leftAlerts = new HashSet<>();
        Set<String> rightAlerts = new HashSet<>();
        for (BreezyWeatherAlert left : activeAlerts) {
            if (left != null && !left.isEmpty()) {
                leftAlerts.add(left.presentationKey + "|severity=" + left.severity);
            }
        }
        for (BreezyWeatherAlert right : other.activeAlerts) {
            if (right != null && !right.isEmpty()) {
                rightAlerts.add(right.presentationKey + "|severity=" + right.severity);
            }
        }
        if (!leftAlerts.equals(rightAlerts)) {
            return false;
        }
        Set<String> leftForecasts = new HashSet<>();
        Set<String> rightForecasts = new HashSet<>();
        for (BreezyWeatherForecast left : forecasts) {
            if (left != null && left.isComplete()) {
                leftForecasts.add(left.displayKey());
            }
        }
        for (BreezyWeatherForecast right : other.forecasts) {
            if (right != null && right.isComplete()) {
                rightForecasts.add(right.displayKey());
            }
        }
        return leftForecasts.equals(rightForecasts);
    }

    BreezyWeatherForecast firstDisplayableForecast() {
        for (BreezyWeatherForecast forecast : forecasts) {
            if (forecast != null && forecast.isComplete()) {
                return forecast;
            }
        }
        return BreezyWeatherForecast.empty();
    }

    String toRelayJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("version", RELAY_VERSION);
            result.put("activeLocationId", activeLocationId);
            result.put("lastSuccessfulSourceAtMillis", lastSuccessfulSourceAtMillis);
            result.put("sourceQuerySucceeded", sourceQuerySucceeded);
            JSONArray alerts = new JSONArray();
            for (BreezyWeatherAlert alert : activeAlerts) {
                JSONObject object = alert != null ? alert.toJson() : null;
                if (object != null) {
                    alerts.put(object);
                }
            }
            result.put("activeAlerts", alerts);
            JSONArray forecasts = new JSONArray();
            for (BreezyWeatherForecast forecast : this.forecasts) {
                if (forecast != null) {
                    forecasts.put(forecast.toJson());
                }
            }
            result.put("forecasts", forecasts);
            return result.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    static BreezyWeatherSnapshot fromRelayJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(json);
            String locationId = object.optString("activeLocationId", "");
            ArrayList<BreezyWeatherAlert> alerts = new ArrayList<>();
            JSONArray alertArray = object.optJSONArray("activeAlerts");
            if (alertArray == null) {
                // Be tolerant of an additive payload that calls this field simply alerts.
                alertArray = object.optJSONArray("alerts");
            }
            if (alertArray != null) {
                for (int i = 0; i < alertArray.length(); i++) {
                    BreezyWeatherAlert alert = BreezyWeatherAlert.fromJson(alertArray.optJSONObject(i),
                            locationId);
                    if (alert != null && !alert.isEmpty()) {
                        alerts.add(alert);
                    }
                }
            }
            ArrayList<BreezyWeatherForecast> forecasts = new ArrayList<>();
            JSONArray forecastArray = object.optJSONArray("forecasts");
            if (forecastArray != null) {
                ZoneId zone = ZoneId.systemDefault();
                for (int i = 0; i < forecastArray.length(); i++) {
                    BreezyWeatherForecast forecast = BreezyWeatherForecast.fromRelayJson(
                            forecastArray.optJSONObject(i));
                    if (forecast != null && forecast.forecastDate != null) {
                        forecasts.add(forecast);
                    }
                }
            }
            return new BreezyWeatherSnapshot(locationId, alerts, forecasts,
                    object.optLong("lastSuccessfulSourceAtMillis", 0L),
                    object.optBoolean("sourceQuerySucceeded", true));
        } catch (Throwable ignored) {
            return null;
        }
    }

    static BreezyWeatherSnapshot fromProviderJson(String json, String fallbackLocationId,
            long nowMillis) {
        try {
            JSONObject envelope = parseProviderObject(json);
            JSONObject object = rootObject(envelope);
            if (object == null || !hasRecognizedSchema(envelope, object)
                    || hasFatalSchema(object)) {
                // A malformed or schema-fatal provider response is a query failure. The relay
                // layer will replay the last confirmed snapshot under its failure grace period.
                return null;
            }
            ZoneId zone = ZoneId.systemDefault();
            long sourceUpdatedAtMillis = sourceUpdatedAtMillis(envelope, object);
            String locationId = firstString(object, "activeLocationId", "locationId", "location_id",
                    "placeId", "place_id", "cityId", "city_id", "id");
            if (locationId.isEmpty() || "CURRENT_POSITION".equalsIgnoreCase(locationId)) {
                locationId = fallbackLocationId;
            }
            if (locationId.isEmpty()) {
                locationId = locationIdentityFromCoordinates(object);
            }
            if (locationId.isEmpty()) {
                locationId = "current-position";
            }

            ArrayList<BreezyWeatherAlert> alerts = new ArrayList<>();
            JSONArray alertArray = object.optJSONArray("alerts");
            if (alertArray != null) {
                alerts.addAll(BreezyWeatherAlert.fromJsonArray(alertArray, locationId));
            }

            ArrayList<BreezyWeatherForecast> forecasts = new ArrayList<>();
            for (JSONArray daily : dailyArrays(object)) {
                for (int i = 0; i < daily.length(); i++) {
                    JSONObject day = daily.optJSONObject(i);
                    BreezyWeatherForecast forecast = BreezyWeatherForecast.fromJson(day, locationId,
                            sourceUpdatedAtMillis, zone);
                    if (forecast.forecastDate != null) {
                        forecasts.add(forecast);
                    }
                }
            }
            // successfulAtMillis describes the successful query/confirmation, while each
            // forecast carries the provider's real source timestamp for its six-hour rule.
            return queried(locationId, alerts, forecasts, nowMillis);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static BreezyWeatherSnapshot fromProviderPayload(byte[] payload, String fallbackLocationId,
            long nowMillis) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(payload));
             InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                builder.append(buffer, 0, read);
            }
            return fromProviderJson(builder.toString(), fallbackLocationId, nowMillis);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static final class QueryResult {
        final boolean queried;
        final BreezyWeatherSnapshot snapshot;

        private QueryResult(boolean queried, BreezyWeatherSnapshot snapshot) {
            this.queried = queried;
            this.snapshot = snapshot != null ? snapshot : BreezyWeatherSnapshot.empty();
        }

        static QueryResult queried(BreezyWeatherSnapshot snapshot) {
            return new QueryResult(true, snapshot);
        }

        static QueryResult notQueried(BreezyWeatherSnapshot cached) {
            return new QueryResult(false, BreezyWeatherSnapshot.failedUsing(cached));
        }
    }

    private static JSONObject parseProviderObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (Throwable ignored) {
            try {
                JSONArray array = new JSONArray(json);
                return array.length() > 0 ? array.optJSONObject(0) : null;
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static JSONObject rootObject(JSONObject envelope) {
        if (envelope == null) {
            return null;
        }
        JSONObject nested = envelope.optJSONObject("weather");
        if (nested != null) {
            return nested;
        }
        nested = envelope.optJSONObject("data");
        return nested != null ? nested : envelope;
    }

    private static boolean hasRecognizedSchema(JSONObject envelope, JSONObject object) {
        if (object == null) {
            return false;
        }
        if (object.length() == 0) {
            return false;
        }
        String[] recognized = {"refreshTime", "refresh_time", "timestamp", "updatedAt",
                "updatedAtMillis", "current", "alerts",
                "dailyForecast", "daily", "forecast", "activeLocationId", "locationId",
                "location_id", "placeId", "place_id", "cityId", "city_id", "latitude",
                "longitude", "lat", "lon", "lng", "currentCondition", "weatherText",
                "condition", "currentTemp", "temperature", "temp", "weatherCode",
                "conditionCode", "currentConditionCode"};
        for (String key : recognized) {
            if (object.has(key) || (envelope != null && envelope.has(key))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFatalSchema(JSONObject object) {
        if (object == null) {
            return true;
        }
        for (String key : new String[]{"alerts", "dailyForecast", "daily"}) {
            if (object.has(key) && object.optJSONArray(key) == null) {
                return true;
            }
        }
        JSONObject forecast = object.optJSONObject("forecast");
        if (object.has("forecast") && forecast == null) {
            return true;
        }
        if (forecast != null) {
            for (String key : new String[]{"daily", "dailyForecast"}) {
                if (forecast.has(key) && forecast.optJSONArray(key) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long sourceUpdatedAtMillis(JSONObject envelope, JSONObject object) {
        // Breezy Weather's WeatherContentProvider publishes refreshTime as the source update
        // timestamp. Keep timestamp/legacy aliases for older relay payloads, but never use the
        // local query time here: forecast freshness is a property of the provider payload.
        String[] keys = {"refreshTime", "refresh_time", "timestamp", "updatedAtMillis",
                "updatedAt", "lastUpdated", "lastUpdatedAt", "updateTime",
                "lastRefreshTime", "lastRefresh"};
        for (String key : keys) {
            long value = BreezyWeatherAlert.parseEpochMillis(
                    envelope != null ? envelope.opt(key) : null);
            if (value > 0L) {
                return value;
            }
            value = BreezyWeatherAlert.parseEpochMillis(
                    object != null ? object.opt(key) : null);
            if (value > 0L) {
                return value;
            }
        }
        return 0L;
    }

    private static List<JSONArray> dailyArrays(JSONObject object) {
        ArrayList<JSONArray> result = new ArrayList<>();
        JSONArray array = object.optJSONArray("dailyForecast");
        if (array != null) {
            result.add(array);
        }
        array = object.optJSONArray("daily");
        if (array != null && !result.contains(array)) {
            result.add(array);
        }
        JSONObject forecast = object.optJSONObject("forecast");
        if (forecast != null) {
            array = forecast.optJSONArray("daily");
            if (array != null) {
                result.add(array);
            }
            array = forecast.optJSONArray("dailyForecast");
            if (array != null) {
                result.add(array);
            }
        }
        return result;
    }

    private static String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private static String locationIdentityFromCoordinates(JSONObject object) {
        String latitude = firstString(object, "latitude", "lat");
        String longitude = firstString(object, "longitude", "lon", "lng");
        String name = firstString(object, "locationName", "city", "name");
        if (latitude.isEmpty() && longitude.isEmpty() && name.isEmpty()) {
            return "";
        }
        return latitude + "," + longitude + "," + name;
    }

    private static List<BreezyWeatherAlert> immutableAlerts(List<BreezyWeatherAlert> input) {
        return input == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(input));
    }

    private static List<BreezyWeatherForecast> immutableForecasts(List<BreezyWeatherForecast> input) {
        return input == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(input));
    }
}
