package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NonLockscreenAodVisibilityGateTest {
    @Test
    public void blocksDesktopAndAppEntryWindowsUntilTheNativeDisplayIsInDoze() {
        assertFalse(NonLockscreenAodVisibilityGate.shouldDraw(
                false, false, true, false, false, false));
        assertFalse(NonLockscreenAodVisibilityGate.shouldDraw(
                false, false, false, false, true, true));
        assertTrue(NonLockscreenAodVisibilityGate.shouldDraw(
                false, true, true, false, false, false));
    }

    @Test
    public void preservesTheExistingLockscreenToAodEntryWindow() {
        assertTrue(NonLockscreenAodVisibilityGate.shouldDraw(
                true, false, true, false, false, false));
        assertTrue(NonLockscreenAodVisibilityGate.shouldDraw(
                true, false, false, false, true, true));
    }

    @Test
    public void permitsAnExplicitBriefAodTrigger() {
        assertTrue(NonLockscreenAodVisibilityGate.shouldDraw(
                false, false, false, true, false, false));
    }
}
