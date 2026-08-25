package dev.codex.pixelaod;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class NativeAodNotificationOrderAdapterTest {
    @Test
    public void systemRankingOrderWinsForEligibleKeys() {
        List<String> ordered = NativeAodNotificationOrderAdapter.orderKeys(
                Arrays.asList("a", "b", "c"), new String[]{"c", "a", "b"});
        assertEquals(Arrays.asList("c", "a", "b"), ordered);
    }

    @Test
    public void missingRankingKeysKeepStableInputOrderAfterRankedItems() {
        List<String> ordered = NativeAodNotificationOrderAdapter.orderKeys(
                Arrays.asList("a", "local1", "b", "local2"), new String[]{"b", "a"});
        assertEquals(Arrays.asList("b", "a", "local1", "local2"), ordered);
    }

    @Test
    public void duplicateAndUnknownSystemKeysDoNotDuplicateEligibleItems() {
        List<String> ordered = NativeAodNotificationOrderAdapter.orderKeys(
                Arrays.asList("a", "b", "a"), new String[]{"missing", "b", "b", "a"});
        assertEquals(Arrays.asList("b", "a"), ordered);
    }
}