package dev.codex.pixelaod;

/**
 * Read-only policy state for the native SystemUI screen-off animation decision.
 *
 * <p>Existing stable Pixel motion is blocked only by an explicit native negative signal. Unknown
 * state deliberately preserves the already-shipping fallback choreography. By contrast, a future
 * vendor Doze-progress adapter may consume continuous progress only after the native permission
 * inputs are positively known for the current transition.</p>
 */
final class VendorScreenOffAnimationEligibility {
    private Boolean displayNeedsBlanking;
    private Boolean shouldControlScreenOff;
    private Boolean shouldAnimateDozingChange;
    private long transitionGeneration;
    private String source = "none";

    synchronized Snapshot beginTransition(String eventSource) {
        transitionGeneration++;
        shouldControlScreenOff = null;
        shouldAnimateDozingChange = null;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot observeDisplayNeedsBlanking(boolean needsBlanking, String eventSource) {
        displayNeedsBlanking = needsBlanking;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot observeShouldControlScreenOff(boolean shouldControl, String eventSource) {
        shouldControlScreenOff = shouldControl;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized Snapshot observeShouldAnimateDozingChange(boolean shouldAnimate,
            String eventSource) {
        shouldAnimateDozingChange = shouldAnimate;
        source = normalize(eventSource);
        return snapshotLocked();
    }

    synchronized boolean allowsExistingMorph() {
        // OOS can report shouldControlScreenOff=false even for a real Keyguard -> AOD transition.
        // That flag describes who controls the screen-off handoff, not whether SystemUI permits
        // presentation motion. Treat only physical display blanking or an explicit dozing-change
        // animation denial as authority to snap the already-stable Pixel morph.
        return !Boolean.TRUE.equals(displayNeedsBlanking)
                && !Boolean.FALSE.equals(shouldAnimateDozingChange);
    }

    synchronized boolean allowsVendorProgress() {
        return Boolean.FALSE.equals(displayNeedsBlanking)
                && Boolean.TRUE.equals(shouldControlScreenOff)
                && Boolean.TRUE.equals(shouldAnimateDozingChange);
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    private Snapshot snapshotLocked() {
        return new Snapshot(transitionGeneration, displayNeedsBlanking, shouldControlScreenOff,
                shouldAnimateDozingChange, allowsExistingMorph(), allowsVendorProgress(), source);
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "unknown" : value;
    }

    static final class Snapshot {
        final long transitionGeneration;
        final Boolean displayNeedsBlanking;
        final Boolean shouldControlScreenOff;
        final Boolean shouldAnimateDozingChange;
        final boolean allowsExistingMorph;
        final boolean allowsVendorProgress;
        final String source;

        Snapshot(long transitionGeneration, Boolean displayNeedsBlanking,
                Boolean shouldControlScreenOff, Boolean shouldAnimateDozingChange,
                boolean allowsExistingMorph, boolean allowsVendorProgress, String source) {
            this.transitionGeneration = transitionGeneration;
            this.displayNeedsBlanking = displayNeedsBlanking;
            this.shouldControlScreenOff = shouldControlScreenOff;
            this.shouldAnimateDozingChange = shouldAnimateDozingChange;
            this.allowsExistingMorph = allowsExistingMorph;
            this.allowsVendorProgress = allowsVendorProgress;
            this.source = source;
        }

        String describe() {
            return "generation=" + transitionGeneration
                    + ",displayNeedsBlanking=" + describeNullable(displayNeedsBlanking)
                    + ",shouldControlScreenOff=" + describeNullable(shouldControlScreenOff)
                    + ",shouldAnimateDozingChange=" + describeNullable(shouldAnimateDozingChange)
                    + ",allowsExistingMorph=" + allowsExistingMorph
                    + ",allowsVendorProgress=" + allowsVendorProgress
                    + ",source=" + source;
        }

        private static String describeNullable(Boolean value) {
            return value == null ? "unknown" : String.valueOf(value);
        }
    }
}
