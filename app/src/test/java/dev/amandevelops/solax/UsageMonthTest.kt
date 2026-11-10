package dev.amandevelops.solax

import dev.amandevelops.solax.data.UppclDayConsumption
import dev.amandevelops.solax.data.UppclMonthData
import dev.amandevelops.solax.ui.UsageMonth
import dev.amandevelops.solax.ui.toUsageMonth
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageMonthTest {

    @Test
    fun `weeksToDate starts on the Sunday containing day one`() {
        // November 2026 starts on a Sunday.
        val month = UsageMonth(
            yearMonth = YearMonth.of(2026, 11),
            kwhByDay = (1..8).associateWith { it * 1.0 },
        )

        val weeks = month.weeksToDate()

        assertEquals(2, weeks.size)
        assertEquals("2026-11-01", weeks[0].start.toString())
        assertEquals((1..7).toList(), weeks[0].days.map { it?.day })
        assertEquals("2026-11-08", weeks[1].start.toString())
        assertEquals(8, weeks[1].days[0]?.day)
        assertTrue(weeks[1].days.drop(1).all { it == null })
    }

    @Test
    fun `weeksToDate pads days before the first of the month`() {
        // April 2026 starts on a Wednesday, so the first week begins in March.
        val month = UsageMonth(
            yearMonth = YearMonth.of(2026, 4),
            kwhByDay = (1..4).associateWith { 2.0 },
        )

        val weeks = month.weeksToDate()

        assertEquals(1, weeks.size)
        assertEquals("2026-03-29", weeks[0].start.toString())
        assertTrue(weeks[0].days.take(3).all { it == null })
        assertEquals(listOf(1, 2, 3, 4), weeks[0].days.drop(3).map { it?.day })
    }

    @Test
    fun `weeksToDate is empty without posted readings`() {
        assertTrue(UsageMonth.empty(YearMonth.of(2026, 11)).weeksToDate().isEmpty())
    }

    @Test
    fun `toUsageMonth drops trailing unposted zero days`() {
        val data = UppclMonthData(
            year = 2026,
            month = 11,
            days = listOf(
                UppclDayConsumption(1, 4.0),
                UppclDayConsumption(2, 5.0),
                UppclDayConsumption(3, 0.0),
                UppclDayConsumption(4, 0.0),
            ),
            fetchedAt = 0L,
        )

        val month = data.toUsageMonth()

        assertEquals(2, month.lastPostedDay)
        assertEquals(9.0, month.totalKwh, 0.0001)
        assertNull(month.kwhOn(3))
    }
}
