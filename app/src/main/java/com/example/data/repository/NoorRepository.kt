package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalAppSettings
import com.example.data.local.NoorDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.local.QuranBookmarkEntity
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AsrMethod
import com.example.data.model.QuranBookmarkDoc
import com.example.data.model.QuranSearchHit
import com.example.data.model.Surah
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.WaqtType
import com.example.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.flow.Flow

class NoorRepository(
    context: Context,
    private val db: NoorDatabase = NoorDatabase.getInstance(context),
    val preferences: PreferencesDataStore = PreferencesDataStore(context),
    private val quranRepo: QuranRepository = QuranRepository(context),
    remoteSyncOverride: FirestoreSyncRepository? = null
) {
    private val appContext = context.applicationContext
    private val bookmarkDao = db.quranBookmarkDao()

    val remoteSync: FirestoreSyncRepository by lazy {
        remoteSyncOverride ?: FirestoreSyncRepository(appContext)
    }

    val settingsFlow: Flow<LocalAppSettings> = preferences.settingsFlow

    fun observeLocalBookmarks(userId: String): Flow<List<QuranBookmarkEntity>> =
        bookmarkDao.observeAllForUser(userId)

    suspend fun getLocalBookmarksOnce(userId: String): List<QuranBookmarkEntity> =
        bookmarkDao.getAllForUserOnce(userId)

    suspend fun getSurahs(): List<Surah> = quranRepo.loadSurahs()

    suspend fun searchQuran(query: String): List<QuranSearchHit> = quranRepo.searchVerses(query)

    suspend fun toggleBookmark(
        userId: String,
        surah: Surah,
        ayahNumber: Int,
        arabicText: String,
        banglaText: String,
        currentlyBookmarked: Boolean
    ): Result<Unit> {
        val bookmarkId = "${surah.number}_$ayahNumber"
        val isGuest = LocalAppSettings.isGuestUserId(userId)
        return if (currentlyBookmarked) {
            bookmarkDao.deleteById(userId, bookmarkId)
            if (isGuest) Result.success(Unit)
            else remoteSync.deleteBookmark(userId, bookmarkId)
        } else {
            val entity = QuranBookmarkEntity(
                bookmarkId = bookmarkId,
                userId = userId,
                surahNumber = surah.number,
                ayahNumber = ayahNumber,
                surahNameBn = surah.nameBangla,
                arabicText = arabicText,
                banglaText = banglaText
            )
            bookmarkDao.upsert(entity)
            if (isGuest) {
                Result.success(Unit)
            } else {
                remoteSync.upsertBookmark(
                    userId,
                    bookmarkId,
                    QuranBookmarkDoc(
                        id = bookmarkId,
                        userId = userId,
                        surahNumber = surah.number.toLong(),
                        ayahNumber = ayahNumber.toLong(),
                        surahNameBn = surah.nameBangla,
                        arabicText = arabicText,
                        banglaText = banglaText
                    )
                )
            }
        }
    }

    /**
     * Reassigns local Room bookmarks from one local userId to another without deleting any data.
     * Used both when a Guest signs in with Google and when a signed-in user signs out to Guest Mode.
     */
    suspend fun migrateLocalBookmarksToUser(
        sourceUserId: String,
        targetUserId: String
    ): List<QuranBookmarkEntity> {
        if (sourceUserId.isNotBlank() && targetUserId.isNotBlank() && sourceUserId != targetUserId) {
            bookmarkDao.migrateUserBookmarks(sourceUserId, targetUserId)
        }
        return bookmarkDao.getAllForUserOnce(targetUserId)
    }

    /**
     * Merges and uploads local guest bookmarks and settings to the newly signed-in Google account's
     * Firestore space without deleting any local data.
     */
    suspend fun uploadLocalGuestDataToCloud(
        guestUserId: String,
        signedInUserId: String,
        settings: LocalAppSettings
    ): Result<Int> {
        if (LocalAppSettings.isGuestUserId(signedInUserId) || signedInUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Cloud upload requires a signed-in account."))
        }
        val mergedBookmarks = migrateLocalBookmarksToUser(guestUserId, signedInUserId)
        var firstFailure: Throwable? = null
        var uploadedCount = 0

        mergedBookmarks.forEach { entity ->
            val res = remoteSync.upsertBookmark(
                userId = signedInUserId,
                bookmarkId = entity.bookmarkId,
                doc = QuranBookmarkDoc(
                    id = entity.bookmarkId,
                    userId = signedInUserId,
                    surahNumber = entity.surahNumber.toLong(),
                    ayahNumber = entity.ayahNumber.toLong(),
                    surahNameBn = entity.surahNameBn,
                    arabicText = entity.arabicText,
                    banglaText = entity.banglaText
                )
            )
            if (res.isSuccess) {
                uploadedCount++
            } else if (firstFailure == null) {
                firstFailure = res.exceptionOrNull()
            }
        }

        val prefsRes = syncPreferencesToCloud(signedInUserId, settings)
        if (prefsRes.isFailure && firstFailure == null) {
            firstFailure = prefsRes.exceptionOrNull()
        }

        return if (firstFailure == null) {
            Result.success(uploadedCount)
        } else {
            Result.failure(firstFailure)
        }
    }

    suspend fun mergeRemoteBookmarks(userId: String, remoteBookmarks: List<QuranBookmarkDoc>) {
        if (remoteBookmarks.isEmpty()) return
        val entities = remoteBookmarks.map { doc ->
            val bId = doc.id.ifBlank { "${doc.surahNumber}_${doc.ayahNumber}" }
            QuranBookmarkEntity(
                bookmarkId = bId,
                userId = userId,
                surahNumber = doc.surahNumber.toInt(),
                ayahNumber = doc.ayahNumber.toInt(),
                surahNameBn = doc.surahNameBn,
                arabicText = doc.arabicText,
                banglaText = doc.banglaText,
                createdAtMillis = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
        bookmarkDao.upsertAll(entities)
    }

    suspend fun syncPreferencesToCloud(userId: String, settings: LocalAppSettings): Result<Unit> {
        if (LocalAppSettings.isGuestUserId(userId) || userId.isBlank()) {
            return Result.success(Unit)
        }
        val doc = UserPreferencesDoc(
            id = "user_settings",
            userId = userId,
            districtId = settings.districtId,
            asrMethod = settings.asrMethod.name,
            themeMode = settings.themeMode.name,
            accentTheme = settings.accentTheme.name,
            quranDarkMode = settings.quranDarkMode,
            quranFontSize = settings.quranFontSize.toLong(),
            prePrayerMinutes = settings.alertConfig.prePrayerMinutes.toLong(),
            languageCode = settings.languageCode
        )
        return remoteSync.savePreferences(userId, doc)
    }

    suspend fun applyRemotePreferences(remote: UserPreferencesDoc) {
        preferences.applyRemotePreferences(
            districtId = remote.districtId.ifBlank { "dhaka" },
            asrMethod = AsrMethod.safeValueOf(remote.asrMethod),
            themeMode = ThemeMode.safeValueOf(remote.themeMode),
            accentTheme = AccentTheme.safeValueOf(remote.accentTheme),
            quranDarkMode = remote.quranDarkMode,
            quranFontSize = remote.quranFontSize.toInt(),
            prePrayerMinutes = remote.prePrayerMinutes.toInt(),
            languageCode = remote.languageCode
        )
    }

    suspend fun setWaqtAlertMode(waqt: WaqtType, mode: AdhanAlertMode) {
        preferences.setWaqtAlertMode(waqt, mode)
    }
}
