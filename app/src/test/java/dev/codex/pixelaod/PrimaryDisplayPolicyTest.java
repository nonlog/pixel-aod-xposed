package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PrimaryDisplayPolicyTest {
    @Test
    public void onlyDefaultDisplayIdIsPrimary() {
        assertTrue(PrimaryDisplayPolicy.isPrimaryDisplayId(0));
        assertFalse(PrimaryDisplayPolicy.isPrimaryDisplayId(1));
        assertFalse(PrimaryDisplayPolicy.isPrimaryDisplayId(2));
        assertFalse(PrimaryDisplayPolicy.isPrimaryDisplayId(PrimaryDisplayPolicy.UNKNOWN_DISPLAY_ID));
    }

    @Test
    public void explicitViewDisplayWinsOverDefaultFallback() {
        assertEquals(2, PrimaryDisplayPolicy.resolveDisplayId(
                2, PrimaryDisplayPolicy.UNKNOWN_DISPLAY_ID, 0));
    }

    @Test
    public void explicitContextDisplayWinsOverDefaultFallback() {
        assertEquals(3, PrimaryDisplayPolicy.resolveDisplayId(
                PrimaryDisplayPolicy.UNKNOWN_DISPLAY_ID, 3, 0));
    }

    @Test
    public void unassociatedGlobalContextMayUseDefaultFallback() {
        assertEquals(0, PrimaryDisplayPolicy.resolveDisplayId(
                PrimaryDisplayPolicy.UNKNOWN_DISPLAY_ID,
                PrimaryDisplayPolicy.UNKNOWN_DISPLAY_ID, 0));
    }
}
