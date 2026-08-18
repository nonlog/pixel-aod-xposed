package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CouiClockInformationPolicyTest {
    @Test
    public void mapsExistingDateAndWeatherTextWithoutDroppingEitherSlot() {
        CouiClockInformationPolicy.Data data = CouiClockInformationPolicy.from(
                "Mon, Aug 3", "33°");

        assertFalse(data.dateText.isEmpty());
        assertEquals("33°", data.weatherText);
    }
}
