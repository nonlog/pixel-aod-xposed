package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NotificationPresentationGateTest {
    @Test
    public void aodRefreshRequiresAnAttachedShownDrawableSurface() {
        assertTrue(NotificationPresentationGate.shouldRefreshAod(
                true, true, true, 1f, true));
        assertFalse(NotificationPresentationGate.shouldRefreshAod(
                true, false, false, 1f, true));
        assertFalse(NotificationPresentationGate.shouldRefreshAod(
                true, true, true, 1f, false));
        assertFalse(NotificationPresentationGate.shouldRefreshAod(
                false, true, true, 1f, true));
        assertFalse(NotificationPresentationGate.shouldRefreshAod(
                true, true, true, 0f, true));
    }

    @Test
    public void lockscreenRefreshRequiresAnActuallyVisibleSurface() {
        assertTrue(NotificationPresentationGate.shouldRefreshLockscreen(
                true, true, true, 1f));
        assertFalse(NotificationPresentationGate.shouldRefreshLockscreen(
                true, false, false, 1f));
        assertFalse(NotificationPresentationGate.shouldRefreshLockscreen(
                true, true, true, 0.01f));
    }
}
