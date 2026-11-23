@file:OptIn(ExperimentalMaterial3Api::class)
package dev.codex.pixelaod

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import java.util.Locale

private const val BREEZY_READ_PROVIDER_PERMISSION = "org.breezyweather.READ_PROVIDER"
private const val REQUEST_BREEZY_WEATHER_RELAY =
    "dev.codex.pixelaod.REQUEST_BREEZY_WEATHER_RELAY"

private enum class ClockDialTarget {
    FORECAST_START,
    FORECAST_END
}

private enum class SettingsPage {
    HOME,
    AOD,
    CLOCK,
    FINGERPRINT,
    AT_A_GLANCE,
    LOCKSCREEN,
    SYSTEM
}

class SettingsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (normalizeAlwaysOnSettings(this)) {
            PixelAodSettings.refresh(this)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val isSystemDark = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isSystemDark
        controller.isAppearanceLightNavigationBars = !isSystemDark
        setContent {
            PixelAodSettingsScreen(onLanguageChanged = { recreate() })
        }
    }

    companion object {
        fun applyLanguage(base: Context): Context {
            val lang = PixelAodSettings.getSharedPreferences(base)
                .getString(
                    PixelAodSettings.KEY_LANGUAGE,
                    PixelAodSettings.defaultString(
                        PixelAodSettings.KEY_LANGUAGE,
                        PixelAodSettings.LANGUAGE_SYSTEM
                    )
                )
                ?: PixelAodSettings.LANGUAGE_SYSTEM
            val locale = when (lang) {
                PixelAodSettings.LANGUAGE_CHINESE -> Locale.SIMPLIFIED_CHINESE
                PixelAodSettings.LANGUAGE_ENGLISH -> Locale.ENGLISH
                else -> return base
            }
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }
    }
}

private fun normalizeAlwaysOnSettings(context: Context): Boolean {
    val prefs = PixelAodSettings.getSharedPreferences(context)
    return PixelAodSettings.normalizeAlwaysEnabledPreferences(prefs)
}

private fun updateModuleBooleanSetting(context: Context, key: String, value: Boolean) {
    val values = ContentValues().apply {
        put("key", key)
        put("value", value)
    }
    val updated = try {
        context.contentResolver.update(PixelAodSettingsProvider.URI, values, null, null)
    } catch (_: Throwable) {
        0
    }
    if (updated > 0) {
        return
    }
    PixelAodSettings.getSharedPreferences(context).edit().putBoolean(key, value).apply()
    PixelAodSettings.refresh(context)
    context.contentResolver.notifyChange(PixelAodSettingsProvider.URI, null)
}

private fun requestBreezyWeatherRefresh(context: Context) {
    context.sendBroadcast(
        Intent(REQUEST_BREEZY_WEATHER_RELAY).setPackage(context.packageName)
    )
}

private fun updateModuleFloatSetting(context: Context, key: String, value: Float) {
    val values = ContentValues().apply {
        put("key", key)
        put("value", value)
    }
    val updated = try {
        context.contentResolver.update(PixelAodSettingsProvider.URI, values, null, null)
    } catch (_: Throwable) {
        0
    }
    if (updated > 0) {
        return
    }
    PixelAodSettings.getSharedPreferences(context).edit().putFloat(key, value).apply()
    PixelAodSettings.refresh(context)
    context.contentResolver.notifyChange(PixelAodSettingsProvider.URI, null)
}

private fun updateModuleStringSetting(context: Context, key: String, value: String) {
    val values = ContentValues().apply {
        put("key", key)
        put("value", value)
    }
    val updated = try {
        context.contentResolver.update(PixelAodSettingsProvider.URI, values, null, null)
    } catch (_: Throwable) {
        0
    }
    if (updated > 0) {
        return
    }
    PixelAodSettings.getSharedPreferences(context).edit().putString(key, value).apply()
    PixelAodSettings.refresh(context)
    context.contentResolver.notifyChange(PixelAodSettingsProvider.URI, null)
}

private fun android.content.SharedPreferences.schemaBoolean(key: String, fallback: Boolean): Boolean {
    return getBoolean(key, PixelAodSettings.defaultBoolean(key, fallback))
}

private fun android.content.SharedPreferences.schemaString(key: String, fallback: String): String {
    return getString(key, PixelAodSettings.defaultString(key, fallback))
        ?: PixelAodSettings.defaultString(key, fallback)
}

private fun android.content.SharedPreferences.schemaFloat(key: String, fallback: Float): Float {
    return getFloat(key, PixelAodSettings.defaultFloat(key, fallback))
}

@Composable
private fun PixelAodSettingsScreen(onLanguageChanged: () -> Unit) {
    PixelAodTheme {
        SettingsContent(onLanguageChanged = onLanguageChanged)
    }
}

