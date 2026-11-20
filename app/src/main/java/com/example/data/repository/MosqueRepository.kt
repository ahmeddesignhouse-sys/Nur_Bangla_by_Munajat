package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.util.LocationHelper
import com.example.util.PrayerTimeCalculator
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
    data class Success(
        val mosques: List<NearbyMosque>,
        val requestedRadiusMeters: Int = 5000,
        val effectiveRadiusMeters: Int = requestedRadiusMeters,
        val fromCache: Boolean = false
    ) : MosqueSearchResult() {
        val wasExpanded: Boolean
            get() = effectiveRadiusMeters > requestedRadiusMeters
    }
    data class Error(val messageBn: String, val messageEn: String) : MosqueSearchResult()
}

/**
 * Fast, reliable nearby mosque discovery via OpenStreetMap Overpass API:
 * - Simple, proven `around:` query for Muslim places of worship (node & way).
 * - 20-second timeout with ONE retry only (never an endless spinner).
 * - Automatic radius expansion (2 km -> 5 km -> 10 km -> 20 km) when a step truly returns 0 results.
 * - 10-minute in-memory result cache & cooperative coroutine cancellation on screen exit.
 * - Specific Bangla/English errors for offline, busy/timeout, and truly empty area.
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
        radiusMeters: Int = 5000,
        forceRefresh: Boolean = false
    ): MosqueSearchResult = withContext(Dispatchers.IO) {
        if (!LocationHelper.isValidCoordinate(userLat, userLon)) {
            return@withContext MosqueSearchResult.Error(
                messageBn = "সঠিক জিপিএস লোকেশন পাওয়া যায়নি। লোকেশন চালু করে আবার চেষ্টা করুন।",
                messageEn = "Valid GPS location unavailable. Please enable Location and retry."
            )
        }

        if (!isOnline()) {
            return@withContext MosqueSearchResult.Offline
        }

        val cacheKey = buildCacheKey(userLat, userLon, radiusMeters)
        if (!forceRefresh) {
            getCachedResult(cacheKey)?.let { cached ->
                runCatching { Log.d(TAG, "Returning cached mosques (${cached.mosques.size}) for $cacheKey") }
                return@withContext cached.copy(fromCache = true)
            }
        }

        val radiusLadder = buildRadiusLadder(radiusMeters)
        var anyServerResponded = false
        var lastException: Exception? = null
        var lastHttpCode: Int? = null

        for (stepRadius in radiusLadder) {
            currentCoroutineContext().ensureActive()

            val query = buildOverpassQuery(userLat, userLon, stepRadius)
            val outcome = executeOverpassWithOneRetry(query)

            when (outcome) {
                is OverpassCallOutcome.Success -> {
                    anyServerResponded = true
                    val maxKm = stepRadius / 1000.0
                    val mosques = parseOverpassResponse(outcome.body, userLat, userLon)
                        .filter { it.distanceKm <= maxKm + 0.15 }
                    runCatching {
                        Log.d(TAG, "Overpass mosques at ${stepRadius}m returned ${mosques.size} results")
                    }
                    if (mosques.isNotEmpty()) {
                        val success = MosqueSearchResult.Success(
                            mosques = mosques,
                            requestedRadiusMeters = radiusMeters,
                            effectiveRadiusMeters = stepRadius,
                            fromCache = false
                        )
                        putCachedResult(cacheKey, success)
                        return@withContext success
                    }
                    // Truly 0 mosques in this radius -> auto-expand to next radius step
                }
                is OverpassCallOutcome.Failure -> {
                    lastException = outcome.exception
                    lastHttpCode = outcome.httpCode
                    // Server failed/timed out even after 1 retry -> stop immediately, do NOT loop radii
                    break
                }
            }
        }

        if (anyServerResponded) {
            return@withContext MosqueSearchResult.Success(
                mosques = emptyList(),
                requestedRadiusMeters = radiusMeters,
                effectiveRadiusMeters = radiusLadder.last()
            )
        }

        if (!isOnline() || lastException is UnknownHostException) {
            MosqueSearchResult.Offline
        } else {
            buildSpecificError(lastException, lastHttpCode)
        }
    }

    private sealed class OverpassCallOutcome {
        data class Success(val body: String) : OverpassCallOutcome()
        data class Failure(val exception: Exception?, val httpCode: Int?) : OverpassCallOutcome()
    }

    private suspend fun executeOverpassWithOneRetry(query: String): OverpassCallOutcome {
        var lastException: Exception? = null
        var lastHttpCode: Int? = null

        // Primary attempt + ONE retry only (2 endpoints max)
        for ((index, endpoint) in OVERPASS_ENDPOINTS.withIndex()) {
            currentCoroutineContext().ensureActive()
            val fullLoggedUrl = "$endpoint?data=${URLEncoder.encode(query, "UTF-8")}"
            val startedMs = System.currentTimeMillis()
            runCatching {
                Log.d(TAG, "Overpass mosque attempt #${index + 1} URL: $fullLoggedUrl")
            }
            try {
                val body = postOverpassQueryCancellable(endpoint, query)
                runCatching {
                    val elapsed = System.currentTimeMillis() - startedMs
                    Log.d(TAG, "Overpass mosque attempt #${index + 1} succeeded in ${elapsed}ms (${body.length} chars)")
                }
                return OverpassCallOutcome.Success(body)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: HttpStatusException) {
                lastHttpCode = e.statusCode
                lastException = e
                runCatching { Log.w(TAG, "Overpass mosque attempt #${index + 1} HTTP ${e.statusCode}") }
            } catch (e: Exception) {
                lastException = e
                runCatching { Log.w(TAG, "Overpass mosque attempt #${index + 1} failed: ${e.javaClass.simpleName}: ${e.message}") }
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
    ): MosqueSearchResult.Error {
        return when {
            lastException is SocketTimeoutException ||
                lastHttpCode == 429 ||
                lastHttpCode == 503 ||
                lastHttpCode == 504 -> MosqueSearchResult.Error(
                messageBn = "সার্ভার ব্যস্ত, আবার চেষ্টা করুন",
                messageEn = "Server busy, please try again"
            )
            else -> MosqueSearchResult.Error(
                messageBn = "সার্ভার ব্যস্ত, আবার চেষ্টা করুন",
                messageEn = "Server busy, please try again"
            )
        }
    }

    internal class HttpStatusException(val statusCode: Int) : IllegalStateException("HTTP $statusCode")

    private data class CacheEntry(
        val savedAtMs: Long,
        val result: MosqueSearchResult.Success
    )

    companion object {
        private const val TAG = "MosqueRepository"
        private const val USER_AGENT = "NurBanglaAndroid/1.7 (com.noorbangla.islam)"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
        const val CACHE_TTL_MS = 10 * 60 * 1000L

        val RADIUS_STEPS_METERS = listOf(2000, 5000, 10000, 20000)

        // Primary + ONE retry endpoint only
        val OVERPASS_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )

        private val resultCache = ConcurrentHashMap<String, CacheEntry>()

        internal fun buildCacheKey(lat: Double, lon: Double, radiusMeters: Int): String {
            val latGrid = (lat * 500).toInt()
            val lonGrid = (lon * 500).toInt()
            return "m_${latGrid}_${lonGrid}_$radiusMeters"
        }

        internal fun getCachedResult(key: String, nowMs: Long = System.currentTimeMillis()): MosqueSearchResult.Success? {
            val entry = resultCache[key] ?: return null
            if (nowMs - entry.savedAtMs > CACHE_TTL_MS) {
                resultCache.remove(key)
                return null
            }
            return entry.result
        }

        internal fun putCachedResult(key: String, result: MosqueSearchResult.Success, nowMs: Long = System.currentTimeMillis()) {
            resultCache[key] = CacheEntry(savedAtMs = nowMs, result = result)
        }

        fun clearCache() {
            resultCache.clear()
        }

        fun buildRadiusLadder(requestedRadiusMeters: Int): List<Int> {
            val sanitized = requestedRadiusMeters.coerceIn(500, 20000)
            return (listOf(sanitized) + RADIUS_STEPS_METERS.filter { it > sanitized }).distinct()
        }

        /**
         * Simple, proven Overpass QL query using `around:radius,lat,lon` for Muslim places of worship.
         */
        fun buildOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
            val latStr = String.format(Locale.US, "%.5f", lat)
            val lonStr = String.format(Locale.US, "%.5f", lon)
            val safeRadius = radiusMeters.coerceIn(500, 20000)
            return """
                [out:json][timeout:20];
                (
                  node["amenity"="place_of_worship"]["religion"="muslim"](around:$safeRadius,$latStr,$lonStr);
                  way["amenity"="place_of_worship"]["religion"="muslim"](around:$safeRadius,$latStr,$lonStr);
                );
                out center 60;
            """.trimIndent()
        }

        fun parseOverpassResponse(
            jsonText: String,
            userLat: Double,
            userLon: Double
        ): List<NearbyMosque> {
            val root = JSONObject(jsonText)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            val results = ArrayList<NearbyMosque>(elements.length())

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

                results.add(
                    NearbyMosque(
                        id = "${el.optString("type", "n")}_${el.optLong("id", i.toLong())}",
                        nameBn = nameBn,
                        nameEn = nameEn,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        distanceKm = haversineKm(userLat, userLon, lat, lon),
                        bearingFromUserDeg = bearingDegrees(userLat, userLon, lat, lon),
                        qiblaBearingDeg = PrayerTimeCalculator.calculateQiblaBearing(lat, lon)
                    )
                )
            }

            return deduplicateAndSort(results)
        }

        fun parseNominatimResponse(
            jsonText: String,
            userLat: Double,
            userLon: Double,
            maxDistanceKm: Double
        ): List<NearbyMosque> {
            val array = JSONArray(jsonText)
            val results = ArrayList<NearbyMosque>(array.length())

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val category = item.optString("category", "").lowercase()
                val type = item.optString("type", "").lowercase()
                if (category in EXCLUDED_OSM_CATEGORIES || type in EXCLUDED_OSM_TYPES) continue

                val lat = item.optString("lat", "").toDoubleOrNull() ?: continue
                val lon = item.optString("lon", "").toDoubleOrNull() ?: continue
                val distKm = haversineKm(userLat, userLon, lat, lon)
                if (distKm > maxDistanceKm) continue

                val rawName = item.optString("name", "").trim()
                val displayName = item.optString("display_name", "").trim()
                val fallbackName = displayName.substringBefore(",").trim()
                val name = rawName.ifBlank { fallbackName }.ifBlank { "Jame Mosque" }

                val addrObj = item.optJSONObject("address")
                val address = buildShortNominatimAddress(addrObj, displayName, name)

                results.add(
                    NearbyMosque(
                        id = "nom_${item.optLong("osm_id", item.optLong("place_id", i.toLong()))}",
                        nameBn = name,
                        nameEn = name,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        distanceKm = distKm,
                        bearingFromUserDeg = bearingDegrees(userLat, userLon, lat, lon),
                        qiblaBearingDeg = PrayerTimeCalculator.calculateQiblaBearing(lat, lon)
                    )
                )
            }

            return deduplicateAndSort(results)
        }

        private fun buildShortNominatimAddress(
            addrObj: JSONObject?,
            displayName: String,
            placeName: String
        ): String {
            if (addrObj != null) {
                val parts = listOfNotNull(
                    addrObj.optString("road", "").takeIf { it.isNotBlank() },
                    addrObj.optString("neighbourhood", "").takeIf { it.isNotBlank() },
                    addrObj.optString("suburb", "").takeIf { it.isNotBlank() },
                    addrObj.optString("city_district", "").takeIf { it.isNotBlank() },
                    addrObj.optString("town", "").takeIf { it.isNotBlank() },
                    addrObj.optString("city", "").takeIf { it.isNotBlank() },
                    addrObj.optString("state_district", "").takeIf { it.isNotBlank() }
                ).distinct().filterNot { it.equals(placeName, ignoreCase = true) }
                if (parts.isNotEmpty()) return parts.take(3).joinToString(", ")
            }
            return displayName
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(placeName, ignoreCase = true) }
                .take(3)
                .joinToString(", ")
        }

        private fun deduplicateAndSort(mosques: List<NearbyMosque>): List<NearbyMosque> {
            val seen = HashSet<String>()
            val unique = ArrayList<NearbyMosque>(mosques.size)
            for (m in mosques.sortedBy { it.distanceKm }) {
                val key = "${m.nameEn.lowercase()}_${(m.latitude * 500).toInt()}_${(m.longitude * 500).toInt()}"
                if (seen.add(key)) {
                    unique.add(m)
                }
            }
            return unique
        }

        private val EXCLUDED_OSM_CATEGORIES = setOf("highway", "boundary", "waterway", "railway", "landuse")
        private val EXCLUDED_OSM_TYPES = setOf("residential", "secondary", "tertiary", "primary", "unclassified", "bus_stop", "road", "path")

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
