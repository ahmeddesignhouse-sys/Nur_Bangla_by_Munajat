package com.example.util

import java.util.Calendar
import java.util.Locale

object BanglaUtils {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val banglaWeekdays = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )
    private val englishWeekdays = listOf(
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    private val englishMonthsBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    private val englishMonthsEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val banglaMonthsBn = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )
    private val banglaMonthsEn = listOf(
        "Boishakh", "Joishtho", "Asharh", "Srabon", "Bhadro", "Ashwin",
        "Kartik", "Agrahayan", "Poush", "Magh", "Falgun", "Chaitra"
    )

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatDigits(input: String, isEnglish: Boolean): String =
        if (isEnglish) input else toBanglaDigits(input)

    fun toBanglaNumber(value: Int): String = toBanglaDigits(value.toString())
    fun toBanglaNumber(value: Long): String = toBanglaDigits(value.toString())

    fun formatNumber(value: Int, isEnglish: Boolean): String =
        if (isEnglish) value.toString() else toBanglaNumber(value)

    fun formatNumber(value: Long, isEnglish: Boolean): String =
        if (isEnglish) value.toString() else toBanglaNumber(value)

    fun formatTime12hBangla(minutesFromMidnight: Int): String =
        formatTime12h(minutesFromMidnight, isEnglish = false)

    fun formatTime12h(minutesFromMidnight: Int, isEnglish: Boolean): String {
        val total = ((minutesFromMidnight % 1440) + 1440) % 1440
        val hour24 = total / 60
        val minute = total % 60
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format(Locale.US, "%02d:%02d", hour12, minute)
        if (isEnglish) {
            val amPm = if (hour24 < 12) "AM" else "PM"
            return "$formatted $amPm"
        }
        val period = when {
            hour24 < 4 -> "রাত"
            hour24 < 6 -> "ভোর"
            hour24 < 12 -> "সকাল"
            hour24 < 15 -> "দুপুর"
            hour24 < 18 -> "বিকাল"
            hour24 < 20 -> "সন্ধ্যা"
            else -> "রাত"
        }
        return "${toBanglaDigits(formatted)} $period"
    }

    fun formatCountdownBangla(remainingSeconds: Long): String =
        formatCountdown(remainingSeconds, isEnglish = false)

    fun formatCountdown(remainingSeconds: Long, isEnglish: Boolean): String {
        val safe = remainingSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        val raw = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        return formatDigits(raw, isEnglish)
    }

    fun getEnglishDateBangla(cal: Calendar): String = getGregorianDate(cal, isEnglish = false)

    fun getGregorianDate(cal: Calendar, isEnglish: Boolean): String {
        val dowIndex = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
        val monthIndex = cal.get(Calendar.MONTH).coerceIn(0, 11)
        val day = formatNumber(cal.get(Calendar.DAY_OF_MONTH), isEnglish)
        val year = formatNumber(cal.get(Calendar.YEAR), isEnglish)
        return if (isEnglish) {
            "${englishWeekdays[dowIndex]}, $day ${englishMonthsEn[monthIndex]} $year"
        } else {
            "${banglaWeekdays[dowIndex]}, $day ${englishMonthsBn[monthIndex]} $year"
        }
    }

    /**
     * Revised Bangla Academy Bengali calendar (Pohela Boishakh = April 14).
     */
    fun getBanglaCalendarDate(cal: Calendar, isEnglish: Boolean = false): String {
        val gYear = cal.get(Calendar.YEAR)
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        val tz = cal.timeZone

        val pohelaBoishakh = Calendar.getInstance(tz).apply {
            set(gYear, Calendar.APRIL, 14, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val current = Calendar.getInstance(tz).apply {
            set(gYear, cal.get(Calendar.MONTH), gDay, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val banglaYear: Int
        val startRef: Calendar
        if (current.before(pohelaBoishakh)) {
            banglaYear = gYear - 594
            startRef = Calendar.getInstance(tz).apply {
                set(gYear - 1, Calendar.APRIL, 14, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else {
            banglaYear = gYear - 593
            startRef = pohelaBoishakh
        }

        val diffDays = ((current.timeInMillis - startRef.timeInMillis + 43_200_000L) / 86_400_000L).toInt()
        val nextGregYear = startRef.get(Calendar.YEAR) + 1
        val prevIsLeap = (nextGregYear % 4 == 0 && nextGregYear % 100 != 0) || (nextGregYear % 400 == 0)
        val effectiveFalgun = if (prevIsLeap) 30 else 29

        val monthLengths = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, effectiveFalgun, 30)
        var rem = diffDays
        var monthIdx = 0
        while (monthIdx < 11 && rem >= monthLengths[monthIdx]) {
            rem -= monthLengths[monthIdx]
            monthIdx++
        }
        val banglaDay = rem + 1
        return if (isEnglish) {
            "$banglaDay ${banglaMonthsEn[monthIdx]} $banglaYear Bangabda"
        } else {
            "${toBanglaNumber(banglaDay)} ${banglaMonthsBn[monthIdx]} ${toBanglaNumber(banglaYear)} বঙ্গাব্দ"
        }
    }

    /**
     * Tabular Islamic Calendar conversion to Hijri date with optional moon-sighting day offset (-1, 0, +1).
     */
    fun getHijriDateBangla(cal: Calendar, dayOffset: Int = 0): String =
        getHijriDate(cal, isEnglish = false, dayOffset = dayOffset)

    fun getHijriDate(cal: Calendar, isEnglish: Boolean, dayOffset: Int = 0): String {
        val hijri = HijriCalendarEngine.fromGregorian(cal, dayOffset)
        return HijriCalendarEngine.formatHijriDate(hijri, isEnglish)
    }
}
