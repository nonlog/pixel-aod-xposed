package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class ContextualAtAGlanceSelectorTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Singapore");
    private static final long NOW = 1_754_400_000_000L;

    @Test
    public void selectsAlertThenCalendarThenForecastAndOnlyOneCard() {
        BreezyWeatherForecast forecast = BreezyWeatherForecast.forFields("loc",
                LocalDate.of(2025, 8, 6), 800, "Clear", 31, 25, NOW);
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("provider-id", "loc", "Storm",
                NOW - 1_000L, NOW + 86_400_000L, 2);
        BreezyWeatherSnapshot snapshot = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.singletonList(forecast), NOW);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();

        ContextualAtAGlanceSelector.Selection selectedAlert = ContextualAtAGlanceSelector.select(
                snapshot, "10:00 Meeting", true, true, true, store, NOW, ZONE,
                1L, true, true, false, "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.WEATHER_ALERT, selectedAlert.card.kind);

        BreezyWeatherSnapshot noAlert = BreezyWeatherSnapshot.queried("loc",
                Collections.emptyList(), Collections.singletonList(forecast), NOW);
        ContextualAtAGlanceSelector.Selection selectedCalendar = ContextualAtAGlanceSelector.select(
                noAlert, "10:00 Meeting", true, true, true, store, NOW, ZONE,
                1L, true, true, false, "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.CALENDAR_EVENT, selectedCalendar.card.kind);

        ContextualAtAGlanceSelector.Selection selectedForecast = ContextualAtAGlanceSelector.select(
                noAlert, "", true, false, true, store, NOW, ZONE,
                1L, true, true, false, "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.WEATHER_FORECAST, selectedForecast.card.kind);
    }

    @Test
    public void lockscreenNeverSelectsOrMarksAlertButAodDoes() {
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("stable", "loc", "Storm",
                NOW - 1_000L, NOW + 86_400_000L, 3);
        BreezyWeatherSnapshot snapshot = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.emptyList(), NOW);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();

        ContextualAtAGlanceSelector.Selection lockscreen = ContextualAtAGlanceSelector.select(
                snapshot, "", true, false, false, store, NOW, ZONE,
                10L, true, true, false, false, "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.NONE, lockscreen.card.kind);
        assertTrue(store.history(alert.presentationKey) == null
                || store.history(alert.presentationKey).firstVisibleAtMillis == 0L);

        ContextualAtAGlanceSelector.Selection aod = ContextualAtAGlanceSelector.select(
                snapshot, "", true, false, false, store, NOW, ZONE,
                10L, true, true, true, false, "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.WEATHER_ALERT, aod.card.kind);
        assertEquals(NOW, store.history(alert.presentationKey).firstVisibleAtMillis);
        assertEquals(10L, store.history(alert.presentationKey).lastRepeatEntryId);
    }

    @Test
    public void lockscreenToAodHandoffKeepsEntryAndCannotReplayAfterCooldown() {
        long first = NOW;
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("stable", "loc", "Storm",
                first - 1_000L, first + 86_400_000L, 3);
        BreezyWeatherSnapshot snapshot = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.emptyList(), first);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();
        ContextualAtAGlanceSelector.select(snapshot, "", true, false, false, store, first,
                ZONE, 20L, true, true, true, false, "Weather alert", "Tomorrow");

        long afterCooldown = first + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS
                + BreezyWeatherAlert.REPEAT_COOLDOWN_MILLIS + 1L;
        BreezyWeatherSnapshot refreshed = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.emptyList(), afterCooldown);
        ContextualAtAGlanceSelector.Selection lockscreen = ContextualAtAGlanceSelector.select(
                refreshed, "", true, false, false, store, afterCooldown,
                ZONE, 20L, false, true, false, false, "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.NONE, lockscreen.card.kind);
        ContextualAtAGlanceSelector.Selection sameEntryAod =
                ContextualAtAGlanceSelector.select(refreshed, "", true, false, false, store,
                        afterCooldown, ZONE, 20L, false, true, true, false,
                        "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.NONE, sameEntryAod.card.kind);
        ContextualAtAGlanceSelector.Selection laterAod =
                ContextualAtAGlanceSelector.select(refreshed, "", true, false, false, store,
                        afterCooldown, ZONE, 21L, true, true, true, false,
                        "Weather alert", "Tomorrow");
        assertEquals(ContextualAtAGlanceCard.Kind.WEATHER_ALERT, laterAod.card.kind);
    }

    @Test
    public void privacyRedactsOnlyPresentationAndKeepsAlertIdentity() {
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("id", "loc", "Storm headline",
                NOW - 1_000L, NOW + 86_400_000L, 3);
        String redacted = ContextualAtAGlancePrivacy.alertText(alert, true, "Weather alert");
        assertEquals("Weather alert", redacted);
        ContextualAtAGlanceCard redactedCard =
                ContextualAtAGlanceCard.alert(alert, redacted, true, 1f);
        assertEquals(alert.presentationKey, redactedCard.identity);
        assertEquals(3, redactedCard.alertSeverity);
        assertEquals("Storm headline", ContextualAtAGlancePrivacy.alertText(alert, false,
                "Weather alert"));

        BreezyWeatherAlert chinese = BreezyWeatherAlert.forFields("cn", "loc",
                "中原发布暴雨蓝色预警", NOW - 1_000L, NOW + 86_400_000L, 1);
        assertEquals("Blue alert for rainstorms",
                ContextualAtAGlancePrivacy.alertText(chinese, false, "Weather alert"));
    }

    @Test
    public void cardTransitionAndSlotRulesDoNotDependOnClockSize() {
        ContextualAtAGlanceCard none = ContextualAtAGlanceCard.none();
        ContextualAtAGlanceCard calendar = ContextualAtAGlanceCard.calendar("Meeting", 1f);
        ContextualAtAGlanceCard forecast = ContextualAtAGlanceCard.forecast(
                BreezyWeatherForecast.forFields("loc", LocalDate.of(2025, 8, 6), 800,
                        "Clear", 31, 25, NOW), "Tomorrow 31\u00b0 / 25\u00b0", .72f);
        assertFalse(none.isVisible());
        assertTrue(calendar.isVisible());
        assertTrue(calendar.isReplacementOf(forecast));
        assertEquals(250L, ContextualAtAGlanceCard.REPLACEMENT_CROSSFADE_MILLIS);
        assertEquals(300L, ContextualAtAGlanceCard.ENTER_LEAVE_FADE_MILLIS);
    }

    @Test
    public void severityEscalationRefreshesTheAlertCardVisual() {
        BreezyWeatherAlert minor = BreezyWeatherAlert.forFields("same-id", "loc",
                "中原发布暴雨预警", NOW - 1_000L, NOW + 86_400_000L, 1);
        BreezyWeatherAlert extreme = BreezyWeatherAlert.forFields("same-id", "loc",
                "中原发布暴雨预警", NOW - 1_000L, NOW + 86_400_000L, 4);
        ContextualAtAGlanceCard minorCard = ContextualAtAGlanceCard.alert(
                minor, "Blue alert for rainstorms", false, 1f);
        ContextualAtAGlanceCard extremeCard = ContextualAtAGlanceCard.alert(
                extreme, "Red alert for rainstorms", false, 1f);

        assertEquals(minor.presentationKey, extreme.presentationKey);
        assertEquals(1, minorCard.alertSeverity);
        assertEquals(4, extremeCard.alertSeverity);
        assertTrue(extremeCard.isReplacementOf(minorCard));
    }

    @Test
    public void lowerRowsMoveFromOldToTargetWithoutAnImmediateMarginJump() {
        assertEquals(40f, ContextualAtAGlancePresentation.lowerRowTranslationAtProgress(
                120, 80, 0f), 0.001f);
        assertEquals(20f, ContextualAtAGlancePresentation.lowerRowTranslationAtProgress(
                120, 80, 0.5f), 0.001f);
        assertEquals(0f, ContextualAtAGlancePresentation.lowerRowTranslationAtProgress(
                120, 80, 1f), 0.001f);
    }

    @Test
    public void schedulesAlertEndBeforeTheTenMinuteDisplayDeadline() {
        long end = NOW + 2_000L;
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("id", "loc", "Storm",
                NOW - 1_000L, end, 2);
        BreezyWeatherSnapshot snapshot = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.emptyList(), NOW);

        ContextualAtAGlanceSelector.Selection selection = ContextualAtAGlanceSelector.select(
                snapshot, "", true, false, false, new ContextualAtAGlanceStateStore(), NOW,
                ZONE, 5L, true, true, false, "Weather alert", "Tomorrow");

        assertEquals(end, selection.nextDeadlineMillis);
    }

    @Test
    public void schedulesSourceFreshnessExpiryBeforeTheTenMinuteDisplayDeadline() {
        long sourceAt = NOW - BreezyWeatherSnapshot.MAX_ALERT_SOURCE_AGE_MILLIS + 2_000L;
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("id", "loc", "Storm",
                NOW - 1_000L, NOW + 86_400_000L, 2);
        BreezyWeatherSnapshot snapshot = BreezyWeatherSnapshot.queried("loc",
                Collections.singletonList(alert), Collections.emptyList(), sourceAt);

        ContextualAtAGlanceSelector.Selection selection = ContextualAtAGlanceSelector.select(
                snapshot, "", true, false, false, new ContextualAtAGlanceStateStore(), NOW,
                ZONE, 6L, true, true, false, "Weather alert", "Tomorrow");

        assertEquals(sourceAt + BreezyWeatherSnapshot.MAX_ALERT_SOURCE_AGE_MILLIS,
                selection.nextDeadlineMillis);
    }
}
