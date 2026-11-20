package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read localized app name for both Bangla and English`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()

        val bnConfig = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("bn"))
        }
        val bnContext = baseContext.createConfigurationContext(bnConfig)
        assertEquals("নূর বাংলা", bnContext.getString(R.string.app_name))

        val enConfig = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }
        val enContext = baseContext.createConfigurationContext(enConfig)
        assertEquals("Nur Bangla", enContext.getString(R.string.app_name))
    }

    @Test
    fun `offline quran loads both bangla and english translations and searches both`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = QuranRepository(context)
        val surahs = repo.loadSurahs()
        assertEquals(18, surahs.size)
        surahs.forEach { surah ->
            assertTrue("Missing English Surah name for ${surah.number}", surah.nameEnglish.isNotBlank())
            surah.ayahs.forEach { ayah ->
                assertTrue("Missing Arabic for ${surah.number}:${ayah.number}", ayah.arabic.isNotBlank())
                assertTrue("Missing Bangla for ${surah.number}:${ayah.number}", ayah.bangla.isNotBlank())
                assertTrue("Missing English for ${surah.number}:${ayah.number}", ayah.english.isNotBlank())
            }
        }

        val bnHits = repo.searchVerses("করুণাময়")
        assertTrue(bnHits.isNotEmpty())

        val enHits = repo.searchVerses("straight path")
        assertTrue(enHits.isNotEmpty())
        assertEquals(1, enHits.first().surah.number)
        assertEquals(6, enHits.first().ayah.number)
    }

    @Test
    fun `language preference defaults to bn and persists en in datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesDataStore(context)
        assertEquals("bn", prefs.settingsFlow.first().languageCode)

        prefs.setLanguageCode("en")
        assertEquals("en", prefs.settingsFlow.first().languageCode)

        prefs.setLanguageCode("bn")
        assertEquals("bn", prefs.settingsFlow.first().languageCode)
    }

    @Test
    fun `dua library covers all 10 categories searches bilingually and bookmarks locally`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val duaRepo = com.example.data.repository.DuaRepository(context)
        val allDuas = duaRepo.loadAllDuas()

        assertTrue("Expected at least 30 authentic duas", allDuas.size >= 30)
        val coveredCategories = allDuas.map { it.category }.toSet()
        com.example.data.model.DuaCategory.entries.forEach { cat ->
            assertTrue("Missing category: ${cat.key}", coveredCategories.contains(cat))
        }
        allDuas.forEach { dua ->
            assertTrue("Missing Arabic for ${dua.id}", dua.arabic.isNotBlank())
            assertTrue("Missing Bangla meaning for ${dua.id}", dua.meaningBn.isNotBlank())
            assertTrue("Missing English meaning for ${dua.id}", dua.meaningEn.isNotBlank())
            assertTrue("Missing Bangla title for ${dua.id}", dua.titleBn.isNotBlank())
            assertTrue("Missing English title for ${dua.id}", dua.titleEn.isNotBlank())
        }

        // Bilingual search
        val enSearch = duaRepo.filterDuas(allDuas, "Iftar", null, false, emptySet())
        assertTrue(enSearch.isNotEmpty())
        val bnSearch = duaRepo.filterDuas(allDuas, "ইফতার", null, false, emptySet())
        assertTrue(bnSearch.isNotEmpty())

        // Local bookmark persistence in DataStore
        val prefs = PreferencesDataStore(context)
        val added = prefs.toggleFavoriteDua("ramadan_1")
        assertTrue(added)
        assertTrue(prefs.settingsFlow.first().favoriteDuaIds.contains("ramadan_1"))

        val favOnly = duaRepo.filterDuas(allDuas, "", null, true, prefs.settingsFlow.first().favoriteDuaIds)
        assertEquals(1, favOnly.size)
        assertEquals("ramadan_1", favOnly.first().id)

        val removed = prefs.toggleFavoriteDua("ramadan_1")
        assertEquals(false, removed)
        assertEquals(false, prefs.settingsFlow.first().favoriteDuaIds.contains("ramadan_1"))
    }

    @Test
    fun `asmaul husna has all 99 names with bilingual meanings and search works`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.AsmaulHusnaRepository(context)
        val names = repo.loadAllNames()

        assertEquals(99, names.size)
        assertEquals((1..99).toList(), names.map { it.number })
        names.forEach { item ->
            assertTrue("Missing Arabic for #${item.number}", item.arabic.isNotBlank())
            assertTrue("Missing transliteration for #${item.number}", item.transliteration.isNotBlank())
            assertTrue("Missing Bangla transliteration for #${item.number}", item.transliterationBn.isNotBlank())
            assertTrue("Missing Bangla meaning for #${item.number}", item.meaningBn.isNotBlank())
            assertTrue("Missing English meaning for #${item.number}", item.meaningEn.isNotBlank())
            assertTrue("Missing Bangla detail for #${item.number}", item.detailBn.isNotBlank())
            assertTrue("Missing English detail for #${item.number}", item.detailEn.isNotBlank())
        }

        assertEquals(1, repo.filterNames(names, "Ar-Rahman").first().number)
        assertEquals(99, repo.filterNames(names, "৯৯").first().number)
        assertEquals(5, repo.filterNames(names, "শান্তিদাতা").first().number)
        assertTrue(repo.filterNames(names, "Merciful").isNotEmpty())
    }

    @Test
    fun `hijri day adjustment persists in datastore and clamps to plus minus 1`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesDataStore(context)
        assertEquals(0, prefs.settingsFlow.first().hijriDayOffset)

        prefs.setHijriDayOffset(1)
        assertEquals(1, prefs.settingsFlow.first().hijriDayOffset)

        prefs.setHijriDayOffset(-1)
        assertEquals(-1, prefs.settingsFlow.first().hijriDayOffset)

        prefs.setHijriDayOffset(5)
        assertEquals(1, prefs.settingsFlow.first().hijriDayOffset)

        prefs.setHijriDayOffset(0)
        assertEquals(0, prefs.settingsFlow.first().hijriDayOffset)
    }

    @Test
    fun `ramadan mode metal prices and zakat history persist locally in datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesDataStore(context)

        assertEquals(false, prefs.settingsFlow.first().ramadanMode)
        prefs.setRamadanMode(true)
        assertEquals(true, prefs.settingsFlow.first().ramadanMode)

        prefs.setMetalPrices(14200.0, 225.0)
        val afterRates = prefs.settingsFlow.first()
        assertEquals(14200.0, afterRates.goldPricePerGramBdt, 0.01)
        assertEquals(225.0, afterRates.silverPricePerGramBdt, 0.01)

        val record = com.example.util.ZakatHistoryRecord(
            id = "test_zakat_1",
            timestampMillis = 1795132800000L,
            netWealthBdt = 775_000.0,
            nisabThresholdBdt = 137_781.0,
            zakatDueBdt = 19_375.0,
            nisabBasis = com.example.util.NisabBasis.SILVER
        )
        prefs.addZakatHistoryRecord(record)
        val savedList = com.example.util.ZakatCalculator.parseHistoryJson(prefs.settingsFlow.first().zakatHistoryJson)
        assertEquals(1, savedList.size)
        assertEquals("test_zakat_1", savedList.first().id)

        prefs.deleteZakatHistoryRecord("test_zakat_1")
        val afterDelete = com.example.util.ZakatCalculator.parseHistoryJson(prefs.settingsFlow.first().zakatHistoryJson)
        assertTrue(afterDelete.isEmpty())
    }
}
