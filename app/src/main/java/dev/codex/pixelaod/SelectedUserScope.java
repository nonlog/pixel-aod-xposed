package dev.codex.pixelaod;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;

import java.lang.reflect.Method;

/** Selected Android user boundary shared by settings and native AOD adapters. */
final class SelectedUserScope {
    private SelectedUserScope() {
    }

    static int resolveSelectedUserId() {
        try {
            Method method = ActivityManager.class.getDeclaredMethod("getCurrentUser");
            method.setAccessible(true);
            Object value = method.invoke(null);
            if (value instanceof Integer) {
                return Math.max(0, (Integer) value);
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> userHandleClass = Class.forName("android.os.UserHandle");
            Method method = userHandleClass.getDeclaredMethod("myUserId");
            method.setAccessible(true);
            Object value = method.invoke(null);
            if (value instanceof Integer) {
                return Math.max(0, (Integer) value);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    static Uri settingsUriForUser(Context context, Uri baseUri, int userId) {
        if (baseUri == null || userId < 0 || !isSystemUiContext(context)) {
            return baseUri;
        }
        String authority = baseUri.getAuthority();
        if (authority == null || authority.isEmpty()) {
            return baseUri;
        }
        return baseUri.buildUpon().authority(scopedAuthority(authority, userId)).build();
    }

    static String scopedAuthority(String authority, int userId) {
        String base = authority != null ? authority : "";
        int separator = base.indexOf('@');
        if (separator >= 0 && separator + 1 < base.length()) {
            base = base.substring(separator + 1);
        }
        return Math.max(0, userId) + "@" + base;
    }

    static boolean isSystemUiContext(Context context) {
        return context != null && "com.android.systemui".equals(context.getPackageName());
    }
}
