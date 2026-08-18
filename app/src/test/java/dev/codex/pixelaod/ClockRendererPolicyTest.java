package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClockRendererPolicyTest {
    @Test
    public void missingBlankAndUnknownValuesDefaultToCouiAfterCutover() {
        String[] configuredValues = {null, "", "   ", "unknown", "COUI_PORTX"};

        for (String configured : configuredValues) {
            ClockRendererPolicy policy = ClockRendererPolicy.parse(configured);
            assertEquals(ClockRendererPolicy.Mode.COUI_PORT, policy.mode());
            assertFalse(policy.useLegacyOwner());
            assertTrue(policy.useCouiOwner());
        }
    }

    @Test
    public void storedValuesSelectExactlyOneOwner() {
        ClockRendererPolicy legacy = ClockRendererPolicy.parse(
                ClockRendererPolicy.VALUE_LEGACY);
        assertEquals("legacy", ClockRendererPolicy.VALUE_LEGACY);
        assertEquals(ClockRendererPolicy.Mode.LEGACY, legacy.mode());
        assertTrue(legacy.useLegacyOwner());
        assertFalse(legacy.useCouiOwner());

        ClockRendererPolicy coui = ClockRendererPolicy.parse(
                ClockRendererPolicy.VALUE_COUI_PORT);
        assertEquals("coui_port", ClockRendererPolicy.VALUE_COUI_PORT);
        assertEquals(ClockRendererPolicy.Mode.COUI_PORT, coui.mode());
        assertFalse(coui.useLegacyOwner());
        assertTrue(coui.useCouiOwner());
    }

    @Test
    public void parserAcceptsTrimmedCaseInsensitiveStoredValues() {
        assertEquals(ClockRendererPolicy.Mode.LEGACY,
                ClockRendererPolicy.parse("  LeGaCy ").mode());
        assertEquals(ClockRendererPolicy.Mode.COUI_PORT,
                ClockRendererPolicy.parse("  COUI_PORT ").mode());
    }

    @Test
    public void schemaExposesCouiStartupDefaultAndExplicitLegacyRollback() {
        assertEquals("clock_renderer", PixelAodSettingsSchema.KEY_CLOCK_RENDERER);
        assertEquals("legacy", PixelAodSettingsSchema.CLOCK_RENDERER_LEGACY);
        assertEquals("coui_port", PixelAodSettingsSchema.CLOCK_RENDERER_COUI_PORT);
        assertEquals("coui_port", PixelAodSettingsSchema.stringDefault(
                PixelAodSettingsSchema.KEY_CLOCK_RENDERER, "wrong"));
        assertTrue(PixelAodSettingsSchema.requiresSystemUiRestart(
                PixelAodSettingsSchema.KEY_CLOCK_RENDERER));

        assertEquals(PixelAodSettingsSchema.KEY_CLOCK_RENDERER,
                PixelAodSettings.KEY_CLOCK_RENDERER);
        assertEquals(PixelAodSettingsSchema.CLOCK_RENDERER_LEGACY,
                PixelAodSettings.CLOCK_RENDERER_LEGACY);
        assertEquals(PixelAodSettingsSchema.CLOCK_RENDERER_COUI_PORT,
                PixelAodSettings.CLOCK_RENDERER_COUI_PORT);
    }
}
