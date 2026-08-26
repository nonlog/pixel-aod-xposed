package dev.codex.pixelaod;

import android.content.Context;

/** Resolves a restrained Material You surface for the transient Pixel Peek card. */
final class PixelPeekMaterialYouPalette {
    private static final int FALLBACK_BACKGROUND = 0xff283030;
    private static final float PRIMARY_TINT_FRACTION = 0.10f;
    private static final int MAX_CHANNEL = 56;

    private PixelPeekMaterialYouPalette() {
    }

    static int background(Context context) {
        if (context == null) {
            return FALLBACK_BACKGROUND;
        }
        int neutral = resolveSystemColor(context, "system_neutral1_800", FALLBACK_BACKGROUND);
        int primary = resolveSystemColor(context, "system_accent1_800", neutral);
        int resolved = blendAndLimit(neutral, primary, PRIMARY_TINT_FRACTION, MAX_CHANNEL);
        PixelAodLog.log("Pixel peek Material You background"
                + " neutral=#" + hexRgb(neutral)
                + " primary=#" + hexRgb(primary)
                + " resolved=#" + hexRgb(resolved)
                + " tint=" + PRIMARY_TINT_FRACTION);
        return resolved;
    }

    static int blendAndLimit(int neutral, int primary, float fraction, int maxChannel) {
        float amount = clamp01(fraction);
        int red = blendChannel(red(neutral), red(primary), amount);
        int green = blendChannel(green(neutral), green(primary), amount);
        int blue = blendChannel(blue(neutral), blue(primary), amount);
        int maximum = Math.max(red, Math.max(green, blue));
        int boundedMax = Math.max(1, maxChannel);
        if (maximum > boundedMax) {
            float scale = boundedMax / (float) maximum;
            red = Math.round(red * scale);
            green = Math.round(green * scale);
            blue = Math.round(blue * scale);
        }
        return argb(255, red, green, blue);
    }

    private static int resolveSystemColor(Context context, String name, int fallback) {
        String[] packages = {"android", context.getPackageName(), "com.android.systemui"};
        for (String packageName : packages) {
            try {
                int id = context.getResources().getIdentifier(name, "color", packageName);
                if (id != 0) {
                    return context.getColor(id);
                }
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private static int blendChannel(int start, int end, float amount) {
        return clampChannel(Math.round(start + ((end - start) * amount)));
    }

    private static int red(int color) {
        return (color >>> 16) & 0xff;
    }

    private static int green(int color) {
        return (color >>> 8) & 0xff;
    }

    private static int blue(int color) {
        return color & 0xff;
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (clampChannel(alpha) << 24)
                | (clampChannel(red) << 16)
                | (clampChannel(green) << 8)
                | clampChannel(blue);
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static String hexRgb(int color) {
        return String.format(java.util.Locale.US, "%06X", color & 0x00ffffff);
    }
}
