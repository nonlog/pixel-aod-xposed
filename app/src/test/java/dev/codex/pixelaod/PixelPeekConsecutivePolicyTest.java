package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PixelPeekConsecutivePolicyTest {
    @Test
    public void singleNotificationNeverDefersNativeDismiss() {
        assertEquals(0L, PixelPeekConsecutivePolicy.computeDeferMillis(
                1_000L, 1_000L, 4_000L, 5_000, 0));
    }

    @Test
    public void consecutiveNotificationDefersDismissForFullDisplayWindow() {
        // Session started at 1000ms, 2nd notification arrived at 4500ms, native timer fires at 6000ms.
        // With 5000ms display duration, target end is 9500ms -> remaining is 3500ms.
        assertEquals(3_500L, PixelPeekConsecutivePolicy.computeDeferMillis(
                1_000L, 4_500L, 6_000L, 5_000, 1));
    }

    @Test
    public void consecutiveNotificationIsCappedByMaxTotalSessionDuration() {
        // Session started at 1000ms (max end = 21000ms), 5th notification arrived at 19000ms,
        // timer fires at 20000ms -> capped to 1000ms instead of 4000ms.
        assertEquals(1_000L, PixelPeekConsecutivePolicy.computeDeferMillis(
                1_000L, 19_000L, 20_000L, 5_000, 4));
        // Once past max end, defer is 0.
        assertEquals(0L, PixelPeekConsecutivePolicy.computeDeferMillis(
                1_000L, 20_500L, 21_500L, 5_000, 5));
    }

    @Test
    public void duplicateDetectionAllowsSameKeyWithNewerPostTime() {
        assertTrue(PixelPeekConsecutivePolicy.isDuplicateNotification(
                "0|com.tencent.mm|100|null|0", 1000L,
                "0|com.tencent.mm|100|null|0", 1000L));
        assertFalse(PixelPeekConsecutivePolicy.isDuplicateNotification(
                "0|com.tencent.mm|100|null|0", 1000L,
                "0|com.tencent.mm|100|null|0", 2500L));
        assertFalse(PixelPeekConsecutivePolicy.isDuplicateNotification(
                "0|com.tencent.mm|100|null|0", 1000L,
                "0|org.telegram.messenger|101|null|0", 1000L));
    }

    @Test
    public void blockedPackagesAndKnownNoiseIdsAreRejected() {
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId(null, 1));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("", 1));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.oplus.olc", 1));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.android.incallui", 1));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.google.android.dialer", 1));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.tencent.mm", 40));
        assertFalse(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.tencent.mm", 41));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.tencent.mobileqq", 129));
        assertTrue(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("com.tencent.mobileqq", 2130840368));
        assertFalse(PixelPeekConsecutivePolicy.shouldRejectPackageOrId("org.telegram.messenger", 7));
    }

    @Test
    public void vendorDisplayDurationFallsBackSafely() {
        assertEquals(5_000L, PixelPeekConsecutivePolicy.resolveDisplayDurationMs(-1));
        assertEquals(5_000L, PixelPeekConsecutivePolicy.resolveDisplayDurationMs(0));
        assertEquals(4_000L, PixelPeekConsecutivePolicy.resolveDisplayDurationMs(4_000));
        assertEquals(5_000L, PixelPeekConsecutivePolicy.resolveDisplayDurationMs(60_000));
    }
}
