package dev.codex.pixelaod;

/** Routes ClockPlugin renders while a screen-off-from-unlocked AOD first frame is pre-armed. */
final class CouiClockNonLockscreenAodPrearmPolicy {
    enum Decision {
        PASS,
        HOLD_INTERMEDIATE,
        CONSUME_AOD
    }

    private CouiClockNonLockscreenAodPrearmPolicy() {
    }

    static boolean shouldPrepare(boolean alreadyPrearmed) {
        return !alreadyPrearmed;
    }

    static Decision route(boolean prearmed, Integer uiState) {
        if (!prearmed) {
            return Decision.PASS;
        }
        if (uiState != null
                && (uiState == CouiClockPluginPresentationMapper.UI_STATE_AOD
                || uiState == CouiClockPluginPresentationMapper.UI_STATE_PANORAMIC_AOD)) {
            return Decision.CONSUME_AOD;
        }
        return Decision.HOLD_INTERMEDIATE;
    }
}
