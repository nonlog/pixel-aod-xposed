package dev.codex.pixelaod;

/**
 * Read-only normalization of Android/SystemUI Keyguard transition semantics.
 *
 * <p>The adapter deliberately stores only string-normalized state from the vendor SystemUI class
 * loader so the module does not acquire a compile-time dependency on SystemUI implementation
 * classes. Unknown or unsupported inputs return no authoritative decision and preserve the
 * existing fallback behavior.</p>
 */
final class NativeKeyguardSceneEligibility {
    enum Scene {
        OFF,
        DOZING,
        DREAMING,
        AOD,
        ALTERNATE_BOUNCER,
        PRIMARY_BOUNCER,
        LOCKSCREEN,
        GLANCEABLE_HUB,
        GONE,
        OCCLUDED,
        UNKNOWN
    }

    enum Phase {
        STARTED,
        RUNNING,
        FINISHED,
        CANCELED,
        UNKNOWN
    }

    private long generation;
    private Scene from = Scene.UNKNOWN;
    private Scene to = Scene.UNKNOWN;
    private Phase phase = Phase.UNKNOWN;
    private float value = Float.NaN;
    private String owner = "unknown";
    private String source = "none";

    synchronized Snapshot observe(String fromName, String toName, float transitionValue,
            String phaseName, String ownerName, String eventSource) {
        generation++;
        from = parseScene(fromName);
        to = parseScene(toName);
        phase = parsePhase(phaseName);
        value = normalizeProgress(transitionValue);
        owner = normalize(ownerName);
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    synchronized boolean allowsPresentationFallbackTrue() {
        Boolean decision = presentationDecision(from, to, phase);
        return decision == null || decision;
    }

    private Snapshot snapshotLocked() {
        Boolean allowed = presentationDecision(from, to, phase);
        return new Snapshot(generation, from, to, phase, value, owner, source, allowed);
    }

    static Boolean presentationDecision(Scene from, Scene to, Phase phase) {
        if (phase == null || phase == Phase.UNKNOWN) {
            return null;
        }
        if (phase == Phase.FINISHED) {
            return sceneEligibility(to);
        }
        if (phase == Phase.CANCELED) {
            return sceneEligibility(from);
        }

        Boolean fromAllowed = sceneEligibility(from);
        Boolean toAllowed = sceneEligibility(to);
        if (Boolean.FALSE.equals(fromAllowed) || Boolean.FALSE.equals(toAllowed)) {
            return false;
        }
        if (Boolean.TRUE.equals(fromAllowed) && Boolean.TRUE.equals(toAllowed)) {
            return true;
        }
        return null;
    }

    static boolean supportsNonLockscreenAodBypass(Scene from, Scene to, Phase phase) {
        if (phase == null || phase == Phase.UNKNOWN || phase == Phase.CANCELED) {
            return false;
        }
        if (phase == Phase.FINISHED && to == Scene.GONE) {
            return true;
        }
        return from == Scene.GONE && (to == Scene.DOZING || to == Scene.AOD);
    }
    static boolean becameIneligible(Boolean before, Boolean after) {
        return !Boolean.FALSE.equals(before) && Boolean.FALSE.equals(after);
    }

    static boolean becameEligible(Boolean before, Boolean after) {
        return Boolean.FALSE.equals(before) && Boolean.TRUE.equals(after);
    }

    static boolean isDozePresentationTransition(Scene from, Scene to) {
        if (from == null || to == null || from == Scene.UNKNOWN || to == Scene.UNKNOWN) {
            return false;
        }
        if (!Boolean.TRUE.equals(sceneEligibility(from))
                || !Boolean.TRUE.equals(sceneEligibility(to))) {
            return false;
        }
        return isAmbientScene(from) || isAmbientScene(to);
    }

    private static boolean isAmbientScene(Scene scene) {
        return scene == Scene.AOD || scene == Scene.DOZING;
    }

    private static Boolean sceneEligibility(Scene scene) {
        if (scene == null || scene == Scene.UNKNOWN) {
            return null;
        }
        switch (scene) {
            case LOCKSCREEN:
            case AOD:
            case DOZING:
                return true;
            case OFF:
            case DREAMING:
            case ALTERNATE_BOUNCER:
            case PRIMARY_BOUNCER:
            case GLANCEABLE_HUB:
            case GONE:
            case OCCLUDED:
                return false;
            default:
                return null;
        }
    }

    private static Scene parseScene(String value) {
        String normalized = normalize(value).toUpperCase(java.util.Locale.US);
        if ("UNDEFINED".equals(normalized) || "NULL".equals(normalized)) {
            return Scene.UNKNOWN;
        }
        try {
            return Scene.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Scene.UNKNOWN;
        }
    }

    private static Phase parsePhase(String value) {
        String normalized = normalize(value).toUpperCase(java.util.Locale.US);
        try {
            return Phase.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Phase.UNKNOWN;
        }
    }

    private static float normalizeProgress(float progress) {
        if (Float.isNaN(progress) || Float.isInfinite(progress)) {
            return Float.NaN;
        }
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        return value.trim();
    }

    static final class Snapshot {
        final long generation;
        final Scene from;
        final Scene to;
        final Phase phase;
        final float value;
        final String owner;
        final String source;
        final Boolean presentationAllowed;

        Snapshot(long generation, Scene from, Scene to, Phase phase, float value,
                String owner, String source, Boolean presentationAllowed) {
            this.generation = generation;
            this.from = from;
            this.to = to;
            this.phase = phase;
            this.value = value;
            this.owner = owner;
            this.source = source;
            this.presentationAllowed = presentationAllowed;
        }

        boolean hasAuthoritativeDecision() {
            return presentationAllowed != null;
        }

        boolean allowsPresentationFallbackTrue() {
            return presentationAllowed == null || presentationAllowed;
        }

        boolean isDozePresentationTransition() {
            return NativeKeyguardSceneEligibility.isDozePresentationTransition(from, to);
        }

        boolean supportsNonLockscreenAodBypass() {
            return NativeKeyguardSceneEligibility.supportsNonLockscreenAodBypass(from, to, phase);
        }

        String describe() {
            return "generation=" + generation
                    + ",from=" + from
                    + ",to=" + to
                    + ",phase=" + phase
                    + ",value=" + (Float.isNaN(value) ? "unknown" : value)
                    + ",presentationAllowed="
                    + (presentationAllowed == null ? "fallback" : presentationAllowed)
                    + ",owner=" + owner
                    + ",source=" + source;
        }
    }
}
