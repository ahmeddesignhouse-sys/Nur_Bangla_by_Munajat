package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AdhanPlaybackService
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AdhanVoice
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.NextWaqtInfo
import com.example.data.model.WaqtAlertConfig
import com.example.data.model.WaqtType
import com.example.data.repository.HalalRestaurantRepository
import com.example.data.repository.MosqueRepository
import com.example.ui.components.formatRadiusExpansionBanner
import com.example.ui.screens.calculateWaqtProgress
import com.example.util.DailyInspirationCatalog
import com.example.util.DailyShareCardGenerator
import com.example.util.ShareCardFormat
import com.example.util.ShareCardTheme
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
class NoorV17FixTest {

    // Issue 1: Daily Hadith & Ayah PNG Share Card (zero clipping across all catalog entries)
    @Test
    fun shareCardRendersFullContentWithoutClippingForAllEntries() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.NOVEMBER, 20, 10, 0, 0)
        }

        // Verify all 21 rotating days render cleanly in Story (1080x1920) & Feed (1080x1080)
        for (dayOffset in 0 until DailyInspirationCatalog.HADITHS.size) {
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val entry = DailyInspirationCatalog.forDate(dayCal)

            val storyBn = DailyShareCardGenerator.renderDailyCard(
                context = context,
                entry = entry,
                isEnglish = false,
                format = ShareCardFormat.STORY,
                theme = ShareCardTheme.DARK_MOSQUE
            )
            assertEquals(1080, storyBn.bitmap.width)
            assertEquals(1920, storyBn.bitmap.height)
            assertFalse("Story Bangla clipped for ${entry.hadith.id}/${entry.ayah.id}", storyBn.isClipped)
            storyBn.bitmap.recycle()

            val feedEn = DailyShareCardGenerator.renderDailyCard(
                context = context,
                entry = entry,
                isEnglish = true,
                format = ShareCardFormat.FEED,
                theme = ShareCardTheme.LIGHT_IVORY
            )
            assertEquals(1080, feedEn.bitmap.width)
            assertEquals(1080, feedEn.bitmap.height)
            assertFalse("Feed English clipped for ${entry.hadith.id}/${entry.ayah.id}", feedEn.isClipped)
            feedEn.bitmap.recycle()
        }
    }

    // Issue 2: Current Prayer Card progress bar math (daytime & overnight Isha wrap)
    @Test
    fun calculateWaqtProgressHandlesDaytimeAndOvernightIsha() {
        val schedule = DailyPrayerSchedule(
            fajrMinute = 270,       // 04:30
            sunriseMinute = 350,    // 05:50
            dhuhrMinute = 720,      // 12:00
            asrMinute = 930,        // 15:30
            sunsetMinute = 1040,    // 17:20
            maghribMinute = 1043,   // 17:23
            ishaMinute = 1120,      // 18:40
            sehriEndMinute = 264,
            iftarMinute = 1043,
            forbiddenTimes = emptyList(),
            ishaEndMinute = 270
        )

        // Dhuhr is 12:00..15:30 (210 mins = 12600s). With 6300s left -> 50% elapsed
        val dhuhrNext = NextWaqtInfo(WaqtType.ASR, 930, remainingSeconds = 6300L, isTomorrow = false)
        assertEquals(0.5f, calculateWaqtProgress(schedule, WaqtType.DHUHR, dhuhrNext), 0.01f)

        // Isha wraps overnight 18:40..04:30 (590 mins = 35400s). With 17700s left -> 50% elapsed
        val ishaNext = NextWaqtInfo(WaqtType.FAJR, 270, remainingSeconds = 17700L, isTomorrow = true)
        assertEquals(0.5f, calculateWaqtProgress(schedule, WaqtType.ISHA, ishaNext), 0.01f)
    }

    // Issue 4: Full offline Makkah, Madinah & Al-Aqsa Azan defaults and bundled MP3 raw assets
    @Test
    fun allFivePrayersDefaultToFullAdhanAndBundledMp3AssetsExist() {
        val defaultConfig = WaqtAlertConfig()
        for (waqt in WaqtType.entries) {
            val setting = defaultConfig.settingFor(waqt)
            assertTrue("${waqt.name} must be enabled by default", setting.enabled)
            assertTrue("${waqt.name} must default to full Adhan voice", setting.voice.isFullAdhan)
            assertEquals(AdhanAlertMode.FULL_ADHAN, defaultConfig.modeFor(waqt))
        }
        assertFalse(AdhanVoice.SHORT_BEEP.isFullAdhan)
        assertFalse(AdhanVoice.SILENT_VIBRATE.isFullAdhan)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fullVoices = listOf(AdhanVoice.MAKKAH, AdhanVoice.MADINAH, AdhanVoice.AL_AQSA)
        for (voice in fullVoices) {
            val rawId = AdhanPlaybackService.rawResIdForVoice(voice)
            context.resources.openRawResource(rawId).use { stream ->
                val size = stream.available()
                assertTrue(
                    "Full Adhan MP3 for ${voice.name} must be > 1 MB (got $size bytes)",
                    size > 1_000_000
                )
            }
        }
    }

    // Issue 5: Mosque & Halal Finder radius ladder, Nominatim highway filtering & Overpass parsing
    @Test
    fun nearbyPlaceSearchLadderAndParsersWorkForNarayanganj() {
        assertEquals(listOf(2000, 5000, 10000, 20000), MosqueRepository.buildRadiusLadder(2000))
        assertEquals(listOf(5000, 10000, 20000), MosqueRepository.buildRadiusLadder(5000))
        assertEquals(listOf(10000, 20000), MosqueRepository.buildRadiusLadder(10000))
        assertEquals(listOf(20000), MosqueRepository.buildRadiusLadder(20000))

        // Narayanganj coordinates
        val userLat = 23.6238
        val userLon = 90.5000

        // Nominatim JSON containing 1 real mosque + 1 highway road named "Mosque Road" (must be excluded!)
        val nominatimMosqueJson = """
            [
              {
                "place_id": 101,
                "osm_id": 5001,
                "lat": "23.6245",
                "lon": "90.5012",
                "category": "amenity",
                "type": "place_of_worship",
                "name": "Ramarbag Shahi Jame Mosque",
                "display_name": "Ramarbag Shahi Jame Mosque, Narayanganj, Dhaka Division, Bangladesh",
                "address": {"road": "College Road", "town": "Narayanganj"}
              },
              {
                "place_id": 102,
                "osm_id": 5002,
                "lat": "23.6240",
                "lon": "90.5005",
                "category": "highway",
                "type": "residential",
                "name": "Mosque Road",
                "display_name": "Mosque Road, Narayanganj"
              }
            ]
        """.trimIndent()

        val mosques = MosqueRepository.parseNominatimResponse(nominatimMosqueJson, userLat, userLon, maxDistanceKm = 5.0)
        assertEquals(1, mosques.size)
        assertEquals("Ramarbag Shahi Jame Mosque", mosques.first().nameEn)
        assertEquals("College Road, Narayanganj", mosques.first().address)

        // Halal restaurant Nominatim parser
        val nominatimFoodJson = """
            [
              {
                "place_id": 201,
                "osm_id": 6001,
                "lat": "23.6250",
                "lon": "90.5020",
                "category": "amenity",
                "type": "restaurant",
                "name": "Nanna Biryani House",
                "display_name": "Nanna Biryani House, Chashara, Narayanganj",
                "address": {"suburb": "Chashara", "town": "Narayanganj"}
              }
            ]
        """.trimIndent()

        val restaurants = HalalRestaurantRepository.parseNominatimResponse(
            jsonText = nominatimFoodJson,
            userLat = userLat,
            userLon = userLon,
            maxDistanceKm = 5.0,
            isExplicitHalalQuery = false
        )
        assertEquals(1, restaurants.size)
        assertEquals("Nanna Biryani House", restaurants.first().nameEn)
        assertEquals("Halal Verified", restaurants.first().halalBadgeEn)

        // Expansion banner strings
        val bannerEn = formatRadiusExpansionBanner(2000, 10000, isMosque = true, isEnglish = true)
        assertEquals("No mosque within 2 km, showing within 10 km", bannerEn)
        val bannerBn = formatRadiusExpansionBanner(2000, 10000, isMosque = true, isEnglish = false)
        assertTrue(bannerBn.contains("২") && bannerBn.contains("১০") && bannerBn.contains("মসজিদ"))
    }

    @Test
    fun overpassQueriesHalalFirstFallbackCacheGpsAndMapFallbackWork() {
        // 1. GPS validation rejects (0,0) and NaN, accepts Narayanganj
        assertFalse(com.example.util.LocationHelper.isValidCoordinate(0.0, 0.0))
        assertFalse(com.example.util.LocationHelper.isValidCoordinate(Double.NaN, 90.5))
        assertTrue(com.example.util.LocationHelper.isValidCoordinate(23.6238, 90.5000))

        // 2. Simple proven Overpass around: queries for Narayanganj at 5 km
        val mosqueQuery = MosqueRepository.buildOverpassQuery(23.6238, 90.5000, 5000)
        assertTrue(mosqueQuery.contains("[timeout:20]"))
        assertTrue(mosqueQuery.contains("""node["amenity"="place_of_worship"]["religion"="muslim"](around:5000,23.62380,90.50000)"""))
        assertTrue(mosqueQuery.contains("""way["amenity"="place_of_worship"]["religion"="muslim"](around:5000,23.62380,90.50000)"""))
        assertEquals(2, MosqueRepository.OVERPASS_ENDPOINTS.size) // primary + 1 retry only

        val strictHalalQuery = HalalRestaurantRepository.buildStrictHalalOverpassQuery(23.6238, 90.5000, 5000)
        assertTrue(strictHalalQuery.contains("diet:halal"))
        assertTrue(strictHalalQuery.contains("around:5000,23.62380,90.50000"))

        val broadFoodQuery = HalalRestaurantRepository.buildAllRestaurantsOverpassQuery(23.6238, 90.5000, 5000)
        assertTrue(broadFoodQuery.contains("""node["amenity"~"^(restaurant|cafe|fast_food)$"](around:5000,23.62380,90.50000)"""))
        assertTrue(broadFoodQuery.contains("""way["amenity"~"^(restaurant|cafe|fast_food)$"](around:5000,23.62380,90.50000)"""))
        assertEquals(2, HalalRestaurantRepository.OVERPASS_ENDPOINTS.size) // primary + 1 retry only

        // 3. Broad fallback puts Halal-named / Halal-tagged restaurants FIRST ahead of closer general cafes
        val broadResponseJson = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 1001,
                  "lat": 23.6239,
                  "lon": 90.5001,
                  "tags": { "amenity": "cafe", "name": "Chashara Coffee Corner" }
                },
                {
                  "type": "way",
                  "id": 1002,
                  "center": { "lat": 23.6310, "lon": 90.5060 },
                  "tags": { "amenity": "restaurant", "name": "Bismillah Halal Biryani", "cuisine": "halal;biryani" }
                }
              ]
            }
        """.trimIndent()
        val parsedFood = HalalRestaurantRepository.parseOverpassResponse(broadResponseJson, 23.6238, 90.5000)
        assertEquals(2, parsedFood.size)
        assertEquals("Bismillah Halal Biryani", parsedFood[0].nameEn)
        assertTrue(parsedFood[0].isExplicitHalal)
        assertEquals("Chashara Coffee Corner", parsedFood[1].nameEn)
        assertFalse(parsedFood[1].isExplicitHalal)

        // 4. 10-minute cache stores & expires after 10 minutes
        MosqueRepository.clearCache()
        val key = MosqueRepository.buildCacheKey(23.6238, 90.5000, 5000)
        val now = 1_000_000L
        val sample = com.example.data.repository.MosqueSearchResult.Success(
            mosques = emptyList(),
            requestedRadiusMeters = 5000,
            effectiveRadiusMeters = 5000
        )
        MosqueRepository.putCachedResult(key, sample, nowMs = now)
        assertTrue(MosqueRepository.getCachedResult(key, nowMs = now + 5 * 60_000L) != null)
        assertTrue(MosqueRepository.getCachedResult(key, nowMs = now + 11 * 60_000L) == null)

        // 5. Google Maps API key detection falls back cleanly to OpenStreetMap when placeholder/empty
        assertFalse(com.example.ui.components.hasValidGoogleMapsApiKey(""))
        assertFalse(com.example.ui.components.hasValidGoogleMapsApiKey("YOUR_GOOGLE_MAPS_API_KEY"))
        assertTrue(com.example.ui.components.hasValidGoogleMapsApiKey("AIzaSyDummyValidLookingGoogleMapsKey123456"))
    }

    @Test
    fun offlineFinderReturnsImmediatelyWithoutSpinning() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val shadowCm = org.robolectric.Shadows.shadowOf(cm)
        shadowCm.setDefaultNetworkActive(false)
        shadowCm.setNetworkCapabilities(cm.activeNetwork, null)

        val startMs = System.currentTimeMillis()
        val mosqueResult = MosqueRepository(context).findNearbyMosques(23.6238, 90.5000, 5000)
        val halalResult = HalalRestaurantRepository(context).findNearbyHalalRestaurants(23.6238, 90.5000, 5000)
        val elapsedMs = System.currentTimeMillis() - startMs

        assertTrue("Expected MosqueSearchResult.Offline, got $mosqueResult", mosqueResult is com.example.data.repository.MosqueSearchResult.Offline)
        assertTrue("Expected HalalSearchResult.Offline, got $halalResult", halalResult is com.example.data.repository.HalalSearchResult.Offline)
        assertTrue("Offline check must finish well under 3 seconds (took ${elapsedMs}ms)", elapsedMs < 3_000L)
    }
}
