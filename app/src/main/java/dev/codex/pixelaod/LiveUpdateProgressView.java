package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** Thin remaining-time progress indicator used only for structured Timer Live Updates. */
final class LiveUpdateProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String timerIdentity = "";
    private long deadlineElapsedRealtime;
    private long totalDurationMillis;
    private int progressPercent = -1;
    private int progressColor;

    LiveUpdateProgressView(Context context) {
        super(context);
        initialize();
    }

    LiveUpdateProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setVisibility(GONE);
    }

    void bind(ContextualAtAGlanceCard card, int color) {
        boolean timer = LiveUpdateMetricLayoutPolicy.usesProgressBar(card);
        if (!timer) {
            clearTimerProgress();
            progressColor = color;
            setVisibility(GONE);
            return;
        }

        String identity = card.identity != null ? card.identity : "";
        long deadline = card.liveUpdateTimeBaseElapsedRealtime;
        long now = SystemClock.elapsedRealtime();
        long remaining = Math.max(0L, deadline - now);
        boolean sameTimer = identity.equals(timerIdentity) && totalDurationMillis > 0L;
        if (!sameTimer) {
            totalDurationMillis = LiveUpdateMetricLayoutPolicy.inferTimerTotalDurationMillis(
                    remaining, card.liveUpdateProgressPercent);
            timerIdentity = identity;
        }
        deadlineElapsedRealtime = deadline;
        boolean changed = progressColor != color || updateProgress(now);
        progressColor = color;
        setVisibility(totalDurationMillis > 0L && progressPercent >= 0 ? VISIBLE : GONE);
        if (changed) {
            invalidate();
        }
    }

    boolean refreshForHostTick() {
        if (totalDurationMillis <= 0L || deadlineElapsedRealtime <= 0L || getVisibility() != VISIBLE) {
            return false;
        }
        boolean changed = updateProgress(SystemClock.elapsedRealtime());
        if (changed) {
            invalidate();
        }
        return changed;
    }

    private boolean updateProgress(long nowElapsedRealtime) {
        int next = LiveUpdateMetricLayoutPolicy.timerRemainingPercent(
                deadlineElapsedRealtime, nowElapsedRealtime, totalDurationMillis);
        boolean changed = progressPercent != next;
        progressPercent = next;
        return changed;
    }

    private void clearTimerProgress() {
        timerIdentity = "";
        deadlineElapsedRealtime = 0L;
        totalDurationMillis = 0L;
        progressPercent = -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (progressPercent < 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        float stroke = Math.max(1f, Math.min(getHeight(), 2f * density));
        float centerY = getHeight() / 2f;
        float radius = stroke / 2f;
        float left = radius;
        float right = Math.max(left, getWidth() - radius);

        paint.setColor(progressColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(stroke);
        paint.setAlpha(56);
        canvas.drawLine(left, centerY, right, centerY, paint);

        paint.setAlpha(220);
        float progressRight = left + (right - left) * (progressPercent / 100f);
        canvas.drawLine(left, centerY, progressRight, centerY, paint);
    }
}
