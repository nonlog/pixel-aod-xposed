package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import android.os.BatteryManager;

import org.junit.Test;

public class CouiBatteryStatusPolicyTest {
    @Test
    public void connectedChargingShowsCharging() {
        assertEquals(CouiBatteryStatusPolicy.State.CHARGING,
                CouiBatteryStatusPolicy.resolve(48, BatteryManager.BATTERY_PLUGGED_USB,
                        BatteryManager.BATTERY_STATUS_CHARGING));
        assertEquals(" · Charging",
                CouiBatteryStatusPolicy.suffix(CouiBatteryStatusPolicy.State.CHARGING));
    }

    @Test
    public void connectedFullShowsCharged() {
        assertEquals(CouiBatteryStatusPolicy.State.CHARGED,
                CouiBatteryStatusPolicy.resolve(100, BatteryManager.BATTERY_PLUGGED_AC,
                        BatteryManager.BATTERY_STATUS_FULL));
        assertEquals(" · Charged",
                CouiBatteryStatusPolicy.suffix(CouiBatteryStatusPolicy.State.CHARGED));
    }

    @Test
    public void connectedHundredPercentNotChargingFallsBackToCharged() {
        assertEquals(CouiBatteryStatusPolicy.State.CHARGED,
                CouiBatteryStatusPolicy.resolve(100, BatteryManager.BATTERY_PLUGGED_USB,
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING));
    }

    @Test
    public void unpluggedFullDoesNotClaimCharged() {
        assertEquals(CouiBatteryStatusPolicy.State.NONE,
                CouiBatteryStatusPolicy.resolve(100, 0, BatteryManager.BATTERY_STATUS_FULL));
    }

    @Test
    public void connectedUnknownOrDischargingHasNoSuffix() {
        assertEquals(CouiBatteryStatusPolicy.State.NONE,
                CouiBatteryStatusPolicy.resolve(80, BatteryManager.BATTERY_PLUGGED_USB,
                        BatteryManager.BATTERY_STATUS_UNKNOWN));
        assertEquals(CouiBatteryStatusPolicy.State.NONE,
                CouiBatteryStatusPolicy.resolve(80, BatteryManager.BATTERY_PLUGGED_USB,
                        BatteryManager.BATTERY_STATUS_DISCHARGING));
        assertEquals("", CouiBatteryStatusPolicy.suffix(CouiBatteryStatusPolicy.State.NONE));
    }
}
