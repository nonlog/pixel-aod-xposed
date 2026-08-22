package dev.codex.pixelaod;

/**
 * Epoch-scoped latch for an authoritative native Keyguard direct-to-Gone transition.
 *
 * <p>The latch may arm only while the vendor ambient session is still active. Once armed it
 * survives DREAM_STOP long enough to suppress the Pixel lockscreen bridge, and is cleared by
 * Keyguard going-away cancellation/completion or by the next ambient session.</p>
 */
final class NativeDirectGoneHandoff {
    private long ambientEpoch;
    private boolean active;
    private String source = "none";

    synchronized Snapshot observeKeyguardGoingAway(boolean goingAway, long currentAmbientEpoch,
            boolean ambientSessionActive, String eventSource) {
        if (!goingAway) {
            active = false;
            ambientEpoch = 0L;
            source = normalize(eventSource);
            return snapshotLocked();
        }
        if (ambientSessionActive && currentAmbientEpoch > 0L) {
            active = true;
            ambientEpoch = currentAmbientEpoch;
            source = normalize(eventSource);
        }
        return snapshotLocked();
    }

    synchronized Snapshot resetForNewAmbientSession(String eventSource) {
        active = false;
        ambientEpoch = 0L;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized boolean isActive() {
        return active;
    }

    synchronized boolean isActiveFor(long epoch) {
        return active && epoch > 0L && ambientEpoch == epoch;
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    private Snapshot snapshotLocked() {
        return new Snapshot(active, ambientEpoch, source);
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    static final class Snapshot {
        final boolean active;
        final long ambientEpoch;
        final String source;

        Snapshot(boolean active, long ambientEpoch, String source) {
            this.active = active;
            this.ambientEpoch = ambientEpoch;
            this.source = source;
        }

        String describe() {
            return "active=" + active + ",ambientEpoch=" + ambientEpoch + ",source=" + source;
        }
    }
}
