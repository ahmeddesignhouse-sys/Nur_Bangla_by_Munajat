package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class NearbyHalalRestaurant(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val address: String,
    val cuisine: String,
    val halalBadgeBn: String,
    val halalBadgeEn: String,
    val rating: String?,
    val openingHours: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val bearingFromUserDeg: Float
) {
    fun displayName(isEnglish: Boolean): String =
        if (isEnglish) nameEn.ifBlank { nameBn } else nameBn.ifBlank { nameEn }

    fun halalBadge(isEnglish: Boolean): String =
        if (isEnglish) halalBadgeEn else halalBadgeBn
}

sealed class HalalSearchResult {
    data object Offline : HalalSearchResult()
    data class Success(val restaurants: List<NearbyHalalRestaurant>) : HalalSearchResult()
    data class Error(val messageBn: String, val messageEn: String) : HalalSearchResult()
}

/**
 * Online nearby Halal restaurant discovery backed by OpenStreetMap Overpass API (no API key required).
 */
class HalalRestaurantRepository(private val context: Context) {

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

    suspend fun findNearbyHalalRestaurants(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int = 5000
    ): HalalSearchResult = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext HalalSearchResult.Offline
        }

        val query = """
            [out:json][timeout:18];
            (
              node["amenity"~"restaurant|fast_food|cafe|food_court"]["diet:halal"~"yes|only"](around:$radiusMeters,$userLat,$userLon);
              way["amenity"~"restaurant|fast_food|cafe|food_court"]["diet:halal"~"yes|only"](around:$radiusMeters,$userLat,$userLon);
              node["amenity"~"restaurant|fast_food|cafe|food_court"]["halal"~"yes|only"](around:$radiusMeters,$userLat,$userLon);
              way["amenity"~"restaurant|fast_food|cafe|food_court"]["halal"~"yes|only"](around:$radiusMeters,$userLat,$userLon);
              node["amenity"~"restaurant|fast_food"]["cuisine"~"halal|bangladeshi|bengali|pakistani|arab|turkish|kebab|biryani|middle_eastern|mughlai|lebanese",i](around:$radiusMeters,$userLat,$userLon);
              way["amenity"~"restaurant|fast_food"]["cuisine"~"halal|bangladeshi|bengali|pakistani|arab|turkish|kebab|biryani|middle_eastern|mughlai|lebanese",i](around:$radiusMeters,$userLat,$userLon);
              node["amenity"~"restaurant|fast_food"]["name"~"halal|biryani|kebab|kabab|mezban|kacchi|tehari|shawarma|grill",i](around:$radiusMeters,$userLat,$userLon);
              way["amenity"~"restaurant|fast_food"]["name"~"halal|biryani|kebab|kabab|mezban|kacchi|tehari|shawarma|grill",i](around:$radiusMeters,$userLat,$userLon);
            );
            out center 60;
        """.trimIndent()

        var lastException: Exception? = null
        for (endpoint in OVERPASS_ENDPOINTS) {
            try {
                val responseText = postOverpassQuery(endpoint, query)
                val places = parseOverpassResponse(responseText, userLat, userLon)
                return@withContext HalalSearchResult.Success(places)
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (!isOnline()) {
            HalalSearchResult.Offline
        } else {
            HalalSearchResult.Error(
                messageBn = "হালাল রেস্টুরেন্টের তথ্য লোড করা যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।",
                messageEn = "Could not load nearby halal restaurants right now (${lastException?.message ?: "network error"}). Please retry."
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
            setRequestProperty("User-Agent", "NurBanglaAndroid/1.5")
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
        ): List<NearbyHalalRestaurant> {
            val root = JSONObject(jsonText)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            val results = ArrayList<NearbyHalalRestaurant>(elements.length())
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
                if (rawName.isBlank() && tags?.optString("name:bn", "").isNullOrBlank() && tags?.optString("name:en", "").isNullOrBlank()) {
                    continue
                }
                val nameBn = tags?.optString("name:bn", "").orEmpty().trim()
                    .ifBlank { rawName }
                    .ifBlank { "হালাল রেস্টুরেন্ট" }
                val nameEn = tags?.optString("name:en", "").orEmpty().trim()
                    .ifBlank { rawName }
                    .ifBlank { "Halal Restaurant" }

                val addressParts = listOfNotNull(
                    tags?.optString("addr:full", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:housenumber", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:street", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:suburb", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:neighbourhood", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:city", "")?.takeIf { it.isNotBlank() }
                ).distinct()
                val address = addressParts.joinToString(", ")

                val rawCuisine = tags?.optString("cuisine", "").orEmpty().trim()
                val formattedCuisine = rawCuisine
                    .split(";", ",")
                    .map { it.trim().replace("_", " ") }
                    .filter { it.isNotBlank() }
                    .joinToString(" • ") { part ->
                        part.replaceFirstChar { ch -> ch.uppercaseChar() }
                    }

                val dietHalal = tags?.optString("diet:halal", "").orEmpty().lowercase()
                val halalFlag = tags?.optString("halal", "").orEmpty().lowercase()
                val (badgeBn, badgeEn) = when {
                    dietHalal == "only" || halalFlag == "only" -> "১০০% হালাল" to "100% Halal"
                    dietHalal == "yes" || halalFlag == "yes" -> "হালাল খাবার" to "Halal Verified"
                    else -> "হালাল-বান্ধব" to "Halal-Friendly"
                }

                val rating = tags?.optString("rating", "")?.takeIf { it.isNotBlank() }
                    ?: tags?.optString("stars", "")?.takeIf { it.isNotBlank() }
                val hours = tags?.optString("opening_hours", "")?.takeIf { it.isNotBlank() }

                val distKm = MosqueRepository.haversineKm(userLat, userLon, lat, lon)
                val dedupeKey = "${nameEn.lowercase()}_${(lat * 1000).toInt()}_${(lon * 1000).toInt()}"
                if (!seenKeys.add(dedupeKey)) continue

                results.add(
                    NearbyHalalRestaurant(
                        id = "${el.optString("type", "n")}_${el.optLong("id", i.toLong())}",
                        nameBn = nameBn,
                        nameEn = nameEn,
                        address = address,
                        cuisine = formattedCuisine,
                        halalBadgeBn = badgeBn,
                        halalBadgeEn = badgeEn,
                        rating = rating,
                        openingHours = hours,
                        latitude = lat,
                        longitude = lon,
                        distanceKm = distKm,
                        bearingFromUserDeg = MosqueRepository.bearingDegrees(userLat, userLon, lat, lon)
                    )
                )
            }

            return results.sortedBy { it.distanceKm }
        }

        fun filterRestaurants(
            restaurants: List<NearbyHalalRestaurant>,
            query: String
        ): List<NearbyHalalRestaurant> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return restaurants
            return restaurants.filter { r ->
                r.nameBn.lowercase().contains(q) ||
                    r.nameEn.lowercase().contains(q) ||
                    r.address.lowercase().contains(q) ||
                    r.cuisine.lowercase().contains(q) ||
                    r.halalBadgeBn.lowercase().contains(q) ||
                    r.halalBadgeEn.lowercase().contains(q)
            }
        }
    }
}
