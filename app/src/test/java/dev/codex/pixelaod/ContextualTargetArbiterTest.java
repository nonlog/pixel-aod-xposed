package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class ContextualTargetArbiterTest {
    private static final long NOW = 1_754_400_000_000L;

    @Test
    public void preservesCurrentModulePriorityAlertThenCalendarThenForecast() {
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("alert", "loc", "Storm",
                NOW - 1_000L, NOW + 60_000L, 2);
        ContextualTarget alertTarget = ContextualTarget.moduleWeatherAlert(
                ContextualAtAGlanceCard.alert(alert, "Storm", false, 1f), alert,
                NOW + 60_000L, true);
        ContextualTarget calendarTarget = ContextualTarget.moduleCalendar(
                ContextualAtAGlanceCard.calendar("10:00 Meeting", 1f), true);
        BreezyWeatherForecast forecast = BreezyWeatherForecast.forFields("loc",
                LocalDate.of(2025, 8, 6), 800, "Clear", 31, 25, NOW);
        ContextualTarget forecastTarget = ContextualTarget.moduleForecast(
                ContextualAtAGlanceCard.forecast(forecast, "Tomorrow 31 / 25", 0.72f),
                NOW + 120_000L, true);

        ContextualTargetArbiter.Selection selection = ContextualTargetArbiter.select(
                Arrays.asList(forecastTarget, calendarTarget, alertTarget), NOW);

        assertEquals(ContextualTarget.Source.MODULE_WEATHER_ALERT, selection.target.source);
        assertEquals(ContextualAtAGlanceCard.Kind.WEATHER_ALERT, selection.card().kind);
        assertEquals(3, selection.eligibleCount);
        assertEquals(3, selection.dedupedCount);
    }

    @Test
    public void filtersValidityPrivacySuppressionAndPresentationBeforeRanking() {
        ContextualAtAGlanceCard high = ContextualAtAGlanceCard.calendar("High", 1f);
        ContextualAtAGlanceCard fallback = ContextualAtAGlanceCard.calendar("Fallback", 1f);
        ContextualTarget expired = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.CRITICAL,
                "expired", high, 0L, NOW, true, true, true, true, 1);
        ContextualTarget privateTarget = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.CRITICAL,
                "private", high, 0L, NOW + 10_000L, true, false, true, true, 1);
        ContextualTarget suppressed = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.CRITICAL,
                "suppressed", high, 0L, NOW + 10_000L, true, true, false, true, 1);
        ContextualTarget ineligibleScene = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.CRITICAL,
                "scene", high, 0L, NOW + 10_000L, true, true, true, false, 1);
        ContextualTarget allowed = new ContextualTarget(
                ContextualTarget.Source.MODULE_CALENDAR, ContextualTarget.Urgency.NORMAL,
                "allowed", fallback, 0L, 0L, true, true, true, true, 1);

        ContextualTargetArbiter.Selection selection = ContextualTargetArbiter.select(
                Arrays.asList(expired, privateTarget, suppressed, ineligibleScene, allowed), NOW);

        assertEquals("allowed", selection.target.semanticKey);
        assertEquals(1, selection.eligibleCount);
    }

    @Test
    public void nativeEquivalentWinsButBlockedNativeLeavesModuleFallback() {
        ContextualAtAGlanceCard nativeCard = ContextualAtAGlanceCard.calendar("Native flight", 1f);
        ContextualAtAGlanceCard moduleCard = ContextualAtAGlanceCard.calendar("Module flight", 1f);
        ContextualTarget nativeTarget = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.NORMAL,
                "flight:42", nativeCard, 0L, NOW + 30_000L,
                true, true, true, true, 1);
        ContextualTarget moduleTarget = new ContextualTarget(
                ContextualTarget.Source.MODULE_CALENDAR, ContextualTarget.Urgency.NORMAL,
                "flight:42", moduleCard, 0L, 0L,
                true, true, true, true, 1);

        ContextualTargetArbiter.Selection nativeWins = ContextualTargetArbiter.select(
                Arrays.asList(moduleTarget, nativeTarget), NOW);
        assertEquals(ContextualTarget.Source.NATIVE_SMARTSPACE, nativeWins.target.source);
        assertEquals(2, nativeWins.eligibleCount);
        assertEquals(1, nativeWins.dedupedCount);

        ContextualTarget blockedNative = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.NORMAL,
                "flight:42", nativeCard, 0L, NOW + 30_000L,
                true, false, true, true, 1);
        ContextualTargetArbiter.Selection fallbackWins = ContextualTargetArbiter.select(
                Arrays.asList(moduleTarget, blockedNative), NOW);
        assertEquals(ContextualTarget.Source.MODULE_CALENDAR, fallbackWins.target.source);
        assertEquals(1, fallbackWins.eligibleCount);
        assertEquals(1, fallbackWins.dedupedCount);
    }

    @Test
    public void oneRowVisualBudgetIsHardAndDeterministic() {
        ContextualTarget calendar = ContextualTarget.moduleCalendar(
                ContextualAtAGlanceCard.calendar("Meeting", 1f), true);
        assertNull(ContextualTargetArbiter.select(
                Collections.singletonList(calendar), NOW, 0).target);
        assertEquals(ContextualTarget.Source.MODULE_CALENDAR,
                ContextualTargetArbiter.select(
                        Collections.singletonList(calendar), NOW, 1).target.source);
    }

    @Test
    public void sameUrgencyPrefersEarlierExpiryAndPublishesNextBoundary() {
        ContextualTarget later = new ContextualTarget(
                ContextualTarget.Source.NATIVE_SMARTSPACE, ContextualTarget.Urgency.NORMAL,
                "later", ContextualAtAGlanceCard.calendar("Later", 1f), 0L,
                NOW + 20_000L, true, true, true, true, 1);
        ContextualTarget sooner = new ContextualTarget(
                ContextualTarget.Source.LIVE_UPDATE, ContextualTarget.Urgency.NORMAL,
                "sooner", ContextualAtAGlanceCard.calendar("Sooner", 1f), 0L,
                NOW + 10_000L, true, true, true, true, 1);

        ContextualTargetArbiter.Selection selection = ContextualTargetArbiter.select(
                Arrays.asList(later, sooner), NOW);

        assertEquals("sooner", selection.target.semanticKey);
        assertEquals(NOW + 10_000L, selection.nextDeadlineMillis);
    }
}
