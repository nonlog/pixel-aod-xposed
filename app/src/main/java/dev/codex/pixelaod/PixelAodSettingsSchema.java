package dev.codex.pixelaod;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PixelAodSettingsSchema {
    public static final String PREFS = "pixel_aod_settings";

    public static final String KEY_MODULE_ENABLED = "module_enabled";
    public static final String KEY_CUSTOM_AOD = "custom_aod";
    public static final String KEY_LOCKSCREEN_CLOCK = "lockscreen_clock";
    public static final String KEY_AOD_DISPLAY_MODE = "module_aod_display_mode";
    public static final String KEY_WEATHER = "weather";
    public static final String KEY_WEATHER_ALERTS = "weather_alerts";
    public static final String KEY_WEATHER_FORECAST = "weather_forecast";
    public static final String KEY_WEATHER_FORECAST_START_TIME = "weather_forecast_start_time";
    public static final String KEY_WEATHER_FORECAST_END_TIME = "weather_forecast_end_time";
    public static final String KEY_WEATHER_ICON_PACK = "weather_icon_pack";
    public static final String KEY_CALENDAR_EVENTS = "calendar_events";
    public static final String KEY_CALENDAR_ICON_PACKAGE = "calendar_icon_package";
    public static final String KEY_NOTIFICATION_ICONS = "notification_icons";
    public static final String KEY_PIXEL_FINGERPRINT_ICON = "pixel_fingerprint_icon";
    public static final String KEY_UDFPS_HDR_PRESS_EFFECT = "udfps_hdr_press_effect";
    public static final String KEY_UDFPS_SUCCESS_RIPPLE = "udfps_success_ripple";
    public static final String KEY_UDFPS_AOD_EXIT_ANIMATION = "udfps_aod_exit_animation";
    /** Startup-only rollback selector for the independent UDFPS renderer. */
    public static final String KEY_UDFPS_RENDERER = "udfps_renderer";
    public static final String UDFPS_RENDERER_COUI_PORT = "coui_port";
    public static final String UDFPS_RENDERER_LEGACY = "legacy";
    public static final String KEY_LOCKSCREEN_NOTIFICATION_POLICY = "lockscreen_notification_policy";
    public static final String KEY_DEBUG_LOGGING = "debug_logging";
    public static final String KEY_AOD_WEIGHT = "aod_weight";
    public static final String KEY_LOCKSCREEN_WEIGHT = "lockscreen_weight";
    public static final String KEY_FORCE_ENGLISH_DATE = "force_english_date";
    public static final String KEY_DISABLE_BURN_IN_OFFSET = "disable_burn_in_offset";
    public static final String KEY_POCKET_MODE = "pocket_mode";
    public static final String KEY_AOD_SCHEDULE_ENABLED = "aod_schedule_enabled";
    public static final String KEY_AOD_SCHEDULE_START_TIME = "aod_schedule_start_time";
    public static final String KEY_AOD_SCHEDULE_END_TIME = "aod_schedule_end_time";
    public static final String KEY_LANGUAGE = "ui_language";

    public static final String AOD_DISPLAY_MODE_CONTINUOUS = "continuous";
    public static final String AOD_DISPLAY_MODE_TRIGGER_ONLY = "trigger_only";
    public static final String LANGUAGE_SYSTEM = "system";
    public static final String LANGUAGE_CHINESE = "zh";
    public static final String LANGUAGE_ENGLISH = "en";

    public static final float DEFAULT_AOD_WEIGHT = 280f;
    public static final float DEFAULT_LOCKSCREEN_WEIGHT = 520f;

    private static final LinkedHashMap<String, SettingSpec> SPECS = new LinkedHashMap<>();

    public static final SettingSpec MODULE_ENABLED = booleanSpec(KEY_MODULE_ENABLED, true, true);
    public static final SettingSpec CUSTOM_AOD = booleanSpec(KEY_CUSTOM_AOD, true, true);
    public static final SettingSpec LOCKSCREEN_CLOCK = booleanSpec(KEY_LOCKSCREEN_CLOCK, true, true);
    public static final SettingSpec AOD_DISPLAY_MODE =
            stringSpec(KEY_AOD_DISPLAY_MODE, AOD_DISPLAY_MODE_CONTINUOUS, false);
    public static final SettingSpec WEATHER = booleanSpec(KEY_WEATHER, true, false);
    public static final SettingSpec WEATHER_ALERTS = booleanSpec(KEY_WEATHER_ALERTS, false, false);
    public static final SettingSpec WEATHER_FORECAST =
            booleanSpec(KEY_WEATHER_FORECAST, false, false);
    public static final SettingSpec WEATHER_FORECAST_START_TIME = stringSpec(
            KEY_WEATHER_FORECAST_START_TIME, ForecastDisplayWindow.DEFAULT_START_TIME, false);
    public static final SettingSpec WEATHER_FORECAST_END_TIME = stringSpec(
            KEY_WEATHER_FORECAST_END_TIME, ForecastDisplayWindow.DEFAULT_END_TIME, false);
    public static final SettingSpec WEATHER_ICON_PACK = stringSpec(KEY_WEATHER_ICON_PACK, "", false);
    public static final SettingSpec CALENDAR_EVENTS = booleanSpec(KEY_CALENDAR_EVENTS, false, false);
    public static final SettingSpec CALENDAR_ICON_PACKAGE =
            stringSpec(KEY_CALENDAR_ICON_PACKAGE, "", false);
    public static final SettingSpec NOTIFICATION_ICONS =
            alwaysEnabledBooleanSpec(KEY_NOTIFICATION_ICONS);
    public static final SettingSpec PIXEL_FINGERPRINT_ICON =
            booleanSpec(KEY_PIXEL_FINGERPRINT_ICON, false, false);
    public static final SettingSpec UDFPS_HDR_PRESS_EFFECT =
            booleanSpec(KEY_UDFPS_HDR_PRESS_EFFECT, true, false);
    public static final SettingSpec UDFPS_SUCCESS_RIPPLE =
            booleanSpec(KEY_UDFPS_SUCCESS_RIPPLE, true, false);
    public static final SettingSpec UDFPS_AOD_EXIT_ANIMATION =
            booleanSpec(KEY_UDFPS_AOD_EXIT_ANIMATION, true, false);
    public static final SettingSpec UDFPS_RENDERER = stringSpec(
            KEY_UDFPS_RENDERER, UDFPS_RENDERER_COUI_PORT, true);
    public static final SettingSpec LOCKSCREEN_NOTIFICATION_POLICY =
            booleanSpec(KEY_LOCKSCREEN_NOTIFICATION_POLICY, true, false);
    public static final SettingSpec DEBUG_LOGGING = booleanSpec(KEY_DEBUG_LOGGING, false, false);
    public static final SettingSpec AOD_WEIGHT =
            floatSpec(KEY_AOD_WEIGHT, DEFAULT_AOD_WEIGHT, false);
    public static final SettingSpec LOCKSCREEN_WEIGHT =
            floatSpec(KEY_LOCKSCREEN_WEIGHT, DEFAULT_LOCKSCREEN_WEIGHT, false);
    public static final SettingSpec FORCE_ENGLISH_DATE =
            booleanSpec(KEY_FORCE_ENGLISH_DATE, false, false);
    public static final SettingSpec DISABLE_BURN_IN_OFFSET =
            booleanSpec(KEY_DISABLE_BURN_IN_OFFSET, false, false);
    public static final SettingSpec POCKET_MODE = alwaysEnabledBooleanSpec(KEY_POCKET_MODE);
    public static final SettingSpec AOD_SCHEDULE_ENABLED =
            booleanSpec(KEY_AOD_SCHEDULE_ENABLED, false, false);
    public static final SettingSpec AOD_SCHEDULE_START_TIME =
            stringSpec(KEY_AOD_SCHEDULE_START_TIME, "22:00", false);
    public static final SettingSpec AOD_SCHEDULE_END_TIME =
            stringSpec(KEY_AOD_SCHEDULE_END_TIME, "07:00", false);
    public static final SettingSpec LANGUAGE = stringSpec(KEY_LANGUAGE, LANGUAGE_SYSTEM, false);

    private PixelAodSettingsSchema() {
    }

    static SettingSpec spec(String key) {
        return SPECS.get(key);
    }

    static Collection<SettingSpec> allSpecs() {
        return Collections.unmodifiableCollection(SPECS.values());
    }

    static boolean booleanDefault(String key, boolean fallback) {
        SettingSpec spec = spec(key);
        return spec != null && spec.defaultValue instanceof Boolean
                ? (Boolean) spec.defaultValue
                : fallback;
    }

    static String stringDefault(String key, String fallback) {
        SettingSpec spec = spec(key);
        return spec != null && spec.defaultValue instanceof String
                ? (String) spec.defaultValue
                : fallback;
    }

    static float floatDefault(String key, float fallback) {
        SettingSpec spec = spec(key);
        return spec != null && spec.defaultValue instanceof Float
                ? (Float) spec.defaultValue
                : fallback;
    }

    static boolean isAlwaysEnabledKey(String key) {
        SettingSpec spec = spec(key);
        return spec != null && spec.alwaysEnabled;
    }

    static boolean requiresSystemUiRestart(String key) {
        SettingSpec spec = spec(key);
        return spec != null && spec.requiresSystemUiRestart;
    }

    static String valueToString(String key, Object rawValue) {
        SettingSpec spec = spec(key);
        if (spec != null && spec.alwaysEnabled) {
            return "true";
        }
        if (rawValue == null) {
            return spec != null ? String.valueOf(spec.defaultValue) : "";
        }
        return String.valueOf(rawValue);
    }

    private static SettingSpec booleanSpec(String key, boolean defaultValue,
            boolean requiresSystemUiRestart) {
        return register(new SettingSpec(key, Type.BOOLEAN, defaultValue,
                requiresSystemUiRestart, false));
    }

    private static SettingSpec alwaysEnabledBooleanSpec(String key) {
        return register(new SettingSpec(key, Type.BOOLEAN, true, false, true));
    }

    private static SettingSpec stringSpec(String key, String defaultValue,
            boolean requiresSystemUiRestart) {
        return register(new SettingSpec(key, Type.STRING,
                defaultValue != null ? defaultValue : "", requiresSystemUiRestart, false));
    }

    private static SettingSpec floatSpec(String key, float defaultValue,
            boolean requiresSystemUiRestart) {
        return register(new SettingSpec(key, Type.FLOAT, defaultValue,
                requiresSystemUiRestart, false));
    }

    private static SettingSpec register(SettingSpec spec) {
        if (spec == null || spec.key == null || spec.key.isEmpty()) {
            return spec;
        }
        SPECS.put(spec.key, spec);
        return spec;
    }

    enum Type {
        BOOLEAN,
        STRING,
        FLOAT
    }

    static final class SettingSpec {
        final String key;
        final Type type;
        final Object defaultValue;
        final boolean requiresSystemUiRestart;
        final boolean alwaysEnabled;

        SettingSpec(String key, Type type, Object defaultValue,
                boolean requiresSystemUiRestart, boolean alwaysEnabled) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.requiresSystemUiRestart = requiresSystemUiRestart;
            this.alwaysEnabled = alwaysEnabled;
        }

        boolean defaultBoolean() {
            return defaultValue instanceof Boolean && (Boolean) defaultValue;
        }

        String defaultString() {
            return defaultValue instanceof String ? (String) defaultValue : "";
        }

        float defaultFloat() {
            return defaultValue instanceof Float ? (Float) defaultValue : 0f;
        }
    }
}
