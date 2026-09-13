package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public final class NotificationIconSnapshotCacheTest {
    @Test
    public void exactOwnerIconConfigurationAndTintHit() {
        NotificationIconSnapshotCache<Object> cache = new NotificationIconSnapshotCache<>(64);
        Object owner = new Object();
        Object icon = new Object();
        Object config = "config-a";
        Object value = new Object();

        cache.put("key", owner, icon, config, 7, value);

        assertSame(value, cache.get("key", owner, icon, config, 7));
    }

    @Test
    public void changedIdentityOrVisualInputsInvalidateEntry() {
        NotificationIconSnapshotCache<Object> cache = new NotificationIconSnapshotCache<>(64);
        Object owner = new Object();
        Object icon = new Object();
        Object value = new Object();

        cache.put("key", owner, icon, "config-a", 7, value);
        assertNull(cache.get("key", new Object(), icon, "config-a", 7));
        assertEquals(0, cache.size());

        cache.put("key", owner, icon, "config-a", 7, value);
        assertNull(cache.get("key", owner, new Object(), "config-a", 7));

        cache.put("key", owner, icon, "config-a", 7, value);
        assertNull(cache.get("key", owner, icon, "config-b", 7));

        cache.put("key", owner, icon, "config-a", 7, value);
        assertNull(cache.get("key", owner, icon, "config-a", 8));
    }

    @Test
    public void cacheIsBoundedAndEvictsLeastRecentlyUsedEntry() {
        NotificationIconSnapshotCache<Integer> cache = new NotificationIconSnapshotCache<>(2);
        Object ownerA = new Object();
        Object ownerB = new Object();
        Object ownerC = new Object();
        Object iconA = new Object();
        Object iconB = new Object();
        Object iconC = new Object();

        cache.put("a", ownerA, iconA, "config", 1, 1);
        cache.put("b", ownerB, iconB, "config", 1, 2);
        assertEquals(Integer.valueOf(1), cache.get("a", ownerA, iconA, "config", 1));
        cache.put("c", ownerC, iconC, "config", 1, 3);

        assertEquals(2, cache.size());
        assertNull(cache.get("b", ownerB, iconB, "config", 1));
        assertEquals(Integer.valueOf(1), cache.get("a", ownerA, iconA, "config", 1));
        assertEquals(Integer.valueOf(3), cache.get("c", ownerC, iconC, "config", 1));
    }

    @Test
    public void removeAndClearReleaseEntries() {
        NotificationIconSnapshotCache<Object> cache = new NotificationIconSnapshotCache<>(64);
        Object owner = new Object();
        Object icon = new Object();

        cache.put("a", owner, icon, "config", 1, new Object());
        cache.put("b", owner, icon, "config", 1, new Object());
        cache.remove("a");
        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }
}
