package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PresentationSettingsConvergenceTest {
    @Test
    public void forceEnglishDateIsNoLongerAPresentationPreference() {
        assertNull(PixelAodSettingsSchema.spec("force_english_date"));
    }
    @Test
    public void pixelNotificationPeekIsLiveAndDefaultsToCurrentTakeoverBehavior() {
        assertTrue(PixelAodSettingsSchema.booleanDefault(
                PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, false));
        assertFalse(PixelAodSettingsSchema.requiresSystemUiRestart(
                PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK));
    }

}
