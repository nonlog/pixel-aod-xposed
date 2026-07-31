package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AodInfoStackLayoutTest {
    @Test
    public void keepsCompactDateNotificationAndMediaRowsEvenlySpaced() {
        assertEquals(226, AodInfoStackLayout.mediaTopAfterNotification(
                150, 184, 184, 188, false, false));
    }

    @Test
    public void followsTheLastVisibleAtAGlanceRow() {
        assertEquals(262, AodInfoStackLayout.mediaTopAfterNotification(
                150, 184, 184, 223, false, true));
        assertEquals(301, AodInfoStackLayout.mediaTopAfterNotification(
                150, 184, 223, 262, true, true));
    }

    @Test
    public void preservesDefaultTopUnlessThePrecedingRowNeedsMoreRoom() {
        assertEquals(132, AodInfoStackLayout.topAfterVisibleRow(132, 120, 6));
        assertEquals(150, AodInfoStackLayout.topAfterVisibleRow(132, 144, 6));
        assertEquals(224, AodInfoStackLayout.topAfterVisibleRow(224, 180, 6));
    }

    @Test
    public void usesTheFallbackHeightBeforeAnInfoRowHasBeenMeasured() {
        assertEquals(144, AodInfoStackLayout.rowBottom(122, 0, 22));
        assertEquals(158, AodInfoStackLayout.rowBottom(122, 36, 22));
    }
}
