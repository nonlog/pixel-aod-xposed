package dev.codex.pixelaod;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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
                genericAlertLabel, tomorrowLabel, ForecastDisplayWindow.DEFAULT_START_TIME,
                ForecastDisplayWindow.DEFAULT_END_TIME);
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
        return select(snapshot, calendarText, weatherAlertsEnabled, calendarEnabled,
                forecastEnabled, stateStore, nowMillis, zoneId, surfaceEntryId,
                nextSurfaceEntry, surfaceVisible, allowWeatherAlerts, sensitiveContentHidden,
                genericAlertLabel, tomorrowLabel, ForecastDisplayWindow.DEFAULT_START_TIME,
                ForecastDisplayWindow.DEFAULT_END_TIME);
    }

    static Selection select(BreezyWeatherSnapshot snapshot, String calendarText,
            boolean weatherAlertsEnabled, boolean calendarEnabled, boolean forecastEnabled,
            ContextualAtAGlanceStateStore stateStore, long nowMillis, ZoneId zoneId,
            long surfaceEntryId, boolean nextSurfaceEntry, boolean surfaceVisible,
            boolean allowWeatherAlerts, boolean sensitiveContentHidden,
            String genericAlertLabel, String tomorrowLabel, String forecastStartTime,
            String forecastEndTime) {
        return select(snapshot, calendarText, weatherAlertsEnabled, calendarEnabled,
                forecastEnabled, stateStore, nowMillis, zoneId, surfaceEntryId,
                nextSurfaceEntry, surfaceVisible, allowWeatherAlerts, sensitiveContentHidden,
                true, genericAlertLabel, tomorrowLabel, forecastStartTime, forecastEndTime);
    }

    static Selection select(BreezyWeatherSnapshot snapshot, String calendarText,
            boolean weatherAlertsEnabled, boolean calendarEnabled, boolean forecastEnabled,
            ContextualAtAGlanceStateStore stateStore, long nowMillis, ZoneId zoneId,
            long surfaceEntryId, boolean nextSurfaceEntry, boolean surfaceVisible,
            boolean allowWeatherAlerts, boolean sensitiveContentHidden,
            boolean contextualPresentationAllowed, String genericAlertLabel, String tomorrowLabel,
            String forecastStartTime, String forecastEndTime) {
        BreezyWeatherSnapshot safeSnapshot = snapshot != null
                ? snapshot : BreezyWeatherSnapshot.empty();
        ContextualAtAGlanceStateStore store = stateStore != null
                ? stateStore : new ContextualAtAGlanceStateStore();
        store.reconcile(safeSnapshot, weatherAlertsEnabled, nowMillis);
        ForecastDisplayWindow displayWindow = ForecastDisplayWindow.fromSettings(
                forecastStartTime, forecastEndTime);
        long forecastDeadline = AtAGlanceWeatherPolicy.nextForecastBoundary(displayWindow,
                nowMillis, zoneId, forecastEnabled);
        List<ContextualTarget> candidates = new ArrayList<>(3);

        BreezyWeatherAlert alert = weatherAlertsEnabled && allowWeatherAlerts
                ? store.select(safeSnapshot.activeAlerts, safeSnapshot, nowMillis, surfaceEntryId,
                nextSurfaceEntry) : BreezyWeatherAlert.empty();
        long alertDeadline = 0L;
        if (!alert.isEmpty()) {
            String text = ContextualAtAGlancePrivacy.alertText(alert, sensitiveContentHidden,
                    genericAlertLabel);
            ContextualAtAGlanceStateStore.AlertDeadline storedDeadline = store.deadline(
                    alert.presentationKey);
            long displayDeadline = storedDeadline != null
                    ? storedDeadline.displayDeadlineMillis : 0L;
            alertDeadline = AtAGlanceWeatherPolicy.nextAlertDeadline(alert, safeSnapshot,
                    displayDeadline, nowMillis);
            candidates.add(ContextualTarget.moduleWeatherAlert(
                    ContextualAtAGlanceCard.alert(alert, text, sensitiveContentHidden, 1f),
                    alert, alertDeadline, contextualPresentationAllowed));
        }

        if (calendarEnabled) {
            ContextualAtAGlanceCard calendar = ContextualAtAGlanceCard.calendar(calendarText, 1f);
            if (calendar.isVisible()) {
                candidates.add(ContextualTarget.moduleCalendar(calendar,
                        contextualPresentationAllowed));
            }
        }

        if (forecastEnabled && zoneId != null) {
            LocalDate tomorrow = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId)
                    .toLocalDate().plusDays(1);
            BreezyWeatherForecast forecast = safeSnapshot.forecastFor(tomorrow);
            if (AtAGlanceWeatherPolicy.forecastEligible(forecast, nowMillis, zoneId, true,
                    displayWindow)) {
                candidates.add(ContextualTarget.moduleForecast(
                        ContextualAtAGlanceCard.forecast(forecast,
                                forecast.formatText(tomorrowLabel), 0.72f),
                        forecastDeadline, contextualPresentationAllowed));
            }
        }

        ContextualTargetArbiter.Selection arbitration = ContextualTargetArbiter.select(
                candidates, nowMillis);
        ContextualTarget selectedTarget = arbitration.target;
        ContextualAtAGlanceCard selectedCard = arbitration.card();
        BreezyWeatherAlert selectedAlert = BreezyWeatherAlert.empty();
        long nextDeadline = AtAGlanceWeatherPolicy.earlierDeadline(
                forecastDeadline, arbitration.nextDeadlineMillis);

        if (selectedTarget != null
                && selectedTarget.source == ContextualTarget.Source.MODULE_WEATHER_ALERT
                && !alert.isEmpty()) {
            selectedAlert = alert;
            if (surfaceVisible) {
                // Only the target that actually wins arbitration may consume the durable alert
                // display/repeat window. A future equivalent native target can therefore win
                // without burning the module fallback before it is ever shown.
                store.markVisible(alert, safeSnapshot, nowMillis, surfaceEntryId);
                ContextualAtAGlanceStateStore.AlertDeadline visibleDeadline = store.deadline(
                        alert.presentationKey);
                long displayDeadline = visibleDeadline != null
                        ? visibleDeadline.displayDeadlineMillis : 0L;
                alertDeadline = AtAGlanceWeatherPolicy.nextAlertDeadline(alert, safeSnapshot,
                        displayDeadline, nowMillis);
            }
            nextDeadline = AtAGlanceWeatherPolicy.earlierDeadline(nextDeadline, alertDeadline);
        }

        return new Selection(selectedCard, selectedAlert, nextDeadline, selectedTarget,
                arbitration.eligibleCount, arbitration.dedupedCount);
    }

    static final class Selection {
        final ContextualAtAGlanceCard card;
        final BreezyWeatherAlert alert;
        final long nextDeadlineMillis;
        final ContextualTarget target;
        final int eligibleTargetCount;
        final int dedupedTargetCount;

        Selection(ContextualAtAGlanceCard card, BreezyWeatherAlert alert,
                long nextDeadlineMillis) {
            this(card, alert, nextDeadlineMillis, null, 0, 0);
        }

        Selection(ContextualAtAGlanceCard card, BreezyWeatherAlert alert,
                long firstDeadlineMillis, long secondDeadlineMillis) {
            this(card, alert, AtAGlanceWeatherPolicy.earlierDeadline(firstDeadlineMillis,
                    secondDeadlineMillis));
        }

        Selection(ContextualAtAGlanceCard card, BreezyWeatherAlert alert,
                long nextDeadlineMillis, ContextualTarget target, int eligibleTargetCount,
                int dedupedTargetCount) {
            this.card = card != null ? card : ContextualAtAGlanceCard.none();
            this.alert = alert != null ? alert : BreezyWeatherAlert.empty();
            this.nextDeadlineMillis = nextDeadlineMillis;
            this.target = target;
            this.eligibleTargetCount = Math.max(0, eligibleTargetCount);
            this.dedupedTargetCount = Math.max(0, dedupedTargetCount);
        }
    }
}
