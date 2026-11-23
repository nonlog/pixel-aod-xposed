package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;

public class BuiltinGoogleWeatherIconsAndSourceTest {

    @Test
    public void builtinGoogleIconsMapToAllTwelveBundledAssets() {
        String[] expectedAssets = {
                BuiltinGoogleWeatherIcons.resolveAssetName(800, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(800, true),
                BuiltinGoogleWeatherIcons.resolveAssetName(801, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(802, true),
                BuiltinGoogleWeatherIcons.resolveAssetName(804, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(500, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(511, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(600, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(741, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(771, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(211, false),
                BuiltinGoogleWeatherIcons.resolveAssetName(200, false)
        };
        assertEquals("weather_clear_day", expectedAssets[0]);
        assertEquals("weather_clear_night", expectedAssets[1]);
        assertEquals("weather_partly_cloudy_day", expectedAssets[2]);
        assertEquals("weather_partly_cloudy_night", expectedAssets[3]);
        assertEquals("weather_cloudy", expectedAssets[4]);
        assertEquals("weather_rain", expectedAssets[5]);
        assertEquals("weather_sleet_hail", expectedAssets[6]);
        assertEquals("weather_snow", expectedAssets[7]);
        assertEquals("weather_fog_haze", expectedAssets[8]);
        assertEquals("weather_wind", expectedAssets[9]);
        assertEquals("weather_thunder", expectedAssets[10]);
        assertEquals("weather_thunderstorm", expectedAssets[11]);

        File assetDir = new File("src/main/assets/weather_icons/google");
        if (!assetDir.isDirectory()) {
            assetDir = new File("app/src/main/assets/weather_icons/google");
        }
        assertTrue("Bundled Google icons directory should exist", assetDir.isDirectory());
        for (String name : expectedAssets) {
            File png = new File(assetDir, name + ".png");
            assertTrue("Missing bundled icon: " + png.getPath(), png.isFile() && png.length() > 100);
        }
    }

    @Test
    public void systemWeatherMapsOplusCodesAndConditionText() {
        assertEquals(800, SystemWeatherProvider.mapOplusWeatherCodeToOwm(0, ""));
        assertEquals(801, SystemWeatherProvider.mapOplusWeatherCodeToOwm(1, ""));
        assertEquals(804, SystemWeatherProvider.mapOplusWeatherCodeToOwm(2, ""));
        assertEquals(211, SystemWeatherProvider.mapOplusWeatherCodeToOwm(4, ""));
        assertEquals(611, SystemWeatherProvider.mapOplusWeatherCodeToOwm(6, ""));
        assertEquals(500, SystemWeatherProvider.mapOplusWeatherCodeToOwm(7, ""));
        assertEquals(600, SystemWeatherProvider.mapOplusWeatherCodeToOwm(13, ""));
        assertEquals(741, SystemWeatherProvider.mapOplusWeatherCodeToOwm(18, ""));
        assertEquals(511, SystemWeatherProvider.mapOplusWeatherCodeToOwm(19, ""));
        assertEquals(771, SystemWeatherProvider.mapOplusWeatherCodeToOwm(20, ""));
        assertEquals(721, SystemWeatherProvider.mapOplusWeatherCodeToOwm(53, ""));
        assertEquals(500, SystemWeatherProvider.mapOplusWeatherCodeToOwm(500, ""));
        assertEquals(211, SystemWeatherProvider.mapOplusWeatherCodeToOwm(Integer.MIN_VALUE, "雷阵雨"));
        assertEquals(800, SystemWeatherProvider.mapOplusWeatherCodeToOwm(Integer.MIN_VALUE, "晴"));
        assertEquals("23°", SystemWeatherProvider.formatSystemTemperature("23.4℃"));
        assertEquals("-4°", SystemWeatherProvider.formatSystemTemperature("-4°C"));
        assertEquals("", SystemWeatherProvider.formatSystemTemperature("null"));
    }

    @Test
    public void weatherSourceSelectionRespectsAutoSystemAndBreezyModes() {
        assertEquals(SystemWeatherProvider.SOURCE_BREEZY, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_AUTO, true, true));
        assertEquals(SystemWeatherProvider.SOURCE_SYSTEM, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_AUTO, false, true));
        assertEquals(SystemWeatherProvider.SOURCE_NONE, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_AUTO, false, false));
        assertEquals(SystemWeatherProvider.SOURCE_SYSTEM, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_SYSTEM, true, true));
        assertEquals(SystemWeatherProvider.SOURCE_NONE, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_SYSTEM, true, false));
        assertEquals(SystemWeatherProvider.SOURCE_BREEZY, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_BREEZY, true, true));
        assertEquals(SystemWeatherProvider.SOURCE_NONE, SystemWeatherProvider.selectWeatherSource(
                PixelAodSettingsSchema.WEATHER_DATA_SOURCE_BREEZY, false, true));
        assertEquals(PixelAodSettingsSchema.WEATHER_DATA_SOURCE_AUTO,
                PixelAodSettingsSchema.stringDefault(PixelAodSettingsSchema.KEY_WEATHER_DATA_SOURCE, "wrong"));
        assertTrue(BuiltinGoogleWeatherIcons.isSelected(""));
        assertTrue(BuiltinGoogleWeatherIcons.isSelected(null));
        assertTrue(BuiltinGoogleWeatherIcons.isSelected(PixelAodSettingsSchema.WEATHER_ICON_PACK_BUILTIN_GOOGLE));
        assertFalse(BuiltinGoogleWeatherIcons.isSelected("org.example.otherpack"));
    }
}
