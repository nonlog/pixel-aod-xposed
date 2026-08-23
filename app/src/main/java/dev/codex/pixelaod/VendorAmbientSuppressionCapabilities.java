package dev.codex.pixelaod;

/**
 * Typed, read-only view of validated SystemUI ambient suppression signals.
 *
 * <p>Each capability is deliberately independent. A native signal is allowed to affect only the
 * capability whose semantics are proven on the current SystemUI build; missing coverage remains
 * UNKNOWN and therefore falls back to the existing vendor lifecycle.</p>
 */
final class VendorAmbientSuppressionCapabilities {
    enum Decision {
        ALLOW,
        DENY,
        UNKNOWN
    }

    enum Reason {
        NONE,
        AMBIENT_DISPLAY_SUPPRESSED,
        AOD_POWER_SAVE,
        UNKNOWN
    }

    private Boolean alwaysOnSuppressed;
    private Boolean aodPowerSave;
    private long generation;
    private String source = "none";

    synchronized Snapshot observeAlwaysOnSuppressed(boolean suppressed, String eventSource) {
        generation++;
        alwaysOnSuppressed = suppressed;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot observeAodPowerSave(boolean powerSave, String eventSource) {
        generation++;
        aodPowerSave = powerSave;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    private Snapshot snapshotLocked() {
        Decision baseAod = baseAodDecision(alwaysOnSuppressed, aodPowerSave);
        Decision notificationPulse = notificationPulseDecision(aodPowerSave);
        Reason baseAodReason = baseAod == Decision.DENY
                ? (Boolean.TRUE.equals(aodPowerSave)
                        ? Reason.AOD_POWER_SAVE : Reason.AMBIENT_DISPLAY_SUPPRESSED)
                : (baseAod == Decision.UNKNOWN ? Reason.UNKNOWN : Reason.NONE);
        Reason notificationReason = notificationPulse == Decision.DENY
                ? Reason.AOD_POWER_SAVE
                : (notificationPulse == Decision.UNKNOWN ? Reason.UNKNOWN : Reason.NONE);
        return new Snapshot(generation, alwaysOnSuppressed, aodPowerSave,
                baseAod, notificationPulse,
                Decision.UNKNOWN, Decision.UNKNOWN, Decision.UNKNOWN,
                baseAodReason, notificationReason, source);
    }

    static Decision baseAodDecision(Boolean alwaysOnSuppressed, Boolean aodPowerSave) {
        if (Boolean.TRUE.equals(alwaysOnSuppressed) || Boolean.TRUE.equals(aodPowerSave)) {
            return Decision.DENY;
        }
        if (Boolean.FALSE.equals(alwaysOnSuppressed) && Boolean.FALSE.equals(aodPowerSave)) {
            return Decision.ALLOW;
        }
        return Decision.UNKNOWN;
    }

    static Decision notificationPulseDecision(Boolean aodPowerSave) {
        // Current OOS directly proves mAodPowerSave as a native pulse suppressor, but clearing
        // that one reason does not prove that every other pulse suppressor is also clear.
        return Boolean.TRUE.equals(aodPowerSave) ? Decision.DENY : Decision.UNKNOWN;
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    static final class Snapshot {
        final long generation;
        final Boolean alwaysOnSuppressed;
        final Boolean aodPowerSave;
        final Decision baseAod;
        final Decision notificationPulse;
        final Decision contextualPresentation;
        final Decision wakeGestures;
        final Decision authenticationPulse;
        final Reason baseAodReason;
        final Reason notificationPulseReason;
        final String source;

        Snapshot(long generation, Boolean alwaysOnSuppressed, Boolean aodPowerSave,
                Decision baseAod, Decision notificationPulse, Decision contextualPresentation,
                Decision wakeGestures, Decision authenticationPulse, Reason baseAodReason,
                Reason notificationPulseReason, String source) {
            this.generation = generation;
            this.alwaysOnSuppressed = alwaysOnSuppressed;
            this.aodPowerSave = aodPowerSave;
            this.baseAod = baseAod;
            this.notificationPulse = notificationPulse;
            this.contextualPresentation = contextualPresentation;
            this.wakeGestures = wakeGestures;
            this.authenticationPulse = authenticationPulse;
            this.baseAodReason = baseAodReason;
            this.notificationPulseReason = notificationPulseReason;
            this.source = source;
        }

        boolean baseAodDenied() {
            return baseAod == Decision.DENY;
        }

        boolean notificationPulseDenied() {
            return notificationPulse == Decision.DENY;
        }

        String baseAodReasonLabel() {
            if (baseAodReason == Reason.AOD_POWER_SAVE) {
                return "vendor-aod-power-save";
            }
            if (baseAodReason == Reason.AMBIENT_DISPLAY_SUPPRESSED) {
                return "vendor-ambient-display-suppressed";
            }
            return baseAodReason == Reason.UNKNOWN
                    ? "vendor-ambient-suppression-unknown" : "vendor-ambient-unsuppressed";
        }

        String describe() {
            return "generation=" + generation
                    + ",alwaysOnSuppressed=" + describeNullable(alwaysOnSuppressed)
                    + ",aodPowerSave=" + describeNullable(aodPowerSave)
                    + ",baseAod=" + baseAod
                    + ",baseAodReason=" + baseAodReason
                    + ",notificationPulse=" + notificationPulse
                    + ",notificationPulseReason=" + notificationPulseReason
                    + ",contextualPresentation=" + contextualPresentation
                    + ",wakeGestures=" + wakeGestures
                    + ",authenticationPulse=" + authenticationPulse
                    + ",source=" + source;
        }

        private static String describeNullable(Boolean value) {
            return value == null ? "unknown" : String.valueOf(value);
        }
    }
}
