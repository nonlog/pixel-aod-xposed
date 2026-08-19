package dev.codex.pixelaod;

import android.os.BatteryManager;

/** Maps Android/OPlus battery broadcasts to the AOD battery status label. */
final class CouiBatteryStatusPolicy {
    enum State {
        NONE,
        CHARGING,
        CHARGED
    }

    private CouiBatteryStatusPolicy() {
    }

    static State resolve(int levelPercent, int plugged, int status) {
        boolean connected = plugged != 0;
        if (!connected) {
            return State.NONE;
        }
        if (status == BatteryManager.BATTERY_STATUS_FULL
                || (levelPercent >= 100 && status == BatteryManager.BATTERY_STATUS_NOT_CHARGING)) {
            return State.CHARGED;
        }
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return State.CHARGING;
        }
        return State.NONE;
    }

    static String suffix(State state) {
        if (state == State.CHARGING) {
            return " · Charging";
        }
        if (state == State.CHARGED) {
            return " · Charged";
        }
        return "";
    }
}
