package dev.codex.pixelaod;

import android.content.Context;

/**
 * Startup-only implementation selection.
 *
 * <p>The value is intentionally cached for the lifetime of the SystemUI process. A settings
 * observer may refresh content, but it must never install a second primary renderer after the
 * process has started.</p>
 */
final class PixelAodFeatureFlags {
    enum UdfpsRenderer {
        COUI_PORT,
        LEGACY
    }

    private static volatile UdfpsRenderer startupUdfpsRenderer = UdfpsRenderer.COUI_PORT;
    private static volatile ClockRendererPolicy startupClockRenderer =
            ClockRendererPolicy.parse(PixelAodSettings.CLOCK_RENDERER_COUI_PORT);

    private PixelAodFeatureFlags() {
    }

    static UdfpsRenderer initialize(Context context) {
        String configured = context == null ? null : PixelAodSettings.getString(
                context,
                PixelAodSettings.KEY_UDFPS_RENDERER,
                PixelAodSettings.UDFPS_RENDERER_COUI_PORT);
        String configuredClock = context == null ? null : PixelAodSettings.getString(
                context,
                PixelAodSettings.KEY_CLOCK_RENDERER,
                PixelAodSettings.CLOCK_RENDERER_COUI_PORT);
        startupUdfpsRenderer = parseUdfpsRenderer(configured);
        startupClockRenderer = parseClockRenderer(configuredClock);
        PixelAodLog.i("COUI UDFPS startup renderer=" + startupUdfpsRenderer
                + " configured=" + (configured == null ? "null" : configured));
        PixelAodLog.i("COUI clock startup renderer=" + startupClockRenderer.mode()
                + " configured=" + (configuredClock == null ? "null" : configuredClock));
        return startupUdfpsRenderer;
    }

    static UdfpsRenderer startupUdfpsRenderer() {
        return startupUdfpsRenderer;
    }

    static boolean useCouiUdfps() {
        return startupUdfpsRenderer == UdfpsRenderer.COUI_PORT;
    }

    static ClockRendererPolicy startupClockRenderer() {
        return startupClockRenderer;
    }

    static boolean useCouiClockRenderer() {
        return startupClockRenderer.useCouiOwner();
    }

    static boolean useLegacyClockRenderer() {
        return startupClockRenderer.useLegacyOwner();
    }

    static ClockRendererPolicy parseClockRenderer(String configured) {
        return ClockRendererPolicy.parse(configured);
    }

    static UdfpsRenderer parseUdfpsRenderer(String configured) {
        if (PixelAodSettings.UDFPS_RENDERER_LEGACY.equalsIgnoreCase(configured)) {
            return UdfpsRenderer.LEGACY;
        }
        return UdfpsRenderer.COUI_PORT;
    }
}
