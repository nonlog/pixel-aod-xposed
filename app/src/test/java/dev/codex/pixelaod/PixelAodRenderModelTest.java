package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

public final class PixelAodRenderModelTest {
    @Test
    public void keepsTheCompactDateAndWeatherOnSeparateLines() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Calendar calendar = Calendar.getInstance(Locale.US);
            calendar.set(2026, Calendar.AUGUST, 3);

            String date = PixelAodRenderModel.formatDate(calendar);
            String weather = PixelAodRenderModel.formatWeatherText("33°");

            assertEquals("Mon, Aug 3", date);
            assertEquals("33°", weather);
            assertFalse(date.contains(weather));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
