package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class PixelAodUdfpsSettingsDefaultsTest {
    @Test
    public void udfpsVisualEffectsDefaultEnabled() {
        assertTrue(PixelAodSettingsSchema.booleanDefault(
                PixelAodSettingsSchema.KEY_UDFPS_HDR_PRESS_EFFECT, false));
        assertTrue(PixelAodSettingsSchema.booleanDefault(
                PixelAodSettingsSchema.KEY_UDFPS_SUCCESS_RIPPLE, false));
        assertTrue(PixelAodSettingsSchema.booleanDefault(
                PixelAodSettingsSchema.KEY_UDFPS_AOD_EXIT_ANIMATION, false));
    }
}
