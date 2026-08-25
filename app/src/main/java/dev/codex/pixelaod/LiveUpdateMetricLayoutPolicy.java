package dev.codex.pixelaod;

/** Pure layout contract for the dedicated Android 17-style Live Update metric block. */
final class LiveUpdateMetricLayoutPolicy {
    static final int ICON_SIZE_DP = 18;
    static final int LABEL_TEXT_DP = 13;
    static final int METRIC_TEXT_DP = 30;
    static final int LABEL_WEIGHT = 500;
    static final int METRIC_WEIGHT = 500;
    static final float LABEL_ALPHA = 0.92f;
    static final int CONTENT_GAP_DP = 7;
    static final int METRIC_TOP_GAP_DP = 1;
    static final int PROGRESS_TOP_GAP_DP = 6;
    static final int PROGRESS_WIDTH_DP = 96;
    static final int PROGRESS_HEIGHT_DP = 2;
    static final int BLOCK_TOP_GAP_DP = 16;
    static final int BLOCK_TO_NOTIFICATION_GAP_DP = 18;

    private LiveUpdateMetricLayoutPolicy() {
    }

    static boolean usesDedicatedBlock(ContextualAtAGlanceCard card) {
        return card != null && card.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE;
    }

    static boolean metricIsPrimary() {
        return METRIC_TEXT_DP > LABEL_TEXT_DP;
    }

    static boolean usesProgressBar(ContextualAtAGlanceCard card) {
        return card != null
                && card.kind == ContextualAtAGlanceCard.Kind.LIVE_UPDATE
                && card.liveUpdateKind == ContextualAtAGlanceCard.LiveUpdateKind.TIMER
                && card.liveUpdateProgressPercent >= 0;
    }
    static long inferTimerTotalDurationMillis(long remainingMillis, int bootstrapPercent) {
        if (remainingMillis <= 0L || bootstrapPercent <= 0 || bootstrapPercent > 100) {
            return 0L;
        }
        long total = Math.round((remainingMillis * 100d) / bootstrapPercent);
        return total > 0L
                && total <= NativeLiveAlertContextualAdapter.TIMER_PROGRESS_MAX_TOTAL_MILLIS
                ? total : 0L;
    }

    static int timerRemainingPercent(long deadlineElapsedRealtime, long nowElapsedRealtime,
            long totalDurationMillis) {
        if (deadlineElapsedRealtime <= 0L || totalDurationMillis <= 0L) {
            return -1;
        }
        long remaining = Math.max(0L, deadlineElapsedRealtime - Math.max(0L, nowElapsedRealtime));
        return Math.max(0, Math.min(100,
                (int) Math.ceil((remaining * 100d) / totalDurationMillis)));
    }
}
