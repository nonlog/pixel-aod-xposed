package dev.codex.pixelaod;

/** Pure mapping for the shared Breezy permission request used by contextual weather features. */
final class ContextualAtAGlancePermission {
    static final String WEATHER_ALERTS = PixelAodSettings.KEY_WEATHER_ALERTS;
    static final String WEATHER_FORECAST = PixelAodSettings.KEY_WEATHER_FORECAST;

    private ContextualAtAGlancePermission() {
    }

    static String normalizeFeatureKey(String key) {
        if (WEATHER_ALERTS.equals(key)) {
            return WEATHER_ALERTS;
        }
        if (WEATHER_FORECAST.equals(key)) {
            return WEATHER_FORECAST;
        }
        return "";
    }

    static boolean isWeatherFeature(String key) {
        return !normalizeFeatureKey(key).isEmpty();
    }
}
