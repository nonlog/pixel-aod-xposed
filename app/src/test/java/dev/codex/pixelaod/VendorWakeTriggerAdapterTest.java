package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VendorWakeTriggerAdapterTest {
    @Test
    public void typeZeroIsSingleTap() {
        VendorWakeTriggerAdapter.Observation observation =
                VendorWakeTriggerAdapter.fromNotifyWakeUpType(0, "vendor");
        assertEquals(VendorWakeTriggerAdapter.Kind.SINGLE_TAP, observation.kind);
        assertEquals("tap", observation.normalizedTrigger);
        assertTrue(observation.presentationCandidate);
    }

    @Test
    public void typeOneIsTiltPickup() {
        VendorWakeTriggerAdapter.Observation observation =
                VendorWakeTriggerAdapter.fromNotifyWakeUpType(1, "vendor");
        assertEquals(VendorWakeTriggerAdapter.Kind.TILT_PICKUP, observation.kind);
        assertEquals("pickup", observation.normalizedTrigger);
        assertTrue(observation.presentationCandidate);
    }

    @Test
    public void typeTwoIsMotion() {
        VendorWakeTriggerAdapter.Observation observation =
                VendorWakeTriggerAdapter.fromNotifyWakeUpType(2, "vendor");
        assertEquals(VendorWakeTriggerAdapter.Kind.MOTION, observation.kind);
        assertEquals("motion", observation.normalizedTrigger);
        assertTrue(observation.presentationCandidate);

        OosAodLifecycleAdapter.TriggerBehavior behavior =
                OosAodLifecycleAdapter.behaviorForTrigger(
                        observation.normalizedTrigger, observation.source, "rawType=2");
        assertEquals("trigger-motion", behavior.eventLabel);
        assertEquals("motion-vendor-transient", behavior.ruleLabel);
        assertTrue(behavior.startsBriefDisplay);
    }

    @Test
    public void unknownTypeStaysObserveOnly() {
        VendorWakeTriggerAdapter.Observation observation =
                VendorWakeTriggerAdapter.fromNotifyWakeUpType(99, "vendor");
        assertEquals(VendorWakeTriggerAdapter.Kind.UNKNOWN, observation.kind);
        assertEquals("unknown", observation.normalizedTrigger);
        assertFalse(observation.presentationCandidate);
    }

    @Test
    public void currentRomDoesNotInventDoubleTapType() {
        for (int rawType = 0; rawType <= 2; rawType++) {
            VendorWakeTriggerAdapter.Observation observation =
                    VendorWakeTriggerAdapter.fromNotifyWakeUpType(rawType, "vendor");
            assertFalse("double-tap".equals(observation.normalizedTrigger));
        }
    }
}
