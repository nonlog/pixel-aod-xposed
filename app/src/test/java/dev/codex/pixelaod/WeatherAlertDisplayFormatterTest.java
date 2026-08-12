package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WeatherAlertDisplayFormatterTest {
    @Test
    public void formatsChineseRainstormAlertsFromStructuredSeverity() {
        assertEquals("Blue alert for rainstorms", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("中原发布暴雨蓝色预警", 0L, 0L, 1)));
        assertEquals("Red alert for rainstorms", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("中原发布暴雨红色预警", 0L, 0L, 4)));
    }

    @Test
    public void prefersStructuredSeverityOverColorWordsInHeadline() {
        assertEquals("Orange alert for extreme heat", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("郑州市气象台发布高温蓝色预警", 0L, 0L, 3)));
    }

    @Test
    public void usesLongestHazardMatch() {
        assertEquals("Red alert for extreme torrential rain", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("气象台发布特大暴雨红色预警", 0L, 0L, 4)));
        assertEquals("Yellow alert for severe thunderstorms and strong winds",
                WeatherAlertDisplayFormatter.format(BreezyWeatherAlert.forFields(
                        "气象台发布雷暴大风黄色预警", 0L, 0L, 2)));
    }

    @Test
    public void fallsBackToHeadlineColorOnlyWhenSeverityIsUnknown() {
        assertEquals("Yellow alert for fog", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("城区发布大雾黄色预警", 0L, 0L, 0)));
    }

    @Test
    public void preservesEnglishAndUnknownChineseSourceText() {
        assertEquals("Severe thunderstorm warning", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("Severe thunderstorm warning", 0L, 0L, 3)));
        assertEquals("某地发布未知新型预警", WeatherAlertDisplayFormatter.format(
                BreezyWeatherAlert.forFields("某地发布未知新型预警", 0L, 0L, 2)));
    }

    @Test
    public void mapsSeverityToProgressivelyStrongerIconLevels() {
        assertEquals(WeatherAlertVisuals.IconLevel.UNKNOWN, WeatherAlertVisuals.iconLevel(0));
        assertEquals(WeatherAlertVisuals.IconLevel.MINOR, WeatherAlertVisuals.iconLevel(1));
        assertEquals(WeatherAlertVisuals.IconLevel.MODERATE, WeatherAlertVisuals.iconLevel(2));
        assertEquals(WeatherAlertVisuals.IconLevel.SEVERE, WeatherAlertVisuals.iconLevel(3));
        assertEquals(WeatherAlertVisuals.IconLevel.EXTREME, WeatherAlertVisuals.iconLevel(4));
    }
}
