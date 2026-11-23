package dev.codex.pixelaod

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
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
    val statusSuccess: Color,
    val statusWarning: Color
)

private val LocalFlClashSurfaces = staticCompositionLocalOf {
    FlClashSurfaces(
        isDark = false,
        page = Color(0xFFF2F3F5),
        card = Color(0xFFFAFBFD),
        block = Color(0xFFECEEF2),
        inset = Color(0xFFE2E5EA),
        floating = Color(0xFFFAFBFD),
        sheetPage = Color(0xFFF2F3F5),
        sheetCard = Color(0xFFFAFBFD),
        onSurface = Color(0xFF14161A),
        onSurfaceVariant = Color(0xFF636A75),
        divider = Color(0xFF14161A).copy(alpha = 0.14f),
        primary = Color(0xFF1A73E8),
        onPrimary = Color.White,
        statusSuccess = Color(0xFF2E7D32),
        statusWarning = Color(0xFFE65100)
    )
}

object PixelAodDesignSystem {
    @Immutable
    data class Spacing(
        val pageHorizontal: Dp = 16.dp,
        val pageBottom: Dp = 104.dp,
        val sectionGap: Dp = 18.dp,
        val sectionInnerGap: Dp = 8.dp,
        val gridGap: Dp = 12.dp,
        val rowHorizontal: Dp = 16.dp,
        val rowVertical: Dp = 12.dp,
        val leadingGap: Dp = 14.dp,
        val trailingGap: Dp = 10.dp
    )

