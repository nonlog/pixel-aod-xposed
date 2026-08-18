package dev.codex.pixelaod

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
        val pageHorizontal: Dp = 16.dp,
        val pageBottom: Dp = 28.dp,
        val titleHorizontal: Dp = 8.dp,
        val sectionGap: Dp = 24.dp,
        val sectionInnerGap: Dp = 10.dp,
        val rowHorizontal: Dp = 16.dp,
        val rowVertical: Dp = 14.dp,
        val leadingGap: Dp = 14.dp,
        val trailingGap: Dp = 12.dp
    )

    @Immutable
    data class Motion(
        val stateChangeMillis: Int = 300,
        val contentChangeMillis: Int = 450
    )

    val spacing = Spacing()
    val motion = Motion()
    val groupShape = RoundedCornerShape(22.dp)
    val heroShape = RoundedCornerShape(28.dp)
    val iconShape = RoundedCornerShape(14.dp)

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = groupShape
    )

    fun groupColor(scheme: ColorScheme): Color = scheme.surfaceContainerLowest
    fun pageColor(scheme: ColorScheme): Color = scheme.background
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
    val baseColors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    // COUI Expressive follows the device wallpaper palette rather than pinning a fixed teal/neutral
    // theme. Keep the complete dynamic Material You role mapping intact on Android 12+; component
    // hierarchy below selects the appropriate surface/container roles without replacing their hues.
    val colors = baseColors
    val baseTypography = Typography()
    val typography = baseTypography.copy(
        displaySmall = baseTypography.displaySmall.copy(
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Normal
        ),
        titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Medium),
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = backDescription,
                            tint = scheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                if (actionIcon != null && onAction != null) {
                    IconButton(onClick = onAction) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionDescription,
                            tint = scheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
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
                Spacer(modifier = Modifier.height(34.dp))
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
                if (subtitle.isNotBlank()) {
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
                }
                content()
            }
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        content()
    }
}

@Composable
fun PixelAodGroup(
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PixelAodDesignSystem.groupShape)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
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
        color = MaterialTheme.colorScheme.secondaryContainer,
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
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
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
fun PixelAodInfoBanner(text: String) {
    Surface(
        shape = PixelAodDesignSystem.heroShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        )
    }
}

@Composable
private fun PixelAodSwitch(checked: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 25.dp else 3.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = PixelAodDesignSystem.motion.stateChangeMillis,
            easing = FastOutSlowInEasing
        ),
        label = "coui-switch-thumb"
    )
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 36.dp)
            .clip(CircleShape)
            .background(if (checked) scheme.primary else Color.Transparent)
            .border(
                width = 2.5.dp,
                color = if (checked) scheme.primary else scheme.outline,
                shape = CircleShape
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 3.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(if (checked) scheme.primaryContainer else scheme.outline),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = null,
                tint = if (checked) scheme.onPrimaryContainer else scheme.surface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun PixelAodHeroMark(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = scheme.secondaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 5.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = PixelAodDesignSystem.heroShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
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
fun PixelAodNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueText: String? = null,
    showDivider: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!valueText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 104.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp)
            )
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
    Surface(
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            PixelAodSwitch(checked)
        }
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
    Surface(
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                valueText,
                modifier = Modifier.widthIn(max = 150.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
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
    Surface(
        color = PixelAodDesignSystem.groupColor(MaterialTheme.colorScheme),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAodLeadingIcon(icon, enabled)
                Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    valueText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.padding(start = 36.dp, top = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
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
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
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
