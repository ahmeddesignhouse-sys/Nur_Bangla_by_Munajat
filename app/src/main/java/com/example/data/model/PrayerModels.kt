package com.example.data.model

enum class WaqtType(
    val key: String,
    val titleBn: String,
    val rakatInfoBn: String
) {
    FAJR("fajr", "ফজর", "২ সুন্নত, ২ ফরয"),
    DHUHR("dhuhr", "যোহর", "৪ সুন্নত, ৪ ফরয, ২ সুন্নত, ২ নফল"),
    ASR("asr", "আসর", "৪ সুন্নত (গাইরে মুয়াক্কাদাহ), ৪ ফরয"),
    MAGHRIB("maghrib", "মাগরিব", "৩ ফরয, ২ সুন্নত, ২ নফল"),
    ISHA("isha", "এশা", "৪ ফরয, ২ সুন্নত, ২ নফল, ৩ বিতর");

    companion object {
        fun fromKey(key: String): WaqtType = entries.firstOrNull { it.key == key } ?: FAJR
    }
}

enum class PrayerStatus(val titleBn: String) {
    NONE("অচিহ্নিত"),
    ON_TIME("সময়মতো"),
    LATE("বিলম্বে"),
    QAZA("কাজা");

    companion object {
        fun safeValueOf(raw: String): PrayerStatus =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class AsrMethod(val titleBn: String, val shadowFactor: Int) {
    HANAFI("হানাফি (বাংলাদেশ প্রচলিত)", 2),
    SHAFI("শাফেয়ী / জমহুর", 1);

    companion object {
        fun safeValueOf(raw: String): AsrMethod =
            entries.firstOrNull { it.name == raw } ?: HANAFI
    }
}

enum class CalculationMethod(
    val titleBn: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null
) {
    AUTO("অটো (দেশ ও জিপিএস অনুযায়ী)", 18.0, 18.0),
    BANGLADESH_KARACHI("ইসলামিক ফাউন্ডেশন / করাচি", 18.0, 18.0),
    MWL("মুসলিম ওয়ার্ল্ড লীগ (MWL)", 18.0, 17.0),
    ISNA("উত্তর আমেরিকা (ISNA)", 15.0, 15.0),
    UMM_AL_QURA("উম্মুল কুরা (মক্কা)", 18.5, 0.0, ishaIntervalMinutes = 90),
    EGYPT("মিশরীয় জেনারেল অথরিটি", 19.5, 17.5);

    companion object {
        fun safeValueOf(raw: String): CalculationMethod =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class AdhanAlertMode(val titleBn: String, val descriptionBn: String) {
    FULL_ADHAN("পূর্ণ আযান", "নির্ধারিত সময়ে আযানের সুর বাজবে"),
    BEEP("সংক্ষিপ্ত বিপ", "হালকা নোটিফিকেশন শব্দ বাজবে"),
    VIBRATE("ভাইব্রেশন", "শব্দ ছাড়া শুধুমাত্র ভাইব্রেট হবে"),
    SILENT("নীরব", "কোনো শব্দ বা ভাইব্রেশন হবে না");

    companion object {
        fun safeValueOf(raw: String): AdhanAlertMode =
            entries.firstOrNull { it.name == raw } ?: BEEP
    }
}

enum class ThemeMode(val titleBn: String) {
    SYSTEM("সিস্টেম ডিফল্ট"),
    LIGHT("লাইট মোড"),
    DARK("ডার্ক মোড");

    companion object {
        fun safeValueOf(raw: String): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class AccentTheme(
    val titleBn: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
) {
    ISLAMIC_GREEN("ইসলামিক সবুজ", 0xFF0B6E4FL, 0xFF9A6B16L),
    ROYAL_BLUE("রয়্যাল নীল", 0xFF1B4965L, 0xFFB07D20L),
    GOLD("মক্কা সোনালি", 0xFF8C5E10L, 0xFF1F6B52L),
    MEDINA_TEAL("মদীনা টিল", 0xFF0E6B74L, 0xFFB56E22L),
    DESERT_ROSE("মরুর গোলাপ", 0xFF8E2B44L, 0xFF8C6A23L),
    NIGHT_INDIGO("তাহাজ্জুদ বেগুনি", 0xFF543C88L, 0xFFB8862DL);

    companion object {
        fun safeValueOf(raw: String): AccentTheme =
            entries.firstOrNull { it.name == raw } ?: ISLAMIC_GREEN
    }
}

data class ForbiddenTimeWindow(
    val labelBn: String,
    val startMinute: Int,
    val endMinute: Int
)

data class DailyPrayerSchedule(
    val fajrMinute: Int,
    val sunriseMinute: Int,
    val dhuhrMinute: Int,
    val asrMinute: Int,
    val sunsetMinute: Int,
    val maghribMinute: Int,
    val ishaMinute: Int,
    val sehriEndMinute: Int,
    val iftarMinute: Int,
    val forbiddenTimes: List<ForbiddenTimeWindow>,
    val timeZoneLabel: String = "GMT+6",
    val appliedMethodTitleBn: String = CalculationMethod.BANGLADESH_KARACHI.titleBn,
    val ishaEndMinute: Int = fajrMinute
) {
    fun minuteFor(waqt: WaqtType): Int = when (waqt) {
        WaqtType.FAJR -> fajrMinute
        WaqtType.DHUHR -> dhuhrMinute
        WaqtType.ASR -> asrMinute
        WaqtType.MAGHRIB -> maghribMinute
        WaqtType.ISHA -> ishaMinute
    }

    fun endMinuteFor(waqt: WaqtType): Int = when (waqt) {
        WaqtType.FAJR -> sunriseMinute
        WaqtType.DHUHR -> asrMinute
        WaqtType.ASR -> sunsetMinute
        WaqtType.MAGHRIB -> ishaMinute
        WaqtType.ISHA -> ishaEndMinute
    }
}

data class NextWaqtInfo(
    val waqt: WaqtType,
    val targetMinuteOfDay: Int,
    val remainingSeconds: Long,
    val isTomorrow: Boolean
)

data class WaqtAlertConfig(
    val fajr: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val dhuhr: AdhanAlertMode = AdhanAlertMode.BEEP,
    val asr: AdhanAlertMode = AdhanAlertMode.BEEP,
    val maghrib: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val isha: AdhanAlertMode = AdhanAlertMode.BEEP,
    val prePrayerMinutes: Int = 15
) {
    fun modeFor(waqt: WaqtType): AdhanAlertMode = when (waqt) {
        WaqtType.FAJR -> fajr
        WaqtType.DHUHR -> dhuhr
        WaqtType.ASR -> asr
        WaqtType.MAGHRIB -> maghrib
        WaqtType.ISHA -> isha
    }
}
