package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Renders the one-line contextual card into the stable calendar-row geometry. */
final class ContextualAtAGlancePresentation {
    private static final long REPLACEMENT_HALF_FADE_MILLIS =
            ContextualAtAGlanceCard.REPLACEMENT_CROSSFADE_MILLIS / 2L;

    private ContextualAtAGlancePresentation() {
    }

    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, String source) {
        return apply(context, row, icon, text, next, infoColor, clockColor, textSizeDp,
                infoWeight, true, source);
    }

    /** Same presentation contract with an explicit first-frame animation gate for COUI_PORT. */
    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, boolean animate, String source) {
        if (row == null || icon == null || text == null) {
            return false;
        }
        ContextualAtAGlanceCard safe = next != null
                ? next : ContextualAtAGlanceCard.none();
        ContextualAtAGlanceCard previous = current(row);
        if (previous.sameContent(safe)) {
            return false;
        }

        boolean hadCard = previous.isVisible();
        boolean hasCard = safe.isVisible();
        boolean replacement = hadCard && hasCard;
        // Publish the target card before the animation starts. The layout owner can therefore
        // calculate the target lower-row positions during a leave animation while the card row
        // remains visible for its fade-out.
        row.animate().cancel();
        row.setTag(safe);
        Runnable applyContent = () -> {
            row.setTag(safe);
            int color = safe.kind == ContextualAtAGlanceCard.Kind.WEATHER_ALERT
                    ? clockColor : infoColor;
            text.setTextColor(color);
            text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
            PixelAodClockView.applySharedClockTypeface(text, context, infoWeight);
            PixelAodClockView.applySharedInfoText(text, context, safe.text);
            Drawable drawable = PixelAodClockView.contextualCardIcon(context, safe, color);
            icon.setImageDrawable(drawable);
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            float alpha = safe.isVisible() ? safe.alpha : 0f;
            icon.setAlpha(alpha);
            text.setAlpha(alpha);
            row.setVisibility(safe.isVisible() ? View.VISIBLE : View.GONE);
            row.setAlpha(safe.isVisible() ? 1f : 0f);
        };

        if (!animate) {
            applyContent.run();
            PixelAodLog.log("updated contextual card kind=" + safe.kind
                    + " text=" + safe.text + " source=" + source + " animate=false");
            return true;
        }

        if (!hadCard && hasCard) {
            applyContent.run();
            row.setAlpha(0f);
            row.animate().alpha(1f).setDuration(ContextualAtAGlanceCard.ENTER_LEAVE_FADE_MILLIS)
                    .start();
        } else if (hadCard && !hasCard) {
            row.animate().alpha(0f).setDuration(ContextualAtAGlanceCard.ENTER_LEAVE_FADE_MILLIS)
                    .withEndAction(applyContent).start();
        } else {
            row.animate().alpha(0f).setDuration(REPLACEMENT_HALF_FADE_MILLIS)
                    .withEndAction(() -> {
                        applyContent.run();
                        row.setAlpha(0f);
                        row.animate().alpha(1f).setDuration(REPLACEMENT_HALF_FADE_MILLIS)
                                .start();
                    }).start();
        }
        PixelAodLog.log("updated contextual card kind=" + safe.kind
                + " text=" + safe.text + " source=" + source);
        return true;
    }

    /** Returns the remaining vertical offset for a lower row at a normalized transition point. */
    static float lowerRowTranslationAtProgress(int oldTopPx, int targetTopPx, float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        return (oldTopPx - targetTopPx) * (1f - clamped);
    }

    /** Coordinates lower notification/media rows with the card enter/leave fade. */
    static void animateLowerRows(View notificationRow, View mediaRow,
            int oldNotificationTopPx, int targetNotificationTopPx,
            int oldMediaTopPx, int targetMediaTopPx) {
        animateLowerRow(notificationRow, oldNotificationTopPx, targetNotificationTopPx);
        animateLowerRow(mediaRow, oldMediaTopPx, targetMediaTopPx);
    }

    private static void animateLowerRow(View row, int oldTopPx, int targetTopPx) {
        if (row == null) {
            return;
        }
        if (row.getVisibility() != View.VISIBLE) {
            row.animate().cancel();
            row.setTranslationY(0f);
            return;
        }
        float start = row.getTranslationY()
                + lowerRowTranslationAtProgress(oldTopPx, targetTopPx, 0f);
        row.animate().cancel();
        if (Math.abs(start) < 0.5f) {
            row.setTranslationY(0f);
            return;
        }
        row.setTranslationY(start);
        row.animate().translationY(0f)
                .setDuration(ContextualAtAGlanceCard.ENTER_LEAVE_FADE_MILLIS)
                .start();
    }

    static ContextualAtAGlanceCard current(LinearLayout row) {
        Object tag = row != null ? row.getTag() : null;
        return tag instanceof ContextualAtAGlanceCard
                ? (ContextualAtAGlanceCard) tag : ContextualAtAGlanceCard.none();
    }
}
