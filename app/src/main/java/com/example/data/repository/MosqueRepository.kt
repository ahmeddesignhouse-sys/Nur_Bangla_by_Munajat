package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.util.PrayerTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyMosque(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val bearingFromUserDeg: Float,
    val qiblaBearingDeg: Float
) {
    fun displayName(isEnglish: Boolean): String =
        if (isEnglish) nameEn.ifBlank { nameBn } else nameBn.ifBlank { nameEn }
}

sealed class MosqueSearchResult {
    data object Offline : MosqueSearchResult()
    data class Success(val mosques: List<NearbyMosque>) : MosqueSearchResult()
    data class Error(val messageBn: String, val messageEn: String) : MosqueSearchResult()
}

/**
 * Online nearby mosque discovery backed by OpenStreetMap Overpass Places API (no API key required).
 */
class MosqueRepository(private val context: Context) {

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun findNearbyMosques(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int = 5000
    ): MosqueSearchResult = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext MosqueSearchResult.Offline
        }

        val query = """
            [out:json][timeout:18];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$userLat,$userLon);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$userLat,$userLon);
              relation["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$userLat,$userLon);
            );
            out center 60;
        """.trimIndent()

        var lastException: Exception? = null
        for (endpoint in OVERPASS_ENDPOINTS) {
            try {
                val responseText = postOverpassQuery(endpoint, query)
                val mosques = parseOverpassResponse(responseText, userLat, userLon)
                return@withContext MosqueSearchResult.Success(mosques)
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (!isOnline()) {
            MosqueSearchResult.Offline
        } else {
            MosqueSearchResult.Error(
                messageBn = "মসজিদের তথ্য লোড করতে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।",
                messageEn = "Could not load nearby mosques right now (${lastException?.message ?: "network error"}). Please retry."
            )
        }
    }

    private fun postOverpassQuery(endpointUrl: String, query: String): String {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 18_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", "NurBanglaAndroid/1.3")
        }
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write("data=" + URLEncoder.encode(query, "UTF-8"))
                writer.flush()
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private val OVERPASS_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )

        fun parseOverpassResponse(
            jsonText: String,
            userLat: Double,
            userLon: Double
        ): List<NearbyMosque> {
            val root = JSONObject(jsonText)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            val results = ArrayList<NearbyMosque>(elements.length())
            val seenKeys = HashSet<String>()

            for (i in 0 until elements.length()) {
                val el = elements.optJSONObject(i) ?: continue
                val center = el.optJSONObject("center")
                val lat = if (el.has("lat")) el.optDouble("lat", Double.NaN)
                else center?.optDouble("lat", Double.NaN) ?: Double.NaN
                val lon = if (el.has("lon")) el.optDouble("lon", Double.NaN)
                else center?.optDouble("lon", Double.NaN) ?: Double.NaN
                if (lat.isNaN() || lon.isNaN()) continue

                val tags = el.optJSONObject("tags")
                val rawName = tags?.optString("name", "").orEmpty().trim()
                val nameBn = tags?.optString("name:bn", "").orEmpty().trim()
                    .ifBlank { rawName }
                    .ifBlank { "জামে মসজিদ" }
                val nameEn = tags?.optString("name:en", "").orEmpty().trim()
                    .ifBlank { rawName }
                    .ifBlank { "Masjid / Mosque" }

                val addressParts = listOfNotNull(
                    tags?.optString("addr:full", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:street", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:suburb", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:neighbourhood", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:city", "")?.takeIf { it.isNotBlank() }
                ).distinct()
                val address = addressParts.joinToString(", ")

                val distKm = haversineKm(userLat, userLon, lat, lon)
                val dedupeKey = "${nameEn.lowercase()}_${(lat * 1000).toInt()}_${(lon * 1000).toInt()}"
                if (!seenKeys.add(dedupeKey)) continue

                results.add(
                    NearbyMosque(
                        id = "${el.optString("type", "n")}_${el.optLong("id", i.toLong())}",
                        nameBn = nameBn,
                        nameEn = nameEn,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        distanceKm = distKm,
                        bearingFromUserDeg = bearingDegrees(userLat, userLon, lat, lon),
                        qiblaBearingDeg = PrayerTimeCalculator.calculateQiblaBearing(lat, lon)
                    )
                )
            }

            return results.sortedBy { it.distanceKm }
        }

        fun filterMosques(mosques: List<NearbyMosque>, query: String): List<NearbyMosque> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return mosques
            return mosques.filter { m ->
                m.nameBn.lowercase().contains(q) ||
                    m.nameEn.lowercase().contains(q) ||
                    m.address.lowercase().contains(q)
            }
        }

        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val dLon = Math.toRadians(lon2 - lon1)
            val y = sin(dLon) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
            return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
        }

        fun cardinalLabel(bearingDeg: Float, isEnglish: Boolean): String {
            val norm = ((bearingDeg % 360f) + 360f) % 360f
            return when {
                norm < 22.5f || norm >= 337.5f -> if (isEnglish) "North" else "উত্তর"
                norm < 67.5f -> if (isEnglish) "North-East" else "উত্তর-পূর্ব"
                norm < 112.5f -> if (isEnglish) "East" else "পূর্ব"
                norm < 157.5f -> if (isEnglish) "South-East" else "দক্ষিণ-পূর্ব"
                norm < 202.5f -> if (isEnglish) "South" else "দক্ষিণ"
                norm < 247.5f -> if (isEnglish) "South-West" else "দক্ষিণ-পশ্চিম"
                norm < 292.5f -> if (isEnglish) "West" else "পশ্চিম"
                else -> if (isEnglish) "North-West" else "উত্তর-পশ্চিম"
            }
        }
    }
}
