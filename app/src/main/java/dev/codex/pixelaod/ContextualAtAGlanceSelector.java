package dev.codex.pixelaod;

import java.time.LocalDate;
import java.time.ZoneId;

/** Selects exactly one contextual card without embedding policy in View code. */
final class ContextualAtAGlanceSelector {
    private ContextualAtAGlanceSelector() {
    }

    static Selection select(BreezyWeatherSnapshot snapshot, String calendarText,
            boolean weatherAlertsEnabled, boolean calendarEnabled, boolean forecastEnabled,
            ContextualAtAGlanceStateStore stateStore, long nowMillis, ZoneId zoneId,
            long surfaceEntryId, boolean nextSurfaceEntry, boolean surfaceVisible,
            boolean sensitiveContentHidden, String genericAlertLabel, String tomorrowLabel) {
        return select(snapshot, calendarText, weatherAlertsEnabled, calendarEnabled,
                forecastEnabled, stateStore, nowMillis, zoneId, surfaceEntryId,
                nextSurfaceEntry, surfaceVisible, true, sensitiveContentHidden,
                genericAlertLabel, tomorrowLabel);
    }

    /**
     * Selects a card for an explicit surface. Weather alerts are AOD-only in the current
     * product policy; lockscreen callers pass false so selection cannot consume the alert window.
     */
    static Selection select(BreezyWeatherSnapshot snapshot, String calendarText,
            boolean weatherAlertsEnabled, boolean calendarEnabled, boolean forecastEnabled,
            ContextualAtAGlanceStateStore stateStore, long nowMillis, ZoneId zoneId,
            long surfaceEntryId, boolean nextSurfaceEntry, boolean surfaceVisible,
            boolean allowWeatherAlerts, boolean sensitiveContentHidden,
            String genericAlertLabel, String tomorrowLabel) {
        BreezyWeatherSnapshot safeSnapshot = snapshot != null
                ? snapshot : BreezyWeatherSnapshot.empty();
        ContextualAtAGlanceStateStore store = stateStore != null
                ? stateStore : new ContextualAtAGlanceStateStore();
        store.reconcile(safeSnapshot, weatherAlertsEnabled, nowMillis);

        BreezyWeatherAlert alert = weatherAlertsEnabled && allowWeatherAlerts
                ? store.select(safeSnapshot.activeAlerts, safeSnapshot, nowMillis, surfaceEntryId,
                nextSurfaceEntry) : BreezyWeatherAlert.empty();
        if (!alert.isEmpty()) {
            String text = ContextualAtAGlancePrivacy.alertText(alert, sensitiveContentHidden,
                    genericAlertLabel);
            if (surfaceVisible) {
                // Reading or caching a relay payload never calls this method with visible=true;
                // only the rendered surface starts the ten-minute window here.
                store.markVisible(alert, safeSnapshot, nowMillis, surfaceEntryId);
            }
            ContextualAtAGlanceStateStore.AlertDeadline deadline = store.deadline(
                    alert.presentationKey);
            long displayDeadline = deadline != null ? deadline.displayDeadlineMillis : 0L;
            return new Selection(ContextualAtAGlanceCard.alert(alert, text,
                    sensitiveContentHidden, 1f), alert, AtAGlanceWeatherPolicy.nextAlertDeadline(
                    alert, safeSnapshot, displayDeadline, nowMillis));
        }

        if (calendarEnabled) {
            ContextualAtAGlanceCard calendar = ContextualAtAGlanceCard.calendar(calendarText, 1f);
            if (calendar.isVisible()) {
                return new Selection(calendar, BreezyWeatherAlert.empty(), 0L);
            }
        }

        if (forecastEnabled && zoneId != null) {
            LocalDate tomorrow = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId)
                    .toLocalDate().plusDays(1);
            BreezyWeatherForecast forecast = safeSnapshot.forecastFor(tomorrow);
            if (AtAGlanceWeatherPolicy.forecastEligible(forecast, nowMillis, zoneId, true)) {
                return new Selection(ContextualAtAGlanceCard.forecast(forecast,
                        forecast.formatText(tomorrowLabel), 0.72f),
                        BreezyWeatherAlert.empty(), 0L);
            }
        }
        return new Selection(ContextualAtAGlanceCard.none(), BreezyWeatherAlert.empty(), 0L);
    }

    static final class Selection {
        final ContextualAtAGlanceCard card;
        final BreezyWeatherAlert alert;
        final long nextDeadlineMillis;

        Selection(ContextualAtAGlanceCard card, BreezyWeatherAlert alert,
                long nextDeadlineMillis) {
            this.card = card != null ? card : ContextualAtAGlanceCard.none();
            this.alert = alert != null ? alert : BreezyWeatherAlert.empty();
            this.nextDeadlineMillis = nextDeadlineMillis;
        }
    }
}
