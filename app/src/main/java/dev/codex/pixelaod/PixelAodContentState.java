package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.List;

/**
 * Presentation-facing content facade for weather, contextual cards and notification semantics.
 *
 * <p>The underlying state is intentionally left in the proven 0.1.380 implementation during M8;
 * callers no longer need to treat {@link PixelAodClockView} as the content repository.</p>
 */
final class PixelAodContentState {
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
