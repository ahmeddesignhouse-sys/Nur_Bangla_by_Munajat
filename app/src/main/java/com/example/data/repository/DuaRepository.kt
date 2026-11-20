package com.example.data.repository

import android.content.Context
import com.example.data.model.DuaCategory
import com.example.data.model.DuaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class DuaRepository(private val context: Context) {

    @Volatile
    private var cachedDuas: List<DuaItem>? = null

    suspend fun loadAllDuas(): List<DuaItem> = withContext(Dispatchers.IO) {
        cachedDuas?.let { return@withContext it }
        val jsonText = context.assets.open("duas_catalog.json")
            .bufferedReader()
            .use { it.readText() }
        val array = JSONArray(jsonText)
        val result = ArrayList<DuaItem>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                DuaItem(
                    id = obj.getString("id"),
                    category = DuaCategory.fromKey(obj.getString("category")),
                    titleBn = obj.getString("titleBn"),
                    titleEn = obj.getString("titleEn"),
                    arabic = obj.getString("arabic"),
                    transliteration = obj.optString("transliteration", ""),
                    meaningBn = obj.getString("meaningBn"),
                    meaningEn = obj.getString("meaningEn"),
                    referenceBn = obj.optString("referenceBn", ""),
                    referenceEn = obj.optString("referenceEn", "")
                )
            )
        }
        cachedDuas = result
        result
    }

    fun filterDuas(
        allDuas: List<DuaItem>,
        query: String,
        selectedCategory: DuaCategory?,
        favoritesOnly: Boolean,
        favoriteIds: Set<String>
    ): List<DuaItem> {
        val trimmed = query.trim().lowercase()
        return allDuas.filter { dua ->
            val matchesFav = !favoritesOnly || favoriteIds.contains(dua.id)
            val matchesCategory = selectedCategory == null || dua.category == selectedCategory
            val matchesQuery = trimmed.isEmpty() ||
                dua.titleBn.lowercase().contains(trimmed) ||
                dua.titleEn.lowercase().contains(trimmed) ||
                dua.meaningBn.lowercase().contains(trimmed) ||
                dua.meaningEn.lowercase().contains(trimmed) ||
                dua.transliteration.lowercase().contains(trimmed) ||
                dua.arabic.contains(query.trim()) ||
                dua.category.titleBn.lowercase().contains(trimmed) ||
                dua.category.titleEn.lowercase().contains(trimmed)
            matchesFav && matchesCategory && matchesQuery
        }
    }
}
