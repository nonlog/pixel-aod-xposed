package dev.codex.pixelaod;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure timing, eligibility, and ordering rules for the contextual weather card. */
final class AtAGlanceWeatherPolicy {
    static final long ALERT_DISPLAY_MILLIS = BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS;
    static final long ALERT_REPEAT_COOLDOWN_MILLIS = BreezyWeatherAlert.REPEAT_COOLDOWN_MILLIS;
    static final long ALERT_SOURCE_GRACE_MILLIS = BreezyWeatherSnapshot.MAX_ALERT_SOURCE_AGE_MILLIS;

    private AtAGlanceWeatherPolicy() {
    }

    static boolean forecastEligible(BreezyWeatherForecast forecast, long nowMillis,
            ZoneId zoneId, boolean enabled) {
        return forecastEligible(forecast, nowMillis, zoneId, enabled,
                ForecastDisplayWindow.defaults());
    }

    static boolean forecastEligible(BreezyWeatherForecast forecast, long nowMillis,
            ZoneId zoneId, boolean enabled, ForecastDisplayWindow displayWindow) {
        ForecastDisplayWindow window = displayWindow != null
                ? displayWindow : ForecastDisplayWindow.defaults();
        return enabled && forecast != null && forecast.isEligible(nowMillis, zoneId)
                && window.contains(nowMillis, zoneId);
    }

    static long nextForecastBoundary(ForecastDisplayWindow displayWindow, long nowMillis,
            ZoneId zoneId, boolean enabled) {
        if (!enabled) {
            return 0L;
        }
        ForecastDisplayWindow window = displayWindow != null
                ? displayWindow : ForecastDisplayWindow.defaults();
        return earliest(window.nextBoundaryMillis(nowMillis, zoneId),
                nextLocalMidnight(nowMillis, zoneId));
    }

    static boolean alertEligible(BreezyWeatherAlert alert, AlertHistory history,
            BreezyWeatherSnapshot snapshot, long nowMillis, long surfaceEntryId,
            boolean nextSurfaceEntry) {
        if (alert == null || alert.isEmpty() || snapshot == null
                || !snapshot.isAlertSourceFresh(nowMillis)
                || !alert.isSourceActive(nowMillis)) {
            return false;
        }
        if (history == null || history.firstVisibleAtMillis <= 0L) {
            return true;
        }
        if (history.displayDeadlineMillis > nowMillis) {
            return true;
        }
        if (alert.severity < 3) {
            return false;
        }
        if (history.cooldownDeadlineMillis > nowMillis) {
            return false;
        }
        return nextSurfaceEntry && surfaceEntryId > 0L
                && history.lastRepeatEntryId != surfaceEntryId;
    }

    static List<BreezyWeatherAlert> orderedAlerts(List<BreezyWeatherAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<BreezyWeatherAlert> result = new ArrayList<>();
        for (BreezyWeatherAlert alert : alerts) {
            if (alert != null && !alert.isEmpty()) {
                result.add(alert);
            }
        }
        result.sort(BreezyWeatherAlert::comparePriority);
        return result;
    }

    static long nextAlertDeadline(BreezyWeatherAlert alert, BreezyWeatherSnapshot snapshot,
            long displayDeadlineMillis, long nowMillis) {
        long next = futureDeadline(displayDeadlineMillis, nowMillis);
        if (alert != null && alert.endMillis > nowMillis) {
            next = earliest(next, alert.endMillis);
        }
        if (snapshot != null && snapshot.lastSuccessfulSourceAtMillis > 0L) {
            long sourceExpiry = snapshot.lastSuccessfulSourceAtMillis
                    + ALERT_SOURCE_GRACE_MILLIS;
            if (sourceExpiry > nowMillis) {
                next = earliest(next, sourceExpiry);
            }
        }
        return next;
    }

    private static long futureDeadline(long deadlineMillis, long nowMillis) {
        return deadlineMillis > nowMillis ? deadlineMillis : 0L;
    }

    private static long nextLocalMidnight(long nowMillis, ZoneId zoneId) {
        if (zoneId == null) {
            return 0L;
        }
        ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(zoneId);
        return now.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    private static long earliest(long current, long candidate) {
        return current <= 0L ? candidate : Math.min(current, candidate);
    }

    static long earlierDeadline(long first, long second) {
        return earliest(first, second);
    }

    static final class AlertHistory {
        final String key;
        final String locationId;
        final String headlineKey;
        long firstVisibleAtMillis;
        long displayDeadlineMillis;
        long cooldownDeadlineMillis;
        long lastSourceConfirmationAtMillis;
        long lastRepeatEntryId;
        int severity;

        AlertHistory(String key, String locationId, String headlineKey, int severity) {
            this.key = key != null ? key : "";
            this.locationId = locationId != null ? locationId : "";
            this.headlineKey = headlineKey != null ? headlineKey : "";
            this.severity = Math.max(0, severity);
        }

        AlertHistory copy() {
            AlertHistory copy = new AlertHistory(key, locationId, headlineKey, severity);
            copy.firstVisibleAtMillis = firstVisibleAtMillis;
            copy.displayDeadlineMillis = displayDeadlineMillis;
            copy.cooldownDeadlineMillis = cooldownDeadlineMillis;
            copy.lastSourceConfirmationAtMillis = lastSourceConfirmationAtMillis;
            copy.lastRepeatEntryId = lastRepeatEntryId;
            return copy;
        }

        void resetForNewPresentation(int newSeverity, long confirmedAtMillis) {
            firstVisibleAtMillis = 0L;
            displayDeadlineMillis = 0L;
            cooldownDeadlineMillis = 0L;
            lastRepeatEntryId = 0L;
            severity = Math.max(0, newSeverity);
            lastSourceConfirmationAtMillis = Math.max(0L, confirmedAtMillis);
        }

        boolean isCurrentlyDisplayed(long nowMillis) {
            return displayDeadlineMillis > nowMillis;
        }
    }
}