@Composable
private fun SettingsContent(
    onLanguageChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PixelAodSettings.getSharedPreferences(context) }

    val moduleEnabled = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_MODULE_ENABLED, true))
    }
    val pixelNotificationPeek = remember {
        mutableStateOf(
            prefs.schemaBoolean(PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, true)
        )
    }
    val powerSavingNotificationAod = remember {
        mutableStateOf(
            prefs.schemaBoolean(PixelAodSettings.KEY_POWER_SAVING_NOTIFICATION_AOD, true)
        )
    }
    val powerSavingChargingAod = remember {
        mutableStateOf(
            prefs.schemaBoolean(PixelAodSettings.KEY_POWER_SAVING_CHARGING_AOD, true)
        )
    }
    val nonLockscreenAodTransition = remember {
        mutableStateOf(
            prefs.schemaString(
                PixelAodSettings.KEY_NON_LOCKSCREEN_AOD_TRANSITION,
                PixelAodSettings.NON_LOCKSCREEN_AOD_TRANSITION_ANIMATED
            )
        )
    }
    val weather = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_WEATHER, true))
    }
    val weatherDataSource = remember {
        mutableStateOf(
            prefs.schemaString(
                PixelAodSettings.KEY_WEATHER_DATA_SOURCE,
                PixelAodSettings.WEATHER_DATA_SOURCE_AUTO
            )
        )
    }
    val weatherAlerts = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_WEATHER_ALERTS, false))
    }
    val weatherForecast = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_WEATHER_FORECAST, false))
    }
    val weatherForecastStartTime = remember {
        mutableStateOf(
            prefs.schemaString(PixelAodSettings.KEY_WEATHER_FORECAST_START_TIME, "21:00")
        )
    }
    val weatherForecastEndTime = remember {
        mutableStateOf(
            prefs.schemaString(PixelAodSettings.KEY_WEATHER_FORECAST_END_TIME, "23:30")
        )
    }
    val pendingBreezyWeatherFeature = remember { mutableStateOf<String?>(null) }
    val calendarEvents = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_CALENDAR_EVENTS, false))
    }
    val calendarIconPackage = remember {
        mutableStateOf(prefs.schemaString(PixelAodSettings.KEY_CALENDAR_ICON_PACKAGE, ""))
    }
    val lockscreenPolicy = remember {
        mutableStateOf(
            prefs.schemaBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true)
        )
    }
    val debugLogging = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_DEBUG_LOGGING, false))
    }
    val pixelFingerprintIcon = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, false))
    }
    val udfpsHdrPressEffect = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_UDFPS_HDR_PRESS_EFFECT, true))
    }
    val udfpsSuccessRipple = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_UDFPS_SUCCESS_RIPPLE, true))
    }
    val udfpsAodExitAnimation = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_UDFPS_AOD_EXIT_ANIMATION, true))
    }
    val weatherIconPack = remember {
        mutableStateOf(prefs.schemaString(PixelAodSettings.KEY_WEATHER_ICON_PACK, ""))
    }
    val language = remember {
        mutableStateOf(
            prefs.schemaString(PixelAodSettings.KEY_LANGUAGE, PixelAodSettings.LANGUAGE_SYSTEM)
        )
    }
    val aodWeight = remember {
        mutableFloatStateOf(
            prefs.schemaFloat(PixelAodSettings.KEY_AOD_WEIGHT, PixelAodSettings.DEFAULT_AOD_WEIGHT)
        )
    }
    val lockscreenWeight = remember {
        mutableFloatStateOf(
            prefs.schemaFloat(
                PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                PixelAodSettings.DEFAULT_LOCKSCREEN_WEIGHT
            )
        )
    }
    val clockWidth = remember {
        mutableFloatStateOf(
            prefs.schemaFloat(
                PixelAodSettings.KEY_CLOCK_WIDTH,
                PixelAodSettings.DEFAULT_CLOCK_WIDTH
            )
        )
    }
    val clockRoundness = remember {
        mutableFloatStateOf(
            prefs.schemaFloat(
                PixelAodSettings.KEY_CLOCK_ROUNDNESS,
                PixelAodSettings.DEFAULT_CLOCK_ROUNDNESS
            )
        )
    }
    val clockColorMode = remember {
        mutableStateOf(
            prefs.schemaString(
                PixelAodSettings.KEY_CLOCK_COLOR_MODE,
                PixelAodSettings.CLOCK_COLOR_MODE_DYNAMIC
            )
        )
    }

    var showClockDial by remember { mutableStateOf(false) }
    var clockDialTarget by remember { mutableStateOf(ClockDialTarget.FORECAST_START) }
    var clockDialTitle by remember { mutableStateOf("") }
    var clockDialHour by remember { mutableIntStateOf(22) }
    var clockDialMinute by remember { mutableIntStateOf(0) }

    val forecastStartTimeLabel = stringResource(R.string.title_weather_forecast_start_time)
    val forecastEndTimeLabel = stringResource(R.string.title_weather_forecast_end_time)

    val showClockDialPicker: (ClockDialTarget, String) -> Unit = { target, currentTime ->
        val parts = currentTime.split(":")
        val defaultHour = when (target) {
            ClockDialTarget.FORECAST_START -> 21
            ClockDialTarget.FORECAST_END -> 23
        }
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: defaultHour
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        clockDialTarget = target
        clockDialTitle = when (target) {
            ClockDialTarget.FORECAST_START -> forecastStartTimeLabel
            ClockDialTarget.FORECAST_END -> forecastEndTimeLabel
        }
        clockDialHour = hour
        clockDialMinute = minute
        showClockDial = true
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            calendarEvents.value = true
            updateModuleBooleanSetting(context, PixelAodSettings.KEY_CALENDAR_EVENTS, true)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.calendar_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val breezyWeatherPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val requestedFeature = ContextualAtAGlancePermission.normalizeFeatureKey(
            pendingBreezyWeatherFeature.value
        )
        pendingBreezyWeatherFeature.value = null
        if (granted && requestedFeature == ContextualAtAGlancePermission.WEATHER_ALERTS) {
            weatherAlerts.value = true
            updateModuleBooleanSetting(context, PixelAodSettings.KEY_WEATHER_ALERTS, true)
            requestBreezyWeatherRefresh(context)
        } else if (granted && requestedFeature == ContextualAtAGlancePermission.WEATHER_FORECAST) {
            weatherForecast.value = true
            updateModuleBooleanSetting(context, PixelAodSettings.KEY_WEATHER_FORECAST, true)
            requestBreezyWeatherRefresh(context)
        } else if (!granted && requestedFeature.isNotEmpty()) {
            Toast.makeText(
                context,
                context.getString(
                    if (requestedFeature == ContextualAtAGlancePermission.WEATHER_FORECAST) {
                        R.string.weather_forecast_permission_denied
                    } else {
                        R.string.weather_alert_permission_denied
                    }
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showWeatherDataSourceDialog by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showCalendarIconAppDialog by remember { mutableStateOf(false) }
    var showNonLockscreenTransitionDialog by remember { mutableStateOf(false) }
    var showClockColorModeDialog by remember { mutableStateOf(false) }

    var currentPageName by rememberSaveable { mutableStateOf(SettingsPage.HOME.name) }
    val currentPage = remember(currentPageName) { SettingsPage.valueOf(currentPageName) }
    val navigate: (SettingsPage) -> Unit = { page -> currentPageName = page.name }
    val navigateAod: () -> Unit = { currentPageName = SettingsPage.AOD.name }
    val selectedBottomPage = when (currentPage) {
        SettingsPage.HOME -> SettingsPage.HOME
        SettingsPage.AOD,
        SettingsPage.CLOCK,
        SettingsPage.FINGERPRINT,
        SettingsPage.AT_A_GLANCE,
        SettingsPage.LOCKSCREEN -> SettingsPage.AOD
        SettingsPage.SYSTEM -> SettingsPage.SYSTEM
    }
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val availablePacks = remember(context) { getAvailableIconPacks(context) }
    val currentWeatherIconLabel = availablePacks
        .find { it.first == weatherIconPack.value }
        ?.second
        ?: stringResource(R.string.default_weather_icon_pack)

    val bottomItems = listOf(
        PixelAodBottomItem(SettingsPage.HOME.name, stringResource(R.string.nav_home), Icons.Outlined.Home),
        PixelAodBottomItem(SettingsPage.AOD.name, stringResource(R.string.nav_aod), Icons.Outlined.Tune),
        PixelAodBottomItem(SettingsPage.SYSTEM.name, stringResource(R.string.nav_system_ui), Icons.Outlined.Android)
    )
    val bottomBar: @Composable () -> Unit = {
        PixelAodBottomBar(
            items = bottomItems,
            selectedValue = selectedBottomPage.name,
            onSelected = { currentPageName = it }
        )
    }

    BackHandler(enabled = currentPage != SettingsPage.HOME) {
        currentPageName = when (currentPage) {
            SettingsPage.CLOCK,
            SettingsPage.FINGERPRINT,
            SettingsPage.AT_A_GLANCE,
            SettingsPage.LOCKSCREEN -> SettingsPage.AOD.name
            else -> SettingsPage.HOME.name
        }
    }

    val surfaces = PixelAodDesignSystem.surfaces
    val weatherSourceSegments = listOf(
        PixelAodSelectionOption(
            PixelAodSettings.WEATHER_DATA_SOURCE_AUTO,
            stringResource(R.string.weather_source_auto)
        ),
        PixelAodSelectionOption(
            PixelAodSettings.WEATHER_DATA_SOURCE_SYSTEM,
            stringResource(R.string.weather_source_system)
        ),
        PixelAodSelectionOption(
            PixelAodSettings.WEATHER_DATA_SOURCE_BREEZY,
            stringResource(R.string.weather_source_breezy)
        )
    )

    when (currentPage) {
        SettingsPage.HOME -> PixelAodPage(
            title = stringResource(R.string.dashboard_title),
            badgeText = if (versionName.isNotEmpty()) "v" + versionName else null,
            actionIcon = Icons.Outlined.Language,
            actionDescription = stringResource(R.string.title_language),
            onAction = { showLanguageDialog = true },
            floatingAction = {
                FlClashActionFab(
                    icon = Icons.Outlined.RestartAlt,
                    label = stringResource(R.string.fab_restart_systemui)
                ) {
                    restartSystemUi(context)
                }
            },
            bottomBar = bottomBar
        ) {
            FlClashStatusCard(
                title = stringResource(R.string.app_name),
                statusText = stringResource(
                    if (moduleEnabled.value) R.string.status_module_active
                    else R.string.status_module_inactive
                ),
                checked = moduleEnabled.value
            ) {
                moduleEnabled.value = it
                updateModuleBooleanSetting(context, PixelAodSettings.KEY_MODULE_ENABLED, it)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.gridGap)
            ) {
                FlClashDashboardCard(
                    icon = Icons.Outlined.Palette,
                    label = stringResource(R.string.section_clock),
                    value = aodWeight.floatValue.toInt().toString() + " / " + lockscreenWeight.floatValue.toInt().toString(),
                    sublabel = stringResource(
                        R.string.card_clock_meta,
                        clockWidth.floatValue.toInt(),
                        clockRoundness.floatValue.toInt()
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    navigate(SettingsPage.CLOCK)
                }
                FlClashDashboardCard(
                    icon = Icons.Outlined.Cloud,
                    label = stringResource(R.string.section_weather),
                    value = weatherDataSourceLabel(weatherDataSource.value),
                    sublabel = currentWeatherIconLabel,
                    statusDotColor = if (weather.value) surfaces.statusSuccess
                    else surfaces.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                ) {
                    navigate(SettingsPage.AT_A_GLANCE)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.gridGap)
            ) {
                FlClashDashboardCard(
                    icon = Icons.Outlined.Fingerprint,
                    label = stringResource(R.string.section_fingerprint),
                    value = stringResource(
                        if (pixelFingerprintIcon.value) R.string.card_fingerprint_pixel
                        else R.string.card_fingerprint_stock
                    ),
                    sublabel = stringResource(R.string.card_fingerprint_meta),
                    statusDotColor = if (pixelFingerprintIcon.value) surfaces.statusSuccess else null,
                    modifier = Modifier.weight(1f)
                ) {
                    navigate(SettingsPage.FINGERPRINT)
                }
                FlClashDashboardCard(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.section_transitions),
                    value = nonLockscreenAodTransitionLabel(nonLockscreenAodTransition.value),
                    sublabel = stringResource(R.string.card_transition_meta),
                    modifier = Modifier.weight(1f)
                ) {
                    showNonLockscreenTransitionDialog = true
                }
            }

            FlClashSegmentedCard(
                icon = Icons.Outlined.Cloud,
                title = stringResource(R.string.title_weather_data_source),
                options = weatherSourceSegments,
                selectedValue = weatherDataSource.value
            ) { selected ->
                if (selected != weatherDataSource.value) {
                    weatherDataSource.value = selected
                    updateModuleStringSetting(
                        context,
                        PixelAodSettings.KEY_WEATHER_DATA_SOURCE,
                        selected
                    )
                    if (selected != PixelAodSettings.WEATHER_DATA_SOURCE_SYSTEM) {
                        requestBreezyWeatherRefresh(context)
                    }
                }
            }
        }

        SettingsPage.AOD -> PixelAodPage(
            title = stringResource(R.string.title_page_aod),
            bottomBar = bottomBar
        ) {
            PixelAodSection(stringResource(R.string.section_aod_pages)) {
                PixelAodGroup {
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.section_clock),
                        subtitle = stringResource(R.string.desc_page_clock),
                        showDivider = true
                    ) { navigate(SettingsPage.CLOCK) }
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.section_fingerprint),
                        subtitle = stringResource(R.string.desc_page_fingerprint),
                        showDivider = true
                    ) { navigate(SettingsPage.FINGERPRINT) }
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.Cloud,
                        title = stringResource(R.string.section_at_a_glance),
                        subtitle = stringResource(R.string.desc_page_at_a_glance),
                        showDivider = true
                    ) { navigate(SettingsPage.AT_A_GLANCE) }
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.Policy,
                        title = stringResource(R.string.section_lockscreen),
                        subtitle = stringResource(R.string.desc_page_lockscreen),
                        showDivider = false
                    ) { navigate(SettingsPage.LOCKSCREEN) }
                }
            }
            PixelAodSection(stringResource(R.string.section_notifications)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Notifications,
                        title = stringResource(R.string.title_pixel_notification_peek),
                        subtitle = stringResource(R.string.desc_pixel_notification_peek),
                        checked = pixelNotificationPeek.value,
                        showDivider = false
                    ) {
                        pixelNotificationPeek.value = it
                        updateModuleBooleanSetting(
                            context, PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, it
                        )
                    }
                }
            }
            PixelAodSection(stringResource(R.string.section_power_saving_enhancements)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Notifications,
                        title = stringResource(R.string.title_power_saving_notification_aod),
                        subtitle = stringResource(R.string.desc_power_saving_notification_aod),
                        checked = powerSavingNotificationAod.value,
                        showDivider = true
                    ) {
                        powerSavingNotificationAod.value = it
                        updateModuleBooleanSetting(
                            context, PixelAodSettings.KEY_POWER_SAVING_NOTIFICATION_AOD, it
                        )
                    }
                    PixelAodToggleRow(
                        icon = Icons.Outlined.BatteryChargingFull,
                        title = stringResource(R.string.title_power_saving_charging_aod),
                        subtitle = stringResource(R.string.desc_power_saving_charging_aod),
                        checked = powerSavingChargingAod.value,
                        showDivider = false
                    ) {
                        powerSavingChargingAod.value = it
                        updateModuleBooleanSetting(
                            context, PixelAodSettings.KEY_POWER_SAVING_CHARGING_AOD, it
                        )
                    }
                }
            }
        }

        SettingsPage.CLOCK -> PixelAodPage(
            title = stringResource(R.string.section_clock),
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
            PixelAodSection(stringResource(R.string.section_appearance)) {
                PixelAodGroup {
                    PixelAodSliderRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_aod_weight),
                        valueText = aodWeight.floatValue.toInt().toString(),
                        value = aodWeight.floatValue,
                        valueRange = 100f..500f,
                        showDivider = true
                    ) {
                        aodWeight.floatValue = it
                        updateModuleFloatSetting(context, PixelAodSettings.KEY_AOD_WEIGHT, it)
                    }
                    PixelAodSliderRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.title_lockscreen_weight),
                        valueText = lockscreenWeight.floatValue.toInt().toString(),
                        value = lockscreenWeight.floatValue,
                        valueRange = 100f..500f,
                        showDivider = true
                    ) {
                        lockscreenWeight.floatValue = it
                        updateModuleFloatSetting(context, PixelAodSettings.KEY_LOCKSCREEN_WEIGHT, it)
                    }
                    PixelAodSliderRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_clock_width),
                        valueText = clockWidth.floatValue.toInt().toString(),
                        value = clockWidth.floatValue,
                        valueRange = 75f..125f,
                        showDivider = true
                    ) {
                        clockWidth.floatValue = it
                        updateModuleFloatSetting(context, PixelAodSettings.KEY_CLOCK_WIDTH, it)
                    }
                    PixelAodSliderRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.title_clock_roundness),
                        valueText = clockRoundness.floatValue.toInt().toString(),
                        value = clockRoundness.floatValue,
                        valueRange = 0f..100f,
                        showDivider = true
                    ) {
                        clockRoundness.floatValue = it
                        updateModuleFloatSetting(context, PixelAodSettings.KEY_CLOCK_ROUNDNESS, it)
                    }
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.title_clock_color_mode),
                        subtitle = stringResource(R.string.desc_clock_color_mode),
                        valueText = clockColorModeLabel(clockColorMode.value),
                        showDivider = false
                    ) {
                        showClockColorModeDialog = true
                    }
                }
            }
            PixelAodSection(stringResource(R.string.section_transitions)) {
                PixelAodGroup {
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_non_lockscreen_aod_transition),
                        subtitle = stringResource(R.string.desc_non_lockscreen_aod_transition),
                        valueText = nonLockscreenAodTransitionLabel(nonLockscreenAodTransition.value),
                        showDivider = false
                    ) {
                        showNonLockscreenTransitionDialog = true
                    }
                }
            }
        }

        SettingsPage.FINGERPRINT -> PixelAodPage(
            title = stringResource(R.string.section_fingerprint),
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
            PixelAodSection(stringResource(R.string.section_fingerprint)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.title_pixel_fingerprint_icon),
                        subtitle = stringResource(R.string.desc_pixel_fingerprint_icon),
                        checked = pixelFingerprintIcon.value,
                        showDivider = false
                    ) {
                        pixelFingerprintIcon.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, it)
                    }
                }
            }
            PixelAodSection(stringResource(R.string.section_fingerprint_effects)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.title_udfps_hdr_press_effect),
                        subtitle = stringResource(R.string.desc_udfps_hdr_press_effect),
                        checked = udfpsHdrPressEffect.value,
                        showDivider = true,
                        enabled = pixelFingerprintIcon.value
                    ) {
                        udfpsHdrPressEffect.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_UDFPS_HDR_PRESS_EFFECT, it)
                    }
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.title_udfps_success_ripple),
                        subtitle = stringResource(R.string.desc_udfps_success_ripple),
                        checked = udfpsSuccessRipple.value,
                        showDivider = true
                    ) {
                        udfpsSuccessRipple.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_UDFPS_SUCCESS_RIPPLE, it)
                    }
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.title_udfps_aod_exit_animation),
                        subtitle = stringResource(R.string.desc_udfps_aod_exit_animation),
                        checked = udfpsAodExitAnimation.value,
                        showDivider = false,
                        enabled = pixelFingerprintIcon.value
                    ) {
                        udfpsAodExitAnimation.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_UDFPS_AOD_EXIT_ANIMATION, it)
                    }
                }
            }
        }

        SettingsPage.AT_A_GLANCE -> PixelAodPage(
            title = stringResource(R.string.section_at_a_glance),
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
            val calendarIconOptions = remember { getCalendarIconAppOptions(context) }
            val currentCalendarIconLabel = calendarIconOptions
                .find { it.first == calendarIconPackage.value }
                ?.second
                ?: calendarIconPackage.value

            PixelAodSection(stringResource(R.string.section_weather)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Cloud,
                        title = stringResource(R.string.title_weather),
                        subtitle = stringResource(R.string.desc_weather),
                        checked = weather.value,
                        showDivider = true
                    ) {
                        weather.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_WEATHER, it)
                    }
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Cloud,
                        title = stringResource(R.string.title_weather_data_source),
                        subtitle = stringResource(R.string.desc_weather_data_source),
                        valueText = weatherDataSourceLabel(weatherDataSource.value),
                        showDivider = true,
                        enabled = weather.value
                    ) {
                        showWeatherDataSourceDialog = true
                    }
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.title_weather_icon_pack),
                        valueText = currentWeatherIconLabel,
                        showDivider = false,
                        enabled = weather.value
                    ) {
                        showIconPackDialog = true
                    }
                }
            }

            PixelAodSection(stringResource(R.string.section_forecast)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Cloud,
                        title = stringResource(R.string.title_weather_forecast),
                        subtitle = stringResource(R.string.desc_weather_forecast),
                        checked = weatherForecast.value,
                        showDivider = true
                    ) {
                        if (!it) {
                            weatherForecast.value = false
                            updateModuleBooleanSetting(
                                context,
                                PixelAodSettings.KEY_WEATHER_FORECAST,
                                false
                            )
                            requestBreezyWeatherRefresh(context)
                        } else if (context.checkSelfPermission(BREEZY_READ_PROVIDER_PERMISSION)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            weatherForecast.value = true
                            updateModuleBooleanSetting(
                                context,
                                PixelAodSettings.KEY_WEATHER_FORECAST,
                                true
                            )
                            requestBreezyWeatherRefresh(context)
                        } else {
                            pendingBreezyWeatherFeature.value =
                                ContextualAtAGlancePermission.WEATHER_FORECAST
                            breezyWeatherPermissionLauncher.launch(BREEZY_READ_PROVIDER_PERMISSION)
                        }
                    }
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_weather_forecast_start_time),
                        valueText = weatherForecastStartTime.value,
                        showDivider = true,
                        enabled = weatherForecast.value
                    ) {
                        showClockDialPicker(
                            ClockDialTarget.FORECAST_START,
                            weatherForecastStartTime.value
                        )
                    }
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_weather_forecast_end_time),
                        valueText = weatherForecastEndTime.value,
                        showDivider = false,
                        enabled = weatherForecast.value
                    ) {
                        showClockDialPicker(
                            ClockDialTarget.FORECAST_END,
                            weatherForecastEndTime.value
                        )
                    }
                }
            }

            PixelAodSection(stringResource(R.string.section_contextual_information)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Cloud,
                        title = stringResource(R.string.title_weather_alerts),
                        subtitle = stringResource(R.string.desc_weather_alerts),
                        checked = weatherAlerts.value,
                        showDivider = true
                    ) { enabled ->
                        if (!enabled) {
                            weatherAlerts.value = false
                            updateModuleBooleanSetting(
                                context,
                                PixelAodSettings.KEY_WEATHER_ALERTS,
                                false
                            )
                            requestBreezyWeatherRefresh(context)
                        } else if (context.checkSelfPermission(BREEZY_READ_PROVIDER_PERMISSION)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            weatherAlerts.value = true
                            updateModuleBooleanSetting(
                                context,
                                PixelAodSettings.KEY_WEATHER_ALERTS,
                                true
                            )
                            requestBreezyWeatherRefresh(context)
                        } else {
                            pendingBreezyWeatherFeature.value =
                                ContextualAtAGlancePermission.WEATHER_ALERTS
                            breezyWeatherPermissionLauncher.launch(BREEZY_READ_PROVIDER_PERMISSION)
                        }
                    }
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_calendar_events),
                        subtitle = stringResource(R.string.desc_calendar_events),
                        checked = calendarEvents.value,
                        showDivider = true
                    ) { enabled ->
                        if (!enabled) {
                            calendarEvents.value = false
                            updateModuleBooleanSetting(
                                context,
                                PixelAodSettings.KEY_CALENDAR_EVENTS,
                                false
                            )
                        } else if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            calendarEvents.value = true
                            updateModuleBooleanSetting(
                                context,
                                PixelAodSettings.KEY_CALENDAR_EVENTS,
                                true
                            )
                        } else {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    }
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_calendar_icon_app),
                        valueText = currentCalendarIconLabel,
                        showDivider = false,
                        enabled = calendarEvents.value
                    ) {
                        showCalendarIconAppDialog = true
                    }
                }
            }
        }

        SettingsPage.LOCKSCREEN -> PixelAodPage(
            title = stringResource(R.string.section_lockscreen),
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
            PixelAodInfoBanner(stringResource(R.string.lockscreen_notifications_compatibility_note))
            PixelAodSection(stringResource(R.string.section_notifications)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.Policy,
                        title = stringResource(R.string.title_lockscreen_policy),
                        subtitle = stringResource(R.string.desc_lockscreen_policy),
                        checked = lockscreenPolicy.value,
                        showDivider = false
                    ) {
                        lockscreenPolicy.value = it
                        updateModuleBooleanSetting(
                            context,
                            PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY,
                            it
                        )
                    }
                }
            }
        }

        SettingsPage.SYSTEM -> PixelAodPage(
            title = stringResource(R.string.nav_system_ui),
            bottomBar = bottomBar
        ) {
            PixelAodInfoBanner(stringResource(R.string.system_ui_warning))
            PixelAodSection(stringResource(R.string.section_general)) {
                PixelAodGroup {
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.title_language),
                        valueText = languageLabel(language.value),
                        showDivider = true
                    ) { showLanguageDialog = true }
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.RestartAlt,
                        title = stringResource(R.string.restart_systemui),
                        subtitle = "",
                        showDivider = false
                    ) { restartSystemUi(context) }
                }
            }
            PixelAodSection(stringResource(R.string.section_diagnostics)) {
                PixelAodGroup {
                    PixelAodToggleRow(
                        icon = Icons.Outlined.BugReport,
                        title = stringResource(R.string.title_debug_logging),
                        subtitle = stringResource(R.string.desc_debug_logging),
                        checked = debugLogging.value,
                        showDivider = false
                    ) {
                        debugLogging.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_DEBUG_LOGGING, it)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = language.value,
            onDismiss = { showLanguageDialog = false },
            onSelected = { selected ->
                showLanguageDialog = false
                if (selected != language.value) {
                    language.value = selected
                    updateModuleStringSetting(context, PixelAodSettings.KEY_LANGUAGE, selected)
                    onLanguageChanged()
                }
            }
        )
    }

    if (showWeatherDataSourceDialog) {
        PixelAodSelectionDialog(
            title = stringResource(R.string.title_weather_data_source),
            current = weatherDataSource.value,
            options = weatherSourceSegments,
            cancelLabel = stringResource(android.R.string.cancel),
            onDismiss = { showWeatherDataSourceDialog = false },
            onSelected = { selected ->
                showWeatherDataSourceDialog = false
                if (selected != weatherDataSource.value) {
                    weatherDataSource.value = selected
                    updateModuleStringSetting(
                        context,
                        PixelAodSettings.KEY_WEATHER_DATA_SOURCE,
                        selected
                    )
                    if (selected != PixelAodSettings.WEATHER_DATA_SOURCE_SYSTEM) {
                        requestBreezyWeatherRefresh(context)
                    }
                }
            }
        )
    }

    if (showIconPackDialog) {
        IconPackDialog(
            current = weatherIconPack.value,
            options = availablePacks,
            onDismiss = { showIconPackDialog = false },
            onSelected = { selected ->
                showIconPackDialog = false
                if (selected != weatherIconPack.value) {
                    weatherIconPack.value = selected
                    updateModuleStringSetting(context, PixelAodSettings.KEY_WEATHER_ICON_PACK, selected)
                }
            }
        )
    }

    if (showCalendarIconAppDialog) {
        val calendarIconOptions = remember { getCalendarIconAppOptions(context) }
        CalendarIconAppDialog(
            current = calendarIconPackage.value,
            options = calendarIconOptions,
            onDismiss = { showCalendarIconAppDialog = false },
            onSelected = { selected ->
                showCalendarIconAppDialog = false
                if (selected != calendarIconPackage.value) {
                    calendarIconPackage.value = selected
                    updateModuleStringSetting(
                        context,
                        PixelAodSettings.KEY_CALENDAR_ICON_PACKAGE,
                        selected
                    )
                }
            }
        )
    }

    if (showNonLockscreenTransitionDialog) {
        NonLockscreenAodTransitionDialog(
            current = nonLockscreenAodTransition.value,
            onDismiss = { showNonLockscreenTransitionDialog = false },
            onSelected = { selected ->
                showNonLockscreenTransitionDialog = false
                if (selected != nonLockscreenAodTransition.value) {
                    nonLockscreenAodTransition.value = selected
                    updateModuleStringSetting(
                        context,
                        PixelAodSettings.KEY_NON_LOCKSCREEN_AOD_TRANSITION,
                        selected
                    )
                }
            }
        )
    }

    if (showClockColorModeDialog) {
        ClockColorModeDialog(
            current = clockColorMode.value,
            onDismiss = { showClockColorModeDialog = false },
            onSelected = { selected ->
                showClockColorModeDialog = false
                if (selected != clockColorMode.value) {
                    clockColorMode.value = selected
                    updateModuleStringSetting(
                        context,
                        PixelAodSettings.KEY_CLOCK_COLOR_MODE,
                        selected
                    )
                }
            }
        )
    }

    if (showClockDial) {
        PixelAodTimePickerDialog(
            title = clockDialTitle,
            initialHour = clockDialHour,
            initialMinute = clockDialMinute,
            cancelLabel = stringResource(android.R.string.cancel),
            confirmLabel = stringResource(android.R.string.ok),
            onDismiss = { showClockDial = false },
            onConfirm = { hour, minute ->
                showClockDial = false
                val formatted = String.format("%02d:%02d", hour, minute)
                when (clockDialTarget) {
                    ClockDialTarget.FORECAST_START -> {
                        weatherForecastStartTime.value = formatted
                        updateModuleStringSetting(
                            context,
                            PixelAodSettings.KEY_WEATHER_FORECAST_START_TIME,
                            formatted
                        )
                        requestBreezyWeatherRefresh(context)
                    }
                    ClockDialTarget.FORECAST_END -> {
                        weatherForecastEndTime.value = formatted
                        updateModuleStringSetting(
                            context,
                            PixelAodSettings.KEY_WEATHER_FORECAST_END_TIME,
                            formatted
                        )
                        requestBreezyWeatherRefresh(context)
                    }
                }
            }
        )
    }
}

