package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/** Loads the module-owned Google Sans Flex asset with the requested variable-font axes. */
final class CouiClockFontLoader {
    private static final String MODULE_PACKAGE_NAME = "dev.codex.pixelaod";
    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private CouiClockFontLoader() {
    }

    static Typeface buildCustomFont(Context context, String variation) {
        if (context == null || variation == null) {
            return null;
        }
        synchronized (CACHE) {
            if (CACHE.containsKey(variation)) {
                return CACHE.get(variation);
            }
        }
        Typeface typeface = null;
        try {
            Context assetContext = context;
            try {
                assetContext = context.createPackageContext(MODULE_PACKAGE_NAME,
                        Context.CONTEXT_IGNORE_SECURITY);
            } catch (Throwable ignored) {
                // A local/unit context may not expose the installed module package.
            }
            typeface = new Typeface.Builder(assetContext.getAssets(),
                    CouiClockFontPolicy.FONT_ASSET_PATH)
                    .setFontVariationSettings(variation)
                    .build();
        } catch (Throwable t) {
            PixelAodLog.log("COUI Google Sans Flex font unavailable variation=" + variation, t);
        }
        synchronized (CACHE) {
            CACHE.put(variation, typeface);
        }
        return typeface;
    }
}
