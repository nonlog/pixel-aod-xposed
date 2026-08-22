package dev.codex.pixelaod;

import android.content.Context;

/**
 * Startup-only implementation selection for the independent UDFPS path.
 *
 * <p>The primary clock owner is no longer selectable: M8 converges SystemUI startup on the
 * validated COUI clock owner. Version-level rollback remains available through the stable release
 * history instead of installing a second runtime architecture.</p>
 */
final class PixelAodFeatureFlags {
    enum UdfpsRenderer {
        COUI_PORT,
        LEGACY
    }

    private static volatile UdfpsRenderer startupUdfpsRenderer = UdfpsRenderer.COUI_PORT;

    private PixelAodFeatureFlags() {
    }

    static UdfpsRenderer initialize(Context context) {
        String configured = context == null ? null : PixelAodSettings.getString(
                context,
                PixelAodSettings.KEY_UDFPS_RENDERER,
                PixelAodSettings.UDFPS_RENDERER_COUI_PORT);
        startupUdfpsRenderer = parseUdfpsRenderer(configured);
        PixelAodLog.i("COUI UDFPS startup renderer=" + startupUdfpsRenderer
                + " configured=" + (configured == null ? "null" : configured));
        PixelAodLog.i("COUI clock startup owner=COUI_PORT fixed=true");
        return startupUdfpsRenderer;
    }

    static UdfpsRenderer startupUdfpsRenderer() {
        return startupUdfpsRenderer;
    }

    static boolean useCouiUdfps() {
        return startupUdfpsRenderer == UdfpsRenderer.COUI_PORT;
    }

    static UdfpsRenderer parseUdfpsRenderer(String configured) {
        if (PixelAodSettings.UDFPS_RENDERER_LEGACY.equalsIgnoreCase(configured)) {
            return UdfpsRenderer.LEGACY;
        }
        return UdfpsRenderer.COUI_PORT;
    }
}
