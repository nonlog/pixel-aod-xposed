package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AodInfoWeightHandoffTest {
    @Test
    public void carriesTheLiveLockscreenWeightIntoAodForTheRemainingMorph() {
        assertTrue(AodInfoWeightHandoff.needsAnimation(500, 450));
        assertEquals(550L, AodInfoWeightHandoff.remainingDurationMillis(500, 450, 500, 550L));
        assertEquals(275L, AodInfoWeightHandoff.remainingDurationMillis(475, 450, 500, 550L));
    }

    @Test
    public void doesNotAnimateOnceTheInformationLineHasReachedAodWeight() {
        assertFalse(AodInfoWeightHandoff.needsAnimation(450, 450));
        assertEquals(0L, AodInfoWeightHandoff.remainingDurationMillis(450, 450, 500, 550L));
    }
}
