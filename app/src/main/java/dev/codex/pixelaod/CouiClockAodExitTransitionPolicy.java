package dev.codex.pixelaod;

/** Pure state-derived decision for the first render-driven callback leaving AOD. */
final class CouiClockAodExitTransitionPolicy {
    enum Decision {
        PRESENT_LOCKSCREEN,
        NORMAL
    }

    private CouiClockAodExitTransitionPolicy() {
    }

    static Decision decide(boolean renderDriven, boolean previousPresentationWasDozing,
            boolean interactive, CouiClockPluginPresentationMapper.Action currentAction,
            CouiClockPresentationModel.Scene lockscreenScene) {
        return renderDriven
                && previousPresentationWasDozing
                && interactive
                && currentAction == CouiClockPluginPresentationMapper.Action.HIDE
                && lockscreenScene != null
                ? Decision.PRESENT_LOCKSCREEN : Decision.NORMAL;
    }

    static boolean animationAllowed(boolean renderDriven, boolean rawUiStateAnimating) {
        return renderDriven && rawUiStateAnimating;
    }
}
