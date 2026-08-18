package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockAodTransitionPolicyTest {
    @Test
    public void animatedEntryStaysActiveThroughThe550MillisecondTargetWindow() {
        CouiClockAodTransitionPolicy.EntryState staged =
                CouiClockAodTransitionPolicy.EntryState.begin(7L, true,
                        CouiClockPresentationModel.AodContent.none());
        CouiClockAodTransitionPolicy.EntryState active = staged.afterAnimationFrame();

        assertTrue(active.isActiveAt(0L));
        assertTrue(active.isActiveAt(549L));
        assertFalse(active.isActiveAt(550L));
        assertEquals(550L, active.completionDelayMillis());
    }

    @Test
    public void staleEntryCompletionCannotFinishTheCurrentGeneration() {
        CouiClockAodTransitionPolicy.EntryState active =
                CouiClockAodTransitionPolicy.EntryState.begin(7L, true,
                        CouiClockPresentationModel.AodContent.notifications(1)).afterAnimationFrame()
                        .defer(CouiClockPresentationModel.AodContent.notifications(2));

        assertEquals(CouiClockAodTransitionPolicy.EntryCompletion.STALE,
                active.completion(7L, 8L, CouiClockPresentationModel.AodContent.none(), false));
    }

    @Test
    public void deferredSameSurfaceBindsDirectlyButSceneChangeUsesLiveCrossfade() {
        CouiClockAodTransitionPolicy.EntryState active =
                CouiClockAodTransitionPolicy.EntryState.begin(7L, true,
                        CouiClockPresentationModel.AodContent.notifications(1)).afterAnimationFrame()
                        .defer(CouiClockPresentationModel.AodContent.notifications(2));

        assertEquals(CouiClockAodTransitionPolicy.EntryCompletion.BIND_DIRECT,
                active.completion(7L, 7L, CouiClockPresentationModel.AodContent.notifications(1),
                        true));
        CouiClockAodTransitionPolicy.EntryState sceneChanging =
                CouiClockAodTransitionPolicy.EntryState.begin(7L, true,
                        CouiClockPresentationModel.AodContent.notifications(1)).afterAnimationFrame()
                        .defer(CouiClockPresentationModel.AodContent.none());
        assertEquals(CouiClockAodTransitionPolicy.EntryCompletion.LIVE_CROSSFADE,
                sceneChanging.completion(7L, 7L,
                        CouiClockPresentationModel.AodContent.notifications(1), true));
    }

    @Test
    public void liveCrossfadeCoalescesWithoutRestartingGeneration() {
        CouiClockAodTransitionPolicy.LiveState first =
                CouiClockAodTransitionPolicy.LiveState.crossfade(10L,
                        CouiClockPresentationModel.AodContent.notifications(1));
        CouiClockAodTransitionPolicy.LiveState latest = first.defer(
                CouiClockPresentationModel.AodContent.media(0));

        assertEquals(10L, latest.generation());
        assertEquals(CouiClockPresentationModel.AodContent.Kind.MEDIA,
                latest.deferred().kind());
        assertEquals(CouiClockAodTransitionPolicy.LiveCompletion.STALE,
                latest.finishIn(9L));
        assertEquals(CouiClockAodTransitionPolicy.LiveCompletion.APPLY_DEFERRED,
                latest.finishIn(10L));
    }

    @Test
    public void livePredrawRequiresCurrentGenerationAndPartialAod() {
        assertTrue(CouiClockAodTransitionPolicy.acceptsLivePreDraw(12L, 12L, true));
        assertFalse(CouiClockAodTransitionPolicy.acceptsLivePreDraw(11L, 12L, true));
        assertFalse(CouiClockAodTransitionPolicy.acceptsLivePreDraw(12L, 12L, false));
    }

    @Test
    public void repeatedSemanticSnapshotDoesNotRestartLiveTargetAnimation() {
        assertFalse(CouiClockAodTransitionPolicy.shouldRetargetLiveContent(
                CouiClockPresentationModel.AodContent.notifications(4),
                CouiClockPresentationModel.AodContent.notifications(4)));
        assertFalse(CouiClockAodTransitionPolicy.shouldRetargetLiveContent(
                CouiClockPresentationModel.AodContent.media(2),
                CouiClockPresentationModel.AodContent.media(2)));
    }

    @Test
    public void actualSemanticContentChangeStillRetargetsLiveAod() {
        assertTrue(CouiClockAodTransitionPolicy.shouldRetargetLiveContent(
                CouiClockPresentationModel.AodContent.notifications(4),
                CouiClockPresentationModel.AodContent.notifications(5)));
        assertTrue(CouiClockAodTransitionPolicy.shouldRetargetLiveContent(
                CouiClockPresentationModel.AodContent.notifications(4),
                CouiClockPresentationModel.AodContent.media(4)));
    }
}
