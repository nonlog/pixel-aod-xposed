package dev.codex.pixelaod;

final class PowerSavingAodPolicy {
    static final String ENERGY_SAVING_MODE = "energy-saving";
    static final String NOTIFICATION_TRIGGER_TYPE = "power-saving-notification";
    private PowerSavingAodPolicy() {}
    static boolean isEnergySavingMode(String mode) { return ENERGY_SAVING_MODE.equals(mode); }
    static boolean isChargingHoldRequested(boolean enabled, String mode, boolean configured, boolean plugged) {
        return enabled && isEnergySavingMode(mode) && configured && plugged;
    }
    static boolean isNotificationEnhancementConfigured(boolean enabled, String mode,
            boolean configured, boolean nativePeekEnabled) {
        return enabled && isEnergySavingMode(mode) && configured && nativePeekEnabled;
    }
    static boolean shouldKeepChargingVisible(boolean requested, boolean interactive,
            boolean proximityBlocked, boolean baseSuppressed, boolean powerAllows) {
        return requested && !interactive && !proximityBlocked && !baseSuppressed && powerAllows;
    }

    static boolean hasVendorTransientPresentationWindow(
            boolean notificationWindowAttached, boolean displayAod) {
        return notificationWindowAttached || displayAod;
    }
    static boolean shouldReapplyNativeHide(boolean previous, boolean now, String mode,
            boolean configured, boolean interactive) {
        return previous && !now && !interactive && configured && isEnergySavingMode(mode);
    }
}
