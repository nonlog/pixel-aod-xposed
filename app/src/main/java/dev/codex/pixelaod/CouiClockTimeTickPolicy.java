package dev.codex.pixelaod;

import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/** Pure policy for the host-owned clock refresh receiver. */
final class CouiClockTimeTickPolicy {
    private CouiClockTimeTickPolicy() {
    }

    static boolean acceptsAction(String action) {
        return IntentAction.TIME_TICK.equals(action)
                || IntentAction.TIME_CHANGED.equals(action)
                || IntentAction.TIMEZONE_CHANGED.equals(action)
                || IntentAction.LOCALE_CHANGED.equals(action)
                || IntentAction.SCREEN_ON.equals(action)
                || IntentAction.SCREEN_OFF.equals(action);
    }

    static boolean shouldRefresh(long lastMinute, long currentMinute) {
        return lastMinute != currentMinute;
    }

    static boolean shouldRefresh(ClockState previous, ClockState current) {
        return previous == null || previous.minute != current.minute
                || previous.offsetMillis != current.offsetMillis
                || previous.is24Hour != current.is24Hour
                || !previous.zoneId.equals(current.zoneId)
                || !previous.locale.equals(current.locale);
    }

    static boolean invalidatesClockFormat(String action) {
        return IntentAction.TIME_CHANGED.equals(action)
                || IntentAction.TIMEZONE_CHANGED.equals(action)
                || IntentAction.LOCALE_CHANGED.equals(action);
    }

    /** One wall-clock sample and its formatting inputs, not an uptime-based minute timer. */
    static final class ClockState {
        final long minute;
        final String zoneId;
        final int offsetMillis;
        final Locale locale;
        final boolean is24Hour;

        ClockState(long nowMillis, TimeZone zone, Locale locale, boolean is24Hour) {
            this.minute = Math.floorDiv(nowMillis, 60_000L);
            this.zoneId = Objects.requireNonNull(zone).getID();
            this.offsetMillis = zone.getOffset(nowMillis);
            this.locale = Objects.requireNonNull(locale);
            this.is24Hour = is24Hour;
        }
    }

    /** Avoids making the JVM policy seam depend on the Android SDK. */
    static final class IntentAction {
        static final String TIME_TICK = "android.intent.action.TIME_TICK";
        static final String TIME_CHANGED = "android.intent.action.TIME_SET";
        static final String TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
        static final String LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED";
        static final String SCREEN_ON = "android.intent.action.SCREEN_ON";
        static final String SCREEN_OFF = "android.intent.action.SCREEN_OFF";

        private IntentAction() {
        }
    }
}
