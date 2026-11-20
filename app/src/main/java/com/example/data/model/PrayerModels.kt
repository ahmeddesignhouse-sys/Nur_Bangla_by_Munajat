package com.example.data.model

enum class WaqtType(
    val key: String,
    val titleBn: String,
    val titleEn: String,
    val rakatInfoBn: String,
    val rakatInfoEn: String
) {
    FAJR("fajr", "ফজর", "Fajr", "২ সুন্নত, ২ ফরয", "2 Sunnah, 2 Fard"),
    DHUHR("dhuhr", "যোহর", "Dhuhr", "৪ সুন্নত, ৪ ফরয, ২ সুন্নত, ২ নফল", "4 Sunnah, 4 Fard, 2 Sunnah, 2 Nafl"),
    ASR("asr", "আসর", "Asr", "৪ সুন্নত (গাইরে মুয়াক্কাদাহ), ৪ ফরয", "4 Sunnah (Ghair Mu'akkadah), 4 Fard"),
    MAGHRIB("maghrib", "মাগরিব", "Maghrib", "৩ ফরয, ২ সুন্নত, ২ নফল", "3 Fard, 2 Sunnah, 2 Nafl"),
    ISHA("isha", "এশা", "Isha", "৪ ফরয, ২ সুন্নত, ২ নফল, ৩ বিতর", "4 Fard, 2 Sunnah, 2 Nafl, 3 Witr");

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun rakatInfo(isEnglish: Boolean): String = if (isEnglish) rakatInfoEn else rakatInfoBn

    companion object {
        fun fromKey(key: String): WaqtType = entries.firstOrNull { it.key == key } ?: FAJR
    }
}

enum class AsrMethod(val titleBn: String, val titleEn: String, val shadowFactor: Int) {
    HANAFI("হানাফি (বাংলাদেশ প্রচলিত)", "Hanafi (Standard in BD)", 2),
    SHAFI("শাফেয়ী / জমহুর", "Shafi'i / Standard", 1);

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun shortTitle(isEnglish: Boolean): String = when (this) {
        HANAFI -> if (isEnglish) "Hanafi" else "হানাফি"
        SHAFI -> if (isEnglish) "Shafi'i" else "শাফেয়ী"
    }

    companion object {
        fun safeValueOf(raw: String): AsrMethod =
            entries.firstOrNull { it.name == raw } ?: HANAFI
    }
}

