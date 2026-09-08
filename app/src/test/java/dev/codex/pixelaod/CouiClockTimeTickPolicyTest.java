package dev.codex.pixelaod;

import org.junit.Test;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CouiClockTimeTickPolicyTest {
    @Test
    public void acceptsClockAndDisplayLifecycleActions() {
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.TIME_TICK"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.TIME_SET"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.TIMEZONE_CHANGED"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.LOCALE_CHANGED"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.SCREEN_ON"));
        assertTrue(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.SCREEN_OFF"));
        assertFalse(CouiClockTimeTickPolicy.acceptsAction("android.intent.action.BATTERY_CHANGED"));
    }

    @Test
    public void refreshesOnlyWhenMinuteChanges() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(Long.MIN_VALUE, 123L));
        assertFalse(CouiClockTimeTickPolicy.shouldRefresh(123L, 123L));
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(123L, 124L));
    }

    private static CouiClockTimeTickPolicy.ClockState state(long millis, String zone,
            Locale locale, boolean is24Hour) {
        return new CouiClockTimeTickPolicy.ClockState(
                millis, TimeZone.getTimeZone(zone), locale, is24Hour);
    }

    @Test public void firstTickAndMissedMinutesRefresh() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(null,
                state(60_000L, "UTC", Locale.US, true)));
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(
                state(60_000L, "UTC", Locale.US, true),
                state(600_000L, "UTC", Locale.US, true)));
    }

    @Test public void duplicateVendorAndBroadcastTicksAreCoalesced() {
        assertFalse(CouiClockTimeTickPolicy.shouldRefresh(
                state(60_001L, "UTC", Locale.US, true),
                state(119_999L, "UTC", Locale.US, true)));
    }

    @Test public void wallClockMovingBackwardsRefreshes() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(
                state(600_000L, "UTC", Locale.US, true),
                state(60_000L, "UTC", Locale.US, true)));
    }

    @Test public void timezoneChangeInSameMinuteRefreshes() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(
                state(60_000L, "UTC", Locale.US, true),
                state(60_000L, "Asia/Singapore", Locale.US, true)));
    }

    @Test public void changedOffsetWithSameTimezoneIdRefreshes() {
        TimeZone zone = TimeZone.getTimeZone("UTC");
        CouiClockTimeTickPolicy.ClockState before =
                new CouiClockTimeTickPolicy.ClockState(60_000L, zone, Locale.US, true);
        zone.setRawOffset(3_600_000);
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(before,
                new CouiClockTimeTickPolicy.ClockState(60_000L, zone, Locale.US, true)));
    }

    @Test public void localeChangeInSameMinuteRefreshes() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(
                state(60_000L, "UTC", Locale.US, true),
                state(60_000L, "UTC", Locale.forLanguageTag("ar-EG"), true)));
    }

    @Test public void hourFormatChangeInSameMinuteRefreshes() {
        assertTrue(CouiClockTimeTickPolicy.shouldRefresh(
                state(60_000L, "UTC", Locale.US, true),
                state(60_000L, "UTC", Locale.US, false)));
    }

    @Test public void broadcastInvalidationDoesNotDefeatMinuteDeduplication() {
        assertFalse(CouiClockTimeTickPolicy.invalidatesClockFormat("android.intent.action.TIME_TICK"));
        assertFalse(CouiClockTimeTickPolicy.invalidatesClockFormat("android.intent.action.SCREEN_ON"));
        assertTrue(CouiClockTimeTickPolicy.invalidatesClockFormat("android.intent.action.TIME_SET"));
        assertTrue(CouiClockTimeTickPolicy.invalidatesClockFormat("android.intent.action.TIMEZONE_CHANGED"));
        assertTrue(CouiClockTimeTickPolicy.invalidatesClockFormat("android.intent.action.LOCALE_CHANGED"));
        assertFalse(CouiClockTimeTickPolicy.acceptsAction(null));
    }
}
