package dev.codex.pixelaod;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/** Durable SystemUI-side history for visible Weather Alert presentations. */
final class ContextualAtAGlanceStateStore {
    private static final String PREFS = "pixel_aod_contextual_at_a_glance";
    private static final String KEY_HISTORY = "weather_alert_history_v1";

    private final SharedPreferences preferences;
    private final Map<String, AtAGlanceWeatherPolicy.AlertHistory> histories = new HashMap<>();

    /** In-memory constructor used by pure tests. */
    ContextualAtAGlanceStateStore() {
        preferences = null;
    }

    ContextualAtAGlanceStateStore(Context context) {
        Context storage = context;
        if (storage != null && android.os.Build.VERSION.SDK_INT >= 24) {
            Context directBoot = storage.createDeviceProtectedStorageContext();
            if (directBoot != null) {
                storage = directBoot;
            }
        }
        preferences = storage != null
                ? storage.getSharedPreferences(PREFS, Context.MODE_PRIVATE) : null;
        load();
    }

    synchronized void reconcile(BreezyWeatherSnapshot snapshot, boolean enabled,
            long nowMillis) {
        if (!enabled) {
            clear();
            return;
        }
        if (snapshot == null || !snapshot.sourceQuerySucceeded) {
            return;
        }
        String locationId = snapshot.activeLocationId;
        Set<String> activeKeys = new HashSet<>();
        for (BreezyWeatherAlert alert : snapshot.activeAlerts) {
            if (alert == null || alert.isEmpty() || !alert.isSourceActive(nowMillis)) {
                continue;
            }
            activeKeys.add(alert.presentationKey);
            AtAGlanceWeatherPolicy.AlertHistory history = histories.get(alert.presentationKey);
            if (history == null) {
                history = new AtAGlanceWeatherPolicy.AlertHistory(alert.presentationKey,
                        locationId, alert.normalizedHeadline, alert.severity);
                histories.put(alert.presentationKey, history);
            } else {
                if (alert.severity > history.severity) {
                    history.resetForNewPresentation(alert.severity,
                            snapshot.lastSuccessfulSourceAtMillis);
                } else {
                    if (alert.severity < history.severity && history.displayDeadlineMillis <= nowMillis) {
                        history.cooldownDeadlineMillis = 0L;
                    }
                    history.severity = alert.severity;
                    history.lastSourceConfirmationAtMillis = snapshot.lastSuccessfulSourceAtMillis;
                }
            }
            history.lastSourceConfirmationAtMillis = snapshot.lastSuccessfulSourceAtMillis;
        }
        // A successful empty/changed source response removes only records for the active
        // location. Histories for previously selected locations remain for a later return.
        if (!locationId.isEmpty()) {
            histories.entrySet().removeIf(entry -> locationId.equals(entry.getValue().locationId)
                    && !activeKeys.contains(entry.getKey()));
        }
        persist();
    }

    synchronized BreezyWeatherAlert select(List<BreezyWeatherAlert> alerts,
            BreezyWeatherSnapshot snapshot, long nowMillis, long surfaceEntryId,
            boolean nextSurfaceEntry) {
        if (alerts == null || snapshot == null) {
            return BreezyWeatherAlert.empty();
        }
        for (BreezyWeatherAlert alert : AtAGlanceWeatherPolicy.orderedAlerts(alerts)) {
            AtAGlanceWeatherPolicy.AlertHistory history = histories.get(alert.presentationKey);
            if (AtAGlanceWeatherPolicy.alertEligible(alert, history, snapshot, nowMillis,
                    surfaceEntryId, nextSurfaceEntry)) {
                return alert;
            }
        }
        return BreezyWeatherAlert.empty();
    }

    synchronized void markVisible(BreezyWeatherAlert alert, BreezyWeatherSnapshot snapshot,
            long nowMillis, long surfaceEntryId) {
        if (alert == null || alert.isEmpty()) {
            return;
        }
        AtAGlanceWeatherPolicy.AlertHistory history = histories.get(alert.presentationKey);
        if (history == null) {
            history = new AtAGlanceWeatherPolicy.AlertHistory(alert.presentationKey,
                    snapshot != null ? snapshot.activeLocationId : alert.locationId,
                    alert.normalizedHeadline, alert.severity);
            histories.put(alert.presentationKey, history);
        }
        // This is the durable identity of the surface entry that last rendered this alert. It
        // must be written for the first visible presentation as well as later repeats; otherwise
        // a refresh on the same continuous lockscreen/AOD surface looks like a new entry after
        // the severe-alert cooldown.
        if (surfaceEntryId > 0L) {
            history.lastRepeatEntryId = surfaceEntryId;
        }
        boolean firstPresentation = history.firstVisibleAtMillis <= 0L;
        if (firstPresentation) {
            history.firstVisibleAtMillis = nowMillis;
        }
        if (history.displayDeadlineMillis <= nowMillis) {
            history.displayDeadlineMillis = nowMillis + AtAGlanceWeatherPolicy.ALERT_DISPLAY_MILLIS;
            history.cooldownDeadlineMillis = alert.severity >= 3
                    ? history.displayDeadlineMillis
                    + AtAGlanceWeatherPolicy.ALERT_REPEAT_COOLDOWN_MILLIS : 0L;
        }
        history.severity = alert.severity;
        if (snapshot != null) {
            history.lastSourceConfirmationAtMillis = snapshot.lastSuccessfulSourceAtMillis;
        }
        persist();
    }

