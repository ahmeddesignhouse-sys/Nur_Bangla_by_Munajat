package com.example

import com.example.data.model.AsrMethod
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.CalculationMethod
import com.example.util.BanglaUtils
import com.example.util.PrayerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

class NoorCoreLogicTest {

    @Test
    fun all64DistrictsArePresentAndUnique() {
        assertEquals(64, BangladeshiDistricts.ALL_64.size)
        assertEquals(64, BangladeshiDistricts.ALL_64.map { it.id }.toSet().size)
        val nearestToDhaka = BangladeshiDistricts.findNearest(23.81, 90.41)
        assertEquals("dhaka", nearestToDhaka.id)
    }

    @Test
    fun dhakaPrayerTimesMatchAttachedPdfRegardlessOfDeviceTimezone() {
        val originalTz = TimeZone.getDefault()
        try {
            // Simulate device/emulator set to US Pacific Time
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            // Coordinates from attached Dhaka PDF: 23°43' N, 90°24' E
            val lat = 23.7167
            val lon = 90.4000
            val tz = PrayerTimeCalculator.resolveTimeZone(lat, lon, isManualDistrict = false)
            assertEquals("Asia/Dhaka", tz.id)

            // Nov 18 check against attached PDF (Fajr 4:56, Sunrise 6:15, Zuhr 11:44, Asr 3:37, Maghrib 5:15, Isha 6:31)
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

            // Jan 1 check against attached PDF (Fajr 5:20, Sunrise 6:40, Zuhr 12:02, Asr 3:47, Maghrib 5:26, Isha 6:44)
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

            // London -> Europe/London + MWL
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

            // Dubai -> Asia/Dubai, Lahore -> Asia/Karachi
            assertEquals("Asia/Dubai", PrayerTimeCalculator.resolveTimeZone(25.2048, 55.2708, isManualDistrict = false).id)
            assertEquals("Asia/Karachi", PrayerTimeCalculator.resolveTimeZone(31.5204, 74.3587, isManualDistrict = false).id)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun prayerTimesAndQiblaForDhakaAreValid() {
        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
        val cal = Calendar.getInstance(dhakaTz).apply {
            set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
        }
        val dhaka = BangladeshiDistricts.findById("dhaka")
        val hanafi = PrayerTimeCalculator.calculateForDate(cal, dhaka.latitude, dhaka.longitude, AsrMethod.HANAFI, 6.0)
        val shafi = PrayerTimeCalculator.calculateForDate(cal, dhaka.latitude, dhaka.longitude, AsrMethod.SHAFI, 6.0)

        assertTrue(hanafi.sehriEndMinute < hanafi.fajrMinute)
        assertTrue(hanafi.fajrMinute < hanafi.sunriseMinute)
        assertTrue(hanafi.sunriseMinute < hanafi.dhuhrMinute)
        assertTrue(shafi.asrMinute < hanafi.asrMinute)
        assertTrue(hanafi.asrMinute < hanafi.sunsetMinute)
        assertTrue(hanafi.sunsetMinute < hanafi.maghribMinute)
        assertTrue(hanafi.maghribMinute < hanafi.ishaMinute)
        assertEquals(3, hanafi.forbiddenTimes.size)

        val bearing = PrayerTimeCalculator.calculateQiblaBearing(dhaka.latitude, dhaka.longitude)
        assertTrue(bearing in 275f..282f)
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

        assertEquals(today.sunriseMinute, today.endMinuteFor(com.example.data.model.WaqtType.FAJR))
        assertEquals(today.asrMinute, today.endMinuteFor(com.example.data.model.WaqtType.DHUHR))
        assertEquals(today.sunsetMinute, today.endMinuteFor(com.example.data.model.WaqtType.ASR))
        assertEquals(today.ishaMinute, today.endMinuteFor(com.example.data.model.WaqtType.MAGHRIB))
        assertEquals(tomorrow.fajrMinute, today.endMinuteFor(com.example.data.model.WaqtType.ISHA))

        assertEquals(6, com.example.data.model.AccentTheme.entries.size)
        com.example.data.model.AccentTheme.entries.forEach { accent ->
            assertEquals(accent, com.example.data.model.AccentTheme.safeValueOf(accent.name))
        }
    }

    @Test
    fun banglaDigitsAndCalendarsFormatCorrectly() {
        assertEquals("১২:৩০", BanglaUtils.toBanglaDigits("12:30"))
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
            set(2026, Calendar.APRIL, 14, 10, 0, 0)
        }
        assertTrue(BanglaUtils.getBanglaCalendarDate(cal).contains("১ বৈশাখ"))
        assertTrue(BanglaUtils.getHijriDateBangla(cal).contains("হিজরি"))
    }
}
