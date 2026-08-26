package dev.codex.pixelaod;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import java.lang.reflect.Method;

/** Reads the selected-user OPlus "Show new notifications on AOD" preference. */
final class NativeOplusPeekSettingAdapter {
    // CPH2573/OOS 16.0.9 uses the legacy AOD preference branch (AOD APK P2.c#a() == false).
    static final String OPLUS_NEW_NOTIFICATION_SETTING =
            "oplus_customize_aod_curved_display_notification_switch";
    // Current AOD APK: integer/oplus_curved_display_notification_switch_default = 1.
    static final int OPLUS_NEW_NOTIFICATION_DEFAULT = 1;

    private NativeOplusPeekSettingAdapter() {
    }

    static boolean isEnabled(Context context) {
        if (context == null) {
            return false;
        }
        Integer value = secureIntForUser(context.getContentResolver(),
                OPLUS_NEW_NOTIFICATION_SETTING,
                SelectedUserScope.resolveSelectedUserId(),
                OPLUS_NEW_NOTIFICATION_DEFAULT);
        return resolve(value, OPLUS_NEW_NOTIFICATION_DEFAULT != 0);
    }

    static boolean resolve(Integer vendorValue, boolean fallback) {
        return vendorValue != null ? vendorValue != 0 : fallback;
    }

    private static Integer secureIntForUser(ContentResolver resolver, String key, int userId,
            Integer defaultValue) {
        if (resolver == null || key == null) {
            return defaultValue;
        }
        try {
            Method method = Settings.Secure.class.getDeclaredMethod("getIntForUser",
                    ContentResolver.class, String.class, int.class, int.class);
            method.setAccessible(true);
            Object value = method.invoke(null, resolver, key, defaultValue, userId);
            return value instanceof Integer ? (Integer) value : defaultValue;
        } catch (Throwable ignored) {
        }
        try {
            return Settings.Secure.getInt(resolver, key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }
}
