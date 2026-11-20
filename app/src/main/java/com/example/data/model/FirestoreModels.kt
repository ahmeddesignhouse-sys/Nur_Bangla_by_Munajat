package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class QuranBookmarkDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val surahNumber: Long = 1L,
    val ayahNumber: Long = 1L,
    val surahNameBn: String = "",
    val arabicText: String = "",
    val banglaText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "surahNumber" to surahNumber.coerceIn(1L, 114L),
        "ayahNumber" to ayahNumber.coerceIn(1L, 286L),
        "surahNameBn" to surahNameBn.take(80),
        "arabicText" to arabicText.take(2000),
        "banglaText" to banglaText.take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class UserPreferencesDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val districtId: String = "dhaka",
    val asrMethod: String = AsrMethod.HANAFI.name,
    val themeMode: String = ThemeMode.SYSTEM.name,
    val accentTheme: String = AccentTheme.ISLAMIC_GREEN.name,
    val quranDarkMode: Boolean = false,
    val quranFontSize: Long = 20L,
    val prePrayerMinutes: Long = 15L,
    val languageCode: String = "bn",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    private fun normalizedLanguage(): String = if (languageCode == "en") "en" else "bn"

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "districtId" to districtId.take(40),
        "asrMethod" to asrMethod,
        "themeMode" to themeMode,
        "accentTheme" to accentTheme,
        "quranDarkMode" to quranDarkMode,
        "quranFontSize" to quranFontSize.coerceIn(14L, 32L),
        "prePrayerMinutes" to prePrayerMinutes.coerceIn(0L, 45L),
        "languageCode" to normalizedLanguage(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "districtId" to districtId.take(40),
        "asrMethod" to asrMethod,
        "themeMode" to themeMode,
        "accentTheme" to accentTheme,
        "quranDarkMode" to quranDarkMode,
        "quranFontSize" to quranFontSize.coerceIn(14L, 32L),
        "prePrayerMinutes" to prePrayerMinutes.coerceIn(0L, 45L),
        "languageCode" to normalizedLanguage(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
