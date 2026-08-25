package dev.codex.pixelaod;

import java.util.Locale;

/** Pure Android 17 MetricStyle-inspired formatting/cadence policy for Live Update metrics. */
final class LiveUpdateMetricFormatPolicy {
    private LiveUpdateMetricFormatPolicy() {
    }

    static boolean shouldScheduleSecondTicks(boolean ambient, boolean ambientSecondRefreshAvailable) {
        return !ambient || ambientSecondRefreshAvailable;
    }

    static String formatDurationSeconds(long totalSeconds, boolean countDown, boolean ambient,
            boolean ambientSecondRefreshAvailable) {
        long seconds = Math.max(0L, totalSeconds);
        if (shouldScheduleSecondTicks(ambient, ambientSecondRefreshAvailable)) {
            return formatChronometer(seconds);
        }
        return countDown ? formatAdaptiveCountdown(seconds) : formatAdaptiveCountUp(seconds);
    }

    static String formatChronometer(long totalSeconds) {
        long clamped = Math.max(0L, totalSeconds);
        long hours = clamped / 3600L;
        long minutes = (clamped % 3600L) / 60L;
        long seconds = clamped % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    /**
     * Low-power AOD countdown equivalent of Metric.TimeDifference FORMAT_ADAPTIVE. Ceiling keeps
     * the displayed minute from becoming stale between the platform's minute-level AOD frames.
     */
    static String formatAdaptiveCountdown(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        if (seconds == 0L) {
            return "0m";
        }
        if (seconds < 60L) {
            return "<1m";
        }
        long minutes = (seconds + 59L) / 60L;
        return formatAdaptiveMinutes(minutes);
    }

    /** Low-power count-up metrics never round into time that has not elapsed yet. */
    static String formatAdaptiveCountUp(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        if (seconds < 60L) {
            return "<1m";
        }
        return formatAdaptiveMinutes(seconds / 60L);
    }

    private static String formatAdaptiveMinutes(long totalMinutes) {
        long minutes = Math.max(0L, totalMinutes);
        if (minutes < 60L) {
            return minutes + "m";
        }
        long hours = minutes / 60L;
        long remainder = minutes % 60L;
        return remainder == 0L ? hours + "h" : hours + "h " + remainder + "m";
    }
}
