package com.example.data.repository

import android.content.Context
import com.example.data.model.Ayah
import com.example.data.model.QuranSearchHit
import com.example.data.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QuranRepository(private val context: Context) {
    @Volatile
    private var cachedSurahs: List<Surah>? = null

    suspend fun loadSurahs(): List<Surah> = withContext(Dispatchers.IO) {
        cachedSurahs?.let { return@withContext it }
        val jsonText = context.applicationContext.assets.open("quran_bangla.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(jsonText)
        val surahArray = root.getJSONArray("surahs")
        val list = ArrayList<Surah>(surahArray.length())
        for (i in 0 until surahArray.length()) {
            val obj = surahArray.getJSONObject(i)
            val ayahArr = obj.getJSONArray("ayahs")
            val ayahs = ArrayList<Ayah>(ayahArr.length())
            for (j in 0 until ayahArr.length()) {
                val aObj = ayahArr.getJSONObject(j)
                ayahs.add(
                    Ayah(
                        number = aObj.getInt("number"),
                        arabic = aObj.getString("arabic"),
                        bangla = aObj.getString("bangla")
                    )
                )
            }
            list.add(
                Surah(
                    number = obj.getInt("number"),
                    nameArabic = obj.getString("nameArabic"),
                    nameBangla = obj.getString("nameBangla"),
                    revelationTypeBn = obj.getString("revelationTypeBn"),
                    totalAyahs = obj.getInt("totalAyahs"),
                    ayahs = ayahs
                )
            )
        }
        cachedSurahs = list
        list
    }

    suspend fun searchVerses(query: String): List<QuranSearchHit> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val surahs = loadSurahs()
        val hits = mutableListOf<QuranSearchHit>()
        for (surah in surahs) {
            val surahMatches = surah.nameBangla.contains(trimmed, ignoreCase = true) ||
                surah.nameArabic.contains(trimmed)
            for (ayah in surah.ayahs) {
                if (surahMatches ||
                    ayah.bangla.contains(trimmed, ignoreCase = true) ||
                    ayah.arabic.contains(trimmed)
                ) {
                    hits.add(QuranSearchHit(surah = surah, ayah = ayah))
                }
            }
        }
        return hits
    }
}
