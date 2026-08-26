package dev.codex.pixelaod;

final class SystemNotificationIconPolicy {
    static final String OPLUS_OTA_PACKAGE = "com.oplus.ota";
    static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    static final String OPLUS_DND_CHANNEL = "channel_dnd_notice";
    static final int OPLUS_DND_NOTIFICATION_ID = 10001;

    private SystemNotificationIconPolicy() {
    }

    static boolean usesBundledSystemUpdateIcon(String packageName) {
        return OPLUS_OTA_PACKAGE.equals(packageName);
    }

    static boolean isOplusDndNotice(String packageName, String channelId, int notificationId) {
        return SYSTEMUI_PACKAGE.equals(packageName)
                && (OPLUS_DND_CHANNEL.equals(channelId)
                        || notificationId == OPLUS_DND_NOTIFICATION_ID);
    }
}
