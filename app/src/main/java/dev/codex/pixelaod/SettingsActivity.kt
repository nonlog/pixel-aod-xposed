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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        // Edge-to-edge so the Material 3 Scaffold paints behind the status bar.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Make status & navigation bar icons legible on both light and dark surfaces.
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
        /**
         * Wraps [base] with the user-selected UI locale. "system" leaves the device
         * locale untouched; "zh"/"en" force Simplified Chinese / English so the
         * settings app can be read independently of the system language.
         */
        fun applyLanguage(base: Context): Context {
            val lang = PixelAodSettings.getSharedPreferences(base)
                .getString(PixelAodSettings.KEY_LANGUAGE, PixelAodSettings.LANGUAGE_SYSTEM)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PixelAodSettingsScreen(onLanguageChanged: () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.module_description),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        actions = {
                            IconButton(onClick = { restartSystemUi(context) }) {
                                Icon(
                                    Icons.Outlined.RestartAlt,
                                    contentDescription = stringResource(R.string.restart_systemui),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                SettingsContent(
                    onLanguageChanged = onLanguageChanged,
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
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

    val customAod = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_CUSTOM_AOD, true))
    }
    val skipDozeOffState = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_SKIP_DOZE_OFF_STATE, false))
    }
    val lockscreenClock = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_LOCKSCREEN_CLOCK, true))
    }
    val weather = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_WEATHER, true))
    }
    val lockscreenPolicy = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true))
    }
    val debugLogging = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_DEBUG_LOGGING, false))
    }
    val weatherIconPack = remember {
        mutableStateOf(prefs.getString(PixelAodSettings.KEY_WEATHER_ICON_PACK, "") ?: "")
    }
    val aodScheduleEnabled = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_AOD_SCHEDULE_ENABLED, false))
    }
    val aodScheduleStartTime = remember {
        mutableStateOf(prefs.getString(PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME, "22:00") ?: "22:00")
    }
    val aodScheduleEndTime = remember {
        mutableStateOf(prefs.getString(PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME, "07:00") ?: "07:00")
    }
    val language = remember {
        mutableStateOf(
            prefs.getString(PixelAodSettings.KEY_LANGUAGE, PixelAodSettings.LANGUAGE_SYSTEM)
                ?: PixelAodSettings.LANGUAGE_SYSTEM
        )
    }
    // ── Clock dial picker state ──
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
            prefs.getFloat(PixelAodSettings.KEY_AOD_WEIGHT, PixelAodSettings.DEFAULT_AOD_WEIGHT)
        )
    }
    val lockscreenWeight = remember {
        mutableFloatStateOf(
            prefs.getFloat(
                PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                PixelAodSettings.DEFAULT_LOCKSCREEN_WEIGHT
            )
        )
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsSection(stringResource(R.string.section_appearance)) {
            ChoiceCard(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.title_language),
                valueText = languageLabel(language.value)
            ) {
                showLanguageDialog = true
            }
            ToggleCard(Icons.Outlined.Palette, stringResource(R.string.title_custom_aod), stringResource(R.string.desc_custom_aod), customAod.value) {
                customAod.value = it
                prefs.edit().putBoolean(PixelAodSettings.KEY_CUSTOM_AOD, it).apply()
            }
            ToggleCard(Icons.Outlined.Schedule, stringResource(R.string.title_lockscreen_clock), stringResource(R.string.desc_lockscreen_clock), lockscreenClock.value) {
                lockscreenClock.value = it
                prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_CLOCK, it).apply()
            }
        }

        SettingsSection(stringResource(R.string.section_clock)) {
            SliderCard(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.title_aod_weight),
                valueText = aodWeight.floatValue.toInt().toString(),
                value = aodWeight.floatValue,
                valueRange = 100f..500f
            ) {
                aodWeight.floatValue = it
                prefs.edit().putFloat(PixelAodSettings.KEY_AOD_WEIGHT, it).apply()
            }
            SliderCard(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.title_lockscreen_weight),
                valueText = lockscreenWeight.floatValue.toInt().toString(),
                value = lockscreenWeight.floatValue,
                valueRange = 100f..500f
            ) {
                lockscreenWeight.floatValue = it
                prefs.edit().putFloat(PixelAodSettings.KEY_LOCKSCREEN_WEIGHT, it).apply()
            }
        }

        SettingsSection(stringResource(R.string.section_behavior)) {
            ToggleCard(Icons.Outlined.Cloud, stringResource(R.string.title_weather), stringResource(R.string.desc_weather), weather.value) {
                weather.value = it
                prefs.edit().putBoolean(PixelAodSettings.KEY_WEATHER, it).apply()
            }
            if (weather.value) {
                val availablePacks = remember { getAvailableIconPacks(context) }
                val currentLabel = availablePacks.find { it.first == weatherIconPack.value }?.second ?: stringResource(R.string.default_weather_icon_pack)

                ChoiceCard(
                    icon = Icons.Outlined.Cloud,
                    title = stringResource(R.string.title_weather_icon_pack),
                    valueText = currentLabel
                ) {
                    showIconPackDialog = true
                }
            }
            ToggleCard(Icons.Outlined.Policy, stringResource(R.string.title_lockscreen_policy), stringResource(R.string.desc_lockscreen_policy), lockscreenPolicy.value) {
                lockscreenPolicy.value = it
                prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, it).apply()
            }
            ToggleCard(Icons.Outlined.Schedule, stringResource(R.string.title_aod_schedule), stringResource(R.string.desc_aod_schedule), aodScheduleEnabled.value) {
                aodScheduleEnabled.value = it
                prefs.edit().putBoolean(PixelAodSettings.KEY_AOD_SCHEDULE_ENABLED, it).apply()
            }
            if (aodScheduleEnabled.value) {
                TimePickerCard(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_schedule_start_time),
                    timeText = aodScheduleStartTime.value
                ) {
                    showClockDialPicker(true, aodScheduleStartTime.value)
                }
                TimePickerCard(
                    icon = Icons.Outlined.Schedule,
                    title = stringResource(R.string.title_schedule_end_time),
                    timeText = aodScheduleEndTime.value
                ) {
                    showClockDialPicker(false, aodScheduleEndTime.value)
                }
            }
        }

        SettingsSection(stringResource(R.string.section_advanced)) {
            ToggleCard(Icons.Outlined.Bolt, stringResource(R.string.title_skip_doze_off_state), stringResource(R.string.desc_skip_doze_off_state), skipDozeOffState.value) {
                skipDozeOffState.value = it
                prefs.edit().putBoolean(PixelAodSettings.KEY_SKIP_DOZE_OFF_STATE, it).apply()
            }
            ToggleCard(Icons.Outlined.BugReport, stringResource(R.string.title_debug_logging), stringResource(R.string.desc_debug_logging), debugLogging.value) {
                debugLogging.value = it
                updateModuleBooleanSetting(context, PixelAodSettings.KEY_DEBUG_LOGGING, it)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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

    // ── Material 3 Time Picker ──
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
                        prefs.edit().putString(PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME, formatted).apply()
                    } else {
                        aodScheduleEndTime.value = formatted
                        prefs.edit().putString(PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME, formatted).apply()
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
                            onClick = { onSelected(value) }
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
                } catch (e: Exception) {
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
                            onClick = { onSelected(value) }
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
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
        )
        content()
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun SliderCard(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(valueText, style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange
            )
        }
    }
}

@Composable
private fun TimePickerCard(
    icon: ImageVector,
    title: String,
    timeText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                timeText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    title: String,
    valueText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
