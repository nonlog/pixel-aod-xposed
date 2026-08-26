package dev.codex.pixelaod;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Calendar;

/** Read-only selected-user/native AOD availability and enablement adapter. */
final class NativeAodAvailabilityAdapter {
    static final String OPLUS_AOD_AVAILABLE_SETTING = "Setting_AodSwitchEnable";
    static final String OPLUS_AOD_ENABLED_SETTING = "Setting_AodEnable";
    static final String OPLUS_AOD_ALWAYS_DISPLAY_SETTING = "Setting_AodEnableImmediate";
    static final String OPLUS_AOD_TIMING_SETTING = "Setting_AodUserSetTime";
    static final String OPLUS_AOD_ENERGY_SAVING_SETTING = "Setting_AodUserEnergySavingSet";
    static final String OPLUS_AOD_START_HOUR_SETTING = "Setting_AodSetTimeBeginHour";
    static final String OPLUS_AOD_START_MINUTE_SETTING = "Setting_AodSetTimeBeginMin";
    static final String OPLUS_AOD_END_HOUR_SETTING = "Setting_AodSetTimeEndHour";
    static final String OPLUS_AOD_END_MINUTE_SETTING = "Setting_AodSetTimeEndMin";

    private NativeAodAvailabilityAdapter() {
    }

