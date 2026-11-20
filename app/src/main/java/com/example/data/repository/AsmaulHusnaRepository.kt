package com.example.data.repository

import android.content.Context
import com.example.data.model.AllahName
import com.example.util.BanglaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AsmaulHusnaRepository(private val context: Context) {

    @Volatile
    private var cachedNames: List<AllahName>? = null

    suspend fun loadAllNames(): List<AllahName> = withContext(Dispatchers.IO) {
        cachedNames?.let { return@withContext it }
        val jsonText = context.assets.open("asmaul_husna.json")
            .bufferedReader()
            .use { it.readText() }
        val array = JSONArray(jsonText)
        val result = ArrayList<AllahName>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                AllahName(
                    number = obj.getInt("number"),
                    arabic = obj.getString("arabic"),
                    transliteration = obj.getString("transliteration"),
                    transliterationBn = obj.getString("transliterationBn"),
                    meaningBn = obj.getString("meaningBn"),
                    meaningEn = obj.getString("meaningEn"),
                    detailBn = obj.getString("detailBn"),
                    detailEn = obj.getString("detailEn")
                )
            )
        }
        cachedNames = result
        result
    }

    fun filterNames(names: List<AllahName>, query: String): List<AllahName> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return names
        return names.filter { item ->
            item.number.toString() == trimmed ||
                BanglaUtils.toBanglaNumber(item.number) == query.trim() ||
                item.transliteration.lowercase().contains(trimmed) ||
                item.transliterationBn.lowercase().contains(trimmed) ||
                item.meaningBn.lowercase().contains(trimmed) ||
                item.meaningEn.lowercase().contains(trimmed) ||
                item.detailBn.lowercase().contains(trimmed) ||
                item.detailEn.lowercase().contains(trimmed) ||
                item.arabic.contains(query.trim())
        }
    }
}
