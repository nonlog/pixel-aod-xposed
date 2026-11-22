package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PixelPeekPresentationPolicyTest {
    @Test
    public void customPeekTakeoverRequiresBothModuleAndNativePreferences() {
        assertTrue(PixelPeekPresentationPolicy.shouldUsePixelPeek(true, true));
        assertFalse(PixelPeekPresentationPolicy.shouldUsePixelPeek(false, true));
        assertFalse(PixelPeekPresentationPolicy.shouldUsePixelPeek(true, false));
    }

    @Test
    public void nativeDrawIsSuppressedOnlyWhenPixelCardActuallyExists() {
        assertTrue(PixelPeekPresentationPolicy.shouldSuppressNativeDraw(true, true, true, true));
        assertFalse(PixelPeekPresentationPolicy.shouldSuppressNativeDraw(false, true, true, true));
        assertFalse(PixelPeekPresentationPolicy.shouldSuppressNativeDraw(true, true, false, true));
        assertFalse(PixelPeekPresentationPolicy.shouldSuppressNativeDraw(true, true, true, false));
    }

    @Test
    public void nativePeekPreservesOnlyConfiguredContinuousAodModes() {
        assertTrue(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                true, true, "all-day", true, false));
        assertTrue(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                true, true, "scheduled", true, false));
        assertFalse(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                true, true, "scheduled", false, false));
        assertFalse(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                true, true, "energy-saving", true, false));
        assertFalse(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                false, true, "all-day", true, false));
        assertFalse(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                true, false, "all-day", true, false));
        assertFalse(PixelPeekPresentationPolicy.shouldPreserveContinuousAod(
                true, true, "all-day", true, true));
    }
    @Test
    public void nativeSurfaceCompositeRunsOnlyForStockPeekWhenModuleAodIsWanted() {
        assertTrue(PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                false, true, true, true));
        assertFalse(PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                true, true, true, true));
        assertFalse(PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                false, false, true, true));
        assertFalse(PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                false, true, false, true));
        assertFalse(PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                false, true, true, false));
    }

}