private fun restartSystemUi(context: Context) {
    val appContext = context.applicationContext
    Thread {
        val messageRes = try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f com.android.systemui"))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                R.string.restart_systemui_success
            } else {
                R.string.restart_systemui_failed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            R.string.restart_systemui_failed
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, appContext.getString(messageRes), Toast.LENGTH_SHORT).show()
        }
    }.start()
}

@Composable
private fun languageLabel(value: String): String = when (value) {
    PixelAodSettings.LANGUAGE_CHINESE -> stringResource(R.string.language_chinese)
    PixelAodSettings.LANGUAGE_ENGLISH -> stringResource(R.string.language_english)
    else -> stringResource(R.string.language_system)
}

@Composable
private fun weatherDataSourceLabel(value: String): String = when (value) {
    PixelAodSettings.WEATHER_DATA_SOURCE_SYSTEM -> stringResource(R.string.weather_source_system)
    PixelAodSettings.WEATHER_DATA_SOURCE_BREEZY -> stringResource(R.string.weather_source_breezy)
    else -> stringResource(R.string.weather_source_auto)
}

@Composable
private fun nonLockscreenAodTransitionLabel(value: String): String = when (value) {
    PixelAodSettings.NON_LOCKSCREEN_AOD_TRANSITION_DIRECT_FINAL ->
        stringResource(R.string.non_lockscreen_aod_transition_direct_final)
    else -> stringResource(R.string.non_lockscreen_aod_transition_animated)
}

