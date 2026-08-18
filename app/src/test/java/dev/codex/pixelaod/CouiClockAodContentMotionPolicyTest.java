package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockAodContentMotionPolicyTest {
    @Test
    public void partialAodEntrySnapsContentGeometryAndAnimatesAlphaOnly() {
        assertFalse(CouiClockAodContentMotionPolicy.preserveCurrentPosition(true, true));
        assertFalse(CouiClockAodContentMotionPolicy.animateTranslation());
    }

    @Test
    public void animatedExitKeepsExistingContentPositionWhileFadingOut() {
        assertTrue(CouiClockAodContentMotionPolicy.preserveCurrentPosition(false, true));
        assertFalse(CouiClockAodContentMotionPolicy.animateTranslation());
    }

    @Test
    public void nonAnimatedTargetCanSnapDirectlyToItsRequestedGeometry() {
        assertFalse(CouiClockAodContentMotionPolicy.preserveCurrentPosition(false, false));
    }
}
