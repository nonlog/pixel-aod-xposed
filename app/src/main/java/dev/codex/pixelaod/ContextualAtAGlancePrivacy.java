package dev.codex.pixelaod;

import android.app.KeyguardManager;
import android.content.Context;
import android.provider.Settings;

/** Uses existing keyguard secure settings for alert headline redaction. */
final class ContextualAtAGlancePrivacy {
    private ContextualAtAGlancePrivacy() {
    }

    static boolean isSensitiveContentHidden(Context context) {
        if (context == null) {
            return false;
        }
        try {
            KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
            if (keyguard == null || !keyguard.isKeyguardLocked()) {
                return false;
            }
            int allowPrivate = Settings.Secure.getInt(context.getContentResolver(),
                    "lock_screen_allow_private_notifications", 1);
            int showNotifications = Settings.Secure.getInt(context.getContentResolver(),
                    "lock_screen_show_notifications", 1);
            return allowPrivate == 0 || showNotifications == 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static String alertText(BreezyWeatherAlert alert, boolean sensitiveContentHidden,
            String genericLabel) {
        if (alert == null || alert.isEmpty()) {
            return "";
        }
        if (sensitiveContentHidden) {
            return genericLabel == null || genericLabel.trim().isEmpty()
                    ? "Weather alert" : genericLabel.trim();
        }
        return alert.headline;
    }
}
