package com.example.data.model

data class Ayah(
    val number: Int,
    val arabic: String,
    val bangla: String,
    val english: String = ""
) {
    fun translation(isEnglish: Boolean): String =
        if (isEnglish && english.isNotBlank()) english else bangla
}

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String = "",
    val revelationTypeBn: String,
    val revelationTypeEn: String = "",
    val totalAyahs: Int,
    val ayahs: List<Ayah>
) {
    fun displayName(isEnglish: Boolean): String =
        if (isEnglish && nameEnglish.isNotBlank()) nameEnglish else nameBangla

    fun revelationType(isEnglish: Boolean): String =
        if (isEnglish && revelationTypeEn.isNotBlank()) revelationTypeEn else revelationTypeBn
}

data class QuranSearchHit(
    val surah: Surah,
    val ayah: Ayah
)

data class DhikrItem(
    val id: String,
    val arabic: String,
    val banglaTitle: String,
    val englishTitle: String,
    val banglaMeaning: String,
    val englishMeaning: String,
    val recommendedCount: Int
) {
    fun title(isEnglish: Boolean): String = if (isEnglish) englishTitle else banglaTitle
    fun meaning(isEnglish: Boolean): String = if (isEnglish) englishMeaning else banglaMeaning
}

object BanglaDhikrCatalog {
    val ALL: List<DhikrItem> = listOf(
        DhikrItem(
            id = "subhanallah",
            arabic = "سُبْحَانَ اللَّهِ",
            banglaTitle = "সুবহানাল্লাহ",
            englishTitle = "SubhanAllah",
            banglaMeaning = "আল্লাহ পবিত্র ও মহিমান্বিত",
            englishMeaning = "Glory be to Allah",
            recommendedCount = 33
        ),
        DhikrItem(
            id = "alhamdulillah",
            arabic = "الْحَمْدُ لِلَّهِ",
            banglaTitle = "আলহামদুলিল্লাহ",
            englishTitle = "Alhamdulillah",
            banglaMeaning = "সমস্ত প্রশংসা একমাত্র আল্লাহর জন্য",
            englishMeaning = "All praise is due to Allah",
            recommendedCount = 33
        ),
        DhikrItem(
            id = "allahuakbar",
            arabic = "اللَّهُ أَكْبَرُ",
            banglaTitle = "আল্লাহু আকবার",
            englishTitle = "Allahu Akbar",
            banglaMeaning = "আল্লাহ সর্বশ্রেষ্ঠ",
            englishMeaning = "Allah is the Greatest",
            recommendedCount = 34
        ),
        DhikrItem(
            id = "lailahaillallah",
            arabic = "لَا إِلَٰهَ إِلَّا اللَّهُ",
            banglaTitle = "লা ইলাহা ইল্লাল্লাহ",
            englishTitle = "La ilaha illallah",
            banglaMeaning = "আল্লাহ ছাড়া কোনো ইলাহ (উপাস্য) নেই",
            englishMeaning = "There is no deity worthy of worship except Allah",
            recommendedCount = 100
        ),
        DhikrItem(
            id = "astaghfirullah",
            arabic = "أَسْتَغْفِرُ اللَّهَ",
            banglaTitle = "আস্তাগফিরুল্লাহ",
            englishTitle = "Astaghfirullah",
            banglaMeaning = "আমি আল্লাহর নিকট ক্ষমা প্রার্থনা করছি",
            englishMeaning = "I seek forgiveness from Allah",
            recommendedCount = 100
        ),
        DhikrItem(
            id = "durood",
            arabic = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
            banglaTitle = "দরুদ শরিফ",
            englishTitle = "Durood Sharif",
            banglaMeaning = "হে আল্লাহ! মুহাম্মদ (সা.)-এর ওপর রহমত বর্ষণ করুন",
            englishMeaning = "O Allah, send blessings upon Prophet Muhammad (PBUH)",
            recommendedCount = 33
        )
    )
}
