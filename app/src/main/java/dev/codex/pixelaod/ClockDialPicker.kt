package dev.codex.pixelaod

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A custom clock-dial (表盘) time picker dialog that replaces the stock
 * [android.app.TimePickerDialog].  The user drags on the circular face to
 * pick hours (1–12, outer ring) or minutes (0–59, inner ring).
 *
 * The dial always shows 1–12 on the face; [is24Hour] and the AM/PM toggle
 * control how the selected hour is mapped to/from the 24-hour value passed
 * to [onTimeSelected].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockDialPickerDialog(
    title: String,
    initialHour: Int,       // 0..23
    initialMinute: Int,     // 0..59
    is24Hour: Boolean = true,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val initIsAM = initialHour < 12
    val initHour12 = when {
        initialHour == 0  -> 12
        initialHour > 12  -> initialHour - 12
        else              -> initialHour
    }

    var hour12 by remember { mutableIntStateOf(initHour12) }    // 1..12
    var minute by remember { mutableIntStateOf(initialMinute) }  // 0..59
    var isAM by remember { mutableStateOf(initIsAM) }
    var isHourMode by remember { mutableStateOf(true) }

    fun hour24(): Int = when {
        isAM && hour12 == 12  -> 0
        isAM                  -> hour12
        !isAM && hour12 == 12 -> 12
        else                  -> hour12 + 12
    }

    fun handAngleDeg(): Float =
        if (isHourMode) (hour12 % 12) * 30f else minute * 6f

    var animatedAngle by remember { mutableFloatStateOf(handAngleDeg()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── Digital readout ──
                val timeLabel = if (is24Hour) {
                    String.format("%02d:%02d", hour24(), minute)
                } else {
                    String.format("%d:%02d %s", hour12, minute, if (isAM) "AM" else "PM")
                }
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Hour / Minute toggle chips ──
                Row(horizontalArrangement = Arrangement.Center) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    FilterChip(
                        selected = isHourMode,
                        onClick = { isHourMode = true },
                        label = { Text("Hour") },
                        colors = chipColors
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = !isHourMode,
                        onClick = { isHourMode = false },
                        label = { Text("Minute") },
                        colors = chipColors
                    )
                }

                // ── AM / PM chips (24h mode only) ──
                if (is24Hour) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        FilterChip(
                            selected = isAM,
                            onClick = { isAM = true },
                            label = { Text("AM") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(
                            selected = !isAM,
                            onClick = { isAM = false },
                            label = { Text("PM") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Clock face ──
                val primary = MaterialTheme.colorScheme.primary
                val onSurface = MaterialTheme.colorScheme.onSurface
                val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
                val outline = MaterialTheme.colorScheme.outline
                val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                ) {
                    ClockDialFace(
                        activeAngle = animatedAngle,
                        isHourMode = isHourMode,
                        primary = primary,
                        onSurface = onSurface,
                        surfaceVariant = surfaceVar,
                        outline = outline,
                        onAngleChanged = { newDeg ->
                            animatedAngle = newDeg
                            if (isHourMode) {
                                val raw = (newDeg / 30f).roundToInt() % 12
                                hour12 = if (raw == 0) 12 else raw
                            } else {
                                val raw = (newDeg / 6f).roundToInt()
                                minute = if (raw >= 60) 0 else raw
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isHourMode) "Drag to set hour" else "Drag to set minute",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVar
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(hour24(), minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ── Canvas clock face ────────────────────────────────────────────────

@Composable
private fun ClockDialFace(
    activeAngle: Float,
    isHourMode: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceVariant: Color,
    outline: Color,
    onAngleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val labelFontSizePx = with(density) { 13.sp.toPx() }

    Canvas(
        modifier = modifier
            .pointerInput(isHourMode) {
                detectDragGestures { change, _ ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val deg = pointToClockAngle(c, change.position)
                    onAngleChanged(deg)
                }
            }
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val strokeW = 2.dp.toPx()

        // ── Tick marks (0–59 minutes) ──
        for (i in 0 until 60) {
            val angRad = clockDegToRad(i * 6f)
            val isMajor = i % 5 == 0
            val tickOuter: Float
            val tickInner: Float

            if (isHourMode) {
                // Outer ring: hour ticks at 5-min major, minor at each minute
                tickOuter = if (isMajor) r * 0.88f else r * 0.85f
                tickInner = r * 0.82f
            } else {
                // Inner ring: minute ticks at 5-min major, minor at each minute
                tickOuter = if (isMajor) r * 0.68f else r * 0.64f
                tickInner = r * 0.58f
            }

            val start = polar(c, tickInner, angRad)
            val end   = polar(c, tickOuter, angRad)
            drawLine(
                color = if (isMajor) onSurface else outline,
                start = start, end = end,
                strokeWidth = if (isMajor) strokeW * 1.4f else strokeW * 0.5f,
                cap = StrokeCap.Round
            )
        }

        // ── Hour number labels (1–12 on the outer ring) ──
        val labelPaint = Paint().apply {
            color = onSurface.toArgb()
            textSize = labelFontSizePx
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        for (h in 1..12) {
            val angRad = clockDegToRad(h * 30f)
            val labelR = r * 0.73f
            val pos = polar(c, labelR, angRad)
            val fm = labelPaint.fontMetrics
            val textY = pos.y - (fm.ascent + fm.descent) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                h.toString(), pos.x, textY, labelPaint
            )
        }

        // ── Highlight arc for the active segment ──
        val arcR = if (isHourMode) r * 0.85f else r * 0.61f
        val sweep = if (isHourMode) 28f else 5f
        val arcStartDeg = activeAngle - sweep / 2f
        drawArc(
            color = primary.copy(alpha = 0.22f),
            startAngle = arcStartDeg + 90f,   // Compose: 0° = 3 o'clock
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - arcR, c.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 10.dp.toPx(), cap = StrokeCap.Round
            )
        )

        // ── Selection hand ──
        val handAngRad = clockDegToRad(activeAngle)
        val handLen = if (isHourMode) r * 0.48f else r * 0.55f
        val handEnd = polar(c, handLen, handAngRad)
        drawLine(
            color = primary,
            start = c, end = handEnd,
            strokeWidth = if (isHourMode) 4.dp.toPx() else 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // ── Center dot ──
        drawCircle(primary, radius = 8.dp.toPx(), center = c)
        drawCircle(surfaceVariant, radius = 4.dp.toPx(), center = c)
    }
}

// ── Geometry helpers ─────────────────────────────────────────────────

/** Convert clock degrees (0° = 12 o'clock, CW) → canvas radians (0 = 3 o'clock, CW). */
private fun clockDegToRad(deg: Float): Float =
    Math.toRadians((deg - 90f).toDouble()).toFloat()

/** Point at distance [r] from [center] at canvas-radians angle [rad]. */
private fun polar(center: Offset, r: Float, rad: Float): Offset =
    Offset(center.x + r * cos(rad), center.y + r * sin(rad))

/** Touch point → clock angle in degrees (0° = 12 o'clock, CW). */
private fun pointToClockAngle(center: Offset, point: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    return ((deg + 90f) % 360f + 360f) % 360f
}
