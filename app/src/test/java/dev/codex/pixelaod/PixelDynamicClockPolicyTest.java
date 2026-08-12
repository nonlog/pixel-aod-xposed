package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PixelDynamicClockPolicyTest {
    @Test
    public void validVendorStateIsAuthoritativeEvenWhenFallbackDisagrees() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_LARGE,
                PixelDynamicClockPolicy.resolve(
                        ClockPluginSceneMachine.CLOCK_SIZE_LARGE, true, true, true));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                PixelDynamicClockPolicy.resolve(
                        ClockPluginSceneMachine.CLOCK_SIZE_SMALL, false, false, false));
    }

    @Test
    public void visibleCardStateIsUsedOnlyWhenVendorStateIsUnavailable() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                PixelDynamicClockPolicy.resolve(null, true, false, false));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_LARGE,
                PixelDynamicClockPolicy.resolve(null, false, true, true));
    }

    @Test
    public void rawActivityIsOnlyBootstrapFallbackWhenNoLayoutSignalExists() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                PixelDynamicClockPolicy.resolve(null, null, true, false));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                PixelDynamicClockPolicy.resolve(null, null, false, true));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_LARGE,
                PixelDynamicClockPolicy.resolve(null, null, false, false));
    }

    @Test
    public void invalidVendorValueDoesNotOverrideFallbackSignals() {
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_SMALL,
                PixelDynamicClockPolicy.resolve(99, true, false, false));
        assertEquals(ClockPluginSceneMachine.CLOCK_SIZE_LARGE,
                PixelDynamicClockPolicy.resolve(99, false, true, true));
    }
}
