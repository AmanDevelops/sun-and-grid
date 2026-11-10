package dev.amandevelops.solax.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.amandevelops.solax.ui.UsageDay
import dev.amandevelops.solax.ui.UsageWeek
import dev.amandevelops.solax.ui.number
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors
import java.time.YearMonth
import kotlin.math.roundToInt

fun Modifier.tap(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
    )
}

@Composable
fun SunGridCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    color: Color = LocalAppColors.current.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(color)
            .padding(padding),
        content = content,
    )
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.line,
    dash: Dp = 4.dp,
    gap: Dp = 6.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        val dashPx = dash.toPx()
        val gapPx = gap.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(dashPx, gapPx),
                0f,
            ),
        )
    }
}

@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulse: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "dot")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    )
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        drawCircle(color = color, radius = radius)
        if (pulse) {
            drawCircle(
                color = color.copy(alpha = (1f - ring) * 0.35f),
                radius = radius + ring * 3.5.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}

@Composable
fun AppHeader(
    state: dev.amandevelops.solax.ui.MonitorUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val today = java.time.LocalDate.now()
    val dateLabel = today.format(
        java.time.format.DateTimeFormatter.ofPattern(
            "EEEE, d MMMM",
            java.util.Locale.getDefault(),
        ),
    )
    val greeting = dev.amandevelops.solax.ui.greeting()
    val (statusText, statusColor, pulse) = when (state.status) {
        dev.amandevelops.solax.ui.MonitorStatus.ONLINE -> Triple("Inverter live", colors.ok, true)
        dev.amandevelops.solax.ui.MonitorStatus.DEMO -> Triple("Example data", colors.ok, true)
        dev.amandevelops.solax.ui.MonitorStatus.OFFLINE -> Triple("Inverter offline", colors.bad, false)
        dev.amandevelops.solax.ui.MonitorStatus.NEEDS_SETUP -> Triple("Not connected", colors.mute, false)
        dev.amandevelops.solax.ui.MonitorStatus.LOADING -> Triple("Connecting…", colors.sun, true)
    }
    AppHeader(
        dateLabel = dateLabel,
        greeting = greeting,
        statusText = statusText,
        statusColor = statusColor,
        pulse = pulse,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun AppHeader(
    dateLabel: String,
    greeting: String,
    statusText: String,
    statusColor: Color,
    pulse: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dateLabel, style = AppText.sub, color = colors.mute)
            Spacer(Modifier.height(4.dp))
            Text(
                text = greeting,
                style = AppText.header,
                color = colors.ink,
                maxLines = 2,
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.card)
                .tap(onOpenSettings)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PulsingDot(color = statusColor, size = 8.dp, pulse = pulse)
            Text(
                text = statusText,
                style = AppText.small.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SunSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
) {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "sun-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sun-angle",
    )
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = angle
                transformOrigin = TransformOrigin.Center
            },
    ) {
        val scale = this.size.width / 60f
        val center = Offset(30f * scale, 30f * scale)
        val sun = colors.sun
        for (i in 0 until 8) {
            rotate(i * 45f, pivot = center) {
                drawLine(
                    color = sun,
                    start = Offset(30f * scale, 5f * scale),
                    end = Offset(30f * scale, 13f * scale),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(color = sun, radius = 10f * scale, center = center)
    }
}

@Composable
fun FlowDiagram(
    panelWatts: Double,
    gridWatts: Double,
    exporting: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val regularTypeface = remember(context) {
        androidx.core.content.res.ResourcesCompat.getFont(context, dev.amandevelops.solax.R.font.bricolage_regular)
    }
    val boldTypeface = remember(context) {
        androidx.core.content.res.ResourcesCompat.getFont(context, dev.amandevelops.solax.R.font.bricolage_bold)
    }
    val transition = rememberInfiniteTransition(label = "flow")
    val dashPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dash",
    )

    val panel = panelWatts.coerceAtLeast(0.0)
    val grid = kotlin.math.abs(gridWatts)
    val percent = if (panel > 1.0) {
        (grid / panel * 100.0).roundToInt().coerceIn(0, 100)
    } else {
        0
    }

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(340f / 96f)) {
        val sx = size.width / 340f
        val sy = size.height / 96f
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy

        // Panels and grid backdrops.
        drawCircle(color = colors.sun.copy(alpha = 0.22f), radius = x(26f), center = Offset(x(60f), y(30f)))
        drawCircle(color = colors.ok.copy(alpha = 0.20f), radius = x(26f), center = Offset(x(280f), y(30f)))

        // Animated flow line.
        drawLine(
            color = colors.sun,
            start = Offset(x(96f), y(30f)),
            end = Offset(x(244f), y(30f)),
            strokeWidth = x(3f),
            cap = StrokeCap.Round,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(x(4f), x(6f)),
                if (exporting) -dashPhase * sx else dashPhase * sx,
            ),
        )

        // Panel icon.
        val iconStroke = x(1.8f)
        drawPath(
            path = Path().apply {
                moveTo(x(48f), y(38f))
                lineTo(x(52f), y(22f))
                lineTo(x(68f), y(22f))
                lineTo(x(72f), y(38f))
                close()
                moveTo(x(50f), y(30f))
                lineTo(x(70f), y(30f))
                moveTo(x(60f), y(22f))
                lineTo(x(60f), y(38f))
            },
            color = colors.ink,
            style = Stroke(width = iconStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Pylon icon.
        drawPath(
            path = Path().apply {
                moveTo(x(280f), y(18f))
                lineTo(x(280f), y(42f))
                moveTo(x(272f), y(42f))
                lineTo(x(280f), y(18f))
                lineTo(x(288f), y(42f))
                moveTo(x(273f), y(27f))
                lineTo(x(287f), y(27f))
                moveTo(x(275f), y(34f))
                lineTo(x(285f), y(34f))
            },
            color = colors.ink,
            style = Stroke(width = iconStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                typeface = regularTypeface
                textAlign = android.graphics.Paint.Align.CENTER
            }
            paint.color = colors.mute.toArgb()
            paint.textSize = 10.sp.toPx()
            canvas.nativeCanvas.drawText(
                if (exporting) "$percent% exported" else "$percent% imported",
                x(170f),
                y(22f),
                paint,
            )

            paint.color = colors.ink.toArgb()
            paint.textSize = 13.sp.toPx()
            paint.typeface = boldTypeface
            canvas.nativeCanvas.drawText(
                "${panel.roundToInt()} W",
                x(60f),
                y(82f),
                paint,
            )
            canvas.nativeCanvas.drawText(
                "${grid.roundToInt()} W",
                x(280f),
                y(82f),
                paint,
            )

            paint.color = colors.mute.toArgb()
            paint.textSize = 10.sp.toPx()
            paint.typeface = regularTypeface
            canvas.nativeCanvas.drawText("Panels", x(60f), y(94f), paint)
            canvas.nativeCanvas.drawText(
                if (exporting) "Sent to grid" else "From grid",
                x(280f),
                y(94f),
                paint,
            )
        }
    }
}

@Composable
fun RaceTrack(
    madeKwh: Double,
    usedKwh: Double?,
    ahead: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        started = true
    }
    val max = (kotlin.math.max(madeKwh, usedKwh ?: 0.0) * 1.06).coerceAtLeast(0.1)
    val fillTarget = (madeKwh / max).toFloat().coerceIn(0f, 1f)
    val markerTarget = ((usedKwh ?: 0.0) / max).toFloat().coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = if (started) fillTarget else 0f,
        animationSpec = tween(1300, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)),
        label = "race-fill",
    )
    val marker by animateFloatAsState(
        targetValue = if (started) markerTarget else 0f,
        animationSpec = tween(1300, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)),
        label = "race-marker",
    )

    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        val top = 9.dp.toPx()
        val trackHeight = 22.dp.toPx()
        val radius = trackHeight / 2f
        val trackColor = lerp(colors.line, colors.grid, 0.13f)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(radius, radius),
        )

        val fillWidth = size.width * fill
        if (fillWidth > 0f) {
            val fillBrush = if (ahead) {
                Brush.horizontalGradient(
                    colors = listOf(colors.sun, colors.ok),
                    startX = 0f,
                    endX = fillWidth,
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFD166), colors.sun),
                    startX = 0f,
                    endX = fillWidth,
                )
            }
            drawRoundRect(
                brush = fillBrush,
                topLeft = Offset(0f, top),
                size = Size(fillWidth, trackHeight),
                cornerRadius = CornerRadius(radius, radius),
            )

            // Sun orb sitting on the leading edge.
            val orbRadius = 18.dp.toPx()
            val orbCenter = Offset(fillWidth, top + trackHeight / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.30f to Color(0xFFFFF6D8),
                    0.34f to colors.sun,
                    0.52f to colors.sun,
                    0.66f to colors.sun.copy(alpha = 0f),
                    center = orbCenter,
                    radius = orbRadius,
                ),
                radius = orbRadius,
                center = orbCenter,
            )
        }

        // Latest UPPCL reading marker.
        if (usedKwh != null) {
            val markerX = (size.width * marker).coerceIn(1.5.dp.toPx(), size.width - 1.5.dp.toPx())
            val markerWidth = 3.dp.toPx()
            drawRoundRect(
                color = colors.grid,
                topLeft = Offset(markerX - markerWidth / 2f, 0f),
                size = Size(markerWidth, size.height),
                cornerRadius = CornerRadius(markerWidth / 2f, markerWidth / 2f),
            )
            drawCircle(
                color = colors.grid,
                radius = 5.5.dp.toPx(),
                center = Offset(markerX, 5.dp.toPx()),
            )
        }
    }
}

