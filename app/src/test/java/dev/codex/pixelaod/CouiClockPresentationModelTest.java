package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockPresentationModelTest {
    @Test
    public void nonDozingPreservesEveryRequestedScene() {
        for (CouiClockPresentationModel.Scene scene
                : CouiClockPresentationModel.Scene.values()) {
            CouiClockPresentationModel model = new CouiClockPresentationModel(
                    scene,
                    false,
                    true,
                    CouiClockPresentationModel.AodContent.media(0));

            assertEquals(scene, model.visualScene());
            assertFalse(model.showsPartialContent());
        }
    }

    @Test
    public void partialAodWithNonePreservesRequestedSceneAndShowsNoContent() {
        CouiClockPresentationModel model = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.IMMERSED,
                true,
                true,
                CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPresentationModel.Scene.IMMERSED, model.visualScene());
        assertFalse(model.showsPartialContent());
    }

    @Test
    public void partialAodNotificationsUseSmallScene() {
        CouiClockPresentationModel model = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.LARGE,
                true,
                true,
                CouiClockPresentationModel.AodContent.notifications(2));

        assertEquals(CouiClockPresentationModel.Scene.SMALL, model.visualScene());
        assertTrue(model.showsPartialContent());
        assertEquals(CouiClockPresentationModel.AodContent.Kind.NOTIFICATIONS,
                model.content().kind());
        assertEquals(2, model.content().notificationIconCount());
    }

    @Test
    public void mediaOnlyRemainsMediaWithZeroNotificationIcons() {
        CouiClockPresentationModel model = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.LARGE,
                true,
                true,
                CouiClockPresentationModel.AodContent.media(0));

        assertEquals(CouiClockPresentationModel.Scene.SMALL, model.visualScene());
        assertTrue(model.showsPartialContent());
        assertEquals(CouiClockPresentationModel.AodContent.Kind.MEDIA,
                model.content().kind());
        assertEquals(0, model.content().notificationIconCount());
    }

    @Test
    public void mediaWithNotificationIconsUsesSmallSceneAndRetainsCount() {
        CouiClockPresentationModel model = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.IMMERSED,
                true,
                true,
                CouiClockPresentationModel.AodContent.media(3));

        assertEquals(CouiClockPresentationModel.Scene.SMALL, model.visualScene());
        assertTrue(model.showsPartialContent());
        assertEquals(CouiClockPresentationModel.AodContent.Kind.MEDIA,
                model.content().kind());
        assertEquals(3, model.content().notificationIconCount());
    }

    @Test
    public void nonPartialAodPreservesRequestedScene() {
        CouiClockPresentationModel model = new CouiClockPresentationModel(
                CouiClockPresentationModel.Scene.SMALL,
                true,
                false,
                CouiClockPresentationModel.AodContent.notifications(1));

        assertEquals(CouiClockPresentationModel.Scene.SMALL, model.visualScene());
        assertFalse(model.showsPartialContent());
    }

    @Test
    public void timingAndMotionConstantsMatchDesignContract() {
        assertEquals(550L, CouiClockPresentationModel.TARGET_TRANSITION_MS);
        assertEquals(150L, CouiClockPresentationModel.LIVE_FADE_OUT_MS);
        assertEquals(200L, CouiClockPresentationModel.LIVE_FADE_IN_MS);
        assertEquals(0.52f, CouiClockPresentationModel.COLON_START_FRACTION, 0f);
        assertEquals(0.22f, CouiClockPresentationModel.COLON_DURATION_FRACTION, 0f);
        assertEquals(0.2f, CouiClockPresentationModel.MOTION_X1, 0f);
        assertEquals(0.0f, CouiClockPresentationModel.MOTION_Y1, 0f);
        assertEquals(0.0f, CouiClockPresentationModel.MOTION_X2, 0f);
        assertEquals(1.0f, CouiClockPresentationModel.MOTION_Y2, 0f);
    }
}
