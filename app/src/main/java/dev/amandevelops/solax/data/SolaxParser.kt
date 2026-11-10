package dev.amandevelops.solax.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.math.max

/**
 * Parses the payload returned by `optType=ReadRealTimeData`.
 *
 * The mapping follows the SolaX Python Wrapper:
 *  0 grid voltage ×0.1 V, 1 PV1 current ×0.1 A, 2 grid frequency ×0.01 Hz,
 *  3 PV1 power W, 4 PV1 voltage ×0.1 V, 8 grid current ×0.1 A,
 *  11 grid power W, 13 total solar power W, 19 total yield ×0.1 kWh,
 *  21 daily yield ×0.1 kWh, 23 inverter temperature °C.
 *
 * Some older dongles only populate index 3, so the larger of 3/13 is used for
 * the live array power.
 */
object SolaxParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    fun parse(body: String, now: Long = System.currentTimeMillis()): Reading {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrElse {
                throw SolaxException(
                    "The inverter sent an unexpected response. Check the dongle password and try again.",
                    it,
                )
            }

        val data = root["Data"]?.let { element ->
            runCatching { element.jsonArray }.getOrNull()
        } ?: throw SolaxException("No inverter data in the response. Check the dongle password.")

        if (data.isEmpty()) {
            throw SolaxException("The inverter returned an empty data set.")
        }

        val info = root["Information"]?.let { element ->
            runCatching { element.jsonArray }.getOrNull()
        }

        fun number(index: Int): Double? = data.getOrNull(index)?.numberOrNull()

        val pv1Power = number(3) ?: 0.0
        val totalSolarPower = number(13) ?: 0.0
        val model = info?.getOrNull(2)?.stringOrNull()
            ?: info?.getOrNull(1)?.stringOrNull()
            ?: ""

        return Reading(
            timestamp = now,
            powerW = max(pv1Power, totalSolarPower).coerceAtLeast(0.0),
            pv1PowerW = pv1Power.coerceAtLeast(0.0),
            dailyYieldKwh = ((number(21) ?: 0.0) / 10.0).coerceAtLeast(0.0),
            totalYieldKwh = ((number(19) ?: 0.0) / 10.0).coerceAtLeast(0.0),
            gridPowerW = number(11) ?: 0.0,
            gridVoltageV = ((number(0) ?: 0.0) / 10.0).coerceAtLeast(0.0),
            gridCurrentA = ((number(8) ?: 0.0) / 10.0).coerceAtLeast(0.0),
            gridFrequencyHz = ((number(2) ?: 0.0) / 100.0).coerceAtLeast(0.0),
            pv1VoltageV = ((number(4) ?: 0.0) / 10.0).coerceAtLeast(0.0),
            pv1CurrentA = ((number(1) ?: 0.0) / 10.0).coerceAtLeast(0.0),
            temperatureC = number(23)?.takeIf { it > -40.0 && it < 120.0 },
            inverterSn = root["sn"]?.stringOrNull().orEmpty(),
            model = model,
            status = root["Status"]?.numberOrNull()?.toInt(),
        )
    }

    private fun JsonElement.numberOrNull(): Double? = when (this) {
        is JsonPrimitive -> content.toDoubleOrNull()
        else -> null
    }

    private fun JsonElement.stringOrNull(): String? = when (this) {
        is JsonPrimitive -> if (isString) content else content.takeIf { it.isNotBlank() }
        else -> null
    }

    private fun JsonArray?.getOrNull(index: Int): JsonElement? =
        if (this == null || index !in indices) null else this[index]
}
