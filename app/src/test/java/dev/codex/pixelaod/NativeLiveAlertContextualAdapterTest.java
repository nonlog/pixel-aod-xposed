package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

public class NativeLiveAlertContextualAdapterTest {
    @After
    public void tearDown() {
        NativeLiveAlertContextualAdapter.clear("test");
    }

    @Test
    public void timerProgressBootstrapUsesStableSeedlingTimestampOrFailsClosed() {
        long startedAt = 1_000L;
        long now = startedAt + 600_000L;
        long remaining = 1_200_000L;

        assertEquals(67, NativeLiveAlertContextualAdapter.timerRemainingPercent(
                startedAt, now, remaining));
        assertEquals(-1, NativeLiveAlertContextualAdapter.timerRemainingPercent(
                startedAt, startedAt + 2_000L, remaining));
        assertEquals(-1, NativeLiveAlertContextualAdapter.timerRemainingPercent(
                now + 1L, now, remaining));
        assertEquals(-1, NativeLiveAlertContextualAdapter.timerRemainingPercent(
                startedAt, startedAt + 23L * 60L * 60L * 1000L,
                2L * 60L * 60L * 1000L));
    }

    @Test
    public void parsesOplusTimerMetricWithoutUsingVendorLabel() {
        assertEquals(600_000L, NativeLiveAlertContextualAdapter.parseTimerRemainingMillis(
                List.of("Nap 时间", "10:00")));
        assertEquals(754_000L, NativeLiveAlertContextualAdapter.parseTimerRemainingMillis(
                List.of("12", ":", "34", "Nap 时间")));
        assertEquals(3_661_000L, NativeLiveAlertContextualAdapter.parseTimerRemainingMillis(
                List.of("1:01:01")));
        assertEquals(-1L, NativeLiveAlertContextualAdapter.parseTimerRemainingMillis(
                List.of("Nap 时间", "not a metric")));
    }

