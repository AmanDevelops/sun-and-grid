package dev.amandevelops.solax.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.amandevelops.solax.data.Analytics
import dev.amandevelops.solax.data.Reading
import dev.amandevelops.solax.ui.MonitorUiState
import dev.amandevelops.solax.ui.UsageDay
import dev.amandevelops.solax.ui.components.AppIcons
import dev.amandevelops.solax.ui.components.ReadingRow
import dev.amandevelops.solax.ui.components.SunGridCard
import dev.amandevelops.solax.ui.components.WeeklyBars
import dev.amandevelops.solax.ui.components.tap
import dev.amandevelops.solax.ui.number
import dev.amandevelops.solax.ui.powerText
import dev.amandevelops.solax.ui.shortDate
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors
import dev.amandevelops.solax.ui.timeLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HistoryScreen(
    state: MonitorUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val aggregates = remember(state.history, state.reading) {
        Analytics.aggregate(state.history, state.reading)
    }
    val today = LocalDate.now()
    val dates = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val week = remember(dates, aggregates) {
        dates.map { date ->
            UsageDay(
                day = date.dayOfMonth,
                kwh = aggregates[date]?.kwh ?: 0.0,
                date = date,
            )
        }
    }
    var selectedIndex by rememberSaveable { mutableIntStateOf(week.lastIndex) }
    val selected = week[selectedIndex]
    val total = week.sumOf { it.kwh }
    val best = week.maxByOrNull { it.kwh }?.takeIf { it.kwh > 0.0 }
    val recent = remember(state.history) { state.history.takeLast(12).reversed() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HistoryHeader(onBack = onBack) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HistoryStat(
                    label = "This week",
                    value = number(total, 1, 1),
                    unit = "kWh",
                    modifier = Modifier.weight(1f),
                )
                HistoryStat(
                    label = "Best day",
                    value = best?.let { number(it.kwh, 1, 1) } ?: "—",
                    unit = "kWh",
                    modifier = Modifier.weight(1f),
                )
                HistoryStat(
                    label = "Saved",
                    value = number(state.history.size.toDouble(), 0, 0),
                    unit = "readings",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SunGridCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Solar made this week", style = AppText.h2, color = colors.ink)
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold)) {
                                    append("${number(selected.kwh, 1, 1)} kWh")
                                }
                                append(" on ${shortDate(selected.date)}")
                            },
                            style = AppText.sub,
                            color = colors.mute,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = number(total, 1, 1),
                            style = AppText.number30,
                            color = colors.ink,
                        )
                        Text(text = "kWh total", style = AppText.sub, color = colors.mute)
                    }
                }
                Spacer(Modifier.height(16.dp))
                WeeklyBars(
                    week = week,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                )
            }
        }

        item {
            SunGridCard {
                Text(text = "Recent readings", style = AppText.h2, color = colors.ink)
                Text(
                    text = if (state.history.isEmpty()) {
                        "Nothing saved yet."
                    } else {
                        "Latest saved samples from your inverter."
                    },
                    style = AppText.sub,
                    color = colors.mute,
                )
                Spacer(Modifier.height(8.dp))
                if (recent.isEmpty()) {
                    Text(
                        text = "Readings appear here while the app is open, or when background " +
                            "collection is on.",
                        style = AppText.body,
                        color = colors.mute,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                } else {
                    recent.forEach { reading ->
                        ReadingRow(
                            title = readingTitle(reading),
                            value = powerText(reading.powerW),
                            register = "${number(reading.dailyYieldKwh, 1, 1)} kWh that day",
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        item {
            Text(
                text = "History stays on this phone. Clear it from Settings whenever you like.",
                style = AppText.sub,
                color = colors.mute,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.card)
                .tap(onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
                tint = colors.ink,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Sun & Grid", style = AppText.sub, color = colors.mute)
            Spacer(Modifier.height(4.dp))
            Text(text = "History", style = AppText.header, color = colors.ink)
        }
    }
}

@Composable
private fun HistoryStat(
    label: String,
    value: String,
    unit: String,
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
        Text(text = unit, style = AppText.sub, color = colors.mute)
    }
}

private fun readingTitle(reading: Reading): String {
    val date = Instant.ofEpochMilli(reading.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return "${shortDate(date)} · ${timeLabel(reading.timestamp)}"
}
