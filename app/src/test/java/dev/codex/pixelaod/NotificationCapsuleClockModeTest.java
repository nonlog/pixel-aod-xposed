package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Regression contract for lockscreen notifications collapsed into the OOS bottom capsule. */
public final class NotificationCapsuleClockModeTest {
    @Test
    public void collapsedCapsuleTrustsVendorLargeWhileNotificationRemainsActive() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_LARGE,
                resolve(ClockPluginSceneMachine.CLOCK_SIZE_LARGE, true, false));
    }

    @Test
    public void visibleCardKeepsVendorSmallWhileNotificationRemainsActive() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                resolve(ClockPluginSceneMachine.CLOCK_SIZE_SMALL, true, false));
    }

    @Test
    public void missingVendorStateFallsBackToNotificationOrMediaPresence() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL, resolve(null, true, false));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL, resolve(null, false, true));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_LARGE, resolve(null, false, false));
    }

    private static int resolve(Integer vendorClockSize, boolean moduleNotifications,
            boolean activeMedia) {
        return ClockPluginLockscreenSizePolicy.resolve(
                vendorClockSize, moduleNotifications, activeMedia);
    }
}
