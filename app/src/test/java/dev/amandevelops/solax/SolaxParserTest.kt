package dev.amandevelops.solax

import dev.amandevelops.solax.data.Analytics
import dev.amandevelops.solax.data.Reading
import dev.amandevelops.solax.data.SolaxException
import dev.amandevelops.solax.data.SolaxParser
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolaxParserTest {

    private fun payload(data: String, info: String = """["X1-Hybrid-5.0-L","","X1-Hybrid-5.0-L"]"""): String =
        """{"sn":"SR1ES8A2B4C6","Status":2,"Data":$data,"Information":$info}"""

    @Test
    fun `parses the local realtime payload`() {
        val data = (0..29).toMutableList().map { 0 }.toMutableList()
        data[0] = 2302 // 230.2 V
        data[1] = 92 // 9.2 A
        data[2] = 5001 // 50.01 Hz
        data[3] = 3120 // W
        data[4] = 3712 // 371.2 V
        data[8] = 45 // 4.5 A
        data[11] = 1250 // W export
        data[13] = 3120 // W
        data[19] = 86405 // 8640.5 kWh
        data[21] = 187 // 18.7 kWh
        data[23] = 42

        val reading = SolaxParser.parse(
            body = payload(data.joinToString(prefix = "[", postfix = "]")),
            now = 1_700_000_000_000L,
        )

        assertEquals(1_700_000_000_000L, reading.timestamp)
        assertEquals(230.2, reading.gridVoltageV, 0.001)
        assertEquals(9.2, reading.pv1CurrentA, 0.001)
        assertEquals(50.01, reading.gridFrequencyHz, 0.001)
        assertEquals(3120.0, reading.powerW, 0.001)
        assertEquals(371.2, reading.pv1VoltageV, 0.001)
        assertEquals(4.5, reading.gridCurrentA, 0.001)
        assertEquals(1250.0, reading.gridPowerW, 0.001)
        assertEquals(8640.5, reading.totalYieldKwh, 0.001)
        assertEquals(18.7, reading.dailyYieldKwh, 0.001)
        assertEquals(42.0, reading.temperatureC!!, 0.001)
        assertEquals("SR1ES8A2B4C6", reading.inverterSn)
        assertEquals("X1-Hybrid-5.0-L", reading.model)
        assertEquals(2, reading.status)
    }

    @Test
    fun `uses the larger of PV1 and total solar power`() {
        val data = List(30) { 0 }
            .toMutableList()
            .apply {
                this[3] = 800
                this[13] = 2400
            }

        val reading = SolaxParser.parse(payload(data.joinToString(prefix = "[", postfix = "]")))

        assertEquals(2400.0, reading.powerW, 0.001)
        assertEquals(800.0, reading.pv1PowerW, 0.001)
    }

    @Test
    fun `clamps negatives and ignores nonsense temperatures`() {
        val data = List(30) { 0 }
            .toMutableList()
            .apply {
                this[3] = -50
                this[13] = -10
                this[19] = -12
                this[21] = -4
                this[23] = 999
            }

        val reading = SolaxParser.parse(payload(data.joinToString(prefix = "[", postfix = "]")))

        assertEquals(0.0, reading.powerW, 0.001)
        assertEquals(0.0, reading.dailyYieldKwh, 0.001)
        assertNull(reading.temperatureC)
    }

    @Test
    fun `handles missing indices`() {
        val reading = SolaxParser.parse(payload("[2301, 10, 5000]"))

        assertEquals(0.0, reading.powerW, 0.001)
        assertEquals(230.1, reading.gridVoltageV, 0.001)
        assertEquals(0.0, reading.dailyYieldKwh, 0.001)
        assertNull(reading.temperatureC)
    }

    @Test(expected = SolaxException::class)
    fun `rejects payloads with no data array`() {
        SolaxParser.parse("""{"sn":"SR1"}""")
    }

    @Test(expected = SolaxException::class)
    fun `rejects empty data arrays`() {
        SolaxParser.parse(payload("[]"))
    }

    @Test(expected = SolaxException::class)
    fun `rejects invalid JSON`() {
        SolaxParser.parse("<html>login required</html>")
    }
}

class AnalyticsTest {

