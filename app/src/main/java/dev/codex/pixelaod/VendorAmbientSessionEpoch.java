package dev.codex.pixelaod;

/** Monotonic module-side lifetime token for one vendor-owned ambient session. */
final class VendorAmbientSessionEpoch {
    private long epoch;
    private boolean active;
    private String source = "none";

    synchronized Snapshot begin(String beginSource) {
        epoch++;
        active = true;
        source = normalize(beginSource);
        return snapshotLocked();
    }

    synchronized Snapshot invalidate(String invalidateSource) {
        epoch++;
        active = false;
        source = normalize(invalidateSource);
        return snapshotLocked();
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    synchronized boolean isCurrent(long candidateEpoch) {
        return active && candidateEpoch > 0L && candidateEpoch == epoch;
    }

    private Snapshot snapshotLocked() {
        return new Snapshot(epoch, active, source);
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    static final class Snapshot {
        final long epoch;
        final boolean active;
        final String source;

        Snapshot(long epoch, boolean active, String source) {
            this.epoch = epoch;
            this.active = active;
            this.source = source;
        }

        String describe() {
            return "epoch=" + epoch + ",active=" + active + ",source=" + source;
        }
    }
}
