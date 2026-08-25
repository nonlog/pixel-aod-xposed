package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Renders the one-line contextual card into the stable calendar-row geometry. */
final class ContextualAtAGlancePresentation {
    private static final int LIVE_UPDATE_ICON_SIZE_DP = 20;
    private static final int LIVE_UPDATE_METRIC_GAP_DP = 8;
    private static final long REPLACEMENT_HALF_FADE_MILLIS =
            ContextualAtAGlanceCard.REPLACEMENT_CROSSFADE_MILLIS / 2L;

    private ContextualAtAGlancePresentation() {
    }

    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, String source) {
        return apply(context, row, icon, text, null, null, next, infoColor, clockColor,
                textSizeDp, infoWeight, true, source);
    }

    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            StructuredLiveUpdateTextView metric, LiveUpdateProgressView progress,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, String source) {
        return apply(context, row, icon, text, metric, progress, next, infoColor, clockColor,
                textSizeDp, infoWeight, true, source);
    }

    /** Same presentation contract with an explicit first-frame animation gate for COUI_PORT. */
    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, boolean animate, String source) {
        return apply(context, row, icon, text, null, null, next, infoColor, clockColor,
                textSizeDp, infoWeight, animate, source);
    }

    /**
     * Dedicated Live Update overload. The contextual label, metric and progress are distinct
     * presentation surfaces so metric changes never become sentence-shaped text updates.
     */
    static boolean apply(Context context, LinearLayout row, ImageView icon, TextView text,
            StructuredLiveUpdateTextView metric, LiveUpdateProgressView progress,
            ContextualAtAGlanceCard next, int infoColor, int clockColor, int textSizeDp,
            int infoWeight, boolean animate, String source) {
        if (row == null || icon == null || text == null) {
            return false;
        }
        animate = SystemAnimationScalePolicy.shouldAnimate(animate);
        ContextualAtAGlanceCard safe = next != null
                ? next : ContextualAtAGlanceCard.none();
        ContextualAtAGlanceCard previous = current(row);
        if (previous.sameContent(safe)) {
            if (metric != null && safe.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE) {
                metric.refreshForHostTick();
            }
            return false;
        }

        boolean hadCard = previous.isVisible();
        boolean hasCard = safe.isVisible();
        boolean inPlaceLiveUpdate = previous.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE
                && safe.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE
                && previous.identity.equals(safe.identity);
        row.animate().cancel();
        row.setTag(safe);
        Runnable applyContent = () -> {
            row.setTag(safe);
            int color = safe.kind == ContextualAtAGlanceCard.Kind.WEATHER_ALERT
                    ? clockColor : infoColor;
            text.setTextColor(color);
            text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
            PixelAodTypography.applySharedClockTypeface(text, context, infoWeight);
            PixelAodTypography.applySharedInfoText(text, context, safe.text);

            boolean liveUpdate = safe.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE;
            applyInlineGeometry(context, icon, text, metric, safe, textSizeDp, liveUpdate);
            if (metric != null) {
                metric.setTextColor(color);
                metric.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
                PixelAodTypography.applySharedClockTypeface(metric, context, infoWeight);
                metric.bind(liveUpdate ? safe : ContextualAtAGlanceCard.none());
                boolean metricVisible = liveUpdate
                        && (safe.isDynamicLiveUpdate() || !safe.liveUpdateMetricText.isEmpty());
                metric.setVisibility(metricVisible ? View.VISIBLE : View.GONE);
                metric.setAlpha(metricVisible ? safe.alpha : 0f);
            }
            if (progress != null) {
                progress.bind(liveUpdate ? safe : ContextualAtAGlanceCard.none(), color);
                progress.setAlpha(liveUpdate && safe.liveUpdateProgressPercent >= 0
                        ? safe.alpha : 0f);
            }

            Drawable drawable = PixelAodContentState.contextualCardIcon(context, safe, color);
            icon.setImageDrawable(drawable);
            icon.setVisibility(drawable != null && safe.isVisible() ? View.VISIBLE : View.GONE);
            icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            float alpha = safe.isVisible() ? safe.alpha : 0f;
            icon.setAlpha(alpha);
            text.setAlpha(alpha);
            row.setVisibility(safe.isVisible() ? View.VISIBLE : View.GONE);
            row.setAlpha(safe.isVisible() ? 1f : 0f);
        };

        // A Live Update owns a stable row. State changes for the same semantic identity update
        // only its metric/progress children and must not replay the contextual crossfade or move
        // lower rows.
        if (inPlaceLiveUpdate) {
            applyContent.run();
            PixelAodLog.log("updated contextual Live Update in place kind="
                    + safe.liveUpdateKind + " source=" + source);
            return false;
        }

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

    private static void applyInlineGeometry(Context context, ImageView icon, TextView text,
            StructuredLiveUpdateTextView metric, ContextualAtAGlanceCard card, int textSizeDp,
            boolean liveUpdate) {
        if (context == null) {
            return;
        }
        if (text.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) text.getLayoutParams();
            int width = liveUpdate ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
            float weight = liveUpdate ? 0f : 1f;
            if (params.width != width || Float.compare(params.weight, weight) != 0) {
                params.width = width;
                params.weight = weight;
                text.setLayoutParams(params);
            }
        }
        if (metric != null && metric.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            metric.setMinWidth(0);
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) metric.getLayoutParams();
            int margin = liveUpdate ? dp(context, LIVE_UPDATE_METRIC_GAP_DP) : 0;
            if (params.getMarginStart() != margin) {
                params.setMarginStart(margin);
                metric.setLayoutParams(params);
            }
        }
        boolean applicationIcon = card != null
                && card.kind == ContextualAtAGlanceCard.Kind.CALENDAR_EVENT
                && ContextualAtAGlanceCalendarIcon.usesApplicationIcon(context);
        if (liveUpdate) {
            int size = dp(context, Math.max(LIVE_UPDATE_ICON_SIZE_DP, textSizeDp));
            if (icon.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) icon.getLayoutParams();
                if (params.width != size || params.height != size) {
                    params.width = size;
                    params.height = size;
                    icon.setLayoutParams(params);
                }
            }
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ContextualAtAGlanceCalendarIcon.applyGeometry(
                    icon, context, textSizeDp, applicationIcon);
        }
    }

    static int liveUpdateIconSizeDp(int textSizeDp) {
        return Math.max(LIVE_UPDATE_ICON_SIZE_DP, Math.max(0, textSizeDp));
    }

    static boolean labelFillsRemainingWidth(ContextualAtAGlanceCard card) {
        return card == null || card.kind != ContextualAtAGlanceCard.Kind.LIVE_UPDATE;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
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
