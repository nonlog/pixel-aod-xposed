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

    @Test
    public void identifiesTheCurrentOplusDndNoticeForNativeGlyphMapping() {
        assertTrue(SystemNotificationIconPolicy.isOplusDndNotice(
                "com.android.systemui", "channel_dnd_notice", 10001));
        assertTrue(SystemNotificationIconPolicy.isOplusDndNotice(
                "com.android.systemui", "other", 10001));
        assertFalse(SystemNotificationIconPolicy.isOplusDndNotice(
                "android", "channel_dnd_notice", 10001));
        assertFalse(SystemNotificationIconPolicy.isOplusDndNotice(
                "com.android.systemui", "other", 42));
    }
}
