package com.example.util

import android.hardware.GeomagneticField
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class CompassAccuracyLevel {
    HIGH,
    MEDIUM,
    LOW,
    UNRELIABLE;

    fun title(isEnglish: Boolean): String = when (this) {
        HIGH -> tr(isEnglish, "নির্ভুলতা: উচ্চ", "Accuracy: High")
        MEDIUM -> tr(isEnglish, "নির্ভুলতা: মাঝারি", "Accuracy: Medium")
        LOW -> tr(isEnglish, "নির্ভুলতা: কম", "Accuracy: Low")
        UNRELIABLE -> tr(isEnglish, "নির্ভুলতা: ক্যালিব্রেশন দরকার", "Accuracy: Calibration Needed")
    }

    val needsCalibration: Boolean
        get() = this == LOW || this == UNRELIABLE

    companion object {
        fun fromSensorAccuracy(accuracy: Int): CompassAccuracyLevel = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
            else -> UNRELIABLE
        }
    }
}

/**
 * Pure math & sensor-fusion helpers for the Qibla compass:
 * - Vector low-pass filtering for Accelerometer + Magnetometer sensor fusion
 * - True North magnetic declination correction via [GeomagneticField]
 * - Continuous unwrapped shortest-arc smoothing so the needle NEVER spins 358° across 0°/360°
 * - Great-circle distance to the Holy Kaaba in Makkah
 */
object QiblaCompassMath {

    const val KAABA_LAT = 21.4225
    const val KAABA_LON = 39.8262

    /**
     * Low-pass filter on a 3-axis sensor vector (accelerometer or magnetometer).
     * Tuned alpha (~0.20f) reacts within < 1s while suppressing hand tremor.
     */
    fun lowPassVector(
        input: FloatArray,
        previous: FloatArray,
        alpha: Float = 0.22f
    ): FloatArray {
        val clamped = alpha.coerceIn(0.05f, 1f)
        val out = FloatArray(3)
        for (i in 0 until 3) {
            out[i] = previous[i] + clamped * (input[i] - previous[i])
        }
        return out
    }

    fun normalizeDegrees(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    /**
     * Shortest signed angular difference from [fromDeg] to [toDeg] in [-180f, 180f].
     */
    fun shortestAngleDelta(fromDeg: Float, toDeg: Float): Float {
        return (((toDeg - fromDeg) % 360f) + 540f) % 360f - 180f
    }

    /**
     * Advances a continuous (unwrapped) angle smoothly toward [targetMod360Deg] along the
     * shortest arc. Because the output is continuous (e.g. 359° -> 361° instead of 359° -> 1°),
     * Compose rotation animations never snap backwards across North!
     */
    fun stepUnwrappedAngle(
        currentContinuousDeg: Float,
        targetMod360Deg: Float,
        smoothingAlpha: Float = 0.30f
    ): Float {
        val delta = shortestAngleDelta(normalizeDegrees(currentContinuousDeg), normalizeDegrees(targetMod360Deg))
        return currentContinuousDeg + delta * smoothingAlpha.coerceIn(0.05f, 1f)
    }

    /**
     * Computes local magnetic declination (in degrees, east-positive) using Android's
     * World Magnetic Model ([GeomagneticField]).
     * Adding declination to magnetic azimuth yields True North azimuth.
     */
    fun computeMagneticDeclinationDegrees(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Float = 0f,
        timeMillis: Long = System.currentTimeMillis()
    ): Float {
        return try {
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitudeMeters,
                timeMillis
            ).declination
        } catch (_: Exception) {
            0f
        }
    }

    /**
     * Converts magnetic North azimuth (0..360) to True North azimuth (0..360).
     */
    fun toTrueNorthAzimuth(magneticAzimuthDeg: Float, declinationDeg: Float): Float {
        return normalizeDegrees(magneticAzimuthDeg + declinationDeg)
    }

    /**
     * Great-circle distance in kilometers from the user's coordinates to the Kaaba in Makkah.
     */
    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun cardinalDirection(bearingDeg: Float, isEnglish: Boolean): String {
        val norm = normalizeDegrees(bearingDeg)
        return when {
            norm < 22.5f || norm >= 337.5f -> tr(isEnglish, "উত্তর", "North")
            norm < 67.5f -> tr(isEnglish, "উত্তর-পূর্ব", "North-East")
            norm < 112.5f -> tr(isEnglish, "পূর্ব", "East")
            norm < 157.5f -> tr(isEnglish, "দক্ষিণ-পূর্ব", "South-East")
            norm < 202.5f -> tr(isEnglish, "দক্ষিণ", "South")
            norm < 247.5f -> tr(isEnglish, "দক্ষিণ-পশ্চিম", "South-West")
            norm < 292.5f -> tr(isEnglish, "পশ্চিম", "West")
            else -> tr(isEnglish, "উত্তর-পশ্চিম", "North-West")
        }
    }

    fun formatTextDirectionSummary(
        qiblaBearingDeg: Float,
        distanceKm: Double,
        isEnglish: Boolean
    ): String {
        val degInt = normalizeDegrees(qiblaBearingDeg).toInt()
        val distInt = distanceKm.toInt()
        val dir = cardinalDirection(qiblaBearingDeg, isEnglish)
        return if (isEnglish) {
            "Kaaba direction $degInt° $dir, distance to Makkah approx. $distInt km"
        } else {
            "কাবার দিক ${BanglaUtils.toBanglaNumber(degInt)}° $dir, মক্কার দূরত্ব প্রায় ${BanglaUtils.toBanglaNumber(distInt)} কিমি"
        }
    }
}
