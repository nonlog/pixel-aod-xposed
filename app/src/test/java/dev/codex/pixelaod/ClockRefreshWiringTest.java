package dev.codex.pixelaod;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source contracts supplement policy tests: the 9038 policies passed with no active caller. */
public class ClockRefreshWiringTest {
    private static String source(String name) throws Exception {
        Path directory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null && !Files.exists(directory.resolve("settings.gradle"))) {
            directory = directory.getParent();
        }
        assertNotNull("repository root", directory);
        return new String(Files.readAllBytes(directory.resolve(
                "app/src/main/java/dev/codex/pixelaod/" + name + ".java")), StandardCharsets.UTF_8);
    }

    private static String section(String text, String from, String to) {
        int start = text.indexOf(from);
        assertTrue("missing start: " + from, start >= 0);
        int end = text.indexOf(to, start + from.length());
        assertTrue("missing end: " + to, end > start);
        return text.substring(start, end);
    }

    @Test public void nativeTickReachesPrimaryEvenWithZeroLegacyInstances() throws Exception {
        String method = section(source("PixelAodClockView"),
                "static void refreshAllForNativeAodTick", "private void refreshPresentation");
        int primary = method.indexOf("ActiveClockRendererController.onTimeTick(source)");
        assertTrue(primary >= 0);
        assertTrue(primary < method.indexOf("for (PixelAodClockView view : INSTANCES)"));
        String dispatch = section(source("PixelAodHook"),
                "private static void handleNativeAodRefreshCallback",
                "private static void reassertStockAodSuppressionAfterNativeTick");
        assertTrue(dispatch.contains("PixelAodClockView.refreshAllForNativeAodTick(source)"));
    }

    @Test public void bothPocketRecoveryPathsReachPrimary() throws Exception {
        String text = source("PixelAodClockView");
        String update = section(text, "static void updateProximityFromOos",
                "private static void clearProximityState");
        assertTrue(update.contains(
                "ActiveClockRendererController.onTimeTick(\"oos-proximity-resume\")"));
        assertTrue(update.contains("ActiveClockRendererController.setPocketGuard"));
        String reset = section(text, "static void resetProximityFromOos",
                "static boolean isProximityNear");
        assertTrue(reset.contains(
                "ActiveClockRendererController.onTimeTick(\"oos-proximity-reset\")"));
        assertTrue(reset.contains("ActiveClockRendererController.setPocketGuard(false"));
    }

    @Test public void persistentHostRefreshesBeforeRevealAndOnAncestorVisibility() throws Exception {
        String text = source("CouiClockHostView");
        String reveal = section(text, "void setPrimaryVisible", "void cancelTransitions");
        assertTrue(reveal.indexOf("onTimeTick(") >= 0);
        assertTrue(reveal.indexOf("onTimeTick(") < reveal.indexOf("setVisibility(VISIBLE)"));
        assertTrue(section(text, "public void onVisibilityAggregated",
                "public void onRtlPropertiesChanged").contains("onTimeTick(\"visibility-resume\")"));
    }

    @Test public void everyPrimaryRevealHonorsLivePocketGuard() throws Exception {
        String reveal = section(source("CouiClockHostView"),
                "void setPrimaryVisible", "void cancelTransitions");
        int guard = reveal.indexOf("PixelAodClockView.isPocketGuardActive()");
        assertTrue("all render and prearm reveals must check the pocket guard", guard >= 0);
        assertTrue(guard < reveal.indexOf("if (visible)"));
        assertTrue(reveal.contains("PixelAodRuntimeState.isDeviceInteractive(getContext())"));
    }

    @Test public void cachedProximityGettersCannotOverrideDwellEvents() throws Exception {
        String hook = source("PixelAodHook");
        String getters = section(hook, "if (\"getProxNear\".equals",
                "} else if (vendorWakeTriggerAuthorityHooked");
        assertEquals("both cached getter paths must defer to the installed task hooks", 2,
                getters.split("!vendorProximityPauseAuthorityHooked", -1).length - 1);
        assertTrue(hook.contains("vendorProximityPauseAuthorityHooked = taskHooked"));
    }

    @Test public void successfulDigitsPrecedeCacheCommitAndAncillaryWork() throws Exception {
        String tick = section(source("CouiClockHostView"),
                "boolean onTimeTick(String source)", "void setInformation");
        assertTrue(tick.indexOf("calendar.setTimeZone(zone)") < tick.indexOf("calendar.setTimeInMillis(nowMillis)"));
        assertTrue(tick.indexOf("glyphSet.digits[i].setText") < tick.indexOf("lastClockState = current"));
        assertTrue(tick.indexOf("lastClockState = current") < tick.indexOf("refreshInformationFromExistingAdapters"));
        assertFalse(tick.contains("present("));
        assertFalse(tick.contains("AlarmManager"));
    }

    @Test public void registryIsAnIndexWhileLiveViewOwnsTheRecord() throws Exception {
        String text = source("CouiClockPluginHostController");
        assertTrue(text.contains("WeakHostRegistry<ViewGroup, HostRecord>"));
        assertTrue(text.contains("host.setTag(R.id.coui_clock_host_record, record)"));
        assertTrue(text.contains("record.host.setTag(R.id.coui_clock_host_record, null)"));
        assertFalse(text.contains("new WeakHashMap<ViewGroup, HostRecord>"));
    }

    @Test public void hdrAttachListenerDoesNotCaptureItsWeakMapKey() throws Exception {
        String listener = section(source("CouiUdfpsController"),
                "View.OnAttachStateChangeListener listener =", "HDR_ATTACH_LISTENERS.put");
        assertTrue(listener.contains("configureHdrLayout((ImageView) view)"));
        assertFalse(listener.contains("configureHdrLayout(pressedIcon)"));
    }

    @Test public void providerWriteAuthorizationPrecedesPreferenceMutation() throws Exception {
        String update = section(source("PixelAodSettingsProvider"),
                "public int update", "private static void putSetting");
        int authorization = update.indexOf("SettingsWritePolicy.isTrustedUid");
        assertTrue(authorization >= 0);
        assertTrue(authorization < update.indexOf("SharedPreferences.Editor"));
        assertTrue(update.contains("if (spec == null)"));
        assertFalse(update.contains("editor.putString(key, value)"));
    }
}