@Composable
fun WeeklyBars(
    week: List<UsageDay>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        grown = true
    }
    val max = (week.maxOfOrNull { it.kwh } ?: 0.0) * 1.08
    val barBrush = Brush.verticalGradient(
        colors = listOf(lerp(colors.grid, colors.sun, 0.55f), colors.grid),
    )
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        week.forEachIndexed { index, entry ->
            val selected = index == selectedIndex
            val height by animateFloatAsState(
                targetValue = if (grown && max > 0.0) {
                    (entry.kwh / max).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                animationSpec = tween(900, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)),
                label = "bar-${entry.day}",
            )
            val opacity by animateFloatAsState(
                targetValue = if (selected) 1f else 0.5f,
                animationSpec = tween(200),
                label = "bar-alpha-${entry.day}",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tap { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(colors.line),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(height)
                            .clip(CircleShape)
                            .alpha(opacity)
                            .background(barBrush),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = days[(entry.date.dayOfWeek.value % 7)],
                    style = AppText.micro,
                    color = if (selected) colors.ink else colors.mute,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = entry.day.toString(),
                    style = AppText.micro,
                    color = if (selected) colors.ink else colors.mute,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun MonthWeekBars(
    week: UsageWeek,
    month: YearMonth,
    maxKwh: Double,
    selectedDay: Int?,
    onSelect: (UsageDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var grown by remember(week.start) { mutableStateOf(false) }
    LaunchedEffect(week.start) {
        kotlinx.coroutines.delay(220)
        grown = true
    }
    val max = (maxKwh * 1.08).coerceAtLeast(0.1)
    val barBrush = Brush.verticalGradient(
        colors = listOf(lerp(colors.grid, colors.sun, 0.55f), colors.grid),
    )
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        week.days.forEachIndexed { index, entry ->
            val date = week.start.plusDays(index.toLong())
            val inMonth = YearMonth.from(date) == month
            val selected = entry != null && entry.day == selectedDay
            val height by animateFloatAsState(
                targetValue = if (grown && entry != null) {
                    (entry.kwh / max).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                animationSpec = tween(900, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)),
                label = "month-bar-${date}",
            )
            val opacity by animateFloatAsState(
                targetValue = if (selected) 1f else 0.5f,
                animationSpec = tween(200),
                label = "month-bar-alpha-${date}",
            )
            val columnModifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (entry != null) Modifier.tap { onSelect(entry) } else Modifier,
                )
            Column(
                modifier = columnModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(if (inMonth) colors.line else Color.Transparent),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (entry != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(height)
                                .clip(CircleShape)
                                .alpha(opacity)
                                .background(barBrush),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = days[(date.dayOfWeek.value % 7)],
                    style = AppText.micro,
                    color = when {
                        !inMonth -> colors.mute.copy(alpha = 0.35f)
                        selected -> colors.ink
                        else -> colors.mute
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (inMonth) date.dayOfMonth.toString() else "",
                    style = AppText.micro,
                    color = when {
                        !inMonth -> Color.Transparent
                        selected -> colors.ink
                        else -> colors.mute
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun GaugeCard(
    value: String,
    unit: String,
    label: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val progress by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)),
        label = "gauge-$label",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(100f / 58f)) {
                val sx = size.width / 100f
                val sy = size.height / 58f
                val path = Path().apply {
                    moveTo(10f * sx, 52f * sy)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = 10f * sx,
                            top = 12f * sy,
                            right = 90f * sx,
                            bottom = 92f * sy,
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false,
                    )
                }
                drawPath(
                    path = path,
                    color = colors.line,
                    style = Stroke(width = 9f * sx, cap = StrokeCap.Round),
                )
                if (progress > 0f) {
                    val measure = PathMeasure()
                    measure.setPath(path, false)
                    val segment = Path()
                    measure.getSegment(0f, measure.length * progress, segment, true)
                    drawPath(
                        path = segment,
                        color = color,
                        style = Stroke(width = 9f * sx, cap = StrokeCap.Round),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 46.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text = value, style = AppText.value22, color = colors.ink)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = AppText.micro.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.mute,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = AppText.small,
            color = colors.mute,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = label, style = AppText.sub, color = colors.mute, maxLines = 1)
        Text(
            text = value,
            style = AppText.value22,
            color = colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
fun ReadingRow(
    title: String,
    value: String,
    register: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = AppText.reading, color = colors.ink)
                Text(text = register, style = AppText.readingSmall, color = colors.mute)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = AppText.reading.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
fun SettingsShortcut(
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortcutCard(
        icon = AppIcons.Settings,
        iconTint = LocalAppColors.current.grid,
        title = "Connection & settings",
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun HistoryShortcut(
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortcutCard(
        icon = AppIcons.Clock,
        iconTint = LocalAppColors.current.ok,
        title = "Reading history",
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .tap(onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = iconTint,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
            Text(text = subtitle, style = AppText.sub, color = colors.mute)
        }
        androidx.compose.material3.Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colors.mute,
        )
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)
