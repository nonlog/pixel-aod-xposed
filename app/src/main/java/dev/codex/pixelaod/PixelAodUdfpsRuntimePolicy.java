package dev.codex.pixelaod;

import android.content.Context;

/**
 * Runtime ownership facade for UDFPS presentation.
 *
 * <p>The stable release contract is a system-owned primary fingerprint glyph with an optional
 * Pixel AOD success ripple. Optional module replacement and the legacy renderer remain supported;
 * callers use this class so release-mode observation/ripple logic no longer needs to know how
 * replacement settings are stored.</p>
 */
final class PixelAodUdfpsRuntimePolicy {
    private PixelAodUdfpsRuntimePolicy() {
    }

    static boolean usesCouiRenderer() {
        return PixelAodFeatureFlags.useCouiUdfps();
    }

    static boolean replacementRequested(Context context) {
        return context != null && PixelAodSettings.getBoolean(
                context, PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, false);
    }

    static boolean primaryGlyphOwnedBySystem(Context context) {
        return primaryGlyphOwnedBySystem(replacementRequested(context));
    }

    static boolean primaryGlyphOwnedBySystem(boolean replacementRequested) {
        return !replacementRequested;
    }

    static boolean successRippleEnabled(Context context) {
        return context != null && PixelAodSettings.getBoolean(
                context, PixelAodSettings.KEY_UDFPS_SUCCESS_RIPPLE, true);
    }

    static boolean couiOwnsReplacement(Context context) {
        return CouiUdfpsOwnershipPolicy.ownsReplacementVisuals(
                usesCouiRenderer(), replacementRequested(context));
    }

    static boolean couiOwnsSuccessRipple(Context context) {
        return CouiUdfpsOwnershipPolicy.ownsSuccessRipple(
                usesCouiRenderer(), successRippleEnabled(context));
    }
}
