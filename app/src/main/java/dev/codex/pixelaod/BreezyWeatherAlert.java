package dev.codex.pixelaod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/** Immutable, privacy-preserving projection of one Breezy Weather active alert. */
final class BreezyWeatherAlert {
    static final long DISPLAY_TIMEOUT_MILLIS = 10L * 60L * 1000L;
    static final long REPEAT_COOLDOWN_MILLIS = 2L * 60L * 60L * 1000L;
    private static final BreezyWeatherAlert EMPTY = new BreezyWeatherAlert("", "", "", 0,
            0L, 0L, 0L);

    final String providerId;
    final String locationId;
    final String headline;
    final long startMillis;
    final long endMillis;
    final int severity;
    /** Legacy relay observation time; visible-window history now lives in SystemUI state. */
    final long observedAtMillis;
    final String identity;
    final String presentationKey;
    final String normalizedHeadline;

    private BreezyWeatherAlert(String providerId, String locationId, String headline,
            int severity, long startMillis, long endMillis, long observedAtMillis) {
        this.providerId = normalizeSimple(providerId);
        this.locationId = normalizeSimple(locationId);
        this.headline = normalizeHeadlineForDisplay(headline);
        this.severity = Math.max(0, severity);
        this.startMillis = Math.max(0L, startMillis);
        this.endMillis = Math.max(0L, endMillis);
        this.observedAtMillis = Math.max(0L, observedAtMillis);
        this.normalizedHeadline = normalizeHeadline(headline);
        this.identity = buildIdentity(this.providerId, this.locationId, this.normalizedHeadline,
                this.startMillis);
        // A stable provider id keeps the logical source identity stable, while a substantive
        // headline change still gets a fresh presentation history through this versioned key.
        this.presentationKey = this.identity + "|headline=" + this.normalizedHeadline;
    }

    static BreezyWeatherAlert empty() {
        return EMPTY;
    }

    boolean isEmpty() {
        return this == EMPTY || headline.isEmpty();
    }

    static BreezyWeatherAlert forFields(String headline, long startMillis, long endMillis,
            int severity) {
        return forFields("", "", headline, startMillis, endMillis, severity);
    }

    static BreezyWeatherAlert forFields(String providerId, String locationId, String headline,
            long startMillis, long endMillis, int severity) {
        return new BreezyWeatherAlert(providerId, locationId, headline, severity, startMillis,
                endMillis, 0L);
    }

