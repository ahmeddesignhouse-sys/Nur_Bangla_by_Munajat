package com.example

import com.example.data.model.AsrMethod
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.CalculationMethod
import com.example.data.repository.MosqueRepository
import com.example.util.NisabBasis
import com.example.util.PrayerTimeCalculator
import com.example.util.RamadanEngine
import com.example.util.WeightUnit
import com.example.util.ZakatCalculator
import com.example.util.ZakatHistoryRecord
import com.example.util.ZakatInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorV13FeatureTest {

    @Test
    fun ramadanTimetableHas30DaysHighlightsTodayRecalculatesByLocationAndCountsDown() {
        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
        val dhaka = BangladeshiDistricts.findById("dhaka")
        val sylhet = BangladeshiDistricts.findById("sylhet")

        // Ramadan 1448 starts 8 Feb 2027; pick 10 Ramadan 1448 as simulated "today"
        val day10Cal = Calendar.getInstance(dhakaTz).apply {
            set(2027, Calendar.FEBRUARY, 17, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertTrue(RamadanEngine.isCurrentlyRamadan(day10Cal))

        val dhakaDays = RamadanEngine.buildRamadanTimetable(
            ramadanHijriYear = 1448,
            latitude = dhaka.latitude,
            longitude = dhaka.longitude,
            timeZone = dhakaTz,
            asrMethod = AsrMethod.HANAFI,
            calculationMethod = CalculationMethod.BANGLADESH_KARACHI,
            hijriDayOffset = 0,
            todayCalendar = day10Cal
        )
        assertEquals(30, dhakaDays.size)
        assertEquals(1, dhakaDays.first().ramadanDay)
        assertEquals(30, dhakaDays.last().ramadanDay)
        assertEquals(1, dhakaDays.count { it.isToday })
        assertEquals(10, dhakaDays.first { it.isToday }.ramadanDay)

        // Verify each day has sensible Sehri <= Fajr < Maghrib <= Iftar
        dhakaDays.forEach { d ->
            assertEquals(9, d.hijriDate.month)
            assertTrue(d.sehriEndMinute <= d.fajrMinute)
            assertTrue(d.fajrMinute < d.maghribMinute)
            assertTrue(d.maghribMinute <= d.iftarMinute)
        }

        // Recalculates when location changes (Sylhet is farther east -> earlier Iftar)
        val sylhetDays = RamadanEngine.buildRamadanTimetable(
            ramadanHijriYear = 1448,
            latitude = sylhet.latitude,
            longitude = sylhet.longitude,
            timeZone = dhakaTz,
            asrMethod = AsrMethod.HANAFI,
            calculationMethod = CalculationMethod.BANGLADESH_KARACHI,
            hijriDayOffset = 0,
            todayCalendar = day10Cal
        )
        assertNotEquals(dhakaDays.first().iftarMinute, sylhetDays.first().iftarMinute)
        assertTrue(sylhetDays.first().iftarMinute < dhakaDays.first().iftarMinute)

        // Live countdown check (3:00 PM -> Next Iftar)
        val todaySched = PrayerTimeCalculator.calculateForDate(
            day10Cal, dhaka.latitude, dhaka.longitude, AsrMethod.HANAFI, 6.0
        )
        val cdAfternoon = RamadanEngine.computeSehriIftarCountdown(day10Cal, todaySched, todaySched.sehriEndMinute)
        assertTrue(cdAfternoon.isNextIftar)
        assertTrue(cdAfternoon.remainingSeconds > 0)
        assertTrue(cdAfternoon.label(isEnglish = true).startsWith("Next Iftar in "))
        assertTrue(cdAfternoon.label(isEnglish = false).startsWith("পরবর্তী ইফতার বাকি "))

        // After Iftar (8:00 PM -> Next Sehri tomorrow)
        val nightCal = Calendar.getInstance(dhakaTz).apply {
            set(2027, Calendar.FEBRUARY, 17, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cdNight = RamadanEngine.computeSehriIftarCountdown(nightCal, todaySched, todaySched.sehriEndMinute)
        assertFalse(cdNight.isNextIftar)
        assertTrue(cdNight.label(isEnglish = true).startsWith("Next Sehri in "))
        assertTrue(cdNight.label(isEnglish = false).startsWith("পরবর্তী সাহরি বাকি "))
    }

    @Test
    fun zakatCalculatorSampleCash100kPlusGold50gVerifiesNisabAndTwoPointFivePercent() {
        // Sample from spec: Cash 100,000 BDT + Gold 50g
        // Default Gold = 13,500 BDT/g -> 50g = 675,000 BDT -> Total = 775,000 BDT
        val silverNisabInput = ZakatInput(
            cashInHandBdt = 100_000.0,
            goldWeight = 50.0,
            goldUnit = WeightUnit.GRAM,
            goldPricePerGramBdt = 13_500.0,
            silverPricePerGramBdt = 210.0,
            nisabBasis = NisabBasis.SILVER
        )
        val silverResult = ZakatCalculator.calculate(silverNisabInput)
        assertEquals(675_000.0, silverResult.goldValueBdt, 0.01)
        assertEquals(775_000.0, silverResult.netZakatableWealthBdt, 0.01)
        assertEquals(128_595.6, silverResult.silverNisabBdt, 0.1)
        assertTrue(silverResult.isZakatDue)
        assertEquals(19_375.0, silverResult.zakatDueBdt, 0.01)
        assertEquals("BDT 19,375", ZakatCalculator.formatBdt(silverResult.zakatDueBdt, isEnglish = true))
        assertEquals("৳ ১৯,৩৭৫", ZakatCalculator.formatBdt(silverResult.zakatDueBdt, isEnglish = false))

        // Under Gold Nisab (87.48g * 13,500 = 1,180,980 BDT): 775,000 < 1,180,980 -> No Zakat due
        val goldResult = ZakatCalculator.calculate(silverNisabInput.copy(nisabBasis = NisabBasis.GOLD))
        assertFalse(goldResult.isZakatDue)
        assertEquals(0.0, goldResult.zakatDueBdt, 0.01)

        // Subtracting debts and using Tola unit (1 tola = 11.664g)
        val tolaInput = ZakatInput(
            bankBalanceBdt = 500_000.0,
            businessGoodsBdt = 200_000.0,
            silverWeight = 10.0,
            silverUnit = WeightUnit.TOLA,
            debtsBdt = 100_000.0,
            silverPricePerGramBdt = 200.0,
            nisabBasis = NisabBasis.SILVER
        )
        val tolaResult = ZakatCalculator.calculate(tolaInput)
        // 10 tola = 116.64g * 200 = 23,328 -> gross = 723,328 -> net = 623,328 -> 2.5% = 15,583.2
        assertEquals(623_328.0, tolaResult.netZakatableWealthBdt, 0.01)
        assertTrue(tolaResult.isZakatDue)
        assertEquals(15_583.2, tolaResult.zakatDueBdt, 0.01)

        // History JSON round-trip
        val record = ZakatHistoryRecord(
            id = "rec-1",
            timestampMillis = 1795132800000L,
            netWealthBdt = 775_000.0,
            nisabThresholdBdt = 128_595.6,
            zakatDueBdt = 19_375.0,
            nisabBasis = NisabBasis.SILVER
        )
        val parsed = ZakatCalculator.parseHistoryJson(ZakatCalculator.serializeHistoryJson(listOf(record)))
        assertEquals(1, parsed.size)
        assertEquals(19_375.0, parsed.first().zakatDueBdt, 0.01)
    }

    @Test
    fun mosqueOverpassParsingSortsByDistanceComputesQiblaAndFiltersBySearch() {
        val sampleJson = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 101,
                  "lat": 23.7289,
                  "lon": 90.4106,
                  "tags": {
                    "name": "Baitul Mukarram National Mosque",
                    "name:bn": "বায়তুল মোকাররম জাতীয় মসজিদ",
                    "addr:street": "Topkhana Road",
                    "addr:city": "Dhaka"
                  }
                },
                {
                  "type": "way",
                  "id": 202,
                  "center": { "lat": 23.8120, "lon": 90.4135 },
                  "tags": {
                    "name": "Gulshan Central Mosque",
                    "name:bn": "গুলশান সেন্ট্রাল মসজিদ",
                    "addr:suburb": "Gulshan"
                  }
                }
              ]
            }
        """.trimIndent()

        // User near Gulshan (23.8103, 90.4125) -> Gulshan Central Mosque must come first
        val mosques = MosqueRepository.parseOverpassResponse(sampleJson, 23.8103, 90.4125)
        assertEquals(2, mosques.size)
        assertEquals("Gulshan Central Mosque", mosques[0].displayName(isEnglish = true))
        assertEquals("গুলশান সেন্ট্রাল মসজিদ", mosques[0].displayName(isEnglish = false))
        assertTrue(mosques[0].distanceKm < mosques[1].distanceKm)
        assertTrue(mosques[0].qiblaBearingDeg in 270f..285f)

        val searchEn = MosqueRepository.filterMosques(mosques, "Baitul")
        assertEquals(1, searchEn.size)
        val searchBn = MosqueRepository.filterMosques(mosques, "গুলশান")
        assertEquals(1, searchBn.size)
    }
}
