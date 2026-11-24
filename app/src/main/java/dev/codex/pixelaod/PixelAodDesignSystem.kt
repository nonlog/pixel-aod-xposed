package dev.codex.pixelaod

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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

@Immutable
data class FlClashSurfaces(
    val isDark: Boolean,
    val page: Color,
    val card: Color,
    val block: Color,
    val inset: Color,
    val floating: Color,
    val sheetPage: Color,
    val sheetCard: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val divider: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val statusSuccess: Color,
    val statusWarning: Color
)

private val LocalFlClashSurfaces = staticCompositionLocalOf {
    val light = lightColorScheme()
    FlClashSurfaces(
        isDark = false,
        page = light.surfaceContainer,
        card = light.surfaceContainerLowest,
        block = light.surfaceContainerHigh,
        inset = light.surfaceContainerHighest,
        floating = light.surfaceContainerHigh,
        sheetPage = light.surfaceContainerLow,
        sheetCard = light.surfaceContainerLowest,
        onSurface = light.onSurface,
        onSurfaceVariant = light.onSurfaceVariant,
        divider = light.outlineVariant,
        primary = light.primary,
        onPrimary = light.onPrimary,
        primaryContainer = light.primaryContainer,
        onPrimaryContainer = light.onPrimaryContainer,
        secondaryContainer = light.secondaryContainer,
        onSecondaryContainer = light.onSecondaryContainer,
        tertiaryContainer = light.tertiaryContainer,
        onTertiaryContainer = light.onTertiaryContainer,
        outline = light.outline,
        outlineVariant = light.outlineVariant,
        statusSuccess = Color(0xFF1E8E3E),
        statusWarning = Color(0xFFE65100)
    )
}

object PixelAodDesignSystem {
    @Immutable
    data class Spacing(
        val pageHorizontal: Dp = 16.dp,
        val pageBottom: Dp = 112.dp,
        val sectionGap: Dp = 20.dp,
        val sectionInnerGap: Dp = 10.dp,
        val gridGap: Dp = 12.dp,
        val rowHorizontal: Dp = 18.dp,
        val rowVertical: Dp = 14.dp,
        val leadingGap: Dp = 16.dp,
        val trailingGap: Dp = 12.dp
    )

    val spacing = Spacing()
    val cardShape = RoundedCornerShape(26.dp)
    val heroShape = RoundedCornerShape(30.dp)
    val blockShape = RoundedCornerShape(20.dp)
    val iconWellShape = CircleShape
    val pillShape = CircleShape
    val dockShape = CircleShape

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp)
    )

    val surfaces: FlClashSurfaces
        @Composable get() = LocalFlClashSurfaces.current
}

@Immutable
data class PixelAodSelectionOption(
    val value: String,
    val label: String,
    val subtitle: String = ""
)

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
    val dynamicScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }

    // Authentic Material 3 Expressive (M3E) tonal surface hierarchy derived directly from
    // the device's Dynamic Color (Monet) palette without flattening to neutral grays.
    val surfaces = FlClashSurfaces(
        isDark = dark,
        page = if (dark) dynamicScheme.surface else dynamicScheme.surfaceContainer,
        card = if (dark) dynamicScheme.surfaceContainerHigh else dynamicScheme.surfaceContainerLowest,
        block = if (dark) dynamicScheme.surfaceContainerHighest else dynamicScheme.surfaceContainerHigh,
        inset = dynamicScheme.surfaceContainerHighest,
        floating = if (dark) dynamicScheme.surfaceContainerHighest else dynamicScheme.surfaceContainerHigh,
        sheetPage = dynamicScheme.surfaceContainerLow,
        sheetCard = if (dark) dynamicScheme.surfaceContainerHigh else dynamicScheme.surfaceContainerLowest,
        onSurface = dynamicScheme.onSurface,
        onSurfaceVariant = dynamicScheme.onSurfaceVariant,
        divider = dynamicScheme.outlineVariant,
        primary = dynamicScheme.primary,
        onPrimary = dynamicScheme.onPrimary,
        primaryContainer = dynamicScheme.primaryContainer,
        onPrimaryContainer = dynamicScheme.onPrimaryContainer,
        secondaryContainer = dynamicScheme.secondaryContainer,
        onSecondaryContainer = dynamicScheme.onSecondaryContainer,
        tertiaryContainer = dynamicScheme.tertiaryContainer,
        onTertiaryContainer = dynamicScheme.onTertiaryContainer,
        outline = dynamicScheme.outline,
        outlineVariant = dynamicScheme.outlineVariant,
        statusSuccess = if (dark) Color(0xFF81C995) else Color(0xFF1E8E3E),
        statusWarning = if (dark) Color(0xFFFFB74D) else Color(0xFFE65100)
    )

    val baseTypography = Typography()
    val typography = baseTypography.copy(
        headlineMedium = baseTypography.headlineMedium.copy(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp
        ),
        titleLarge = baseTypography.titleLarge.copy(
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        ),
        titleMedium = baseTypography.titleMedium.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = baseTypography.bodyLarge.copy(
            fontSize = 15.5.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = baseTypography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 19.sp
        ),
        bodySmall = baseTypography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 17.5.sp
        ),
        labelLarge = baseTypography.labelLarge.copy(
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp
        ),
        labelMedium = baseTypography.labelMedium.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        ),
        labelSmall = baseTypography.labelSmall.copy(
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium
        )
    )

    CompositionLocalProvider(LocalFlClashSurfaces provides surfaces) {
        MaterialTheme(
            colorScheme = dynamicScheme,
            typography = typography,
            shapes = PixelAodDesignSystem.shapes,
            content = content
        )
    }
}

