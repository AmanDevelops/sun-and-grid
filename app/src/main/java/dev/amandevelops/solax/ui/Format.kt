package dev.amandevelops.solax.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val shortDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val shortWeekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun number(value: Double, minDecimals: Int = 0, maxDecimals: Int = 1): String {
    val format = NumberFormat.getNumberInstance(Locale.getDefault())
    format.minimumFractionDigits = minDecimals
    format.maximumFractionDigits = maxDecimals
    return format.format(value)
}

fun energyText(kwh: Double): String = when {
    kwh >= 1000 -> "${number(kwh / 1000.0, 2, 2)} MWh"
    abs(kwh) >= 100 -> "${number(kwh, 0, 0)} kWh"
    else -> "${number(kwh, 1, 1)} kWh"
}

fun energyValue(kwh: Double): Pair<String, String> = when {
    kwh >= 1000 -> number(kwh / 1000.0, 2, 2) to "MWh"
    else -> number(kwh, 1, 1) to "kWh"
}

fun powerValue(watts: Double): Pair<String, String> = when {
    abs(watts) >= 1000 -> number(watts / 1000.0, 2, 2) to "kW"
    abs(watts) >= 10 -> number(watts, 0, 0) to "W"
    else -> number(watts, 0, 0) to "W"
}

fun powerText(watts: Double): String {
    val (value, unit) = powerValue(abs(watts))
    return "$value $unit"
}

fun signedPowerText(watts: Double): String {
    val prefix = if (watts >= 0) "+" else "−"
    return prefix + powerText(abs(watts))
}

fun percentText(value: Double): String = "${value.roundToInt()}%"

fun signedPercentText(value: Double): String {
    val rounded = value.roundToInt()
    val sign = if (rounded >= 0) "+" else "−"
    return "$sign${abs(rounded)}%"
}

fun monthTitle(date: LocalDate): String = date.format(monthFormatter)

fun shortDate(date: LocalDate): String = date.format(shortDateFormatter)

fun shortWeekday(date: LocalDate): String = date.format(shortWeekdayFormatter)

fun timeLabel(timestamp: Long): String =
    java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .format(timeFormatter)

fun weekRangeLabel(dates: List<LocalDate>): String {
    if (dates.isEmpty()) return ""
    val start = dates.first()
    val end = dates.last()
    return if (start.month == end.month) {
        "${start.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${start.dayOfMonth} – ${end.dayOfMonth}"
    } else {
        "${start.dayOfMonth} ${start.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} – " +
            "${end.dayOfMonth} ${end.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
    }
}

fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

fun relativeTime(timestamp: Long?, now: Long = System.currentTimeMillis()): String {
    if (timestamp == null) return "Not synced yet"
    val seconds = ((now - timestamp) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 8 -> "Just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}

fun co2Text(kg: Double): Pair<String, String> = when {
    kg >= 1000 -> number(kg / 1000.0, 2, 2) to "t CO₂"
    else -> number(kg, 0, 0) to "kg CO₂"
}

fun weekdayLetter(date: LocalDate): String =
    date.dayOfWeek.name.take(1)

fun gridDirectionLabel(gridExportW: Double): String = when {
    gridExportW > 30 -> "Exporting to grid"
    gridExportW < -30 -> "Importing from grid"
    else -> "Grid idle"
}
