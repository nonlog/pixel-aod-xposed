@file:OptIn(ExperimentalMaterial3Api::class)
package dev.codex.pixelaod

import android.content.Context
import android.content.ContentValues
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

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

/** COUI-like soft page wash from wallpaper-driven scheme (not a fixed teal). */
private fun couiPageBackground(scheme: androidx.compose.material3.ColorScheme, dark: Boolean): Color {
    return if (dark) {
        scheme.surfaceContainerLowest
    } else {
        // Slight cool wash using surface + primary tint so accent still comes from wallpaper.
        val base = scheme.surface
        val accent = scheme.primaryContainer
        Color(
            red = base.red * 0.82f + accent.red * 0.18f,
            green = base.green * 0.82f + accent.green * 0.18f,
            blue = base.blue * 0.82f + accent.blue * 0.18f,
            alpha = 1f
        )
    }
}

private fun couiCardColor(scheme: androidx.compose.material3.ColorScheme, dark: Boolean): Color {
    return if (dark) scheme.surfaceContainerHigh else scheme.surface
}

@Composable
private fun PixelAodSettingsScreen(onLanguageChanged: () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    // Wallpaper / system accent — never a hard-coded teal.
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val pageBg = couiPageBackground(colors, dark)
    val cardBg = couiCardColor(colors, dark)

    MaterialTheme(colorScheme = colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top action — COUI places refresh/restart top-end over the large title.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { restartSystemUi(context) }) {
                        Icon(
                            Icons.Outlined.RestartAlt,
                            contentDescription = stringResource(R.string.restart_systemui),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    // Large expressive page title (COUI Expressive).
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            lineHeight = 42.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
                    )
                    Text(
                        text = stringResource(R.string.module_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 22.dp)
                    )

                    SettingsContent(
                        onLanguageChanged = onLanguageChanged,
                        cardColor = cardBg
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    onLanguageChanged: () -> Unit,
    cardColor: Color,
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
    var clockDialIsStart by remember { mutableStateOf(true) }
    var clockDialTitle by remember { mutableStateOf("") }
    var clockDialHour by remember { mutableIntStateOf(22) }
    var clockDialMinute by remember { mutableIntStateOf(0) }

    val startTimeLabel = stringResource(R.string.title_schedule_start_time)
    val endTimeLabel = stringResource(R.string.title_schedule_end_time)

    val showClockDialPicker: (Boolean, String) -> Unit = { isStart, currentTime ->
        val parts = currentTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: if (isStart) 22 else 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        clockDialIsStart = isStart
        clockDialTitle = if (isStart) startTimeLabel else endTimeLabel
        clockDialHour = hour
        clockDialMinute = minute
        showClockDial = true
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

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showDisplayModeDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        CouiSection(stringResource(R.string.section_appearance)) {
            CouiGroupCard(cardColor) {
                CouiToggleRow(
                    icon = Icons.Outlined.Bolt,
                    title = stringResource(R.string.title_module_enabled),
                    subtitle = stringResource(R.string.desc_module_enabled),
                    checked = moduleEnabled.value,
                    showDivider = true
                ) {
                    moduleEnabled.value = it
                    updateModuleBooleanSetting(context, PixelAodSettings.KEY_MODULE_ENABLED, it)
                }
                CouiChoiceRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.title_language),
                    valueText = languageLabel(language.value),
                    showDivider = false
                ) {
                    showLanguageDialog = true
                }
            }
        }

        CouiSection(stringResource(R.string.section_clock)) {
            CouiGroupCard(cardColor) {
                CouiToggleRow(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(R.string.title_pixel_fingerprint_icon),
                    subtitle = stringResource(R.string.desc_pixel_fingerprint_icon),
                    checked = pixelFingerprintIcon.value,
                    showDivider = true
                ) {
                    pixelFingerprintIcon.value = it
                    updateModuleBooleanSetting(context, PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, it)
                }
                CouiSliderRow(
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
                CouiSliderRow(
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

        CouiSection(stringResource(R.string.section_behavior)) {
            CouiGroupCard(cardColor) {
                CouiChoiceRow(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_aod_behavior),
                    valueText = aodDisplayModeLabel(aodDisplayMode.value),
                    showDivider = true
                ) {
                    showDisplayModeDialog = true
                }
                if (aodDisplayMode.value == PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS) {
                    CouiToggleRow(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.title_continuous_schedule),
                        subtitle = stringResource(R.string.desc_continuous_schedule),
                        checked = aodScheduleEnabled.value,
                        showDivider = true
                    ) {
                        aodScheduleEnabled.value = it
                        updateModuleBooleanSetting(context, PixelAodSettings.KEY_AOD_SCHEDULE_ENABLED, it)
                    }
                    if (aodScheduleEnabled.value) {
                        CouiChoiceRow(
                            icon = Icons.Outlined.Schedule,
                            title = stringResource(R.string.title_schedule_start_time),
                            valueText = aodScheduleStartTime.value,
                            showDivider = true
                        ) {
                            showClockDialPicker(true, aodScheduleStartTime.value)
                        }
                        CouiChoiceRow(
                            icon = Icons.Outlined.Schedule,
                            title = stringResource(R.string.title_schedule_end_time),
                            valueText = aodScheduleEndTime.value,
                            showDivider = true
                        ) {
                            showClockDialPicker(false, aodScheduleEndTime.value)
                        }
                    }
                }
                CouiToggleRow(
                    icon = Icons.Outlined.Cloud,
                    title = stringResource(R.string.title_weather),
                    subtitle = stringResource(R.string.desc_weather),
                    checked = weather.value,
                    showDivider = true
                ) {
                    weather.value = it
                    prefs.edit().putBoolean(PixelAodSettings.KEY_WEATHER, it).apply()
                }
                if (weather.value) {
                    val availablePacks = remember { getAvailableIconPacks(context) }
                    val currentLabel = availablePacks.find { it.first == weatherIconPack.value }?.second
                        ?: stringResource(R.string.default_weather_icon_pack)
                    CouiChoiceRow(
                        icon = Icons.Outlined.Cloud,
                        title = stringResource(R.string.title_weather_icon_pack),
                        valueText = currentLabel,
                        showDivider = true
                    ) {
                        showIconPackDialog = true
                    }
                }
                CouiToggleRow(
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

        CouiSection(stringResource(R.string.section_advanced)) {
            CouiGroupCard(cardColor) {
                CouiToggleRow(
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
        val state = rememberTimePickerState(
            initialHour = clockDialHour,
            initialMinute = clockDialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showClockDial = false },
            title = { Text(clockDialTitle) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    showClockDial = false
                    val formatted = String.format("%02d:%02d", state.hour, state.minute)
                    if (clockDialIsStart) {
                        aodScheduleStartTime.value = formatted
                        updateModuleStringSetting(
                            context,
                            PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME,
                            formatted
                        )
                    } else {
                        aodScheduleEndTime.value = formatted
                        updateModuleStringSetting(
                            context,
                            PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME,
                            formatted
                        )
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockDial = false }) {
                    Text("Cancel")
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
    val options = listOf(
        PixelAodSettings.LANGUAGE_SYSTEM to stringResource(R.string.language_system),
        PixelAodSettings.LANGUAGE_CHINESE to stringResource(R.string.language_chinese),
        PixelAodSettings.LANGUAGE_ENGLISH to stringResource(R.string.language_english)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_language)) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == current,
                                onClick = { onSelected(value) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == current,
                            onClick = { onSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun AodDisplayModeDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val options = listOf(
        PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS to stringResource(R.string.aod_behavior_continuous_trigger),
        PixelAodSettings.AOD_DISPLAY_MODE_TRIGGER_ONLY to stringResource(R.string.aod_behavior_trigger_only)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_aod_behavior)) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == current,
                                onClick = { onSelected(value) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == current,
                            onClick = { onSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
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

@Composable
private fun IconPackDialog(
    current: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_weather_icon_pack)) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == current,
                                onClick = { onSelected(value) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == current,
                            onClick = { onSelected(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// ── COUI Expressive building blocks (structure from screenshots; accent = wallpaper) ──

private val CouiCardShape = RoundedCornerShape(28.dp)
private val CouiRowPaddingH = 18.dp
private val CouiRowPaddingV = 16.dp

@Composable
private fun CouiSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            // Wallpaper primary — same role as COUI teal section headers.
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp)
        )
        content()
    }
}

@Composable
private fun CouiGroupCard(cardColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = CouiCardShape,
        color = cardColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun CouiRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = CouiRowPaddingH),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun CouiLeadingIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun CouiToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    showDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = CouiRowPaddingH, vertical = CouiRowPaddingV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CouiLeadingIcon(icon)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedBorderColor = Color.Transparent,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        if (showDivider) {
            CouiRowDivider()
        }
    }
}

@Composable
private fun CouiChoiceRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = CouiRowPaddingH, vertical = CouiRowPaddingV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CouiLeadingIcon(icon)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                valueText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (showDivider) {
            CouiRowDivider()
        }
    }
}

@Composable
private fun CouiSliderRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    showDivider: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiRowPaddingH, vertical = CouiRowPaddingV)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CouiLeadingIcon(icon)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        valueText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
        if (showDivider) {
            CouiRowDivider()
        }
    }
}
