package dev.codex.pixelaod;

/** Resolves the lockscreen clock size without confusing active notifications with visible cards. */
final class ClockPluginLockscreenSizePolicy {
    private ClockPluginLockscreenSizePolicy() {
    }

    static int resolve(Integer vendorClockSize, boolean moduleNotifications, boolean activeMedia) {
        return resolve(vendorClockSize, null, moduleNotifications, activeMedia);
    }

    static int resolve(Integer vendorClockSize, Boolean visibleLockscreenContent,
            boolean moduleNotifications, boolean activeMedia) {
        return PixelDynamicClockPolicy.resolve(vendorClockSize, visibleLockscreenContent,
                moduleNotifications, activeMedia);
    }
}
