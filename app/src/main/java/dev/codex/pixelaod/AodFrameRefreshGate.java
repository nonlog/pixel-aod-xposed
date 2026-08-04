package dev.codex.pixelaod;

/**
 * Limits explicit replacement-view refreshes.  A native ClockPlugin render already supplies a
 * frame, so allowing every nested refresh request to schedule another frame creates a feedback
 * loop on OOS 16.0.9.
 */
final class AodFrameRefreshGate {
    static final long MIN_INTERVAL_MILLIS = 250L;
    private boolean framePending;
    private long lastFrameDispatchedAt = Long.MIN_VALUE;

    synchronized boolean request(long nowMillis) {
        if (framePending) {
            return false;
        }
        long ageMillis = nowMillis - lastFrameDispatchedAt;
        if (lastFrameDispatchedAt != Long.MIN_VALUE
                && ageMillis >= 0L
                && ageMillis < MIN_INTERVAL_MILLIS) {
            return false;
        }
        framePending = true;
        return true;
    }

    synchronized void markFrameDispatched(long nowMillis) {
        framePending = false;
        lastFrameDispatchedAt = nowMillis;
    }
}
