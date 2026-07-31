package dev.codex.pixelaod;

final class SystemNotificationIconPolicy {
    static final String OPLUS_OTA_PACKAGE = "com.oplus.ota";

    private SystemNotificationIconPolicy() {
    }

    static boolean usesBundledSystemUpdateIcon(String packageName) {
        return OPLUS_OTA_PACKAGE.equals(packageName);
    }
}
