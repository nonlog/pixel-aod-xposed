package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CouiClockAodContentPolicyTest {
    @Test
    public void noContentKeepsLargeAodSurface() {
        CouiClockPresentationModel.AodContent content =
                CouiClockAodContentPolicy.fromSemanticState(false, 0);

        assertEquals(CouiClockPresentationModel.AodContent.Kind.NONE, content.kind());
        assertEquals(0, content.notificationIconCount());
    }

    @Test
    public void notificationsSelectPartialSmallSurface() {
        CouiClockPresentationModel.AodContent content =
                CouiClockAodContentPolicy.fromSemanticState(false, 3);

        assertEquals(CouiClockPresentationModel.AodContent.Kind.NOTIFICATIONS, content.kind());
        assertEquals(3, content.notificationIconCount());
    }

    @Test
    public void mediaOnlyRetainsMediaWithZeroNotificationIcons() {
        CouiClockPresentationModel.AodContent content =
                CouiClockAodContentPolicy.fromSemanticState(true, 0);

        assertEquals(CouiClockPresentationModel.AodContent.Kind.MEDIA, content.kind());
        assertEquals(0, content.notificationIconCount());
    }

    @Test
    public void mediaAndNotificationsKeepMediaAndIconCount() {
        CouiClockPresentationModel.AodContent content =
                CouiClockAodContentPolicy.fromSemanticState(true, 4);

        assertEquals(CouiClockPresentationModel.AodContent.Kind.MEDIA, content.kind());
        assertEquals(4, content.notificationIconCount());
    }
}
