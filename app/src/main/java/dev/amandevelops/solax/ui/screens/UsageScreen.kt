package dev.amandevelops.solax.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.amandevelops.solax.ui.MonitorUiState
import dev.amandevelops.solax.ui.UsageMonth
import dev.amandevelops.solax.ui.components.AppHeader
import dev.amandevelops.solax.ui.components.SunGridCard
import dev.amandevelops.solax.ui.components.tap
import dev.amandevelops.solax.ui.number
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun UsageScreen(
    state: MonitorUiState,
    usage: UsageMonth,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val today = LocalDate.now()
    val month = usage.yearMonth
    var selectedDay by rememberSaveable(month) { mutableStateOf<Int?>(null) }
    val posted = usage.kwhByDay
    val max = posted.values.maxOrNull() ?: 0.0
    val min = posted.values.minOrNull() ?: 0.0
    val peak = usage.peak
    val dailySolar = state.reading?.dailyYieldKwh ?: 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AppHeader(state = state, onOpenSettings = onOpenSettings) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UsageStat(
                    label = "Month so far",
                    value = number(usage.totalKwh, 1, 1),
                    modifier = Modifier.weight(1f),
                )
                UsageStat(
                    label = "Daily average",
                    value = number(usage.averageKwh, 1, 1),
                    modifier = Modifier.weight(1f),
                )
                UsageStat(
                    label = peak?.let { "Peak, day ${it.day}" } ?: "Peak day",
                    value = peak?.let { number(it.kwh, 2, 2) } ?: "0.00",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SunGridCard {
                Text(
                    text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}, day by day",
                    style = AppText.h2,
                    color = colors.ink,
                )
                Spacer(Modifier.height(12.dp))
                CalendarGrid(
                    usage = usage,
                    today = today,
                    min = min,
                    max = max,
                    selectedDay = selectedDay,
                    onSelect = { selectedDay = it },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = calendarReadout(
                        selectedDay = selectedDay,
                        usage = usage,
                        today = today,
                        dailySolar = dailySolar,
                    ),
                    style = AppText.sub,
                    color = colors.ink,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun UsageStat(
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
        Text(
            text = label,
            style = AppText.sub,
            color = colors.mute,
            maxLines = 1,
        )
        Text(
            text = value,
            style = AppText.value22,
            color = colors.ink,
            maxLines = 1,
        )
        Text(text = "kWh", style = AppText.sub, color = colors.mute)
    }
}

@Composable
private fun CalendarGrid(
    usage: UsageMonth,
    today: LocalDate,
    min: Double,
    max: Double,
    selectedDay: Int?,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val firstOffset = usage.yearMonth.atDay(1).dayOfWeek.value % 7
    val cells: List<Int?> = buildList {
        repeat(firstOffset) { add(null) }
        for (day in 1..usage.length) add(day)
        while (size % 7 != 0) add(null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            "SMTWTFS".forEach { letter ->
                Text(
                    text = letter.toString(),
                    style = AppText.tiny,
                    color = colors.mute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val value = usage.kwhOn(day)
                        val pending = value == null
                        val selected = day == selectedDay
                        val isToday = day == today.dayOfMonth
                        val q = if (value == null) {
                            0f
                        } else if (max > min) {
                            ((value - min) / (max - min)).toFloat().coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        val shape = RoundedCornerShape(12.dp)
                        val fill = if (pending) {
                            Color.Transparent
                        } else {
                            colors.grid.copy(alpha = 0.08f + 0.92f * q)
                        }
                        val textColor = when {
                            pending -> colors.mute
                            q > 0.4f -> Color.White
                            else -> colors.ink
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .background(fill)
                                .then(
                                    if (pending) {
                                        Modifier.dashedBorder(
                                            color = colors.line,
                                            strokeWidth = 1.5.dp,
                                            radius = 12.dp,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    when {
                                        selected -> Modifier.border(2.dp, colors.ink, shape)
                                        isToday -> Modifier.border(2.dp, colors.sun, shape)
                                        else -> Modifier
                                    },
                                )
                                .tap { onSelect(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.toString(),
                                style = AppText.micro.copy(fontWeight = FontWeight.Bold),
                                color = textColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp,
): Modifier = drawBehind {
    val width = strokeWidth.toPx()
    val dash = 4.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(width / 2f, width / 2f),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash), 0f),
        ),
    )
}

@Composable
private fun calendarReadout(
    selectedDay: Int?,
    usage: UsageMonth,
    today: LocalDate,
    dailySolar: Double,
): androidx.compose.ui.text.AnnotatedString {
    val colors = LocalAppColors.current
    if (selectedDay == null) {
        return buildAnnotatedString {
            append("Shades run from your lowest to your highest usage day: darker means more grid usage.")
        }
    }
    val value = usage.kwhOn(selectedDay)
    return buildAnnotatedString {
        val isToday = selectedDay == today.dayOfMonth
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.ink)) {
            append(if (isToday) "Today" else "Day $selectedDay")
        }
        when {
            value != null -> append(": used ${number(value, 2, 2)} kWh")
            isToday -> append(": solar has made ${number(dailySolar, 1, 1)} kWh so far.")
            else -> append(": not posted yet")
        }
    }
}
