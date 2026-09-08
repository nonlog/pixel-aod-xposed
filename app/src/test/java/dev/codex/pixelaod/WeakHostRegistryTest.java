package dev.codex.pixelaod;

import org.junit.Test;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import static org.junit.Assert.*;

public class WeakHostRegistryTest {
    private static final class Record {
        final Object root;
        Record(Object root) { this.root = root; }
    }

    @Test public void liveOwnerCanKeepARecordThatReferencesItsKey() {
        WeakHostRegistry<Object, Record> registry = new WeakHostRegistry<>();
        Object root = new Object();
        Record owner = new Record(root);
        registry.put(root, owner);
        assertSame(owner, registry.get(root));
        assertSame(owner, registry.snapshot().get(0));
    }

    @Test public void replacementAndRemovalDoNotLeaveOldRecords() {
        WeakHostRegistry<Object, Record> registry = new WeakHostRegistry<>();
        Object root = new Object();
        Record first = new Record(root), second = new Record(root);
        registry.put(root, first);
        registry.put(root, second);
        assertSame(second, registry.get(root));
        assertEquals(1, registry.snapshot().size());
        registry.remove(root);
        assertNull(registry.get(root));
        assertTrue(registry.snapshot().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, WeakReference<Record>> entries(
            WeakHostRegistry<Object, Record> registry) throws Exception {
        Field field = WeakHostRegistry.class.getDeclaredField("entries");
        field.setAccessible(true);
        Object entries = field.get(registry);
        assertTrue("keys must also be weak", entries instanceof WeakHashMap);
        return (Map<Object, WeakReference<Record>>) entries;
    }

    @Test public void collectedValueIsPrunedOnLookupWithoutRelyingOnGcTiming() throws Exception {
        WeakHostRegistry<Object, Record> registry = new WeakHostRegistry<>();
        Object root = new Object();
        Record record = new Record(root);
        registry.put(root, record);
        entries(registry).get(root).clear();
        assertNull(registry.get(root));
        assertTrue(entries(registry).isEmpty());
    }

    @Test public void snapshotPrunesCollectedValuesAndKeepsLiveOwner() throws Exception {
        WeakHostRegistry<Object, Record> registry = new WeakHostRegistry<>();
        Object firstRoot = new Object(), secondRoot = new Object();
        Record first = new Record(firstRoot), second = new Record(secondRoot);
        registry.put(firstRoot, first);
        registry.put(secondRoot, second);
        entries(registry).get(firstRoot).clear();
        assertEquals(1, registry.snapshot().size());
        assertSame(second, registry.snapshot().get(0));
        assertEquals(1, entries(registry).size());
    }
}
