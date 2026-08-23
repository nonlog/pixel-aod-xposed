package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NonLockscreenAodTransitionPolicyTest {
    @Test
    public void animatedIsDefaultAndUnknownFallback() {
        assertEquals(NonLockscreenAodTransitionPolicy.Mode.ANIMATED,
                NonLockscreenAodTransitionPolicy.fromSetting(null));
        assertEquals(NonLockscreenAodTransitionPolicy.Mode.ANIMATED,
                NonLockscreenAodTransitionPolicy.fromSetting("future_value"));
        assertEquals(PixelAodSettingsSchema.NON_LOCKSCREEN_AOD_TRANSITION_ANIMATED,
                PixelAodSettingsSchema.stringDefault(
                        PixelAodSettingsSchema.KEY_NON_LOCKSCREEN_AOD_TRANSITION, "wrong"));
    }

    @Test
    public void directFinalSettingSelectsDirectFinalMode() {
        NonLockscreenAodTransitionPolicy.Mode mode =
                NonLockscreenAodTransitionPolicy.fromSetting(
                        PixelAodSettingsSchema.NON_LOCKSCREEN_AOD_TRANSITION_DIRECT_FINAL);

        assertEquals(NonLockscreenAodTransitionPolicy.Mode.DIRECT_FINAL, mode);
        assertTrue(NonLockscreenAodTransitionPolicy.isDirectFinal(mode));
    }

    @Test
    public void transitionPreferenceIsLiveAndDoesNotRequireSystemUiRestart() {
        assertFalse(PixelAodSettingsSchema.requiresSystemUiRestart(
                PixelAodSettingsSchema.KEY_NON_LOCKSCREEN_AOD_TRANSITION));
    }
}