package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.BundledPlacesCatalog
import com.example.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
        val answeredMirror: String = "",
        val fromCache: Boolean = false
    ) : HalalSearchResult() {
        val wasExpanded: Boolean
            get() = effectiveRadiusMeters > requestedRadiusMeters
    }

    /**
     * Returned when every online Overpass mirror + Nominatim failed within the 15s hard cap.
     * Always provides the curated local Dhaka/Narayanganj Halal restaurants list (Halal-first,
     * sorted by distance) PLUS the exact diagnostic log.
     */
    data class FallbackBundled(
        val restaurants: List<NearbyHalalRestaurant>,
        val diagnostics: List<SourceAttemptDiagnostic>,
        val timedOutOverall: Boolean = false
    ) : HalalSearchResult()

    data class Error(
        val messageBn: String,
        val messageEn: String,
        val diagnostics: List<SourceAttemptDiagnostic> = emptyList()
    ) : HalalSearchResult()
}

/**
 * 100% FREE nearby Halal restaurant discovery using OpenStreetMap Overpass API (5 mirrors)
 * + Nominatim fallback + curated offline Dhaka/Narayanganj fallback. Zero API keys, zero billing, hard 15s cap.
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
                runCatching {
                    Log.i(TAG, "Cache hit: ${cached.restaurants.size} restaurants (mirror=${cached.answeredMirror})")
                }
                return@withContext cached.copy(fromCache = true)
            }
        }

        val radiusLadder = buildRadiusLadder(radiusMeters)
        val startedAllMs = System.currentTimeMillis()
        var anyOnlineAnswered = false
        var lastAnsweredMirror = ""
        var lastDiagnostics: List<SourceAttemptDiagnostic> = emptyList()
        var timedOutOverall = false

        for (stepRadius in radiusLadder) {
            currentCoroutineContext().ensureActive()
            val remainingMs = FreePlacesNetworkEngine.TOTAL_SEARCH_DEADLINE_MS - (System.currentTimeMillis() - startedAllMs)
            if (remainingMs < 1_000L) {
                timedOutOverall = true
                break
            }

            val maxKm = stepRadius / 1000.0
            val query = buildOverpassQuery(userLat, userLon, stepRadius)
            val outcome = FreePlacesNetworkEngine.fetchOverpassThenNominatim(
                overpassQuery = query,
                nominatimQuery = "restaurant",
                lat = userLat,
                lon = userLon,
                radiusMeters = stepRadius,
                totalDeadlineMs = remainingMs
            )

            when (outcome) {
                is FreePlacesFetchOutcome.OverpassSuccess -> {
                    anyOnlineAnswered = true
                    lastAnsweredMirror = outcome.mirrorHost
                    val results = parseOverpassResponse(outcome.body, userLat, userLon)
                        .filter { it.distanceKm <= maxKm + 0.2 }
                    runCatching {
                        Log.i(TAG, "Halal Overpass [${outcome.mirrorHost}] radius=${stepRadius}m -> ${results.size} results")
                    }
                    if (results.isNotEmpty()) {
                        val success = HalalSearchResult.Success(
                            restaurants = results,
                            requestedRadiusMeters = radiusMeters,
                            effectiveRadiusMeters = stepRadius,
                            answeredMirror = outcome.mirrorHost,
                            fromCache = false
                        )
                        putCachedResult(cacheKey, success)
                        return@withContext success
                    }
                    // Truly 0 restaurants in this radius -> auto-expand to next radius step
                }

                is FreePlacesFetchOutcome.NominatimSuccess -> {
                    anyOnlineAnswered = true
                    lastAnsweredMirror = "nominatim.openstreetmap.org"
                    val results = parseNominatimResponse(
                        jsonText = outcome.body,
                        userLat = userLat,
                        userLon = userLon,
                        maxDistanceKm = maxKm + 0.5,
                        isExplicitHalalQuery = false
                    )
                    if (results.isNotEmpty()) {
                        val success = HalalSearchResult.Success(
                            restaurants = results,
                            requestedRadiusMeters = radiusMeters,
                            effectiveRadiusMeters = stepRadius,
                            answeredMirror = lastAnsweredMirror,
                            fromCache = false
                        )
                        putCachedResult(cacheKey, success)
                        return@withContext success
                    }
                }

                is FreePlacesFetchOutcome.AllFailed -> {
                    lastDiagnostics = outcome.diagnostics
                    timedOutOverall = outcome.timedOutOverall
                    // All online sources failed -> stop looping radii and serve bundled list
                    break
                }
            }
        }

        if (anyOnlineAnswered) {
            return@withContext HalalSearchResult.Success(
                restaurants = emptyList(),
                requestedRadiusMeters = radiusMeters,
                effectiveRadiusMeters = radiusLadder.last(),
                answeredMirror = lastAnsweredMirror
            )
        }

        if (!isOnline()) {
            return@withContext HalalSearchResult.Offline
        }

        HalalSearchResult.FallbackBundled(
            restaurants = BundledPlacesCatalog.halalRestaurantsSorted(userLat, userLon),
            diagnostics = lastDiagnostics,
            timedOutOverall = timedOutOverall
        )
    }

    internal sealed class OverpassCallOutcome {
        data class Success(val body: String, val mirrorHost: String, val elapsedMs: Long) : OverpassCallOutcome()
        data class AllMirrorsFailed(
            val lastException: Exception?,
            val diagnostics: List<SourceAttemptDiagnostic> = emptyList()
        ) : OverpassCallOutcome()
    }

    internal suspend fun executeOverpassAcrossMirrors(
        query: String,
        lat: Double = 23.6238,
        lon: Double = 90.5000,
        radiusMeters: Int = 5000
    ): OverpassCallOutcome {
        return when (
            val outcome = FreePlacesNetworkEngine.fetchOverpassThenNominatim(
                overpassQuery = query,
                nominatimQuery = "restaurant",
                lat = lat,
                lon = lon,
                radiusMeters = radiusMeters
            )
        ) {
            is FreePlacesFetchOutcome.OverpassSuccess -> OverpassCallOutcome.Success(
                body = outcome.body,
                mirrorHost = outcome.mirrorHost,
                elapsedMs = outcome.elapsedMs
            )
            is FreePlacesFetchOutcome.NominatimSuccess -> OverpassCallOutcome.Success(
                body = outcome.body,
                mirrorHost = "nominatim.openstreetmap.org",
                elapsedMs = outcome.elapsedMs
            )
            is FreePlacesFetchOutcome.AllFailed -> OverpassCallOutcome.AllMirrorsFailed(
                lastException = outcome.lastException,
                diagnostics = outcome.diagnostics
            )
        }
    }

    private data class CacheEntry(
        val savedAtMs: Long,
        val result: HalalSearchResult.Success
    )

    companion object {
        private const val TAG = "HalalRestaurantRepo"
        const val CACHE_TTL_MS = 10 * 60 * 1000L

        // Five FREE Overpass mirrors tried in priority order
        val OVERPASS_MIRRORS: List<String> = FreePlacesNetworkEngine.OVERPASS_MIRRORS

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

        fun buildOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
            val latStr = String.format(Locale.US, "%.5f", lat)
            val lonStr = String.format(Locale.US, "%.5f", lon)
            val safeRadius = radiusMeters.coerceIn(500, 20000)
            return "[out:json][timeout:25]; (node[\"amenity\"~\"restaurant|cafe|fast_food\"](around:$safeRadius,$latStr,$lonStr); way[\"amenity\"~\"restaurant|cafe|fast_food\"](around:$safeRadius,$latStr,$lonStr);); out center 50;"
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