@Composable
fun PixelAodPage(
    title: String,
    subtitle: String = "",
    badgeText: String? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    backDescription: String? = null,
    floatingAction: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val s = PixelAodDesignSystem.surfaces
    val isRoot = onBack == null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(s.page)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isRoot) 72.dp else 60.dp)
                    .padding(
                        start = if (isRoot) 20.dp else 8.dp,
                        end = 16.dp,
                        top = if (isRoot) 12.dp else 4.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Surface(
                        shape = CircleShape,
                        color = s.card,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = backDescription,
                                tint = s.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            style = if (isRoot) MaterialTheme.typography.headlineMedium
                            else MaterialTheme.typography.titleLarge,
                            color = s.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = s.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = PixelAodDesignSystem.pillShape,
                            color = s.secondaryContainer
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = s.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                if (actionIcon != null && onAction != null) {
                    Surface(
                        shape = CircleShape,
                        color = s.secondaryContainer,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAction)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = actionDescription,
                                tint = s.onSecondaryContainer,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PixelAodDesignSystem.spacing.pageHorizontal)
                    .padding(top = 6.dp, bottom = PixelAodDesignSystem.spacing.pageBottom)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.sectionGap),
                content = content
            )
        }

        if (floatingAction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 94.dp)
            ) {
                floatingAction()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            bottomBar()
        }
    }
}

