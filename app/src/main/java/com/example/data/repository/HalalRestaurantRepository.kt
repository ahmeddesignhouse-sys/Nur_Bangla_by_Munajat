package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.util.LocationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
    val bearingFromUserDeg: Float,
    val isExplicitHalal: Boolean = false
) {
    fun displayName(isEnglish: Boolean): String =
        if (isEnglish) nameEn.ifBlank { nameBn } else nameBn.ifBlank { nameEn }

    fun halalBadge(isEnglish: Boolean): String =
        if (isEnglish) halalBadgeEn else halalBadgeBn
}

sealed class HalalSearchResult {
    data object Offline : HalalSearchResult()
    data class Success(
        val restaurants: List<NearbyHalalRestaurant>,
        val requestedRadiusMeters: Int = 5000,
        val effectiveRadiusMeters: Int = requestedRadiusMeters,
        val usedAllRestaurantsFallback: Boolean = false,
        val fromCache: Boolean = false
    ) : HalalSearchResult() {
        val wasExpanded: Boolean
            get() = effectiveRadiusMeters > requestedRadiusMeters
    }
    data class Error(val messageBn: String, val messageEn: String) : HalalSearchResult()
}

/**
 * Fast, reliable nearby Halal restaurant discovery via OpenStreetMap Overpass API:
 * - Primary: strict Halal tag/cuisine/name query (`around:radius,lat,lon`) for restaurant/cafe/fast_food.
 * - Automatic Fallback: if strict query returns 0 results, fetches ALL nearby restaurants/cafes/fast_food
 *   within the same radius and ranks any with "Halal" in name/cuisine FIRST.
 * - Automatic radius expansion (2 km -> 5 km -> 10 km -> 20 km) if still 0 results.
 * - 20-second timeout with ONE retry only, 10-minute in-memory cache, full URL logging & cancellation.
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
        radiusMeters: Int = 5000,
        forceRefresh: Boolean = false
    ): HalalSearchResult = withContext(Dispatchers.IO) {
        if (!LocationHelper.isValidCoordinate(userLat, userLon)) {
            return@withContext HalalSearchResult.Error(
                messageBn = "সঠিক জিপিএস লোকেশন পাওয়া যায়নি। লোকেশন চালু করে আবার চেষ্টা করুন।",
                messageEn = "Valid GPS location unavailable. Please enable Location and retry."
            )
        }

        if (!isOnline()) {
            return@withContext HalalSearchResult.Offline
        }

        val cacheKey = buildCacheKey(userLat, userLon, radiusMeters)
        if (!forceRefresh) {
            getCachedResult(cacheKey)?.let { cached ->
                runCatching { Log.d(TAG, "Returning cached halal places (${cached.restaurants.size}) for $cacheKey") }
                return@withContext cached.copy(fromCache = true)
            }
        }

        val radiusLadder = buildRadiusLadder(radiusMeters)
        var anyServerResponded = false
        var lastException: Exception? = null
        var lastHttpCode: Int? = null

        for (stepRadius in radiusLadder) {
            currentCoroutineContext().ensureActive()
            val maxKm = stepRadius / 1000.0

            // 1. Primary: Strict Halal query (diet:halal=yes OR cuisine~halal OR name~Halal)
            val strictQuery = buildStrictHalalOverpassQuery(userLat, userLon, stepRadius)
            val strictOutcome = executeOverpassWithOneRetry(strictQuery, "strict_halal")
            when (strictOutcome) {
                is OverpassCallOutcome.Success -> {
                    anyServerResponded = true
                    val strictResults = parseOverpassResponse(strictOutcome.body, userLat, userLon)
                        .filter { it.distanceKm <= maxKm + 0.15 }
                    runCatching {
                        Log.d(TAG, "Overpass strict halal at ${stepRadius}m returned ${strictResults.size} results")
                    }
                    if (strictResults.isNotEmpty()) {
                        val success = HalalSearchResult.Success(
                            restaurants = strictResults,
                            requestedRadiusMeters = radiusMeters,
                            effectiveRadiusMeters = stepRadius,
                            usedAllRestaurantsFallback = false,
                            fromCache = false
                        )
                        putCachedResult(cacheKey, success)
                        return@withContext success
                    }
                }
                is OverpassCallOutcome.Failure -> {
                    lastException = strictOutcome.exception
                    lastHttpCode = strictOutcome.httpCode
                    // Server failed/timed out even after 1 retry -> stop immediately
                    break
                }
            }

            // 2. Fallback within the SAME radius: ALL restaurants / cafes / fast_food, Halal-named first
            currentCoroutineContext().ensureActive()
            val broadQuery = buildAllRestaurantsOverpassQuery(userLat, userLon, stepRadius)
            val broadOutcome = executeOverpassWithOneRetry(broadQuery, "all_restaurants_fallback")
            when (broadOutcome) {
                is OverpassCallOutcome.Success -> {
                    anyServerResponded = true
                    val broadResults = parseOverpassResponse(broadOutcome.body, userLat, userLon)
                        .filter { it.distanceKm <= maxKm + 0.15 }
                    runCatching {
                        Log.d(TAG, "Overpass broad restaurant fallback at ${stepRadius}m returned ${broadResults.size} results")
                    }
                    if (broadResults.isNotEmpty()) {
                        val success = HalalSearchResult.Success(
                            restaurants = broadResults,
                            requestedRadiusMeters = radiusMeters,
                            effectiveRadiusMeters = stepRadius,
                            usedAllRestaurantsFallback = true,
                            fromCache = false
                        )
                        putCachedResult(cacheKey, success)
                        return@withContext success
                    }
                    // Truly 0 restaurants in this radius -> auto-expand to next radius step
                }
                is OverpassCallOutcome.Failure -> {
                    lastException = broadOutcome.exception
                    lastHttpCode = broadOutcome.httpCode
                    break
                }
            }
        }

        if (anyServerResponded) {
            return@withContext HalalSearchResult.Success(
                restaurants = emptyList(),
                requestedRadiusMeters = radiusMeters,
                effectiveRadiusMeters = radiusLadder.last()
            )
        }

        if (!isOnline() || lastException is UnknownHostException) {
            HalalSearchResult.Offline
        } else {
            buildSpecificError(lastException, lastHttpCode)
        }
    }

    private sealed class OverpassCallOutcome {
        data class Success(val body: String) : OverpassCallOutcome()
        data class Failure(val exception: Exception?, val httpCode: Int?) : OverpassCallOutcome()
    }

    private suspend fun executeOverpassWithOneRetry(query: String, label: String): OverpassCallOutcome {
        var lastException: Exception? = null
        var lastHttpCode: Int? = null

        // Primary attempt + ONE retry only (2 endpoints max)
        for ((index, endpoint) in OVERPASS_ENDPOINTS.withIndex()) {
            currentCoroutineContext().ensureActive()
            val fullLoggedUrl = "$endpoint?data=${URLEncoder.encode(query, "UTF-8")}"
            val startedMs = System.currentTimeMillis()
            runCatching {
                Log.d(TAG, "Overpass $label attempt #${index + 1} URL: $fullLoggedUrl")
            }
            try {
                val body = postOverpassQueryCancellable(endpoint, query)
                runCatching {
                    val elapsed = System.currentTimeMillis() - startedMs
                    Log.d(TAG, "Overpass $label attempt #${index + 1} succeeded in ${elapsed}ms (${body.length} chars)")
                }
                return OverpassCallOutcome.Success(body)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpStatusException) {
                lastHttpCode = e.statusCode
                lastException = e
                runCatching { Log.w(TAG, "Overpass $label attempt #${index + 1} HTTP ${e.statusCode}") }
            } catch (e: Exception) {
                lastException = e
                runCatching { Log.w(TAG, "Overpass $label attempt #${index + 1} failed: ${e.javaClass.simpleName}: ${e.message}") }
            }
        }

        return OverpassCallOutcome.Failure(lastException, lastHttpCode)
    }

    private suspend fun postOverpassQueryCancellable(endpointUrl: String, query: String): String {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val job = currentCoroutineContext()[Job]
        val cancelHandle = job?.invokeOnCompletion { cause ->
            if (cause != null) {
                runCatching { conn.disconnect() }
            }
        }

        return try {
            currentCoroutineContext().ensureActive()
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write("data=" + URLEncoder.encode(query, "UTF-8"))
                writer.flush()
            }
            currentCoroutineContext().ensureActive()
            val code = conn.responseCode
            if (code !in 200..299) {
                throw HttpStatusException(code)
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            cancelHandle?.dispose()
            runCatching { conn.disconnect() }
        }
    }

    private fun buildSpecificError(
        lastException: Exception?,
        lastHttpCode: Int?
    ): HalalSearchResult.Error {
        return when {
            lastException is SocketTimeoutException ||
                lastHttpCode == 429 ||
                lastHttpCode == 503 ||
                lastHttpCode == 504 -> HalalSearchResult.Error(
                messageBn = "সার্ভার ব্যস্ত, আবার চেষ্টা করুন",
                messageEn = "Server busy, please try again"
            )
            else -> HalalSearchResult.Error(
                messageBn = "সার্ভার ব্যস্ত, আবার চেষ্টা করুন",
                messageEn = "Server busy, please try again"
            )
        }
    }

    internal class HttpStatusException(val statusCode: Int) : IllegalStateException("HTTP $statusCode")

    private data class CacheEntry(
        val savedAtMs: Long,
        val result: HalalSearchResult.Success
    )

    companion object {
        private const val TAG = "HalalRestaurantRepo"
        private const val USER_AGENT = "NurBanglaAndroid/1.7 (com.noorbangla.islam)"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
        const val CACHE_TTL_MS = 10 * 60 * 1000L

        // Primary + ONE retry endpoint only
        val OVERPASS_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )

        private val HALAL_EXPLICIT_REGEX = Regex("halal|হালাল", RegexOption.IGNORE_CASE)
        private val HALAL_FRIENDLY_REGEX = Regex(
            "(biryani|biriyani|kebab|kabab|mezban|kacchi|tehari|shawarma|grill|mughlai|tandoori|nihari|haleem|bangla|dhakaiya)",
            RegexOption.IGNORE_CASE
        )

        private val FOOD_AMENITY_TYPES = setOf("restaurant", "fast_food", "cafe", "food_court", "bakery")

        private val resultCache = ConcurrentHashMap<String, CacheEntry>()

        internal fun buildCacheKey(lat: Double, lon: Double, radiusMeters: Int): String {
            val latGrid = (lat * 500).toInt()
            val lonGrid = (lon * 500).toInt()
            return "h_${latGrid}_${lonGrid}_$radiusMeters"
        }

        internal fun getCachedResult(key: String, nowMs: Long = System.currentTimeMillis()): HalalSearchResult.Success? {
            val entry = resultCache[key] ?: return null
            if (nowMs - entry.savedAtMs > CACHE_TTL_MS) {
                resultCache.remove(key)
                return null
            }
            return entry.result
        }

        internal fun putCachedResult(key: String, result: HalalSearchResult.Success, nowMs: Long = System.currentTimeMillis()) {
            resultCache[key] = CacheEntry(savedAtMs = nowMs, result = result)
        }

        fun clearCache() {
            resultCache.clear()
        }

        fun buildRadiusLadder(requestedRadiusMeters: Int): List<Int> =
            MosqueRepository.buildRadiusLadder(requestedRadiusMeters)

        /**
         * Primary strict Halal Overpass query (`around:radius,lat,lon`) for restaurant/cafe/fast_food
         * matching diet:halal=yes|only OR cuisine~halal OR name~Halal.
         */
        fun buildStrictHalalOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
            val latStr = String.format(Locale.US, "%.5f", lat)
            val lonStr = String.format(Locale.US, "%.5f", lon)
            val safeRadius = radiusMeters.coerceIn(500, 20000)
            return """
                [out:json][timeout:20];
                (
                  node["amenity"~"^(restaurant|cafe|fast_food)$"]["diet:halal"~"^(yes|only)$",i](around:$safeRadius,$latStr,$lonStr);
                  way["amenity"~"^(restaurant|cafe|fast_food)$"]["diet:halal"~"^(yes|only)$",i](around:$safeRadius,$latStr,$lonStr);
                  node["amenity"~"^(restaurant|cafe|fast_food)$"]["cuisine"~"halal",i](around:$safeRadius,$latStr,$lonStr);
                  way["amenity"~"^(restaurant|cafe|fast_food)$"]["cuisine"~"halal",i](around:$safeRadius,$latStr,$lonStr);
                  node["amenity"~"^(restaurant|cafe|fast_food)$"]["name"~"halal",i](around:$safeRadius,$latStr,$lonStr);
                  way["amenity"~"^(restaurant|cafe|fast_food)$"]["name"~"halal",i](around:$safeRadius,$latStr,$lonStr);
                );
                out center 60;
            """.trimIndent()
        }

        /**
         * Broad Overpass fallback query when strict Halal tags return 0 results in regions
         * (like Bangladesh) where OSM mappers tag amenity=restaurant/cafe/fast_food without diet:halal.
         */
        fun buildAllRestaurantsOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
            val latStr = String.format(Locale.US, "%.5f", lat)
            val lonStr = String.format(Locale.US, "%.5f", lon)
            val safeRadius = radiusMeters.coerceIn(500, 20000)
            return """
                [out:json][timeout:20];
                (
                  node["amenity"~"^(restaurant|cafe|fast_food)$"](around:$safeRadius,$latStr,$lonStr);
                  way["amenity"~"^(restaurant|cafe|fast_food)$"](around:$safeRadius,$latStr,$lonStr);
                );
                out center 60;
            """.trimIndent()
        }

        fun parseOverpassResponse(
            jsonText: String,
            userLat: Double,
            userLon: Double
        ): List<NearbyHalalRestaurant> {
            val root = JSONObject(jsonText)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            val results = ArrayList<NearbyHalalRestaurant>(elements.length())

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
                val rawNameBn = tags?.optString("name:bn", "").orEmpty().trim()
                val rawNameEn = tags?.optString("name:en", "").orEmpty().trim()
                if (rawName.isBlank() && rawNameBn.isBlank() && rawNameEn.isBlank()) {
                    continue
                }
                val nameBn = rawNameBn.ifBlank { rawName }.ifBlank { rawNameEn }
                val nameEn = rawNameEn.ifBlank { rawName }.ifBlank { rawNameBn }

                val addressParts = listOfNotNull(
                    tags?.optString("addr:full", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:housenumber", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:street", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:suburb", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:neighbourhood", "")?.takeIf { it.isNotBlank() },
                    tags?.optString("addr:city", "")?.takeIf { it.isNotBlank() }
                ).distinct()
                val address = addressParts.joinToString(", ")

                val amenity = tags?.optString("amenity", "").orEmpty().lowercase()
                val rawCuisine = tags?.optString("cuisine", "").orEmpty().trim()
                val formattedCuisine = rawCuisine
                    .split(";", ",")
                    .map { it.trim().replace("_", " ") }
                    .filter { it.isNotBlank() }
                    .joinToString(" • ") { part ->
                        part.replaceFirstChar { ch -> ch.uppercaseChar() }
                    }
                    .ifBlank {
                        when (amenity) {
                            "cafe" -> "Cafe"
                            "fast_food" -> "Fast Food"
                            else -> "Restaurant"
                        }
                    }

                val dietHalal = tags?.optString("diet:halal", "").orEmpty().lowercase()
                val halalFlag = tags?.optString("halal", "").orEmpty().lowercase()
                val nameOrCuisineHasHalal = HALAL_EXPLICIT_REGEX.containsMatchIn(nameEn) ||
                    HALAL_EXPLICIT_REGEX.containsMatchIn(nameBn) ||
                    HALAL_EXPLICIT_REGEX.containsMatchIn(rawCuisine)

                val isExplicitHalal = dietHalal == "only" || dietHalal == "yes" ||
                    halalFlag == "only" || halalFlag == "yes" ||
                    nameOrCuisineHasHalal

                val isHalalFriendlyKeyword = HALAL_FRIENDLY_REGEX.containsMatchIn(nameEn) ||
                    HALAL_FRIENDLY_REGEX.containsMatchIn(rawCuisine)

                val (badgeBn, badgeEn) = when {
                    dietHalal == "only" || halalFlag == "only" -> "১০০% হালাল" to "100% Halal"
                    isExplicitHalal -> "হালাল খাবার" to "Halal Verified"
                    isHalalFriendlyKeyword -> "হালাল-বান্ধব" to "Halal-Friendly"
                    else -> "নিকটবর্তী রেস্টুরেন্ট" to "Nearby Dining"
                }

                val rating = tags?.optString("rating", "")?.takeIf { it.isNotBlank() }
                    ?: tags?.optString("stars", "")?.takeIf { it.isNotBlank() }
                val hours = tags?.optString("opening_hours", "")?.takeIf { it.isNotBlank() }

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
                        distanceKm = MosqueRepository.haversineKm(userLat, userLon, lat, lon),
                        bearingFromUserDeg = MosqueRepository.bearingDegrees(userLat, userLon, lat, lon),
                        isExplicitHalal = isExplicitHalal
                    )
                )
            }

            return deduplicateAndSort(results)
        }

        fun parseNominatimResponse(
            jsonText: String,
            userLat: Double,
            userLon: Double,
            maxDistanceKm: Double,
            isExplicitHalalQuery: Boolean
        ): List<NearbyHalalRestaurant> {
            val array = JSONArray(jsonText)
            val results = ArrayList<NearbyHalalRestaurant>(array.length())

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val category = item.optString("category", "").lowercase()
                val type = item.optString("type", "").lowercase()
                if (category == "highway" || category == "boundary" || category == "waterway") continue
                if (!isExplicitHalalQuery && category == "amenity" && type !in FOOD_AMENITY_TYPES) continue

                val lat = item.optString("lat", "").toDoubleOrNull() ?: continue
                val lon = item.optString("lon", "").toDoubleOrNull() ?: continue
                val distKm = MosqueRepository.haversineKm(userLat, userLon, lat, lon)
                if (distKm > maxDistanceKm) continue

                val rawName = item.optString("name", "").trim()
                val displayName = item.optString("display_name", "").trim()
                val name = rawName.ifBlank { displayName.substringBefore(",").trim() }
                if (name.isBlank()) continue

                val addrObj = item.optJSONObject("address")
                val address = if (addrObj != null) {
                    listOfNotNull(
                        addrObj.optString("road", "").takeIf { it.isNotBlank() },
                        addrObj.optString("neighbourhood", "").takeIf { it.isNotBlank() },
                        addrObj.optString("suburb", "").takeIf { it.isNotBlank() },
                        addrObj.optString("town", "").takeIf { it.isNotBlank() },
                        addrObj.optString("city", "").takeIf { it.isNotBlank() }
                    ).distinct().filterNot { it.equals(name, ignoreCase = true) }.take(3).joinToString(", ")
                } else {
                    displayName.split(",").map { it.trim() }.drop(1).take(3).joinToString(", ")
                }

                val isExplicitHalal = isExplicitHalalQuery ||
                    HALAL_EXPLICIT_REGEX.containsMatchIn(name) ||
                    HALAL_FRIENDLY_REGEX.containsMatchIn(name)
                val (badgeBn, badgeEn) = if (isExplicitHalal) {
                    "হালাল খাবার" to "Halal Verified"
                } else {
                    "নিকটবর্তী রেস্টুরেন্ট" to "Nearby Dining"
                }

                val cuisineLabel = when (type) {
                    "fast_food" -> "Fast Food"
                    "cafe" -> "Cafe"
                    "food_court" -> "Food Court"
                    else -> "Restaurant"
                }

                results.add(
                    NearbyHalalRestaurant(
                        id = "nom_${item.optLong("osm_id", item.optLong("place_id", i.toLong()))}",
                        nameBn = name,
                        nameEn = name,
                        address = address,
                        cuisine = cuisineLabel,
                        halalBadgeBn = badgeBn,
                        halalBadgeEn = badgeEn,
                        rating = null,
                        openingHours = null,
                        latitude = lat,
                        longitude = lon,
                        distanceKm = distKm,
                        bearingFromUserDeg = MosqueRepository.bearingDegrees(userLat, userLon, lat, lon),
                        isExplicitHalal = isExplicitHalal
                    )
                )
            }

            return deduplicateAndSort(results)
        }

        /**
         * Sorts explicit Halal places FIRST, then by nearest distance, and removes duplicates.
         */
        private fun deduplicateAndSort(places: List<NearbyHalalRestaurant>): List<NearbyHalalRestaurant> {
            val seen = HashSet<String>()
            val unique = ArrayList<NearbyHalalRestaurant>(places.size)
            val sorted = places.sortedWith(
                compareByDescending<NearbyHalalRestaurant> { it.isExplicitHalal }
                    .thenBy { it.distanceKm }
            )
            for (p in sorted) {
                val key = "${p.nameEn.lowercase()}_${(p.latitude * 500).toInt()}_${(p.longitude * 500).toInt()}"
                if (seen.add(key)) {
                    unique.add(p)
                }
            }
            return unique
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
