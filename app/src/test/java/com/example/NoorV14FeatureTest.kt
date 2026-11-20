package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalAppSettings
import com.example.data.local.PreferencesDataStore
import com.example.util.DailyInspirationCatalog
import com.example.util.QuranReciter
import com.example.widget.PrayerTimesWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorV14FeatureTest {

    @Test
    fun quranReciterAudioUrlsAndDailyGoalResetAcrossDays() = runTest {
        // Verify at least 2 reciters and valid EveryAyah URLs (including non-contiguous Ayatul Kursi 2:255)
        assertTrue(QuranReciter.entries.size >= 2)
        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
            QuranReciter.buildAyahUrl(QuranReciter.ALAFASY, 1, 1)
        )
        assertEquals(
            "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/002255.mp3",
            QuranReciter.buildAyahUrl(QuranReciter.ABDUL_BASIT, 2, 255)
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesDataStore(context)

        // Reciter & daily goal persist in DataStore
        prefs.setQuranReciter(QuranReciter.SUDAIS.id)
        prefs.setDailyQuranGoal(20)
        prefs.recordQuranAyahProgress("2026-11-20", 1, 1)
        prefs.recordQuranAyahProgress("2026-11-20", 1, 2)
        prefs.recordQuranAyahProgress("2026-11-20", 1, 2) // duplicate ayah on same day does not double-count

        val day1 = prefs.settingsFlow.first()
        assertEquals(QuranReciter.SUDAIS.id, day1.quranReciterId)
        assertEquals(20, day1.dailyQuranGoal)
        assertEquals(2, day1.todayReadAyahCount("2026-11-20"))
        // Automatic daily reset when date rolls over
        assertEquals(0, day1.todayReadAyahCount("2026-11-21"))

        // Recording on next day starts fresh at 1
        prefs.recordQuranAyahProgress("2026-11-21", 112, 1)
        val day2 = prefs.settingsFlow.first()
        assertEquals(1, day2.todayReadAyahCount("2026-11-21"))
        assertFalse(day2.todayReadAyahKeys("2026-11-21").contains("1_1"))
    }

    @Test
    fun dailyHadithAndAyahRotatesDailyIncludesArabicBanglaEnglishAndSevenPreviousDays() {
        val tz = TimeZone.getTimeZone("Asia/Dhaka")
        val day1 = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 20, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val day2 = (day1.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        val entry1 = DailyInspirationCatalog.forDate(day1)
        val entry2 = DailyInspirationCatalog.forDate(day2)

        // Rotates next day
        assertNotEquals(entry1.hadith.id, entry2.hadith.id)
        assertNotEquals(entry1.ayah.id, entry2.ayah.id)

        // Always has Arabic + Bangla + English + source
        assertTrue(entry1.hadith.arabic.isNotBlank())
        assertTrue(entry1.hadith.bangla.isNotBlank())
        assertTrue(entry1.hadith.english.isNotBlank())
        assertTrue(entry1.hadith.sourceBn.isNotBlank())
        assertTrue(entry1.hadith.sourceEn.isNotBlank())

        assertTrue(entry1.ayah.arabic.isNotBlank())
        assertTrue(entry1.ayah.bangla.isNotBlank())
        assertTrue(entry1.ayah.english.isNotBlank())
        assertTrue(entry1.ayah.sourceEn.contains("Quran"))

        // Share text contains Arabic + both languages
        val shareEn = entry1.formatShareText(isEnglish = true)
        assertTrue(shareEn.contains(entry1.hadith.arabic))
        assertTrue(shareEn.contains(entry1.hadith.english))
        assertTrue(shareEn.contains(entry1.ayah.english))

        // Today + last 7 previous days = 8 total entries
        val history = DailyInspirationCatalog.todayAndPreviousSevenDays(day1)
        assertEquals(8, history.size)
        assertEquals(0, history.first().daysAgo)
        assertEquals(7, history.last().daysAgo)
    }

    @Test
    fun homeScreenWidgetSnapshotRespectsLanguageLocationTimezoneAndBuildsRemoteViews() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixedTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.NOVEMBER, 20, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val bnSettings = LocalAppSettings(languageCode = "bn", districtId = "dhaka", useGps = false)
        val enSettings = LocalAppSettings(languageCode = "en", districtId = "dhaka", useGps = false)
        val londonGpsSettings = LocalAppSettings(
            languageCode = "en",
            useGps = true,
            gpsLatitude = 51.5074,
            gpsLongitude = -0.1278,
            gpsLocationLabel = "London"
        )

        val bnSnap = PrayerTimesWidgetProvider.computeSnapshot(bnSettings, fixedTime)
        val enSnap = PrayerTimesWidgetProvider.computeSnapshot(enSettings, fixedTime)
        val londonSnap = PrayerTimesWidgetProvider.computeSnapshot(londonGpsSettings, fixedTime)

        assertTrue(bnSnap.locationHeader.contains("নূর বাংলা"))
        assertEquals("পরবর্তী ওয়াক্ত", bnSnap.nextLabel)
        assertEquals("সাহরি", bnSnap.sehriLabel)
        assertEquals("ইফতার", bnSnap.iftarLabel)
        assertTrue(bnSnap.hijriDate.isNotBlank())
        assertTrue(bnSnap.remainingSeconds > 0)

        assertTrue(enSnap.locationHeader.contains("Nur Bangla"))
        assertEquals("Next Prayer", enSnap.nextLabel)
        assertEquals("Sehri", enSnap.sehriLabel)
        assertEquals("Iftar", enSnap.iftarLabel)
        assertTrue(enSnap.hijriDate.isNotBlank())

        // Location/timezone change recalculates widget snapshot cleanly
        assertTrue(londonSnap.locationHeader.contains("London"))
        assertNotEquals(enSnap.iftarTime, londonSnap.iftarTime)

        // RemoteViews builds without error
        val rv = PrayerTimesWidgetProvider.buildRemoteViews(context, enSnap)
        assertNotNull(rv)
    }
}
