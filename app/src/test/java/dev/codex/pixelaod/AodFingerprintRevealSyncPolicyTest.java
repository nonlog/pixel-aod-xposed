package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AodFingerprintRevealSyncPolicyTest {
    @Test
    public void systemGlyphSynchronizesOnlyInsidePixelAodPresentation() {
        assertTrue(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                true, true, false, false, true, true));
        assertFalse(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                false, true, false, false, true, true));
        assertFalse(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                true, false, false, false, true, true));
        assertFalse(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                true, true, true, false, true, true));
        assertFalse(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                true, true, false, true, true, true));
        assertFalse(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                true, true, false, false, false, true));
        assertFalse(AodFingerprintRevealSyncPolicy.shouldSynchronize(
                true, true, false, false, true, false));
    }

    @Test
    public void revealProgressUsesSlowestVisibleAodSurface() {
        assertEquals(0f, AodFingerprintRevealSyncPolicy.revealProgress(
                false, 1f, false, 0f), 0.0001f);
        assertEquals(0.4f, AodFingerprintRevealSyncPolicy.revealProgress(
                true, 0.4f, false, 0f), 0.0001f);
        assertEquals(0.25f, AodFingerprintRevealSyncPolicy.revealProgress(
                true, 1f, true, 0.75f), 0.0001f);
        assertEquals(0.2f, AodFingerprintRevealSyncPolicy.revealProgress(
                true, 0.2f, true, 0.4f), 0.0001f);
    }

    @Test
    public void nativeBrightnessIsScaledByAodReveal() {
        assertEquals(0.2f, AodFingerprintRevealSyncPolicy.synchronizedIconAlpha(
                0.8f, 0.25f), 0.0001f);
        assertEquals(0f, AodFingerprintRevealSyncPolicy.synchronizedIconAlpha(
                0.8f, -1f), 0.0001f);
        assertEquals(1f, AodFingerprintRevealSyncPolicy.synchronizedIconAlpha(
                2f, 2f), 0.0001f);
        assertTrue(AodFingerprintRevealSyncPolicy.isComplete(1f));
        assertFalse(AodFingerprintRevealSyncPolicy.isComplete(0.9f));
    }
}
