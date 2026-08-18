package dev.codex.pixelaod;

/** Pure policy for the host-owned clock refresh receiver. */
final class CouiClockTimeTickPolicy {
    private CouiClockTimeTickPolicy() {
    }

    static boolean acceptsAction(String action) {
        return IntentAction.TIME_TICK.equals(action)
                || IntentAction.TIME_CHANGED.equals(action)
                || IntentAction.TIMEZONE_CHANGED.equals(action)
                || IntentAction.SCREEN_ON.equals(action)
                || IntentAction.SCREEN_OFF.equals(action);
    }

    static boolean shouldRefresh(long lastMinute, long currentMinute) {
        return lastMinute != currentMinute;
    }

    /** Avoids making the JVM policy seam depend on the Android SDK. */
    static final class IntentAction {
        static final String TIME_TICK = "android.intent.action.TIME_TICK";
        static final String TIME_CHANGED = "android.intent.action.TIME_SET";
        static final String TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
        static final String SCREEN_ON = "android.intent.action.SCREEN_ON";
        static final String SCREEN_OFF = "android.intent.action.SCREEN_OFF";

        private IntentAction() {
        }
    }
}
