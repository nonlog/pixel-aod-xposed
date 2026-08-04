package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AodInfoWeightHandoffTest {
    @Test
    public void mapsTheEntireClockTransitionIntoTheCompensatedInfoRange() {
        assertEquals(400, AodInfoWeightHandoff.synchronizedInfoWeight(201, 201, 451));
        assertEquals(450, AodInfoWeightHandoff.synchronizedInfoWeight(326, 201, 451));
        assertEquals(500, AodInfoWeightHandoff.synchronizedInfoWeight(451, 201, 451));
    }

    @Test
    public void clampsTheLiveClockWeightToTheCompensatedRange() {
        assertEquals(400, AodInfoWeightHandoff.synchronizedInfoWeight(-1_000, 201, 451));
        assertEquals(500, AodInfoWeightHandoff.synchronizedInfoWeight(900, 201, 451));
    }

    @Test
    public void usesTheMiddleOfTheCompensatedRangeWhenClockWeightsAreEqual() {
        assertEquals(450, AodInfoWeightHandoff.synchronizedInfoWeight(300, 300, 300));
    }
}
