package dev.codex.pixelaod;

/** Pure ownership policy for the AOD-to-lockscreen handoff window. */
final class CouiClockAodExitContinuityPolicy {
    enum Decision {
        KEEP_REPLACEMENT,
        HIDE_REPLACEMENT
    }

    private CouiClockAodExitContinuityPolicy() {
    }

    static Decision decide(boolean handoffPending, boolean previousPresentationWasDozing,
            CouiClockPluginPresentationMapper.Action action) {
        return handoffPending
                && previousPresentationWasDozing
                && action == CouiClockPluginPresentationMapper.Action.HIDE
                ? Decision.KEEP_REPLACEMENT : Decision.HIDE_REPLACEMENT;
    }

    static boolean completesHandoff(boolean handoffPending, boolean nextPresentationIsDozing,
            CouiClockPluginPresentationMapper.Action action) {
        return handoffPending
                && !nextPresentationIsDozing
                && action == CouiClockPluginPresentationMapper.Action.PRESENT;
    }
}
