package dev.codex.pixelaod;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Dedicated metric value surface for a structured Live Update.
 *
 * <p>Interactive surfaces use a local one-second tick. Deep AOD only schedules one-second ticks
 * when the vendor exposes a proven ramless repaint path; otherwise it renders an adaptive metric
 * and refreshes from the platform's own AOD/minute cadence.</p>
 */
final class StructuredLiveUpdateTextView extends TextView {
    private static final long MIN_TICK_DELAY_MILLIS = 80L;

    private ContextualAtAGlanceCard boundCard = ContextualAtAGlanceCard.none();
    private String lastRenderedMetric = "";
    private boolean ambient;
    private boolean ambientSecondRefreshAvailable;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            boolean changed = renderNow();
            if (changed && ambient && ambientSecondRefreshAvailable) {
                PixelAodHook.requestLiveUpdateMetricAodRefresh(
                        "metric-tick#" + boundCard.identity);
            }
            scheduleNextTick();
        }
    };

    StructuredLiveUpdateTextView(Context context) {
        super(context);
        initialize();
    }

    StructuredLiveUpdateTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        setIncludeFontPadding(false);
        setSingleLine(true);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setTextAlignment(TEXT_ALIGNMENT_VIEW_START);
        setFontFeatureSettings("tnum");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
    }

    void bind(ContextualAtAGlanceCard card) {
        bind(card, ambient, ambientSecondRefreshAvailable);
    }

    void configureAmbientMode(boolean ambient, boolean ambientSecondRefreshAvailable) {
        boolean changed = this.ambient != ambient
                || this.ambientSecondRefreshAvailable != ambientSecondRefreshAvailable;
        this.ambient = ambient;
        this.ambientSecondRefreshAvailable = ambientSecondRefreshAvailable;
        if (!changed) {
            return;
        }
        removeCallbacks(tick);
        renderNow();
        if (shouldTick() && isAttachedToWindow()) {
            scheduleNextTick();
        }
    }

    void bind(ContextualAtAGlanceCard card, boolean ambient,
            boolean ambientSecondRefreshAvailable) {
        ContextualAtAGlanceCard safe = card != null ? card : ContextualAtAGlanceCard.none();
        boolean layoutMayChange = !boundCard.identity.equals(safe.identity)
                || boundCard.liveUpdateKind != safe.liveUpdateKind
                || !boundCard.liveUpdateMetricText.equals(safe.liveUpdateMetricText)
                || boundCard.liveUpdateTimeBaseElapsedRealtime
                != safe.liveUpdateTimeBaseElapsedRealtime
                || boundCard.liveUpdateCountDown != safe.liveUpdateCountDown
                || this.ambient != ambient
                || this.ambientSecondRefreshAvailable != ambientSecondRefreshAvailable;
        boundCard = safe;
        this.ambient = ambient;
        this.ambientSecondRefreshAvailable = ambientSecondRefreshAvailable;
        removeCallbacks(tick);
        renderNow();
        if (shouldTick() && isAttachedToWindow()) {
            scheduleNextTick();
        }
        if (layoutMayChange) {
            requestLayout();
        }
    }

    boolean refreshForHostTick() {
        return renderNow();
    }

    void clearDynamicBinding() {
        removeCallbacks(tick);
        boundCard = ContextualAtAGlanceCard.none();
        lastRenderedMetric = "";
        ambient = false;
        ambientSecondRefreshAvailable = false;
        setText("");
    }

    private boolean renderNow() {
        ContextualAtAGlanceCard card = boundCard;
        String metric;
        if (card.isDynamicLiveUpdate()) {
            long now = SystemClock.elapsedRealtime();
            long delta = card.liveUpdateCountDown
                    ? Math.max(0L, card.liveUpdateTimeBaseElapsedRealtime - now)
                    : Math.max(0L, now - card.liveUpdateTimeBaseElapsedRealtime);
            long seconds = card.liveUpdateCountDown
                    ? (delta + 999L) / 1000L : delta / 1000L;
            metric = LiveUpdateMetricFormatPolicy.formatDurationSeconds(
                    seconds, card.liveUpdateCountDown, ambient, ambientSecondRefreshAvailable);
        } else if (card.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE) {
            metric = PixelAodRenderModel.normalizeAtAGlanceExtra(card.liveUpdateMetricText);
        } else {
            metric = "";
        }
        if (lastRenderedMetric.equals(metric)) {
            return false;
        }
        lastRenderedMetric = metric;
        PixelAodTypography.applySharedInfoText(this, getContext(), metric);
        return true;
    }

    private boolean shouldTick() {
        return boundCard.isDynamicLiveUpdate()
                && LiveUpdateMetricFormatPolicy.shouldScheduleSecondTicks(
                        ambient, ambientSecondRefreshAvailable);
    }

    private void scheduleNextTick() {
        ContextualAtAGlanceCard card = boundCard;
        if (!shouldTick() || !isAttachedToWindow()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (card.liveUpdateCountDown && now >= card.liveUpdateTimeBaseElapsedRealtime) {
            return;
        }
        long delay = 1000L - (now % 1000L);
        postDelayed(tick, Math.max(MIN_TICK_DELAY_MILLIS, delay));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        renderNow();
        scheduleNextTick();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    static String formatDurationSeconds(long totalSeconds) {
        return LiveUpdateMetricFormatPolicy.formatChronometer(totalSeconds);
    }
}
