package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PixelAodFeatureFlagsTest {
    @Test
    public void onlyExplicitLegacyValueSelectsRollbackRenderer() {
        assertEquals(PixelAodFeatureFlags.UdfpsRenderer.LEGACY,
                PixelAodFeatureFlags.parseUdfpsRenderer("legacy"));
        assertEquals(PixelAodFeatureFlags.UdfpsRenderer.LEGACY,
                PixelAodFeatureFlags.parseUdfpsRenderer("LEGACY"));
        assertEquals(PixelAodFeatureFlags.UdfpsRenderer.COUI_PORT,
                PixelAodFeatureFlags.parseUdfpsRenderer("coui_port"));
        assertEquals(PixelAodFeatureFlags.UdfpsRenderer.COUI_PORT,
                PixelAodFeatureFlags.parseUdfpsRenderer("unknown"));
        assertEquals(PixelAodFeatureFlags.UdfpsRenderer.COUI_PORT,
                PixelAodFeatureFlags.parseUdfpsRenderer(null));
    }

    @Test
    public void clockStartupParserDefaultsToCouiAndKeepsOwnersExclusive() {
        String[] configuredValues = {null, "", " ", "unknown", "legacy", "coui_port"};
        for (String configured : configuredValues) {
            ClockRendererPolicy policy = PixelAodFeatureFlags.parseClockRenderer(configured);
            assertEquals(policy.useCouiOwner(), !policy.useLegacyOwner());
            assertTrue(policy.useLegacyOwner() ^ policy.useCouiOwner());
        }

        assertEquals(ClockRendererPolicy.Mode.COUI_PORT,
                PixelAodFeatureFlags.parseClockRenderer(null).mode());
        assertEquals(ClockRendererPolicy.Mode.LEGACY,
                PixelAodFeatureFlags.parseClockRenderer("legacy").mode());
        assertEquals(ClockRendererPolicy.Mode.COUI_PORT,
                PixelAodFeatureFlags.parseClockRenderer("coui_port").mode());
    }

    @Test
    public void startupClockRendererExposesOneImmutableOwnerSelection() {
        ClockRendererPolicy startup = PixelAodFeatureFlags.startupClockRenderer();
        assertEquals(startup.useCouiOwner(), !startup.useLegacyOwner());
        assertTrue(PixelAodFeatureFlags.useLegacyClockRenderer()
                ^ PixelAodFeatureFlags.useCouiClockRenderer());
        assertFalse(PixelAodFeatureFlags.useLegacyClockRenderer()
                && PixelAodFeatureFlags.useCouiClockRenderer());
    }
}
