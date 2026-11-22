package dev.codex.pixelaod

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared settings presentation system inspired by FlClash's clean Material 3 layout:
 * compact 56dp top bar, low-elevation 16dp grouped list cards with hairline inset dividers,
 * 15sp single-line titles, 12.5sp concise subtitles, and native Material 3 switches.
 */
object PixelAodDesignSystem {
    @Immutable
    data class Spacing(
        val pageHorizontal: Dp = 16.dp,
        val pageBottom: Dp = 20.dp,
        val titleHorizontal: Dp = 4.dp,
        val sectionGap: Dp = 14.dp,
        val sectionInnerGap: Dp = 6.dp,
        val rowHorizontal: Dp = 16.dp,
        val rowVertical: Dp = 12.dp,
        val leadingGap: Dp = 14.dp,
        val trailingGap: Dp = 10.dp
    )

    @Immutable
    data class Motion(
        val stateChangeMillis: Int = 220,
        val contentChangeMillis: Int = 300
    )

    val spacing = Spacing()
    val motion = Motion()
    val groupShape = RoundedCornerShape(16.dp)
    val heroShape = RoundedCornerShape(16.dp)
    val iconShape = RoundedCornerShape(12.dp)

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    fun groupColor(scheme: ColorScheme): Color = scheme.surfaceContainerLow
    fun pageColor(scheme: ColorScheme): Color = scheme.surface
    fun bottomBarColor(scheme: ColorScheme): Color = scheme.surfaceContainer
}

@Immutable
data class PixelAodSelectionOption(val value: String, val label: String)

@Immutable
data class PixelAodBottomItem(
    val value: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun PixelAodTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val baseTypography = Typography()
    val typography = baseTypography.copy(
        titleLarge = baseTypography.titleLarge.copy(
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Medium
        ),
        titleMedium = baseTypography.titleMedium.copy(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        ),
        bodyMedium = baseTypography.bodyMedium.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodySmall = baseTypography.bodySmall.copy(
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        ),
        labelLarge = baseTypography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    )
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = PixelAodDesignSystem.shapes,
        content = content
    )
}

@Composable
fun PixelAodPage(
    title: String,
    subtitle: String,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    backDescription: String? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelAodDesignSystem.pageColor(scheme))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = if (onBack != null) 4.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = backDescription,
                            tint = scheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (actionIcon != null && onAction != null) {
                    IconButton(onClick = onAction) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionDescription,
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PixelAodDesignSystem.spacing.pageHorizontal)
                    .padding(top = 4.dp, bottom = PixelAodDesignSystem.spacing.pageBottom),
                verticalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.sectionGap),
                content = content
            )
            bottomBar()
        }
    }
}

@Composable
fun PixelAodSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.sectionInnerGap),
        modifier = Modifier.animateContentSize()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        content()
    }
}

@Composable
fun PixelAodGroup(
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = PixelAodDesignSystem.groupShape,
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun PixelAodHeroToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = PixelAodDesignSystem.heroShape,
        color = if (checked) scheme.primaryContainer else scheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = if (checked) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (checked) scheme.onPrimaryContainer else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked) {
                            scheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            scheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.trailingGap))
            PixelAodSwitch(checked = checked, enabled = true, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun PixelAodInfoBanner(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun PixelAodSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = scheme.onPrimary,
            checkedTrackColor = scheme.primary,
            uncheckedThumbColor = scheme.outline,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedBorderColor = scheme.outline
        )
    )
}

@Composable
fun PixelAodHeroMark(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = scheme.secondaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = size.minDimension * 0.135f
            val mark = Path().apply {
                moveTo(w * 0.28f, h * 0.80f)
                lineTo(w * 0.28f, h * 0.30f)
                cubicTo(
                    w * 0.28f, h * 0.18f,
                    w * 0.44f, h * 0.15f,
                    w * 0.56f, h * 0.19f
                )
                cubicTo(
                    w * 0.72f, h * 0.24f,
                    w * 0.74f, h * 0.48f,
                    w * 0.55f, h * 0.53f
                )
                cubicTo(
                    w * 0.43f, h * 0.56f,
                    w * 0.34f, h * 0.54f,
                    w * 0.28f, h * 0.58f
                )
            }
            drawPath(
                path = mark,
                color = scheme.primary,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            val center = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.34f)
            val radius = size.minDimension * 0.17f
            drawCircle(color = scheme.tertiaryContainer, radius = radius, center = center)
            drawCircle(
                color = scheme.tertiary,
                radius = radius,
                center = center,
                style = Stroke(width = size.minDimension * 0.035f)
            )
            drawLine(
                color = scheme.onTertiaryContainer,
                start = center,
                end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.52f),
                strokeWidth = size.minDimension * 0.035f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = scheme.onTertiaryContainer,
                start = center,
                end = androidx.compose.ui.geometry.Offset(
                    center.x + radius * 0.48f,
                    center.y + radius * 0.20f
                ),
                strokeWidth = size.minDimension * 0.035f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PixelAodActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = PixelAodDesignSystem.heroShape,
        color = scheme.secondaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSecondaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelAodDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
    )
}

@Composable
private fun PixelAodLeadingIcon(icon: ImageVector, enabled: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f),
        modifier = Modifier.size(22.dp)
    )
}

@Composable
fun PixelAodNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueText: String? = null,
    showDivider: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!valueText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 112.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            PixelAodDivider()
        }
    }
}

@Composable
fun PixelAodToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    showDivider: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.trailingGap))
            PixelAodSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) onCheckedChange else null
            )
        }
        if (showDivider) {
            PixelAodDivider()
        }
    }
}

@Composable
fun PixelAodChoiceRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    showDivider: Boolean,
    subtitle: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = valueText,
                modifier = Modifier.widthIn(max = 136.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        if (showDivider) {
            PixelAodDivider()
        }
    }
}

@Composable
fun PixelAodSliderRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    showDivider: Boolean,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAodLeadingIcon(icon, enabled)
                Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.padding(start = 36.dp, top = 2.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
        if (showDivider) {
            PixelAodDivider()
        }
    }
}

@Composable
fun PixelAodBottomBar(
    items: List<PixelAodBottomItem>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = PixelAodDesignSystem.bottomBarColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = item.value == selectedValue
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(item.value) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun PixelAodSelectionDialog(
    title: String,
    current: String,
    options: List<PixelAodSelectionOption>,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    scrollable: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            val bodyModifier = if (scrollable) {
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = bodyModifier.clip(RoundedCornerShape(14.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                options.forEach { option ->
                    val selected = option.value == current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.value) },
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelAodTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    cancelLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = scheme.surfaceContainerHigh,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
        },
        text = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = scheme.surfaceContainer,
                            clockDialSelectedContentColor = scheme.onPrimary,
                            clockDialUnselectedContentColor = scheme.onSurface,
                            selectorColor = scheme.primary,
                            containerColor = Color.Transparent,
                            periodSelectorBorderColor = scheme.outlineVariant,
                            periodSelectorSelectedContainerColor = scheme.secondaryContainer,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = scheme.onSecondaryContainer,
                            periodSelectorUnselectedContentColor = scheme.onSurfaceVariant,
                            timeSelectorSelectedContainerColor = scheme.primaryContainer,
                            timeSelectorUnselectedContainerColor = scheme.surfaceContainer,
                            timeSelectorSelectedContentColor = scheme.onPrimaryContainer,
                            timeSelectorUnselectedContentColor = scheme.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        }
    )
}
