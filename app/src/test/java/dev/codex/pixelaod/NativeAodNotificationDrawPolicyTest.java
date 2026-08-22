package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NativeAodNotificationDrawPolicyTest {
    @Test
    public void suppressesOnlyExactOplusAodIconRowDuringOwnedAmbientSession() {
        assertTrue(NativeAodNotificationDrawPolicy.shouldSuppress(
                true, true, false, true, true));
        assertFalse(NativeAodNotificationDrawPolicy.shouldSuppress(
                false, true, false, true, true));
        assertFalse(NativeAodNotificationDrawPolicy.shouldSuppress(
                true, false, false, true, true));
        assertFalse(NativeAodNotificationDrawPolicy.shouldSuppress(
                true, true, true, true, true));
        assertFalse(NativeAodNotificationDrawPolicy.shouldSuppress(
                true, true, false, false, true));
        assertFalse(NativeAodNotificationDrawPolicy.shouldSuppress(
                true, true, false, true, false));
    }
}
