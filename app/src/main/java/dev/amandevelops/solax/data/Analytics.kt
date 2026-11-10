package dev.amandevelops.solax.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

data class DayAggregate(
    val date: LocalDate,
    val kwh: Double?,
    val peakW: Double?,
    val avgTempC: Double?,
    val sampleCount: Int,
)

object Analytics {

    fun weekStart(anchor: LocalDate): LocalDate =
        anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    /** Sunday-first week to match the calendar in the UI. */
    fun weekDates(anchor: LocalDate): List<LocalDate> {
        val start = weekStart(anchor)
        return (0L..6L).map { start.plusDays(it) }
    }

    fun aggregate(
        history: List<Reading>,
        live: Reading?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, DayAggregate> {
        val buckets = LinkedHashMap<LocalDate, Bucket>()

        fun add(reading: Reading) {
            val date = Instant.ofEpochMilli(reading.timestamp).atZone(zone).toLocalDate()
            buckets.getOrPut(date) { Bucket(date) }.add(reading)
        }

        history.forEach(::add)
        live?.let(::add)
        return buckets.mapValues { (_, bucket) -> bucket.toAggregate() }
    }

    /** Positive means export to the grid, negative means import. */
    fun gridExportW(reading: Reading, invertSign: Boolean): Double =
        if (invertSign) -reading.gridPowerW else reading.gridPowerW

    fun homeUseW(reading: Reading, invertSign: Boolean): Double =
        (reading.powerW - gridExportW(reading, invertSign)).coerceAtLeast(0.0)

    fun periodTotal(dates: List<LocalDate>, aggregates: Map<LocalDate, DayAggregate>): Double =
        dates.sumOf { aggregates[it]?.kwh ?: 0.0 }

    private class Bucket(private val date: LocalDate) {
        private var maxDailyKwh = 0.0
        private var peakW = 0.0
        private var tempSum = 0.0
        private var tempCount = 0
        private var count = 0

        fun add(reading: Reading) {
            count++
            maxDailyKwh = max(maxDailyKwh, reading.dailyYieldKwh)
            peakW = max(peakW, reading.powerW)
            reading.temperatureC?.let {
                tempSum += it
                tempCount++
            }
        }

        fun toAggregate(): DayAggregate = DayAggregate(
            date = date,
            kwh = if (count > 0) maxDailyKwh else null,
            peakW = if (count > 0) peakW else null,
            avgTempC = if (tempCount > 0) tempSum / tempCount else null,
            sampleCount = count,
        )
    }
}
