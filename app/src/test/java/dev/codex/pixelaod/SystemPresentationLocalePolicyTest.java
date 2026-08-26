package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

public final class SystemPresentationLocalePolicyTest {
    @Test
    public void localizesClockDigitsInsteadOfForcingAscii() {
        assertEquals("1305", SystemPresentationLocalePolicy.formatFourDigitTime(
                Locale.US, 13, 5));
        assertEquals("١٣٠٥", SystemPresentationLocalePolicy.formatFourDigitTime(
                Locale.forLanguageTag("ar-EG"), 13, 5));
        assertEquals("۱۳۰۵", SystemPresentationLocalePolicy.formatFourDigitTime(
                Locale.forLanguageTag("fa-IR"), 13, 5));
    }

    @Test
    public void keepsCompactAndLargeTimeFormattingOnTheSameLocaleDigits() {
        Locale locale = Locale.forLanguageTag("ar-EG");
        assertEquals("٠٩:٠٧", SystemPresentationLocalePolicy.formatClockText(
                locale, 9, 7, true));
        assertEquals("٠٩\n٠٧", SystemPresentationLocalePolicy.formatClockText(
                locale, 9, 7, false));
    }

    @Test
    public void dateFormatterHonorsThePlatformSuppliedOrderingPattern() {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.set(2026, Calendar.AUGUST, 3);

        assertEquals("Mon, Aug 3", SystemPresentationLocalePolicy.formatWithPattern(
                calendar, Locale.US, "EEE, MMM d"));
        assertEquals("3 Aug Mon", SystemPresentationLocalePolicy.formatWithPattern(
                calendar, Locale.US, "d MMM EEE"));
    }
}
