package dev.codex.pixelaod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/** A minimal, privacy-preserving projection of Breezy Weather's active alert. */
final class BreezyWeatherAlert {
    private static final BreezyWeatherAlert EMPTY = new BreezyWeatherAlert("", 0L, 0L, 0);

    final String headline;
    final long startMillis;
    final long endMillis;
    final int severity;

    private BreezyWeatherAlert(String headline, long startMillis, long endMillis, int severity) {
        this.headline = headline != null ? headline.trim() : "";
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.severity = severity;
    }

    static BreezyWeatherAlert empty() {
        return EMPTY;
    }

    static BreezyWeatherAlert fromRelayJson(String json) {
        if (json == null || json.isEmpty()) {
            return EMPTY;
        }
        try {
            return fromAlertObject(new JSONObject(json));
        } catch (Throwable ignored) {
            return EMPTY;
        }
    }

    static BreezyWeatherAlert fromProviderJson(String json, long nowMillis) {
        if (json == null || json.isEmpty()) {
            return EMPTY;
        }
        try {
            JSONObject weather = new JSONObject(json);
            JSONArray alerts = weather.optJSONArray("alerts");
            if (alerts == null) {
                return EMPTY;
            }
            BreezyWeatherAlert best = EMPTY;
            for (int i = 0; i < alerts.length(); i++) {
                BreezyWeatherAlert candidate = fromAlertObject(alerts.optJSONObject(i));
                best = selectActive(nowMillis, best, candidate);
            }
            return best;
        } catch (Throwable ignored) {
            return EMPTY;
        }
    }

    boolean isActive(long nowMillis) {
        return !headline.isEmpty()
                && (startMillis <= 0L || startMillis <= nowMillis)
                && (endMillis <= 0L || endMillis > nowMillis);
    }

    boolean sameDisplay(BreezyWeatherAlert other) {
        return other != null
                && headline.equals(other.headline)
                && startMillis == other.startMillis
                && endMillis == other.endMillis
                && severity == other.severity;
    }

    static BreezyWeatherAlert forFields(String headline, long startMillis, long endMillis,
            int severity) {
        return new BreezyWeatherAlert(headline, startMillis, endMillis, severity);
    }

    static BreezyWeatherAlert selectActive(long nowMillis, BreezyWeatherAlert current,
            BreezyWeatherAlert candidate) {
        if (current == null || !current.isActive(nowMillis)) {
            current = EMPTY;
        }
        if (candidate == null || !candidate.isActive(nowMillis)) {
            return current;
        }
        if (current == EMPTY || candidate.severity > current.severity
                || (candidate.severity == current.severity && candidate.endMillis > 0L
                && (current.endMillis == 0L || candidate.endMillis < current.endMillis))) {
            return candidate;
        }
        return current;
    }

    String toRelayJson() {
        if (this == EMPTY || headline.isEmpty()) {
            return "";
        }
        try {
            JSONObject result = new JSONObject();
            result.put("headline", headline);
            result.put("startMillis", startMillis);
            result.put("endMillis", endMillis);
            result.put("severity", severity);
            return result.toString();
        } catch (Throwable ignored) {
            return "";
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

    private static BreezyWeatherAlert fromAlertObject(JSONObject alert) {
        if (alert == null) {
            return EMPTY;
        }
        String headline = firstNonEmpty(alert.optString("headline", ""),
                alert.optString("title", ""), alert.optString("description", ""));
        return new BreezyWeatherAlert(headline,
                firstEpochMillis(alert, "startMillis", "startDate", "start"),
                firstEpochMillis(alert, "endMillis", "endDate", "end"),
                severityRank(alert.opt("severity")));
    }

    private static long firstEpochMillis(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            if (value instanceof Number) {
                return normalizeEpochMillis(((Number) value).longValue());
            }
            if (value instanceof String) {
                try {
                    return normalizeEpochMillis(Long.parseLong((String) value));
                } catch (NumberFormatException ignored) {
                    // Try the next Breezy schema alias.
                }
            }
        }
        return 0L;
    }

    private static long normalizeEpochMillis(long timestamp) {
        return timestamp > 0L && timestamp < 100_000_000_000L ? timestamp * 1000L : timestamp;
    }

    private static int severityRank(Object raw) {
        if (raw instanceof Number) {
            return Math.max(0, ((Number) raw).intValue());
        }
        String value = raw != null ? String.valueOf(raw).trim().toUpperCase() : "";
        if (value.contains("EXTREME")) return 4;
        if (value.contains("SEVERE") || value.contains("MAJOR")) return 3;
        if (value.contains("MODERATE")) return 2;
        if (value.contains("MINOR")) return 1;
        return 0;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
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
