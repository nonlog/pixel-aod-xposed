package dev.codex.pixelaod;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Source contracts for the COUI 2.7 notification performance port. */
public final class NotificationPerformanceWiringTest {
    private static String source(String name) throws Exception {
        Path directory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null && !Files.exists(directory.resolve("settings.gradle"))) {
            directory = directory.getParent();
        }
        assertNotNull("repository root", directory);
        return new String(Files.readAllBytes(directory.resolve(
                "app/src/main/java/dev/codex/pixelaod/" + name + ".java")),
                StandardCharsets.UTF_8);
    }

    private static String section(String text, String from, String to) {
        int start = text.indexOf(from);
        assertTrue("missing start: " + from, start >= 0);
        int end = text.indexOf(to, start + from.length());
        assertTrue("missing end: " + to, end > start);
        return text.substring(start, end);
    }

    @Test
    public void liveAlertAndTorchRefreshesShareTheBoundedBatch() throws Exception {
        String text = source("PixelAodHook");
        assertTrue(text.contains("LIVE_ALERT_NOTIFICATION_REFRESH_DELAYS_MILLIS"));
        assertTrue(text.contains("new NotificationRefreshBatch("));
        String torch = section(text, "private static void onTorchStateChanged",
                "private static void scheduleLiveAlertNotificationRefresh");
        assertTrue(torch.contains("LIVE_ALERT_NOTIFICATION_REFRESH_BATCH.request"));
        assertFalse(torch.contains("MAIN.postDelayed"));
        String live = section(text, "private static void scheduleLiveAlertNotificationRefresh",
                "private static void requestNativeAodFrameRefreshKickForLiveAlert");
        assertTrue(live.contains("LIVE_ALERT_NOTIFICATION_REFRESH_BATCH.request"));
        assertFalse(live.contains("MAIN.postDelayed"));
    }

    @Test
    public void notificationRemovalInvalidatesBothDrawableCaches() throws Exception {
        String remove = section(source("PixelAodHook"),
                "private static void removeCachedNotification",
                "private static void scheduleCachedNotificationSnapshotRefresh");
        assertTrue(remove.contains("NOTIFICATION_CAPSULE_ICON_POLICY.removeFinalDrawable"));
        assertTrue(remove.contains("PixelAodClockView.invalidateNotificationIconSnapshot"));
    }

    @Test
    public void smallIconResolutionUsesBoundedSnapshotCache() throws Exception {
        String text = source("PixelAodClockView");
        assertTrue(text.contains("new NotificationIconSnapshotCache<>(64)"));
        String load = section(text, "static Drawable loadSmallIconDrawable",
                "private static Drawable loadSmallIconDrawableUncached");
        assertTrue(load.contains("NOTIFICATION_ICON_SNAPSHOT_CACHE.get"));
        assertTrue(load.contains("snapshotNotificationIcon"));
        assertTrue(load.contains("NOTIFICATION_ICON_SNAPSHOT_CACHE.put"));
    }

    @Test
    public void maskClassificationReusesPixelBufferInsteadOfPerPixelReads() throws Exception {
        String text = source("PixelAodClockView");
        String classifiers = section(text, "private static boolean looksLikeTinyForeground",
                "private static String mediaCandidatesSignatureLocked");
        assertTrue(classifiers.contains("readIconMaskPixels"));
        assertFalse(classifiers.contains("bitmap.getPixel("));
        assertFalse(classifiers.contains("boolean[][]"));
        String usb = section(text, "private static boolean mayBeColoredSystemUiUsbIcon",
                "private static int parseThemeColor");
        assertTrue(usb.contains("readIconMaskPixels"));
        assertFalse(usb.contains("bitmap.getPixel("));
    }
}