enum class CalculationMethod(
    val titleBn: String,
    val titleEn: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int? = null
) {
    AUTO("অটো (দেশ ও জিপিএস অনুযায়ী)", "Auto (By Country & GPS)", 18.0, 18.0),
    BANGLADESH_KARACHI("ইসলামিক ফাউন্ডেশন / করাচি", "Islamic Foundation / Karachi", 18.0, 18.0),
    MWL("মুসলিম ওয়ার্ল্ড লীগ (MWL)", "Muslim World League (MWL)", 18.0, 17.0),
    ISNA("উত্তর আমেরিকা (ISNA)", "ISNA (North America)", 15.0, 15.0),
    UMM_AL_QURA("উম্মুল কুরা (মক্কা)", "Umm al-Qura (Makkah)", 18.5, 0.0, ishaIntervalMinutes = 90),
    EGYPT("মিশরীয় জেনারেল অথরিটি", "Egyptian General Authority", 19.5, 17.5);

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn

    companion object {
        fun safeValueOf(raw: String): CalculationMethod =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class AdhanAlertMode(
    val titleBn: String,
    val titleEn: String,
    val descriptionBn: String,
    val descriptionEn: String
) {
    FULL_ADHAN("পূর্ণ আযান", "Full Adhan", "নির্ধারিত সময়ে আযানের সুর বাজবে", "Plays full Adhan sound at prayer time"),
    BEEP("সংক্ষিপ্ত বিপ", "Short Beep", "হালকা নোটিফিকেশন শব্দ বাজবে", "Plays a brief notification tone"),
    VIBRATE("ভাইব্রেশন", "Vibrate", "শব্দ ছাড়া শুধুমাত্র ভাইব্রেট হবে", "Vibrates silently without audio"),
    SILENT("নীরব", "Silent", "কোনো শব্দ বা ভাইব্রেশন হবে না", "No sound or vibration");

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun description(isEnglish: Boolean): String = if (isEnglish) descriptionEn else descriptionBn
    fun shortTitle(isEnglish: Boolean): String = when (this) {
        FULL_ADHAN -> if (isEnglish) "Adhan" else "আযান"
        BEEP -> if (isEnglish) "Beep" else "বিপ"
        VIBRATE -> if (isEnglish) "Vibrate" else "ভাইব্রেট"
        SILENT -> if (isEnglish) "Silent" else "নীরব"
    }

    companion object {
        fun safeValueOf(raw: String): AdhanAlertMode =
            entries.firstOrNull { it.name == raw } ?: BEEP
    }
}

enum class ThemeMode(val titleBn: String, val titleEn: String) {
    SYSTEM("সিস্টেম ডিফল্ট", "System"),
    LIGHT("লাইট মোড", "Light"),
    DARK("ডার্ক মোড", "Dark");

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn

    companion object {
        fun safeValueOf(raw: String): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}

enum class AccentTheme(
    val titleBn: String,
    val titleEn: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
) {
    ISLAMIC_GREEN("ইসলামিক সবুজ", "Islamic Green", 0xFF0B6E4FL, 0xFF9A6B16L),
    ROYAL_BLUE("রয়্যাল নীল", "Royal Blue", 0xFF1B4965L, 0xFFB07D20L),
    GOLD("মক্কা সোনালি", "Makkah Gold", 0xFF8C5E10L, 0xFF1F6B52L),
    MEDINA_TEAL("মদীনা টিল", "Madinah Teal", 0xFF0E6B74L, 0xFFB56E22L),
    DESERT_ROSE("মরুর গোলাপ", "Desert Rose", 0xFF8E2B44L, 0xFF8C6A23L),
    NIGHT_INDIGO("তাহাজ্জুদ বেগুনি", "Tahajjud Indigo", 0xFF543C88L, 0xFFB8862DL);

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn

    companion object {
        fun safeValueOf(raw: String): AccentTheme =
            entries.firstOrNull { it.name == raw } ?: ISLAMIC_GREEN
    }
}

data class ForbiddenTimeWindow(
    val labelBn: String,
    val labelEn: String,
    val startMinute: Int,
    val endMinute: Int
) {
    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

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
    val appliedMethod: CalculationMethod = CalculationMethod.BANGLADESH_KARACHI,
    val ishaEndMinute: Int = fajrMinute
) {
    fun appliedMethodTitle(isEnglish: Boolean): String = appliedMethod.title(isEnglish)

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

enum class AdhanVoice(
    val titleBn: String,
    val titleEn: String,
    val shortTitleBn: String,
    val shortTitleEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val isFullAdhan: Boolean
) {
    MAKKAH(
        titleBn = "মক্কা আযান (মসজিদুল হারাম)",
        titleEn = "Makkah Adhan (Masjid al-Haram)",
        shortTitleBn = "মক্কা (পূর্ণ)",
        shortTitleEn = "Makkah Full",
        subtitleBn = "পূর্ণ অফলাইন মক্কা আযান (প্রায় ৩.৫ মিনিট)",
        subtitleEn = "Full offline Makkah Masjid al-Haram Adhan (~3.5 min)",
        isFullAdhan = true
    ),
    MADINAH(
        titleBn = "মদীনা আযান (মসজিদে নববী)",
        titleEn = "Madinah Adhan (Masjid an-Nabawi)",
        shortTitleBn = "মদীনা (পূর্ণ)",
        shortTitleEn = "Madinah Full",
        subtitleBn = "পূর্ণ অফলাইন মদীনা আযান (প্রায় ৩.৫ মিনিট)",
        subtitleEn = "Full offline Madinah Masjid an-Nabawi Adhan (~3.5 min)",
        isFullAdhan = true
    ),
    AL_AQSA(
        titleBn = "আল-আকসা আযান (পূর্ণ)",
        titleEn = "Al-Aqsa Adhan (Full)",
        shortTitleBn = "আল-আকসা",
        shortTitleEn = "Al-Aqsa Full",
        subtitleBn = "পূর্ণ অফলাইন মসজিদুল আকসা আযান (প্রায় ৩ মিনিট)",
        subtitleEn = "Full offline Masjid al-Aqsa Adhan (~3 min)",
        isFullAdhan = true
    ),
    SHORT_BEEP(
        titleBn = "সংক্ষিপ্ত নোটিফিকেশন বিপ",
        titleEn = "Short Notification Beep",
        shortTitleBn = "বিপ",
        shortTitleEn = "Beep",
        subtitleBn = "আযান ছাড়া হালকা সিস্টেম নোটিফিকেশন শব্দ",
        subtitleEn = "Brief notification tone without Adhan",
        isFullAdhan = false
    ),
    SILENT_VIBRATE(
        titleBn = "নীরব / শুধু ভাইব্রেশন",
        titleEn = "Silent / Vibration Only",
        shortTitleBn = "ভাইব্রেট",
        shortTitleEn = "Vibrate",
        subtitleBn = "শব্দ ছাড়া শুধুমাত্র ভাইব্রেশন ও নোটিফিকেশন",
        subtitleEn = "Vibration & notification without sound",
        isFullAdhan = false
    );

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun shortTitle(isEnglish: Boolean): String = if (isEnglish) shortTitleEn else shortTitleBn
    fun subtitle(isEnglish: Boolean): String = if (isEnglish) subtitleEn else subtitleBn

    fun toLegacyAlertMode(enabled: Boolean): AdhanAlertMode {
        if (!enabled) return AdhanAlertMode.SILENT
        return when (this) {
            MAKKAH, MADINAH, AL_AQSA -> AdhanAlertMode.FULL_ADHAN
            SHORT_BEEP -> AdhanAlertMode.BEEP
            SILENT_VIBRATE -> AdhanAlertMode.VIBRATE
        }
    }

    companion object {
        fun safeValueOf(raw: String?): AdhanVoice =
            entries.firstOrNull { it.name == raw } ?: MAKKAH

        fun fromLegacyMode(mode: AdhanAlertMode): AdhanVoice = when (mode) {
            AdhanAlertMode.FULL_ADHAN -> MAKKAH
            AdhanAlertMode.BEEP -> SHORT_BEEP
            AdhanAlertMode.VIBRATE -> SILENT_VIBRATE
            AdhanAlertMode.SILENT -> SILENT_VIBRATE
        }
    }
}

data class PerPrayerAlertSetting(
    val enabled: Boolean = true,
    val voice: AdhanVoice = AdhanVoice.MAKKAH,
    val reminderMinutesBefore: Int = 15
) {
    val legacyAlertMode: AdhanAlertMode
        get() = voice.toLegacyAlertMode(enabled)
}

data class WaqtAlertConfig(
    val fajr: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val dhuhr: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val asr: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val maghrib: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val isha: AdhanAlertMode = AdhanAlertMode.FULL_ADHAN,
    val prePrayerMinutes: Int = 15,
    val fajrSetting: PerPrayerAlertSetting = PerPrayerAlertSetting(true, AdhanVoice.MAKKAH, 15),
    val dhuhrSetting: PerPrayerAlertSetting = PerPrayerAlertSetting(true, AdhanVoice.MADINAH, 10),
    val asrSetting: PerPrayerAlertSetting = PerPrayerAlertSetting(true, AdhanVoice.AL_AQSA, 10),
    val maghribSetting: PerPrayerAlertSetting = PerPrayerAlertSetting(true, AdhanVoice.MAKKAH, 5),
    val ishaSetting: PerPrayerAlertSetting = PerPrayerAlertSetting(true, AdhanVoice.MADINAH, 15)
) {
    fun settingFor(waqt: WaqtType): PerPrayerAlertSetting = when (waqt) {
        WaqtType.FAJR -> fajrSetting
        WaqtType.DHUHR -> dhuhrSetting
        WaqtType.ASR -> asrSetting
        WaqtType.MAGHRIB -> maghribSetting
        WaqtType.ISHA -> ishaSetting
    }

    fun modeFor(waqt: WaqtType): AdhanAlertMode = settingFor(waqt).legacyAlertMode
}