@Composable
private fun clockColorModeLabel(value: String): String = when (value) {
    PixelAodSettings.CLOCK_COLOR_MODE_WHITE -> stringResource(R.string.clock_color_mode_white)
    else -> stringResource(R.string.clock_color_mode_dynamic)
}

@Composable
private fun ClockColorModeDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    PixelAodSelectionDialog(
        title = stringResource(R.string.title_clock_color_mode),
        current = current,
        options = listOf(
            PixelAodSelectionOption(
                PixelAodSettings.CLOCK_COLOR_MODE_DYNAMIC,
                stringResource(R.string.clock_color_mode_dynamic)
            ),
            PixelAodSelectionOption(
                PixelAodSettings.CLOCK_COLOR_MODE_WHITE,
                stringResource(R.string.clock_color_mode_white)
            )
        ),
        cancelLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        onSelected = onSelected
    )
}

@Composable
private fun LanguageDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    PixelAodSelectionDialog(
        title = stringResource(R.string.title_language),
        current = current,
        options = listOf(
            PixelAodSelectionOption(
                PixelAodSettings.LANGUAGE_SYSTEM,
                stringResource(R.string.language_system)
            ),
            PixelAodSelectionOption(
                PixelAodSettings.LANGUAGE_CHINESE,
                stringResource(R.string.language_chinese)
            ),
            PixelAodSelectionOption(
                PixelAodSettings.LANGUAGE_ENGLISH,
                stringResource(R.string.language_english)
            )
        ),
        cancelLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        onSelected = onSelected
    )
}

