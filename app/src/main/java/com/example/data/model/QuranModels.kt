package com.example.data.model

data class Ayah(
    val number: Int,
    val arabic: String,
    val bangla: String
)

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val revelationTypeBn: String,
    val totalAyahs: Int,
    val ayahs: List<Ayah>
)

data class QuranSearchHit(
    val surah: Surah,
    val ayah: Ayah
)

data class DhikrItem(
    val id: String,
    val arabic: String,
    val banglaTitle: String,
    val banglaMeaning: String,
    val recommendedCount: Int
)

object BanglaDhikrCatalog {
    val ALL: List<DhikrItem> = listOf(
        DhikrItem(
            id = "subhanallah",
            arabic = "سُبْحَانَ اللَّهِ",
            banglaTitle = "সুবহানাল্লাহ",
            banglaMeaning = "আল্লাহ পবিত্র ও মহিমান্বিত",
            recommendedCount = 33
        ),
        DhikrItem(
            id = "alhamdulillah",
            arabic = "الْحَمْدُ لِلَّهِ",
            banglaTitle = "আলহামদুলিল্লাহ",
            banglaMeaning = "সমস্ত প্রশংসা একমাত্র আল্লাহর জন্য",
            recommendedCount = 33
        ),
        DhikrItem(
            id = "allahuakbar",
            arabic = "اللَّهُ أَكْبَرُ",
            banglaTitle = "আল্লাহু আকবার",
            banglaMeaning = "আল্লাহ সর্বশ্রেষ্ঠ",
            recommendedCount = 34
        ),
        DhikrItem(
            id = "lailahaillallah",
            arabic = "لَا إِلَٰهَ إِلَّا اللَّهُ",
            banglaTitle = "লা ইলাহা ইল্লাল্লাহ",
            banglaMeaning = "আল্লাহ ছাড়া কোনো ইলাহ (উপাস্য) নেই",
            recommendedCount = 100
        ),
        DhikrItem(
            id = "astaghfirullah",
            arabic = "أَسْتَغْفِرُ اللَّهَ",
            banglaTitle = "আস্তাগফিরুল্লাহ",
            banglaMeaning = "আমি আল্লাহর নিকট ক্ষমা প্রার্থনা করছি",
            recommendedCount = 100
        ),
        DhikrItem(
            id = "durood",
            arabic = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
            banglaTitle = "দরুদ শরিফ",
            banglaMeaning = "হে আল্লাহ! মুহাম্মদ (সা.)-এর ওপর রহমত বর্ষণ করুন",
            recommendedCount = 33
        )
    )
}
