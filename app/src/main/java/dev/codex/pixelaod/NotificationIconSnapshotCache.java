package dev.codex.pixelaod;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Bounded per-notification snapshot cache modeled after COUI 2.7's 64-entry icon cache. */
final class NotificationIconSnapshotCache<T> {
    private final int maxEntries;
    private final LinkedHashMap<String, SnapshotEntry<T>> entries;

    NotificationIconSnapshotCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<String, SnapshotEntry<T>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, SnapshotEntry<T>> eldest) {
                return size() > NotificationIconSnapshotCache.this.maxEntries;
            }
        };
    }

    synchronized T get(String key, Object owner, Object iconToken,
            Object configurationToken, int tintColor) {
        if (key == null || key.isEmpty() || owner == null || iconToken == null) {
            return null;
        }
        SnapshotEntry<T> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.owner.get() != owner
                || entry.iconToken != iconToken
                || !Objects.equals(entry.configurationToken, configurationToken)
                || entry.tintColor != tintColor) {
            entries.remove(key);
            return null;
        }
        return entry.value;
    }

    synchronized void put(String key, Object owner, Object iconToken,
            Object configurationToken, int tintColor, T value) {
        if (key == null || key.isEmpty() || owner == null || iconToken == null || value == null) {
            return;
        }
        entries.put(key, new SnapshotEntry<>(
                owner, iconToken, configurationToken, tintColor, value));
    }

    synchronized void remove(String key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    synchronized void clear() {
        entries.clear();
    }

    synchronized int size() {
        return entries.size();
    }

    private static final class SnapshotEntry<T> {
        final WeakReference<Object> owner;
        final Object iconToken;
        final Object configurationToken;
        final int tintColor;
        final T value;

        SnapshotEntry(Object owner, Object iconToken, Object configurationToken, int tintColor,
                T value) {
            this.owner = new WeakReference<>(owner);
            this.iconToken = iconToken;
            this.configurationToken = configurationToken;
            this.tintColor = tintColor;
            this.value = value;
        }
    }
}
