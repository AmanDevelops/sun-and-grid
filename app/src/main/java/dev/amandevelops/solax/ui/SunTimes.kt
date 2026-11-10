package dev.amandevelops.solax.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.PI
import kotlin.math.roundToLong

data class SunTimes(
    val sunrise: LocalTime,
    val sunset: LocalTime,
) {
    fun daylightProgress(now: LocalTime): Float {
        val start = sunrise.toSecondOfDay().toFloat()
        val end = sunset.toSecondOfDay().toFloat()
        if (end <= start) return 0f
        return ((now.toSecondOfDay() - start) / (end - start)).coerceIn(0f, 1f)
    }
}

/**
 * Sunrise and sunset for the UPPCL service region (Lucknow). No location
 * permission is needed, and the calculation is cached per calendar day.
 */
object SunTimesCalculator {
    private const val LATITUDE = 26.8467
    private const val LONGITUDE = 80.9462
    private const val ZENITH = 90.833

    private var cachedDate: LocalDate? = null
    private var cached: SunTimes? = null

    @Synchronized
    fun forDate(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): SunTimes {
        cachedDate?.let { if (it == date) return cached ?: fallback() }
        val computed = compute(date, zone)
        cachedDate = date
        cached = computed
        return computed
    }

    private fun compute(date: LocalDate, zone: ZoneId): SunTimes {
        val dayOfYear = date.dayOfYear
        val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1)
        val equationOfTime = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma)
            )
        val declination =
            0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) +
                0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) +
                0.00148 * sin(3 * gamma)

        val latRad = Math.toRadians(LATITUDE)
        val cosHourAngle = cos(Math.toRadians(ZENITH)) /
            (cos(latRad) * cos(declination)) -
            tan(latRad) * tan(declination)
        if (cosHourAngle !in -1.0..1.0) return fallback()
        val hourAngle = Math.toDegrees(acos(cosHourAngle))

        val sunriseUtc = 720.0 - 4.0 * (LONGITUDE + hourAngle) - equationOfTime
        val sunsetUtc = 720.0 - 4.0 * (LONGITUDE - hourAngle) - equationOfTime
        val utcMidnight = date.atStartOfDay(ZoneOffset.UTC).toInstant()

        fun toLocal(minutes: Double): LocalTime =
            utcMidnight.plusSeconds((minutes * 60.0).roundToLong())
                .atZone(zone)
                .toLocalTime()

        return SunTimes(
            sunrise = toLocal(sunriseUtc),
            sunset = toLocal(sunsetUtc),
        )
    }

    private fun fallback(): SunTimes = SunTimes(LocalTime.of(6, 12), LocalTime.of(17, 41))
}

private val sunTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun sunTimeLabel(time: LocalTime): String =
    time.format(sunTimeFormatter).lowercase(Locale.getDefault())
