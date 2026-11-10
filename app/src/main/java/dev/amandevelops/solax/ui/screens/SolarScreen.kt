package dev.amandevelops.solax.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.amandevelops.solax.data.Analytics
import dev.amandevelops.solax.ui.MonitorUiState
import dev.amandevelops.solax.ui.components.AppHeader
import dev.amandevelops.solax.ui.components.SunGridCard
import dev.amandevelops.solax.ui.energyText
import dev.amandevelops.solax.ui.number
import dev.amandevelops.solax.ui.theme.AppText
import dev.amandevelops.solax.ui.theme.LocalAppColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SolarScreen(
    state: MonitorUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val reading = state.reading
    val dailyKwh = reading?.dailyYieldKwh ?: 0.0
    val lifetimeKwh = reading?.totalYieldKwh ?: 0.0
    val powerW = reading?.powerW ?: 0.0
    val gridExportW = reading?.let { Analytics.gridExportW(it, state.settings.invertGridSign) } ?: 0.0
    val exporting = gridExportW >= 0

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
                SolarStat(
                    label = "Today",
                    value = "${number(dailyKwh, 1, 1)} kWh",
                    modifier = Modifier.weight(1f),
                )
                SolarStat(
                    label = "Lifetime",
                    value = energyText(lifetimeKwh),
                    modifier = Modifier.weight(1f),
                )
                SolarStat(
                    label = "Right now",
                    value = "${number(powerW / 1000.0, 2, 2)} kW",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SunGridCard {
                Text(text = "Panel side", style = AppText.h2, color = colors.ink)
                Spacer(Modifier.height(12.dp))
                Trio(
                    first = "${number(reading?.pv1VoltageV ?: 0.0, 1, 1)} V" to "Voltage",
                    second = "${number(reading?.pv1CurrentA ?: 0.0, 1, 1)} A" to "Current",
                    third = "${number(powerW, 0, 0)} W" to "Power",
                )
            }
        }

        item {
            SunGridCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Grid side", style = AppText.h2, color = colors.ink)
                    val chipColor = if (exporting) colors.ok else colors.bad
                    Text(
                        text = if (exporting) {
                            "Exporting ${number(abs(gridExportW) / 1000.0, 2, 2)} kW"
                        } else {
                            "Importing ${number(abs(gridExportW) / 1000.0, 2, 2)} kW"
                        },
                        style = AppText.chip,
                        color = chipColor,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipColor.copy(alpha = 0.16f))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Trio(
                    first = "${number(reading?.gridVoltageV ?: 0.0, 1, 1)} V" to "Voltage",
                    second = "${number(reading?.gridCurrentA ?: 0.0, 1, 1)} A" to "Current",
                    third = "${number(reading?.gridFrequencyHz ?: 0.0, 2, 2)} Hz" to "Frequency",
                )
            }
        }
    }
}

@Composable
private fun SolarStat(
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
private fun Trio(
    first: Pair<String, String>,
    second: Pair<String, String>,
    third: Pair<String, String>,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(first, second, third).forEach { (value, label) ->
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = AppText.value19,
                    color = colors.ink,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = AppText.small,
                    color = colors.mute,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}
