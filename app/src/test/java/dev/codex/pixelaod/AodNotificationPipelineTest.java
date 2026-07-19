package dev.codex.pixelaod;

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
}
