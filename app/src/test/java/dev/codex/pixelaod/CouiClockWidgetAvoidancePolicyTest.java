package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CouiClockWidgetAvoidancePolicyTest {

    @Test
    public void matchesColorOsLockscreenWidgetsAndExcludesSecurityViews() {
        assertTrue(CouiClockWidgetAvoidancePolicy.isLockscreenWidgetCandidate(
                "com.oplus.keyguard.plugin.WidgetPlugin", ""));
        assertTrue(CouiClockWidgetAvoidancePolicy.isLockscreenWidgetCandidate(
                "android.widget.FrameLayout", "oplus_keyguard_widget_container"));
        assertTrue(CouiClockWidgetAvoidancePolicy.isLockscreenWidgetCandidate(
                "com.oplus.systemui.keyguard.clockstyle.KeyguardStyleWidgetView", ""));
        assertFalse(CouiClockWidgetAvoidancePolicy.isLockscreenWidgetCandidate(
                "com.oplus.keyguard.security.widget.PinSimpleLockInputWidget", ""));
        assertFalse(CouiClockWidgetAvoidancePolicy.isLockscreenWidgetCandidate(
                "com.oplus.systemui.keyguard.clock.big.widget.MyCustomizedFrameLayout", ""));
        assertFalse(CouiClockWidgetAvoidancePolicy.isLockscreenWidgetCandidate(
                "dev.codex.pixelaod.CouiClockHostView", "keyguard_widget"));
    }

    @Test
    public void shiftsClockAndDateUpOnlyWhenDateCollidesWithLockscreenWidget() {
        assertEquals(0f, CouiClockWidgetAvoidancePolicy.resolveVerticalShiftUpPx(
                2400f, 500f, 1400f, 1600f, 180f, 40f), 0.01f);
        assertEquals(160f, CouiClockWidgetAvoidancePolicy.resolveVerticalShiftUpPx(
                2400f, 500f, 1480f, 1360f, 180f, 40f), 0.01f);
        assertEquals(320f, CouiClockWidgetAvoidancePolicy.resolveVerticalShiftUpPx(
                2400f, 500f, 1700f, 1100f, 180f, 40f), 0.01f);
    }
}
