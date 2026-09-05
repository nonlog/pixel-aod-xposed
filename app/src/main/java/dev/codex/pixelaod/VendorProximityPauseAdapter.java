package dev.codex.pixelaod;

/**
 * Presentation-only normalization of the OPlus proximity dwell pipeline.
 *
 * <p>Current OOS owns the actual sensor and dwell timer inside
 * OplusWakeUpController.ProximityTask. A raw NEAR request therefore enters PAUSING, while the
 * delayed vendor task commit enters PAUSED. FAR cancels a pending vendor task or resumes from the
 * committed PAUSED state. This class never registers a sensor, schedules a timer, or controls
 * panel/doze power.</p>
 */
final class VendorProximityPauseAdapter {
    enum Phase {
        ACTIVE,
        PAUSING,
        PAUSED
    }

    private boolean rawKnown;
    private boolean rawNear;
    private boolean committedKnown;
    private boolean committedNear;
    private long generation;
    private String source = "none";

    synchronized Snapshot observeRawNear(boolean near, String eventSource) {
        Phase previous = phaseLocked();
        rawKnown = true;
        rawNear = near;
        generation++;
        source = normalize(eventSource);
        return snapshotLocked(previous);
    }

    synchronized Snapshot observeCommittedNear(boolean near, String eventSource) {
        Phase previous = phaseLocked();
        committedKnown = true;
        committedNear = near;
        if (!near) {
            // A committed FAR is authoritative even if an older raw NEAR observation was retained.
            rawKnown = true;
            rawNear = false;
        }
        generation++;
        source = normalize(eventSource);
        return snapshotLocked(previous);
    }

    synchronized Snapshot reset(String eventSource) {
        Phase previous = phaseLocked();
        rawKnown = false;
        rawNear = false;
        committedKnown = false;
        committedNear = false;
        generation++;
        source = normalize(eventSource);
        return snapshotLocked(previous);
    }

    synchronized Snapshot snapshot() {
        Phase phase = phaseLocked();
        return new Snapshot(generation, phase, phase, rawKnown, rawNear,
                committedKnown, committedNear, source);
    }

    synchronized boolean blocksPresentation() {
        return phaseLocked() == Phase.PAUSED;
    }

    synchronized boolean blocksNotificationPulse() {
        return phaseLocked() != Phase.ACTIVE;
    }

    private Snapshot snapshotLocked(Phase previous) {
        Phase phase = phaseLocked();
        return new Snapshot(generation, previous, phase, rawKnown, rawNear,
                committedKnown, committedNear, source);
    }

    private Phase phaseLocked() {
        if (committedKnown && committedNear) {
            return Phase.PAUSED;
        }
        if (rawKnown && rawNear) {
            return Phase.PAUSING;
        }
        return Phase.ACTIVE;
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    static final class Snapshot {
        final long generation;
        final Phase previousPhase;
        final Phase phase;
        final boolean rawKnown;
        final boolean rawNear;
        final boolean committedKnown;
        final boolean committedNear;
        final String source;

        Snapshot(long generation, Phase previousPhase, Phase phase,
                boolean rawKnown, boolean rawNear,
                boolean committedKnown, boolean committedNear, String source) {
            this.generation = generation;
            this.previousPhase = previousPhase;
            this.phase = phase;
            this.rawKnown = rawKnown;
            this.rawNear = rawNear;
            this.committedKnown = committedKnown;
            this.committedNear = committedNear;
            this.source = source;
        }

        boolean phaseChanged() {
            return previousPhase != phase;
        }

        boolean resumedPresentation() {
            return previousPhase == Phase.PAUSED && phase == Phase.ACTIVE;
        }

        boolean blocksPresentation() {
            return phase == Phase.PAUSED;
        }

        boolean blocksNotificationPulse() {
            return phase != Phase.ACTIVE;
        }

        String describe() {
            return "generation=" + generation
                    + ",phase=" + phase
                    + ",previousPhase=" + previousPhase
                    + ",raw=" + (rawKnown ? String.valueOf(rawNear) : "unknown")
                    + ",committed=" + (committedKnown
                    ? String.valueOf(committedNear) : "unknown")
                    + ",presentationBlocked=" + blocksPresentation()
                    + ",notificationPulseBlocked=" + blocksNotificationPulse()
                    + ",source=" + source;
        }
    }
}
