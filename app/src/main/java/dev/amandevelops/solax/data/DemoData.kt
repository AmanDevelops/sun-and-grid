package dev.amandevelops.solax.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic, realistic demo data. The first launch shows a populated app
 * instead of an empty dashboard; switching demo mode off uses the real inverter.
 */
object DemoData {
    const val SERIAL = "SR1ES8A2B4C6"
    const val MODEL = "X1-Hybrid-5.0-L"

    private const val CAPACITY_KW = 5.2
    private const val LIFETIME_BASE_KWH = 8_640.0
    private const val START_HOUR = 6.0
    private const val END_HOUR = 20.0

    fun history(
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Reading> {
        val today = now.atZone(zone).toLocalDate()
        val samples = mutableListOf<Reading>()
        var lifetime = LIFETIME_BASE_KWH

        for (daysAgo in 29 downTo 0) {
            val date = today.minusDays(daysAgo.toLong())
            val energy = dailyEnergy(date)
            val weather = weatherFactor(date)
            val totalAtStart = lifetime
            val dayStart = date.atStartOfDay(zone)
            val minuteLimit = if (daysAgo == 0) {
                now.atZone(zone).toLocalTime().let { it.hour * 60 + it.minute }
            } else {
                Int.MAX_VALUE
            }

            var minute = (START_HOUR * 60).toInt()
            while (minute <= END_HOUR * 60) {
                if (minute <= minuteLimit) {
                    val hour = minute / 60.0
                    val shape = solarShape(hour)
                    val wobble = 1.0 + 0.06 * sin(minute * 0.9 + daysAgo)
                    val powerW = (CAPACITY_KW * shape * weather * wobble * 1000.0).coerceAtLeast(0.0)
                    val daily = energy * energyFraction(hour)
                    val total = totalAtStart + daily
                    val pvVoltage = if (powerW > 20) 360.0 + 24.0 * sin(minute * 0.07 + daysAgo) else 0.0
                    val home = homeUseW(powerW)
                    val grid = powerW - home

                    samples += Reading(
                        timestamp = dayStart.plusMinutes(minute.toLong()).toInstant().toEpochMilli(),
                        powerW = powerW,
                        pv1PowerW = powerW,
                        dailyYieldKwh = daily,
                        totalYieldKwh = total,
                        gridPowerW = grid,
                        gridVoltageV = 230.0 + 2.5 * sin(minute * 0.21),
                        gridCurrentA = abs(grid) / 230.0,
                        gridFrequencyHz = 50.0 + 0.03 * sin(minute * 0.37),
                        pv1VoltageV = pvVoltage,
                        pv1CurrentA = if (pvVoltage > 0) powerW / pvVoltage else 0.0,
                        temperatureC = 30.0 + 12.0 * shape * weather + 0.6 * sin(minute * 0.3),
                        inverterSn = SERIAL,
                        model = MODEL,
                    )
                }
                minute += 30
            }
            lifetime += energy
        }

        return samples
    }

    fun live(
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Reading {
        val zoned = now.atZone(zone)
        val date = zoned.toLocalDate()
        val minutes = zoned.hour * 60 + zoned.minute + zoned.second / 60.0
        val hour = minutes / 60.0
        val shape = solarShape(hour)
        val weather = weatherFactor(date)
        val wobble = 1.0 + 0.028 * sin(now.epochSecond / 31.0) + 0.014 * sin(now.epochSecond / 7.0)
        val powerW = (CAPACITY_KW * shape * weather * wobble * 1000.0).coerceAtLeast(0.0)
        val energy = dailyEnergy(date)
        val daily = energy * energyFraction(hour)
        val previousDays = (1..29).sumOf { dailyEnergy(date.minusDays(it.toLong())) }
        val total = LIFETIME_BASE_KWH + previousDays + daily
        val home = homeUseW(powerW)
        val grid = powerW - home
        val pvVoltage = if (powerW > 20) 360.0 + 24.0 * sin(minutes * 0.0042) else 0.0

        return Reading(
            timestamp = now.toEpochMilli(),
            powerW = powerW,
            pv1PowerW = powerW,
            dailyYieldKwh = daily,
            totalYieldKwh = total,
            gridPowerW = grid,
            gridVoltageV = 230.0 + 2.5 * sin(minutes * 0.013),
            gridCurrentA = abs(grid) / 230.0,
            gridFrequencyHz = 50.0 + 0.03 * sin(minutes * 0.022),
            pv1VoltageV = pvVoltage,
            pv1CurrentA = if (pvVoltage > 0) powerW / pvVoltage else 0.0,
            temperatureC = 30.0 + 12.0 * shape * weather,
            inverterSn = SERIAL,
            model = MODEL,
        )
    }

    private fun solarShape(hour: Double): Double {
        if (hour <= START_HOUR || hour >= END_HOUR) return 0.0
        return sin(PI * (hour - START_HOUR) / (END_HOUR - START_HOUR))
    }

    private fun energyFraction(hour: Double): Double {
        if (hour <= START_HOUR) return 0.0
        if (hour >= END_HOUR) return 1.0
        return (1.0 - cos(PI * (hour - START_HOUR) / (END_HOUR - START_HOUR))) / 2.0
    }

    private fun dailyEnergy(date: LocalDate): Double {
        return 13.0 + Random(seed(date)).nextDouble() * 11.0
    }

    private fun weatherFactor(date: LocalDate): Double {
        return 0.68 + Random(seed(date) + 7).nextDouble() * 0.32
    }

    private fun seed(date: LocalDate): Long = date.toEpochDay() * 1_000_003L

    private fun homeUseW(powerW: Double): Double {
        if (powerW <= 20.0) return 300.0
        val load = 450.0 + 280.0 * (powerW / 1000.0)
        return min(powerW, load)
    }
}
