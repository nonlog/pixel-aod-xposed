package dev.codex.pixelaod;

/**
 * Monotonic cancellation token for host-owned AOD entry and live-content transitions.
 */
final class CouiClockTransitionGeneration {
    private long generation;

    long begin() {
        return ++generation;
    }

    void invalidate() {
        generation++;
    }

    boolean isCurrent(long candidate) {
        return candidate != 0L && candidate == generation;
    }

    long current() {
        return generation;
    }
}
