package dev.codex.pixelaod;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class PresentationSettingsConvergenceTest {
    @Test
    public void forceEnglishDateIsNoLongerAPresentationPreference() {
        assertNull(PixelAodSettingsSchema.spec("force_english_date"));
    }
}
