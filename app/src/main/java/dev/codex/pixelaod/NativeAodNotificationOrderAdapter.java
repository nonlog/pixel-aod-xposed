package dev.codex.pixelaod;

import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Read-only ordering adapter over NotificationListenerService.RankingMap#getOrderedKeys(). */
final class NativeAodNotificationOrderAdapter {
    private NativeAodNotificationOrderAdapter() {
    }

    static StatusBarNotification[] apply(StatusBarNotification[] eligible,
            String[] systemOrderedKeys) {
        if (eligible == null || eligible.length == 0) {
            return new StatusBarNotification[0];
        }
        LinkedHashMap<String, StatusBarNotification> byKey = new LinkedHashMap<>();
        for (StatusBarNotification sbn : eligible) {
            if (sbn != null && sbn.getKey() != null) {
                byKey.putIfAbsent(sbn.getKey(), sbn);
            }
        }
        List<String> ordered = orderKeys(byKey.keySet(), systemOrderedKeys);
        ArrayList<StatusBarNotification> result = new ArrayList<>(ordered.size());
        for (String key : ordered) {
            StatusBarNotification sbn = byKey.get(key);
            if (sbn != null) {
                result.add(sbn);
            }
        }
        return result.toArray(new StatusBarNotification[0]);
    }

    static List<String> orderKeys(Iterable<String> eligibleKeys, String[] systemOrderedKeys) {
        LinkedHashSet<String> remaining = new LinkedHashSet<>();
        if (eligibleKeys != null) {
            for (String key : eligibleKeys) {
                if (key != null && !key.isEmpty()) {
                    remaining.add(key);
                }
            }
        }
        ArrayList<String> result = new ArrayList<>(remaining.size());
        if (systemOrderedKeys != null) {
            for (String key : systemOrderedKeys) {
                if (key != null && remaining.remove(key)) {
                    result.add(key);
                }
            }
        }
        result.addAll(remaining);
        return result;
    }

    static String signature(String[] systemOrderedKeys) {
        if (systemOrderedKeys == null || systemOrderedKeys.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String key : systemOrderedKeys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\u0001');
            }
            builder.append(key);
        }
        return builder.toString();
    }
}