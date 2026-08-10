package dev.codex.pixelaod;

import android.app.Notification;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AodNotificationPipelineTest {
    @Test
    public void rejectsOnlySyntheticAndroidAutogroupSummaryCarriers() {
        assertTrue(AodNotificationPipeline.isSyntheticAutogroupSummaryFlags(0x00000200 | 0x00000400));
        assertFalse(AodNotificationPipeline.isSyntheticAutogroupSummaryFlags(0x00000200));
        assertFalse(AodNotificationPipeline.isSyntheticAutogroupSummaryFlags(0x00000400));
    }

    @Test
    public void recognizesLauncherResourcesWithoutTreatingNormalSmallIconsAsLaunchers() {
        assertTrue(AodNotificationPipeline.isLauncherStyleSmallIconResourceName(
                "net.oneplus.weather:drawable/ic_launcher_weather"));
        assertTrue(AodNotificationPipeline.isLauncherStyleSmallIconResourceName(
                "drawable/ic_launcher"));
        assertFalse(AodNotificationPipeline.isLauncherStyleSmallIconResourceName(
                "net.oneplus.weather:drawable/ic_stat_weather"));
        assertFalse(AodNotificationPipeline.isLauncherStyleSmallIconResourceName(null));
    }

    @Test
    public void silentDefaultImportanceRemainsEligibleButLowOrLessIsRejected() {
        int silent = AodNotificationPipeline.NOTIFICATION_FLAG_SILENT;
        assertFalse(AodNotificationPipeline.shouldHideForLockscreenImportance(silent, 3));
        assertTrue(AodNotificationPipeline.shouldHideForLockscreenImportance(silent, 2));
        assertTrue(AodNotificationPipeline.shouldHideForLockscreenImportance(silent, 1));
        assertFalse(AodNotificationPipeline.isLowImportanceForLockscreenPolicy(
                AodNotificationPipeline.NotificationManagerImportance.UNKNOWN));
    }

    @Test
    public void preservesSystemTransportAndSecretExclusions() {
        assertTrue(AodNotificationPipeline.isExcludedFromLockscreenPolicyOverride(
                "com.android.systemui", null, Notification.VISIBILITY_PRIVATE, false));
        assertTrue(AodNotificationPipeline.isExcludedFromLockscreenPolicyOverride(
                "com.example", Notification.CATEGORY_TRANSPORT, Notification.VISIBILITY_PRIVATE, false));
        assertTrue(AodNotificationPipeline.isExcludedFromLockscreenPolicyOverride(
                "com.example", null, Notification.VISIBILITY_SECRET, false));
        assertTrue(AodNotificationPipeline.isExcludedFromLockscreenPolicyOverride(
                "com.example", null, Notification.VISIBILITY_PRIVATE, true));
        assertTrue(AodNotificationPipeline.isExcludedFromLockscreenPolicyOverride(
                "com.example", null, Notification.VISIBILITY_PRIVATE, false, true));
        assertFalse(AodNotificationPipeline.isExcludedFromLockscreenPolicyOverride(
                "com.arn.scrobble", null, Notification.VISIBILITY_PRIVATE, false));
    }
}
