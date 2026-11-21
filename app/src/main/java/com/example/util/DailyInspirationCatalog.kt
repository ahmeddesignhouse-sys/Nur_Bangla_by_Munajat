package com.example.util

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.io.InputStream
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class InspirationSlot(
    val index: Int,
    val labelBn: String,
    val labelEn: String
) {
    SLOT_1(
        index = 0,
        labelBn = "সকাল ৫টা – ৮টা",
        labelEn = "5:00 AM – 8:00 AM"
    ),
    SLOT_2(
        index = 1,
        labelBn = "সকাল ৮টা – ১১টা",
        labelEn = "8:00 AM – 11:00 AM"
    ),
    SLOT_3(
        index = 2,
        labelBn = "সকাল ১১টা – পরদিন ভোর ৫টা",
        labelEn = "11:00 AM – 5:00 AM"
    );

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

enum class SpecialIslamicDay(
    val tag: String,
    val labelBn: String,
    val labelEn: String
) {
    EID_UL_FITR("eid_ul_fitr", "পবিত্র ঈদুল ফিতর", "Eid ul-Fitr"),
    EID_UL_ADHA("eid_ul_adha", "পবিত্র ঈদুল আযহা", "Eid ul-Adha"),
    ARAFAH("arafah", "পবিত্র আরাফার দিন", "Day of Arafah"),
    LAYLATUL_QADR("laylatul_qadr", "রমজানের শেষ ১০ রাত • লাইলাতুল কদর", "Last 10 Nights • Laylatul Qadr"),
    RAMADAN("ramadan", "পবিত্র রমজান", "Blessed Ramadan"),
    SHAB_E_BARAT("shab_e_barat", "শবে বরাত (১৫ শাবান)", "Shab-e-Barat (15 Sha'ban)"),
    MUHARRAM_ASHURA("muharram_ashura", "মুহররম / পবিত্র আশুরা", "Muharram / Day of Ashura"),
    JUMMAH("jummah", "জুমার দিন", "Blessed Friday (Jummah)");

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

data class DailyHadithItem(
    val id: String,
    val arabic: String,
    val bangla: String,
    val english: String,
    val sourceBn: String,
    val sourceEn: String,
    val narratorBn: String,
    val narratorEn: String,
    val specialDay: String? = null
) {
    fun meaning(isEnglish: Boolean): String = if (isEnglish) english else bangla
    fun source(isEnglish: Boolean): String = if (isEnglish) sourceEn else sourceBn
    fun narrator(isEnglish: Boolean): String = if (isEnglish) narratorEn else narratorBn
}

data class DailyAyahItem(
    val id: String,
    val arabic: String,
    val bangla: String,
    val english: String,
    val sourceBn: String,
    val sourceEn: String,
    val specialDay: String? = null
) {
    fun meaning(isEnglish: Boolean): String = if (isEnglish) english else bangla
    fun source(isEnglish: Boolean): String = if (isEnglish) sourceEn else sourceBn
}

data class DailyInspirationEntry(
    val dateKey: String,
    val daysAgo: Int,
    val gregorianDateBn: String,
    val gregorianDateEn: String,
    val hijriDateBn: String,
    val hijriDateEn: String,
    val hadith: DailyHadithItem,
    val ayah: DailyAyahItem,
    val slot: InspirationSlot = InspirationSlot.SLOT_1,
    val specialDay: SpecialIslamicDay? = null
) {
    fun gregorianDate(isEnglish: Boolean): String = if (isEnglish) gregorianDateEn else gregorianDateBn
    fun hijriDate(isEnglish: Boolean): String = if (isEnglish) hijriDateEn else hijriDateBn
    fun slotLabel(isEnglish: Boolean): String = slot.label(isEnglish)
    fun specialDayLabel(isEnglish: Boolean): String? = specialDay?.label(isEnglish)

    fun formatShareText(isEnglish: Boolean): String = buildString {
        if (isEnglish) {
            appendLine("Nur Bangla — Daily Hadith & Ayah ($gregorianDateEn | $hijriDateEn)")
            specialDay?.let { appendLine("Special Day: ${it.labelEn}") }
            appendLine()
            appendLine("Hadith (${hadith.sourceEn}):")
            appendLine(hadith.arabic)
            appendLine("English: ${hadith.english}")
            appendLine("Bangla: ${hadith.bangla}")
            appendLine()
            appendLine("Ayah (${ayah.sourceEn}):")
            appendLine(ayah.arabic)
            appendLine("English: ${ayah.english}")
            appendLine("Bangla: ${ayah.bangla}")
            appendLine()
            append(AppLinks.shareCaptionFooter(isEnglish = true))
        } else {
            appendLine("নূর বাংলা — দৈনিক হাদিস ও আয়াত ($gregorianDateBn | $hijriDateBn)")
            specialDay?.let { appendLine("বিশেষ দিন: ${it.labelBn}") }
            appendLine()
            appendLine("হাদিস (${hadith.sourceBn}):")
            appendLine(hadith.arabic)
            appendLine("বাংলা: ${hadith.bangla}")
            appendLine("English: ${hadith.english}")
            appendLine()
            appendLine("আয়াত (${ayah.sourceBn}):")
            appendLine(ayah.arabic)
            appendLine("বাংলা: ${ayah.bangla}")
            appendLine("English: ${ayah.english}")
            appendLine()
            append(AppLinks.shareCaptionFooter(isEnglish = false))
        }
    }.trim()
}

/**
 * Verified 1,095-Hadith & 1,095-Ayah catalog loaded once from bundled asset JSON files
 * (`hadiths_catalog.json`, `ayahs_catalog.json`) and cached in memory.
 *
 * 3-slot daily rotation in Asia/Dhaka time (zero repeats across 365 days = 1,095 slots):
 * - Slot 1 (index 0): 05:00 – 08:00
 * - Slot 2 (index 1): 08:00 – 11:00
 * - Slot 3 (index 2): 11:00 – 05:00 next day
 *
 * Mapping: index = (dayOfYear - 1) * 3 + slot, where dayOfYear is 1..365 and slot is 0, 1, 2.
 * Leap-year note: For day 366 of a leap year, indices 0..2 are reused for that single extra day.
 */
object DailyInspirationCatalog {

    const val HADITHS_ASSET_FILE = "hadiths_catalog.json"
    const val AYAHS_ASSET_FILE = "ayahs_catalog.json"
    val DHAKA_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    @Volatile
    private var cachedHadiths: List<DailyHadithItem>? = null

    @Volatile
    private var cachedAyahs: List<DailyAyahItem>? = null

    @Volatile
    private var specialHadithByTag: Map<String, DailyHadithItem> = emptyMap()

    @Volatile
    private var specialAyahByTag: Map<String, DailyAyahItem> = emptyMap()

    val HADITHS: List<DailyHadithItem>
        get() = cachedHadiths ?: loadFromJvmFallbackOrEmptyHadiths()

    val AYAHS: List<DailyAyahItem>
        get() = cachedAyahs ?: loadFromJvmFallbackOrEmptyAyahs()

    fun initialize(context: Context) {
        if (cachedHadiths != null && cachedAyahs != null) return
        synchronized(this) {
            if (cachedHadiths == null) {
                val parsed = context.assets.open(HADITHS_ASSET_FILE).use { parseHadithsJson(it) }
                setHadithsInternal(parsed)
            }
            if (cachedAyahs == null) {
                val parsed = context.assets.open(AYAHS_ASSET_FILE).use { parseAyahsJson(it) }
                setAyahsInternal(parsed)
            }
        }
    }

    private fun setHadithsInternal(list: List<DailyHadithItem>) {
        cachedHadiths = list
        specialHadithByTag = list.mapNotNull { item ->
            item.specialDay?.takeIf { it.isNotBlank() }?.let { it to item }
        }.toMap()
    }

    private fun setAyahsInternal(list: List<DailyAyahItem>) {
        cachedAyahs = list
        specialAyahByTag = list.mapNotNull { item ->
            item.specialDay?.takeIf { it.isNotBlank() }?.let { it to item }
        }.toMap()
    }

    private fun loadFromJvmFallbackOrEmptyHadiths(): List<DailyHadithItem> {
        synchronized(this) {
            cachedHadiths?.let { return it }
            val candidates = listOf(
                File("app/src/main/assets/$HADITHS_ASSET_FILE"),
                File("src/main/assets/$HADITHS_ASSET_FILE")
            )
            val file = candidates.firstOrNull { it.exists() }
            if (file != null) {
                val parsed = file.inputStream().use { parseHadithsJson(it) }
                setHadithsInternal(parsed)
                return parsed
            }
            return emptyList()
        }
    }

    private fun loadFromJvmFallbackOrEmptyAyahs(): List<DailyAyahItem> {
        synchronized(this) {
            cachedAyahs?.let { return it }
            val candidates = listOf(
                File("app/src/main/assets/$AYAHS_ASSET_FILE"),
                File("src/main/assets/$AYAHS_ASSET_FILE")
            )
            val file = candidates.firstOrNull { it.exists() }
            if (file != null) {
                val parsed = file.inputStream().use { parseAyahsJson(it) }
                setAyahsInternal(parsed)
                return parsed
            }
            return emptyList()
        }
    }

    internal fun parseHadithsJson(input: InputStream): List<DailyHadithItem> {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val result = ArrayList<DailyHadithItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val special = obj.optString("specialDay", "").takeIf { it.isNotBlank() }
            result.add(
                DailyHadithItem(
                    id = obj.getString("id"),
                    arabic = obj.getString("arabic"),
                    bangla = obj.getString("bangla"),
                    english = obj.getString("english"),
                    sourceBn = obj.getString("sourceBn"),
                    sourceEn = obj.getString("sourceEn"),
                    narratorBn = obj.getString("narratorBn"),
                    narratorEn = obj.getString("narratorEn"),
                    specialDay = special
                )
            )
        }
        return result
    }

    internal fun parseAyahsJson(input: InputStream): List<DailyAyahItem> {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val result = ArrayList<DailyAyahItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val special = obj.optString("specialDay", "").takeIf { it.isNotBlank() }
            result.add(
                DailyAyahItem(
                    id = obj.getString("id"),
                    arabic = obj.getString("arabic"),
                    bangla = obj.getString("bangla"),
                    english = obj.getString("english"),
                    sourceBn = obj.getString("sourceBn"),
                    sourceEn = obj.getString("sourceEn"),
                    specialDay = special
                )
            )
        }
        return result
    }

    /**
     * Converts any [Calendar] instant to an Asia/Dhaka calendar for consistent slot boundaries.
     */
    fun toDhakaCalendar(source: Calendar): Calendar {
        return Calendar.getInstance(DHAKA_TIMEZONE).apply {
            timeInMillis = source.timeInMillis
        }
    }

    /**
     * Resolves the current rotation slot in Asia/Dhaka time:
     * - 05:00 – 07:59 -> SLOT_1 (0)
     * - 08:00 – 10:59 -> SLOT_2 (1)
     * - 11:00 – 23:59 -> SLOT_3 (2) of today
     * - 00:00 – 04:59 -> SLOT_3 (2) of the previous day
     */
    fun slotFor(calendar: Calendar): InspirationSlot {
        val dhaka = toDhakaCalendar(calendar)
        val hour = dhaka.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..7 -> InspirationSlot.SLOT_1
            hour in 8..10 -> InspirationSlot.SLOT_2
            else -> InspirationSlot.SLOT_3
        }
    }

    /**
     * Logical rotation day calendar in Asia/Dhaka time:
     * between 00:00 and 04:59, the active slot belongs to the previous day's Slot 3 (11:00–05:00).
     */
    fun logicalDayCalendar(calendar: Calendar): Calendar {
        val dhaka = toDhakaCalendar(calendar)
        if (dhaka.get(Calendar.HOUR_OF_DAY) < 5) {
            dhaka.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dhaka
    }

    /**
     * Deterministic index into the 1,095-entry catalog:
     * index = (dayOfYear - 1) * 3 + slot, where dayOfYear is 1..365 and slot is 0, 1, 2.
     * For leap years (day 366), day 366 reuses indices 0..2 for that extra day.
     */
    fun catalogIndexFor(dayOfYear: Int, slot: InspirationSlot): Int {
        val normalizedDay = if (dayOfYear >= 366) 1 else dayOfYear.coerceIn(1, 365)
        return (normalizedDay - 1) * 3 + slot.index
    }

    fun slotKeyFor(calendar: Calendar): String {
        val logical = logicalDayCalendar(calendar)
        val slot = slotFor(calendar)
        return "${dateKeyFor(logical)}_s${slot.index}"
    }

    fun detectSpecialDay(
        logicalDayCal: Calendar,
        hijriDayOffset: Int = 0
    ): SpecialIslamicDay? {
        val hijri = HijriCalendarEngine.fromGregorian(logicalDayCal, hijriDayOffset)
        return when {
            hijri.month == 10 && hijri.day == 1 -> SpecialIslamicDay.EID_UL_FITR
            hijri.month == 12 && hijri.day == 10 -> SpecialIslamicDay.EID_UL_ADHA
            hijri.month == 12 && hijri.day == 9 -> SpecialIslamicDay.ARAFAH
            hijri.month == 9 && hijri.day >= 21 -> SpecialIslamicDay.LAYLATUL_QADR
            hijri.month == 9 -> SpecialIslamicDay.RAMADAN
            hijri.month == 8 && hijri.day == 15 -> SpecialIslamicDay.SHAB_E_BARAT
            hijri.month == 1 && (hijri.day == 1 || hijri.day == 10) -> SpecialIslamicDay.MUHARRAM_ASHURA
            logicalDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY -> SpecialIslamicDay.JUMMAH
            else -> null
        }
    }

    /**
     * Minutes remaining until the next Asia/Dhaka slot boundary (05:00, 08:00, or 11:00).
     */
    fun minutesUntilNextSlot(calendar: Calendar): Int {
        val dhaka = toDhakaCalendar(calendar)
        val hour = dhaka.get(Calendar.HOUR_OF_DAY)
        val minute = dhaka.get(Calendar.MINUTE)
        val nowMinutes = hour * 60 + minute
        val nextBoundaryMinutes = when {
            nowMinutes < 5 * 60 -> 5 * 60
            nowMinutes < 8 * 60 -> 8 * 60
            nowMinutes < 11 * 60 -> 11 * 60
            else -> 24 * 60 + 5 * 60 // 05:00 next morning
        }
        return (nextBoundaryMinutes - nowMinutes).coerceAtLeast(1)
    }

    fun formatNextSlotCountdown(calendar: Calendar, isEnglish: Boolean): String {
        val totalMinutes = minutesUntilNextSlot(calendar)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (isEnglish) {
            when {
                hours > 0 && mins > 0 -> "Next Hadith: ${hours}h ${mins}m"
                hours > 0 -> "Next Hadith: ${hours}h"
                else -> "Next Hadith: ${mins}m"
            }
        } else {
            when {
                hours > 0 && mins > 0 ->
                    "পরের হাদিস: ${BanglaUtils.toBanglaNumber(hours)} ঘণ্টা ${BanglaUtils.toBanglaNumber(mins)} মিনিট"
                hours > 0 ->
                    "পরের হাদিস: ${BanglaUtils.toBanglaNumber(hours)} ঘণ্টা"
                else ->
                    "পরের হাদিস: ${BanglaUtils.toBanglaNumber(mins)} মিনিট"
            }
        }
    }

    fun dateKeyFor(calendar: Calendar): String {
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun forDate(
        calendar: Calendar,
        hijriDayOffset: Int = 0,
        daysAgo: Int = 0,
        forcedSlot: InspirationSlot? = null
    ): DailyInspirationEntry {
        val hadiths = HADITHS
        val ayahs = AYAHS
        check(hadiths.isNotEmpty() && ayahs.isNotEmpty()) {
            "DailyInspirationCatalog not initialized — call DailyInspirationCatalog.initialize(context) first"
        }

        val slot = forcedSlot ?: slotFor(calendar)
        val logicalCal = if (forcedSlot != null) toDhakaCalendar(calendar) else logicalDayCalendar(calendar)
        val dayOfYear = logicalCal.get(Calendar.DAY_OF_YEAR)
        val baseIndex = catalogIndexFor(dayOfYear, slot)

        // Day-relevant override applies ONLY on Slot 1
        val specialDay = if (slot == InspirationSlot.SLOT_1) {
            detectSpecialDay(logicalCal, hijriDayOffset)
        } else {
            null
        }

        val hadith = specialDay?.let { specialHadithByTag[it.tag] }
            ?: hadiths[Math.floorMod(baseIndex, hadiths.size)]
        val ayah = specialDay?.let { specialAyahByTag[it.tag] }
            ?: ayahs[Math.floorMod(baseIndex, ayahs.size)]

        return DailyInspirationEntry(
            dateKey = dateKeyFor(logicalCal),
            daysAgo = daysAgo,
            gregorianDateBn = BanglaUtils.getGregorianDate(logicalCal, isEnglish = false),
            gregorianDateEn = BanglaUtils.getGregorianDate(logicalCal, isEnglish = true),
            hijriDateBn = BanglaUtils.getHijriDate(logicalCal, false, hijriDayOffset),
            hijriDateEn = BanglaUtils.getHijriDate(logicalCal, true, hijriDayOffset),
            hadith = hadith,
            ayah = ayah,
            slot = slot,
            specialDay = specialDay
        )
    }

    /**
     * Index 0 is the current live slot entry; indices 1..7 are the previous 7 days (each showing Slot 1).
     */
    fun todayAndPreviousSevenDays(
        todayCalendar: Calendar,
        hijriDayOffset: Int = 0
    ): List<DailyInspirationEntry> {
        val todayEntry = forDate(todayCalendar, hijriDayOffset = hijriDayOffset, daysAgo = 0)
        val logicalToday = logicalDayCalendar(todayCalendar)
        val pastEntries = (1..7).map { offset ->
            val cal = (logicalToday.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
            }
            forDate(
                calendar = cal,
                hijriDayOffset = hijriDayOffset,
                daysAgo = offset,
                forcedSlot = InspirationSlot.SLOT_1
            )
        }
        return listOf(todayEntry) + pastEntries
    }
}
