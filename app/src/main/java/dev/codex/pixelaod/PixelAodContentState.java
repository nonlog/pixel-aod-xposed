package dev.codex.pixelaod;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.time.ZoneId;
import java.util.List;

/**
 * Presentation-facing content facade for weather, contextual cards and notification semantics.
 *
 * <p>The underlying state is intentionally left in the proven 0.1.380 implementation during M8;
 * callers no longer need to treat {@link PixelAodClockView} as the content repository.</p>
 */
final class PixelAodContentState {
    private static final long FORECAST_REQUERY_MIN_INTERVAL_MILLIS = 60_000L;
    private static long lastForecastRequeryAtMillis;

    private PixelAodContentState() {
    }

    static void ensureBreezyWeatherReceiver(Context context) {
        PixelAodClockView.ensureBreezyWeatherReceiver(context);
    }

    static PixelAodClockView.WeatherSnapshot currentFreshWeather(Context context) {
        return PixelAodClockView.currentFreshWeather(context);
    }

    static Drawable resolveWeatherIconDrawable(Context context,
            PixelAodClockView.WeatherSnapshot weather, int fallbackColor) {
        return PixelAodClockView.resolveWeatherIconDrawable(context, weather, fallbackColor);
    }

    static ContextualAtAGlanceSelector.Selection selectContextualCard(Context context,
            boolean surfaceVisible, String source) {
        return PixelAodClockView.selectContextualCard(context, surfaceVisible, source);
    }

    static ContextualAtAGlanceSelector.Selection selectContextualCard(Context context,
            boolean surfaceVisible, boolean allowWeatherAlerts, String source) {
        return PixelAodClockView.selectContextualCard(
                context, surfaceVisible, allowWeatherAlerts, source);
    }

    /**
     * Reconciles the contextual forecast cache with Breezy once the COUI host has entered AOD presentation
     * inside the configured forecast window but arbitration has no card to render. The request is
     * explicit-package and throttled; the receiver owns the provider query and existing relay path.
     */
    static void maybeRequestForecastSnapshot(Context context, boolean aodPresentationActive,
            ContextualAtAGlanceCard selectedCard, String source) {
        if (context == null || !aodPresentationActive
                || (selectedCard != null && selectedCard.isVisible())) {
            return;
        }
        boolean enabled = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_WEATHER_FORECAST, false);
        String startTime = PixelAodSettings.getString(context,
                PixelAodSettings.KEY_WEATHER_FORECAST_START_TIME,
                ForecastDisplayWindow.DEFAULT_START_TIME);
        String endTime = PixelAodSettings.getString(context,
                PixelAodSettings.KEY_WEATHER_FORECAST_END_TIME,
                ForecastDisplayWindow.DEFAULT_END_TIME);
        ForecastDisplayWindow window = ForecastDisplayWindow.fromSettings(startTime, endTime);
        long nowMillis = System.currentTimeMillis();
        if (!AtAGlanceWeatherPolicy.forecastWindowActive(
                nowMillis, ZoneId.systemDefault(), enabled, window)) {
            return;
        }

        synchronized (PixelAodContentState.class) {
            if (lastForecastRequeryAtMillis > 0L
                    && nowMillis - lastForecastRequeryAtMillis
                    < FORECAST_REQUERY_MIN_INTERVAL_MILLIS) {
                return;
            }
            lastForecastRequeryAtMillis = nowMillis;
        }
        try {
            Intent request = new Intent(BreezyWeatherRelayReceiver.ACTION_REQUEST_RELAY)
                    .setPackage(AodNotificationPipeline.MODULE_PACKAGE)
                    .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(request);
            PixelAodLog.log("requested Breezy forecast snapshot source="
                    + (source != null ? source : "unknown"));
        } catch (Throwable t) {
            synchronized (PixelAodContentState.class) {
                if (lastForecastRequeryAtMillis == nowMillis) {
                    lastForecastRequeryAtMillis = 0L;
                }
            }
            PixelAodLog.log("failed to request Breezy forecast snapshot", t);
        }
    }

    static long beginContextualSurfaceEntry(String source) {
        return PixelAodClockView.beginContextualSurfaceEntry(source);
    }

    static long currentContextualSurfaceEntry() {
        return PixelAodClockView.currentContextualSurfaceEntry();
    }

    static Drawable contextualCardIcon(Context context, ContextualAtAGlanceCard card, int color) {
        return PixelAodClockView.contextualCardIcon(context, card, color);
    }

    static boolean hasSelectedCalendarApplicationIcon(Context context) {
        return PixelAodClockView.hasSelectedCalendarApplicationIcon(context);
    }

    static void setCalendarAtAGlanceExtra(String extra, String source) {
        PixelAodClockView.setCalendarAtAGlanceExtra(extra, source);
    }

    static List<Drawable> currentCouiNotificationIcons(Context context) {
        return PixelAodClockView.currentCouiNotificationIcons(context);
    }

    static void resetSelectedUserContentState(String source) {
        PixelAodClockView.resetSelectedUserContentState(source);
    }
}
