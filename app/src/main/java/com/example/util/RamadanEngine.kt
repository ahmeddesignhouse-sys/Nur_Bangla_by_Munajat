package com.example.util

import com.example.data.model.AsrMethod
import com.example.data.model.CalculationMethod
import com.example.data.model.DailyPrayerSchedule
import java.util.Calendar
import java.util.TimeZone

data class RamadanDayEntry(
    val ramadanDay: Int, // 1..30
    val hijriDate: HijriDate,
    val gregorianDate: GregorianDate,
    val sehriEndMinute: Int,
    val fajrMinute: Int,
    val maghribMinute: Int,
    val iftarMinute: Int,
    val isToday: Boolean
)

data class SehriIftarCountdown(
    val isNextIftar: Boolean,
    val targetMinuteOfDay: Int,
    val remainingSeconds: Long
) {
    fun label(isEnglish: Boolean): String {
        val timer = BanglaUtils.formatCountdown(remainingSeconds, isEnglish)
        return if (isNextIftar) {
            if (isEnglish) "Next Iftar in $timer" else "পরবর্তী ইফতার বাকি $timer"
        } else {
            if (isEnglish) "Next Sehri in $timer" else "পরবর্তী সাহরি বাকি $timer"
        }
    }

    fun shortEventTitle(isEnglish: Boolean): String {
        return if (isNextIftar) {
            if (isEnglish) "Next Iftar" else "পরবর্তী ইফতার"
        } else {
            if (isEnglish) "Next Sehri" else "পরবর্তী সাহরি"
        }
    }
}

object RamadanEngine {

    /**
     * Returns true if the given date falls inside the month of Ramadan (Hijri month 9).
     */
    fun isCurrentlyRamadan(calendar: Calendar, hijriDayOffset: Int = 0): Boolean {
        return HijriCalendarEngine.fromGregorian(calendar, hijriDayOffset).month == 9
    }

    /**
     * Picks the active or upcoming Ramadan Hijri year for the current date.
     * If we are in months 1..9, returns the current Hijri year; if after Ramadan (10..12), returns next Hijri year.
     */
    fun defaultRamadanHijriYear(calendar: Calendar, hijriDayOffset: Int = 0): Int {
        val today = HijriCalendarEngine.fromGregorian(calendar, hijriDayOffset)
        return if (today.month <= 9) today.year else today.year + 1
    }

    /**
     * Generates a complete 30-day Ramadan timetable for the specified Hijri year and user location.
     * Works 100% offline using astronomical calculation and recalculates automatically whenever location changes.
     */
    fun buildRamadanTimetable(
        ramadanHijriYear: Int,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone,
        asrMethod: AsrMethod,
        calculationMethod: CalculationMethod,
        hijriDayOffset: Int = 0,
        todayCalendar: Calendar = Calendar.getInstance(timeZone)
    ): List<RamadanDayEntry> {
        val todayJdn = HijriCalendarEngine.gregorianToJdn(
            year = todayCalendar.get(Calendar.YEAR),
            month = todayCalendar.get(Calendar.MONTH) + 1,
            day = todayCalendar.get(Calendar.DAY_OF_MONTH)
        )
        val firstDayJdn = HijriCalendarEngine.hijriToJdn(ramadanHijriYear, 9, 1) - hijriDayOffset.coerceIn(-1, 1)

        return (1..30).map { dayNumber ->
            val gJdn = firstDayJdn + (dayNumber - 1)
            val gDate = HijriCalendarEngine.jdnToGregorian(gJdn)
            val hDate = HijriDate(ramadanHijriYear, 9, dayNumber)
            val dayCal = Calendar.getInstance(timeZone).apply {
                set(gDate.year, gDate.month - 1, gDate.day, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val tzHours = timeZone.getOffset(dayCal.timeInMillis) / 3_600_000.0
            val schedule = PrayerTimeCalculator.calculateForDate(
                calendar = dayCal,
                latitude = latitude,
                longitude = longitude,
                asrMethod = asrMethod,
                timeZoneHours = tzHours,
                calculationMethod = calculationMethod
            )
            RamadanDayEntry(
                ramadanDay = dayNumber,
                hijriDate = hDate,
                gregorianDate = gDate,
                sehriEndMinute = schedule.sehriEndMinute,
                fajrMinute = schedule.fajrMinute,
                maghribMinute = schedule.maghribMinute,
                iftarMinute = schedule.iftarMinute,
                isToday = gJdn == todayJdn
            )
        }
    }

    /**
     * Computes live countdown to the next Sehri end or Iftar from the current time.
     */
    fun computeSehriIftarCountdown(
        now: Calendar,
        todaySchedule: DailyPrayerSchedule,
        tomorrowSehriEndMinute: Int
    ): SehriIftarCountdown {
        val nowSeconds = now.get(Calendar.HOUR_OF_DAY) * 3600L +
            now.get(Calendar.MINUTE) * 60L +
            now.get(Calendar.SECOND)
        val sehriSec = todaySchedule.sehriEndMinute * 60L
        val iftarSec = todaySchedule.iftarMinute * 60L

        return when {
            nowSeconds < sehriSec -> SehriIftarCountdown(
                isNextIftar = false,
                targetMinuteOfDay = todaySchedule.sehriEndMinute,
                remainingSeconds = (sehriSec - nowSeconds).coerceAtLeast(0L)
            )
            nowSeconds < iftarSec -> SehriIftarCountdown(
                isNextIftar = true,
                targetMinuteOfDay = todaySchedule.iftarMinute,
                remainingSeconds = (iftarSec - nowSeconds).coerceAtLeast(0L)
            )
            else -> {
                val secsUntilMidnight = (24L * 3600L) - nowSeconds
                val rem = secsUntilMidnight + tomorrowSehriEndMinute * 60L
                SehriIftarCountdown(
                    isNextIftar = false,
                    targetMinuteOfDay = tomorrowSehriEndMinute,
                    remainingSeconds = rem.coerceAtLeast(0L)
                )
            }
        }
    }
}
