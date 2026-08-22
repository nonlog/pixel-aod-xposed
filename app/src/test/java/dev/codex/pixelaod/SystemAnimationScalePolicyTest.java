package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SystemAnimationScalePolicyTest {
    @Test
    public void zeroScaleDisablesMotionAndSnapsModuleDelay() {
        SystemAnimationScalePolicy.Snapshot snapshot =
                SystemAnimationScalePolicy.fromRawScale(0f);

        assertFalse(snapshot.animationsEnabled);
        assertFalse(snapshot.defaultScale);
        assertEquals(550L, snapshot.frameworkAnimatorDurationMillis(550L));
        assertEquals(0L, snapshot.scaledNonAnimatorDelayMillis(550L));
    }

    @Test
    public void oneScalePreservesProvenBaselineExactly() {
        SystemAnimationScalePolicy.Snapshot snapshot =
                SystemAnimationScalePolicy.fromRawScale(1f);

        assertTrue(snapshot.animationsEnabled);
        assertTrue(snapshot.defaultScale);
        assertEquals(550L, snapshot.frameworkAnimatorDurationMillis(550L));
        assertEquals(550L, snapshot.scaledNonAnimatorDelayMillis(550L));
    }

    @Test
    public void halfScaleLeavesAnimatorBaselineUnchangedButScalesPairedDelay() {
        SystemAnimationScalePolicy.Snapshot snapshot =
                SystemAnimationScalePolicy.fromRawScale(0.5f);

        assertTrue(snapshot.animationsEnabled);
        assertFalse(snapshot.defaultScale);
        assertEquals(550L, snapshot.frameworkAnimatorDurationMillis(550L));
        assertEquals(275L, snapshot.scaledNonAnimatorDelayMillis(550L));
    }

    @Test
    public void doubleScaleLeavesAnimatorBaselineUnchangedButScalesPairedDelay() {
        SystemAnimationScalePolicy.Snapshot snapshot =
                SystemAnimationScalePolicy.fromRawScale(2f);

        assertTrue(snapshot.animationsEnabled);
        assertEquals(550L, snapshot.frameworkAnimatorDurationMillis(550L));
        assertEquals(1100L, snapshot.scaledNonAnimatorDelayMillis(550L));
    }

    @Test
    public void tinyEnabledScaleKeepsPositivePairedDelay() {
        SystemAnimationScalePolicy.Snapshot snapshot =
                SystemAnimationScalePolicy.fromRawScale(0.0001f);

        assertTrue(snapshot.animationsEnabled);
        assertEquals(1L, snapshot.scaledNonAnimatorDelayMillis(1L));
    }

    @Test
    public void invalidScaleFallsBackToStableOneScalePath() {
        SystemAnimationScalePolicy.Snapshot negative =
                SystemAnimationScalePolicy.fromRawScale(-1f);
        SystemAnimationScalePolicy.Snapshot nan =
                SystemAnimationScalePolicy.fromRawScale(Float.NaN);
        SystemAnimationScalePolicy.Snapshot infinite =
                SystemAnimationScalePolicy.fromRawScale(Float.POSITIVE_INFINITY);

        assertTrue(negative.defaultScale);
        assertTrue(nan.defaultScale);
        assertTrue(infinite.defaultScale);
        assertEquals(550L, negative.scaledNonAnimatorDelayMillis(550L));
    }
}