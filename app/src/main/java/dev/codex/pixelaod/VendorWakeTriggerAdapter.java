package dev.codex.pixelaod;

/**
 * Read-only normalization of the exact OPlus wake-trigger fanout seam.
 *
 * <p>The current supported OOS build classifies its own sensors/gestures before calling
 * {@code OplusWakeUpController#notifyWakeUpCallback(int)}. Pixel AOD observes that result only;
 * it does not register a sensor, request a gesture mode, extend a vendor wake window, or force
 * display state.</p>
 */
final class VendorWakeTriggerAdapter {
    enum Kind {
        SINGLE_TAP,
        TILT_PICKUP,
        MOTION,
        UNKNOWN
    }

    private VendorWakeTriggerAdapter() {
    }

    static Observation fromNotifyWakeUpType(int rawType, String source) {
        switch (rawType) {
            case 0:
                return new Observation(rawType, Kind.SINGLE_TAP, "tap", true, source);
            case 1:
                return new Observation(rawType, Kind.TILT_PICKUP, "pickup", true, source);
            case 2:
                return new Observation(rawType, Kind.MOTION, "motion", true, source);
            default:
                return new Observation(rawType, Kind.UNKNOWN, "unknown", false, source);
        }
    }

    static final class Observation {
        final int rawType;
        final Kind kind;
        final String normalizedTrigger;
        final boolean presentationCandidate;
        final String source;

        Observation(int rawType, Kind kind, String normalizedTrigger,
                boolean presentationCandidate, String source) {
            this.rawType = rawType;
            this.kind = kind != null ? kind : Kind.UNKNOWN;
            this.normalizedTrigger = normalize(normalizedTrigger);
            this.presentationCandidate = presentationCandidate;
            this.source = normalize(source);
        }

        String describe() {
            return "rawType=" + rawType
                    + ",kind=" + kind
                    + ",normalizedTrigger=" + normalizedTrigger
                    + ",presentationCandidate=" + presentationCandidate
                    + ",source=" + source;
        }
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }
}
