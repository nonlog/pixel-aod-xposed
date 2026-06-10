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
import androidx.compose.runtime.Composable
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
                                Text("Pixel AOD", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "OPlus / modern LSPosed",
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
        context.getSharedPreferences(PixelAodSettings.PREFS, Context.MODE_PRIVATE)
    }

    val customAod = remember {
        mutableStateOf(prefs.getBoolean(PixelAodSettings.KEY_CUSTOM_AOD, true))
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
        ToggleCard(Icons.Outlined.Palette, "自定义 AOD", "双排 Pixel 时钟", customAod.value) {
            customAod.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_CUSTOM_AOD, it).apply()
        }
        ToggleCard(Icons.Outlined.Schedule, "锁屏时钟", "锁屏与 AOD 统一样式", lockscreenClock.value) {
            lockscreenClock.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_CLOCK, it).apply()
        }
        ToggleCard(Icons.Outlined.Cloud, "天气信息", "Breezy Weather / Gadgetbridge", weather.value) {
            weather.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_WEATHER, it).apply()
        }
        ToggleCard(Icons.Outlined.Notifications, "通知图标", "AOD 单色通知图标", notificationIcons.value) {
            notificationIcons.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_NOTIFICATION_ICONS, it).apply()
        }
        ToggleCard(Icons.Outlined.Policy, "锁屏通知策略", "修正 OOS 锁屏显示规则", lockscreenPolicy.value) {
            lockscreenPolicy.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, it).apply()
        }
        ToggleCard(Icons.Outlined.BugReport, "调试日志", "输出 PixelAod 日志", debugLogging.value) {
            debugLogging.value = it
            prefs.edit().putBoolean(PixelAodSettings.KEY_DEBUG_LOGGING, it).apply()
        }
        SliderCard(
            icon = Icons.Outlined.Bolt,
            title = "时钟缩放",
            valueText = "%.0f%%".format(clockScale.floatValue * 100f),
            value = clockScale.floatValue,
            valueRange = 0.9f..1.15f
        ) {
            clockScale.floatValue = it
            prefs.edit().putFloat(PixelAodSettings.KEY_CLOCK_SCALE, it).apply()
        }
        SliderCard(
            icon = Icons.Outlined.Schedule,
            title = "AOD 字重",
            valueText = aodWeight.floatValue.toInt().toString(),
            value = aodWeight.floatValue,
            valueRange = 200f..420f
        ) {
            aodWeight.floatValue = it
            prefs.edit().putFloat(PixelAodSettings.KEY_AOD_WEIGHT, it).apply()
        }
        SliderCard(
            icon = Icons.Outlined.Palette,
            title = "锁屏字重",
            valueText = lockscreenWeight.floatValue.toInt().toString(),
            value = lockscreenWeight.floatValue,
            valueRange = 420f..650f
        ) {
            lockscreenWeight.floatValue = it
            prefs.edit().putFloat(PixelAodSettings.KEY_LOCKSCREEN_WEIGHT, it).apply()
        }
        Spacer(modifier = Modifier.height(6.dp))
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
