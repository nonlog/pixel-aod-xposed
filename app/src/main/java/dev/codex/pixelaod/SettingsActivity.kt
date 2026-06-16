package dev.codex.pixelaod

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelAodSettingsScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PixelAodSettingsScreen() {
    val context = LocalContext.current
    val dark = false
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
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { padding ->
                SettingsContent(
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
private fun SettingsContent(modifier: Modifier = Modifier) {
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
    val notificationIcons = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_NOTIFICATION_ICONS, true))
    }
    val lockscreenPolicy = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true))
    }
    val debugLogging = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_DEBUG_LOGGING, false))
    }
    val pocketMode = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_POCKET_MODE, true))
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
    val showTimePicker = { isStart: Boolean, currentTime: String ->
        val parts = currentTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: if (isStart) 22 else 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        android.app.TimePickerDialog(context, { _, h, m ->
            val formatted = String.format("%02d:%02d", h, m)
            if (isStart) {
                aodScheduleStartTime.value = formatted
                prefs.edit().putString(PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME, formatted).apply()
            } else {
                aodScheduleEndTime.value = formatted
                prefs.edit().putString(PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME, formatted).apply()
            }
        }, hour, minute, true).show()
    }
    val clockScale = remember {
        mutableFloatStateOf(
            prefs.getFloat(PixelAodSettings.KEY_CLOCK_SCALE, PixelAodSettings.DEFAULT_CLOCK_SCALE)
        )
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ToggleCard(Icons.Outlined.Palette, stringResource(R.string.title_custom_aod), stringResource(R.string.desc_custom_aod), customAod.value) {
            customAod.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_CUSTOM_AOD, it).apply()
        }
        ToggleCard(Icons.Outlined.Bolt, stringResource(R.string.title_skip_doze_off_state), stringResource(R.string.desc_skip_doze_off_state), skipDozeOffState.value) {
            skipDozeOffState.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_SKIP_DOZE_OFF_STATE, it).apply()
        }
        ToggleCard(Icons.Outlined.Schedule, stringResource(R.string.title_lockscreen_clock), stringResource(R.string.desc_lockscreen_clock), lockscreenClock.value) {
            lockscreenClock.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_CLOCK, it).apply()
        }
        ToggleCard(Icons.Outlined.Cloud, stringResource(R.string.title_weather), stringResource(R.string.desc_weather), weather.value) {
            weather.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_WEATHER, it).apply()
        }
        ToggleCard(Icons.Outlined.Notifications, stringResource(R.string.title_notification_icons), stringResource(R.string.desc_notification_icons), notificationIcons.value) {
            notificationIcons.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_NOTIFICATION_ICONS, it).apply()
        }
        ToggleCard(Icons.Outlined.Policy, stringResource(R.string.title_lockscreen_policy), stringResource(R.string.desc_lockscreen_policy), lockscreenPolicy.value) {
            lockscreenPolicy.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, it).apply()
        }
        ToggleCard(Icons.Outlined.BugReport, stringResource(R.string.title_debug_logging), stringResource(R.string.desc_debug_logging), debugLogging.value) {
            debugLogging.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_DEBUG_LOGGING, it).apply()
        }
        ToggleCard(Icons.Outlined.Policy, stringResource(R.string.title_pocket_mode), stringResource(R.string.desc_pocket_mode), pocketMode.value) {
            pocketMode.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_POCKET_MODE, it).apply()
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
                showTimePicker(true, aodScheduleStartTime.value)
            }
            TimePickerCard(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.title_schedule_end_time),
                timeText = aodScheduleEndTime.value
            ) {
                showTimePicker(false, aodScheduleEndTime.value)
            }
        }
        SliderCard(
            icon = Icons.Outlined.Bolt,
            title = stringResource(R.string.title_clock_scale),
            valueText = "%.0f%%".format(clockScale.floatValue * 100f),
            value = clockScale.floatValue,
            valueRange = 0.9f..1.15f
        ) {
            clockScale.floatValue = it
            prefs.edit().putFloat(PixelAodSettings.KEY_CLOCK_SCALE, it).apply()
        }
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
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f com.android.systemui"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Text(stringResource(R.string.restart_systemui))
        }
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

