package com.example.util

import java.util.Calendar
import kotlin.math.floor

object BanglaUtils {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val banglaWeekdays = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    private val englishMonthsBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val banglaMonths = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )

    private val hijriMonthsBn = listOf(
        "মুহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জমাদিউল আউয়াল", "জমাদিউস সানি",
        "রজব", "শাবান", "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
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

    fun toBanglaNumber(value: Int): String = toBanglaDigits(value.toString())
    fun toBanglaNumber(value: Long): String = toBanglaDigits(value.toString())

    fun formatTime12hBangla(minutesFromMidnight: Int): String {
        val total = ((minutesFromMidnight % 1440) + 1440) % 1440
        val hour24 = total / 60
        val minute = total % 60
        val period = when {
            hour24 < 4 -> "রাত"
            hour24 < 12 -> "ভোর/সকাল".let { if (hour24 < 6) "ভোর" else "সকাল" }
            hour24 < 15 -> "দুপুর"
            hour24 < 18 -> "বিকাল"
            hour24 < 20 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format("%02d:%02d", hour12, minute)
        return "${toBanglaDigits(formatted)} $period"
    }

    fun formatCountdownBangla(remainingSeconds: Long): String {
        val safe = remainingSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        val raw = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        return toBanglaDigits(raw)
    }

    fun getEnglishDateBangla(cal: Calendar): String {
        val dayOfWeek = banglaWeekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val day = toBanglaNumber(cal.get(Calendar.DAY_OF_MONTH))
        val month = englishMonthsBn[cal.get(Calendar.MONTH)]
        val year = toBanglaNumber(cal.get(Calendar.YEAR))
        return "$dayOfWeek, $day $month $year"
    }

    /**
     * Revised Bangla Academy Bengali calendar (Pohela Boishakh = April 14).
     */
    fun getBanglaCalendarDate(cal: Calendar): String {
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1 // 1..12
        val gDay = cal.get(Calendar.DAY_OF_MONTH)

        val isLeap = (gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)
        val falgunDays = if (isLeap) 30 else 29

        val tz = cal.timeZone
        // Day of year relative to April 14 (anchored at 12:00 noon to avoid DST 23h/25h day drift)
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
        val prevIsLeap = ((startRef.get(Calendar.YEAR) + 1) % 4 == 0 && (startRef.get(Calendar.YEAR) + 1) % 100 != 0) || ((startRef.get(Calendar.YEAR) + 1) % 400 == 0)
        val effectiveFalgun = if (prevIsLeap) 30 else 29

        // Bangla Academy 2019 revision: Boishakh..Ashwin = 31 (6 months), Kartik..Magh = 30 (4 months), Falgun = 29/30, Chaitra = 30
        val monthLengths = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, effectiveFalgun, 30)
        var rem = diffDays
        var monthIdx = 0
        while (monthIdx < 11 && rem >= monthLengths[monthIdx]) {
            rem -= monthLengths[monthIdx]
            monthIdx++
        }
        val banglaDay = rem + 1
        return "${toBanglaNumber(banglaDay)} ${banglaMonths[monthIdx]} ${toBanglaNumber(banglaYear)} বঙ্গাব্দ"
    }

    /**
     * Kuwaiti / Tabular Islamic Calendar conversion to Hijri date in Bangla.
     */
    fun getHijriDateBangla(cal: Calendar): String {
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        var m = month
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        var a = floor(y / 100.0).toInt()
        var b = 2 - a + floor(a / 4.0).toInt()
        if (y < 1583) b = 0
        if (y == 1582) {
            if (m > 10) b = -10
            if (m == 10) {
                b = 0
                if (day > 4) b = -10
            }
        }
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524
        val epoch = 1948439.5
        val iYear = 10631.0 / 30.0
        val shift1 = 8.01 / 60.0
        var z = jd - epoch
        val cyc = floor(z / 10631.0)
        z -= 10631.0 * cyc
        val j = floor((z - shift1) / iYear)
        val hYear = (30 * cyc + j).toInt()
        z -= floor(j * iYear + shift1)
        var hMonth = floor((z + 28.5001) / 29.5).toInt()
        if (hMonth == 13) hMonth = 12
        val hDay = (z - floor(29.5001 * hMonth - 29)).toInt().coerceIn(1, 30)
        val monthName = hijriMonthsBn[(hMonth - 1).coerceIn(0, 11)]
        return "${toBanglaNumber(hDay)} $monthName ${toBanglaNumber(hYear)} হিজরি"
    }

    fun getDateKey(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", y, m, d)
    }

    fun formatDateKeyBangla(dateKey: String): String {
        val parts = dateKey.split("-")
        if (parts.size != 3) return toBanglaDigits(dateKey)
        val y = parts[0].toIntOrNull() ?: return toBanglaDigits(dateKey)
        val m = parts[1].toIntOrNull() ?: return toBanglaDigits(dateKey)
        val d = parts[2].toIntOrNull() ?: return toBanglaDigits(dateKey)
        val monthName = englishMonthsBn.getOrElse(m - 1) { "" }
        return "${toBanglaNumber(d)} $monthName ${toBanglaNumber(y)}"
    }
}
