package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SystemNotificationIconPolicyTest {
    @Test
    public void mapsOnlyTheConfirmedOplusOtaPackageToTheSystemUpdateGlyph() {
        assertTrue(SystemNotificationIconPolicy.usesBundledSystemUpdateIcon("com.oplus.ota"));
        assertFalse(SystemNotificationIconPolicy.usesBundledSystemUpdateIcon("com.android.systemui"));
        assertFalse(SystemNotificationIconPolicy.usesBundledSystemUpdateIcon("com.example.update"));
    }
}
