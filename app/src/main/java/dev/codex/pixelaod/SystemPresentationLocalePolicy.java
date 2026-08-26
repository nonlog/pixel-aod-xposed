package dev.codex.pixelaod;

import android.content.Context;
import android.os.LocaleList;
import android.text.format.DateFormat;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Locale/time-format boundary for SystemUI-owned Pixel presentation. */
final class SystemPresentationLocalePolicy {
    private static final String DATE_SKELETON = "MMMEd";

    private SystemPresentationLocalePolicy() {
    }

    static Locale resolveLocale(Context context) {
        if (context != null) {
            try {
                LocaleList locales = context.getResources().getConfiguration().getLocales();
                if (locales != null && !locales.isEmpty() && locales.get(0) != null) {
                    return locales.get(0);
                }
            } catch (Throwable ignored) {
            }
        }
        Locale locale = Locale.getDefault();
        return locale != null ? locale : Locale.US;
    }

    static boolean is24Hour(Context context) {
        if (context == null) {
            return true;
        }
        // The public SDK only exposes the Context overload, while SystemUI builds also expose a
        // user-id overload. Prefer that current-user seam when present without taking a hard
        // compile-time dependency on a hidden API.
        try {
            Method method = DateFormat.class.getDeclaredMethod(
                    "is24HourFormat", Context.class, int.class);
            method.setAccessible(true);
            Object value = method.invoke(null, context, SelectedUserScope.resolveSelectedUserId());
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (Throwable ignored) {
        }
        return DateFormat.is24HourFormat(context);
    }

    static String formatDate(Context context, Calendar calendar) {
        Locale locale = resolveLocale(context);
        String pattern;
        try {
            pattern = DateFormat.getBestDateTimePattern(locale, DATE_SKELETON);
        } catch (Throwable ignored) {
            pattern = "EEE, MMM d";
        }
        return formatWithPattern(calendar, locale, pattern);
    }

    static String formatAccessibleTime(Context context, Calendar calendar) {
        Locale locale = resolveLocale(context);
        String skeleton = is24Hour(context) ? "Hm" : "hm";
        String pattern;
        try {
            pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        } catch (Throwable ignored) {
            pattern = is24Hour(context) ? "HH:mm" : "h:mm a";
        }
        return formatWithPattern(calendar, locale, pattern);
    }

    static String formatClockText(Locale locale, int hour, int minute, boolean compact) {
        Locale resolved = locale != null ? locale : Locale.US;
        String separator = compact ? ":" : "\n";
        return String.format(resolved, "%02d%s%02d", hour, separator, minute);
    }

    static String formatFourDigitTime(Locale locale, int hour, int minute) {
        Locale resolved = locale != null ? locale : Locale.US;
        String value = String.format(resolved, "%02d%02d", hour, minute);
        if (value.length() == 4
                && Character.isDigit(value.charAt(0))
                && Character.isDigit(value.charAt(1))
                && Character.isDigit(value.charAt(2))
                && Character.isDigit(value.charAt(3))) {
            return value;
        }
        // The four separate COUI glyph views require one BMP decimal digit per slot. This fallback
        // is only for an exotic formatter that inserts marks or non-decimal symbols.
        return String.format(Locale.US, "%02d%02d", hour, minute);
    }

    static String formatWithPattern(Calendar calendar, Locale locale, String pattern) {
        if (calendar == null) {
            return "";
        }
        Locale resolved = locale != null ? locale : Locale.US;
        String resolvedPattern = pattern != null && !pattern.isEmpty()
                ? pattern : "EEE, MMM d";
        return new SimpleDateFormat(resolvedPattern, resolved).format(calendar.getTime());
    }
}
