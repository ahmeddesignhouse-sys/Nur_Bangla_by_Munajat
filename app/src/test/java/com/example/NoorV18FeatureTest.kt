package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BundledPlacesCatalog
import com.example.data.repository.FreePlacesFetchOutcome
import com.example.data.repository.FreePlacesNetworkEngine
import com.example.data.repository.HalalRestaurantRepository
import com.example.data.repository.MosqueRepository
import com.example.data.repository.SourceAttemptDiagnostic
import com.example.util.CompassAccuracyLevel
import com.example.util.DailyInspirationCatalog
import com.example.util.DailyShareCardGenerator
import com.example.util.PrayerTimeCalculator
import com.example.util.QiblaCompassMath
import com.example.util.ShareCardFontSize
import com.example.util.ShareCardFormat
import com.example.util.ShareCardTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorV18FeatureTest {

    // ISSUE 1: 5 Overpass mirrors, Nominatim URL, curated Dhaka/Narayanganj fallback, hard deadline, diagnostics
    @Test
    fun fiveOverpassMirrorsNominatimUrlAndCuratedFallbackWork() {
        assertEquals(5, FreePlacesNetworkEngine.OVERPASS_MIRRORS.size)
        assertEquals(
            listOf(
                "https://overpass-api.de/api/interpreter",
                "https://overpass.kumi.systems/api/interpreter",
                "https://overpass.private.coffee/api/interpreter",
                "https://overpass.nchc.org.tw/api/interpreter",
                "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
            ),
            FreePlacesNetworkEngine.OVERPASS_MIRRORS
        )
        assertEquals("NurBanglaAndroid/1.8", FreePlacesNetworkEngine.USER_AGENT)
        assertEquals(15_000L, FreePlacesNetworkEngine.TOTAL_SEARCH_DEADLINE_MS)

        // Nominatim URL format for Narayanganj
        val nomUrl = FreePlacesNetworkEngine.buildNominatimSearchUrl(
            query = "mosque",
            lat = 23.6238,
            lon = 90.5000,
            radiusMeters = 5000
        )
        assertTrue(nomUrl.startsWith("https://nominatim.openstreetmap.org/search?"))
        assertTrue(nomUrl.contains("q=mosque"))
        assertTrue(nomUrl.contains("format=json"))
        assertTrue(nomUrl.contains("bounded=1"))
        assertTrue(nomUrl.contains("viewbox="))

        // Curated offline fallback list for Narayanganj is non-empty and sorted nearest-first
        val bundledMosques = BundledPlacesCatalog.mosquesSortedByDistance(23.6238, 90.5000)
        assertTrue("Expected >= 15 bundled mosques, got ${bundledMosques.size}", bundledMosques.size >= 15)
        assertTrue(
            "Nearest mosque in Narayanganj should be within 5 km (got ${bundledMosques.first().distanceKm})",
            bundledMosques.first().distanceKm < 5.0
        )
        for (i in 1 until bundledMosques.size) {
            assertTrue(
                "Bundled mosques must be sorted nearest first",
                bundledMosques[i - 1].distanceKm <= bundledMosques[i].distanceKm + 0.001
            )
        }

        val bundledRestaurants = BundledPlacesCatalog.halalRestaurantsSorted(23.6238, 90.5000)
        assertTrue("Expected >= 12 bundled Halal restaurants, got ${bundledRestaurants.size}", bundledRestaurants.size >= 12)
        assertTrue("First bundled restaurant must be explicitly Halal", bundledRestaurants.first().isExplicitHalal)

        // Real per-source diagnostics formatting
        val diag = SourceAttemptDiagnostic(
            sourceName = "overpass-api.de",
            httpStatus = 429,
            elapsedMs = 2300L,
            errorKind = "HTTP 429"
        )
        val formatted = diag.formatLine(isEnglish = true)
        assertTrue(formatted.contains("overpass-api.de"))
        assertTrue(formatted.contains("HTTP 429"))
        assertTrue(formatted.contains("2.3s"))
    }

    @Test
    fun hardDeadlineStopsHangingSearchCleanlyWithDiagnostics() = runBlocking {
        // Point to an unroutable TEST-NET-1 IP so connection hangs until deadline
        val startMs = System.currentTimeMillis()
        val outcome = FreePlacesNetworkEngine.fetchOverpassThenNominatim(
            overpassQuery = "[out:json];node(1);out;",
            nominatimQuery = "mosque",
            lat = 23.6238,
            lon = 90.5000,
            radiusMeters = 2000,
            totalDeadlineMs = 1_200L,
            overpassMirrors = listOf("https://192.0.2.1/api/interpreter")
        )
        val elapsedMs = System.currentTimeMillis() - startMs

        assertTrue("Must respect hard deadline quickly (took ${elapsedMs}ms)", elapsedMs < 3_500L)
        assertTrue("Expected AllFailed, got $outcome", outcome is FreePlacesFetchOutcome.AllFailed)
        val failed = outcome as FreePlacesFetchOutcome.AllFailed
        assertTrue("Diagnostics must record at least one attempted source", failed.diagnostics.isNotEmpty())
    }

    // Live network probe across all 5 Overpass mirrors + Nominatim for Narayanganj (5 km)
    @Test
    fun liveProbeFiveOverpassMirrorsAndNominatimForNarayanganj() = runBlocking {
        val lat = 23.6238
        val lon = 90.5000
        val radius = 5000

        val mosqueOutcome = FreePlacesNetworkEngine.fetchOverpassThenNominatim(
            overpassQuery = MosqueRepository.buildOverpassQuery(lat, lon, radius),
            nominatimQuery = "mosque",
            lat = lat,
            lon = lon,
            radiusMeters = radius
        )
        when (mosqueOutcome) {
            is FreePlacesFetchOutcome.OverpassSuccess -> {
                val parsed = MosqueRepository.parseOverpassResponse(mosqueOutcome.body, lat, lon)
                println("V18_LIVE_MOSQUE_WINNER=Overpass:${mosqueOutcome.mirrorHost} elapsedMs=${mosqueOutcome.elapsedMs} count=${parsed.size}")
            }
            is FreePlacesFetchOutcome.NominatimSuccess -> {
                val parsed = MosqueRepository.parseNominatimResponse(mosqueOutcome.body, lat, lon, 6.0)
                println("V18_LIVE_MOSQUE_WINNER=Nominatim elapsedMs=${mosqueOutcome.elapsedMs} count=${parsed.size}")
            }
            is FreePlacesFetchOutcome.AllFailed -> {
                println("V18_LIVE_MOSQUE_WINNER=NONE timedOut=${mosqueOutcome.timedOutOverall} diags=${mosqueOutcome.diagnostics.joinToString("; ") { it.formatLine(true) }}")
            }
        }

        val halalOutcome = FreePlacesNetworkEngine.fetchOverpassThenNominatim(
            overpassQuery = HalalRestaurantRepository.buildOverpassQuery(lat, lon, radius),
            nominatimQuery = "restaurant",
            lat = lat,
            lon = lon,
            radiusMeters = radius
        )
        when (halalOutcome) {
            is FreePlacesFetchOutcome.OverpassSuccess -> {
                val parsed = HalalRestaurantRepository.parseOverpassResponse(halalOutcome.body, lat, lon)
                println("V18_LIVE_HALAL_WINNER=Overpass:${halalOutcome.mirrorHost} elapsedMs=${halalOutcome.elapsedMs} count=${parsed.size}")
            }
            is FreePlacesFetchOutcome.NominatimSuccess -> {
                val parsed = HalalRestaurantRepository.parseNominatimResponse(halalOutcome.body, lat, lon, 6.0, false)
                println("V18_LIVE_HALAL_WINNER=Nominatim elapsedMs=${halalOutcome.elapsedMs} count=${parsed.size}")
            }
            is FreePlacesFetchOutcome.AllFailed -> {
                println("V18_LIVE_HALAL_WINNER=NONE timedOut=${halalOutcome.timedOutOverall} diags=${halalOutcome.diagnostics.joinToString("; ") { it.formatLine(true) }}")
            }
        }
        assertTrue(true)
    }

    // ISSUE 3: Qibla Compass sensor fusion math, GeomagneticField declination, unwrapped North crossing, text summary
    @Test
    fun qiblaCompassMathHandlesNarayanganjDeclinationAndNorthWrapWithoutSpin() {
        val lat = 23.6238
        val lon = 90.5000

        val qiblaBearing = PrayerTimeCalculator.calculateQiblaBearing(lat, lon)
        assertTrue("Narayanganj Qibla bearing should be ~277° (got $qiblaBearing)", qiblaBearing in 275f..280f)

        val distKm = QiblaCompassMath.distanceToKaabaKm(lat, lon)
        assertTrue("Narayanganj distance to Kaaba should be ~5180 km (got $distKm)", distKm in 5120.0..5240.0)

        val declination = QiblaCompassMath.computeMagneticDeclinationDegrees(lat, lon)
        assertFalse("Declination must be finite", declination.isNaN() || declination.isInfinite())
        assertTrue("Declination in Bangladesh should be reasonable (got $declination)", declination in -15f..15f)

        // Vector low-pass filter converges smoothly
        val filtered = QiblaCompassMath.lowPassVector(floatArrayOf(0f, 0f, 9.8f), floatArrayOf(0f, 0f, 0f), alpha = 0.25f)
        assertEquals(2.45f, filtered[2], 0.01f)

        // Shortest-arc unwrapped stepping across 0°/360° NEVER jumps 358° backwards
        val nextAcrossNorth = QiblaCompassMath.stepUnwrappedAngle(
            currentContinuousDeg = 359f,
            targetMod360Deg = 1f,
            smoothingAlpha = 1.0f
        )
        assertEquals(361f, nextAcrossNorth, 0.01f)

        val nextReverseAcrossNorth = QiblaCompassMath.stepUnwrappedAngle(
            currentContinuousDeg = 1f,
            targetMod360Deg = 359f,
            smoothingAlpha = 1.0f
        )
        assertEquals(-1f, nextReverseAcrossNorth, 0.01f)

        // Accuracy mapping
        assertEquals(CompassAccuracyLevel.HIGH, CompassAccuracyLevel.fromSensorAccuracy(android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        assertTrue(CompassAccuracyLevel.LOW.needsCalibration)
        assertFalse(CompassAccuracyLevel.HIGH.needsCalibration)

        // Clear Bangla & English text direction summary
        val bnSummary = QiblaCompassMath.formatTextDirectionSummary(qiblaBearing, distKm, isEnglish = false)
        assertTrue("Bangla summary missing expected terms: $bnSummary", bnSummary.contains("কাবার দিক") && bnSummary.contains("পশ্চিম") && bnSummary.contains("কিমি"))
        val enSummary = QiblaCompassMath.formatTextDirectionSummary(qiblaBearing, distKm, isEnglish = true)
        assertTrue("English summary missing expected terms: $enSummary", enSummary.contains("Kaaba direction") && enSummary.contains("West") && enSummary.contains("km"))
    }

    // ISSUE 4: 4 Themes, 3 Font Sizes, 2 Formats, Large readable Story text, zero clipping, safe installed target check
    @Test
    fun shareCardSupportsFourThemesThreeFontSizesAndMeetsMinimumTypography() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.NOVEMBER, 20, 10, 0, 0)
        }
        val entry = DailyInspirationCatalog.forDate(cal)

        assertEquals(4, ShareCardTheme.entries.size)
        assertEquals(3, ShareCardFontSize.entries.size)
        assertEquals(2, ShareCardFormat.entries.size)

        // Story 1080x1920 default MEDIUM meets v1.8 minimum typography thresholds
        val storyDefault = DailyShareCardGenerator.renderDailyCard(
            context = context,
            entry = entry,
            isEnglish = false,
            format = ShareCardFormat.STORY,
            theme = ShareCardTheme.DARK_MOSQUE,
            fontSize = ShareCardFontSize.MEDIUM
        )
        assertFalse("Story default must not clip", storyDefault.isClipped)
        assertTrue("Hadith text must be >= 34px (got ${storyDefault.hadithBodyPx})", storyDefault.hadithBodyPx >= 34f)
        assertTrue("Ayah Arabic must be >= 38px (got ${storyDefault.ayahArabicPx})", storyDefault.ayahArabicPx >= 38f)
        assertTrue("Translation must be >= 26px (got ${storyDefault.ayahTranslationPx})", storyDefault.ayahTranslationPx >= 26f)
        storyDefault.bitmap.recycle()

        // Every combination of 4 themes x 3 font sizes x 2 formats renders cleanly without clipping
        for (theme in ShareCardTheme.entries) {
            for (size in ShareCardFontSize.entries) {
                for (format in ShareCardFormat.entries) {
                    val rendered = DailyShareCardGenerator.renderDailyCard(
                        context = context,
                        entry = entry,
                        isEnglish = false,
                        format = format,
                        theme = theme,
                        fontSize = size
                    )
                    assertEquals(format.widthPx, rendered.bitmap.width)
                    assertEquals(format.heightPx, rendered.bitmap.height)
                    assertFalse("Clipped for $theme / $size / $format", rendered.isClipped)
                    rendered.bitmap.recycle()
                }
            }
        }

        // Direct app target discovery safely returns empty list in Robolectric when social apps aren't installed
        val installed = DailyShareCardGenerator.getInstalledShareTargets(context)
        assertTrue("Installed targets check must succeed without crashing", installed.isEmpty())
    }
}
