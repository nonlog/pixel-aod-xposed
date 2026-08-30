package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Renders the one-line contextual card into the stable calendar-row geometry. */
final class ContextualAtAGlancePresentation {
    private static final long REPLACEMENT_HALF_FADE_MILLIS =
            ContextualAtAGlanceCard.REPLACEMENT_CROSSFADE_MILLIS / 2L;
    private static final Map<LinearLayout, ContextualAtAGlanceCard> DISPLAYED_CARDS =
            Collections.synchronizedMap(new WeakHashMap<>());

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
        return apply(context, row, icon, text, next, infoColor, clockColor, textSizeDp,
                infoWeight, animate, null, source);
    }

    /**
     * Same presentation contract with an optional callback fired only when the rendered pixels
     * actually switch cards. Target state is still published before the fade for lower-row layout.
     */
    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, boolean animate, Runnable onDisplayedContentChanged, String source) {
        if (row == null || icon == null || text == null) {
            return false;
        }
        animate = SystemAnimationScalePolicy.shouldAnimate(animate);
        ContextualAtAGlanceCard safe = next != null
                ? next : ContextualAtAGlanceCard.none();
        ContextualAtAGlanceCard previous = current(row);
        ensureDisplayedCard(row, previous);
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
            setDisplayedCard(row, safe);
            int color = visualColor(safe, infoColor, clockColor);
            text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
            PixelAodTypography.applySharedClockTypeface(text, context, infoWeight);
            PixelAodTypography.applySharedInfoText(text, context, safe.text);
            Drawable drawable = PixelAodContentState.contextualCardIcon(context, safe, color);
            icon.setImageDrawable(drawable);
            icon.setVisibility(drawable != null && safe.isVisible() ? View.VISIBLE : View.GONE);
            applyVisualStyle(icon, text, safe, infoColor, clockColor);
            float alpha = safe.isVisible() ? safe.alpha : 0f;
            icon.setAlpha(alpha);
            text.setAlpha(alpha);
            // Commit geometry after the new text/icon are prepared but before the row can reveal
            // them. Otherwise a NONE -> forecast first frame can inherit the old START X and
            // visibly jump to the Large centered target on the next posted target pass.
            if (onDisplayedContentChanged != null) {
                onDisplayedContentChanged.run();
            }
            row.setVisibility(safe.isVisible() ? View.VISIBLE : View.GONE);
            row.setAlpha(safe.isVisible() ? 1f : 0f);
        };

        if (!animate) {
            applyContent.run();
            PixelAodLog.log("updated contextual card kind=" + safe.kind
                    + " textPresent=" + !safe.text.isEmpty()
                    + " source=" + source + " animate=false");
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
                + " textPresent=" + !safe.text.isEmpty() + " source=" + source);
        return true;
    }

    /** Re-applies colour styling to the pixels that are actually still displayed on this row. */
    static void restyleDisplayed(LinearLayout row, ImageView icon, TextView text,
            int infoColor, int clockColor) {
        if (row == null || icon == null || text == null) {
            return;
        }
        applyVisualStyle(icon, text, displayed(row), infoColor, clockColor);
    }

    static ContextualAtAGlanceCard displayed(LinearLayout row) {
        if (row == null) {
            return ContextualAtAGlanceCard.none();
        }
        ContextualAtAGlanceCard displayed = DISPLAYED_CARDS.get(row);
        return displayed != null ? displayed : current(row);
    }

    private static void ensureDisplayedCard(LinearLayout row, ContextualAtAGlanceCard fallback) {
        if (row == null) {
            return;
        }
        synchronized (DISPLAYED_CARDS) {
            if (!DISPLAYED_CARDS.containsKey(row)) {
                DISPLAYED_CARDS.put(row,
                        fallback != null ? fallback : ContextualAtAGlanceCard.none());
            }
        }
    }

    private static void setDisplayedCard(LinearLayout row, ContextualAtAGlanceCard card) {
        if (row != null) {
            DISPLAYED_CARDS.put(row,
                    card != null ? card : ContextualAtAGlanceCard.none());
        }
    }

    private static int visualColor(ContextualAtAGlanceCard card, int infoColor, int clockColor) {
        ContextualAtAGlanceCard safe = card != null ? card : ContextualAtAGlanceCard.none();
        if (safe.kind == ContextualAtAGlanceCard.Kind.WEATHER_FORECAST) {
            return Color.WHITE;
        }
        return safe.kind == ContextualAtAGlanceCard.Kind.WEATHER_ALERT ? clockColor : infoColor;
    }

    private static void applyVisualStyle(ImageView icon, TextView text,
            ContextualAtAGlanceCard card, int infoColor, int clockColor) {
        ContextualAtAGlanceCard safe = card != null ? card : ContextualAtAGlanceCard.none();
        int color = visualColor(safe, infoColor, clockColor);
        text.setTextColor(color);
        if (safe.kind == ContextualAtAGlanceCard.Kind.WEATHER_FORECAST) {
            // Forecast is the sole owner of source-colour artwork. Never allow a target-card
            // accent refresh to tint forecast pixels that are still visible during a crossfade.
            icon.setImageTintList(null);
            icon.clearColorFilter();
        } else {
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
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
        if (!SystemAnimationScalePolicy.animationsEnabled()) {
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
