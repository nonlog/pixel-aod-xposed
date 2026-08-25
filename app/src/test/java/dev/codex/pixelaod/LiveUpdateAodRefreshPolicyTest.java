package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LiveUpdateAodRefreshPolicyTest {
    @Test
    public void ramlessRefreshRequiresAllStockCapabilities() {
        assertTrue(LiveUpdateAodRefreshPolicy.canUseRamless(true, true, true));
        assertFalse(LiveUpdateAodRefreshPolicy.canUseRamless(false, true, true));
        assertFalse(LiveUpdateAodRefreshPolicy.canUseRamless(true, false, true));
        assertFalse(LiveUpdateAodRefreshPolicy.canUseRamless(true, true, false));
        assertFalse(LiveUpdateAodRefreshPolicy.canUseRamless(null, true, true));
    }

    @Test
    public void secondTicksNeverUseMinuteSemanticKick() {
        assertFalse(LiveUpdateAodRefreshPolicy.canUseMinuteSemanticKickForSecondTick());
    }
}