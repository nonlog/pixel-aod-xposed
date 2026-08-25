package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class NativeNotificationIconPresentationPolicyTest {
    @Test
    public void nullContextUsesAndroidAodFallbackResources() {
        NativeNotificationIconPresentationPolicy.Snapshot snapshot =
                NativeNotificationIconPresentationPolicy.resolve(null, true);
        assertEquals(3, snapshot.maxVisibleIcons);
        assertEquals(4, snapshot.dotDiameterPx());
        assertEquals(3, snapshot.dotPaddingPx);
    }
}