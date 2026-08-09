package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.time.Instant;

import org.json.JSONObject;
import org.junit.Test;

public final class BreezyWeatherAlertTest {
    @Test
    public void parsesSecondsMillisecondsIsoValidityAndLeavesMissingEndOpen() throws Exception {
        long expectedStart = 1_785_898_800_000L;
        BreezyWeatherAlert seconds = BreezyWeatherAlert.fromJson(new JSONObject(
                "{\"alertId\":\"seconds\",\"startDate\":1785898800,"
                        + "\"endDate\":1785902400,\"headline\":\"Storm\",\"severity\":1}"),
                "loc");
        BreezyWeatherAlert millis = BreezyWeatherAlert.fromJson(new JSONObject(
                "{\"alertId\":\"millis\",\"startDate\":1785898800000,"
                        + "\"endDate\":1785902400000,\"headline\":\"Storm\",\"severity\":1}"),
                "loc");
        BreezyWeatherAlert iso = BreezyWeatherAlert.fromJson(new JSONObject(
                "{\"alertId\":\"iso\",\"start\":\"2026-08-05T11:00:00+08:00\","
                        + "\"expires\":\"2026-08-05T12:00:00+08:00\","
                        + "\"headline\":\"Storm\",\"severity\":1}"), "loc");
        BreezyWeatherAlert open = BreezyWeatherAlert.fromJson(new JSONObject(
                "{\"alertId\":\"open\",\"startDate\":1785898800000,"
                        + "\"endDate\":null,\"headline\":\"Storm\",\"severity\":1}"),
                "loc");

        assertEquals(expectedStart, seconds.startMillis);
        assertEquals(1_785_902_400_000L, seconds.endMillis);
        assertEquals(expectedStart, millis.startMillis);
        assertEquals(1_785_902_400_000L, millis.endMillis);
        assertEquals(expectedStart, iso.startMillis);
        assertEquals(1_785_902_400_000L, iso.endMillis);
        assertEquals(0L, open.endMillis);
        assertTrue(open.isSourceActive(expectedStart + 365L * 24L * 60L * 60L * 1000L));
        assertFalse(seconds.isSourceActive(1_785_902_400_000L));
        assertEquals(expectedStart, Instant.parse("2026-08-05T03:00:00Z").toEpochMilli());
    }

    @Test
    public void choosesTheHighestSeverityActiveAlert() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert minor = BreezyWeatherAlert.forFields(
                "Minor rain", 1_749_999_000_000L, 1_750_001_000_000L, 1);
        BreezyWeatherAlert severe = BreezyWeatherAlert.forFields(
                "Severe thunderstorm", 1_749_999_000_000L, 1_750_001_000_000L, 3);
        BreezyWeatherAlert alert = BreezyWeatherAlert.selectActive(now, minor, severe);

