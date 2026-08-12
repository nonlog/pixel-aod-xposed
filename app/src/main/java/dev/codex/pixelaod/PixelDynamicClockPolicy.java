package dev.codex.pixelaod;

/**
 * Pixel-style Dynamic clock policy for the OPlus host.
 *
 * <p>When OPlus publishes a valid ClockPlugin size it is the authoritative layout state because
 * that state is produced by the same vendor notification/card pipeline that owns the visible
 * lockscreen geometry. Module card detection is only a fallback while the vendor state is absent;
 * raw notification/media activity is the final bootstrap fallback when neither layout signal is
 * available.</p>
 */
final class PixelDynamicClockPolicy {
    private PixelDynamicClockPolicy() {
    }

    static int resolve(Integer vendorClockSize, Boolean visibleLockscreenContent,
            boolean moduleNotifications, boolean activeMedia) {
        if (vendorClockSize != null
                && (vendorClockSize == ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                || vendorClockSize == ClockPluginSceneMachine.CLOCK_SIZE_LARGE)) {
            return vendorClockSize;
        }
        if (visibleLockscreenContent != null) {
            return visibleLockscreenContent
                    ? ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                    : ClockPluginSceneMachine.CLOCK_SIZE_LARGE;
        }
        return moduleNotifications || activeMedia
                ? ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                : ClockPluginSceneMachine.CLOCK_SIZE_LARGE;
    }
}
