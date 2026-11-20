package com.example

import com.example.data.model.AccentTheme
import com.example.data.model.AsrMethod
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.CalculationMethod
import com.example.data.model.WaqtType
import com.example.util.BanglaUtils
import com.example.util.PrayerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

class NoorCoreLogicTest {

    @Test
    fun all64DistrictsHaveBothBanglaAndEnglishNames() {
        assertEquals(64, BangladeshiDistricts.ALL_64.size)
        assertEquals(64, BangladeshiDistricts.ALL_64.map { it.id }.toSet().size)
        BangladeshiDistricts.ALL_64.forEach { d ->
            assertTrue("Missing Bangla name for ${d.id}", d.nameBn.isNotBlank())
            assertTrue("Missing English name for ${d.id}", d.nameEn.isNotBlank())
            assertTrue("Missing Bangla division for ${d.id}", d.divisionBn.isNotBlank())
            assertTrue("Missing English division for ${d.id}", d.divisionEn.isNotBlank())
        }
        val nearestToDhaka = BangladeshiDistricts.findNearest(23.81, 90.41)
        assertEquals("dhaka", nearestToDhaka.id)
        assertEquals("Dhaka", nearestToDhaka.displayName(isEnglish = true))
        assertEquals("ঢাকা", nearestToDhaka.displayName(isEnglish = false))
    }

    @Test
    fun dhakaPrayerTimesMatchAttachedPdfRegardlessOfDeviceTimezone() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            val lat = 23.7167
            val lon = 90.4000
            val tz = PrayerTimeCalculator.resolveTimeZone(lat, lon, isManualDistrict = false)
            assertEquals("Asia/Dhaka", tz.id)

            val nov18 = Calendar.getInstance(tz).apply { set(2026, Calendar.NOVEMBER, 18, 12, 0, 0) }
            val novSchedule = PrayerTimeCalculator.calculateForDate(
                calendar = nov18,
                latitude = lat,
                longitude = lon,
                asrMethod = AsrMethod.HANAFI,
                timeZoneHours = tz.getOffset(nov18.timeInMillis) / 3_600_000.0
            )
            assertTrue("Nov 18 Fajr (${novSchedule.fajrMinute}) ~ 04:56 (296)", abs(novSchedule.fajrMinute - 296) <= 2)
            assertTrue("Nov 18 Sunrise (${novSchedule.sunriseMinute}) ~ 06:15 (375)", abs(novSchedule.sunriseMinute - 375) <= 2)
            assertTrue("Nov 18 Dhuhr (${novSchedule.dhuhrMinute}) ~ 11:44 (704)", abs(novSchedule.dhuhrMinute - 704) <= 2)
            assertTrue("Nov 18 Asr (${novSchedule.asrMinute}) ~ 15:37 (937)", abs(novSchedule.asrMinute - 937) <= 2)
            assertTrue("Nov 18 Maghrib (${novSchedule.maghribMinute}) ~ 17:15 (1035)", abs(novSchedule.maghribMinute - 1035) <= 2)
            assertTrue("Nov 18 Isha (${novSchedule.ishaMinute}) ~ 18:31 (1111)", abs(novSchedule.ishaMinute - 1111) <= 2)

            val jan1 = Calendar.getInstance(tz).apply { set(2026, Calendar.JANUARY, 1, 12, 0, 0) }
            val janSchedule = PrayerTimeCalculator.calculateForDate(
                calendar = jan1,
                latitude = lat,
                longitude = lon,
                asrMethod = AsrMethod.HANAFI,
                timeZoneHours = tz.getOffset(jan1.timeInMillis) / 3_600_000.0
            )
            assertTrue("Jan 1 Fajr (${janSchedule.fajrMinute}) ~ 05:20 (320)", abs(janSchedule.fajrMinute - 320) <= 2)
            assertTrue("Jan 1 Dhuhr (${janSchedule.dhuhrMinute}) ~ 12:02 (722)", abs(janSchedule.dhuhrMinute - 722) <= 2)
            assertTrue("Jan 1 Asr (${janSchedule.asrMinute}) ~ 15:47 (947)", abs(janSchedule.asrMinute - 947) <= 2)
            assertTrue("Jan 1 Maghrib (${janSchedule.maghribMinute}) ~ 17:26 (1046)", abs(janSchedule.maghribMinute - 1046) <= 2)
            assertTrue("Jan 1 Isha (${janSchedule.ishaMinute}) ~ 18:44 (1124)", abs(janSchedule.ishaMinute - 1124) <= 2)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun worldwideGpsAdaptsTimezoneAndCalculationMethodAutomatically() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dhaka"))

