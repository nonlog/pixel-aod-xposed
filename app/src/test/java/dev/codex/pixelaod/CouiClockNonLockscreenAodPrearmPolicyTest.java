package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockNonLockscreenAodPrearmPolicyTest {
    @Test
    public void binderPrearmMakesWakefulnessFallbackIdempotent() {
        assertTrue(CouiClockNonLockscreenAodPrearmPolicy.shouldPrepare(false));
        assertFalse(CouiClockNonLockscreenAodPrearmPolicy.shouldPrepare(true));
    }

    @Test
    public void prearmedUnlockedRenderCannotMoveTheFirstFrame() {
        assertEquals(CouiClockNonLockscreenAodPrearmPolicy.Decision.HOLD_INTERMEDIATE,
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        true, CouiClockPluginPresentationMapper.UI_STATE_UNLOCKED));
    }

    @Test
    public void prearmedKeyguardRenderCannotMoveTheFirstFrame() {
        assertEquals(CouiClockNonLockscreenAodPrearmPolicy.Decision.HOLD_INTERMEDIATE,
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        true, CouiClockPluginPresentationMapper.UI_STATE_KEYGUARD));
    }

    @Test
    public void realAodRenderConsumesThePrearm() {
        assertEquals(CouiClockNonLockscreenAodPrearmPolicy.Decision.CONSUME_AOD,
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        true, CouiClockPluginPresentationMapper.UI_STATE_AOD));
        assertEquals(CouiClockNonLockscreenAodPrearmPolicy.Decision.CONSUME_AOD,
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        true, CouiClockPluginPresentationMapper.UI_STATE_PANORAMIC_AOD));
    }

    @Test
    public void normalClockPluginRoutingIsUnchangedWithoutPrearm() {
        assertEquals(CouiClockNonLockscreenAodPrearmPolicy.Decision.PASS,
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        false, CouiClockPluginPresentationMapper.UI_STATE_UNLOCKED));
        assertEquals(CouiClockNonLockscreenAodPrearmPolicy.Decision.PASS,
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        false, CouiClockPluginPresentationMapper.UI_STATE_AOD));
    }
}
