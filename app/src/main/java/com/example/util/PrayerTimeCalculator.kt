package com.example.util

import com.example.data.model.AsrMethod
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.CalculationMethod
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.ForbiddenTimeWindow
import com.example.data.model.NextWaqtInfo
import com.example.data.model.WaqtType
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Universal Astronomical Prayer Time Calculator calibrated against Islamic Foundation Bangladesh
 * (Dhaka perpetual table) and automatically adapting worldwide via GPS & timezone resolution.
 */
object PrayerTimeCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206
    private const val SUNRISE_SUNSET_ANGLE = 0.8333

    fun isInsideBangladesh(latitude: Double, longitude: Double): Boolean {
        return latitude in 20.3..26.7 && longitude in 88.0..92.7
    }

    /**
     * Resolves the correct TimeZone for a coordinate pair:
     * - Manual Bangladesh district or coordinates inside Bangladesh -> Asia/Dhaka (GMT+6)
     * - Worldwide GPS -> uses device TimeZone if its offset matches the solar longitude (within 2.5h,
     *   preserving DST & half-hour offsets like India +5:30), otherwise resolves regional timezone.
     */
    fun resolveTimeZone(
        latitude: Double,
        longitude: Double,
        isManualDistrict: Boolean,
        atMillis: Long = System.currentTimeMillis()
    ): TimeZone {
        if (isManualDistrict || isInsideBangladesh(latitude, longitude)) {
            return TimeZone.getTimeZone("Asia/Dhaka")
        }

        // Unambiguous regional IANA timezones (preserve exact local offsets & IANA DST rules even if device clock lags)
        val regionalTzId: String? = when {
            latitude in 23.6..37.1 && longitude in 60.8..74.6 -> "Asia/Karachi"
            latitude in 6.0..37.0 && longitude in 68.0..97.5 -> "Asia/Kolkata"
            latitude in 22.5..26.5 && longitude in 51.4..56.5 -> "Asia/Dubai"
            latitude in 12.0..33.0 && longitude in 34.0..56.5 -> "Asia/Riyadh"
            latitude in 1.1..1.5 && longitude in 103.6..104.1 -> "Asia/Singapore"
            latitude in 1.0..7.5 && longitude in 99.5..119.5 -> "Asia/Kuala_Lumpur"
            latitude in -11.0..6.0 && longitude in 95.0..115.0 -> "Asia/Jakarta"
            latitude in 30.0..46.0 && longitude in 129.0..146.0 -> "Asia/Tokyo"
            latitude in -44.0..-10.0 && longitude in 140.0..154.0 -> "Australia/Sydney"
            latitude in 36.0..42.5 && longitude in 26.0..45.0 -> "Europe/Istanbul"
            latitude in 49.0..60.0 && longitude in -10.5..2.0 -> "Europe/London"
            latitude in 36.0..60.0 && longitude in 2.0..24.0 -> "Europe/Berlin"
            latitude in 24.0..60.0 && longitude in -141.0..-52.0 -> when {
                longitude > -85.0 -> "America/New_York"
                longitude > -102.0 -> "America/Chicago"
                longitude > -115.0 -> "America/Denver"
                else -> "America/Los_Angeles"
            }
            else -> null
        }
        if (regionalTzId != null) {
            return TimeZone.getTimeZone(regionalTzId)
        }

        val deviceTz = TimeZone.getDefault()
        val deviceOffsetHours = deviceTz.getOffset(atMillis) / 3_600_000.0
        val solarOffsetHours = longitude / 15.0
        if (abs(deviceOffsetHours - solarOffsetHours) <= 2.0) {
            return deviceTz
        }

        val approxHours = (longitude / 15.0).roundToInt().coerceIn(-12, 14)
        val sign = if (approxHours >= 0) "+" else "-"
        return TimeZone.getTimeZone("GMT$sign${abs(approxHours)}")
    }

    fun formatTimeZoneLabel(tz: TimeZone, atMillis: Long = System.currentTimeMillis()): String {
        val offsetMins = tz.getOffset(atMillis) / 60_000
        val sign = if (offsetMins >= 0) "+" else "-"
        val absMins = abs(offsetMins)
        val hours = absMins / 60
        val mins = absMins % 60
        val raw = if (mins == 0) "GMT$sign$hours" else String.format("GMT%s%d:%02d", sign, hours, mins)
        return BanglaUtils.toBanglaDigits(raw)
    }

    fun resolveCalculationMethod(
        latitude: Double,
        longitude: Double,
        preferred: CalculationMethod
    ): CalculationMethod {
        if (preferred != CalculationMethod.AUTO) return preferred
        return when {
            // Bangladesh, India, Pakistan, South Asia
            latitude in 5.0..37.0 && longitude in 60.0..98.0 -> CalculationMethod.BANGLADESH_KARACHI
            // Saudi Arabia / Gulf
            latitude in 12.0..32.0 && longitude in 34.0..60.0 -> CalculationMethod.UMM_AL_QURA
            // North America
            longitude < -50.0 -> CalculationMethod.ISNA
            // Egypt & Africa
            latitude in -35.0..32.0 && longitude in -18.0..35.0 -> CalculationMethod.EGYPT
            // Europe & Rest of World
            else -> CalculationMethod.MWL
        }
    }

    fun calculateForDate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        asrMethod: AsrMethod,
        timeZoneHours: Double,
        calculationMethod: CalculationMethod = CalculationMethod.AUTO,
        timeZoneLabel: String = "GMT+6"
    ): DailyPrayerSchedule {
        val effectiveMethod = resolveCalculationMethod(latitude, longitude, calculationMethod)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val (decl, eqT) = sunPosition(jd + 0.5)

        val transit = 12.0 + timeZoneHours - longitude / 15.0 - eqT
        val sunriseHours = transit - hourAngle(latitude, decl, SUNRISE_SUNSET_ANGLE)
        val sunsetHours = transit + hourAngle(latitude, decl, SUNRISE_SUNSET_ANGLE)
        val fajrHours = transit - hourAngle(latitude, decl, effectiveMethod.fajrAngle)

        val asrShadow = asrMethod.shadowFactor.toDouble()
        val asrAngle = -Math.toDegrees(atan(1.0 / (asrShadow + tan(Math.toRadians(abs(latitude - decl))))))
        val asrHours = transit + hourAngle(latitude, decl, asrAngle)

        val sunriseMin = toMinutes(sunriseHours)
        val sunsetMin = toMinutes(sunsetHours)
        val solarNoonMin = toMinutes(transit)

        val fajrMin = toMinutes(fajrHours)
        val dhuhrMin = solarNoonMin
        val asrMin = toMinutes(asrHours)
        // Safety buffer: +3 min after sunset for Maghrib/Iftar (matches Dhaka Islamic Foundation/Academy table)
        val maghribMin = (sunsetMin + 3) % 1440
        val ishaMin = if (effectiveMethod.ishaIntervalMinutes != null) {
            (maghribMin + effectiveMethod.ishaIntervalMinutes) % 1440
        } else {
            toMinutes(transit + hourAngle(latitude, decl, effectiveMethod.ishaAngle))
        }
        val sehriEndMin = ((fajrMin - 5) + 1440) % 1440
        val iftarMin = maghribMin

        val forbidden = listOf(
            ForbiddenTimeWindow("সূর্যোদয়ের নিষিদ্ধ সময়", sunriseMin, (sunriseMin + 15) % 1440),
            ForbiddenTimeWindow("দ্বিপ্রহর (যাওয়াল) নিষিদ্ধ সময়", ((solarNoonMin - 10) + 1440) % 1440, solarNoonMin),
            ForbiddenTimeWindow("সূর্যাস্তের নিষিদ্ধ সময়", ((sunsetMin - 15) + 1440) % 1440, sunsetMin)
        )

        return DailyPrayerSchedule(
            fajrMinute = fajrMin,
            sunriseMinute = sunriseMin,
            dhuhrMinute = dhuhrMin,
            asrMinute = asrMin,
            sunsetMinute = sunsetMin,
            maghribMinute = maghribMin,
            ishaMinute = ishaMin,
            sehriEndMinute = sehriEndMin,
            iftarMinute = iftarMin,
            forbiddenTimes = forbidden,
            timeZoneLabel = timeZoneLabel,
            appliedMethodTitleBn = effectiveMethod.titleBn
        )
    }

    fun computeNextWaqt(
        nowInTargetTz: Calendar,
        todaySchedule: DailyPrayerSchedule,
        tomorrowFajrMinute: Int
    ): NextWaqtInfo {
        val nowSeconds = nowInTargetTz.get(Calendar.HOUR_OF_DAY) * 3600L +
            nowInTargetTz.get(Calendar.MINUTE) * 60L +
            nowInTargetTz.get(Calendar.SECOND)

        val candidates = listOf(
            WaqtType.FAJR to todaySchedule.fajrMinute,
            WaqtType.DHUHR to todaySchedule.dhuhrMinute,
            WaqtType.ASR to todaySchedule.asrMinute,
            WaqtType.MAGHRIB to todaySchedule.maghribMinute,
            WaqtType.ISHA to todaySchedule.ishaMinute
        )

        for ((waqt, minute) in candidates) {
            val targetSec = minute * 60L
            if (targetSec > nowSeconds) {
                return NextWaqtInfo(
                    waqt = waqt,
                    targetMinuteOfDay = minute,
                    remainingSeconds = targetSec - nowSeconds,
                    isTomorrow = false
                )
            }
        }

        val tomorrowTargetSec = (1440L + tomorrowFajrMinute) * 60L
        return NextWaqtInfo(
            waqt = WaqtType.FAJR,
            targetMinuteOfDay = tomorrowFajrMinute,
            remainingSeconds = (tomorrowTargetSec - nowSeconds).coerceAtLeast(0L),
            isTomorrow = true
        )
    }

    fun getCurrentWaqt(nowInTargetTz: Calendar, schedule: DailyPrayerSchedule): WaqtType? {
        val minuteOfDay = nowInTargetTz.get(Calendar.HOUR_OF_DAY) * 60 + nowInTargetTz.get(Calendar.MINUTE)
        return when {
            minuteOfDay in schedule.fajrMinute until schedule.sunriseMinute -> WaqtType.FAJR
            minuteOfDay in schedule.dhuhrMinute until schedule.asrMinute -> WaqtType.DHUHR
            minuteOfDay in schedule.asrMinute until schedule.sunsetMinute -> WaqtType.ASR
            minuteOfDay in schedule.maghribMinute until schedule.ishaMinute -> WaqtType.MAGHRIB
            minuteOfDay >= schedule.ishaMinute || minuteOfDay < schedule.fajrMinute -> WaqtType.ISHA
            else -> null
        }
    }

    /**
     * Returns bearing from North (0..360 degrees clockwise) toward Kaaba.
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Float {
        val latRad = Math.toRadians(latitude)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val y = sin(dLon) * cos(kaabaLatRad)
        val x = cos(latRad) * sin(kaabaLatRad) - sin(latRad) * cos(kaabaLatRad) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return (((deg % 360.0) + 360.0) % 360.0).toFloat()
    }

    fun distanceToKaabaKm(latitude: Double, longitude: Double): Int {
        return BangladeshiDistricts
            .haversineKm(latitude, longitude, KAABA_LAT, KAABA_LON)
            .roundToInt()
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d

        val ra = Math.toDegrees(
            atan2(
                cos(Math.toRadians(e)) * sin(Math.toRadians(l)),
                cos(Math.toRadians(l))
            )
        ) / 15.0
        val eqT = q / 15.0 - fixHour(ra)
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        return decl to eqT
    }

    private fun hourAngle(latitude: Double, decl: Double, angle: Double): Double {
        val latRad = Math.toRadians(latitude)
        val declRad = Math.toRadians(decl)
        val num = -sin(Math.toRadians(angle)) - sin(latRad) * sin(declRad)
        val den = cos(latRad) * cos(declRad)
        val ratio = (num / den).coerceIn(-0.9999, 0.9999)
        return Math.toDegrees(acos(ratio)) / 15.0
    }

    private fun fixAngle(a: Double): Double = (a % 360.0 + 360.0) % 360.0
    private fun fixHour(a: Double): Double = (a % 24.0 + 24.0) % 24.0

    private fun toMinutes(hours: Double): Int {
        val total = (hours * 60.0).roundToInt()
        return ((total % 1440) + 1440) % 1440
    }
}
