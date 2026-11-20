package com.example

import com.example.util.HijriCalendarEngine
import com.example.util.HijriDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HijriCalendarTest {

    @Test
    fun gregorianAndHijriRoundTripAndMoonSightingAdjustmentAreExact() {
        val tz = TimeZone.getTimeZone("Asia/Dhaka")
        val nov20 = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 20, 12, 0, 0)
        }

        val h0 = HijriCalendarEngine.fromGregorian(nov20, dayOffset = 0)
        val hPlus1 = HijriCalendarEngine.fromGregorian(nov20, dayOffset = 1)
        val hMinus1 = HijriCalendarEngine.fromGregorian(nov20, dayOffset = -1)

        assertEquals(1448, h0.year)
        assertEquals(6, h0.month) // Jumada al-Thani 1448
        assertEquals(9, h0.day)
        assertEquals(10, hPlus1.day)
        assertEquals(8, hMinus1.day)

        // Round-trip back to Gregorian under each offset
        listOf(-1, 0, 1).forEach { offset ->
            val h = HijriCalendarEngine.fromGregorian(nov20, dayOffset = offset)
            val gBack = HijriCalendarEngine.toGregorian(h, dayOffset = offset)
            assertEquals(2026, gBack.year)
            assertEquals(11, gBack.month)
            assertEquals(20, gBack.day)
        }
    }

    @Test
    fun monthNavigationAndMonthGridCellsCoverAll29Or30Days() {
        assertEquals(1449 to 1, HijriCalendarEngine.shiftHijriMonth(1448, 12, 1))
        assertEquals(1447 to 12, HijriCalendarEngine.shiftHijriMonth(1448, 1, -1))

        for (month in 1..12) {
            val cells = HijriCalendarEngine.buildMonthCells(1448, month, dayOffset = 0)
            assertTrue("Month $month should have 29 or 30 days", cells.size in 29..30)
            assertEquals(1, cells.first().hijriDate.day)
            assertEquals(cells.size, cells.last().hijriDate.day)
            // Verify consecutive Gregorian dates
            for (i in 1 until cells.size) {
                val prevJdn = HijriCalendarEngine.gregorianToJdn(
                    cells[i - 1].gregorianDate.year,
                    cells[i - 1].gregorianDate.month,
                    cells[i - 1].gregorianDate.day
                )
                val currJdn = HijriCalendarEngine.gregorianToJdn(
                    cells[i].gregorianDate.year,
                    cells[i].gregorianDate.month,
                    cells[i].gregorianDate.day
                )
                assertEquals(prevJdn + 1L, currJdn)
            }
        }
    }

    @Test
    fun allRequiredIslamicEventsAreMarkedWithBilingualLabelsAndDates() {
        val events = HijriCalendarEngine.eventsForHijriYear(1448, dayOffset = 0)
        val ids = events.map { it.event.id }.toSet()

        val requiredIds = listOf(
            "ramadan_start",
            "eid_ul_fitr",
            "eid_ul_adha",
            "shab_e_barat",
            "shab_e_meraj",
            "ashura",
            "miladunnabi",
            "hajj_tarwiyah",
            "hajj_arafah"
        )
        requiredIds.forEach { reqId ->
            assertTrue("Missing required Islamic event: $reqId", ids.contains(reqId))
        }

        events.forEach { occ ->
            assertTrue(occ.event.titleBn.isNotBlank())
            assertTrue(occ.event.titleEn.isNotBlank())
            assertTrue(occ.event.descriptionBn.isNotBlank())
            assertTrue(occ.event.descriptionEn.isNotBlank())
            assertTrue(occ.gregorianDate.year in 2026..2027)
        }

        val ramadanFormattedEn = HijriCalendarEngine.formatHijriDate(HijriDate(1448, 9, 1), isEnglish = true)
        val ramadanFormattedBn = HijriCalendarEngine.formatHijriDate(HijriDate(1448, 9, 1), isEnglish = false)
        assertEquals("1 Ramadan 1448 AH", ramadanFormattedEn)
        assertEquals("১ রমজান ১৪৪৮ হিজরি", ramadanFormattedBn)
    }
}
