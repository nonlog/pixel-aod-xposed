package dev.codex.pixelaod;

import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class NativeSmartspaceContextualAdapterTest {
    @After
    public void tearDown() {
        NativeSmartspaceContextualAdapter.clear("test");
    }

    @Test
    public void displayTextCombinesDistinctTitleAndSubtitleWithoutDuplication() {
        assertEquals("Timer · 5 min left",
                NativeSmartspaceContextualAdapter.composeDisplayText("Timer", "5 min left"));
        assertEquals("Calendar",
                NativeSmartspaceContextualAdapter.composeDisplayText("Calendar", "Calendar"));
        assertEquals("Gate 12",
                NativeSmartspaceContextualAdapter.composeDisplayText("", "Gate 12"));
    }

    @Test
    public void calendarSemanticKeyMatchesModuleCalendarFallback() {
        String text = "Dentist at 15:00";
        assertEquals(ContextualAtAGlanceCard.calendar(text, 1f).identity,
                NativeSmartspaceContextualAdapter.semanticKeyFor(
                        NativeSmartspaceContextualAdapter.FEATURE_CALENDAR,
                        "native-id", text));
    }

    @Test
    public void genericTargetUsesStableNativeIdAndFallbackTtl() {
        long receivedAt = 10_000L;
        NativeSmartspaceContextualAdapter.Snapshot snapshot =
                new NativeSmartspaceContextualAdapter.Snapshot(
                        "target-42", 7, "Alarm · 07:30", receivedAt, 0L, false);
        ContextualTarget target = snapshot.toContextualTarget(true, false);

        assertNotNull(target);
        assertEquals(ContextualTarget.Source.NATIVE_SMARTSPACE, target.source);
        assertEquals("native-smartspace:target-42", target.semanticKey);
        assertEquals(receivedAt + NativeSmartspaceContextualAdapter.FALLBACK_TTL_MILLIS,
                target.expiresAtMillis);
        assertEquals(ContextualAtAGlanceCard.Kind.NATIVE_SMARTSPACE, target.card.kind);
        assertTrue(target.privacyEligible);
        assertTrue(target.suppressionEligible);
    }

    @Test
    public void sensitiveTargetRespectsAdditionalAodPrivacyGate() {
        NativeSmartspaceContextualAdapter.Snapshot snapshot =
                new NativeSmartspaceContextualAdapter.Snapshot(
                        "private", 6, "Private reminder", 1_000L, 5_000L, true);

        ContextualTarget hidden = snapshot.toContextualTarget(true, true);
        ContextualTarget allowed = snapshot.toContextualTarget(true, false);
        assertNotNull(hidden);
        assertFalse(hidden.privacyEligible);
        assertNotNull(allowed);
        assertTrue(allowed.privacyEligible);
    }

    @Test
    public void typedContextualSuppressionStillOwnsNativeTargetEligibility() {
        NativeSmartspaceContextualAdapter.Snapshot snapshot =
                new NativeSmartspaceContextualAdapter.Snapshot(
                        "flight", 4, "Flight · On time", 1_000L, 10_000L, false);
        ContextualTarget target = snapshot.toContextualTarget(false, false);

        assertNotNull(target);
        assertFalse(target.suppressionEligible);
        assertNull(ContextualTargetArbiter.select(List.of(target), 2_000L).target);
    }

    @Test
    public void dedicatedCurrentWeatherFeatureDoesNotConsumeContextualRow() {
        assertFalse(NativeSmartspaceContextualAdapter.shouldIncludeFeature(
                NativeSmartspaceContextualAdapter.FEATURE_WEATHER));
        assertTrue(NativeSmartspaceContextualAdapter.shouldIncludeFeature(
                NativeSmartspaceContextualAdapter.FEATURE_CALENDAR));
    }

    @Test
    public void nativeCalendarWinsEquivalentModuleFallbackAfterEligibility() {
        long now = 5_000L;
        String text = "Team sync at 16:00";
        NativeSmartspaceContextualAdapter.Snapshot snapshot =
                new NativeSmartspaceContextualAdapter.Snapshot(
                        "calendar-native", NativeSmartspaceContextualAdapter.FEATURE_CALENDAR,
                        text, 1_000L, 20_000L, false);
        ContextualTarget nativeTarget = snapshot.toContextualTarget(true, false);
        ContextualTarget moduleTarget = ContextualTarget.moduleCalendar(
                ContextualAtAGlanceCard.calendar(text, 1f), true);

        ContextualTargetArbiter.Selection selection = ContextualTargetArbiter.select(
                List.of(moduleTarget, nativeTarget), now);
        assertNotNull(selection.target);
        assertEquals(ContextualTarget.Source.NATIVE_SMARTSPACE, selection.target.source);
        assertEquals(2, selection.eligibleCount);
        assertEquals(1, selection.dedupedCount);
    }
}
