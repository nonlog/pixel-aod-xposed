package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockGeometryPolicyTest {
    private static final float EPSILON = 0f;

    @Test
    public void lockscreenLargeTargetMatchesDesignLiterals() {
        CouiClockGeometryPolicy.SurfaceTarget target = target(
                CouiClockGeometryPolicy.Surface.LS_LARGE);

        assertEquals(.47f, target.baseWidthRatio, EPSILON);
        assertEquals(.215f, target.topRatio, EPSILON);
        assertEquals(-10f, target.topDp, EPSILON);
        assertEquals(1.0f, target.scale, EPSILON);
        assertEquals(450f, target.weight, EPSILON);
        assertEquals(144f, target.opsz, EPSILON);
        assertEquals(-.07f, target.trackingFactor, EPSILON);
        assertFalse(target.burnInEnabled);
        assertSharedVariation(target);
    }

    @Test
    public void aodLargeTargetMatchesDesignLiterals() {
        CouiClockGeometryPolicy.SurfaceTarget target = target(
                CouiClockGeometryPolicy.Surface.AOD_LARGE);

        assertEquals(.47f, target.baseWidthRatio, EPSILON);
        assertEquals(.215f, target.topRatio, EPSILON);
        assertEquals(-24f, target.topDp, EPSILON);
        assertEquals(.9f, target.scale, EPSILON);
        assertEquals(100f, target.weight, EPSILON);
        assertEquals(144f, target.opsz, EPSILON);
        assertEquals(-.06f, target.trackingFactor, EPSILON);
        assertTrue(target.burnInEnabled);
        assertSharedVariation(target);
    }

    @Test
    public void lockscreenSmallTargetMatchesDesignLiterals() {
        CouiClockGeometryPolicy.SurfaceTarget target = target(
                CouiClockGeometryPolicy.Surface.LS_SMALL);

        assertEquals(.36170214f, target.scale, EPSILON);
        assertEquals(.25f, target.centerRatio, EPSILON);
        assertEquals(8f, target.centerDp, EPSILON);
        assertEquals(.105f, target.topRatio, EPSILON);
        assertEquals(25f, target.topDp, EPSILON);
        assertEquals(500f, target.weight, EPSILON);
        assertEquals(96f, target.opsz, EPSILON);
        assertEquals(-.09f, target.trackingFactor, EPSILON);
        assertFalse(target.burnInEnabled);
        assertSharedVariation(target);
    }

    @Test
    public void aodSmallTargetMatchesDesignLiterals() {
        CouiClockGeometryPolicy.SurfaceTarget target = target(
                CouiClockGeometryPolicy.Surface.AOD_SMALL);

        assertEquals(.36170214f, target.scale, EPSILON);
        assertEquals(.25f, target.centerRatio, EPSILON);
        assertEquals(10f, target.centerDp, EPSILON);
        assertEquals(.105f, target.topRatio, EPSILON);
        assertEquals(25f, target.topDp, EPSILON);
        assertEquals(180f, target.weight, EPSILON);
        assertEquals(96f, target.opsz, EPSILON);
        assertEquals(-.09f, target.trackingFactor, EPSILON);
        assertTrue(target.burnInEnabled);
        assertSharedVariation(target);
    }

    @Test
    public void lockscreenImmersedTargetMatchesDesignLiterals() {
        CouiClockGeometryPolicy.SurfaceTarget target = target(
                CouiClockGeometryPolicy.Surface.LS_IMMERSED);

        assertEquals(.32978722f, target.scale, EPSILON);
        assertEquals(.25f, target.centerRatio, EPSILON);
        assertEquals(8f, target.centerDp, EPSILON);
        assertEquals(.072f, target.topRatio, EPSILON);
        assertEquals(30f, target.topDp, EPSILON);
        assertEquals(500f, target.weight, EPSILON);
        assertEquals(96f, target.opsz, EPSILON);
        assertEquals(-.09f, target.trackingFactor, EPSILON);
        assertEquals(.155f, target.textRatio, EPSILON);
        assertEquals(.09f, target.infoYRatio, EPSILON);
        assertFalse(target.burnInEnabled);
        assertSharedVariation(target);
    }

    @Test
    public void informationContentAndBurnInConstantsMatchDesignLiterals() {
        assertEquals(.75f, CouiClockGeometryPolicy.INFO_CENTER_RATIO, EPSILON);
        assertEquals(-36f, CouiClockGeometryPolicy.LOCKSCREEN_INFO_X_DP, EPSILON);
        assertEquals(-34f, CouiClockGeometryPolicy.AOD_INFO_X_DP, EPSILON);
        assertEquals(.118f, CouiClockGeometryPolicy.INFO_Y_RATIO, EPSILON);
        assertEquals(33f, CouiClockGeometryPolicy.INFO_Y_OFFSET_DP, EPSILON);
        assertEquals(3f, CouiClockGeometryPolicy.DATE_WEATHER_GAP_DP, EPSILON);
        assertEquals(.255f, CouiClockGeometryPolicy.PARTIAL_CONTENT_TOP_RATIO, EPSILON);
        assertEquals(32f, CouiClockGeometryPolicy.PARTIAL_CONTENT_X_DP, EPSILON);
        assertEquals(28f, CouiClockGeometryPolicy.MEDIA_TO_NOTIFICATION_GAP_DP, EPSILON);
        assertEquals(18f, CouiClockGeometryPolicy.NOTIFICATION_ICON_SIZE_DP, EPSILON);
        assertEquals(15f, CouiClockGeometryPolicy.NOTIFICATION_ICON_GAP_DP, EPSILON);
        assertEquals(7, CouiClockGeometryPolicy.MAX_NOTIFICATION_ICONS);
        assertEquals(83, CouiClockGeometryPolicy.BURN_IN_X_PERIOD_MINUTES);
        assertEquals(521, CouiClockGeometryPolicy.BURN_IN_Y_PERIOD_MINUTES);
        assertEquals(.75f, CouiClockGeometryPolicy.BATTERY_BURN_IN_X_SCALE, EPSILON);
        assertEquals(.5f, CouiClockGeometryPolicy.BATTERY_BURN_IN_Y_SCALE, EPSILON);
        assertEquals(64f, CouiClockGeometryPolicy.BATTERY_BOTTOM_MARGIN_DP, EPSILON);
    }

    private static CouiClockGeometryPolicy.SurfaceTarget target(
            CouiClockGeometryPolicy.Surface surface) {
        return CouiClockGeometryPolicy.target(surface);
    }

    private static void assertSharedVariation(CouiClockGeometryPolicy.SurfaceTarget target) {
        assertEquals(100f, target.wdth, EPSILON);
        assertEquals(100f, target.rond, EPSILON);
        assertEquals(0f, target.grad, EPSILON);
        assertEquals(0f, target.slnt, EPSILON);
    }
}