    static Decision read(Context context, boolean vendorLifecycleValid) {
        if (context == null) {
            return evaluate(null, null, null, null, false, false, vendorLifecycleValid,
                    -1, "no-context");
        }
        int userId = selectedUserId();
        Integer vendorAvailable = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_AVAILABLE_SETTING, userId);
        Integer vendorEnabled = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_ENABLED_SETTING, userId);
        Integer alwaysDisplay = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_ALWAYS_DISPLAY_SETTING, userId);
        Integer timingSet = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_TIMING_SETTING, userId);
        Integer energySavingSet = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_ENERGY_SAVING_SETTING, userId);
        int startHour = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_START_HOUR_SETTING, userId, 7);
        int startMinute = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_START_MINUTE_SETTING, userId, 0);
        int endHour = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_END_HOUR_SETTING, userId, 23);
        int endMinute = secureIntForUser(context.getContentResolver(),
                OPLUS_AOD_END_MINUTE_SETTING, userId, 0);
        Boolean frameworkAvailable = ambientDisplayBoolean(context,
                "alwaysOnAvailableForUser", userId);
        if (frameworkAvailable == null) {
            frameworkAvailable = ambientDisplayBoolean(context, "alwaysOnAvailable", null);
        }
        Boolean frameworkEnabled = ambientDisplayBoolean(context, "alwaysOnEnabled", userId);
        boolean deviceProvisioned = globalInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1) != 0;
        boolean userSetupComplete = secureIntForUser(context.getContentResolver(),
                "user_setup_complete", userId, 1) != 0;
        return evaluate(vendorAvailable, vendorEnabled, frameworkAvailable, frameworkEnabled,
                deviceProvisioned, userSetupComplete, vendorLifecycleValid, userId,
                alwaysDisplay, timingSet, energySavingSet,
                startHour, startMinute, endHour, endMinute, currentMinuteOfDay(), "runtime");
    }

    static Decision evaluate(Integer vendorAvailable, Integer vendorEnabled,
            Boolean frameworkAvailable, Boolean frameworkEnabled,
            boolean deviceProvisioned, boolean userSetupComplete,
            boolean vendorLifecycleValid, int userId, String source) {
        return evaluate(vendorAvailable, vendorEnabled, frameworkAvailable, frameworkEnabled,
                deviceProvisioned, userSetupComplete, vendorLifecycleValid, userId,
                null, null, null, 0, 0, 0, 0, -1, source);
    }

    static Decision evaluate(Integer vendorAvailable, Integer vendorEnabled,
            Boolean frameworkAvailable, Boolean frameworkEnabled,
            boolean deviceProvisioned, boolean userSetupComplete,
            boolean vendorLifecycleValid, int userId,
            Integer alwaysDisplay, Integer timingSet, Integer energySavingSet,
            int startHour, int startMinute, int endHour, int endMinute, int minuteOfDay,
            String source) {
        Boolean available = vendorAvailable != null
                ? Boolean.valueOf(vendorAvailable != 0) : frameworkAvailable;
        Boolean enabled = vendorEnabled != null
                ? Boolean.valueOf(vendorEnabled != 0) : frameworkEnabled;
        String availabilitySource = vendorAvailable != null
                ? "oplus-secure:" + OPLUS_AOD_AVAILABLE_SETTING
                : (frameworkAvailable != null ? "framework-ambient" : "vendor-lifecycle-fallback");
        String enablementSource = vendorEnabled != null
                ? "oplus-secure:" + OPLUS_AOD_ENABLED_SETTING
                : (frameworkEnabled != null ? "framework-ambient" : "vendor-lifecycle-fallback");

        // If no stable configuration seam exists, an already-valid native ambient lifecycle is
        // conservative positive evidence for this session. It never creates or extends Doze.
        if (available == null && vendorLifecycleValid) {
            available = true;
        }
        if (enabled == null && vendorLifecycleValid) {
            enabled = true;
        }

        boolean nativeAvailable = Boolean.TRUE.equals(available);
        boolean nativeEnabled = Boolean.TRUE.equals(enabled);
        boolean provisioned = deviceProvisioned && userSetupComplete;
        boolean configuredEligible = nativeAvailable && nativeEnabled && provisioned;
        boolean continuousEligible = configuredEligible && vendorLifecycleValid;
        boolean modeSettingsKnown = alwaysDisplay != null || timingSet != null || energySavingSet != null;
        boolean scheduleWindowEligible = true;
        boolean prearmEligible = configuredEligible;
        String displayMode = "native-unspecified";
        if (configuredEligible && modeSettingsKnown) {
            if (energySavingSet != null && energySavingSet != 0) {
                displayMode = "energy-saving";
                prearmEligible = false;
            } else if (alwaysDisplay != null && alwaysDisplay == 1) {
                displayMode = "all-day";
                prearmEligible = true;
            } else if (timingSet != null && timingSet == 1) {
                displayMode = "scheduled";
                scheduleWindowEligible = isWithinSchedule(
                        startHour, startMinute, endHour, endMinute, minuteOfDay);
                prearmEligible = scheduleWindowEligible;
            } else {
                // OPlus treats an enabled AOD with none of the explicit continuous modes selected
                // as energy-saving behavior. Do not pre-arm a continuous Pixel surface here; wait
                // for the vendor ambient lifecycle to become real.
                displayMode = "energy-saving";
                prearmEligible = false;
            }
        }
        String reason;
        if (!deviceProvisioned) {
            reason = "device-not-provisioned";
        } else if (!userSetupComplete) {
            reason = "selected-user-not-setup";
        } else if (!nativeAvailable) {
            reason = "native-aod-unavailable";
        } else if (!nativeEnabled) {
            reason = "native-aod-disabled";
        } else if (!vendorLifecycleValid) {
            if ("scheduled".equals(displayMode) && !scheduleWindowEligible) {
                reason = "native-aod-schedule-inactive";
            } else if ("energy-saving".equals(displayMode)) {
                reason = "native-aod-energy-saving-idle";
            } else {
                reason = "vendor-ambient-lifecycle-inactive";
            }
        } else {
            reason = "native-aod-eligible";
        }
        return new Decision(nativeAvailable, nativeEnabled, provisioned, vendorLifecycleValid,
                configuredEligible, continuousEligible, prearmEligible, scheduleWindowEligible,
                displayMode, userId, availabilitySource, enablementSource, reason,
                source != null ? source : "");
    }

    static boolean isWithinSchedule(int startHour, int startMinute,
            int endHour, int endMinute, int minuteOfDay) {
        if (minuteOfDay < 0) {
            return false;
        }
        int start = clampHour(startHour) * 60 + clampMinute(startMinute);
        int end = clampHour(endHour) * 60 + clampMinute(endMinute);
        if (start == end) {
            return true;
        }
        if (start < end) {
            return minuteOfDay >= start && minuteOfDay < end;
        }
        return minuteOfDay >= start || minuteOfDay < end;
    }

    private static int clampHour(int value) {
        return Math.max(0, Math.min(23, value));
    }

    private static int clampMinute(int value) {
        return Math.max(0, Math.min(59, value));
    }

    private static int currentMinuteOfDay() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }

    private static int selectedUserId() {
        return SelectedUserScope.resolveSelectedUserId();
    }

    private static Integer secureIntForUser(ContentResolver resolver, String key, int userId) {
        return secureIntForUser(resolver, key, userId, null);
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
            int sentinel = Integer.MIN_VALUE;
            Object value = method.invoke(null, resolver, key, sentinel, userId);
            if (value instanceof Integer) {
                int parsed = (Integer) value;
                return parsed != sentinel ? parsed : defaultValue;
            }
        } catch (Throwable ignored) {
        }
        try {
            return Settings.Secure.getInt(resolver, key);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    private static int globalInt(ContentResolver resolver, String key, int defaultValue) {
        if (resolver == null || key == null) {
            return defaultValue;
        }
        try {
            return Settings.Global.getInt(resolver, key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    private static Boolean ambientDisplayBoolean(Context context, String methodName,
            Integer userId) {
        try {
            Class<?> clazz = Class.forName("android.hardware.display.AmbientDisplayConfiguration");
            Constructor<?> constructor = clazz.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(context);
            Method method;
            Object value;
            if (userId != null) {
                method = clazz.getDeclaredMethod(methodName, int.class);
                method.setAccessible(true);
                value = method.invoke(instance, userId);
            } else {
                method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                value = method.invoke(instance);
            }
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static final class Decision {
        final boolean nativeAvailable;
        final boolean nativeEnabled;
        final boolean provisioned;
        final boolean vendorLifecycleValid;
        final boolean configuredEligible;
        final boolean continuousEligible;
        final boolean prearmEligible;
        final boolean scheduleWindowEligible;
        final String displayMode;
        final int userId;
        final String availabilitySource;
        final String enablementSource;
        final String reason;
        final String source;

        Decision(boolean nativeAvailable, boolean nativeEnabled, boolean provisioned,
                boolean vendorLifecycleValid, boolean configuredEligible,
                boolean continuousEligible, boolean prearmEligible,
                boolean scheduleWindowEligible, String displayMode, int userId,
                String availabilitySource, String enablementSource, String reason, String source) {
            this.nativeAvailable = nativeAvailable;
            this.nativeEnabled = nativeEnabled;
            this.provisioned = provisioned;
            this.vendorLifecycleValid = vendorLifecycleValid;
            this.configuredEligible = configuredEligible;
            this.continuousEligible = continuousEligible;
            this.prearmEligible = prearmEligible;
            this.scheduleWindowEligible = scheduleWindowEligible;
            this.displayMode = displayMode;
            this.userId = userId;
            this.availabilitySource = availabilitySource;
            this.enablementSource = enablementSource;
            this.reason = reason;
            this.source = source;
        }

        String describe() {
            return "available=" + nativeAvailable
                    + ",enabled=" + nativeEnabled
                    + ",provisioned=" + provisioned
                    + ",vendorLifecycle=" + vendorLifecycleValid
                    + ",configuredEligible=" + configuredEligible
                    + ",continuousEligible=" + continuousEligible
                    + ",prearmEligible=" + prearmEligible
                    + ",scheduleWindowEligible=" + scheduleWindowEligible
                    + ",displayMode=" + displayMode
                    + ",user=" + userId
                    + ",availabilitySource=" + availabilitySource
                    + ",enablementSource=" + enablementSource
                    + ",reason=" + reason;
        }
    }
}
