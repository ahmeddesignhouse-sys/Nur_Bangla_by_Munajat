package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AdhanAlarmScheduler
import com.example.data.local.LocalAppSettings
import com.example.data.local.PreferencesDataStore
import com.example.data.model.AdhanVoice
import com.example.data.model.PerPrayerAlertSetting
import com.example.data.model.WaqtAlertConfig
import com.example.data.model.WaqtType
import com.example.data.repository.HalalRestaurantRepository
import com.example.util.HajjUmrahCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class NoorV15FeatureTest {

    @Test
    fun perPrayerNotificationOffProducesZeroAlarmsAndOnUsesVoiceAndReminder() {
        val baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.MAY, 10, 2, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowEpoch = baseCal.timeInMillis

        val config = WaqtAlertConfig(
            fajrSetting = PerPrayerAlertSetting(enabled = false, voice = AdhanVoice.MAKKAH, reminderMinutesBefore = 15),
            dhuhrSetting = PerPrayerAlertSetting(enabled = true, voice = AdhanVoice.MADINAH, reminderMinutesBefore = 10),
            asrSetting = PerPrayerAlertSetting(enabled = true, voice = AdhanVoice.AL_AQSA, reminderMinutesBefore = 5),
            maghribSetting = PerPrayerAlertSetting(enabled = true, voice = AdhanVoice.SILENT_VIBRATE, reminderMinutesBefore = 15),
            ishaSetting = PerPrayerAlertSetting(enabled = true, voice = AdhanVoice.SHORT_BEEP, reminderMinutesBefore = 0)
        )
        val settings = LocalAppSettings(
            districtId = "dhaka",
            useGps = false,
            alertConfig = config
        )

        val planned = AdhanAlarmScheduler.planUpcomingAlarms(settings, nowEpoch)

        // Fajr OFF => zero Fajr alarms (neither main nor pre-reminder)
        assertTrue("Fajr disabled should yield zero alarms", planned.none { it.waqt == WaqtType.FAJR })

        // Dhuhr ON with MADINAH and 10 min reminder
        val dhuhrMain = planned.firstOrNull { it.waqt == WaqtType.DHUHR && !it.isPreReminder }
        val dhuhrPre = planned.firstOrNull { it.waqt == WaqtType.DHUHR && it.isPreReminder }
        assertNotNull(dhuhrMain)
        assertNotNull(dhuhrPre)
        assertEquals(AdhanVoice.MADINAH, dhuhrMain!!.voice)
        assertEquals(10, dhuhrPre!!.preMinutes)
        assertEquals(10 * 60_000L, dhuhrMain.triggerAtMillis - dhuhrPre.triggerAtMillis)

        // Maghrib Silent/Vibrate keeps silent/vibrate on pre-reminder too
        val maghribPre = planned.firstOrNull { it.waqt == WaqtType.MAGHRIB && it.isPreReminder }
        assertNotNull(maghribPre)
        assertEquals(AdhanVoice.SILENT_VIBRATE, maghribPre!!.voice)

        // Isha has 0 min reminder => only main alarm, no pre-reminder
        assertEquals(1, planned.count { it.waqt == WaqtType.ISHA })
    }

    @Test
    fun perPrayerAlarmsAdaptToTravelTimezone() {
        val originalTz = TimeZone.getDefault()
        try {
            val referenceEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(2026, Calendar.JUNE, 15, 0, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Dhaka manual district (Asia/Dhaka UTC+6)
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dhaka"))
            val dhakaPlanned = AdhanAlarmScheduler.planUpcomingAlarms(
                LocalAppSettings(districtId = "dhaka", useGps = false),
                nowEpoch = referenceEpoch
            )
            val dhakaDhuhr = dhakaPlanned.first { it.waqt == WaqtType.DHUHR && !it.isPreReminder }

            // Travel to London GPS (Europe/London UTC+1 in June)
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            val londonPlanned = AdhanAlarmScheduler.planUpcomingAlarms(
                LocalAppSettings(
                    useGps = true,
                    gpsLatitude = 51.5074,
                    gpsLongitude = -0.1278
                ),
                nowEpoch = referenceEpoch
            )
            val londonDhuhr = londonPlanned.first { it.waqt == WaqtType.DHUHR && !it.isPreReminder }

            // London solar noon occurs roughly 6 hours after Dhaka solar noon
            val diffHours = (londonDhuhr.triggerAtMillis - dhakaDhuhr.triggerAtMillis) / 3_600_000.0
            assertTrue("Expected London Dhuhr ~6h after Dhaka, got $diffHours h", diffHours in 5.0..7.0)
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun perPrayerPreferencesAndHajjChecklistPersistInDataStore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = PreferencesDataStore(context)

        store.setWaqtEnabled(WaqtType.FAJR, false)
        store.setWaqtAdhanVoice(WaqtType.DHUHR, AdhanVoice.AL_AQSA)
        store.setWaqtReminderMinutes(WaqtType.DHUHR, 10)

        val updated = store.settingsFlow.first()
        assertFalse(updated.alertConfig.settingFor(WaqtType.FAJR).enabled)
        assertEquals(AdhanVoice.AL_AQSA, updated.alertConfig.settingFor(WaqtType.DHUHR).voice)
        assertEquals(10, updated.alertConfig.settingFor(WaqtType.DHUHR).reminderMinutesBefore)

        // Checklist toggle & persistence
        store.resetHajjChecklist()
        assertTrue(store.settingsFlow.first().hajjCompletedStepIds.isEmpty())

        val added = store.toggleHajjStepCompleted("step_01_ihram")
        assertTrue(added)
        store.toggleHajjStepCompleted("step_03_tawaf")
        val afterTicks = store.settingsFlow.first().hajjCompletedStepIds
        assertEquals(setOf("step_01_ihram", "step_03_tawaf"), afterTicks)

        val removed = store.toggleHajjStepCompleted("step_01_ihram")
        assertFalse(removed)
        assertEquals(setOf("step_03_tawaf"), store.settingsFlow.first().hajjCompletedStepIds)

        store.resetHajjChecklist()
        assertTrue(store.settingsFlow.first().hajjCompletedStepIds.isEmpty())
    }

    @Test
    fun hajjUmrahCatalogContainsAllRequiredStepsDuasAndPracticalSections() {
        val steps = HajjUmrahCatalog.STEPS
        assertEquals(12, steps.size)

        val allTitlesEn = steps.joinToString(" ") { it.titleEn.lowercase() }
        listOf("ihram", "tawaf", "sa'i", "mina", "arafat", "muzdalifah", "jamarat", "tawaf al-ifadah").forEach { keyword ->
            assertTrue("Expected step containing '$keyword'", allTitlesEn.contains(keyword))
        }

        steps.forEach { step ->
            assertTrue("Step ${step.id} missing Arabic Dua", step.duaArabic.isNotBlank())
            assertTrue("Step ${step.id} missing Bangla Dua", step.duaBangla.isNotBlank())
            assertTrue("Step ${step.id} missing English Dua", step.duaEnglish.isNotBlank())
            assertTrue("Step ${step.id} missing Bangla desc", step.descriptionBn.isNotBlank())
            assertTrue("Step ${step.id} missing English desc", step.descriptionEn.isNotBlank())
        }

        val practicalIds = HajjUmrahCatalog.PRACTICAL_SECTIONS.map { it.id }.toSet()
        assertEquals(setOf("packing", "ihram_rules", "common_mistakes"), practicalIds)
        HajjUmrahCatalog.PRACTICAL_SECTIONS.forEach { sec ->
            assertTrue(sec.itemsBn.isNotEmpty())
            assertTrue(sec.itemsEn.isNotEmpty())
        }
    }

    @Test
    fun halalOverpassParserSortsByDistanceAndFiltersByNameAreaCuisine() {
        val userLat = 23.8103
        val userLon = 90.4125
        val sampleJson = """
            {
              "elements": [
                {
                  "type": "node",
                  "id": 201,
                  "lat": 23.8250,
                  "lon": 90.4200,
                  "tags": {
                    "name": "Star Kabab & Restaurant",
                    "name:bn": "স্টার কাবাব এন্ড রেস্টুরেন্ট",
                    "amenity": "restaurant",
                    "cuisine": "bangladeshi;kebab",
                    "diet:halal": "only",
                    "addr:street": "Banani Road 11",
                    "addr:city": "Dhaka",
                    "rating": "4.6"
                  }
                },
                {
                  "type": "way",
                  "id": 202,
                  "center": { "lat": 23.8120, "lon": 90.4130 },
                  "tags": {
                    "name": "Kacchi Bhai",
                    "name:bn": "কাচ্চি ভাই",
                    "amenity": "restaurant",
                    "cuisine": "biryani",
                    "diet:halal": "yes",
                    "addr:suburb": "Dhanmondi",
                    "opening_hours": "11:00-23:00"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = HalalRestaurantRepository.parseOverpassResponse(sampleJson, userLat, userLon)
        assertEquals(2, parsed.size)

        // Sorted ascending by distance => Kacchi Bhai (closer) comes first
        assertEquals("Kacchi Bhai", parsed[0].nameEn)
        assertEquals("Star Kabab & Restaurant", parsed[1].nameEn)
        assertTrue(parsed[0].distanceKm < parsed[1].distanceKm)
        assertEquals("100% Halal", parsed[1].halalBadgeEn)
        assertEquals("4.6", parsed[1].rating)
        assertEquals("Halal Verified", parsed[0].halalBadgeEn)

        // Filter by cuisine, area, and Bangla name
        assertEquals(1, HalalRestaurantRepository.filterRestaurants(parsed, "biryani").size)
        assertEquals(1, HalalRestaurantRepository.filterRestaurants(parsed, "Banani").size)
        assertEquals(1, HalalRestaurantRepository.filterRestaurants(parsed, "স্টার কাবাব").size)
        assertEquals(2, HalalRestaurantRepository.filterRestaurants(parsed, "").size)
    }
}
