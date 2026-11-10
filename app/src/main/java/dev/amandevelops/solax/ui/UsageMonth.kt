package dev.amandevelops.solax.ui

import dev.amandevelops.solax.data.UppclMonthData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.round
import kotlin.random.Random

data class UsageDay(
    val day: Int,
    val kwh: Double,
    val date: LocalDate,
)

/**
 * One Sunday-first calendar week on the Today usage chart. [days] always has
 * seven entries so weekday columns stay aligned, while days outside the month
 * or not posted yet are null.
 */
data class UsageWeek(
    val start: LocalDate,
    val days: List<UsageDay?>,
)

/**
 * One month of grid consumption. Only days published by UPPCL are present, so
 * the UI never has to guess that "today - 1" is the latest reading.
 */
data class UsageMonth(
    val yearMonth: YearMonth,
    val kwhByDay: Map<Int, Double>,
) {
    val length: Int get() = yearMonth.lengthOfMonth()

    val lastPostedDay: Int? get() = kwhByDay.keys.maxOrNull()

    val postedDays: List<Int> get() = kwhByDay.keys.sorted()

    val totalKwh: Double get() = kwhByDay.values.sum()

    val averageKwh: Double
        get() = lastPostedDay?.let { totalKwh / it } ?: 0.0

    val peak: UsageDay?
        get() {
            val entry = kwhByDay.maxByOrNull { it.value } ?: return null
            return UsageDay(entry.key, entry.value, yearMonth.atDay(entry.key))
        }

    fun kwhOn(day: Int): Double? = kwhByDay[day]

    fun dateOf(day: Int): LocalDate = yearMonth.atDay(day)

    fun latestWeek(count: Int = 7): List<UsageDay> {
        val last = lastPostedDay ?: return emptyList()
        val first = (last - count + 1).coerceAtLeast(1)
        return (first..last).map { day ->
            UsageDay(day, kwhByDay[day] ?: 0.0, yearMonth.atDay(day))
        }
    }

    /**
     * Calendar weeks from the week containing day 1 through the week containing
     * the latest posted reading. Used by the swipeable month chart.
     */
    fun weeksToDate(): List<UsageWeek> {
        val last = lastPostedDay ?: return emptyList()
        val monthStart = yearMonth.atDay(1)
        val firstWeekStart = monthStart.minusDays((monthStart.dayOfWeek.value % 7).toLong())
        val lastDate = yearMonth.atDay(last)
        return buildList {
            var weekStart = firstWeekStart
            while (!weekStart.isAfter(lastDate)) {
                val days = (0..6).map { offset ->
                    val date = weekStart.plusDays(offset.toLong())
                    if (YearMonth.from(date) != yearMonth) {
                        null
                    } else {
                        kwhByDay[date.dayOfMonth]?.let { kwh ->
                            UsageDay(date.dayOfMonth, kwh, date)
                        }
                    }
                }
                add(UsageWeek(weekStart, days))
                weekStart = weekStart.plusWeeks(1)
            }
        }
    }

    companion object {
        fun empty(month: YearMonth = YearMonth.now()): UsageMonth = UsageMonth(month, emptyMap())
    }
}

fun UppclMonthData.toUsageMonth(): UsageMonth {
    val month = YearMonth.of(year, month)
    val sorted = days
        .filter { it.day in 1..month.lengthOfMonth() }
        .sortedBy { it.day }
    val last = sorted.lastOrNull { it.consumption > 0.0 }?.day ?: return UsageMonth.empty(month)
    val published = sorted
        .filter { it.day <= last }
        .associate { it.day to it.consumption.coerceAtLeast(0.0) }
    return UsageMonth(month, published)
}

/**
 * Deterministic example consumption for demo mode. UPPCL normally lags by a
 * couple of days, so the latest posted day is two days behind today.
 */
fun demoUsageMonth(today: LocalDate = LocalDate.now()): UsageMonth {
    val month = YearMonth.from(today)
    val last = today.dayOfMonth - 2
    if (last < 1) return UsageMonth.empty(month)

    val random = Random(today.year * 10_000 + today.monthValue)
    val values = LinkedHashMap<Int, Double>()
    for (day in 1..last) {
        val date = month.atDay(day)
        val weekend = if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            1.12
        } else {
            1.0
        }
        val kwh = (4.05 + random.nextDouble() * 2.8) * weekend
        values[day] = round(kwh * 100.0) / 100.0
    }
    return UsageMonth(month, values)
}
