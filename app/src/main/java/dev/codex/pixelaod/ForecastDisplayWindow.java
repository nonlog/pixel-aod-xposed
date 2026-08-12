package dev.codex.pixelaod;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Immutable local-time window used to make tomorrow's forecast available. */
final class ForecastDisplayWindow {
    static final String DEFAULT_START_TIME = "21:00";
    static final String DEFAULT_END_TIME = "23:30";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    final LocalTime start;
    final LocalTime end;

    private ForecastDisplayWindow(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    static ForecastDisplayWindow fromSettings(String startValue, String endValue) {
        LocalTime start = parse(startValue);
        LocalTime end = parse(endValue);
        if (start == null || end == null || start.equals(end)) {
            return defaults();
        }
        return new ForecastDisplayWindow(start, end);
    }

    static ForecastDisplayWindow defaults() {
        return new ForecastDisplayWindow(LocalTime.of(21, 0), LocalTime.of(23, 30));
    }

    boolean contains(long nowMillis, ZoneId zoneId) {
        if (zoneId == null) {
            return false;
        }
        LocalTime now = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalTime();
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        // Crossing midnight: 21:00-01:00 is [21:00, 24:00) U [00:00, 01:00).
        return !now.isBefore(start) || now.isBefore(end);
    }

    long nextBoundaryMillis(long nowMillis, ZoneId zoneId) {
        if (zoneId == null) {
            return 0L;
        }
        ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(zoneId);
        return Math.min(nextOccurrence(now, start), nextOccurrence(now, end));
    }

    private static long nextOccurrence(ZonedDateTime now, LocalTime time) {
        ZonedDateTime candidate = now.toLocalDate().atTime(time).atZone(now.getZone());
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.toInstant().toEpochMilli();
    }

    private static LocalTime parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
