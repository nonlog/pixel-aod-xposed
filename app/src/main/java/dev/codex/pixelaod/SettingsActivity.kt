@file:OptIn(ExperimentalMaterial3Api::class)
package dev.codex.pixelaod

import android.content.ContentValues
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import java.util.Locale

private const val BREEZY_READ_PROVIDER_PERMISSION = "org.breezyweather.READ_PROVIDER"
private const val REQUEST_BREEZY_WEATHER_RELAY =
    "dev.codex.pixelaod.REQUEST_BREEZY_WEATHER_RELAY"

private enum class ClockDialTarget {
    AOD_START,
    AOD_END,
    FORECAST_START,
    FORECAST_END
}

private enum class SettingsPage {
    HOME,
    AOD,
    AOD_DISPLAY,
    CLOCK,
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
    val prefs = remember {
        PixelAodSettings.getSharedPreferences(context)
    }

    val moduleEnabled = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_MODULE_ENABLED, true))
    }
    val aodDisplayMode = remember {
        mutableStateOf(
            prefs.schemaString(
                PixelAodSettings.KEY_AOD_DISPLAY_MODE,
                PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS
            )
        )
    }
    val weather = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_WEATHER, true))
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
    val aodScheduleEnabled = remember {
        mutableStateOf(prefs.schemaBoolean(PixelAodSettings.KEY_AOD_SCHEDULE_ENABLED, false))
    }
    val aodScheduleStartTime = remember {
        mutableStateOf(prefs.schemaString(PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME, "22:00"))
    }
    val aodScheduleEndTime = remember {
        mutableStateOf(prefs.schemaString(PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME, "07:00"))
    }
    val language = remember {
        mutableStateOf(
            prefs.schemaString(PixelAodSettings.KEY_LANGUAGE, PixelAodSettings.LANGUAGE_SYSTEM)
        )
    }
    var showClockDial by remember { mutableStateOf(false) }
    var clockDialTarget by remember { mutableStateOf(ClockDialTarget.AOD_START) }
    var clockDialTitle by remember { mutableStateOf("") }
    var clockDialHour by remember { mutableIntStateOf(22) }
    var clockDialMinute by remember { mutableIntStateOf(0) }

    val startTimeLabel = stringResource(R.string.title_schedule_start_time)
    val endTimeLabel = stringResource(R.string.title_schedule_end_time)
    val forecastStartTimeLabel = stringResource(R.string.title_weather_forecast_start_time)
    val forecastEndTimeLabel = stringResource(R.string.title_weather_forecast_end_time)

    val showClockDialPicker: (ClockDialTarget, String) -> Unit = { target, currentTime ->
        val parts = currentTime.split(":")
        val defaultHour = when (target) {
            ClockDialTarget.AOD_START -> 22
            ClockDialTarget.AOD_END -> 7
            ClockDialTarget.FORECAST_START -> 21
            ClockDialTarget.FORECAST_END -> 23
        }
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: defaultHour
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        clockDialTarget = target
        clockDialTitle = when (target) {
            ClockDialTarget.AOD_START -> startTimeLabel
            ClockDialTarget.AOD_END -> endTimeLabel
            ClockDialTarget.FORECAST_START -> forecastStartTimeLabel
            ClockDialTarget.FORECAST_END -> forecastEndTimeLabel
        }
        clockDialHour = hour
        clockDialMinute = minute
        showClockDial = true
    }
    val aodWeight = remember {
        mutableFloatStateOf(
            prefs.schemaFloat(PixelAodSettings.KEY_AOD_WEIGHT, PixelAodSettings.DEFAULT_AOD_WEIGHT)
        )
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
    val lockscreenWeight = remember {
        mutableFloatStateOf(
            prefs.schemaFloat(
                PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                PixelAodSettings.DEFAULT_LOCKSCREEN_WEIGHT
            )
        )
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showCalendarIconAppDialog by remember { mutableStateOf(false) }
    var showDisplayModeDialog by remember { mutableStateOf(false) }

    var currentPageName by rememberSaveable { mutableStateOf(SettingsPage.HOME.name) }
    val currentPage = remember(currentPageName) { SettingsPage.valueOf(currentPageName) }
    val navigate: (SettingsPage) -> Unit = { page -> currentPageName = page.name }
    val navigateHome: () -> Unit = { currentPageName = SettingsPage.HOME.name }
    val navigateAod: () -> Unit = { currentPageName = SettingsPage.AOD.name }
    val selectedBottomPage = when (currentPage) {
        SettingsPage.HOME -> SettingsPage.HOME
        SettingsPage.AOD,
        SettingsPage.AOD_DISPLAY,
        SettingsPage.CLOCK,
        SettingsPage.AT_A_GLANCE,
        SettingsPage.LOCKSCREEN -> SettingsPage.AOD
        SettingsPage.SYSTEM -> SettingsPage.SYSTEM
    }
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val bottomItems = listOf(
        PixelAodBottomItem(SettingsPage.HOME.name, stringResource(R.string.nav_home), Icons.Outlined.Home),
        PixelAodBottomItem(SettingsPage.AOD.name, stringResource(R.string.nav_aod), Icons.Outlined.Schedule),
        PixelAodBottomItem(SettingsPage.SYSTEM.name, stringResource(R.string.nav_system_ui), Icons.Outlined.Android)
    )
    val bottomBar: @Composable () -> Unit = {
        PixelAodBottomBar(
            items = bottomItems,
            selectedValue = selectedBottomPage.name,
            onSelected = { currentPageName = it }
        )
    }
    val continuousAodMode =
        aodDisplayMode.value == PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS
    val scheduleTimesEnabled = continuousAodMode && aodScheduleEnabled.value
    BackHandler(enabled = currentPage != SettingsPage.HOME) {
        currentPageName = when (currentPage) {
            SettingsPage.AOD_DISPLAY,
            SettingsPage.CLOCK,
            SettingsPage.AT_A_GLANCE,
            SettingsPage.LOCKSCREEN -> SettingsPage.AOD.name
            else -> SettingsPage.HOME.name
        }
    }

    when (currentPage) {
        SettingsPage.HOME -> PixelAodPage(
            title = stringResource(R.string.nav_home),
            subtitle = "",
            bottomBar = bottomBar
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelAodHeroMark(modifier = Modifier.size(112.dp))
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = versionName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.module_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            PixelAodActionCard(
                icon = Icons.Outlined.RestartAlt,
                title = stringResource(R.string.restart_systemui),
                subtitle = stringResource(R.string.desc_restart_systemui)
            ) { restartSystemUi(context) }

            PixelAodHeroToggle(
                title = stringResource(R.string.title_module_enabled),
                subtitle = stringResource(R.string.desc_module_enabled),
                checked = moduleEnabled.value
            ) {
                moduleEnabled.value = it
                updateModuleBooleanSetting(context, PixelAodSettings.KEY_MODULE_ENABLED, it)
            }

            PixelAodSection(stringResource(R.string.section_general)) {
                PixelAodGroup {
                    PixelAodChoiceRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.title_language),
                        valueText = languageLabel(language.value),
                        showDivider = false
                    ) { showLanguageDialog = true }
                }
            }
        }
        SettingsPage.AOD -> PixelAodPage(
            title = stringResource(R.string.title_page_aod),
            subtitle = "",
            bottomBar = bottomBar
        ) {
            PixelAodSection(stringResource(R.string.section_aod_pages)) {
                PixelAodGroup {
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_aod_display_behavior),
                        subtitle = stringResource(R.string.desc_aod_display_behavior),
                        valueText = aodDisplayModeLabel(aodDisplayMode.value),
                        showDivider = true
                    ) { navigate(SettingsPage.AOD_DISPLAY) }
                    PixelAodNavigationRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.section_clock),
                        subtitle = stringResource(R.string.desc_page_clock),
                        showDivider = true
                    ) { navigate(SettingsPage.CLOCK) }
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
        }

        SettingsPage.AOD_DISPLAY -> PixelAodPage(
            title = stringResource(R.string.title_aod_display_behavior),
            subtitle = "",
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
        PixelAodSection(stringResource(R.string.section_aod_display)) {
            PixelAodGroup() {
                PixelAodChoiceRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_aod_behavior),
                    valueText = aodDisplayModeLabel(aodDisplayMode.value),
                    showDivider = true
                ) {
                    showDisplayModeDialog = true
                }
                PixelAodToggleRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_continuous_schedule),
                    subtitle = if (continuousAodMode) {
                        stringResource(R.string.desc_continuous_schedule)
                    } else {
                        stringResource(R.string.desc_continuous_schedule_disabled)
                    },
                    checked = aodScheduleEnabled.value,
                    showDivider = true,
                    enabled = continuousAodMode
                ) {
                    aodScheduleEnabled.value = it
                    updateModuleBooleanSetting(context, PixelAodSettings.KEY_AOD_SCHEDULE_ENABLED, it)
                }
                PixelAodChoiceRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_schedule_start_time),
                    valueText = aodScheduleStartTime.value,
                    showDivider = true,
                    enabled = scheduleTimesEnabled
                ) {
                    showClockDialPicker(
                        ClockDialTarget.AOD_START,
                        aodScheduleStartTime.value
                    )
                }
                PixelAodChoiceRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_schedule_end_time),
                    valueText = aodScheduleEndTime.value,
                    showDivider = false,
                    enabled = scheduleTimesEnabled
                ) {
                    showClockDialPicker(
                        ClockDialTarget.AOD_END,
                        aodScheduleEndTime.value
                    )
                }
            }
        }
        }

        SettingsPage.CLOCK -> PixelAodPage(
            title = stringResource(R.string.section_clock),
            subtitle = "",
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
        PixelAodSection(stringResource(R.string.section_appearance)) {
            PixelAodGroup() {
                PixelAodSliderRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_aod_weight),
                    valueText = aodWeight.floatValue.toInt().toString(),
                    value = aodWeight.floatValue,
                    valueRange = 100f..500f,
                    showDivider = true
                ) {
                    aodWeight.floatValue = it
                    prefs.edit().putFloat(PixelAodSettings.KEY_AOD_WEIGHT, it).apply()
                }
                PixelAodSliderRow(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.title_lockscreen_weight),
                    valueText = lockscreenWeight.floatValue.toInt().toString(),
                    value = lockscreenWeight.floatValue,
                    valueRange = 100f..500f,
                    showDivider = false
                ) {
                    lockscreenWeight.floatValue = it
                    prefs.edit().putFloat(PixelAodSettings.KEY_LOCKSCREEN_WEIGHT, it).apply()
                }
            }
        }

        PixelAodSection(stringResource(R.string.section_fingerprint)) {
            PixelAodGroup() {
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
            PixelAodGroup() {
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
                    showDivider = true,
                    enabled = pixelFingerprintIcon.value
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
            subtitle = "",
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
        val availablePacks = remember { getAvailableIconPacks(context) }
        val currentWeatherIconLabel = availablePacks
            .find { it.first == weatherIconPack.value }
            ?.second
            ?: stringResource(R.string.default_weather_icon_pack)
        val calendarIconOptions = remember { getCalendarIconAppOptions(context) }
        val currentCalendarIconLabel = calendarIconOptions
            .find { it.first == calendarIconPackage.value }
            ?.second
            ?: calendarIconPackage.value

        PixelAodSection(stringResource(R.string.section_weather)) {
            PixelAodGroup() {
                PixelAodToggleRow(
                    icon = Icons.Outlined.Cloud,
                    title = stringResource(R.string.title_weather),
                    subtitle = stringResource(R.string.desc_weather),
                    checked = weather.value,
                    showDivider = true
                ) {
                    weather.value = it
                    prefs.edit().putBoolean(PixelAodSettings.KEY_WEATHER, it).apply()
                }
                PixelAodChoiceRow(
                    icon = Icons.Outlined.Cloud,
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
            PixelAodGroup() {
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
            PixelAodGroup() {
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
            subtitle = "",
            onBack = navigateAod,
            backDescription = stringResource(R.string.navigate_back),
            bottomBar = bottomBar
        ) {
        PixelAodSection(stringResource(R.string.section_notifications)) {
            PixelAodGroup() {
                PixelAodToggleRow(
                    icon = Icons.Outlined.Policy,
                    title = stringResource(R.string.title_lockscreen_policy),
                    subtitle = stringResource(R.string.desc_lockscreen_policy),
                    checked = lockscreenPolicy.value,
                    showDivider = false
                ) {
                    lockscreenPolicy.value = it
                    prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, it).apply()
                }
            }
        }
        }

        SettingsPage.SYSTEM -> PixelAodPage(
            title = stringResource(R.string.nav_system_ui),
            subtitle = "",
            bottomBar = bottomBar
        ) {
        PixelAodInfoBanner(stringResource(R.string.system_ui_warning))
        PixelAodSection(stringResource(R.string.section_diagnostics)) {
            PixelAodGroup() {
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
                    prefs.edit().putString(PixelAodSettings.KEY_LANGUAGE, selected).apply()
                    onLanguageChanged()
                }
            }
        )
    }

    if (showIconPackDialog) {
        val availablePacks = remember { getAvailableIconPacks(context) }
        IconPackDialog(
            current = weatherIconPack.value,
            options = availablePacks,
            onDismiss = { showIconPackDialog = false },
            onSelected = { selected ->
                showIconPackDialog = false
                if (selected != weatherIconPack.value) {
                    weatherIconPack.value = selected
                    prefs.edit().putString(PixelAodSettings.KEY_WEATHER_ICON_PACK, selected).apply()
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

    if (showDisplayModeDialog) {
        AodDisplayModeDialog(
            current = aodDisplayMode.value,
            onDismiss = { showDisplayModeDialog = false },
            onSelected = { selected ->
                showDisplayModeDialog = false
                if (selected != aodDisplayMode.value) {
                    aodDisplayMode.value = selected
                    updateModuleStringSetting(context, PixelAodSettings.KEY_AOD_DISPLAY_MODE, selected)
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
                    ClockDialTarget.AOD_START -> {
                        aodScheduleStartTime.value = formatted
                        updateModuleStringSetting(
                            context,
                            PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME,
                            formatted
                        )
                    }
                    ClockDialTarget.AOD_END -> {
                        aodScheduleEndTime.value = formatted
                        updateModuleStringSetting(
                            context,
                            PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME,
                            formatted
                        )
                    }
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
private fun aodDisplayModeLabel(value: String): String = when (value) {
    PixelAodSettings.AOD_DISPLAY_MODE_TRIGGER_ONLY -> stringResource(R.string.aod_behavior_trigger_only)
    else -> stringResource(R.string.aod_behavior_continuous_trigger)
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
private fun AodDisplayModeDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    PixelAodSelectionDialog(
        title = stringResource(R.string.title_aod_behavior),
        current = current,
        options = listOf(
            PixelAodSelectionOption(
                PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS,
                stringResource(R.string.aod_behavior_continuous_trigger)
            ),
            PixelAodSelectionOption(
                PixelAodSettings.AOD_DISPLAY_MODE_TRIGGER_ONLY,
                stringResource(R.string.aod_behavior_trigger_only)
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
    // Android has no calendar-app registry. Calendar editors advertise this event MIME type.
    val calendarEventIntent = Intent(Intent.ACTION_INSERT)
        .setType("vnd.android.cursor.item/event")
    val packages = linkedMapOf<String, String>()
    for (resolveInfo in pm.queryIntentActivities(calendarEventIntent, PackageManager.MATCH_DEFAULT_ONLY)) {
        // OPlus's missing-calendar recovery activity deliberately ranks below valid handlers.
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