    val spacing = Spacing()
    val cardShape = RoundedCornerShape(20.dp)
    val blockShape = RoundedCornerShape(14.dp)
    val iconWellShape = RoundedCornerShape(10.dp)
    val pillShape = RoundedCornerShape(999.dp)
    val dockShape = RoundedCornerShape(28.dp)

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp)
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
        dark -> darkColorScheme(primary = Color(0xFF669DF6), onPrimary = Color(0xFF0A192F))
        else -> lightColorScheme(primary = Color(0xFF1A73E8), onPrimary = Color.White)
    }

    val surfaces = if (dark) {
        FlClashSurfaces(
            isDark = true,
            page = Color(0xFF0F1012),
            card = Color(0xFF1E2024),
            block = Color(0xFF2B2E34),
            inset = Color(0xFF383C44),
            floating = Color(0xFF26292F),
            sheetPage = Color(0xFF1B1D21),
            sheetCard = Color(0xFF282B31),
            onSurface = Color(0xFFF5F6F8),
            onSurfaceVariant = Color(0xFF9BA1AC),
            divider = Color(0xFFF5F6F8).copy(alpha = 0.14f),
            primary = dynamicScheme.primary,
            onPrimary = dynamicScheme.onPrimary,
            statusSuccess = Color(0xFF66BB6A),
            statusWarning = Color(0xFFFFA726)
        )
    } else {
        FlClashSurfaces(
            isDark = false,
            page = Color(0xFFF2F3F5),
            card = Color(0xFFFAFBFD),
            block = Color(0xFFECEEF2),
            inset = Color(0xFFE2E5EA),
            floating = Color(0xFFFAFBFD),
            sheetPage = Color(0xFFF2F3F5),
            sheetCard = Color(0xFFFAFBFD),
            onSurface = Color(0xFF14161A),
            onSurfaceVariant = Color(0xFF636A75),
            divider = Color(0xFF14161A).copy(alpha = 0.14f),
            primary = dynamicScheme.primary,
            onPrimary = dynamicScheme.onPrimary,
            statusSuccess = Color(0xFF2E7D32),
            statusWarning = Color(0xFFE65100)
        )
    }

    val normalizedScheme = dynamicScheme.copy(
        background = surfaces.page,
        surface = surfaces.page,
        surfaceContainerLowest = surfaces.page,
        surfaceContainerLow = surfaces.card,
        surfaceContainer = surfaces.card,
        surfaceContainerHigh = surfaces.block,
        surfaceContainerHighest = surfaces.inset,
        onSurface = surfaces.onSurface,
        onSurfaceVariant = surfaces.onSurfaceVariant,
        outlineVariant = surfaces.divider
    )

    val baseTypography = Typography()
    val typography = baseTypography.copy(
        headlineMedium = baseTypography.headlineMedium.copy(
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        ),
        titleLarge = baseTypography.titleLarge.copy(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = baseTypography.titleMedium.copy(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = baseTypography.bodyLarge.copy(
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        bodyMedium = baseTypography.bodyMedium.copy(
            fontSize = 13.5.sp,
            lineHeight = 18.sp
        ),
        bodySmall = baseTypography.bodySmall.copy(
            fontSize = 12.5.sp,
            lineHeight = 16.5.sp
        ),
        labelLarge = baseTypography.labelLarge.copy(
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        ),
        labelMedium = baseTypography.labelMedium.copy(
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium
        ),
        labelSmall = baseTypography.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium
        )
    )

    CompositionLocalProvider(LocalFlClashSurfaces provides surfaces) {
        MaterialTheme(
            colorScheme = normalizedScheme,
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
                    .heightIn(min = if (isRoot) 62.dp else 54.dp)
                    .padding(
                        start = if (isRoot) 20.dp else 6.dp,
                        end = 12.dp,
                        top = if (isRoot) 8.dp else 2.dp,
                        bottom = 4.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = backDescription,
                            tint = s.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = PixelAodDesignSystem.pillShape,
                            color = s.inset,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = s.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (actionIcon != null && onAction != null) {
                    Surface(
                        shape = CircleShape,
                        color = s.card,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAction)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = actionDescription,
                                tint = s.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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
                    .padding(end = 18.dp, bottom = 88.dp)
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
            color = s.onSurfaceVariant,
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
    val elevation = if (s.isDark) 0.dp else 1.5.dp
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = PixelAodDesignSystem.cardShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
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
                .heightIn(min = 112.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(s.block),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = s.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = s.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (statusDotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.5.sp),
                    color = s.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sublabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sublabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
    val elevation = if (s.isDark) 0.dp else 1.5.dp
    val dotColor = if (checked) s.statusSuccess else s.onSurfaceVariant.copy(alpha = 0.45f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = PixelAodDesignSystem.cardShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(PixelAodDesignSystem.cardShape)
            .clickable { onCheckedChange(!checked) },
        shape = PixelAodDesignSystem.cardShape,
        color = s.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAodHeroMark(modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = s.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked) s.statusSuccess else s.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
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
    val elevation = if (s.isDark) 0.dp else 1.5.dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = PixelAodDesignSystem.cardShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = PixelAodDesignSystem.cardShape,
        color = s.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(s.block),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = s.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = s.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PixelAodDesignSystem.blockShape)
                    .background(s.block)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val selected = option.value == selectedValue
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) s.primary else Color.Transparent,
                        animationSpec = tween(180),
                        label = "segBg"
                    )
                    val fgColor by animateColorAsState(
                        targetValue = if (selected) s.onPrimary else s.onSurfaceVariant,
                        animationSpec = tween(180),
                        label = "segFg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { onSelected(option.value) }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
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
            .shadow(
                elevation = if (s.isDark) 6.dp else 8.dp,
                shape = PixelAodDesignSystem.pillShape,
                ambientColor = s.primary.copy(alpha = 0.25f),
                spotColor = s.primary.copy(alpha = 0.28f)
            )
            .clip(PixelAodDesignSystem.pillShape)
            .clickable(onClick = onClick),
        shape = PixelAodDesignSystem.pillShape,
        color = s.primary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = s.onPrimary,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                color = s.onPrimary,
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
        color = s.card,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = s.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
        colors = SwitchDefaults.colors(
            checkedThumbColor = s.onPrimary,
            checkedTrackColor = s.primary,
            uncheckedThumbColor = if (s.isDark) Color(0xFFD5D8DF) else Color.White,
            uncheckedTrackColor = s.inset,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = s.primary.copy(alpha = 0.35f),
            disabledUncheckedTrackColor = s.inset.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun PixelAodHeroMark(modifier: Modifier = Modifier) {
    val s = PixelAodDesignSystem.surfaces
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(s.block)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
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
                color = s.primary,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            val center = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.34f)
            val radius = size.minDimension * 0.17f
            drawCircle(color = s.inset, radius = radius, center = center)
            drawCircle(
                color = s.onSurface,
                radius = radius,
                center = center,
                style = Stroke(width = size.minDimension * 0.04f)
            )
            drawLine(
                color = s.onSurface,
                start = center,
                end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.52f),
                strokeWidth = size.minDimension * 0.04f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = s.onSurface,
                start = center,
                end = androidx.compose.ui.geometry.Offset(
                    center.x + radius * 0.48f,
                    center.y + radius * 0.20f
                ),
                strokeWidth = size.minDimension * 0.04f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PixelAodDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = PixelAodDesignSystem.surfaces.divider
    )
}

@Composable
private fun PixelAodLeadingIcon(icon: ImageVector, enabled: Boolean) {
    val s = PixelAodDesignSystem.surfaces
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(PixelAodDesignSystem.iconWellShape)
            .background(if (enabled) s.block else s.block.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(18.dp)
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
                .heightIn(min = 58.dp)
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
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!valueText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 112.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) s.onSurfaceVariant.copy(alpha = 0.65f)
                else s.onSurfaceVariant.copy(alpha = 0.28f),
                modifier = Modifier.size(19.dp)
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
                .heightIn(min = 58.dp)
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
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 1,
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
                .heightIn(min = 58.dp)
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
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                modifier = Modifier.widthIn(max = 132.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) s.onSurfaceVariant else s.onSurfaceVariant.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) s.onSurfaceVariant.copy(alpha = 0.55f)
                else s.onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAodLeadingIcon(icon, enabled)
                Spacer(modifier = Modifier.width(PixelAodDesignSystem.spacing.leadingGap))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) s.onSurface else s.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = s.block,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelLarge,
                        color = s.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier.padding(start = 46.dp, top = 2.dp),
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
    val platterColor = if (s.isDark) s.inset else s.block
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = if (s.isDark) 8.dp else 10.dp,
                    shape = PixelAodDesignSystem.dockShape,
                    ambientColor = Color.Black.copy(alpha = if (s.isDark) 0.45f else 0.10f),
                    spotColor = Color.Black.copy(alpha = if (s.isDark) 0.45f else 0.12f)
                ),
            shape = PixelAodDesignSystem.dockShape,
            color = s.floating,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = item.value == selectedValue
                    val itemBg by animateColorAsState(
                        targetValue = if (selected) platterColor else Color.Transparent,
                        animationSpec = tween(180),
                        label = "dockPlatter"
                    )
                    val contentTint by animateColorAsState(
                        targetValue = if (selected) s.primary else s.onSurfaceVariant,
                        animationSpec = tween(180),
                        label = "dockTint"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(23.dp))
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
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
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
                .padding(bottom = 20.dp),
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
                                .clickable { onSelected(option.value) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = s.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (option.subtitle.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = s.onSurfaceVariant,
                                        maxLines = 1,
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
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = s.divider
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
        shape = RoundedCornerShape(24.dp),
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
                            periodSelectorBorderColor = s.divider,
                            periodSelectorSelectedContainerColor = s.primary,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = s.onPrimary,
                            periodSelectorUnselectedContentColor = s.onSurfaceVariant,
                            timeSelectorSelectedContainerColor = s.primary,
                            timeSelectorUnselectedContainerColor = s.block,
                            timeSelectorSelectedContentColor = s.onPrimary,
                            timeSelectorUnselectedContentColor = s.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(confirmLabel, color = s.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = s.onSurfaceVariant)
            }
        }
    )
}
