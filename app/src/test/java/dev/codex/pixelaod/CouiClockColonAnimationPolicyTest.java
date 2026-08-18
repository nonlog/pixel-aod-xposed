package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CouiClockColonAnimationPolicyTest {
    @Test
    public void appearingColonUsesReferenceDelayAndAlphaDuration() {
        assertEquals(286L, CouiClockColonAnimationPolicy.alphaStartDelay(1f, 0f, 550L));
        assertEquals(121L, CouiClockColonAnimationPolicy.alphaDuration(550L));
    }

    @Test
    public void disappearingColonStartsImmediatelyAndNeverUsesZeroDuration() {
        assertEquals(0L, CouiClockColonAnimationPolicy.alphaStartDelay(0f, 1f, 550L));
        assertEquals(1L, CouiClockColonAnimationPolicy.alphaDuration(0L));
    }
}
