package dev.codex.pixelaod;

/**
 * Coalesces rapid notification listener callbacks without delaying the first snapshot.
 */
final class NotificationSnapshotRefreshGate {
    static final long MIN_INTERVAL_MILLIS = 500L;
    static final long NO_SCHEDULE = -1L;

    private boolean snapshotPending;
    private long lastDispatchAtMillis = Long.MIN_VALUE;

    synchronized long requestDelayMillis(long nowMillis) {
        if (snapshotPending) {
            return NO_SCHEDULE;
        }
        snapshotPending = true;
        if (lastDispatchAtMillis == Long.MIN_VALUE) {
            return 0L;
        }
        long elapsedMillis = Math.max(0L, nowMillis - lastDispatchAtMillis);
        return elapsedMillis >= MIN_INTERVAL_MILLIS
                ? 0L : MIN_INTERVAL_MILLIS - elapsedMillis;
    }

    synchronized void markDispatched(long nowMillis) {
        snapshotPending = false;
        lastDispatchAtMillis = nowMillis;
    }
}
