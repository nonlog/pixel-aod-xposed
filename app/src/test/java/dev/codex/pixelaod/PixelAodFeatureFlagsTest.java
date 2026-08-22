package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PixelAodFeatureFlagsTest {
    @Test
    public void onlyExplicitLegacyValueSelectsRollbackUdfpsRenderer() {
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
    public void startupUdfpsRendererAndAccessorStayConsistent() {
        PixelAodFeatureFlags.UdfpsRenderer startup = PixelAodFeatureFlags.startupUdfpsRenderer();
        assertTrue(startup == PixelAodFeatureFlags.UdfpsRenderer.COUI_PORT
                || startup == PixelAodFeatureFlags.UdfpsRenderer.LEGACY);
        assertEquals(startup == PixelAodFeatureFlags.UdfpsRenderer.COUI_PORT,
                PixelAodFeatureFlags.useCouiUdfps());
        assertFalse(PixelAodFeatureFlags.parseUdfpsRenderer("legacy")
                == PixelAodFeatureFlags.UdfpsRenderer.COUI_PORT);
    }
}