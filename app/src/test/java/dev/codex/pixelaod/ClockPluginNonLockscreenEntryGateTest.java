package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ClockPluginNonLockscreenEntryGateTest {
    @Test
    public void parksStaleLockscreenSceneDuringNonLockscreenScreenOff() {
        ClockPluginNonLockscreenEntryGate gate = new ClockPluginNonLockscreenEntryGate();
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_000L));
        assertEquals(true, gate.shouldParkPersistentHost(
                "desktop-to-aod", false, false, false));
    }

    @Test
    public void keepsParkingOwnedTraceAfterPrePresentationDeadlineExpires() {
        ClockPluginNonLockscreenEntryGate gate = new ClockPluginNonLockscreenEntryGate();
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_000L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.CANCEL,
                gate.evaluate("desktop-to-aod", false, false, 1_120L));
        assertEquals(true, gate.shouldParkPersistentHost(
                "desktop-to-aod", false, false, false));
    }

    @Test
    public void doesNotParkUnownedInteractiveOrNativeAodPresentation() {
        ClockPluginNonLockscreenEntryGate gate = new ClockPluginNonLockscreenEntryGate();
        assertEquals(false, gate.shouldParkPersistentHost(
                "desktop-to-aod", false, false, false));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_000L));
        assertEquals(false, gate.shouldParkPersistentHost(
                "different-trace", false, false, false));
        assertEquals(false, gate.shouldParkPersistentHost(
                "desktop-to-aod", false, true, false));
        assertEquals(false, gate.shouldParkPersistentHost(
                "desktop-to-aod", false, false, true));
        assertEquals(false, gate.shouldParkPersistentHost(
                "desktop-to-aod", true, false, false));
    }

    @Test
    public void abandonsPrePresentationWhenNativeAodDidNotArriveBeforeTheDeadline() {
        ClockPluginNonLockscreenEntryGate gate = new ClockPluginNonLockscreenEntryGate();

        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_000L));
        assertEquals(ClockPluginNonLockscreenEntryGate.RETRY_INTERVAL_MILLIS,
                gate.retryDelayMillis(1_000L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_119L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.CANCEL,
                gate.evaluate("desktop-to-aod", false, false, 1_120L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.CANCEL,
                gate.evaluate("desktop-to-aod", false, true, 1_136L));
    }

    @Test
    public void presentsImmediatelyOnceTheNativeDisplayReportsAod() {
        ClockPluginNonLockscreenEntryGate gate = new ClockPluginNonLockscreenEntryGate();

        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_000L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.PRESENT,
                gate.evaluate("desktop-to-aod", false, true, 1_016L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.ALREADY_PRESENTED,
                gate.evaluate("desktop-to-aod", false, true, 1_032L));
    }

    @Test
    public void cancelsADeferredEntryIfTheUserWakesBeforeDoze() {
        ClockPluginNonLockscreenEntryGate gate = new ClockPluginNonLockscreenEntryGate();

        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.DEFER,
                gate.evaluate("desktop-to-aod", false, false, 1_000L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.CANCEL,
                gate.evaluate("desktop-to-aod", true, false, 1_016L));
        assertEquals(ClockPluginNonLockscreenEntryGate.Decision.CANCEL,
                gate.evaluate("desktop-to-aod", false, true, 1_032L));
    }
}
