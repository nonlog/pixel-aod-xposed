package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiUdfpsStateMachineTest {
    @Test
    public void followsCouiSurfaceAndPressLifecycle() {
        CouiUdfpsStateMachine machine = new CouiUdfpsStateMachine();

        assertEquals(CouiUdfpsStateMachine.Surface.HIDDEN, machine.snapshot().surface);
        assertEquals(CouiUdfpsStateMachine.Surface.AOD,
                machine.dispatch(CouiUdfpsStateMachine.Event.SHOW_AOD).surface);
        assertTrue(machine.dispatch(CouiUdfpsStateMachine.Event.TOUCH_DOWN).pressActive);
        assertFalse(machine.dispatch(CouiUdfpsStateMachine.Event.TOUCH_UP).pressActive);
        assertTrue(machine.dispatch(CouiUdfpsStateMachine.Event.SUCCESS).successActive);
        assertFalse(machine.dispatch(CouiUdfpsStateMachine.Event.FAILURE).successActive);
    }

    @Test
    public void nativeTimeoutStopsReplacementEffectsUntilNewShow() {
        CouiUdfpsStateMachine machine = new CouiUdfpsStateMachine();
        machine.dispatch(CouiUdfpsStateMachine.Event.SHOW_AOD);
        machine.dispatch(CouiUdfpsStateMachine.Event.TOUCH_DOWN);

        CouiUdfpsStateMachine.Snapshot timeout = machine.dispatch(
                CouiUdfpsStateMachine.Event.NATIVE_TIMEOUT);
        assertTrue(timeout.nativeTimeoutHidden);
        assertFalse(timeout.pressActive);
        assertFalse(timeout.successActive);
        assertFalse(machine.dispatch(CouiUdfpsStateMachine.Event.SUCCESS).successActive);

        CouiUdfpsStateMachine.Snapshot restored = machine.dispatch(
                CouiUdfpsStateMachine.Event.SHOW_AOD);
        assertFalse(restored.nativeTimeoutHidden);
        assertTrue(machine.dispatch(CouiUdfpsStateMachine.Event.TOUCH_DOWN).pressActive);
    }

    @Test
    public void resetClearsAllVisualState() {
        CouiUdfpsStateMachine machine = new CouiUdfpsStateMachine();
        machine.dispatch(CouiUdfpsStateMachine.Event.SHOW_LOCKSCREEN);
        machine.dispatch(CouiUdfpsStateMachine.Event.TOUCH_DOWN);
        machine.dispatch(CouiUdfpsStateMachine.Event.SUCCESS);

        CouiUdfpsStateMachine.Snapshot reset = machine.dispatch(
                CouiUdfpsStateMachine.Event.RESET);
        assertEquals(CouiUdfpsStateMachine.Surface.HIDDEN, reset.surface);
        assertFalse(reset.pressActive);
        assertFalse(reset.successActive);
        assertFalse(reset.nativeTimeoutHidden);
    }

    @Test
    public void preservesReferenceAnimationTimingsAndExitClamp() {
        assertEquals(420L, CouiUdfpsStateMachine.stateTransitionDurationMillis());
        assertEquals(180L, CouiUdfpsStateMachine.pressExpandDurationMillis());
        assertEquals(160L, CouiUdfpsStateMachine.pressRetractDurationMillis());
        assertEquals(500L, CouiUdfpsStateMachine.successDurationMillis());
        assertEquals(100L, CouiUdfpsStateMachine.clampAodExitDurationMillis(1L));
        assertEquals(1_234L, CouiUdfpsStateMachine.clampAodExitDurationMillis(1_234L));
        assertEquals(2_000L, CouiUdfpsStateMachine.clampAodExitDurationMillis(9_999L));
    }

    @Test
    public void liveRefreshReconcilesAodAndTouchWithoutCachedEvents() {
        CouiUdfpsStateMachine machine = new CouiUdfpsStateMachine();

        CouiUdfpsStateMachine.Snapshot shown = machine.synchronizeLive(
                true, true, false);
        assertEquals(CouiUdfpsStateMachine.Surface.AOD, shown.surface);
        assertTrue(shown.pressActive);

        CouiUdfpsStateMachine.Snapshot released = machine.synchronizeLive(
                true, false, false);
        assertEquals(CouiUdfpsStateMachine.Surface.AOD, released.surface);
        assertFalse(released.pressActive);

        machine.dispatch(CouiUdfpsStateMachine.Event.HIDE);
        CouiUdfpsStateMachine.Snapshot hidden = machine.synchronizeLive(
                true, false, false);
        assertEquals(CouiUdfpsStateMachine.Surface.HIDDEN, hidden.surface);
    }

    @Test
    public void visibilityArgumentsSupportBooleanAndAndroidVisibilityIntegers() {
        assertEquals(Boolean.TRUE, CouiUdfpsStateMachine.visibilityArgument(
                new Object[] { true }));
        assertEquals(Boolean.FALSE, CouiUdfpsStateMachine.visibilityArgument(
                new Object[] { false }));
        assertEquals(Boolean.TRUE, CouiUdfpsStateMachine.visibilityArgument(
                new Object[] { 0 }));
        assertEquals(Boolean.FALSE, CouiUdfpsStateMachine.visibilityArgument(
                new Object[] { 8 }));
        assertEquals(null, CouiUdfpsStateMachine.visibilityArgument(
                new Object[] { "not-visibility" }));
    }

    @Test
    public void updateMonitorSuccessUsesItsOwningUiMech() {
        Object owningUiMech = new Object();
        Object staleFallback = new Object();
        String callbackClass =
                "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateMonitorCallback$1";

        assertEquals(owningUiMech, CouiUdfpsStateMachine.resolveAuthenticationUiMech(
                callbackClass, owningUiMech, staleFallback));
        assertEquals(staleFallback, CouiUdfpsStateMachine.resolveAuthenticationUiMech(
                "com.oplus.systemui.statusbar.phone.OplusBiometricUnlockControllerExImpl",
                owningUiMech, staleFallback));
    }
}
