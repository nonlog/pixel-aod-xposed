package dev.codex.pixelaod;

import android.content.Context;
import android.content.res.Resources;

/** Current-SystemUI resource adapter for AOD/lockscreen notification capacity and overflow dots. */
final class NativeNotificationIconPresentationPolicy {
    static final int FALLBACK_MAX_ICONS = 3;
    static final int FALLBACK_DOT_RADIUS_DP = 2;
    static final int FALLBACK_DOT_PADDING_DP = 3;

    private NativeNotificationIconPresentationPolicy() {
    }

    static Snapshot resolve(Context context, boolean dozing) {
        int maxIcons = readInteger(context,
                dozing ? "max_notif_icons_on_aod" : "max_notif_icons_on_lockscreen",
                FALLBACK_MAX_ICONS);
        int dotRadius = readDimensionPx(context, "overflow_dot_radius",
                dp(context, FALLBACK_DOT_RADIUS_DP));
        int dotPadding = readDimensionPx(context, "overflow_icon_dot_padding",
                dp(context, FALLBACK_DOT_PADDING_DP));
        return new Snapshot(Math.max(1, maxIcons), Math.max(1, dotRadius),
                Math.max(0, dotPadding));
    }

    private static int readInteger(Context context, String name, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            Resources resources = context.getResources();
            int id = resources.getIdentifier(name, "integer", "com.android.systemui");
            return id != 0 ? resources.getInteger(id) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int readDimensionPx(Context context, String name, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            Resources resources = context.getResources();
            int id = resources.getIdentifier(name, "dimen", "com.android.systemui");
            return id != 0 ? resources.getDimensionPixelSize(id) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int dp(Context context, int value) {
        float density = context != null ? context.getResources().getDisplayMetrics().density : 1f;
        return Math.max(1, Math.round(value * density));
    }

    static final class Snapshot {
        final int maxVisibleIcons;
        final int dotRadiusPx;
        final int dotPaddingPx;

        Snapshot(int maxVisibleIcons, int dotRadiusPx, int dotPaddingPx) {
            this.maxVisibleIcons = Math.max(1, maxVisibleIcons);
            this.dotRadiusPx = Math.max(1, dotRadiusPx);
            this.dotPaddingPx = Math.max(0, dotPaddingPx);
        }

        int dotDiameterPx() {
            return dotRadiusPx * 2;
        }
    }
}