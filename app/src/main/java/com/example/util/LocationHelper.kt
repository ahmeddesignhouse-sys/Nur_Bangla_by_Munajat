package com.example.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {
    fun fetchCurrentGpsLocation(
        context: Context,
        scope: CoroutineScope,
        isEnglish: Boolean,
        showToast: Boolean,
        onResolved: (Double, Double, String?, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        scope.launch {
                            val place = reverseGeocodeCityCountry(
                                context, location.latitude, location.longitude, isEnglish
                            )
                            onResolved(location.latitude, location.longitude, place, showToast)
                        }
                    } else {
                        client.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                scope.launch {
                                    val place = reverseGeocodeCityCountry(
                                        context, last.latitude, last.longitude, isEnglish
                                    )
                                    onResolved(last.latitude, last.longitude, place, showToast)
                                }
                            } else if (showToast) {
                                onError(
                                    tr(
                                        isEnglish,
                                        "জিপিএস লোকেশন পাওয়া যায়নি; জেলা তালিকা থেকে বেছে নিন।",
                                        "GPS location unavailable; please pick a district manually."
                                    )
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (showToast) {
                        onError(
                            tr(
                                isEnglish,
                                "জিপিএস লোকেশন নিতে সমস্যা হয়েছে।",
                                "Failed to get GPS location."
                            )
                        )
                    }
                }
        } catch (_: SecurityException) {
            if (showToast) {
                onError(
                    tr(
                        isEnglish,
                        "লোকেশন অনুমতি প্রয়োজন।",
                        "Location permission is required."
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
                    withTimeoutOrNull(4000L) {
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
