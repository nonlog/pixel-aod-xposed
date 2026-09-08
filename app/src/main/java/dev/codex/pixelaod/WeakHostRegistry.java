package dev.codex.pixelaod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Index, not owner, of host lifecycle records. Both sides must be weak because a record may
 * reference its root (and its child views). A live host must retain its own record separately.
 */
final class WeakHostRegistry<K, V> {
    private final Map<K, WeakReference<V>> entries = new WeakHashMap<>();

    synchronized void put(K key, V value) {
        entries.put(Objects.requireNonNull(key), new WeakReference<>(Objects.requireNonNull(value)));
    }

    synchronized V get(K key) {
        WeakReference<V> reference = entries.get(key);
        V value = reference == null ? null : reference.get();
        if (reference != null && value == null) {
            entries.remove(key);
        }
        return value;
    }

    synchronized void remove(K key) {
        entries.remove(key);
    }

    synchronized List<V> snapshot() {
        List<V> result = new ArrayList<>();
        Iterator<WeakReference<V>> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            V value = iterator.next().get();
            if (value == null) {
                iterator.remove();
            } else {
                result.add(value);
            }
        }
        return result;
    }
}