@Composable
private fun NonLockscreenAodTransitionDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    PixelAodSelectionDialog(
        title = stringResource(R.string.title_non_lockscreen_aod_transition),
        current = current,
        options = listOf(
            PixelAodSelectionOption(
                PixelAodSettings.NON_LOCKSCREEN_AOD_TRANSITION_ANIMATED,
                stringResource(R.string.non_lockscreen_aod_transition_animated)
            ),
            PixelAodSelectionOption(
                PixelAodSettings.NON_LOCKSCREEN_AOD_TRANSITION_DIRECT_FINAL,
                stringResource(R.string.non_lockscreen_aod_transition_direct_final)
            )
        ),
        cancelLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        onSelected = onSelected
    )
}

private fun getAvailableIconPacks(context: Context): List<Pair<String, String>> {
    val packs = mutableListOf<Pair<String, String>>()
    packs.add("" to context.getString(R.string.default_weather_icon_pack))
    packs.add(
        PixelAodSettings.WEATHER_ICON_PACK_BUILTIN_GOOGLE to
            context.getString(R.string.builtin_google_weather_icon_pack)
    )
    val pm = context.packageManager

    val intents = listOf(
        Intent("org.breezyweather.ICON_PROVIDER"),
        Intent("com.dvtonder.chronus.ICON_PACK"),
        Intent("com.dvtonder.chronus.ICON_PACK_THEME")
    )

    val allPackages = mutableSetOf<String>()

    for (intent in intents) {
        val resolveInfos = pm.queryIntentActivities(intent, 0) + pm.queryBroadcastReceivers(intent, 0)
        for (info in resolveInfos) {
            val packageName = info.activityInfo?.packageName ?: continue
            if (allPackages.add(packageName)) {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    packs.add(packageName to label)
                } catch (_: Exception) {
                }
            }
        }
    }
    return packs
}

