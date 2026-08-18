package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockVisualStylePolicyTest {
    @Test
    public void lockscreenFallsBackToReferenceClockColorAndImmersedIsWhite() {
        assertEquals(-1515784, CouiClockVisualStylePolicy.clockColor(
                CouiClockPresentationModel.Scene.LARGE, false,
                Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(0x00123456, CouiClockVisualStylePolicy.clockColor(
                CouiClockPresentationModel.Scene.LARGE, false,
                0x00123456, Integer.MIN_VALUE));
        assertEquals(0xffffffff, CouiClockVisualStylePolicy.clockColor(
                CouiClockPresentationModel.Scene.IMMERSED, false,
                0x00123456, Integer.MIN_VALUE));
    }

    @Test
    public void aodPrefersAodMonetThenLockscreenMonetThenFallback() {
        assertEquals(0x0000abcd, CouiClockVisualStylePolicy.clockColor(
                CouiClockPresentationModel.Scene.LARGE, true,
                0x00123456, 0x0000abcd));
        assertEquals(0x00123456, CouiClockVisualStylePolicy.clockColor(
                CouiClockPresentationModel.Scene.LARGE, true,
                0x00123456, Integer.MIN_VALUE));
        assertEquals(-1515784, CouiClockVisualStylePolicy.clockColor(
                CouiClockPresentationModel.Scene.LARGE, true,
                Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void notificationOverflowUsesTheSameResolvedAccentAsNotificationGlyphs() {
        assertEquals(0xffa1b2c3, CouiClockVisualStylePolicy.notificationOverflowColor(0xffa1b2c3));
        assertEquals(0xff123456, CouiClockVisualStylePolicy.notificationOverflowColor(0xff123456));
    }

    @Test
    public void contextualAodRowUsesTheExactClockAccentAtFullStrength() {
        assertEquals(0xffa1b2c3, CouiClockVisualStylePolicy.contextualAccentColor(
                CouiClockPresentationModel.Scene.SMALL, true,
                0xff123456, 0xffa1b2c3));
        assertEquals(1f, CouiClockVisualStylePolicy.contextualContentAlpha(true), 0f);
        assertEquals(0f, CouiClockVisualStylePolicy.contextualContentAlpha(false), 0f);
    }

    @Test
    public void informationShadowIsClearedOnlyForImmersedOrPartialMediaInputs() {
        assertFalse(CouiClockVisualStylePolicy.shouldApplyInformationShadow(
                CouiClockPresentationModel.Scene.IMMERSED, false, false, false));
        assertFalse(CouiClockVisualStylePolicy.shouldApplyInformationShadow(
                CouiClockPresentationModel.Scene.SMALL, true, true, true));
        assertTrue(CouiClockVisualStylePolicy.shouldApplyInformationShadow(
                CouiClockPresentationModel.Scene.LARGE, false, false, false));
        assertTrue(CouiClockVisualStylePolicy.shouldApplyInformationShadow(
                CouiClockPresentationModel.Scene.SMALL, true, true, false));
        assertEquals(1711276032, CouiClockVisualStylePolicy.INFORMATION_SHADOW_COLOR);
        assertEquals(1.5f, CouiClockVisualStylePolicy.INFORMATION_SHADOW_RADIUS_DP, 0f);
        assertEquals(0.5f, CouiClockVisualStylePolicy.INFORMATION_SHADOW_DY_DP, 0f);
    }
}
