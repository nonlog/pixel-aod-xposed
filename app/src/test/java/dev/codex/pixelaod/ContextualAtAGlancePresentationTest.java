package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ContextualAtAGlancePresentationTest {
    @Test
    public void liveUpdateUsesCenteredPrimaryMetricHierarchy() {
        ContextualAtAGlanceCard live = ContextualAtAGlanceCard.liveUpdate(
                "live:test", ContextualAtAGlanceCard.LiveUpdateKind.HOTSPOT,
                "Hotspot", "1", -1, 0L, false, 1f);

        assertTrue(LiveUpdateMetricLayoutPolicy.usesDedicatedBlock(live));
        assertTrue(LiveUpdateMetricLayoutPolicy.metricIsPrimary());
        assertEquals(18, LiveUpdateMetricLayoutPolicy.ICON_SIZE_DP);
        assertEquals(13, LiveUpdateMetricLayoutPolicy.LABEL_TEXT_DP);
        assertEquals(30, LiveUpdateMetricLayoutPolicy.METRIC_TEXT_DP);
        assertEquals(500, LiveUpdateMetricLayoutPolicy.LABEL_WEIGHT);
        assertEquals(500, LiveUpdateMetricLayoutPolicy.METRIC_WEIGHT);
        assertEquals(16, LiveUpdateMetricLayoutPolicy.BLOCK_TOP_GAP_DP);
        assertEquals(18, LiveUpdateMetricLayoutPolicy.BLOCK_TO_NOTIFICATION_GAP_DP);
    }

    @Test
    public void onlyTimerUsesThinRemainingProgressBar() {
        ContextualAtAGlanceCard timer = ContextualAtAGlanceCard.liveUpdate(
                "live:timer", ContextualAtAGlanceCard.LiveUpdateKind.TIMER,
                "Timer", "", 67, 1_800_000L, true, 1f);
        ContextualAtAGlanceCard installer = ContextualAtAGlanceCard.liveUpdate(
                "live:installer", ContextualAtAGlanceCard.LiveUpdateKind.PROGRESS,
                "Installing", "68%", 68, 0L, false, 1f);

        assertTrue(LiveUpdateMetricLayoutPolicy.usesProgressBar(timer));
        assertFalse(LiveUpdateMetricLayoutPolicy.usesProgressBar(installer));
        assertEquals(96, LiveUpdateMetricLayoutPolicy.PROGRESS_WIDTH_DP);
        assertEquals(2, LiveUpdateMetricLayoutPolicy.PROGRESS_HEIGHT_DP);
    }

    @Test
    public void timerProgressRecomputesFromStableDeadlineAndTotal() {
        assertEquals(2_400_000L,
                LiveUpdateMetricLayoutPolicy.inferTimerTotalDurationMillis(1_200_000L, 50));
        assertEquals(50,
                LiveUpdateMetricLayoutPolicy.timerRemainingPercent(
                        3_000_000L, 1_000_000L, 4_000_000L));
        assertEquals(0,
                LiveUpdateMetricLayoutPolicy.timerRemainingPercent(
                        1_000_000L, 1_100_000L, 4_000_000L));
        assertEquals(0L,
                LiveUpdateMetricLayoutPolicy.inferTimerTotalDurationMillis(1_200_000L, -1));
    }

    @Test
    public void ordinaryContextualCardKeepsFillRemainingWidth() {
        assertTrue(ContextualAtAGlancePresentation.labelFillsRemainingWidth(
                ContextualAtAGlanceCard.calendar("Calendar", 1f)));
    }
}
