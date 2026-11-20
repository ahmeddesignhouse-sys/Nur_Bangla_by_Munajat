package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalAppSettings
import com.example.data.local.NoorDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.model.Surah
import com.example.data.repository.NoorRepository
import com.example.ui.AppSessionState
import com.example.ui.resolveAppSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorV16FeatureTest {

    @Test
    fun sessionResolverHandlesFreshInstallGuestMigrationAndSignOutWithoutLockout() {
        // 1. Fresh install, no Google account, Guest not tapped yet -> AuthScreen
        assertEquals(
            AppSessionState.Unauthenticated,
            resolveAppSession(firebaseUid = null, isGuestMode = false, guestUserId = "")
        )

        // 2. User taps Continue as Guest -> Guest Mode with persistent guest ID
        assertEquals(
            AppSessionState.Guest("guest-1234"),
            resolveAppSession(firebaseUid = null, isGuestMode = true, guestUserId = "guest-1234")
        )

        // 3. Guest signs in with Google -> triggers GuestPendingMigration before switching
        assertEquals(
            AppSessionState.GuestPendingMigration("guest-1234", "firebase-uid-99"),
            resolveAppSession(firebaseUid = "firebase-uid-99", isGuestMode = true, guestUserId = "guest-1234")
        )

        // 4. After migration finishes (isGuestMode = false) -> SignedIn
        assertEquals(
            AppSessionState.SignedIn("firebase-uid-99"),
            resolveAppSession(firebaseUid = "firebase-uid-99", isGuestMode = false, guestUserId = "guest-1234")
        )

        // 5. Signed-in user taps Sign Out -> transitions directly into Guest Mode (no AuthScreen lock-out)
        assertEquals(
            AppSessionState.Guest("guest-1234"),
            resolveAppSession(
                firebaseUid = "firebase-uid-99",
                isGuestMode = true,
                guestUserId = "guest-1234",
                isSigningOut = true
            )
        )
        assertEquals(
            AppSessionState.Guest("guest-1234"),
            resolveAppSession(
                firebaseUid = null,
                isGuestMode = true,
                guestUserId = "guest-1234",
                isSigningOut = false
            )
        )
    }

    @Test
    fun guestModePersistsUuidLanguageDistrictAndBookmarksOfflineWithoutFirestore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PreferencesDataStore(context)
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, NoorDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val guestId = store.enableGuestMode()
            assertTrue("Expected guest- prefix", guestId.startsWith(LocalAppSettings.GUEST_ID_PREFIX))
            assertTrue(LocalAppSettings.isGuestUserId(guestId))

            // Calling enableGuestMode again reuses the same stable UUID
            assertEquals(guestId, store.enableGuestMode())

            // Change language & district in Guest Mode
            store.setLanguageCode("en")
            store.setDistrict("sylhet")

            val savedSettings = store.settingsFlow.first()
            assertTrue(savedSettings.isGuestMode)
            assertEquals(guestId, savedSettings.guestUserId)
            assertEquals("en", savedSettings.languageCode)
            assertEquals("sylhet", savedSettings.districtId)

            // NoorRepository with NO Firebase/Firestore initialized must work completely offline for Guest
            val repo = NoorRepository(context = context, db = inMemoryDb, preferences = store)
            val fatihah = Surah(
                number = 1,
                nameArabic = "الفاتحة",
                nameBangla = "আল-ফাতিহা",
                nameEnglish = "Al-Fatihah",
                revelationTypeBn = "মাক্কী",
                revelationTypeEn = "Makki",
                totalAyahs = 7,
                ayahs = emptyList()
            )
            val bookmarkRes = repo.toggleBookmark(
                userId = guestId,
                surah = fatihah,
                ayahNumber = 1,
                arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                banglaText = "পরম করুণাময় অসীম দয়ালু আল্লাহর নামে",
                currentlyBookmarked = false
            )
            assertTrue(bookmarkRes.isSuccess)
            assertTrue(repo.syncPreferencesToCloud(guestId, savedSettings).isSuccess)

            val guestBookmarks = repo.getLocalBookmarksOnce(guestId)
            assertEquals(1, guestBookmarks.size)
            assertEquals("1_1", guestBookmarks.first().bookmarkId)

            // Migrate guest bookmarks to signed-in account (no deletion of local data)
            val signedInUid = "google-uid-abc"
            val migratedToGoogle = repo.migrateLocalBookmarksToUser(guestId, signedInUid)
            assertEquals(1, migratedToGoogle.size)
            assertEquals(signedInUid, migratedToGoogle.first().userId)

            // Sign out back to Guest Mode -> bookmarks are preserved for Guest Mode!
            val migratedBackToGuest = repo.migrateLocalBookmarksToUser(signedInUid, guestId)
            assertEquals(1, migratedBackToGuest.size)
            assertEquals(guestId, migratedBackToGuest.first().userId)

            // Disable guest mode keeps guestUserId intact
            store.setGuestModeEnabled(false)
            val afterDisable = store.settingsFlow.first()
            assertFalse(afterDisable.isGuestMode)
            assertEquals(guestId, afterDisable.guestUserId)
        } finally {
            inMemoryDb.close()
        }
    }
}
