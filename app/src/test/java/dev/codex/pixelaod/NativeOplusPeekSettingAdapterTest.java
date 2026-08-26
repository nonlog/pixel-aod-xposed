package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NativeOplusPeekSettingAdapterTest {
    @Test
    public void vendorPreferenceIsAuthoritativeWhenKnown() {
        assertTrue(NativeOplusPeekSettingAdapter.resolve(1, false));
        assertFalse(NativeOplusPeekSettingAdapter.resolve(0, true));
    }

    @Test
    public void verifiedVendorDefaultIsFallbackWhenReadUnavailable() {
        assertTrue(NativeOplusPeekSettingAdapter.resolve(null, true));
        assertFalse(NativeOplusPeekSettingAdapter.resolve(null, false));
    }

    @Test
    public void currentOplusLegacyDefaultIsEnabled() {
        assertTrue(NativeOplusPeekSettingAdapter.OPLUS_NEW_NOTIFICATION_DEFAULT != 0);
    }
}
