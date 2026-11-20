package com.example.data.model

enum class DuaCategory(
    val key: String,
    val titleBn: String,
    val titleEn: String
) {
    MORNING("morning", "সকালের আজকার", "Morning"),
    EVENING("evening", "সন্ধ্যার আজকার", "Evening"),
    AFTER_PRAYER("after_prayer", "নামাজের পর", "After Prayer"),
    BEFORE_SLEEP("before_sleep", "ঘুমানোর আগে", "Before Sleep"),
    TRAVEL("travel", "সফর ও যাত্রা", "Travel"),
    EATING("eating", "খাওয়া-দাওয়া", "Eating"),
    HARDSHIP("hardship", "বিপদ ও দুশ্চিন্তা", "Hardship"),
    HOME("home", "ঘরে প্রবেশ ও বের হওয়া", "Entering/Leaving Home"),
    WUDU("wudu", "ওযুর দোয়া", "Wudu"),
    RAMADAN("ramadan", "রমজান ও রোজা", "Ramadan");

    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn

    companion object {
        fun fromKey(key: String): DuaCategory =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: MORNING
    }
}

data class DuaItem(
    val id: String,
    val category: DuaCategory,
    val titleBn: String,
    val titleEn: String,
    val arabic: String,
    val transliteration: String,
    val meaningBn: String,
    val meaningEn: String,
    val referenceBn: String,
    val referenceEn: String
) {
    fun title(isEnglish: Boolean): String = if (isEnglish) titleEn else titleBn
    fun meaning(isEnglish: Boolean): String = if (isEnglish) meaningEn else meaningBn
    fun reference(isEnglish: Boolean): String = if (isEnglish) referenceEn else referenceBn
}
