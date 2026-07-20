package dev.codex.pixelaod;

/**
 * Converts OPlus ClockPlugin render state into one persistent host presentation.
 *
 * <p>Transient render states must never tear down a visible clock during a
 * lockscreen-to-AOD handoff. {@link Scene#HOLD} therefore preserves the last committed scene.</p>
 */
final class ClockPluginSceneMachine {
    static final int UI_STATE_UNLOCKED = 1;
    static final int UI_STATE_KEYGUARD = 2;
    static final int UI_STATE_AOD = 3;
    static final int UI_STATE_PANORAMIC_AOD = 5;

    static final int CLOCK_SIZE_SMALL = 0;
    static final int CLOCK_SIZE_LARGE = 1;

    enum Scene {
        HOLD,
        HIDDEN,
        LOCKSCREEN_LARGE,
        LOCKSCREEN_SMALL,
        AOD_LARGE,
        AOD_SMALL;

        boolean isLockscreen() {
            return this == LOCKSCREEN_LARGE || this == LOCKSCREEN_SMALL;
        }

        boolean isAod() {
            return this == AOD_LARGE || this == AOD_SMALL;
        }
    }

    static final class Decision {
        final Scene scene;
        final Scene previousScene;
        final boolean changed;
        final boolean enteringAod;
        final boolean leavingAod;
        final boolean preparingAod;
        final boolean staleLockscreenRenderRejected;

        Decision(Scene scene, Scene previousScene, boolean changed,
                boolean enteringAod, boolean leavingAod, boolean preparingAod) {
            this(scene, previousScene, changed, enteringAod, leavingAod, preparingAod, false);
        }

        Decision(Scene scene, Scene previousScene, boolean changed,
                boolean enteringAod, boolean leavingAod, boolean preparingAod,
                boolean staleLockscreenRenderRejected) {
            this.scene = scene;
            this.previousScene = previousScene;
            this.changed = changed;
            this.enteringAod = enteringAod;
            this.leavingAod = leavingAod;
            this.preparingAod = preparingAod;
            this.staleLockscreenRenderRejected = staleLockscreenRenderRejected;
        }

        Decision withEnteringAod(boolean value) {
            if (enteringAod == value) {
                return this;
            }
            return new Decision(scene, previousScene, changed, value, leavingAod, preparingAod,
                    staleLockscreenRenderRejected);
        }
    }

    private Scene committedScene = Scene.HIDDEN;

    Decision resolve(Integer uiState, Integer clockSizeState, boolean uiStateAnimating,
            boolean moduleAodAllowed, boolean preserveAodWhileLifecycleSettles,
            boolean compactAod, boolean interactive, boolean keyguardShowing) {
        return resolve(uiState, clockSizeState, uiStateAnimating, moduleAodAllowed,
                preserveAodWhileLifecycleSettles, compactAod, interactive, keyguardShowing,
                false);
    }

    Decision resolve(Integer uiState, Integer clockSizeState, boolean uiStateAnimating,
            boolean moduleAodAllowed, boolean preserveAodWhileLifecycleSettles,
            boolean compactAod, boolean interactive, boolean keyguardShowing,
            boolean displayInAodState) {
        if (committedScene.isAod()
                && uiState != null
                && uiState == UI_STATE_KEYGUARD
                && !interactive
                && displayInAodState) {
            return new Decision(committedScene, committedScene, false,
                    false, false, false, true);
        }
        Scene requested = resolveRequestedScene(uiState, clockSizeState, uiStateAnimating,
                moduleAodAllowed, compactAod, interactive, keyguardShowing);
        // ClockPlugin can publish its AOD render state one frame before the module lifecycle
        // says its overlay may draw.  Preserve the already-rendered lockscreen scene until the
        // policy catches up; once an AOD scene is committed, a denied policy still hides it.
        if (requested == Scene.HIDDEN
                && isAodState(uiState)
                && !moduleAodAllowed
                && preserveAodWhileLifecycleSettles
                && committedScene != Scene.HIDDEN) {
            return new Decision(committedScene, committedScene, false, false, false,
                    committedScene.isLockscreen());
        }
        if (requested == Scene.HOLD) {
            return new Decision(committedScene, committedScene, false, false, false, false);
        }

        Scene previous = committedScene;
        committedScene = requested;
        boolean changed = previous != requested;
        return new Decision(requested, previous, changed,
                changed && previous.isLockscreen() && requested.isAod(),
                changed && previous.isAod() && requested.isLockscreen(),
                false);
    }

    private static boolean isAodState(Integer uiState) {
        return uiState != null
                && (uiState == UI_STATE_AOD || uiState == UI_STATE_PANORAMIC_AOD);
    }

    private static Scene resolveRequestedScene(Integer uiState, Integer clockSizeState,
            boolean uiStateAnimating, boolean moduleAodAllowed, boolean compactAod,
            boolean interactive, boolean keyguardShowing) {
        if (uiState == null || uiState == 0) {
            return Scene.HOLD;
        }
        switch (uiState) {
            case UI_STATE_UNLOCKED:
                // OPlus emits UNLOCKED while the ClockPlugin's own keyguard animation is still
                // running. Rebuild a lockscreen scene even if an earlier lifecycle callback
                // temporarily hid the host; a settled unlocked state remains eligible to hide it.
                if (interactive && uiStateAnimating) {
                    return clockSizeState != null && clockSizeState == CLOCK_SIZE_SMALL
                            ? Scene.LOCKSCREEN_SMALL : Scene.LOCKSCREEN_LARGE;
                }
                return interactive && !keyguardShowing ? Scene.HIDDEN : Scene.HOLD;
            case UI_STATE_KEYGUARD:
                return clockSizeState != null && clockSizeState == CLOCK_SIZE_SMALL
                        ? Scene.LOCKSCREEN_SMALL : Scene.LOCKSCREEN_LARGE;
            case UI_STATE_AOD:
            case UI_STATE_PANORAMIC_AOD:
                if (!moduleAodAllowed) {
                    return Scene.HIDDEN;
                }
                return compactAod ? Scene.AOD_SMALL : Scene.AOD_LARGE;
            default:
                return Scene.HOLD;
        }
    }
}
