package dev.codex.pixelaod;

/** Pure fallback policy for classifying the origin of a screen-off AOD entry. */
final class CouiClockAodEntryOriginPolicy {
    private CouiClockAodEntryOriginPolicy() {
    }

    static boolean isFromLockscreen(boolean keyguardLocked,
            boolean recentInteractiveLockscreen,
            boolean lockscreenSurfaceVisible,
            boolean interactiveAtCallback) {
        return isFromLockscreen(keyguardLocked, recentInteractiveLockscreen,
                lockscreenSurfaceVisible, interactiveAtCallback, false);
    }

    static boolean isFromLockscreen(boolean keyguardLocked,
            boolean recentInteractiveLockscreen,
            boolean lockscreenSurfaceVisible,
            boolean interactiveAtCallback,
            boolean couiLockscreenPresentation) {
        return keyguardLocked
                || recentInteractiveLockscreen
                || (lockscreenSurfaceVisible && interactiveAtCallback)
                || couiLockscreenPresentation;
    }

    /**
     * COUI 2.5's pre-Keyguard binder latch is authoritative when present. On OPlus the
     * WakefulnessLifecycle callback may run after KeyguardManager already reports locked, so an
     * unlocked desktop screen-off must not be reclassified as lockscreen-origin at that point.
     */
    static boolean resolveFromLockscreen(Boolean pendingSleepFromUnlocked,
            boolean keyguardLocked,
            boolean recentInteractiveLockscreen,
            boolean lockscreenSurfaceVisible,
            boolean interactiveAtCallback) {
        if (pendingSleepFromUnlocked != null) {
            return !pendingSleepFromUnlocked;
        }
        return isFromLockscreen(keyguardLocked, recentInteractiveLockscreen,
                lockscreenSurfaceVisible, interactiveAtCallback);
    }

    /**
     * The vendor ClockPlugin publishes an explicit keyguard UI state before framework keyguard
     * queries are reliable on this device. This is positive lockscreen evidence only while the
     * process is interactive; it must not classify a dozing AOD callback as a lockscreen entry.
     */
    static boolean isCouiLockscreenPresentation(boolean interactive, Integer uiState,
            boolean keyguardShowing) {
        return interactive && (keyguardShowing
                || (uiState != null
                && uiState == CouiClockPluginPresentationMapper.UI_STATE_KEYGUARD));
    }
}
