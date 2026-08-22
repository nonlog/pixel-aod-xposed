package dev.codex.pixelaod;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Read-only selected-user/native AOD availability and enablement adapter. */
final class NativeAodAvailabilityAdapter {
    static final String OPLUS_AOD_AVAILABLE_SETTING = "Setting_AodSwitchEnable";
    static final String OPLUS_AOD_ENABLED_SETTING = "Setting_AodEnable";

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
                "runtime");
    }

    static Decision evaluate(Integer vendorAvailable, Integer vendorEnabled,
            Boolean frameworkAvailable, Boolean frameworkEnabled,
            boolean deviceProvisioned, boolean userSetupComplete,
            boolean vendorLifecycleValid, int userId, String source) {
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
            reason = "vendor-ambient-lifecycle-inactive";
        } else {
            reason = "native-aod-eligible";
        }
        return new Decision(nativeAvailable, nativeEnabled, provisioned, vendorLifecycleValid,
                configuredEligible, continuousEligible, userId, availabilitySource,
                enablementSource, reason,
                source != null ? source : "");
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
        final int userId;
        final String availabilitySource;
        final String enablementSource;
        final String reason;
        final String source;

        Decision(boolean nativeAvailable, boolean nativeEnabled, boolean provisioned,
                boolean vendorLifecycleValid, boolean configuredEligible,
                boolean continuousEligible, int userId, String availabilitySource,
                String enablementSource, String reason, String source) {
            this.nativeAvailable = nativeAvailable;
            this.nativeEnabled = nativeEnabled;
            this.provisioned = provisioned;
            this.vendorLifecycleValid = vendorLifecycleValid;
            this.configuredEligible = configuredEligible;
            this.continuousEligible = continuousEligible;
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
                    + ",user=" + userId
                    + ",availabilitySource=" + availabilitySource
                    + ",enablementSource=" + enablementSource
                    + ",reason=" + reason;
        }
    }
}
