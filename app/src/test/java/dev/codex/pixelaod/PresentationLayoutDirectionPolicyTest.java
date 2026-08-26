package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PresentationLayoutDirectionPolicyTest {
    @Test
    public void mirrorsStartAnchorsWithoutChangingLtrCoordinates() {
        assertEquals(128f, PresentationLayoutDirectionPolicy.startAlignedX(
                1440f, 400f, 128f, false), 0.001f);
        assertEquals(912f, PresentationLayoutDirectionPolicy.startAlignedX(
                1440f, 400f, 128f, true), 0.001f);
        assertEquals(370f, PresentationLayoutDirectionPolicy.mirrorCenter(
                1440f, 370f, false), 0.001f);
        assertEquals(1070f, PresentationLayoutDirectionPolicy.mirrorCenter(
                1440f, 370f, true), 0.001f);
    }

    @Test
    public void keepsCompactInformationClearOfClockInBothDirections() {
        assertEquals(760f, PresentationLayoutDirectionPolicy.compactInformationStart(
                720f, 400f, 100f, 712f, 48f, 64f, 976f, false), 0.001f);
        assertEquals(280f, PresentationLayoutDirectionPolicy.compactInformationStart(
                320f, 400f, 728f, 1340f, 48f, 64f, 976f, true), 0.001f);
    }
}
