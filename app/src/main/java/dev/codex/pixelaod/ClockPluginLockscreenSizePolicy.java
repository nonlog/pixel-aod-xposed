package dev.codex.pixelaod;

/** Resolves the lockscreen clock size without confusing active notifications with visible cards. */
final class ClockPluginLockscreenSizePolicy {
    private ClockPluginLockscreenSizePolicy() {
    }

    static int resolve(Integer vendorClockSize, boolean moduleNotifications, boolean activeMedia) {
        if (vendorClockSize != null) {
            return vendorClockSize;
        }
        return moduleNotifications || activeMedia
                ? ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                : ClockPluginSceneMachine.CLOCK_SIZE_LARGE;
    }
}
