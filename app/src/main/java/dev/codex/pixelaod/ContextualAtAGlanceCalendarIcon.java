package dev.codex.pixelaod;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/** Shared calendar icon resolution and geometry for the lockscreen/AOD contextual slot. */
final class ContextualAtAGlanceCalendarIcon {
    static final int APPLICATION_ICON_SIZE_DP = 22;
    static final float APPLICATION_ICON_SCALE = 1.25f;
    static final int APPLICATION_ICON_LEADING_OFFSET_DP = 6;

    private ContextualAtAGlanceCalendarIcon() {
    }

    static int iconSizeDp(int textSizeDp, boolean applicationIcon) {
        return applicationIcon ? Math.max(textSizeDp, APPLICATION_ICON_SIZE_DP) : textSizeDp;
    }

    static float iconScale(boolean applicationIcon) {
        return applicationIcon ? APPLICATION_ICON_SCALE : 1f;
    }

    static int leadingOffsetDp(boolean applicationIcon) {
        return applicationIcon ? APPLICATION_ICON_LEADING_OFFSET_DP : 0;
    }

    static boolean usesApplicationIcon(Context context) {
        return PixelAodContentState.hasSelectedCalendarApplicationIcon(context);
    }

    static void applyGeometry(ImageView icon, Context context, int textSizeDp,
            boolean applicationIcon) {
        if (icon == null || context == null) {
            return;
        }
        int sizePx = dp(context, iconSizeDp(textSizeDp, applicationIcon));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) icon.getLayoutParams();
        if (params != null && (params.width != sizePx || params.height != sizePx)) {
            params.width = sizePx;
            params.height = sizePx;
            icon.setLayoutParams(params);
        }
        float scale = iconScale(applicationIcon);
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setTranslationX(0f);
    }

    static void applyRowMargins(FrameLayout.LayoutParams params, Context context, int baseEdgeDp,
            boolean applicationIcon) {
        if (params == null || context == null) {
            return;
        }
        int leadingOffset = dp(context, leadingOffsetDp(applicationIcon));
        int baseEdge = dp(context, baseEdgeDp);
        params.leftMargin = baseEdge - leadingOffset;
        params.rightMargin = baseEdge + leadingOffset;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
