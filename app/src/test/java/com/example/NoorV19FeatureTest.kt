package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppLinks
import com.example.util.DailyInspirationCatalog
import com.example.util.DailyInspirationEntry
import com.example.util.DailyShareCardGenerator
import com.example.util.InspirationSlot
import com.example.util.ShareCardFontSize
import com.example.util.ShareCardFormat
import com.example.util.ShareCardTheme
import com.example.util.ShareCardVideoGenerator
import com.example.util.ShareVideoDuration
import com.example.util.ShareVideoMusic
import com.example.util.SpecialIslamicDay
import com.example.util.VideoShareDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorV19FeatureTest {

    @Test
    fun versionAndPackageMetadataMatchV19PlayStoreBuild() {
        assertEquals("com.noorbangla.islam", BuildConfig.APPLICATION_ID)
        assertEquals(10, BuildConfig.VERSION_CODE)
        assertEquals("1.9", BuildConfig.VERSION_NAME)

        // Verify Play Store compliance: REQUEST_INSTALL_PACKAGES must NOT be in AndroidManifest
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
        assertFalse(
            "REQUEST_INSTALL_PACKAGES must be removed for Google Play Store",
            requested.contains("android.permission.REQUEST_INSTALL_PACKAGES")
        )
    }

    @Test
    fun catalogHas1095VerifiedHadithsAnd1095VerifiedAyahsWithZeroRepeatsAndZeroEmpties() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DailyInspirationCatalog.initialize(context)

        val hadiths = DailyInspirationCatalog.HADITHS
        val ayahs = DailyInspirationCatalog.AYAHS

        assertEquals(1095, hadiths.size)
        assertEquals(1095, ayahs.size)

        // Check unique IDs, non-empty fields, and zero duplicate Arabic entries
        val hadithIds = HashSet<String>(1095)
        val hadithArabics = HashSet<String>(1095)
        for (h in hadiths) {
            assertTrue("Duplicate hadith id ${h.id}", hadithIds.add(h.id))
            assertTrue("Empty hadith arabic for ${h.id}", h.arabic.isNotBlank())
            assertTrue("Empty hadith bangla for ${h.id}", h.bangla.isNotBlank())
            assertTrue("Empty hadith english for ${h.id}", h.english.isNotBlank())
            assertTrue("Empty hadith sourceBn for ${h.id}", h.sourceBn.isNotBlank())
            assertTrue("Empty hadith sourceEn for ${h.id}", h.sourceEn.isNotBlank())
            assertTrue("Empty hadith narratorBn for ${h.id}", h.narratorBn.isNotBlank())
            assertTrue("Empty hadith narratorEn for ${h.id}", h.narratorEn.isNotBlank())
            assertTrue("Duplicate hadith arabic for ${h.id}", hadithArabics.add(h.arabic.trim()))
        }

        val ayahIds = HashSet<String>(1095)
        val ayahArabics = HashSet<String>(1095)
        for (a in ayahs) {
            assertTrue("Duplicate ayah id ${a.id}", ayahIds.add(a.id))
            assertTrue("Empty ayah arabic for ${a.id}", a.arabic.isNotBlank())
            assertTrue("Empty ayah bangla for ${a.id}", a.bangla.isNotBlank())
            assertTrue("Empty ayah english for ${a.id}", a.english.isNotBlank())
            assertTrue("Empty ayah sourceBn for ${a.id}", a.sourceBn.isNotBlank())
            assertTrue("Empty ayah sourceEn for ${a.id}", a.sourceEn.isNotBlank())
            assertTrue("Duplicate ayah arabic for ${a.id}", ayahArabics.add(a.arabic.trim()))
        }

        // All 8 special-day tags must be present in both catalogs
        val requiredSpecialTags = SpecialIslamicDay.entries.map { it.tag }.toSet()
        val foundHadithTags = hadiths.mapNotNull { it.specialDay }.toSet()
        val foundAyahTags = ayahs.mapNotNull { it.specialDay }.toSet()
        for (tag in requiredSpecialTags) {
            assertTrue("Missing specialDay tag '$tag' in hadiths_catalog.json", tag in foundHadithTags)
            assertTrue("Missing specialDay tag '$tag' in ayahs_catalog.json", tag in foundAyahTags)
        }

        // Deterministic mapping (dayOfYear 1..365 x 3 slots) covers all 1,095 unique indices with zero repeats
        val seenIndices = HashSet<Int>(1095)
        for (day in 1..365) {
            for (slot in InspirationSlot.entries) {
                val idx = DailyInspirationCatalog.catalogIndexFor(day, slot)
                assertTrue("Index out of range: $idx", idx in 0 until 1095)
                assertTrue("Repeated index $idx for day=$day slot=$slot", seenIndices.add(idx))
            }
        }
        assertEquals(1095, seenIndices.size)

        // Leap-year day 366 reuses indices 0..2 cleanly
        assertEquals(0, DailyInspirationCatalog.catalogIndexFor(366, InspirationSlot.SLOT_1))
        assertEquals(1, DailyInspirationCatalog.catalogIndexFor(366, InspirationSlot.SLOT_2))
        assertEquals(2, DailyInspirationCatalog.catalogIndexFor(366, InspirationSlot.SLOT_3))
    }

    @Test
    fun threeHourSlotBoundariesSpecialFridaySlotAndCountdownWorkInAsiaDhaka() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DailyInspirationCatalog.initialize(context)
        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")

        fun dhakaCal(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
            Calendar.getInstance(dhakaTz).apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }

        // Pick a non-Friday ordinary day: Tuesday, Oct 13, 2026
        val at0459 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.OCTOBER, 13, 4, 59))
        val at0501 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.OCTOBER, 13, 5, 1))
        val at0759 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.OCTOBER, 13, 7, 59))
        val at0801 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.OCTOBER, 13, 8, 1))
        val at1059 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.OCTOBER, 13, 10, 59))
        val at1101 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.OCTOBER, 13, 11, 1))

        assertEquals(InspirationSlot.SLOT_3, at0459.slot)
        assertEquals("2026-10-12", at0459.dateKey) // belongs to previous day's 11:00–05:00 slot
        assertEquals(InspirationSlot.SLOT_1, at0501.slot)
        assertEquals(InspirationSlot.SLOT_1, at0759.slot)
        assertEquals(InspirationSlot.SLOT_2, at0801.slot)
        assertEquals(InspirationSlot.SLOT_2, at1059.slot)
        assertEquals(InspirationSlot.SLOT_3, at1101.slot)

        assertEquals(at0501.hadith.id, at0759.hadith.id)
        assertEquals(at0801.hadith.id, at1059.hadith.id)
        assertNotEquals(at0459.hadith.id, at0501.hadith.id)
        assertNotEquals(at0759.hadith.id, at0801.hadith.id)
        assertNotEquals(at1059.hadith.id, at1101.hadith.id)
        assertNotEquals(at0501.ayah.id, at0801.ayah.id)
        assertNotEquals(at0801.ayah.id, at1101.ayah.id)

        // Friday Slot 1 (Nov 20, 2026 is Friday) triggers Jummah special-day override
        val fridaySlot1 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.NOVEMBER, 20, 6, 30))
        assertEquals(SpecialIslamicDay.JUMMAH, fridaySlot1.specialDay)
        assertEquals("jummah", fridaySlot1.hadith.specialDay)
        assertEquals("jummah", fridaySlot1.ayah.specialDay)
        assertNotNull(fridaySlot1.specialDayLabel(isEnglish = false))
        assertNotNull(fridaySlot1.specialDayLabel(isEnglish = true))

        // Friday Slot 2 does NOT repeat Jummah override
        val fridaySlot2 = DailyInspirationCatalog.forDate(dhakaCal(2026, Calendar.NOVEMBER, 20, 9, 0))
        assertNull(fridaySlot2.specialDay)
        assertNotEquals(fridaySlot1.hadith.id, fridaySlot2.hadith.id)

        // Countdown formatting
        val cdBn = DailyInspirationCatalog.formatNextSlotCountdown(
            dhakaCal(2026, Calendar.OCTOBER, 13, 5, 45),
            isEnglish = false
        )
        assertTrue("Expected Bangla countdown with ২ ঘণ্টা ১৫ মিনিট, got: $cdBn", cdBn.contains("পরের হাদিস") && cdBn.contains("২") && cdBn.contains("১৫"))
        val cdEn = DailyInspirationCatalog.formatNextSlotCountdown(
            dhakaCal(2026, Calendar.OCTOBER, 13, 5, 45),
            isEnglish = true
        )
        assertEquals("Next Hadith: 2h 15m", cdEn)

        // Previous 7 days archive returns 8 items (today + 7 past days at Slot 1)
        val archive = DailyInspirationCatalog.todayAndPreviousSevenDays(
            dhakaCal(2026, Calendar.OCTOBER, 13, 9, 30)
        )
        assertEquals(8, archive.size)
        assertEquals(0, archive[0].daysAgo)
        assertEquals(InspirationSlot.SLOT_2, archive[0].slot)
        for (i in 1..7) {
            assertEquals(i, archive[i].daysAgo)
            assertEquals(InspirationSlot.SLOT_1, archive[i].slot)
        }
    }

    @Test
    fun longestCatalogEntryRendersZeroClippingAcrossAll12ThemesAnd5FontSizes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DailyInspirationCatalog.initialize(context)

        val longestHadith = DailyInspirationCatalog.HADITHS.maxBy {
            it.arabic.length + maxOf(it.bangla.length, it.english.length)
        }
        val longestAyah = DailyInspirationCatalog.AYAHS.maxBy {
            it.arabic.length + maxOf(it.bangla.length, it.english.length)
        }

        val stressEntry = DailyInspirationEntry(
            dateKey = "2026-11-20",
            daysAgo = 0,
            gregorianDateBn = "২০ নভেম্বর ২০২৬",
            gregorianDateEn = "20 November 2026",
            hijriDateBn = "১০ জুমাদাল উলা ১৪৪৮ হিজরি",
            hijriDateEn = "10 Jumada al-Awwal 1448 AH",
            hadith = longestHadith,
            ayah = longestAyah,
            slot = InspirationSlot.SLOT_1,
            specialDay = SpecialIslamicDay.JUMMAH
        )

        assertEquals(12, ShareCardTheme.entries.size)
        assertEquals(5, ShareCardFontSize.entries.size)
        assertEquals(0.90f, ShareCardFontSize.SMALL.scaleMultiplier, 0.001f)
        assertEquals(1.00f, ShareCardFontSize.MEDIUM.scaleMultiplier, 0.001f)
        assertEquals(1.12f, ShareCardFontSize.LARGE.scaleMultiplier, 0.001f)
        assertEquals(1.28f, ShareCardFontSize.EXTRA_LARGE.scaleMultiplier, 0.001f)
        assertEquals(1.45f, ShareCardFontSize.XXL.scaleMultiplier, 0.001f)

        // Test every combination of 12 themes x 5 font sizes x 2 formats across Bangla & English
        for (theme in ShareCardTheme.entries) {
            for (fontSize in ShareCardFontSize.entries) {
                for (format in ShareCardFormat.entries) {
                    val isEn = (theme.ordinal + fontSize.ordinal) % 2 == 1
                    val rendered = DailyShareCardGenerator.renderDailyCard(
                        context = context,
                        entry = stressEntry,
                        isEnglish = isEn,
                        format = format,
                        theme = theme,
                        fontSize = fontSize
                    )
                    assertEquals(format.widthPx, rendered.bitmap.width)
                    assertEquals(format.heightPx, rendered.bitmap.height)
                    assertFalse(
                        "Longest entry clipped for theme=$theme size=$fontSize format=$format isEn=$isEn",
                        rendered.isClipped
                    )
                    rendered.bitmap.recycle()
                }
            }
        }
    }

    @Test
    fun videoShareAudioAssetsKenBurnsAndAppLinksAreValidWithoutQrCode() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Video durations: 15 / 30 / 60s
        assertEquals(listOf(15, 30, 60), ShareVideoDuration.entries.map { it.seconds })
        assertEquals(3, ShareVideoMusic.entries.size)
        assertEquals(5, VideoShareDestination.entries.size)

        // Verify both royalty-free CC0 .m4a tracks exist in res/raw and total size is tiny (< 1 MB)
        var totalAudioBytes = 0
        for (music in ShareVideoMusic.entries) {
            val rawId = music.rawResId
            if (rawId != null) {
                context.resources.openRawResource(rawId).use { stream ->
                    val bytes = stream.readBytes()
                    assertTrue("Audio track ${music.name} must be > 20 KB", bytes.size > 20_000)
                    totalAudioBytes += bytes.size
                }
            }
        }
        assertTrue(
            "Total bundled background music size must be well under 8 MB (got $totalAudioBytes bytes)",
            totalAudioBytes < 1_000_000
        )

        // Ken Burns zoom smoothly increases 1.00x -> 1.06x
        assertEquals(1.00f, ShareCardVideoGenerator.kenBurnsScale(0f), 0.001f)
        assertEquals(1.03f, ShareCardVideoGenerator.kenBurnsScale(0.5f), 0.002f)
        assertEquals(1.06f, ShareCardVideoGenerator.kenBurnsScale(1.0f), 0.001f)

        // Exact App Download URL appended to Bangla & English captions
        assertEquals(
            "https://github.com/ahmeddesignhouse-sys/nur-bangla-updates/releases/tag/v1.8",
            AppLinks.APP_DOWNLOAD_URL
        )
        DailyInspirationCatalog.initialize(context)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.OCTOBER, 13, 6, 0, 0)
        }
        val entry = DailyInspirationCatalog.forDate(cal)
        val captionBn = entry.formatShareText(isEnglish = false)
        val captionEn = entry.formatShareText(isEnglish = true)
        assertTrue("Bangla caption missing APP_DOWNLOAD_URL", captionBn.contains(AppLinks.APP_DOWNLOAD_URL))
        assertTrue("Bangla caption missing Bengali CTA", captionBn.contains("নূর বাংলা ডাউনলোড করুন"))
        assertTrue("English caption missing APP_DOWNLOAD_URL", captionEn.contains(AppLinks.APP_DOWNLOAD_URL))
        assertTrue("English caption missing English CTA", captionEn.contains("Download Nur Bangla"))
    }
}
