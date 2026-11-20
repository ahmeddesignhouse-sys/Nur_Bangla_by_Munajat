package com.example.util

import java.util.Calendar
import java.util.TimeZone

data class HijriDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..30
)

data class GregorianDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
)

data class IslamicEvent(
    val id: String,
    val hijriMonth: Int, // 1..12
    val hijriDay: Int,   // 1..30
    val titleBn: String,
    val titleEn: String,
    val descriptionBn: String,
    val descriptionEn: String,
    val isMajorEid: Boolean = false
) {
    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun description(isEnglish: Boolean): String = if (isEnglish) descriptionEn else descriptionBn
}

data class HijriMonthDayCell(
    val hijriDate: HijriDate,
    val gregorianDate: GregorianDate,
    val dayOfWeek: Int, // 0 = Sunday .. 6 = Saturday
    val isToday: Boolean,
    val events: List<IslamicEvent>
)

data class HijriEventOccurrence(
    val event: IslamicEvent,
    val hijriDate: HijriDate,
    val gregorianDate: GregorianDate
)

/**
 * Bidirectional Tabular/Kuwaiti Islamic Calendar engine with moon-sighting day adjustment (-1, 0, +1).
 */
object HijriCalendarEngine {

    val hijriMonthsBn = listOf(
        "মুহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জমাদিউল আউয়াল", "জমাদিউস সানি",
        "রজব", "শাবান", "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )

    val hijriMonthsEn = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    val shortGregorianMonthsBn = listOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন",
        "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )

    val shortGregorianMonthsEn = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val fullGregorianMonthsBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    val fullGregorianMonthsEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val shortWeekdaysBn = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
    val shortWeekdaysEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val ISLAMIC_EVENTS: List<IslamicEvent> = listOf(
        IslamicEvent(
            id = "islamic_new_year",
            hijriMonth = 1,
            hijriDay = 1,
            titleBn = "হিজরি নববর্ষ",
            titleEn = "Islamic New Year",
            descriptionBn = "মুহররম মাসের প্রথম দিন — ইসলামী হিজরি সনের সূচনা।",
            descriptionEn = "First day of Muharram marking the start of the new Islamic Hijri year."
        ),
        IslamicEvent(
            id = "ashura",
            hijriMonth = 1,
            hijriDay = 10,
            titleBn = "পবিত্র আশুরা",
            titleEn = "Day of Ashura",
            descriptionBn = "১০ই মুহররম — নফল রোজা ও ইবাদতের অত্যন্ত ফজিলতপূর্ণ দিন।",
            descriptionEn = "10th of Muharram — a blessed day of fasting and remembrance."
        ),
        IslamicEvent(
            id = "miladunnabi",
            hijriMonth = 3,
            hijriDay = 12,
            titleBn = "ঈদে মিলাদুন্নবী (ﷺ)",
            titleEn = "Prophet's Birthday (Eid-e-Miladunnabi)",
            descriptionBn = "১২ই রবিউল আউয়াল — মহানবী হযরত মুহাম্মদ (ﷺ)-এর জন্ম ও সীরাত স্মরণের দিন।",
            descriptionEn = "12th of Rabi al-Awwal — commemorating the birth and life of Prophet Muhammad (PBUH)."
        ),
        IslamicEvent(
            id = "shab_e_meraj",
            hijriMonth = 7,
            hijriDay = 27,
            titleBn = "শবে মেরাজ",
            titleEn = "Shab-e-Meraj (Isra & Mi'raj)",
            descriptionBn = "২৭শে রজব — মহানবী (ﷺ)-এর পবিত্র ইসরা ও মেরাজ এবং পাঁচ ওয়াক্ত নামাজ ফরজ হওয়ার স্মরণীয় রজনী।",
            descriptionEn = "27th of Rajab — Night Journey and Ascension of the Prophet (PBUH) when five daily prayers were prescribed."
        ),
        IslamicEvent(
            id = "shab_e_barat",
            hijriMonth = 8,
            hijriDay = 15,
            titleBn = "শবে বরাত",
            titleEn = "Shab-e-Barat (Mid-Sha'ban)",
            descriptionBn = "১৫ই শাবান — ক্ষমা প্রার্থনা, তওবা ও নফল ইবাদতের বরকতময় রজনী।",
            descriptionEn = "15th of Sha'ban — Night of Forgiveness, repentance, and prayer."
        ),
        IslamicEvent(
            id = "ramadan_start",
            hijriMonth = 9,
            hijriDay = 1,
            titleBn = "পবিত্র রমজান শুরু",
            titleEn = "Ramadan Begins",
            descriptionBn = "১লা রমজান — রহমত, মাগফিরাত ও নাজাতের পবিত্র সিয়াম সাধনার মাস শুরু।",
            descriptionEn = "1st of Ramadan — start of the sacred month of fasting, Quran, and mercy.",
            isMajorEid = true
        ),
        IslamicEvent(
            id = "shab_e_qadr",
            hijriMonth = 9,
            hijriDay = 27,
            titleBn = "সম্ভাব্য শবে কদর",
            titleEn = "Laylatul Qadr (27th Night)",
            descriptionBn = "২৭শে রমজান — হাজার মাসের চেয়েও উত্তম মহিমান্বিত রজনী।",
            descriptionEn = "27th of Ramadan — the Night of Decree, better than a thousand months."
        ),
        IslamicEvent(
            id = "eid_ul_fitr",
            hijriMonth = 10,
            hijriDay = 1,
            titleBn = "ঈদুল ফিতর",
            titleEn = "Eid-ul-Fitr",
            descriptionBn = "১লা শাওয়াল — রমজানের সিয়াম সাধনা শেষে মুসলিম উম্মাহর আনন্দ উৎসব।",
            descriptionEn = "1st of Shawwal — Festival of Breaking the Fast celebrating the completion of Ramadan.",
            isMajorEid = true
        ),
        IslamicEvent(
            id = "hajj_tarwiyah",
            hijriMonth = 12,
            hijriDay = 8,
            titleBn = "হজের প্রথম দিন (তারবিয়াহ)",
            titleEn = "Hajj Begins (Day of Tarwiyah)",
            descriptionBn = "৮ই জিলহজ — হাজীগণ মিনায় অবস্থানের মাধ্যমে পবিত্র হজের আনুষ্ঠানিকতা শুরু করেন।",
            descriptionEn = "8th of Dhu al-Hijjah — Pilgrims depart for Mina as the rituals of Hajj begin."
        ),
        IslamicEvent(
            id = "hajj_arafah",
            hijriMonth = 12,
            hijriDay = 9,
            titleBn = "পবিত্র আরাফাহ দিবস (হজ)",
            titleEn = "Day of Arafah (Hajj)",
            descriptionBn = "৯ই জিলহজ — হজের প্রধান রুকন আরাফাতের ময়দানে অবস্থান এবং গাইরে হাজীদের জন্য ফজিলতপূর্ণ রোজা।",
            descriptionEn = "9th of Dhu al-Hijjah — Standing at Arafah, the pinnacle of Hajj; fasting is highly recommended for non-pilgrims."
        ),
        IslamicEvent(
            id = "eid_ul_adha",
            hijriMonth = 12,
            hijriDay = 10,
            titleBn = "ঈদুল আযহা (কোরবানির ঈদ)",
            titleEn = "Eid-ul-Adha",
            descriptionBn = "১০ই জিলহজ — মহান ত্যাগ ও কোরবানির মহিমান্বিত ঈদ এবং হজের গুরুত্বপূর্ণ দিন।",
            descriptionEn = "10th of Dhu al-Hijjah — Festival of Sacrifice commemorating Prophet Ibrahim's devotion.",
            isMajorEid = true
        ),
        IslamicEvent(
            id = "hajj_tashriq_1",
            hijriMonth = 12,
            hijriDay = 11,
            titleBn = "আইয়ামে তাশরিক (হজের দিন)",
            titleEn = "Days of Tashriq (Hajj Day 4)",
            descriptionBn = "১১ই জিলহজ — মিনায় অবস্থান, কঙ্কর নিক্ষেপ ও তাকবীরে তাশরিকের দিন।",
            descriptionEn = "11th of Dhu al-Hijjah — Days of Tashriq in Mina and ongoing Hajj rituals."
        ),
        IslamicEvent(
            id = "hajj_tashriq_2",
            hijriMonth = 12,
            hijriDay = 12,
            titleBn = "আইয়ামে তাশরিক (হজের দিন)",
            titleEn = "Days of Tashriq (Hajj Day 5)",
            descriptionBn = "১২ই জিলহজ — কোরবানি ও হজের তাশরিক দিবস।",
            descriptionEn = "12th of Dhu al-Hijjah — Second day of Tashriq during Hajj."
        ),
        IslamicEvent(
            id = "hajj_tashriq_3",
            hijriMonth = 12,
            hijriDay = 13,
            titleBn = "আইয়ামে তাশরিক শেষ দিন (হজ)",
            titleEn = "Final Day of Tashriq (Hajj)",
            descriptionBn = "১৩ই জিলহজ — আইয়ামে তাশরিক ও হজের আনুষ্ঠানিক সমাপ্তি দিন।",
            descriptionEn = "13th of Dhu al-Hijjah — Final day of Tashriq concluding the days of Hajj."
        )
    )

    fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14L - month) / 12L
        val y = year + 4800L - a
        val m = month + 12L * a - 3L
        return day + (153L * m + 2L) / 5L + 365L * y + y / 4L - y / 100L + y / 400L - 32045L
    }

    fun jdnToGregorian(jdn: Long): GregorianDate {
        val a = jdn + 32044L
        val b = (4L * a + 3L) / 146097L
        val c = a - (146097L * b) / 4L
        val d = (4L * c + 3L) / 1461L
        val e = c - (1461L * d) / 4L
        val m = (5L * e + 2L) / 153L
        val day = (e - (153L * m + 2L) / 5L + 1L).toInt()
        val month = (m + 3L - 12L * (m / 10L)).toInt()
        val year = (100L * b + d - 4800L + m / 10L).toInt()
        return GregorianDate(year = year, month = month, day = day)
    }

    fun hijriToJdn(year: Int, month: Int, day: Int): Long {
        val y = year.toLong()
        val m = month.toLong()
        val d = day.toLong()
        return ((11L * y + 3L) / 30L) + 354L * y + 30L * m - ((m - 1L) / 2L) + d + 1948440L - 385L
    }

    fun jdnToHijri(jdn: Long): HijriDate {
        var l = jdn - 1948440L + 10632L
        val n = (l - 1L) / 10631L
        l = l - 10631L * n + 354L
        val j = ((10985L - l) / 5316L) * ((50L * l) / 17719L) + (l / 5670L) * ((43L * l) / 15238L)
        l = l - ((30L - j) / 15L) * ((17719L * j) / 50L) - (j / 16L) * ((15238L * j) / 43L) + 29L
        val month = ((24L * l) / 709L).toInt().coerceIn(1, 12)
        val day = (l - (709L * month) / 24L).toInt().coerceIn(1, 30)
        val year = (30L * n + j - 30L).toInt()
        return HijriDate(year = year, month = month, day = day)
    }

    /**
     * Converts a Gregorian Calendar date to HijriDate, applying the user's moon-sighting day offset (-1..+1).
     */
    fun fromGregorian(calendar: Calendar, dayOffset: Int = 0): HijriDate {
        val clampedOffset = dayOffset.coerceIn(-1, 1)
        val gJdn = gregorianToJdn(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )
        return jdnToHijri(gJdn + clampedOffset)
    }

    /**
     * Converts a Hijri date back to its corresponding Gregorian date under the active moon-sighting offset.
     */
    fun toGregorian(hijriDate: HijriDate, dayOffset: Int = 0): GregorianDate {
        val clampedOffset = dayOffset.coerceIn(-1, 1)
        val hJdn = hijriToJdn(hijriDate.year, hijriDate.month, hijriDate.day)
        return jdnToGregorian(hJdn - clampedOffset)
    }

    /**
     * Exact day-of-week for a JDN: 0 = Sunday, 1 = Monday, ..., 6 = Saturday.
     */
    fun dayOfWeekForJdn(jdn: Long): Int {
        // JDN 0 was Monday; (jdn + 1) % 7 yields 0 = Sunday .. 6 = Saturday
        return (((jdn + 1L) % 7L + 7L) % 7L).toInt()
    }

    fun daysInHijriMonth(year: Int, month: Int): Int {
        val start = hijriToJdn(year, month, 1)
        val (nextYear, nextMonth) = if (month == 12) (year + 1) to 1 else year to (month + 1)
        val end = hijriToJdn(nextYear, nextMonth, 1)
        return (end - start).toInt().coerceIn(29, 30)
    }

    fun shiftHijriMonth(year: Int, month: Int, deltaMonths: Int): Pair<Int, Int> {
        val zeroBased = year * 12 + (month - 1) + deltaMonths
        val newYear = Math.floorDiv(zeroBased, 12)
        val newMonth = Math.floorMod(zeroBased, 12) + 1
        return newYear to newMonth
    }

    fun eventsOn(hijriMonth: Int, hijriDay: Int): List<IslamicEvent> {
        return ISLAMIC_EVENTS.filter { it.hijriMonth == hijriMonth && it.hijriDay == hijriDay }
    }

    fun buildMonthCells(
        hijriYear: Int,
        hijriMonth: Int,
        dayOffset: Int = 0,
        todayCalendar: Calendar = Calendar.getInstance(TimeZone.getDefault())
    ): List<HijriMonthDayCell> {
        val clampedOffset = dayOffset.coerceIn(-1, 1)
        val todayJdn = gregorianToJdn(
            year = todayCalendar.get(Calendar.YEAR),
            month = todayCalendar.get(Calendar.MONTH) + 1,
            day = todayCalendar.get(Calendar.DAY_OF_MONTH)
        )
        val totalDays = daysInHijriMonth(hijriYear, hijriMonth)
        return (1..totalDays).map { day ->
            val hDate = HijriDate(hijriYear, hijriMonth, day)
            val gJdn = hijriToJdn(hijriYear, hijriMonth, day) - clampedOffset
            val gDate = jdnToGregorian(gJdn)
            HijriMonthDayCell(
                hijriDate = hDate,
                gregorianDate = gDate,
                dayOfWeek = dayOfWeekForJdn(gJdn),
                isToday = gJdn == todayJdn,
                events = eventsOn(hijriMonth, day)
            )
        }
    }

    fun eventsForHijriYear(hijriYear: Int, dayOffset: Int = 0): List<HijriEventOccurrence> {
        return ISLAMIC_EVENTS.map { event ->
            val hDate = HijriDate(hijriYear, event.hijriMonth, event.hijriDay)
            HijriEventOccurrence(
                event = event,
                hijriDate = hDate,
                gregorianDate = toGregorian(hDate, dayOffset)
            )
        }
    }

    fun monthName(hijriMonth: Int, isEnglish: Boolean): String {
        val idx = (hijriMonth - 1).coerceIn(0, 11)
        return if (isEnglish) hijriMonthsEn[idx] else hijriMonthsBn[idx]
    }

    fun formatHijriDate(date: HijriDate, isEnglish: Boolean): String {
        val monthStr = monthName(date.month, isEnglish)
        return if (isEnglish) {
            "${date.day} $monthStr ${date.year} AH"
        } else {
            "${BanglaUtils.toBanglaNumber(date.day)} $monthStr ${BanglaUtils.toBanglaNumber(date.year)} হিজরি"
        }
    }

    fun formatGregorianShort(date: GregorianDate, isEnglish: Boolean): String {
        val mIdx = (date.month - 1).coerceIn(0, 11)
        return if (isEnglish) {
            "${date.day} ${shortGregorianMonthsEn[mIdx]}"
        } else {
            "${BanglaUtils.toBanglaNumber(date.day)} ${shortGregorianMonthsBn[mIdx]}"
        }
    }

    fun formatGregorianFull(date: GregorianDate, isEnglish: Boolean): String {
        val mIdx = (date.month - 1).coerceIn(0, 11)
        return if (isEnglish) {
            "${date.day} ${fullGregorianMonthsEn[mIdx]} ${date.year}"
        } else {
            "${BanglaUtils.toBanglaNumber(date.day)} ${fullGregorianMonthsBn[mIdx]} ${BanglaUtils.toBanglaNumber(date.year)}"
        }
    }

    fun formatGregorianMonthSpan(cells: List<HijriMonthDayCell>, isEnglish: Boolean): String {
        if (cells.isEmpty()) return ""
        val first = cells.first().gregorianDate
        val last = cells.last().gregorianDate
        val firstM = if (isEnglish) shortGregorianMonthsEn[(first.month - 1).coerceIn(0, 11)] else shortGregorianMonthsBn[(first.month - 1).coerceIn(0, 11)]
        val lastM = if (isEnglish) shortGregorianMonthsEn[(last.month - 1).coerceIn(0, 11)] else shortGregorianMonthsBn[(last.month - 1).coerceIn(0, 11)]
        val firstY = BanglaUtils.formatNumber(first.year, isEnglish)
        val lastY = BanglaUtils.formatNumber(last.year, isEnglish)
        return when {
            first.year == last.year && first.month == last.month -> "$firstM $firstY"
            first.year == last.year -> "$firstM – $lastM $firstY"
            else -> "$firstM $firstY – $lastM $lastY"
        }
    }
}
