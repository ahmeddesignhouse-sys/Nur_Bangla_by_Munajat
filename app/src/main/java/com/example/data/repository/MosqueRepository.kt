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
        val answeredMirror: String = "",
        val fromCache: Boolean = false
    ) : MosqueSearchResult() {
        val wasExpanded: Boolean
            get() = effectiveRadiusMeters > requestedRadiusMeters
    }
    data class Error(val messageBn: String, val messageEn: String) : MosqueSearchResult()
}

/**
 * 100% FREE nearby mosque discovery using OpenStreetMap Overpass API:
 * - Tries 3 Overpass mirrors in order (1 primary + 2 retries total) via HTTP POST body (`data=...`).
 * - 30-second read timeout per server, 25-second Overpass query timeout.
 * - Logs and returns which mirror answered.
 * - Auto-expands radius (2 km -> 5 km -> 10 km -> 20 km) if a step truly returns 0 mosques.
 * - 10-minute in-memory cache, Dispatchers.IO execution, cooperative cancellation on screen exit.
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
                runCatching {
                    Log.i(TAG, "Cache hit: ${cached.mosques.size} mosques (mirror=${cached.answeredMirror})")
                }
                return@withContext cached.copy(fromCache = true)
            }
        }

        val radiusLadder = buildRadiusLadder(radiusMeters)
        var anyMirrorAnswered = false
        var lastAnsweredMirror = ""
        var lastException: Exception? = null

        for (stepRadius in radiusLadder) {
            currentCoroutineContext().ensureActive()

            val query = buildOverpassQuery(userLat, userLon, stepRadius)
            val outcome = executeOverpassAcrossMirrors(query)

            when (outcome) {
                is OverpassCallOutcome.Success -> {
                    anyMirrorAnswered = true
                    lastAnsweredMirror = outcome.mirrorHost
                    val maxKm = stepRadius / 1000.0
                    val mosques = parseOverpassResponse(outcome.body, userLat, userLon)
                        .filter { it.distanceKm <= maxKm + 0.2 }
                    runCatching {
                        Log.i(TAG, "Mosque Overpass [${outcome.mirrorHost}] radius=${stepRadius}m -> ${mosques.size} results")
                    }
                    if (mosques.isNotEmpty()) {
                        val success = MosqueSearchResult.Success(
                            mosques = mosques,
                            requestedRadiusMeters = radiusMeters,
                            effectiveRadiusMeters = stepRadius,
                            answeredMirror = outcome.mirrorHost,
                            fromCache = false
                        )
                        putCachedResult(cacheKey, success)
                        return@withContext success
                    }
                    // Truly 0 mosques in this radius -> auto-expand to next radius step
                }
                is OverpassCallOutcome.AllMirrorsFailed -> {
                    lastException = outcome.lastException
                    // All 3 mirrors failed -> stop immediately, do NOT loop radii
                    break
                }
            }
        }

        if (anyMirrorAnswered) {
            return@withContext MosqueSearchResult.Success(
                mosques = emptyList(),
                requestedRadiusMeters = radiusMeters,
                effectiveRadiusMeters = radiusLadder.last(),
                answeredMirror = lastAnsweredMirror
            )
        }

        if (!isOnline() || lastException is UnknownHostException) {
            MosqueSearchResult.Offline
        } else {
            MosqueSearchResult.Error(
                messageBn = "তিনটি সার্ভারই ব্যস্ত, ১ মিনিট পর আবার চেষ্টা করুন",
                messageEn = "All 3 servers are busy, please try again in 1 minute"
            )
        }
    }

    internal sealed class OverpassCallOutcome {
        data class Success(val body: String, val mirrorHost: String, val elapsedMs: Long) : OverpassCallOutcome()
        data class AllMirrorsFailed(val lastException: Exception?) : OverpassCallOutcome()
    }

    internal suspend fun executeOverpassAcrossMirrors(query: String): OverpassCallOutcome {
        var lastException: Exception? = null

        // Try all 3 mirrors IN ORDER (1 initial + 2 retries total)
        for ((index, endpoint) in OVERPASS_MIRRORS.withIndex()) {
            currentCoroutineContext().ensureActive()
            val mirrorHost = runCatching { URL(endpoint).host }.getOrDefault(endpoint)
            val startedMs = System.currentTimeMillis()
            runCatching {
                Log.d(TAG, "Overpass mosque POST attempt #${index + 1}/3 -> $endpoint")
            }
            try {
                val body = postOverpassQueryCancellable(endpoint, query)
                validateOverpassJsonBody(body)
                val elapsedMs = System.currentTimeMillis() - startedMs
                runCatching {
                    Log.i(TAG, "Overpass mosque mirror answered: $mirrorHost in ${elapsedMs}ms (${body.length} chars)")
                }
                return OverpassCallOutcome.Success(body = body, mirrorHost = mirrorHost, elapsedMs = elapsedMs)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                lastException = e
                val elapsedMs = System.currentTimeMillis() - startedMs
                runCatching {
                    Log.w(TAG, "Overpass mosque mirror $mirrorHost failed in ${elapsedMs}ms: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        return OverpassCallOutcome.AllMirrorsFailed(lastException)
    }

    private suspend fun postOverpassQueryCancellable(endpointUrl: String, query: String): String {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
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

    internal class HttpStatusException(val statusCode: Int) : IllegalStateException("HTTP $statusCode")

    private data class CacheEntry(
        val savedAtMs: Long,
        val result: MosqueSearchResult.Success
    )

    companion object {
        private const val TAG = "MosqueRepository"
        private const val USER_AGENT = "NurBanglaAndroid/1.7 (com.noorbangla.islam; OpenStreetMap Overpass client)"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 30_000
        const val CACHE_TTL_MS = 10 * 60 * 1000L

        val RADIUS_STEPS_METERS = listOf(2000, 5000, 10000, 20000)

        // Three FREE Overpass mirrors tried strictly in order (1 primary + 2 retries)
        val OVERPASS_MIRRORS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
        )

        private val resultCache = ConcurrentHashMap<String, CacheEntry>()

        internal fun validateOverpassJsonBody(body: String) {
            val trimmed = body.trim()
            if (!trimmed.startsWith("{")) {
                throw IllegalStateException("Overpass returned non-JSON body")
            }
            val root = JSONObject(trimmed)
            val elements = root.optJSONArray("elements")
                ?: throw IllegalStateException("Overpass JSON missing elements array")
            val remark = root.optString("remark", "").trim()
            // Overpass can return HTTP 200 with empty elements + runtime error remark when overloaded
            if (elements.length() == 0 && remark.isNotBlank() &&
                (remark.contains("timeout", ignoreCase = true) ||
                    remark.contains("error", ignoreCase = true) ||
                    remark.contains("memory", ignoreCase = true))
            ) {
                throw IllegalStateException("Overpass remark error: $remark")
            }
        }

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
         * Simple, proven Overpass QL query for Muslim places of worship (node & way).
         */
        fun buildOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
            val latStr = String.format(Locale.US, "%.5f", lat)
            val lonStr = String.format(Locale.US, "%.5f", lon)
            val safeRadius = radiusMeters.coerceIn(500, 20000)
            return "[out:json][timeout:25]; (node[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$safeRadius,$latStr,$lonStr); way[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$safeRadius,$latStr,$lonStr);); out center 50;"
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