    static BreezyWeatherAlert fromRelayJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return EMPTY;
        }
        try {
            JSONObject object = new JSONObject(json);
            BreezyWeatherAlert alert = fromJson(object, object.optString("locationId", ""));
            // Older cache records did not have an observation time. A real provider start time is
            // a safe lower bound, so legacy stale warnings cannot reappear indefinitely.
            return alert.observedAtMillis <= 0L && alert.startMillis > 0L
                    ? alert.withObservedAt(alert.startMillis) : alert;
        } catch (Throwable ignored) {
            return EMPTY;
        }
    }

    static BreezyWeatherAlert fromJson(JSONObject object, String fallbackLocationId) {
        if (object == null) {
            return EMPTY;
        }
        String providerId = firstString(object, "providerId", "alertId", "id", "identifier",
                "eventId", "event_id", "uid");
        String locationId = firstString(object, "locationId", "location_id", "placeId",
                "place_id", "cityId", "city_id");
        if (locationId.isEmpty()) {
            locationId = fallbackLocationId;
        }
        String headline = firstString(object, "headline", "title", "event", "description");
        return new BreezyWeatherAlert(providerId, locationId, headline,
                severityRank(object.opt("severity")),
                firstEpochMillis(object, "startMillis", "startDate", "start", "onset",
                        "validFromMillis", "validFrom", "validityStart", "validityStartTime",
                        "effectiveFrom", "begin", "beginDate", "startTime"),
                firstEpochMillis(object, "endMillis", "endDate", "end", "expires",
                        "validToMillis", "validTo", "validityEnd", "validityEndTime",
                        "effectiveTo", "finish", "finishDate", "endTime"),
                firstEpochMillis(object, "observedAtMillis"));
    }

    static List<BreezyWeatherAlert> fromJsonArray(JSONArray array, String locationId) {
        if (array == null || array.length() == 0) {
            return Collections.emptyList();
        }
        ArrayList<BreezyWeatherAlert> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            BreezyWeatherAlert alert = fromJson(array.optJSONObject(i), locationId);
            if (!alert.isEmpty()) {
                result.add(alert);
            }
        }
        return result;
    }

    static BreezyWeatherAlert fromProviderJson(String json, long nowMillis) {
        if (json == null || json.trim().isEmpty()) {
            return EMPTY;
        }
        try {
            Object parsed = new org.json.JSONTokener(json).nextValue();
            JSONObject weather = parsed instanceof JSONObject ? (JSONObject) parsed : null;
            if (parsed instanceof JSONArray) {
                weather = ((JSONArray) parsed).optJSONObject(0);
            }
            if (weather == null) {
                return EMPTY;
            }
            JSONArray alerts = weather.optJSONArray("alerts");
            BreezyWeatherAlert best = EMPTY;
            if (alerts != null) {
                for (BreezyWeatherAlert candidate : fromJsonArray(alerts,
                        weather.optString("locationId", ""))) {
                    best = selectActive(nowMillis, best, candidate);
                }
            }
            return best;
        } catch (Throwable ignored) {
            return EMPTY;
        }
    }

    static BreezyWeatherAlert fromProviderPayload(byte[] payload, long nowMillis) {
        if (payload == null || payload.length == 0) {
            return EMPTY;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(payload));
             InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
            StringBuilder json = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                json.append(buffer, 0, read);
            }
            return fromProviderJson(json.toString(), nowMillis);
        } catch (Throwable ignored) {
            return EMPTY;
        }
    }

    boolean isSourceActive(long nowMillis) {
        return !isEmpty()
                && (startMillis <= 0L || startMillis <= nowMillis)
                && (endMillis <= 0L || endMillis > nowMillis);
    }

    /** Legacy one-window view used by old callers; new policy history is external. */
    boolean isActive(long nowMillis) {
        return isSourceActive(nowMillis)
                && (observedAtMillis <= 0L
                || (nowMillis >= observedAtMillis
                && nowMillis - observedAtMillis < DISPLAY_TIMEOUT_MILLIS));
    }

    long displayExpiresAtMillis() {
        return observedAtMillis > 0L ? observedAtMillis + DISPLAY_TIMEOUT_MILLIS : 0L;
    }

    boolean sameLogicalIdentity(BreezyWeatherAlert other) {
        return other != null && identity.equals(other.identity);
    }

    boolean samePresentation(BreezyWeatherAlert other) {
        return other != null && presentationKey.equals(other.presentationKey)
                && severity == other.severity;
    }

    /** End-time changes preserve the visible card content and logical history. */
    boolean sameDisplay(BreezyWeatherAlert other) {
        return samePresentation(other);
    }

    /** Legacy helper: preserve the first observation time for an identical relay record. */
    static BreezyWeatherAlert observeForDisplay(BreezyWeatherAlert candidate,
            BreezyWeatherAlert stored, long nowMillis) {
        if (candidate == null || !candidate.isSourceActive(nowMillis)) {
            return EMPTY;
        }
        if (candidate.startMillis > 0L
                && nowMillis - candidate.startMillis >= DISPLAY_TIMEOUT_MILLIS) {
            return EMPTY;
        }
        if (stored != null && !stored.isEmpty() && candidate.samePresentation(stored)
                && stored.observedAtMillis > 0L) {
            return candidate.withObservedAt(stored.observedAtMillis);
        }
        return candidate.withObservedAt(nowMillis);
    }

    static BreezyWeatherAlert selectActive(long nowMillis, BreezyWeatherAlert current,
            BreezyWeatherAlert candidate) {
        if (current == null || !current.isSourceActive(nowMillis)) {
            current = EMPTY;
        }
        if (candidate == null || !candidate.isSourceActive(nowMillis)) {
            return current;
        }
        if (current.isEmpty() || comparePriority(candidate, current) < 0) {
            return candidate;
        }
        return current;
    }

    static int comparePriority(BreezyWeatherAlert left, BreezyWeatherAlert right) {
        if (left == null) return right == null ? 0 : 1;
        if (right == null) return -1;
        int severity = Integer.compare(right.severity, left.severity);
        if (severity != 0) return severity;
        boolean leftHasEnd = left.endMillis > 0L;
        boolean rightHasEnd = right.endMillis > 0L;
        if (leftHasEnd != rightHasEnd) return leftHasEnd ? -1 : 1;
        if (leftHasEnd && left.endMillis != right.endMillis) {
            return Long.compare(left.endMillis, right.endMillis);
        }
        if (left.startMillis != right.startMillis) {
            return Long.compare(left.startMillis, right.startMillis);
        }
        return left.identity.compareTo(right.identity);
    }

    BreezyWeatherAlert withObservedAt(long observedAtMillis) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new BreezyWeatherAlert(providerId, locationId, headline, severity, startMillis,
                endMillis, observedAtMillis);
    }

    JSONObject toJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("providerId", providerId);
            result.put("locationId", locationId);
            result.put("headline", headline);
            result.put("startMillis", startMillis);
            result.put("endMillis", endMillis);
            result.put("severity", severity);
            result.put("observedAtMillis", observedAtMillis);
        } catch (Throwable ignored) {
            // JSONObject only fails for malformed values; the model contains none.
        }
        return result;
    }

    String toRelayJson() {
        return isEmpty() ? "" : toJson().toString();
    }

    private static String buildIdentity(String providerId, String locationId,
            String normalizedHeadline, long startMillis) {
        if (!providerId.isEmpty()) {
            return "provider:" + normalizeIdentityPart(providerId)
                    + "|location:" + normalizeIdentityPart(locationId);
        }
        return "location:" + normalizeIdentityPart(locationId)
                + "|headline:" + normalizedHeadline
                + "|start:" + startMillis;
    }

    static String normalizeHeadline(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private static String normalizeHeadlineForDisplay(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    private static String normalizeSimple(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeIdentityPart(String value) {
        return normalizeSimple(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private static long firstEpochMillis(JSONObject object, String... keys) {
        for (String key : keys) {
            long parsed = parseEpochMillis(object.opt(key));
            if (parsed > 0L) {
                return parsed;
            }
        }
        // Some provider revisions group validity fields under an object while retaining the
        // same start/end aliases. Keep an omitted end as zero; never derive one locally.
        for (String containerKey : new String[]{"validity", "validTime", "validityPeriod",
                "timeRange"}) {
            JSONObject nested = object.optJSONObject(containerKey);
            if (nested != null && nested != object) {
                long parsed = firstEpochMillis(nested, keys);
                if (parsed > 0L) {
                    return parsed;
                }
            }
        }
        return 0L;
    }

    private static long normalizeEpochMillis(long timestamp) {
        return timestamp > 0L && timestamp < 100_000_000_000L ? timestamp * 1000L : timestamp;
    }

    /** Parses Breezy's epoch seconds/milliseconds and ISO-8601 date aliases. */
    static long parseEpochMillis(Object value) {
        if (value instanceof Number) {
            return normalizeEpochMillis(((Number) value).longValue());
        }
        if (!(value instanceof String)) {
            return 0L;
        }
        String text = ((String) value).trim();
        if (text.isEmpty()) {
            return 0L;
        }
        try {
            return normalizeEpochMillis(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
            // Try ISO-8601 forms below.
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // Try offset and local ISO forms below.
        }
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // Try a zoned or local provider string below.
        }
        try {
            return ZonedDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME)
                    .toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // Try a local date-time using the device zone as a final ISO fallback.
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }

    static int severityRank(Object raw) {
        if (raw instanceof Number) {
            return Math.max(0, ((Number) raw).intValue());
        }
        String value = raw != null ? String.valueOf(raw).trim().toUpperCase(Locale.ROOT) : "";
        if (value.contains("EXTREME")) return 4;
        if (value.contains("SEVERE") || value.contains("MAJOR")) return 3;
        if (value.contains("MODERATE")) return 2;
        if (value.contains("MINOR")) return 1;
        return 0;
    }

    static final class QueryResult {
        final boolean queried;
        final BreezyWeatherAlert alert;

        private QueryResult(boolean queried, BreezyWeatherAlert alert) {
            this.queried = queried;
            this.alert = alert != null ? alert : EMPTY;
        }

        static QueryResult queried(BreezyWeatherAlert alert) {
            return new QueryResult(true, alert);
        }

        static QueryResult notQueried() {
            return new QueryResult(false, EMPTY);
        }
    }
}
