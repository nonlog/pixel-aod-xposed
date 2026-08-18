package dev.codex.pixelaod;

/**
 * Direct ClockPlugin-state mapping matching COUI Expressive 2.5's syncHost contract.
 *
 * <p>ClockPlugin does not have a transient "hide replacement" presentation. A non-zero UI state
 * with a known clock size remains a presentation update on the same persistent host. State 0 (or
 * a missing state/size) simply produces no presentation update. Actual host removal belongs to
 * ClockPlugin unload/module teardown, not to the rendered UI-state vocabulary.</p>
 */
final class CouiClockPluginPresentationMapper {
    static final int UI_STATE_UNLOCKED = 1;
    static final int UI_STATE_KEYGUARD = 2;
    static final int UI_STATE_AOD = 3;
    static final int UI_STATE_PANORAMIC_AOD = 5;

    static final int CLOCK_SIZE_SMALL = 0;
    static final int CLOCK_SIZE_LARGE = 1;
    static final int CLOCK_SIZE_IMMERSED = 2;

    // HIDE is retained only so old pure-policy tests/source still compile while the runtime no
    // longer produces or consumes it. COUI reference syncHost never hides the replacement for a
    // transient rendered UI state.
    enum Action {
        PRESENT,
        HIDE,
        HOLD
    }

    static final class Mapping {
        private final Action action;
        private final CouiClockPresentationModel presentation;
        private final boolean animate;

        private Mapping(Action action, CouiClockPresentationModel presentation,
                boolean animate) {
            this.action = action;
            this.presentation = presentation;
            this.animate = animate;
        }

        static Mapping present(CouiClockPresentationModel presentation, boolean animate) {
            return new Mapping(Action.PRESENT, presentation, animate);
        }

        static Mapping hold() {
            return new Mapping(Action.HOLD, null, false);
        }

        Action action() {
            return action;
        }

        CouiClockPresentationModel presentation() {
            return presentation;
        }

        boolean animate() {
            return animate;
        }
    }

    private CouiClockPluginPresentationMapper() {
    }

    static Mapping map(Integer uiState, Integer clockSizeState, boolean animate,
            CouiClockPresentationModel.AodContent content) {
        return mapReference(uiState, clockSizeState, null, animate, content);
    }

    /**
     * Mirrors COUI 2.5 syncHost's presentation branch.
     *
     * @param lastLockscreenScene the last known non-AOD scene, used only by panoramic AOD when the
     *                            vendor size tracker is temporarily unavailable.
     */
    static Mapping mapReference(Integer uiState, Integer clockSizeState,
            CouiClockPresentationModel.Scene lastLockscreenScene, boolean animate,
            CouiClockPresentationModel.AodContent content) {
        if (uiState == null || uiState == 0) {
            return Mapping.hold();
        }

        CouiClockPresentationModel.AodContent safeContent = content == null
                ? CouiClockPresentationModel.AodContent.none() : content;
        boolean dozing = uiState == UI_STATE_AOD || uiState == UI_STATE_PANORAMIC_AOD;
        boolean partialAod = uiState == UI_STATE_AOD;
        CouiClockPresentationModel.Scene scene = sceneForClockSize(clockSizeState);

        if (partialAod) {
            // COUI syncHost first forces requested LARGE for partial AOD; the presentation model
            // then derives visual SMALL when notification/media content is actually present.
            scene = CouiClockPresentationModel.Scene.LARGE;
        } else if (uiState == UI_STATE_PANORAMIC_AOD && scene == null) {
            scene = lastLockscreenScene != null
                    ? lastLockscreenScene : CouiClockPresentationModel.Scene.LARGE;
        }

        if (scene == null) {
            return Mapping.hold();
        }
        CouiClockPresentationModel.AodContent presentationContent =
                uiState == UI_STATE_PANORAMIC_AOD
                        ? CouiClockPresentationModel.AodContent.none() : safeContent;
        return Mapping.present(new CouiClockPresentationModel(
                scene, dozing, partialAod, presentationContent), animate);
    }

    static Mapping forcedLockscreenEntry(Integer clockSizeState, boolean animate,
            CouiClockPresentationModel.AodContent content) {
        CouiClockPresentationModel.Scene scene = sceneForClockSize(clockSizeState);
        return scene == null
                ? Mapping.hold()
                : Mapping.present(new CouiClockPresentationModel(
                        scene, false, false, content), animate);
    }

    /**
     * Compatibility overload for older callers. Interactive/keyguard/module policy do not alter
     * ClockPlugin's presentation mapping in the COUI reference path.
     */
    static Mapping map(Integer uiState, Integer clockSizeState, boolean uiStateAnimating,
            boolean interactive, boolean keyguardShowing, boolean moduleAodAllowed,
            boolean animate, CouiClockPresentationModel.AodContent content) {
        return mapReference(uiState, clockSizeState, null, animate, content);
    }

    static CouiClockPresentationModel.Scene sceneForClockSize(Integer clockSizeState) {
        if (clockSizeState == null) {
            return null;
        }
        switch (clockSizeState) {
            case CLOCK_SIZE_SMALL:
                return CouiClockPresentationModel.Scene.SMALL;
            case CLOCK_SIZE_LARGE:
                return CouiClockPresentationModel.Scene.LARGE;
            case CLOCK_SIZE_IMMERSED:
                return CouiClockPresentationModel.Scene.IMMERSED;
            default:
                return null;
        }
    }
}