private fun getCalendarIconAppOptions(context: Context): List<Pair<String, String>> {
    val options = mutableListOf("" to context.getString(R.string.default_calendar_icon_app))
    val pm = context.packageManager
    val calendarEventIntent = Intent(Intent.ACTION_INSERT)
        .setType("vnd.android.cursor.item/event")
    val packages = linkedMapOf<String, String>()
    for (resolveInfo in pm.queryIntentActivities(calendarEventIntent, PackageManager.MATCH_DEFAULT_ONLY)) {
        if (resolveInfo.priority < 0) {
            continue
        }
        val packageName = resolveInfo.activityInfo?.packageName ?: continue
        try {
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(applicationInfo).toString()
            if (!packages.containsKey(packageName)) {
                packages[packageName] = label
            }
        } catch (_: Exception) {
        }
    }
    packages.toList()
        .sortedBy { it.second.lowercase(Locale.getDefault()) }
        .forEach { options.add(it.first to it.second) }
    return options
}

@Composable
private fun CalendarIconAppDialog(
    current: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    PixelAodSelectionDialog(
        title = stringResource(R.string.title_calendar_icon_app),
        current = current,
        options = options.map { PixelAodSelectionOption(it.first, it.second) },
        cancelLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        onSelected = onSelected,
        scrollable = true
    )
}

@Composable
private fun IconPackDialog(
    current: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    PixelAodSelectionDialog(
        title = stringResource(R.string.title_weather_icon_pack),
        current = current,
        options = options.map { PixelAodSelectionOption(it.first, it.second) },
        cancelLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        onSelected = onSelected,
        scrollable = options.size > 6
    )
}
