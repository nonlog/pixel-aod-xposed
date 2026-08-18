package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class CouiClockAodEntryNormalizationPolicyTest {
    @Test
    public void partialAodNotificationsEnterSmallOnTheFirstFrame() {
        CouiClockPresentationModel raw = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.LARGE,
                true,
                true,
                CouiClockPresentationModel.AodContent.notifications(2));

        CouiClockPresentationModel normalized =
                CouiClockAodEntryNormalizationPolicy.normalizeUnlockedEntry(raw);

        assertEquals(CouiClockPresentationModel.Scene.SMALL, normalized.requestedScene());
        assertEquals(CouiClockPresentationModel.Scene.SMALL, normalized.visualScene());
    }

    @Test
    public void partialAodMediaEntersSmallOnTheFirstFrame() {
        CouiClockPresentationModel raw = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.LARGE,
                true,
                true,
                CouiClockPresentationModel.AodContent.media(0));

        CouiClockPresentationModel normalized =
                CouiClockAodEntryNormalizationPolicy.normalizeUnlockedEntry(raw);

        assertEquals(CouiClockPresentationModel.Scene.SMALL, normalized.requestedScene());
        assertEquals(CouiClockPresentationModel.Scene.SMALL, normalized.visualScene());
    }

    @Test
    public void partialAodWithoutContentKeepsLargeEntry() {
        CouiClockPresentationModel raw = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.LARGE,
                true,
                true,
                CouiClockPresentationModel.AodContent.none());

        CouiClockPresentationModel normalized =
                CouiClockAodEntryNormalizationPolicy.normalizeUnlockedEntry(raw);

        assertEquals(CouiClockPresentationModel.Scene.LARGE, normalized.requestedScene());
        assertEquals(CouiClockPresentationModel.Scene.LARGE, normalized.visualScene());
    }

    @Test
    public void nonPartialPresentationIsNotRewritten() {
        CouiClockPresentationModel lockscreen = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.SMALL,
                false,
                false,
                CouiClockPresentationModel.AodContent.notifications(2));

        assertSame(lockscreen,
                CouiClockAodEntryNormalizationPolicy.normalizeUnlockedEntry(lockscreen));
    }
}
