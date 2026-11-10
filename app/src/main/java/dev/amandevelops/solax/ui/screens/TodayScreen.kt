package dev.amandevelops.solax.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.amandevelops.solax.data.Analytics
import dev.amandevelops.solax.ui.MonitorUiState
import dev.amandevelops.solax.ui.SunTimes
import dev.amandevelops.solax.ui.SunTimesCalculator
import dev.amandevelops.solax.ui.UsageDay
import dev.amandevelops.solax.ui.UsageMonth
import dev.amandevelops.solax.ui.UsageWeek
import dev.amandevelops.solax.ui.components.AppHeader
import dev.amandevelops.solax.ui.components.DashedDivider
import dev.amandevelops.solax.ui.components.FlowDiagram
import dev.amandevelops.solax.ui.components.MonthWeekBars
import dev.amandevelops.solax.ui.components.RaceTrack
import dev.amandevelops.solax.ui.components.SunGridCard
import dev.amandevelops.solax.ui.components.SunSpinner
import dev.amandevelops.solax.ui.number
import dev.amandevelops.solax.ui.shortDate
import dev.amandevelops.solax.ui.sunTimeLabel
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TodayScreen(
    state: MonitorUiState,
    usage: UsageMonth,
    usageIsDemo: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val reading = state.reading
    val today = LocalDate.now()
    val sunTimes = remember(today) { SunTimesCalculator.forDate(today) }
    val now = LocalTime.now()
    val daylight = sunTimes.daylightProgress(LocalTime.of(now.hour, now.minute))

    val dailyKwh = reading?.dailyYieldKwh ?: 0.0
    val lifetimeKwh = reading?.totalYieldKwh ?: 0.0
    val powerW = reading?.powerW ?: 0.0
    val gridExportW = reading?.let { Analytics.gridExportW(it, state.settings.invertGridSign) } ?: 0.0
    // This installation feeds all generated solar straight to the grid, so the
    // flow card shows the full array output leaving through the meter.
    val generating = powerW > 30.0
    val exporting = generating || gridExportW > 0.0
    val gridFlowW = if (generating) powerW else abs(gridExportW)

    val hasUsage = usage.lastPostedDay != null
    val latestDay = usage.lastPostedDay
    val latestUsage = latestDay?.let { usage.kwhOn(it) }
    val monthTotal = usage.totalKwh
    val weeks = remember(usage) { usage.weeksToDate() }
    var selectedDay by rememberSaveable(usage.yearMonth) {
        mutableIntStateOf(latestDay ?: 0)
    }
    LaunchedEffect(latestDay) {
        if (latestDay != null && usage.kwhOn(selectedDay) == null) selectedDay = latestDay
    }
    val selected = usage.kwhOn(selectedDay)?.let { kwh ->
        UsageDay(selectedDay, kwh, usage.dateOf(selectedDay))
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AppHeader(state = state, onOpenSettings = onOpenSettings) }

        item {
            SunArcCard(
                powerKw = powerW / 1000.0,
                sunTimes = sunTimes,
                daylight = daylight,
            )
        }

        item {
            SunGridCard {
                FlowDiagram(
                    panelWatts = powerW,
                    gridWatts = gridFlowW,
                    exporting = exporting,
                )
            }
        }

        item {
            GenerationRaceCard(
                dailyKwh = dailyKwh,
                lifetimeKwh = lifetimeKwh,
                latestDay = latestDay,
                latestUsage = latestUsage,
            )
        }

        item {
            WeeklyUsageCard(
                usage = usage,
                weeks = weeks,
                totalKwh = monthTotal,
                selectedDay = selected,
                latestDay = latestDay,
                hasUsage = hasUsage,
                onSelect = { selectedDay = it.day },
            )
        }

        item {
            val note = when {
                hasUsage && usageIsDemo ->
                    "Example grid usage. Connect UPPCL in settings for real readings."

                hasUsage -> "Grid usage comes from UPPCL and can lag by a day or more."
                else -> "Connect UPPCL in settings to see your grid usage."
            }
            Text(
                text = note,
                style = AppText.sub,
                color = colors.mute,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SunArcCard(
    powerKw: Double,
    sunTimes: SunTimes,
    daylight: Float,
) {
    val colors = LocalAppColors.current
    var started by remember { mutableStateOf(false) }
    var orbVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        started = true
        kotlinx.coroutines.delay(1220)
        orbVisible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (started) daylight else 0f,
        animationSpec = tween(1600, easing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)),
        label = "sun-progress",
    )
    val orbAlpha by animateFloatAsState(
        targetValue = if (orbVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "orb-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(colors.sky1, colors.sky2))),
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(340f / 190f),
            ) {
                val sx = size.width / 340f
                val sy = size.height / 190f
                val arc = Path().apply {
                    moveTo(30f * sx, 170f * sy)
                    arcTo(
                        rect = Rect(30f * sx, 30f * sy, 310f * sx, 310f * sy),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false,
                    )
                }

                drawPath(
                    path = arc,
                    color = Color.White.copy(alpha = 0.6f),
                    style = Stroke(
                        width = 3f * sx,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(1f * sx, 2.4f * sx),
                            0f,
                        ),
                    ),
                )

                if (progress > 0f) {
                    val measure = PathMeasure()
                    measure.setPath(arc, false)
                    val segment = Path()
                    measure.getSegment(0f, measure.length * progress, segment, true)
                    drawPath(
                        path = segment,
                        color = colors.sun,
                        style = Stroke(width = 5f * sx, cap = StrokeCap.Round),
                    )
                }

                drawLine(
                    color = colors.ink.copy(alpha = 0.25f),
                    start = Offset(10f * sx, 172f * sy),
                    end = Offset(330f * sx, 172f * sy),
                    strokeWidth = 1f * sx,
                )

                if (orbAlpha > 0f) {
                    val angle = PI * (1.0 - daylight)
                    val orbX = 170f + 140f * cos(angle).toFloat()
                    val orbY = 170f - 140f * sin(angle).toFloat()
                    val center = Offset(orbX * sx, orbY * sy)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.95f),
                            0.4f to Color(0xFFFFC24D).copy(alpha = 0.55f),
                            1f to Color(0xFFFFC24D).copy(alpha = 0f),
                            center = center,
                            radius = 34f * sx,
                        ),
                        radius = 34f * sx,
                        center = center,
                        alpha = orbAlpha,
                    )
                    drawCircle(
                        color = Color(0xFFFFF6D8),
                        radius = 12f * sx,
                        center = center,
                        alpha = orbAlpha,
                    )
                    drawCircle(
                        color = colors.sun,
                        radius = 12f * sx,
                        center = center,
                        alpha = orbAlpha,
                        style = Stroke(width = 4f * sx),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Sunrise ${sunTimeLabel(sunTimes.sunrise)}",
                    style = AppText.small,
                    color = colors.ink.copy(alpha = 0.7f),
                )
                Text(
                    text = "Sunset ${sunTimeLabel(sunTimes.sunset)}",
                    style = AppText.small,
                    color = colors.ink.copy(alpha = 0.7f),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = String.format(java.util.Locale.getDefault(), "%.2f", powerKw),
                style = AppText.hero,
                color = colors.ink,
            )
            Text(
                text = "kW from your roof",
                style = AppText.sub,
                color = colors.ink.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun GenerationRaceCard(
    dailyKwh: Double,
    lifetimeKwh: Double,
    latestDay: Int?,
    latestUsage: Double?,
) {
    val colors = LocalAppColors.current
    val hasUsage = latestUsage != null && latestUsage > 0.0
    val percent = if (hasUsage) {
        (dailyKwh / latestUsage * 100.0).roundToInt()
    } else {
        0
    }
    val gap = (latestUsage ?: 0.0) - dailyKwh
    val ahead = hasUsage && gap <= 0.0
    val chipTint = when {
        !hasUsage -> colors.grid
        ahead -> colors.ok
        else -> colors.bad
    }
    val chipText = when {
        !hasUsage -> "No UPPCL data"
        ahead -> "$percent% · +${number(-gap, 1, 1)} kWh ahead"
        else -> "$percent% · ${number(gap, 1, 1)} kWh to go"
    }

    SunGridCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SunSpinner(size = 58.dp)
                Column {
                    Text(text = "Today’s generation", style = AppText.sub, color = colors.mute)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = number(dailyKwh, 1, 1),
                            style = AppText.number40,
                            color = colors.ink,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "kWh",
                            style = AppText.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                            ),
                            color = colors.mute,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Lifetime", style = AppText.micro, color = colors.mute)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = number(lifetimeKwh, 0, 0),
                        style = AppText.value15,
                        color = colors.ink,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "kWh",
                        style = AppText.sub,
                        color = colors.mute,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        DashedDivider()
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val label = buildAnnotatedString {
                append("Versus ")
                if (latestDay != null) {
                    withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold)) {
                        append("Day $latestDay usage")
                    }
                } else {
                    withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold)) {
                        append("grid usage")
                    }
                }
                append(" ")
                withStyle(SpanStyle(fontSize = 11.sp, color = colors.mute)) {
                    append(if (hasUsage) "(latest from UPPCL)" else "")
                }
            }
            Text(
                text = label,
                style = AppText.sub,
                color = colors.mute,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = chipText,
                style = AppText.chip,
                color = chipTint,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(chipTint.copy(alpha = 0.16f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(16.dp))
        RaceTrack(
            madeKwh = dailyKwh,
            usedKwh = latestUsage,
            ahead = ahead,
        )
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "${number(dailyKwh, 1, 1)} kWh",
                    style = AppText.value20,
                    color = colors.ink,
                )
                Text(text = "made today", style = AppText.small, color = colors.mute)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = latestUsage?.let { "${number(it, 2, 2)} kWh" } ?: "—",
                    style = AppText.value20,
                    color = colors.ink,
                )
                Text(text = "used on the grid", style = AppText.small, color = colors.mute)
            }
        }
    }
}

