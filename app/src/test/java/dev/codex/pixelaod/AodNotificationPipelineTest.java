package dev.codex.pixelaod;

import android.app.Notification;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
    public void nativeLockscreenVisibilityResultIsConsumedWithoutLocalImportancePolicy() {
        assertTrue(AodNotificationPipeline.isVisibleFromNativeDecision(false, null));
        assertTrue(AodNotificationPipeline.isVisibleFromNativeDecision(null, false));
        assertFalse(AodNotificationPipeline.isVisibleFromNativeDecision(true, false));
        assertFalse(AodNotificationPipeline.isVisibleFromNativeDecision(false, true));
        assertFalse(AodNotificationPipeline.isVisibleFromNativeDecision(null, null));
    }

    @Test
    public void hardRankingHideCannotBeOverriddenByStaleNativeVisibility() {
        AodNotificationPipeline.RankingSnapshot overrideSecret =
                new AodNotificationPipeline.RankingSnapshot(
                        Notification.VISIBILITY_SECRET, Notification.VISIBILITY_PRIVATE,
                        AodNotificationPipeline.NotificationManagerImportance.DEFAULT, 0);
        AodNotificationPipeline.RankingSnapshot channelSecret =
                new AodNotificationPipeline.RankingSnapshot(
                        Notification.VISIBILITY_PRIVATE, Notification.VISIBILITY_SECRET,
                        AodNotificationPipeline.NotificationManagerImportance.DEFAULT, 0);
        AodNotificationPipeline.RankingSnapshot importanceNone =
                new AodNotificationPipeline.RankingSnapshot(
                        Notification.VISIBILITY_PRIVATE, Notification.VISIBILITY_PRIVATE,
                        AodNotificationPipeline.NotificationManagerImportance.NONE, 0);
        AodNotificationPipeline.RankingSnapshot visible =
                new AodNotificationPipeline.RankingSnapshot(
                        Notification.VISIBILITY_PRIVATE, Notification.VISIBILITY_PRIVATE,
                        AodNotificationPipeline.NotificationManagerImportance.DEFAULT, 0);

        assertTrue(AodNotificationPipeline.isAuthoritativelyHiddenByRanking(
                false, false, overrideSecret));
        assertTrue(AodNotificationPipeline.isAuthoritativelyHiddenByRanking(
                false, false, channelSecret));
        assertTrue(AodNotificationPipeline.isAuthoritativelyHiddenByRanking(
                false, false, importanceNone));
        assertFalse(AodNotificationPipeline.isAuthoritativelyHiddenByRanking(
                false, false, visible));
        assertFalse(AodNotificationPipeline.isAuthoritativelyHiddenByRanking(
                true, false, overrideSecret));
        assertFalse(AodNotificationPipeline.isAuthoritativelyHiddenByRanking(
                false, true, overrideSecret));
    }

    @Test
    public void oosLockscreenCompatibilityOnlyCorrectsOtherwiseEligibleNotifications() {
        assertTrue(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.example", false, true, null,
                Notification.VISIBILITY_PRIVATE, false, false, 3));
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.android.systemui", false, true, null,
                Notification.VISIBILITY_PRIVATE, false, false, 3));
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.example", false, true, Notification.CATEGORY_TRANSPORT,
                Notification.VISIBILITY_PRIVATE, false, false, 3));
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.example", false, true, null,
                Notification.VISIBILITY_SECRET, false, false, 3));
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.example", false, true, null,
                Notification.VISIBILITY_PRIVATE, true, false, 3));
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.example", false, true, null,
                Notification.VISIBILITY_PRIVATE, false, true, 3));
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                "com.example", false, true, null,
                Notification.VISIBILITY_PRIVATE, false, false, 2));
    }

    @Test
    public void moduleTestNotificationRemainsUsableForCompatibilityVerification() {
        assertFalse(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                AodNotificationPipeline.MODULE_PACKAGE, false, true, null,
                Notification.VISIBILITY_PRIVATE, false, false, 3));
        assertTrue(AodNotificationPipeline.isEligibleForOosLockscreenVisibilityCorrection(
                AodNotificationPipeline.MODULE_PACKAGE, true, true, null,
                Notification.VISIBILITY_PRIVATE, false, false, 2));
    }

    @Test
    public void notificationPresentationSignatureIgnoresOnlyPostTime() {
        String before = AodNotificationPipeline.notificationPresentationSignature(
                "0|com.example|1|null|10001", 1_000L,
                Notification.VISIBILITY_PRIVATE, Notification.FLAG_ONGOING_EVENT,
                "status", "resource:com.example:0x7f080001");
        String postTimeOnlyUpdate = AodNotificationPipeline.notificationPresentationSignature(
                "0|com.example|1|null|10001", 2_000L,
                Notification.VISIBILITY_PRIVATE, Notification.FLAG_ONGOING_EVENT,
                "status", "resource:com.example:0x7f080001");
        String iconUpdate = AodNotificationPipeline.notificationPresentationSignature(
                "0|com.example|1|null|10001", 2_000L,
                Notification.VISIBILITY_PRIVATE, Notification.FLAG_ONGOING_EVENT,
                "status", "resource:com.example:0x7f080002");

        assertEquals(before, postTimeOnlyUpdate);
        assertNotEquals(before, iconUpdate);
    }
}
