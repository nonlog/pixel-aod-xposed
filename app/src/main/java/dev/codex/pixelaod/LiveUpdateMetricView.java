package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Android 17 MetricStyle-inspired dedicated Live Update surface for AOD/lockscreen. */
final class LiveUpdateMetricView extends LinearLayout {
    static final int ICON_SIZE_DP = LiveUpdateMetricLayoutPolicy.ICON_SIZE_DP;
    static final int LABEL_TEXT_DP = LiveUpdateMetricLayoutPolicy.LABEL_TEXT_DP;
    static final int METRIC_TEXT_DP = LiveUpdateMetricLayoutPolicy.METRIC_TEXT_DP;
    static final int CONTENT_GAP_DP = LiveUpdateMetricLayoutPolicy.CONTENT_GAP_DP;
    static final int METRIC_TOP_GAP_DP = LiveUpdateMetricLayoutPolicy.METRIC_TOP_GAP_DP;
    static final int PROGRESS_TOP_GAP_DP = LiveUpdateMetricLayoutPolicy.PROGRESS_TOP_GAP_DP;
    static final int PROGRESS_WIDTH_DP = LiveUpdateMetricLayoutPolicy.PROGRESS_WIDTH_DP;
    static final int PROGRESS_HEIGHT_DP = LiveUpdateMetricLayoutPolicy.PROGRESS_HEIGHT_DP;

    private final ImageView iconView;
    private final LinearLayout contentColumn;
    private final TextView labelView;
    private final StructuredLiveUpdateTextView metricView;
    private final LiveUpdateProgressView progressView;
    private ContextualAtAGlanceCard boundCard = ContextualAtAGlanceCard.none();

    LiveUpdateMetricView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.TOP | Gravity.START);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(GONE);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(iconView, new LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP)));

        contentColumn = new LinearLayout(context);
        contentColumn.setOrientation(VERTICAL);
        contentColumn.setGravity(Gravity.START);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.setMarginStart(dp(CONTENT_GAP_DP));
        addView(contentColumn, contentParams);

        labelView = new TextView(context);
        labelView.setIncludeFontPadding(false);
        labelView.setSingleLine(true);
        labelView.setGravity(Gravity.START);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, LABEL_TEXT_DP);
        contentColumn.addView(labelView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        metricView = new StructuredLiveUpdateTextView(context);
        metricView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, METRIC_TEXT_DP);
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metricParams.topMargin = dp(METRIC_TOP_GAP_DP);
        contentColumn.addView(metricView, metricParams);

        progressView = new LiveUpdateProgressView(context);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                dp(PROGRESS_WIDTH_DP), dp(PROGRESS_HEIGHT_DP));
        progressParams.topMargin = dp(PROGRESS_TOP_GAP_DP);
        contentColumn.addView(progressView, progressParams);
    }

    void bind(ContextualAtAGlanceCard card, int color, boolean ambient,
            boolean ambientSecondRefreshAvailable) {
        ContextualAtAGlanceCard safe = card != null ? card : ContextualAtAGlanceCard.none();
        boolean live = safe.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE && safe.isVisible();
        boundCard = live ? safe : ContextualAtAGlanceCard.none();
        if (!live) {
            metricView.bind(ContextualAtAGlanceCard.none(), ambient, ambientSecondRefreshAvailable);
            progressView.bind(ContextualAtAGlanceCard.none(), color);
            setVisibility(GONE);
            return;
        }

        Drawable drawable = PixelAodContentState.contextualCardIcon(context(), safe, color);
        iconView.setImageDrawable(drawable);
        iconView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        iconView.setVisibility(drawable != null ? VISIBLE : GONE);

        PixelAodTypography.applySharedClockTypeface(labelView, context(),
                LiveUpdateMetricLayoutPolicy.LABEL_WEIGHT);
        PixelAodTypography.applySharedInfoText(labelView, context(), safe.text);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, LABEL_TEXT_DP);
        labelView.setTextColor(color);
        labelView.setAlpha(LiveUpdateMetricLayoutPolicy.LABEL_ALPHA);

        PixelAodTypography.applySharedClockTypeface(metricView, context(),
                LiveUpdateMetricLayoutPolicy.METRIC_WEIGHT);
        metricView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, METRIC_TEXT_DP);
        metricView.setTextColor(color);
        metricView.setAlpha(1f);
        metricView.bind(safe, ambient, ambientSecondRefreshAvailable);
        boolean metricVisible = safe.isDynamicLiveUpdate() || !safe.liveUpdateMetricText.isEmpty();
        metricView.setVisibility(metricVisible ? VISIBLE : GONE);

        progressView.bind(safe, color);
        setAlpha(safe.alpha);
        setVisibility(VISIBLE);
    }

    boolean refreshForHostTick(String source) {
        if (boundCard.kind != ContextualAtAGlanceCard.Kind.LIVE_UPDATE) {
            return false;
        }
        boolean changed = metricView.refreshForHostTick();
        changed |= progressView.refreshForHostTick();
        if (changed) {
            PixelAodLog.log("refreshed Live Update metric from host tick kind="
                    + boundCard.liveUpdateKind + " source=" + source);
        }
        return changed;
    }

    StructuredLiveUpdateTextView metricViewForTests() {
        return metricView;
    }

    private Context context() {
        return getContext();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