            val dhakaCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
                set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
            }
            val dhakaSchedule = PrayerTimeCalculator.calculateForDate(
                dhakaCal, 23.8103, 90.4125, AsrMethod.HANAFI, 6.0
            )

            // London -> Europe/London + MWL (must differ from Dhaka times!)
            val londonTz = PrayerTimeCalculator.resolveTimeZone(51.5074, -0.1278, isManualDistrict = false)
            assertEquals("Europe/London", londonTz.id)
            assertEquals(
                CalculationMethod.MWL,
                PrayerTimeCalculator.resolveCalculationMethod(51.5074, -0.1278, CalculationMethod.AUTO)
            )
            val londonCal = Calendar.getInstance(londonTz).apply { set(2026, Calendar.NOVEMBER, 18, 12, 0, 0) }
            val londonSchedule = PrayerTimeCalculator.calculateForDate(
                calendar = londonCal,
                latitude = 51.5074,
                longitude = -0.1278,
                asrMethod = AsrMethod.HANAFI,
                timeZoneHours = londonTz.getOffset(londonCal.timeInMillis) / 3_600_000.0
            )
            assertTrue(londonSchedule.fajrMinute in 300..360)
            assertTrue(londonSchedule.dhuhrMinute in 690..740)
            assertTrue(londonSchedule.maghribMinute in 950..1020)
            assertNotEquals(dhakaSchedule.maghribMinute, londonSchedule.maghribMinute)

            // Makkah -> Asia/Riyadh + Umm Al-Qura (Isha = Maghrib + 90m)
            val makkahTz = PrayerTimeCalculator.resolveTimeZone(21.4225, 39.8262, isManualDistrict = false)
            assertEquals("Asia/Riyadh", makkahTz.id)
            assertEquals(
                CalculationMethod.UMM_AL_QURA,
                PrayerTimeCalculator.resolveCalculationMethod(21.4225, 39.8262, CalculationMethod.AUTO)
            )
            val makkahCal = Calendar.getInstance(makkahTz).apply { set(2026, Calendar.NOVEMBER, 18, 12, 0, 0) }
            val makkahSchedule = PrayerTimeCalculator.calculateForDate(
                calendar = makkahCal,
                latitude = 21.4225,
                longitude = 39.8262,
                asrMethod = AsrMethod.SHAFI,
                timeZoneHours = makkahTz.getOffset(makkahCal.timeInMillis) / 3_600_000.0
            )
            assertEquals((makkahSchedule.maghribMinute + 90) % 1440, makkahSchedule.ishaMinute)

            // New York -> America/New_York + ISNA
            val nyTz = PrayerTimeCalculator.resolveTimeZone(40.7128, -74.0060, isManualDistrict = false)
            assertEquals("America/New_York", nyTz.id)
            assertEquals(
                CalculationMethod.ISNA,
                PrayerTimeCalculator.resolveCalculationMethod(40.7128, -74.0060, CalculationMethod.AUTO)
            )

            // Dubai, Doha, Cairo, Tokyo, Lahore
            assertEquals("Asia/Dubai", PrayerTimeCalculator.resolveTimeZone(25.2048, 55.2708, isManualDistrict = false).id)
            assertEquals("Asia/Qatar", PrayerTimeCalculator.resolveTimeZone(25.2854, 51.5310, isManualDistrict = false).id)
            assertEquals("Africa/Cairo", PrayerTimeCalculator.resolveTimeZone(30.0444, 31.2357, isManualDistrict = false).id)
            assertEquals("Asia/Tokyo", PrayerTimeCalculator.resolveTimeZone(35.6762, 139.6503, isManualDistrict = false).id)
            assertEquals("Asia/Karachi", PrayerTimeCalculator.resolveTimeZone(31.5204, 74.3587, isManualDistrict = false).id)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun eachWaqtHasCorrectStartAndEndTimesAndMultipleThemesExist() {
        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
        val cal = Calendar.getInstance(dhakaTz).apply {
            set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
        }
        val tomorrowCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val dhaka = BangladeshiDistricts.findById("dhaka")
        val tomorrow = PrayerTimeCalculator.calculateForDate(tomorrowCal, dhaka.latitude, dhaka.longitude, AsrMethod.HANAFI, 6.0)
        val today = PrayerTimeCalculator.calculateForDate(cal, dhaka.latitude, dhaka.longitude, AsrMethod.HANAFI, 6.0)
            .copy(ishaEndMinute = tomorrow.fajrMinute)

        assertEquals(today.sunriseMinute, today.endMinuteFor(WaqtType.FAJR))
        assertEquals(today.asrMinute, today.endMinuteFor(WaqtType.DHUHR))
        assertEquals(today.sunsetMinute, today.endMinuteFor(WaqtType.ASR))
        assertEquals(today.ishaMinute, today.endMinuteFor(WaqtType.MAGHRIB))
        assertEquals(tomorrow.fajrMinute, today.endMinuteFor(WaqtType.ISHA))

        assertEquals(6, AccentTheme.entries.size)
        AccentTheme.entries.forEach { accent ->
            assertEquals(accent, AccentTheme.safeValueOf(accent.name))
            assertTrue(accent.title(true).isNotBlank())
            assertTrue(accent.title(false).isNotBlank())
        }
    }

    @Test
    fun bilingualDigitsTimesAndCalendarsFormatCorrectly() {
        assertEquals("১২:৩০", BanglaUtils.formatDigits("12:30", isEnglish = false))
        assertEquals("12:30", BanglaUtils.formatDigits("12:30", isEnglish = true))

        assertEquals("04:56 AM", BanglaUtils.formatTime12h(296, isEnglish = true))
        assertEquals("০৪:৫৬ ভোর", BanglaUtils.formatTime12h(296, isEnglish = false))
        assertEquals("05:15 PM", BanglaUtils.formatTime12h(1035, isEnglish = true))

        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
            set(2026, Calendar.APRIL, 14, 10, 0, 0)
        }
        assertTrue(BanglaUtils.getBanglaCalendarDate(cal, isEnglish = false).contains("১ বৈশাখ"))
        assertTrue(BanglaUtils.getBanglaCalendarDate(cal, isEnglish = true).contains("1 Boishakh"))
        assertTrue(BanglaUtils.getHijriDate(cal, isEnglish = false).contains("হিজরি"))
        assertTrue(BanglaUtils.getHijriDate(cal, isEnglish = true).contains("AH"))
        assertTrue(BanglaUtils.getGregorianDate(cal, isEnglish = true).contains("April"))
        assertFalse(BanglaUtils.getGregorianDate(cal, isEnglish = true).contains("এপ্রিল"))
    }
}
