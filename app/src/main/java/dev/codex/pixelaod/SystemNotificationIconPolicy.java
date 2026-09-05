package dev.codex.pixelaod;

import java.util.Locale;

final class SystemNotificationIconPolicy {
    static final String OPLUS_OTA_PACKAGE = "com.oplus.ota";
    static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    static final String PHONE_SERVICES_PACKAGE = "com.android.phone";
    static final String OPLUS_DND_CHANNEL = "channel_dnd_notice";
    static final int OPLUS_DND_NOTIFICATION_ID = 10001;
    static final float OPLUS_DND_VISUAL_SCALE = 1.22f;

    private SystemNotificationIconPolicy() {
    }

    static boolean usesBundledSystemUpdateIcon(String packageName) {
        return OPLUS_OTA_PACKAGE.equals(packageName);
    }

    static boolean isPhoneServicesPackage(String packageName) {
        return PHONE_SERVICES_PACKAGE.equals(packageName);
    }

    static boolean isPhoneServicesNoSim(String packageName, String notificationText,
            String resourceName) {
        if (!isPhoneServicesPackage(packageName)) {
            return false;
        }
        String text = normalize(notificationText);
        String resource = normalize(resourceName).replace('-', '_');
        if (resource.contains("no_sim")
                || resource.contains("nosim")
                || resource.contains("sim_off")
                || resource.contains("sim_disabled")
                || resource.contains("sim_missing")) {
            return true;
        }
        if (text.contains("no sim") || text.contains("no sim card")) {
            return true;
        }
        return text.contains("sim card")
                && (text.contains("not installed")
                        || text.contains("not detected")
                        || text.contains("missing")
                        || text.contains("insert"));
    }

    static boolean isOplusDndNotice(String packageName, String channelId, int notificationId) {
        return SYSTEMUI_PACKAGE.equals(packageName)
                && (OPLUS_DND_CHANNEL.equals(channelId)
                        || notificationId == OPLUS_DND_NOTIFICATION_ID);
    }

    static float visualScaleFor(String packageName, String channelId, int notificationId) {
        return isOplusDndNotice(packageName, channelId, notificationId)
                ? OPLUS_DND_VISUAL_SCALE : 1f;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}
