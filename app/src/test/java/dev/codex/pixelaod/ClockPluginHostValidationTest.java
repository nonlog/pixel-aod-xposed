package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClockPluginHostValidationTest {
    @Test
    public void rejectsTransparentAncestor() {
        assertFalse(ClockPluginHostValidation.isDrawableNode(true, 0, 0f));
    }

    @Test
    public void acceptsAttachedVisibleOpaqueAncestor() {
        assertTrue(ClockPluginHostValidation.isDrawableNode(true, 0, 1f));
    }

    @Test
    public void rejectsDetachedOrInvisibleAncestor() {
        assertFalse(ClockPluginHostValidation.isDrawableNode(false, 0, 1f));
        assertFalse(ClockPluginHostValidation.isDrawableNode(true, 4, 1f));
    }
}
