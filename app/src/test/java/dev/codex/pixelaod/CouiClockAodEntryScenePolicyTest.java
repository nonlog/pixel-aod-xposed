package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class CouiClockAodEntryScenePolicyTest {
    @Test
    public void partialAodNotificationsNormalizeTheStagedTargetToSmall() {
        CouiClockPresentationModel.Scene normalized =
                CouiClockAodEntryScenePolicy.normalizeRequestedScene(
                        CouiClockPresentationModel.Scene.SMALL,
                        true,
                        CouiClockPresentationModel.AodContent.notifications(5));

        assertEquals(CouiClockPresentationModel.Scene.SMALL, normalized);
        assertNotEquals(CouiClockPresentationModel.Scene.LARGE, normalized);
    }

    @Test
    public void partialAodNotificationsOverrideAnAccidentallyLargeCallerTarget() {
        assertEquals(CouiClockPresentationModel.Scene.SMALL,
                CouiClockAodEntryScenePolicy.normalizeRequestedScene(
                        CouiClockPresentationModel.Scene.LARGE,
                        true,
                        CouiClockPresentationModel.AodContent.notifications(5)));
    }

    @Test
    public void partialAodWithoutContentUsesLarge() {
        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                CouiClockAodEntryScenePolicy.normalizeRequestedScene(
                        CouiClockPresentationModel.Scene.SMALL,
                        true,
                        CouiClockPresentationModel.AodContent.none()));
    }

    @Test
    public void nonPartialAodRetainsTheLegitimateRequestedScene() {
        assertEquals(CouiClockPresentationModel.Scene.IMMERSED,
                CouiClockAodEntryScenePolicy.normalizeRequestedScene(
                        CouiClockPresentationModel.Scene.IMMERSED,
                        false,
                        CouiClockPresentationModel.AodContent.notifications(5)));
    }
}
