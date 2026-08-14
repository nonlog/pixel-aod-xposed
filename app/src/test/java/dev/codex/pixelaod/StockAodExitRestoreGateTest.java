package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StockAodExitRestoreGateTest {
    @Test
    public void defersRestoreWhenInteractiveArrivesBeforeTheCurrentAodHostRetires() {
        assertTrue(StockAodExitRestoreGate.shouldDeferRestore(
                true, true, true, true));
    }

    @Test
    public void releasesAfterTheAodHostIsDetachedOrRemoved() {
        assertFalse(StockAodExitRestoreGate.shouldDeferRestore(
                true, true, false, true));
    }

    @Test
    public void neverLetsAnOldAodHostBlockANewerTrace() {
        assertFalse(StockAodExitRestoreGate.shouldDeferRestore(
                true, true, true, false));
    }

    @Test
    public void doesNotChangeNormalNonInteractiveAodSuppressionOwnership() {
        assertFalse(StockAodExitRestoreGate.shouldDeferRestore(
                false, true, true, true));
        assertFalse(StockAodExitRestoreGate.shouldDeferRestore(
                true, false, true, true));
    }
}