@Composable
fun PixelAodSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val s = PixelAodDesignSystem.surfaces
    Column(
        verticalArrangement = Arrangement.spacedBy(PixelAodDesignSystem.spacing.sectionInnerGap),
        modifier = Modifier.animateContentSize()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = s.primary,
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
    val s = PixelAodDesignSystem.surfaces
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = PixelAodDesignSystem.cardShape,
        color = s.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun FlClashDashboardCard(
    icon: ImageVector,
    label: String,
    value: String,
    sublabel: String = "",
    statusDotColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val s = PixelAodDesignSystem.surfaces
    Surface(
        modifier = modifier
            .clip(PixelAodDesignSystem.cardShape)
            .clickable(onClick = onClick),
        shape = PixelAodDesignSystem.cardShape,
        color = s.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 126.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(s.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = s.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (statusDotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = s.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = s.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sublabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sublabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = s.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FlClashStatusCard(
    title: String,
    statusText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val s = PixelAodDesignSystem.surfaces
    val containerColor by animateColorAsState(
        targetValue = if (checked) s.primaryContainer else s.block,
        animationSpec = tween(220),
        label = "heroContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (checked) s.onPrimaryContainer else s.onSurface,
        animationSpec = tween(220),
        label = "heroContent"
    )
    val dotColor = if (checked) s.primary else s.onSurfaceVariant.copy(alpha = 0.55f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PixelAodDesignSystem.heroShape)
            .clickable { onCheckedChange(!checked) },
        shape = PixelAodDesignSystem.heroShape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodHeroMark(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = contentColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            PixelAodSwitch(checked = checked, enabled = true, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun FlClashSegmentedCard(
    icon: ImageVector,
    title: String,
    options: List<PixelAodSelectionOption>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    val s = PixelAodDesignSystem.surfaces
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PixelAodDesignSystem.cardShape,
        color = s.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(s.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = s.onTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = s.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(s.block)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val selected = option.value == selectedValue
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) s.secondaryContainer else Color.Transparent,
                        animationSpec = tween(200),
                        label = "segBg"
                    )
                    val fgColor by animateColorAsState(
                        targetValue = if (selected) s.onSecondaryContainer else s.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "segFg"
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { onSelected(option.value) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = fgColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = fgColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlClashActionFab(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val s = PixelAodDesignSystem.surfaces
    Surface(
        modifier = Modifier
            .clip(PixelAodDesignSystem.pillShape)
            .clickable(onClick = onClick),
        shape = PixelAodDesignSystem.pillShape,
        color = s.primaryContainer,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = s.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = s.onPrimaryContainer,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PixelAodInfoBanner(text: String) {
    val s = PixelAodDesignSystem.surfaces
    Surface(
        shape = PixelAodDesignSystem.cardShape,
        color = s.secondaryContainer.copy(alpha = if (s.isDark) 0.45f else 0.55f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = s.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun PixelAodSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val s = PixelAodDesignSystem.surfaces
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = s.onPrimary,
            checkedTrackColor = s.primary,
            checkedIconColor = s.primary,
            uncheckedThumbColor = s.outline,
            uncheckedTrackColor = s.block,
            uncheckedBorderColor = s.outline,
            disabledCheckedTrackColor = s.primary.copy(alpha = 0.35f),
            disabledUncheckedTrackColor = s.block.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = s.outline.copy(alpha = 0.35f)
        )
    )
}

@Composable
fun PixelAodHeroMark(modifier: Modifier = Modifier) {
    val s = PixelAodDesignSystem.surfaces
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(s.primary)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(9.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = size.minDimension * 0.14f
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
                color = s.onPrimary,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            val center = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.34f)
            val radius = size.minDimension * 0.17f
            drawCircle(color = s.primaryContainer, radius = radius, center = center)
            drawCircle(
                color = s.onPrimary,
                radius = radius,
                center = center,
                style = Stroke(width = size.minDimension * 0.045f)
            )
            drawLine(
                color = s.onPrimaryContainer,
                start = center,
                end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.52f),
                strokeWidth = size.minDimension * 0.045f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = s.onPrimaryContainer,
                start = center,
                end = androidx.compose.ui.geometry.Offset(
                    center.x + radius * 0.48f,
                    center.y + radius * 0.20f
                ),
                strokeWidth = size.minDimension * 0.045f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PixelAodDivider() {
    // M3E Android 16 segmented list separator: crisp 2dp page-colored gap between stacked items.
    HorizontalDivider(
        thickness = 2.dp,
        color = PixelAodDesignSystem.surfaces.page
    )
}

@Composable
private fun PixelAodLeadingIcon(icon: ImageVector, enabled: Boolean) {
    val s = PixelAodDesignSystem.surfaces
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(PixelAodDesignSystem.iconWellShape)
            .background(
                if (enabled) s.secondaryContainer
                else s.block.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) s.onSecondaryContainer else s.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp)
        )
    }
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
    val s = PixelAodDesignSystem.surfaces
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!valueText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = s.block
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .widthIn(max = 116.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) s.onSurfaceVariant.copy(alpha = 0.7f)
                else s.onSurfaceVariant.copy(alpha = 0.28f),
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
    val s = PixelAodDesignSystem.surfaces
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 4,
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
    val s = PixelAodDesignSystem.surfaces
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodLeadingIcon(icon, enabled)
            Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = s.block
            ) {
                Text(
                    text = valueText,
                    modifier = Modifier
                        .widthIn(max = 132.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) s.onSurfaceVariant.copy(alpha = 0.65f)
                else s.onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(19.dp)
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
    val s = PixelAodDesignSystem.surfaces
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAodLeadingIcon(icon, enabled)
                Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = CircleShape,
                    color = s.primaryContainer
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelMedium,
                        color = s.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.padding(start = 52.dp, top = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = s.primary,
                    activeTrackColor = s.primary,
                    inactiveTrackColor = s.inset
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
    val s = PixelAodDesignSystem.surfaces
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = PixelAodDesignSystem.dockShape,
            color = s.floating,
            border = BorderStroke(1.dp, s.outlineVariant.copy(alpha = 0.55f)),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = item.value == selectedValue
                    val itemBg by animateColorAsState(
                        targetValue = if (selected) s.secondaryContainer else Color.Transparent,
                        animationSpec = tween(200),
                        label = "dockPlatter"
                    )
                    val contentTint by animateColorAsState(
                        targetValue = if (selected) s.onSecondaryContainer else s.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "dockTint"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(itemBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelected(item.value) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = contentTint,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val s = PixelAodDesignSystem.surfaces
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = s.sheetPage,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = s.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                shape = PixelAodDesignSystem.cardShape,
                color = s.sheetCard,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val listModifier = if (scrollable) {
                    Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
                Column(modifier = listModifier) {
                    options.forEachIndexed { index, option ->
                        val selected = option.value == current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) s.secondaryContainer.copy(alpha = 0.45f)
                                    else Color.Transparent
                                )
                                .clickable { onSelected(option.value) }
                                .padding(horizontal = 18.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selected) s.onSecondaryContainer else s.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (option.subtitle.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = s.onSurfaceVariant,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (selected) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = s.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < options.lastIndex) {
                            HorizontalDivider(
                                thickness = 2.dp,
                                color = s.sheetPage
                            )
                        }
                    }
                }
            }
        }
    }
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
    val s = PixelAodDesignSystem.surfaces
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = s.floating,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = s.onSurface
            )
        },
        text = {
            Surface(
                shape = PixelAodDesignSystem.cardShape,
                color = s.card,
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
                            clockDialColor = s.block,
                            clockDialSelectedContentColor = s.onPrimary,
                            clockDialUnselectedContentColor = s.onSurface,
                            selectorColor = s.primary,
                            containerColor = Color.Transparent,
                            periodSelectorBorderColor = s.outlineVariant,
                            periodSelectorSelectedContainerColor = s.primaryContainer,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = s.onPrimaryContainer,
                            periodSelectorUnselectedContentColor = s.onSurfaceVariant,
                            timeSelectorSelectedContainerColor = s.primaryContainer,
                            timeSelectorUnselectedContainerColor = s.block,
                            timeSelectorSelectedContentColor = s.onPrimaryContainer,
                            timeSelectorUnselectedContentColor = s.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(confirmLabel, color = s.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = s.onSurfaceVariant)
            }
        }
    )
}
