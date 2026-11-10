package dev.amandevelops.solax.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.amandevelops.solax.data.Analytics
import dev.amandevelops.solax.ui.MonitorUiState
import dev.amandevelops.solax.ui.components.AppHeader
import dev.amandevelops.solax.ui.components.AppIcons
import dev.amandevelops.solax.ui.components.GaugeCard
import dev.amandevelops.solax.ui.components.HistoryShortcut
import dev.amandevelops.solax.ui.components.ReadingRow
import dev.amandevelops.solax.ui.components.SettingsShortcut
import dev.amandevelops.solax.ui.components.tap
import dev.amandevelops.solax.ui.number
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DeviceScreen(
    state: MonitorUiState,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val reading = state.reading
    val gridExport = reading?.let { Analytics.gridExportW(it, state.settings.invertGridSign) } ?: 0.0

    val gridVoltage = reading?.gridVoltageV ?: 0.0
    val frequency = reading?.gridFrequencyHz ?: 0.0
    val temperature = reading?.temperatureC ?: 0.0
    val panelVoltage = reading?.pv1VoltageV ?: 0.0
    val powerW = reading?.powerW ?: 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AppHeader(state = state, onOpenSettings = onOpenSettings) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GaugeCard(
                        value = number(gridVoltage, 1, 1),
                        unit = "V",
                        label = "Grid voltage",
                        fraction = ((gridVoltage - 200.0) / 60.0).toFloat(),
                        color = colors.grid,
                        modifier = Modifier.weight(1f),
                    )
                    GaugeCard(
                        value = number(frequency, 2, 2),
                        unit = "Hz",
                        label = "Grid frequency",
                        fraction = ((frequency - 49.0) / 2.0).toFloat(),
                        color = colors.ok,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GaugeCard(
                        value = if (reading?.temperatureC != null) {
                            number(temperature, 0, 0)
                        } else {
                            "—"
                        },
                        unit = "°C",
                        label = "Inverter temperature",
                        fraction = (temperature / 80.0).toFloat(),
                        color = colors.sun,
                        modifier = Modifier.weight(1f),
                    )
                    GaugeCard(
                        value = number(panelVoltage, 1, 1),
                        unit = "V",
                        label = "Panel voltage",
                        fraction = (panelVoltage / 500.0).toFloat(),
                        color = colors.sun,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            AllReadingsCard(
                reading = reading,
                gridExport = gridExport,
                powerW = powerW,
            )
        }

        item {
            Text(
                text = if (state.settings.demoMode) {
                    "Demo values. Raw registers are scaled for you."
                } else {
                    "Live values. Raw registers are scaled for you."
                },
                style = AppText.sub,
                color = colors.mute,
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                textAlign = TextAlign.Center,
            )
        }

        item {
            SettingsShortcut(
                subtitle = when {
                    state.settings.demoMode -> "Example data"
                    state.settings.dongleSerial.isBlank() -> "Connect your inverter"
                    else -> state.settings.host
                },
                onClick = onOpenSettings,
            )
        }

        item {
            HistoryShortcut(
                subtitle = when (val count = state.history.size) {
                    0 -> "Saved readings appear here"
                    1 -> "1 saved reading"
                    else -> "$count saved readings"
                },
                onClick = onOpenHistory,
            )
        }
    }
}

@Composable
private fun AllReadingsCard(
    reading: dev.amandevelops.solax.data.Reading?,
    gridExport: Double,
    powerW: Double,
) {
    val colors = LocalAppColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tap { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "All inverter readings",
                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = if (expanded) "Hide readings" else "Show readings",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = colors.mute,
            )
        }

        if (expanded) {
            ReadingRow(
                title = "Grid voltage",
                value = "${number(reading?.gridVoltageV ?: 0.0, 1, 1)} V",
                register = "Register 0, ×0.1",
            )
            ReadingRow(
                title = "PV1 current",
                value = "${number(reading?.pv1CurrentA ?: 0.0, 1, 1)} A",
                register = "Register 1, ×0.1",
            )
            ReadingRow(
                title = "Grid frequency",
                value = "${number(reading?.gridFrequencyHz ?: 0.0, 2, 2)} Hz",
                register = "Register 2, ×0.01",
            )
            ReadingRow(
                title = "PV1 input power",
                value = "${number(reading?.pv1PowerW ?: 0.0, 0, 0)} W",
                register = "Register 3",
            )
            ReadingRow(
                title = "PV1 voltage",
                value = "${number(reading?.pv1VoltageV ?: 0.0, 1, 1)} V",
                register = "Register 4, ×0.1",
            )
            ReadingRow(
                title = "Grid current",
                value = "${number(reading?.gridCurrentA ?: 0.0, 1, 1)} A",
                register = "Register 8, ×0.1",
            )
            ReadingRow(
                title = "Grid power",
                value = "${number(abs(gridExport), 0, 0)} W ${if (gridExport >= 0) "exporting" else "importing"}",
                register = "Register 11",
            )
            ReadingRow(
                title = "Total solar power",
                value = "${number(powerW, 0, 0)} W",
                register = "Register 13",
            )
            ReadingRow(
                title = "Total yield",
                value = "${number(reading?.totalYieldKwh ?: 0.0, 0, 1)} kWh",
                register = "Register 19, ×0.1",
            )
            ReadingRow(
                title = "Daily yield",
                value = "${number(reading?.dailyYieldKwh ?: 0.0, 1, 1)} kWh",
                register = "Register 21, ×0.1",
            )
            ReadingRow(
                title = "Inverter temperature",
                value = reading?.temperatureC?.let { "${number(it, 0, 0)} °C" } ?: "—",
                register = "Register 23",
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}
