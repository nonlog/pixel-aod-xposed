package dev.codex.pixelaod

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared settings presentation system.
 *
 * The visual hierarchy follows COUI Expressive 2.5: wallpaper-derived Material 3 color,
 * edge-to-edge system surfaces, a compact large title, low-elevation rounded groups, primary
 * section labels and controls, and one continuous settings column.  No setting key or persistence
 * semantics live here; this file owns presentation only.
 */
object PixelAodDesignSystem {
    @Immutable
    data class Spacing(
        val pageHorizontal: Dp = 20.dp,
        val pageBottom: Dp = 28.dp,
        val titleHorizontal: Dp = 4.dp,
        val sectionGap: Dp = 18.dp,
        val sectionInnerGap: Dp = 10.dp,
        val rowHorizontal: Dp = 18.dp,
        val rowVertical: Dp = 16.dp,
        val leadingGap: Dp = 16.dp,
        val trailingGap: Dp = 12.dp
    )

    @Immutable
    data class Motion(
        val stateChangeMillis: Int = 300,
        val contentChangeMillis: Int = 450
    )

    val spacing = Spacing()
    val motion = Motion()
    val groupShape = RoundedCornerShape(28.dp)
    val heroShape = RoundedCornerShape(28.dp)
    val iconShape = RoundedCornerShape(16.dp)

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = groupShape
    )

    fun groupColor(scheme: ColorScheme): Color = scheme.surfaceContainerLow
    fun pageColor(scheme: ColorScheme): Color = scheme.background
}

@Immutable
data class PixelAodSelectionOption(val value: String, val label: String)

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
        displaySmall = baseTypography.displaySmall.copy(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold
        ),
        titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
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
    actionIcon: ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
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
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onAction) {
                    Icon(
                        actionIcon,
                        contentDescription = actionDescription,
                        tint = scheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PixelAodDesignSystem.spacing.pageHorizontal)
                    .padding(bottom = PixelAodDesignSystem.spacing.pageBottom),
                verticalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.sectionGap)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(
                        start = PixelAodDesignSystem.spacing.titleHorizontal,
                        end = PixelAodDesignSystem.spacing.titleHorizontal,
                        bottom = 2.dp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = PixelAodDesignSystem.spacing.titleHorizontal,
                        end = PixelAodDesignSystem.spacing.titleHorizontal,
                        bottom = 4.dp
                    )
                )
                content()
            }
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        content()
    }
}

@Composable
fun PixelAodGroup(
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (enabled) 0.dp else 1.dp,
        animationSpec = androidx.compose.animation.core.tween(
            PixelAodDesignSystem.motion.stateChangeMillis,
            easing = FastOutSlowInEasing
        ),
        label = "group-elevation"
    )
    Surface(
        shape = PixelAodDesignSystem.groupShape,
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = elevation,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.animateContentSize(), content = content)
    }
}

@Composable
fun PixelAodHeroToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = PixelAodDesignSystem.heroShape,
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(PixelAodDesignSystem.iconShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.trailingGap))
            PixelAodSwitch(checked)
        }
    }
}

@Composable
private fun PixelAodSwitch(checked: Boolean) {
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

@Composable
private fun PixelAodDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = 58.dp,
            end = PixelAodDesignSystem.spacing.rowHorizontal
        ),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun PixelAodLeadingIcon(icon: ImageVector, enabled: Boolean) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        modifier = Modifier.size(24.dp)
    )
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(
                    horizontal = PixelAodDesignSystem.spacing.rowHorizontal,
                    vertical = PixelAodDesignSystem.spacing.rowVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.trailingGap))
            PixelAodSwitch(checked)
        }
        if (showDivider) PixelAodDivider()
    }
}

@Composable
fun PixelAodChoiceRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    showDivider: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = PixelAodDesignSystem.spacing.rowHorizontal,
                    vertical = PixelAodDesignSystem.spacing.rowVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.trailingGap))
            Text(
                valueText,
                modifier = Modifier.widthIn(max = 160.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        if (showDivider) PixelAodDivider()
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
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PixelAodDesignSystem.spacing.rowHorizontal,
                    vertical = PixelAodDesignSystem.spacing.rowVertical
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAodLeadingIcon(icon, enabled)
                Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        valueText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
        if (showDivider) PixelAodDivider()
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
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            val bodyModifier = if (scrollable) {
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(modifier = bodyModifier) {
                options.forEach { option ->
                    val selected = option.value == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = { onSelected(option.value) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelected(option.value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
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