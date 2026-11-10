package dev.amandevelops.solax.data

import kotlinx.serialization.Serializable

/**
 * One point-in-time snapshot read from a SolaX inverter's local Pocket Wi-Fi API.
 *
 * Field indices are documented in the protocol README. Values are stored in the
 * units exposed by the inverter (W, V, A, °C, kWh).
 */
@Serializable
data class Reading(
    val timestamp: Long = System.currentTimeMillis(),
    val powerW: Double = 0.0,
    val pv1PowerW: Double = 0.0,
    val dailyYieldKwh: Double = 0.0,
    val totalYieldKwh: Double = 0.0,
    val gridPowerW: Double = 0.0,
    val gridVoltageV: Double = 0.0,
    val gridCurrentA: Double = 0.0,
    val gridFrequencyHz: Double = 0.0,
    val pv1VoltageV: Double = 0.0,
    val pv1CurrentA: Double = 0.0,
    val temperatureC: Double? = null,
    val inverterSn: String = "",
    val model: String = "",
    val status: Int? = null,
) {
    val powerKw: Double get() = powerW / 1000.0
    val isGenerating: Boolean get() = powerW > 30.0
}

class SolaxException(message: String, cause: Throwable? = null) : Exception(message, cause)