    @Test
    public void parsesOnlyHotspotCountAndBuildsCompactSemanticLabels() {
        assertEquals(1, NativeLiveAlertContextualAdapter.parseConnectedDeviceCount(
                List.of("1 device connected Personal Hotspot")));
        assertEquals(3, NativeLiveAlertContextualAdapter.parseConnectedDeviceCount(
                List.of("个人热点", "3 台设备")));
        assertEquals(-1, NativeLiveAlertContextualAdapter.parseConnectedDeviceCount(
                List.of("Personal Hotspot")));
        assertEquals("Installing", NativeLiveAlertContextualAdapter
                .progressSemanticLabel("Installing package"));
        assertEquals("Progress", NativeLiveAlertContextualAdapter
                .progressSemanticLabel("Some long vendor title"));
    }
    @Test
    public void genericVendorTextIsFailClosed() {
        long now = 10_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("generic", "pkg.generic",
                        NativeLiveAlertContextualAdapter.SemanticKind.NONE,
                        "Unsupported", "", -1, 0L, false)),
                now, "test-vendor");
        assertTrue(NativeLiveAlertContextualAdapter.currentTargets(true, false, now).isEmpty());
    }

    @Test
    public void timerIsStructuredHighUrgencyDynamicTarget() {
        long now = 20_000L;
        long deadline = 600_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451943", "com.oneplus.deskclock",
                        NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                        "Timer", deadline, true)), now, "test-vendor");

        List<ContextualTarget> targets =
                NativeLiveAlertContextualAdapter.currentTargets(true, false, now);
        assertEquals(1, targets.size());
        ContextualTarget target = targets.get(0);
        assertEquals(ContextualTarget.Source.LIVE_UPDATE, target.source);
        assertEquals(ContextualTarget.Urgency.HIGH, target.urgency);
        assertEquals(ContextualAtAGlanceCard.LiveUpdateKind.TIMER,
                target.card.liveUpdateKind);
        assertEquals("Timer", target.card.text);
        assertEquals("", target.card.liveUpdateMetricText);
        assertEquals(-1, target.card.liveUpdateProgressPercent);
        assertEquals(deadline, target.card.liveUpdateTimeBaseElapsedRealtime);
        assertTrue(target.card.liveUpdateCountDown);
        assertTrue(target.card.isDynamicLiveUpdate());
    }

    @Test
    public void perSecondTimerSamplesKeepStableDeadlineAndDoNotChangeSnapshot() {
        long now = 30_000L;
        long deadline = 630_000L;
        NativeLiveAlertContextualAdapter.Update first =
                NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                        vendor("268451943", "com.oneplus.deskclock",
                                NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                                "Timer", deadline, true)), now, "first");
        assertTrue(first.changed);

        // One second later OPlus may report a deadline with scheduler jitter. Preserve the original
        // monotonic base so this does not cause a whole-card update every second.
        NativeLiveAlertContextualAdapter.Update second =
                NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                        vendor("268451943", "com.oneplus.deskclock",
                                NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                                "Timer", deadline + 900L, true)), now + 1_000L, "second");
        assertFalse(second.changed);
        assertEquals(deadline, NativeLiveAlertContextualAdapter
                .currentTargets(true, false, now + 1_000L).get(0).card
                .liveUpdateTimeBaseElapsedRealtime);
    }

    @Test
    public void materiallyChangedTimerDeadlineIsAccepted() {
        long now = 40_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451943", "com.oneplus.deskclock",
                        NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                        "Timer", 640_000L, true)), now, "first");
        NativeLiveAlertContextualAdapter.Update update =
                NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                        vendor("268451943", "com.oneplus.deskclock",
                                NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                                "Timer", 700_000L, true)), now + 1_000L, "changed");
        assertTrue(update.changed);
        assertEquals(700_000L, NativeLiveAlertContextualAdapter
                .currentTargets(true, false, now + 1_000L).get(0).card
                .liveUpdateTimeBaseElapsedRealtime);
    }

    @Test
    public void hotspotUsesCompactStructuredCountAndNormalUrgency() {
        long now = 50_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451843", "com.oplus.wirelesssettings",
                        NativeLiveAlertContextualAdapter.SemanticKind.HOTSPOT,
                        "Hotspot", "1", -1, 0L, false)), now, "hotspot");
        ContextualTarget target = NativeLiveAlertContextualAdapter
                .currentTargets(true, false, now).get(0);
        assertEquals(ContextualTarget.Urgency.NORMAL, target.urgency);
        assertEquals(ContextualAtAGlanceCard.LiveUpdateKind.HOTSPOT,
                target.card.liveUpdateKind);
        assertEquals("Hotspot", target.card.text);
        assertEquals("1", target.card.liveUpdateMetricText);
        assertEquals(-1, target.card.liveUpdateProgressPercent);
        assertFalse(target.card.isDynamicLiveUpdate());
    }

    @Test
    public void timerOutranksHotspotInSharedArbiter() {
        long now = 60_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451843", "com.oplus.wirelesssettings",
                        NativeLiveAlertContextualAdapter.SemanticKind.HOTSPOT,
                        "Hotspot", "1", -1, 0L, false),
                vendor("268451943", "com.oneplus.deskclock",
                        NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                        "Timer", 660_000L, true)), now, "both");
        ContextualTargetArbiter.Selection selection = ContextualTargetArbiter.select(
                NativeLiveAlertContextualAdapter.currentTargets(true, false, now), now);
        assertEquals("live-update:268451943", selection.target.semanticKey);
    }

    @Test
    public void progressAndCallUseStructuredKinds() {
        long now = 70_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("install", "com.example.installer",
                        NativeLiveAlertContextualAdapter.SemanticKind.PROGRESS,
                        "Installing", "68%", 68, 0L, false),
                vendor("call", "com.android.incallui",
                        NativeLiveAlertContextualAdapter.SemanticKind.CALL,
                        "Call", 50_000L, false)), now, "structured");
        List<ContextualTarget> targets = NativeLiveAlertContextualAdapter
                .currentTargets(true, false, now);
        assertEquals(2, targets.size());
        assertEquals(ContextualAtAGlanceCard.LiveUpdateKind.PROGRESS,
                targets.get(0).card.liveUpdateKind);
        assertEquals("Installing", targets.get(0).card.text);
        assertEquals("68%", targets.get(0).card.liveUpdateMetricText);
        assertEquals(68, targets.get(0).card.liveUpdateProgressPercent);
        assertEquals(ContextualAtAGlanceCard.LiveUpdateKind.CALL,
                targets.get(1).card.liveUpdateKind);
        assertEquals(ContextualTarget.Urgency.CRITICAL, targets.get(1).urgency);
        assertTrue(targets.get(1).card.isDynamicLiveUpdate());
    }

    @Test
    public void finalAodAuthorityOnlyUpgradesExistingStructuredModel() {
        long now = 80_000L;
        NativeLiveAlertContextualAdapter.observeActiveSet(active("268451943"), now,
                "active");
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("service_id", "268451943");
        raw.put("package_name", "com.oneplus.deskclock");
        raw.put("title", "raw vendor timer text");
        raw.put("des", "09:59");
        raw.put("should_show", true);
        NativeLiveAlertContextualAdapter.observeAodData(raw, now, "raw-first");
        assertTrue(NativeLiveAlertContextualAdapter.currentTargets(true, false, now).isEmpty());

        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451943", "com.oneplus.deskclock",
                        NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                        "Timer", 680_000L, true)), now + 1L, "structured");
        List<ContextualTarget> targets = NativeLiveAlertContextualAdapter
                .currentTargets(true, false, now + 1L);
        assertEquals(1, targets.size());
        assertEquals("Timer", targets.get(0).card.text);
    }

    @Test
    public void privacyAndTypedSuppressionStillGateStructuredLiveUpdates() {
        long now = 90_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451943", "com.oneplus.deskclock",
                        NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                        "Timer", 690_000L, true)), now, "timer");
        assertNull(ContextualTargetArbiter.select(
                NativeLiveAlertContextualAdapter.currentTargets(true, true, now), now).target);
        assertNull(ContextualTargetArbiter.select(
                NativeLiveAlertContextualAdapter.currentTargets(false, false, now), now).target);
    }

    @Test
    public void fullVendorSnapshotRemovalDropsStructuredTarget() {
        long now = 100_000L;
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(
                vendor("268451943", "com.oneplus.deskclock",
                        NativeLiveAlertContextualAdapter.SemanticKind.TIMER,
                        "Timer", 700_000L, true)), now, "timer");
        assertFalse(NativeLiveAlertContextualAdapter.currentTargets(true, false, now).isEmpty());
        NativeLiveAlertContextualAdapter.observeVendorLiveSet(List.of(), now + 1L, "removed");
        assertTrue(NativeLiveAlertContextualAdapter.currentTargets(true, false, now + 1L).isEmpty());
    }

    private static Map<String, Object> active(String serviceId) {
        Map<String, Object> active = new LinkedHashMap<>();
        active.put(serviceId, 1L);
        return active;
    }

    private static NativeLiveAlertContextualAdapter.VendorLivePayload vendor(
            String serviceId, String packageName,
            NativeLiveAlertContextualAdapter.SemanticKind kind, String label,
            long timeBaseElapsedRealtime, boolean countDown) {
        return vendor(serviceId, packageName, kind, label, "", -1,
                timeBaseElapsedRealtime, countDown);
    }

    private static NativeLiveAlertContextualAdapter.VendorLivePayload vendor(
            String serviceId, String packageName,
            NativeLiveAlertContextualAdapter.SemanticKind kind, String label, String metricText,
            int progressPercent, long timeBaseElapsedRealtime, boolean countDown) {
        return new NativeLiveAlertContextualAdapter.VendorLivePayload(
                serviceId, packageName, kind, label, metricText, progressPercent,
                timeBaseElapsedRealtime, countDown, true, true);
    }
}
