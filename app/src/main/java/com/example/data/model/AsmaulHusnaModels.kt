package com.example.data.model

data class AllahName(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val transliterationBn: String,
    val meaningBn: String,
    val meaningEn: String,
    val detailBn: String,
    val detailEn: String
) {
    fun displayTransliteration(isEnglish: Boolean): String =
        if (isEnglish) transliteration else transliterationBn

    fun meaning(isEnglish: Boolean): String =
        if (isEnglish) meaningEn else meaningBn

    fun detail(isEnglish: Boolean): String =
        if (isEnglish) detailEn else detailBn
}
