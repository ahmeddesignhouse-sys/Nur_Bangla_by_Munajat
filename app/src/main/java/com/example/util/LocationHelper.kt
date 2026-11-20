package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

data class ResolvedGpsPoint(
    val latitude: Double,
    val longitude: Double,
    val source: String
)

object LocationHelper {
    private const val TAG = "LocationHelper"
    private const val FRESH_FIX_MAX_AGE_MS = 5 * 60 * 1000L
    private const val LIVE_GPS_TIMEOUT_MS = 5_000L

    fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        if (lat.isNaN() || lon.isNaN()) return false
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return false
        // Reject Null Island (0.0, 0.0) which happens when an uninitialized GPS Location is returned
        if (abs(lat) < 0.0001 && abs(lon) < 0.0001) return false
        return true
    }

    /**
     * Fast, bounded GPS coordinate resolver for Nearby Mosque & Halal Finders.
     * - Never hangs indefinitely (strict 5-second cap on fresh GPS fix).
     * - Rejects (0.0, 0.0) / NaN coordinates.
     * - Immediately uses a recent Fused/LocationManager fix (< 5 min) so Overpass starts right away.
     * - Falls back gracefully to any valid cached fix or the app's already-known GPS/district coords.
     */
    @SuppressLint("MissingPermission")
    suspend fun resolveLocationFast(
        context: Context,
        fallbackLat: Double? = null,
        fallbackLon: Double? = null
    ): ResolvedGpsPoint? {
        val nowMs = System.currentTimeMillis()
        var bestCached: Location? = null

        try {
            val client = LocationServices.getFusedLocationProviderClient(context)

            // 1. Instant check: Fused lastLocation (capped at 1.5s)
            val fusedLast = withTimeoutOrNull(1_500L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    client.lastLocation
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
            }
            if (fusedLast != null && isValidCoordinate(fusedLast.latitude, fusedLast.longitude)) {
                bestCached = fusedLast
                val ageMs = abs(nowMs - fusedLast.time)
                if (fusedLast.time > 0L && ageMs <= FRESH_FIX_MAX_AGE_MS) {
                    runCatching {
                        Log.d(TAG, "GPS resolved instantly from recent fused lastLocation: ${fusedLast.latitude}, ${fusedLast.longitude}")
                    }
                    return ResolvedGpsPoint(fusedLast.latitude, fusedLast.longitude, "fused_last")
                }
            }

            // 2. Check Android LocationManager last known (GPS + Network)
            val lmBest = getBestLocationManagerLastKnown(context)
            if (lmBest != null && isValidCoordinate(lmBest.latitude, lmBest.longitude)) {
                if (bestCached == null || lmBest.time > bestCached.time) {
                    bestCached = lmBest
                }
                val ageMs = abs(nowMs - lmBest.time)
                if (lmBest.time > 0L && ageMs <= FRESH_FIX_MAX_AGE_MS) {
                    runCatching {
                        Log.d(TAG, "GPS resolved instantly from LocationManager: ${lmBest.latitude}, ${lmBest.longitude}")
                    }
                    return ResolvedGpsPoint(lmBest.latitude, lmBest.longitude, "location_manager")
                }
            }

            // 3. Request a fresh current location with strict 5-second timeout + CancellationToken
            val cts = CancellationTokenSource()
            val fresh = withTimeoutOrNull(LIVE_GPS_TIMEOUT_MS) {
                suspendCancellableCoroutine<Location?> { cont ->
                    cont.invokeOnCancellation { cts.cancel() }
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
            }
            if (fresh == null) {
                cts.cancel()
            } else if (isValidCoordinate(fresh.latitude, fresh.longitude)) {
                runCatching {
                    Log.d(TAG, "GPS resolved from fresh getCurrentLocation: ${fresh.latitude}, ${fresh.longitude}")
                }
                return ResolvedGpsPoint(fresh.latitude, fresh.longitude, "fused_current")
            }
        } catch (se: SecurityException) {
            runCatching { Log.w(TAG, "Location permission missing while resolving GPS", se) }
            return null
        } catch (e: Exception) {
            runCatching { Log.w(TAG, "Error resolving GPS location: ${e.message}") }
        }

        // 4. Fallback to any valid older cached fix from Fused / LocationManager
        if (bestCached != null && isValidCoordinate(bestCached.latitude, bestCached.longitude)) {
            runCatching {
                Log.d(TAG, "GPS fallback to older cached fix: ${bestCached.latitude}, ${bestCached.longitude}")
            }
            return ResolvedGpsPoint(bestCached.latitude, bestCached.longitude, "cached_fix")
        }

        // 5. Fallback to the app's already-known GPS/district location (e.g. Narayanganj / Dhaka)
        if (fallbackLat != null && fallbackLon != null && isValidCoordinate(fallbackLat, fallbackLon)) {
            runCatching {
                Log.d(TAG, "GPS fallback to app active location: $fallbackLat, $fallbackLon")
            }
            return ResolvedGpsPoint(fallbackLat, fallbackLon, "app_fallback")
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private fun getBestLocationManagerLastKnown(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var best: Location? = null
            for (provider in providers) {
                val loc = try {
                    lm.getLastKnownLocation(provider)
                } catch (_: Exception) {
                    null
                }
                if (loc != null && isValidCoordinate(loc.latitude, loc.longitude)) {
                    if (best == null || loc.time > best.time) {
                        best = loc
                    }
                }
            }
            best
        } catch (_: Exception) {
            null
        }
    }

    fun fetchCurrentGpsLocation(
        context: Context,
        scope: CoroutineScope,
        isEnglish: Boolean,
        showToast: Boolean,
        onResolved: (Double, Double, String?, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val resolved = resolveLocationFast(context)
            if (resolved != null) {
                val place = reverseGeocodeCityCountry(
                    context,
                    resolved.latitude,
                    resolved.longitude,
                    isEnglish
                )
                onResolved(resolved.latitude, resolved.longitude, place, showToast)
            } else {
                onError(
                    tr(
                        isEnglish,
                        "জিপিএস লোকেশন পাওয়া যায়নি; অনুগ্রহ করে লোকেশন চালু করে আবার চেষ্টা করুন।",
                        "GPS location unavailable; please ensure Location is ON and try again."
                    )
                )
            }
        }
    }

    suspend fun reverseGeocodeCityCountry(
        context: Context,
        lat: Double,
        lon: Double,
        isEnglish: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val locales = if (isEnglish) {
                listOf(Locale.ENGLISH, Locale.forLanguageTag("bn-BD"))
            } else {
                listOf(Locale.forLanguageTag("bn-BD"), Locale.ENGLISH)
            }
            for (locale in locales) {
                val geocoder = Geocoder(context, locale)
                val first: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    withTimeoutOrNull(3_000L) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(addresses.firstOrNull())
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            })
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
                if (first != null) {
                    val city = first.locality ?: first.subAdminArea ?: first.adminArea
                    val country = first.countryName
                    val joined = listOfNotNull(city, country)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .joinToString(", ")
                    if (joined.isNotBlank()) return@withContext joined
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
