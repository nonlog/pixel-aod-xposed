package dev.codex.pixelaod;

/** Exact COUI 2.5 motion policy for partial-AOD content rows. */
final class CouiClockAodContentMotionPolicy {
    private CouiClockAodContentMotionPolicy() {
    }

    /**
     * While an animated transition is leaving partial AOD, COUI keeps the content row at its
     * current position and fades it out. This avoids a hidden row jumping back to the default
     * non-AOD anchor before it disappears.
     */
    static boolean preserveCurrentPosition(boolean partialAodActive, boolean animate) {
        return !partialAodActive && animate;
    }

    /**
     * COUI 2.5 snaps notification/media rows to their target geometry before an alpha animation;
     * their X/Y is never part of the normal AOD-entry property animation.
     */
    static boolean animateTranslation() {
        return false;
    }
}