        assertEquals("Severe thunderstorm", alert.headline);
        assertEquals(3, alert.severity);
        assertTrue(alert.isActive(now));
    }

    @Test
    public void ignoresFutureAndExpiredAlerts() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert future = BreezyWeatherAlert.forFields(
                "Future", 1_750_001_000_000L, 0L, 4);
        BreezyWeatherAlert expired = BreezyWeatherAlert.forFields(
                "Expired", 0L, 1_749_999_000_000L, 4);
        BreezyWeatherAlert alert = BreezyWeatherAlert.selectActive(now, future, expired);

        assertFalse(alert.isActive(now));
        assertEquals("", alert.headline);
    }

    @Test
    public void keepsTheFirstObservedTimeWhenTheSameAlertIsRefreshed() {
        long observedAt = 1_750_000_000_000L;
        BreezyWeatherAlert source = BreezyWeatherAlert.forFields(
                "Thunderstorm", observedAt - 1_000L, observedAt + 86_400_000L, 3);
        BreezyWeatherAlert stored = BreezyWeatherAlert.observeForDisplay(source,
                BreezyWeatherAlert.empty(), observedAt);
        BreezyWeatherAlert refreshed = BreezyWeatherAlert.observeForDisplay(source, stored,
                observedAt + 9L * 60L * 1000L);

        assertEquals(observedAt, refreshed.observedAtMillis);
        assertTrue(refreshed.isActive(observedAt + 9L * 60L * 1000L));
        assertFalse(refreshed.isActive(observedAt + 10L * 60L * 1000L));
    }

    @Test
    public void doesNotRestartTheClockForAnAlreadyOldAlert() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert staleSource = BreezyWeatherAlert.forFields(
                "Old warning", now - 2L * 60L * 60L * 1000L, now + 86_400_000L, 2);

        BreezyWeatherAlert observed = BreezyWeatherAlert.observeForDisplay(
                staleSource, BreezyWeatherAlert.empty(), now);

        assertEquals("", observed.headline);
        assertFalse(observed.isActive(now));
    }

    @Test
    public void normalizesFallbackIdentityAndPrefersProviderId() {
        BreezyWeatherAlert first = BreezyWeatherAlert.forFields("", "loc-a", "Storm!  Warning",
                1_000L, 9_000L, 2);
        BreezyWeatherAlert equivalent = BreezyWeatherAlert.forFields("", "loc-a",
                " storm warning ", 1_000L, 8_000L, 2);
        BreezyWeatherAlert otherLocation = BreezyWeatherAlert.forFields("", "loc-b",
                "Storm warning", 1_000L, 8_000L, 2);
        BreezyWeatherAlert provider = BreezyWeatherAlert.forFields("provider-id", "loc-a",
                "Storm warning", 1_000L, 8_000L, 2);
        BreezyWeatherAlert providerHeadlineChange = BreezyWeatherAlert.forFields("provider-id",
                "loc-a", "Storm warning extended", 1_000L, 8_000L, 2);

        assertTrue(first.sameLogicalIdentity(equivalent));
        assertFalse(first.sameLogicalIdentity(otherLocation));
        assertTrue(provider.identity.startsWith("provider:"));
        assertTrue(provider.sameLogicalIdentity(providerHeadlineChange));
        assertFalse(provider.presentationKey.equals(providerHeadlineChange.presentationKey));
        assertTrue(provider.sameDisplay(BreezyWeatherAlert.forFields("provider-id", "loc-a",
                "Storm warning", 1_000L, 10_000L, 2)));
    }

    @Test
    public void ordersSeverityThenKnownEarlierEndThenStartThenIdentity() {
        long now = 1_000_000L;
        BreezyWeatherAlert laterEnd = BreezyWeatherAlert.forFields("a", "loc", "A", now - 1,
                now + 10_000L, 3);
        BreezyWeatherAlert earlierEnd = BreezyWeatherAlert.forFields("b", "loc", "B", now - 1,
                now + 5_000L, 3);
        BreezyWeatherAlert noEnd = BreezyWeatherAlert.forFields("c", "loc", "C", now - 1,
                0L, 3);
        assertTrue(BreezyWeatherAlert.comparePriority(earlierEnd, laterEnd) < 0);
        assertTrue(BreezyWeatherAlert.comparePriority(laterEnd, noEnd) < 0);
        assertEquals(earlierEnd, BreezyWeatherAlert.selectActive(now, laterEnd, earlierEnd));
    }

    @Test
    public void startsWindowOnlyWhenVisibleAndAppliesOneTimeAndSevereRepeatRules() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert minor = BreezyWeatherAlert.forFields("minor", "loc", "Minor", now - 1,
                now + 86_400_000L, 1);
        BreezyWeatherSnapshot snapshot = snapshot("loc", minor, now);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();

        assertEquals(minor, store.select(snapshot.activeAlerts, snapshot, now, 1L, true));
        assertTrue(store.history(minor.presentationKey) == null
                || store.history(minor.presentationKey).firstVisibleAtMillis == 0L);
        store.markVisible(minor, snapshot, now, 1L);
        assertEquals(now, store.history(minor.presentationKey).firstVisibleAtMillis);
        assertEquals(BreezyWeatherAlert.empty(), store.select(snapshot.activeAlerts, snapshot,
                now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS + 1L, 1L, true));

        BreezyWeatherAlert severe = BreezyWeatherAlert.forFields("severe", "loc", "Severe",
                now - 1, now + 86_400_000L, 3);
        ContextualAtAGlanceStateStore severeStore = new ContextualAtAGlanceStateStore();
        BreezyWeatherSnapshot severeSnapshot = snapshot("loc", severe, now);
        severeStore.markVisible(severe, severeSnapshot, now, 1L);
        long afterCooldown = now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS
                + BreezyWeatherAlert.REPEAT_COOLDOWN_MILLIS + 1L;
        BreezyWeatherSnapshot refreshed = snapshot("loc", severe, afterCooldown);
        assertEquals(BreezyWeatherAlert.empty(), severeStore.select(severeSnapshot.activeAlerts,
                severeSnapshot, afterCooldown, 1L, false));
        severeStore.reconcile(refreshed, true, afterCooldown);
        assertEquals(severe, severeStore.select(refreshed.activeAlerts, refreshed,
                afterCooldown, 2L, true));
    }

    @Test
    public void severeRepeatRequiresAGenuinelyNewSurfaceEntryAndSurvivesRestart() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert severe = BreezyWeatherAlert.forFields("stable", "loc", "Severe", now - 1,
                now + 86_400_000L, 3);
        BreezyWeatherSnapshot snapshot = snapshot("loc", severe, now);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();

        store.markVisible(severe, snapshot, now, 41L);
        long afterCooldown = now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS
                + BreezyWeatherAlert.REPEAT_COOLDOWN_MILLIS + 1L;
        BreezyWeatherSnapshot refreshed = snapshot("loc", severe, afterCooldown);
        assertEquals(BreezyWeatherAlert.empty(), store.select(refreshed.activeAlerts, refreshed,
                afterCooldown, 41L, true));
        assertEquals(BreezyWeatherAlert.empty(), store.select(refreshed.activeAlerts, refreshed,
                afterCooldown, 41L, false));

        String persisted = store.serializeForTests();
        ContextualAtAGlanceStateStore restarted = new ContextualAtAGlanceStateStore();
        restarted.restoreForTests(persisted);
        assertEquals(BreezyWeatherAlert.empty(), restarted.select(refreshed.activeAlerts, refreshed,
                afterCooldown, 41L, true));
        assertEquals(severe, restarted.select(refreshed.activeAlerts, refreshed,
                afterCooldown, 42L, true));
        restarted.markVisible(severe, refreshed, afterCooldown, 42L);
        assertEquals(BreezyWeatherAlert.empty(), restarted.select(refreshed.activeAlerts, refreshed,
                afterCooldown + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS + 1L, 42L, true));
    }

    @Test
    public void unknownMinorAndModerateAlertsAreEligibleOnceOnly() {
        long now = 1_750_000_000_000L;
        for (int severity : new int[]{0, 1, 2}) {
            BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("once-" + severity, "loc",
                    "Alert " + severity, now - 1L, 0L, severity);
            BreezyWeatherSnapshot snapshot = snapshot("loc", alert, now);
            ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();
            assertEquals(alert, store.select(snapshot.activeAlerts, snapshot, now, 1L, true));
            store.markVisible(alert, snapshot, now, 1L);
            long afterWindow = now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS + 1L;
            BreezyWeatherSnapshot refreshed = snapshot("loc", alert, afterWindow);
            assertEquals(BreezyWeatherAlert.empty(), store.select(refreshed.activeAlerts,
                    refreshed, afterWindow, 2L, true));
        }
    }

    @Test
    public void failureGraceAlsoBoundsAlertsWithoutEndTimeAndDisableReenableResetsHistory() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert alert = BreezyWeatherAlert.forFields("no-end", "loc", "No end",
                now - 1L, 0L, 3);
        BreezyWeatherSnapshot snapshot = snapshot("loc", alert, now);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();
        store.markVisible(alert, snapshot, now, 1L);
        BreezyWeatherSnapshot failed = BreezyWeatherSnapshot.failedUsing(snapshot);
        long afterGrace = now + BreezyWeatherSnapshot.MAX_ALERT_SOURCE_AGE_MILLIS + 1L;
        BreezyWeatherSnapshot staleFailure = BreezyWeatherSnapshot.failedUsing(
                BreezyWeatherSnapshot.queried("loc", Collections.singletonList(alert),
                        Collections.emptyList(), now));
        assertEquals(BreezyWeatherAlert.empty(), store.select(staleFailure.activeAlerts,
                staleFailure, afterGrace, 2L, true));

        store.reconcile(failed, false, now + 1L);
        assertEquals(0, store.size());
        store.reconcile(snapshot, true, now + 2L);
        assertEquals(alert, store.select(snapshot.activeAlerts, snapshot, now + 2L, 3L, true));
    }

    @Test
    public void preservesHistoryAcrossRestartAndSourceRecoveryButClearsOnSuccessRemoval() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert severe = BreezyWeatherAlert.forFields("stable", "loc", "Severe", now - 1,
                now + 86_400_000L, 3);
        BreezyWeatherSnapshot snapshot = snapshot("loc", severe, now);
        ContextualAtAGlanceStateStore first = new ContextualAtAGlanceStateStore();
        first.markVisible(severe, snapshot, now, 1L);
        long firstVisible = first.history(severe.presentationKey).firstVisibleAtMillis;

        ContextualAtAGlanceStateStore restarted = new ContextualAtAGlanceStateStore();
        restarted.restoreForTests(first.serializeForTests());
        assertEquals(firstVisible, restarted.history(severe.presentationKey).firstVisibleAtMillis);

        BreezyWeatherSnapshot failed = BreezyWeatherSnapshot.failedUsing(snapshot);
        assertTrue(failed.isAlertSourceFresh(now + 59L * 60L * 1000L));
        assertFalse(failed.isAlertSourceFresh(now + 60L * 60L * 1000L + 1L));
        restarted.reconcile(failed, true, now + 1L);
        restarted.reconcile(snapshot, true, now + 2L);
        assertEquals(firstVisible, restarted.history(severe.presentationKey).firstVisibleAtMillis);

        restarted.reconcile(BreezyWeatherSnapshot.queried("loc", Collections.emptyList(),
                Collections.emptyList(), now + 3L), true, now + 3L);
        assertEquals(0, restarted.size());
    }

    @Test
    public void severityIncreaseIsNewImmediatePresentationAndDecreaseDoesNotReplay() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert minor = BreezyWeatherAlert.forFields("stable", "loc", "Warning", now - 1,
                now + 86_400_000L, 1);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();
        BreezyWeatherSnapshot minorSnapshot = snapshot("loc", minor, now);
        store.markVisible(minor, minorSnapshot, now, 1L);

        BreezyWeatherAlert severe = BreezyWeatherAlert.forFields("stable", "loc", "Warning", now - 1,
                now + 86_400_000L, 3);
        BreezyWeatherSnapshot severeSnapshot = snapshot("loc", severe, now + 1L);
        store.reconcile(severeSnapshot, true, now + 1L);
        assertEquals(0, store.history(severe.presentationKey).firstVisibleAtMillis);
        assertEquals(severe, store.select(severeSnapshot.activeAlerts, severeSnapshot, now + 1L,
                2L, true));

        store.markVisible(severe, severeSnapshot, now + 1L, 2L);
        BreezyWeatherAlert decreased = BreezyWeatherAlert.forFields("stable", "loc", "Warning",
                now - 1, now + 86_400_000L, 1);
        BreezyWeatherSnapshot decreasedSnapshot = snapshot("loc", decreased,
                now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS + 2L);
        store.reconcile(decreasedSnapshot, true, now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS + 2L);
        assertEquals(BreezyWeatherAlert.empty(), store.select(decreasedSnapshot.activeAlerts,
                decreasedSnapshot, now + BreezyWeatherAlert.DISPLAY_TIMEOUT_MILLIS + 2L, 3L, true));
    }

    @Test
    public void retainsEachLocationHistoryAndDoesNotPausePreemptedDeadline() {
        long now = 1_750_000_000_000L;
        BreezyWeatherAlert first = BreezyWeatherAlert.forFields("first", "loc-a", "First", now - 1,
                now + 86_400_000L, 3);
        BreezyWeatherAlert higher = BreezyWeatherAlert.forFields("higher", "loc-a", "Higher",
                now - 1, now + 86_400_000L, 4);
        ContextualAtAGlanceStateStore store = new ContextualAtAGlanceStateStore();
        BreezyWeatherSnapshot firstSnapshot = snapshot("loc-a", first, now);
        store.markVisible(first, firstSnapshot, now, 1L);
        long deadline = store.history(first.presentationKey).displayDeadlineMillis;
        BreezyWeatherSnapshot both = BreezyWeatherSnapshot.queried("loc-a",
                Arrays.asList(first, higher), Collections.emptyList(), now);
        assertEquals(higher, store.select(both.activeAlerts, both, now + 1L, 1L, true));
        assertEquals(deadline, store.history(first.presentationKey).displayDeadlineMillis);

        BreezyWeatherAlert sameHeadlineOtherLocation = BreezyWeatherAlert.forFields("first",
                "loc-b", "First", now - 1, now + 86_400_000L, 3);
        BreezyWeatherSnapshot other = snapshot("loc-b", sameHeadlineOtherLocation, now);
        store.reconcile(other, true, now);
        assertEquals(2, store.size());
    }

    private static BreezyWeatherSnapshot snapshot(String location, BreezyWeatherAlert alert,
            long sourceAt) {
        return BreezyWeatherSnapshot.queried(location,
                alert == null ? Collections.emptyList() : Collections.singletonList(alert),
                Collections.emptyList(), sourceAt);
    }
}
