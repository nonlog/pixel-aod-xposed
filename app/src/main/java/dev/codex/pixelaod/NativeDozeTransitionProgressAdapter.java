package dev.codex.pixelaod;

/**
 * Read-only normalization of the native Keyguard/Doze transition progress seam.
 *
 * <p>This adapter never creates time or progress. It only accepts progress carried by the current
 * SystemUI TransitionStep seam and only for ordinary Lockscreen <-> ambient presentation
 * handoffs. Lifecycle ownership and permission to consume the signal remain separate.</p>
 */
final class NativeDozeTransitionProgressAdapter {
    enum Direction {
        ENTERING_AMBIENT,
        LEAVING_AMBIENT,
        NONE
    }

    enum Phase {
        STARTED,
        RUNNING,
        FINISHED,
        CANCELED,
        UNKNOWN
    }

    private boolean seamAvailable;
    private long generation;
    private Direction direction = Direction.NONE;
    private Phase phase = Phase.UNKNOWN;
    private float transitionProgress = Float.NaN;
    private float ambientFraction = Float.NaN;
    private String from = "unknown";
    private String to = "unknown";
    private String owner = "unknown";
    private String source = "none";
    private boolean reliable;
    private boolean continuousObserved;

    synchronized Snapshot markSeamAvailable(String eventSource) {
        seamAvailable = true;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot observe(String fromName, String toName, float value,
            String phaseName, String ownerName, String eventSource) {
        generation++;
        String nextFrom = normalizeState(fromName);
        String nextTo = normalizeState(toName);
        Phase nextPhase = parsePhase(phaseName);
        String nextOwner = normalize(ownerName);
        boolean newTransition = nextPhase == Phase.STARTED
                || !nextFrom.equals(from)
                || !nextTo.equals(to)
                || !nextOwner.equals(owner);
        if (newTransition || nextPhase == Phase.CANCELED || nextPhase == Phase.UNKNOWN) {
            continuousObserved = false;
        }
        from = nextFrom;
        to = nextTo;
        phase = nextPhase;
        owner = nextOwner;
        direction = directionFor(from, to);
        transitionProgress = normalizeProgress(value);
        source = normalize(eventSource);
        reliable = seamAvailable
                && direction != Direction.NONE
                && phaseCarriesProgress(phase)
                && !Float.isNaN(transitionProgress);
        if (reliable && phase == Phase.RUNNING) {
            continuousObserved = true;
        }
        ambientFraction = reliable
                ? (direction == Direction.ENTERING_AMBIENT
                        ? transitionProgress : 1.0f - transitionProgress)
                : Float.NaN;
        return snapshotLocked();
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    private Snapshot snapshotLocked() {
        return new Snapshot(seamAvailable, generation, direction, phase, transitionProgress,
                ambientFraction, from, to, owner, source, reliable, continuousObserved);
    }

    static Direction directionFor(String fromName, String toName) {
        String from = normalizeState(fromName);
        String to = normalizeState(toName);
        if ("LOCKSCREEN".equals(from) && isAmbient(to)) {
            return Direction.ENTERING_AMBIENT;
        }
        if (isAmbient(from) && "LOCKSCREEN".equals(to)) {
            return Direction.LEAVING_AMBIENT;
        }
        return Direction.NONE;
    }

    static boolean phaseCarriesProgress(Phase phase) {
        return phase == Phase.STARTED || phase == Phase.RUNNING || phase == Phase.FINISHED;
    }

    private static boolean isAmbient(String state) {
        return "AOD".equals(state) || "DOZING".equals(state);
    }

    private static Phase parsePhase(String value) {
        String normalized = normalizeState(value);
        try {
            return Phase.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Phase.UNKNOWN;
        }
    }

    private static float normalizeProgress(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return Float.NaN;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static String normalizeState(String value) {
        return normalize(value).toUpperCase(java.util.Locale.US);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return "unknown";
        }
        return value.trim();
    }

    static final class Snapshot {
        final boolean seamAvailable;
        final long generation;
        final Direction direction;
        final Phase phase;
        final float transitionProgress;
        final float ambientFraction;
        final String from;
        final String to;
        final String owner;
        final String source;
        final boolean reliable;
        final boolean continuousObserved;

        Snapshot(boolean seamAvailable, long generation, Direction direction, Phase phase,
                float transitionProgress, float ambientFraction, String from, String to,
                String owner, String source, boolean reliable, boolean continuousObserved) {
            this.seamAvailable = seamAvailable;
            this.generation = generation;
            this.direction = direction;
            this.phase = phase;
            this.transitionProgress = transitionProgress;
            this.ambientFraction = ambientFraction;
            this.from = from;
            this.to = to;
            this.owner = owner;
            this.source = source;
            this.reliable = reliable;
            this.continuousObserved = continuousObserved;
        }

        boolean canConsume(boolean vendorProgressAllowed, boolean systemAnimationsEnabled) {
            return reliable && continuousObserved
                    && vendorProgressAllowed && systemAnimationsEnabled;
        }

        boolean isRunningSample() {
            return reliable && phase == Phase.RUNNING;
        }

        String describe(boolean vendorProgressAllowed, boolean systemAnimationsEnabled) {
            return "seamAvailable=" + seamAvailable
                    + ",generation=" + generation
                    + ",from=" + from
                    + ",to=" + to
                    + ",direction=" + direction
                    + ",phase=" + phase
                    + ",transitionProgress=" + describeFloat(transitionProgress)
                    + ",ambientFraction=" + describeFloat(ambientFraction)
                    + ",reliable=" + reliable
                    + ",continuousObserved=" + continuousObserved
                    + ",vendorProgressAllowed=" + vendorProgressAllowed
                    + ",systemAnimationsEnabled=" + systemAnimationsEnabled
                    + ",canConsume=" + canConsume(vendorProgressAllowed, systemAnimationsEnabled)
                    + ",owner=" + owner
                    + ",source=" + source;
        }

        private static String describeFloat(float value) {
            return Float.isNaN(value) ? "unknown" : String.valueOf(value);
        }
    }
}