    synchronized AtAGlanceWeatherPolicy.AlertHistory history(String key) {
        AtAGlanceWeatherPolicy.AlertHistory history = histories.get(key);
        return history != null ? history.copy() : null;
    }

    synchronized AlertDeadline deadline(String key) {
        AtAGlanceWeatherPolicy.AlertHistory history = histories.get(key);
        return history == null ? null : new AlertDeadline(history.displayDeadlineMillis);
    }

    synchronized int size() {
        return histories.size();
    }

    static final class AlertDeadline {
        final long displayDeadlineMillis;

        AlertDeadline(long displayDeadlineMillis) {
            this.displayDeadlineMillis = displayDeadlineMillis;
        }
    }

    synchronized void clear() {
        histories.clear();
        if (preferences != null) {
            preferences.edit().remove(KEY_HISTORY).apply();
        }
    }

    String serializeForTests() {
        synchronized (this) {
            StringBuilder result = new StringBuilder();
            for (AtAGlanceWeatherPolicy.AlertHistory history : histories.values()) {
                if (result.length() > 0) {
                    result.append('\n');
                }
                result.append(encode(history.key)).append('|')
                        .append(encode(history.locationId)).append('|')
                        .append(encode(history.headlineKey)).append('|')
                        .append(history.severity).append('|')
                        .append(history.firstVisibleAtMillis).append('|')
                        .append(history.displayDeadlineMillis).append('|')
                        .append(history.cooldownDeadlineMillis).append('|')
                        .append(history.lastSourceConfirmationAtMillis).append('|')
                        .append(history.lastRepeatEntryId);
            }
            return result.toString();
        }
    }

    synchronized void restoreForTests(String serialized) {
        histories.clear();
        if (serialized == null || serialized.isEmpty()) {
            return;
        }
        try {
            for (String line : serialized.split("\\r?\\n")) {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 9) {
                    continue;
                }
                AtAGlanceWeatherPolicy.AlertHistory history =
                        new AtAGlanceWeatherPolicy.AlertHistory(decode(parts[0]),
                                decode(parts[1]), decode(parts[2]), Integer.parseInt(parts[3]));
                history.firstVisibleAtMillis = Long.parseLong(parts[4]);
                history.displayDeadlineMillis = Long.parseLong(parts[5]);
                history.cooldownDeadlineMillis = Long.parseLong(parts[6]);
                history.lastSourceConfirmationAtMillis = Long.parseLong(parts[7]);
                history.lastRepeatEntryId = Long.parseLong(parts[8]);
                histories.put(history.key, history);
            }
        } catch (Throwable ignored) {
            histories.clear();
        }
    }

    private void load() {
        if (preferences == null) {
            return;
        }
        readHistories(preferences.getString(KEY_HISTORY, "[]"));
    }

    private void persist() {
        if (preferences == null) {
            return;
        }
        preferences.edit().putString(KEY_HISTORY, serializeHistories()).apply();
    }

    private String serializeHistories() {
        JSONArray array = new JSONArray();
        for (AtAGlanceWeatherPolicy.AlertHistory history : histories.values()) {
            JSONObject object = new JSONObject();
            try {
                object.put("key", history.key);
                object.put("locationId", history.locationId);
                object.put("headlineKey", history.headlineKey);
                object.put("severity", history.severity);
                object.put("firstVisibleAtMillis", history.firstVisibleAtMillis);
                object.put("displayDeadlineMillis", history.displayDeadlineMillis);
                object.put("cooldownDeadlineMillis", history.cooldownDeadlineMillis);
                object.put("lastSourceConfirmationAtMillis",
                        history.lastSourceConfirmationAtMillis);
                object.put("lastRepeatEntryId", history.lastRepeatEntryId);
                array.put(object);
            } catch (Throwable ignored) {
                // Skip only the malformed record; other histories remain durable.
            }
        }
        return array.toString();
    }

    private void readHistories(String serialized) {
        try {
            JSONArray array = new JSONArray(serialized != null ? serialized : "[]");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String key = object.optString("key", "");
                if (key.isEmpty()) {
                    continue;
                }
                AtAGlanceWeatherPolicy.AlertHistory history =
                        new AtAGlanceWeatherPolicy.AlertHistory(key,
                                object.optString("locationId", ""),
                                object.optString("headlineKey", ""),
                                object.optInt("severity", 0));
                history.firstVisibleAtMillis = object.optLong("firstVisibleAtMillis", 0L);
                history.displayDeadlineMillis = object.optLong("displayDeadlineMillis", 0L);
                history.cooldownDeadlineMillis = object.optLong("cooldownDeadlineMillis", 0L);
                history.lastSourceConfirmationAtMillis =
                        object.optLong("lastSourceConfirmationAtMillis", 0L);
                history.lastRepeatEntryId = object.optLong("lastRepeatEntryId", 0L);
                histories.put(key, history);
            }
        } catch (Throwable ignored) {
            histories.clear();
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(
                (value != null ? value : "").getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
