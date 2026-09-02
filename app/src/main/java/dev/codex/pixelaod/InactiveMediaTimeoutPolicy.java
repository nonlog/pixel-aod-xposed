package dev.codex.pixelaod;

/** Elapsed-realtime arithmetic for inactive media expiry; deep sleep must count toward age. */
final class InactiveMediaTimeoutPolicy {
    private InactiveMediaTimeoutPolicy() {
    }

    static long deadline(long startedAtElapsedRealtime, long timeoutMillis) {
        if (startedAtElapsedRealtime <= 0L || timeoutMillis <= 0L) {
            return startedAtElapsedRealtime;
        }
        if (startedAtElapsedRealtime > Long.MAX_VALUE - timeoutMillis) {
            return Long.MAX_VALUE;
        }
        return startedAtElapsedRealtime + timeoutMillis;
    }

    static long remainingDelay(long startedAtElapsedRealtime, long nowElapsedRealtime,
            long timeoutMillis) {
        long deadline = deadline(startedAtElapsedRealtime, timeoutMillis);
        if (deadline <= 0L || nowElapsedRealtime >= deadline) {
            return 0L;
        }
        return deadline - nowElapsedRealtime;
    }

    static boolean isWithinTimeout(long startedAtElapsedRealtime, long nowElapsedRealtime,
            long timeoutMillis) {
        if (startedAtElapsedRealtime <= 0L || nowElapsedRealtime < startedAtElapsedRealtime) {
            return false;
        }
        return nowElapsedRealtime < deadline(startedAtElapsedRealtime, timeoutMillis);
    }
}
