package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SelectiveBiometricPulseAdapterTest {
    @Test
    public void nativeTouchDownStartsAuthUiOnlyPresentation() {
        SelectiveBiometricPulseAdapter.Snapshot snapshot =
                new SelectiveBiometricPulseAdapter().observeOverlayReason(8, "test");

        assertEquals(SelectiveBiometricPulseAdapter.Presentation.AUTH_UI_ONLY,
                snapshot.presentation);
        assertTrue(snapshot.authUiActive);
        assertTrue(snapshot.blocksPixelContent());
        assertEquals("native-auth-ui-only-pulse", snapshot.blockReason());
    }

    @Test
    public void bothNativeTouchUpReasonsEndAuthPresentation() {
        SelectiveBiometricPulseAdapter adapter = new SelectiveBiometricPulseAdapter();
        adapter.observeOverlayReason(8, "down");
        SelectiveBiometricPulseAdapter.Snapshot up = adapter.observeOverlayReason(9, "up");
        assertFalse(up.blocksPixelContent());
        assertEquals(SelectiveBiometricPulseAdapter.Presentation.IDLE, up.presentation);

        adapter.observeOverlayReason(8, "down-2");
        SelectiveBiometricPulseAdapter.Snapshot upAlt = adapter.observeOverlayReason(10, "up-2");
        assertFalse(upAlt.blocksPixelContent());
        assertEquals(SelectiveBiometricPulseAdapter.Presentation.IDLE, upAlt.presentation);
    }

    @Test
    public void overlayHideTerminatesAuthPresentation() {
        SelectiveBiometricPulseAdapter adapter = new SelectiveBiometricPulseAdapter();
        adapter.observeOverlayReason(8, "down");

        SelectiveBiometricPulseAdapter.Snapshot hidden =
                adapter.observeOverlayHidden("hideUdfpsOverlay");

        assertFalse(hidden.authUiActive);
        assertFalse(hidden.blocksPixelContent());
        assertEquals(-1, hidden.lastReason);
    }

    @Test
    public void normalIconShowReasonClearsAnyStaleTouchState() {
        SelectiveBiometricPulseAdapter adapter = new SelectiveBiometricPulseAdapter();
        adapter.observeOverlayReason(8, "down");

        SelectiveBiometricPulseAdapter.Snapshot iconShow =
                adapter.observeOverlayReason(4, "new-session-icon");

        assertFalse(iconShow.blocksPixelContent());
        assertEquals(4, iconShow.lastReason);
    }

    @Test
    public void unsupportedReasonDoesNotInventOrClearPresentation() {
        SelectiveBiometricPulseAdapter adapter = new SelectiveBiometricPulseAdapter();
        SelectiveBiometricPulseAdapter.Snapshot idle =
                adapter.observeOverlayReason(7, "unsupported-idle");
        assertFalse(idle.blocksPixelContent());

        adapter.observeOverlayReason(8, "down");
        SelectiveBiometricPulseAdapter.Snapshot stillActive =
                adapter.observeOverlayReason(99, "unsupported-active");
        assertTrue(stillActive.blocksPixelContent());
    }

    @Test
    public void repeatedSameTouchReasonDoesNotAdvanceGeneration() {
        SelectiveBiometricPulseAdapter adapter = new SelectiveBiometricPulseAdapter();
        long first = adapter.observeOverlayReason(8, "first").generation;
        long second = adapter.observeOverlayReason(8, "second").generation;

        assertEquals(first, second);
    }
}
