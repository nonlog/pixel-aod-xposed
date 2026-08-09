package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ContextualAtAGlancePermissionTest {
    @Test
    public void remembersOnlyTheFeatureThatRequestedBreezyPermission() {
        assertEquals(PixelAodSettings.KEY_WEATHER_FORECAST,
                ContextualAtAGlancePermission.normalizeFeatureKey(
                        PixelAodSettings.KEY_WEATHER_FORECAST));
        assertEquals(PixelAodSettings.KEY_WEATHER_ALERTS,
                ContextualAtAGlancePermission.normalizeFeatureKey(
                        PixelAodSettings.KEY_WEATHER_ALERTS));
        assertTrue(ContextualAtAGlancePermission.isWeatherFeature(
                PixelAodSettings.KEY_WEATHER_FORECAST));
        assertTrue(ContextualAtAGlancePermission.isWeatherFeature(
                PixelAodSettings.KEY_WEATHER_ALERTS));
        assertFalse(ContextualAtAGlancePermission.isWeatherFeature("other"));
    }
}