@Composable
private fun WeeklyUsageCard(
    usage: UsageMonth,
    weeks: List<UsageWeek>,
    totalKwh: Double,
    selectedDay: UsageDay?,
    latestDay: Int?,
    hasUsage: Boolean,
    onSelect: (UsageDay) -> Unit,
) {
    val colors = LocalAppColors.current
    val monthName = monthShort(usage.yearMonth.atDay(1))
    val maxKwh = remember(usage) { usage.kwhByDay.values.maxOrNull() ?: 0.0 }

    SunGridCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Grid usage this month", style = AppText.h2, color = colors.ink)
                Text(
                    text = when {
                        !hasUsage -> buildAnnotatedString { append("No readings yet") }
                        selectedDay != null -> buildAnnotatedString {
                            withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold)) {
                                append("${number(selectedDay.kwh, 2, 2)} kWh")
                            }
                            append(" on ${shortDate(selectedDay.date)}")
                            if (selectedDay.day == latestDay) append(" · latest")
                        }
                        else -> buildAnnotatedString { append("Tap a bar for details") }
                    },
                    style = AppText.sub,
                    color = colors.mute,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = number(totalKwh, 1, 1),
                    style = AppText.number30,
                    color = colors.ink,
                )
                Text(
                    text = "kWh since 1 $monthName",
                    style = AppText.sub,
                    color = colors.mute,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (weeks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Grid readings appear here once UPPCL posts them.",
                    style = AppText.sub,
                    color = colors.mute,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            key(weeks.size) {
                val pagerState = rememberPagerState(initialPage = weeks.lastIndex) { weeks.size }
                LaunchedEffect(pagerState.currentPage, weeks) {
                    val pageDays = weeks.getOrNull(pagerState.currentPage)
                        ?.days
                        ?.filterNotNull()
                        .orEmpty()
                    if (pageDays.isNotEmpty() && pageDays.none { it.day == selectedDay?.day }) {
                        onSelect(pageDays.last())
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(174.dp),
                    pageSpacing = 16.dp,
                ) { page ->
                    val week = weeks[page]
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = weekRangeLabel(week, usage),
                            style = AppText.micro,
                            color = colors.mute,
                        )
                        Spacer(Modifier.height(8.dp))
                        MonthWeekBars(
                            week = week,
                            month = usage.yearMonth,
                            maxKwh = maxKwh,
                            selectedDay = selectedDay?.day,
                            onSelect = onSelect,
                        )
                    }
                }
                if (weeks.size > 1 && pagerState.currentPage > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Swipe the chart for earlier weeks",
                        style = AppText.micro,
                        color = colors.mute,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun monthShort(date: LocalDate): String =
    date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun weekRangeLabel(week: UsageWeek, usage: UsageMonth): String {
    val start = maxOf(week.start, usage.yearMonth.atDay(1))
    val end = minOf(week.start.plusDays(6), usage.yearMonth.atEndOfMonth())
    return if (start.month == end.month) {
        "${start.dayOfMonth}–${end.dayOfMonth} ${monthShort(start)}"
    } else {
        "${start.dayOfMonth} ${monthShort(start)} – ${end.dayOfMonth} ${monthShort(end)}"
    }
}
