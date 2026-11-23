package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the bundled Google Weather icon pack directly from module APK assets
 * so users do not need to install an external icon-pack APK.
 */
final class BuiltinGoogleWeatherIcons {
    static final String PACK_ID = PixelAodSettingsSchema.WEATHER_ICON_PACK_BUILTIN_GOOGLE;
    static final String ASSET_DIR = "weather_icons/google/";

    private static final Map<String, Bitmap> BITMAP_CACHE = new HashMap<>();

    private BuiltinGoogleWeatherIcons() {
    }

    static boolean isSelected(String packId) {
        return packId == null || packId.isEmpty() || PACK_ID.equals(packId);
    }

    static String resolveAssetName(int weatherCode, boolean isNight) {
        if (weatherCode >= 200 && weatherCode < 300) {
            if (weatherCode == 210 || weatherCode == 211 || weatherCode == 212 || weatherCode == 221) {
                return "weather_thunder";
            }
            return "weather_thunderstorm";
        }
        if (weatherCode == 511) {
            return "weather_sleet_hail";
        }
        if (weatherCode >= 300 && weatherCode < 600) {
            return "weather_rain";
        }
        if (weatherCode >= 611 && weatherCode <= 616) {
            return "weather_sleet_hail";
        }
        if (weatherCode >= 600 && weatherCode < 700) {
            return "weather_snow";
        }
        if (weatherCode >= 700 && weatherCode < 770) {
            return "weather_fog_haze";
        }
        if (weatherCode >= 770 && weatherCode < 800) {
            return "weather_wind";
        }
        if (weatherCode == 800) {
            return isNight ? "weather_clear_night" : "weather_clear_day";
        }
        if (weatherCode == 801 || weatherCode == 802) {
            return isNight ? "weather_partly_cloudy_night" : "weather_partly_cloudy_day";
        }
        if (weatherCode == 803 || weatherCode == 804) {
            return "weather_cloudy";
        }
        return isNight ? "weather_clear_night" : "weather_clear_day";
    }

    static Drawable loadDrawable(Context context, int weatherCode, boolean isNight) {
        if (context == null) {
            return null;
        }
        String assetName = resolveAssetName(weatherCode, isNight);
        Bitmap bitmap = loadBitmap(context, assetName);
        if (bitmap == null) {
            return null;
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static Bitmap loadBitmap(Context context, String assetName) {
        synchronized (BITMAP_CACHE) {
            Bitmap cached = BITMAP_CACHE.get(assetName);
            if (cached != null && !cached.isRecycled()) {
                return cached;
            }
        }
        Context assetContext = resolveModuleContext(context);
        if (assetContext == null) {
            return null;
        }
        String path = ASSET_DIR + assetName + ".png";
        try (InputStream stream = assetContext.getAssets().open(path)) {
            Bitmap decoded = BitmapFactory.decodeStream(stream);
            if (decoded != null) {
                synchronized (BITMAP_CACHE) {
                    BITMAP_CACHE.put(assetName, decoded);
                }
            }
            return decoded;
        } catch (Throwable t) {
            PixelAodLog.e("Failed to load built-in Google weather icon " + path, t);
            return null;
        }
    }

    private static Context resolveModuleContext(Context context) {
        try {
            if ("dev.codex.pixelaod".equals(context.getPackageName())) {
                return context;
            }
            return context.createPackageContext(
                    "dev.codex.pixelaod",
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (Throwable t) {
            PixelAodLog.e("Failed to open module context for built-in weather icons", t);
            return context;
        }
    }
}
