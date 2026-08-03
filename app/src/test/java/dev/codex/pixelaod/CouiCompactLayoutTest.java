package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CouiCompactLayoutTest {
    @Test
    public void matchesTheReferenceCoordinatesOnTheOnePlus12Canvas() {
        float density = 4f;

        assertEquals(400, CouiCompactLayout.clockCenterX(1440, density));
        assertEquals(433, CouiCompactLayout.clockTop(3168, density));
        assertEquals(944, CouiCompactLayout.infoCenterX(1440, density));
        assertEquals(506, CouiCompactLayout.infoTop(3168, density));
        assertEquals(128, CouiCompactLayout.mediaLeft(density));
        assertEquals(808, CouiCompactLayout.mediaTop(3168));
    }

    @Test
    public void keepsClockAndInformationContentCenteredOnTheirAnchors() {
        float density = 4f;

        assertEquals(240, CouiCompactLayout.clockLeft(1440, 320, density));
        assertEquals(744, CouiCompactLayout.infoLeft(1440, 400, density));
    }

    @Test
    public void fallsBackToTheExistingDpCoordinatesUntilTheViewportIsMeasured() {
        CouiCompactLayout.Anchors anchors = CouiCompactLayout.anchors(0, 0, 320, 400, 4f);

        assertEquals(108, anchors.clockLeftPx);
        assertEquals(296, anchors.clockTopPx);
        assertEquals(136, anchors.infoLeftPx);
        assertEquals(600, anchors.infoTopPx);
        assertEquals(936, CouiCompactLayout.mediaTopForViewport(0, 4f));
    }
}