    private val zone = ZoneId.of("Europe/London")
    private fun at(date: String, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(
            java.time.LocalDateTime.of(LocalDate.parse(date), java.time.LocalTime.of(hour, minute)),
            zone,
        ).toInstant().toEpochMilli()

    private fun reading(
        date: String,
        hour: Int,
        powerW: Double,
        dailyKwh: Double,
        gridW: Double,
        temp: Double? = 30.0,
    ) = Reading(
        timestamp = at(date, hour),
        powerW = powerW,
        dailyYieldKwh = dailyKwh,
        gridPowerW = gridW,
        temperatureC = temp,
    )

    @Test
    fun `weeks run Sunday to Saturday`() {
        val wednesday = LocalDate.parse("2026-11-04")
        val week = Analytics.weekDates(wednesday)

        assertEquals(LocalDate.parse("2026-11-01"), week.first())
        assertEquals(LocalDate.parse("2026-11-07"), week.last())
        assertEquals(7, week.size)
    }

    @Test
    fun `aggregates daily peak, yield and temperature`() {
        val history = listOf(
            reading("2026-11-04", 9, 1200.0, 1.2, 400.0, 28.0),
            reading("2026-11-04", 12, 3400.0, 8.6, -500.0, 36.0),
            reading("2026-11-04", 15, 2100.0, 12.1, 900.0, 32.0),
        )

        val aggregate = Analytics.aggregate(history, live = null, zone = zone)
        val day = aggregate.getValue(LocalDate.parse("2026-11-04"))

        assertEquals(12.1, day.kwh!!, 0.001)
        assertEquals(3400.0, day.peakW!!, 0.001)
        assertEquals(32.0, day.avgTempC!!, 0.001)
        assertEquals(3, day.sampleCount)
    }

    @Test
    fun `live reading is included in today's bucket`() {
        val history = listOf(reading("2026-11-04", 9, 1200.0, 1.2, 400.0))
        val live = reading("2026-11-04", 12, 4000.0, 9.9, 1500.0)

        val aggregate = Analytics.aggregate(history, live, zone)

        assertEquals(9.9, aggregate.getValue(LocalDate.parse("2026-11-04")).kwh!!, 0.001)
        assertEquals(4000.0, aggregate.getValue(LocalDate.parse("2026-11-04")).peakW!!, 0.001)
    }

    @Test
    fun `grid sign inversion flips export and import`() {
        val exporting = reading("2026-11-04", 12, 3000.0, 5.0, 1200.0)
        val importing = reading("2026-11-04", 18, 0.0, 9.0, -600.0)

        assertEquals(1200.0, Analytics.gridExportW(exporting, invertSign = false), 0.001)
        assertEquals(-1200.0, Analytics.gridExportW(exporting, invertSign = true), 0.001)
        assertEquals(-600.0, Analytics.gridExportW(importing, invertSign = false), 0.001)
        assertEquals(600.0, Analytics.gridExportW(importing, invertSign = true), 0.001)
    }

    @Test
    fun `home use is generation minus export and never negative`() {
        val exporting = reading("2026-11-04", 12, 3000.0, 5.0, 1200.0)
        val idle = reading("2026-11-05", 2, 0.0, 0.0, -500.0)

        assertEquals(1800.0, Analytics.homeUseW(exporting, invertSign = false), 0.001)
        assertEquals(500.0, Analytics.homeUseW(idle, invertSign = false), 0.001)
    }

    @Test
    fun `period totals sum only dates with data`() {
        val aggregates = Analytics.aggregate(
            listOf(
                reading("2026-11-01", 12, 1000.0, 4.0, 0.0),
                reading("2026-11-03", 12, 1000.0, 6.5, 0.0),
            ),
            live = null,
            zone = zone,
        )

        val total = Analytics.periodTotal(
            listOf(
                LocalDate.parse("2026-11-01"),
                LocalDate.parse("2026-11-02"),
                LocalDate.parse("2026-11-03"),
            ),
            aggregates,
        )

        assertEquals(10.5, total, 0.001)
    }

    @Test
    fun `demo history is deterministic and plausible`() {
        val zone = ZoneId.of("Europe/London")
        val now = Instant.parse("2026-11-06T12:00:00Z")
        val first = dev.amandevelops.solax.data.DemoData.history(now, zone)
        val second = dev.amandevelops.solax.data.DemoData.history(now, zone)

        assertEquals(first.size, second.size)
        assertTrue(first.size > 700)
        assertTrue(first.all { it.powerW >= 0.0 })
        assertTrue(first.all { it.dailyYieldKwh in 0.0..40.0 })
        assertTrue(first.zipWithNext().all { (a, b) -> a.timestamp <= b.timestamp })
        assertTrue(first.map { it.totalYieldKwh }.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `demo live output changes through the day`() {
        val zone = ZoneId.of("Europe/London")
        val morning = dev.amandevelops.solax.data.DemoData.live(Instant.parse("2026-11-06T09:00:00Z"), zone)
        val noon = dev.amandevelops.solax.data.DemoData.live(Instant.parse("2026-11-06T12:30:00Z"), zone)
        val night = dev.amandevelops.solax.data.DemoData.live(Instant.parse("2026-11-06T23:00:00Z"), zone)

        assertTrue(noon.powerW > morning.powerW)
        assertEquals(0.0, night.powerW, 0.001)
        assertTrue(abs(noon.gridPowerW) < noon.powerW + 1.0)
    }
}
