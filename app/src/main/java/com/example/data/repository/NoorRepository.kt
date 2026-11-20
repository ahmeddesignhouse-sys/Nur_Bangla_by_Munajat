package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalAppSettings
import com.example.data.local.NoorDatabase
import com.example.data.local.PrayerLogEntity
import com.example.data.local.PreferencesDataStore
import com.example.data.local.QuranBookmarkEntity
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AsrMethod
import com.example.data.model.PrayerLogDoc
import com.example.data.model.PrayerStatus
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
    val remoteSync: FirestoreSyncRepository = FirestoreSyncRepository(context)
) {
    private val prayerLogDao = db.prayerLogDao()
    private val bookmarkDao = db.quranBookmarkDao()

    val settingsFlow: Flow<LocalAppSettings> = preferences.settingsFlow

    fun observeLocalPrayerLogs(userId: String): Flow<List<PrayerLogEntity>> =
        prayerLogDao.observeAllForUser(userId)

    fun observeLocalBookmarks(userId: String): Flow<List<QuranBookmarkEntity>> =
        bookmarkDao.observeAllForUser(userId)

    suspend fun getSurahs(): List<Surah> = quranRepo.loadSurahs()

    suspend fun searchQuran(query: String): List<QuranSearchHit> = quranRepo.searchVerses(query)

    suspend fun searchPrayerLogs(userId: String, query: String): List<PrayerLogEntity> =
        prayerLogDao.searchLogs(userId, query.trim())

    suspend fun updatePrayerStatus(
        userId: String,
        dateKey: String,
        waqt: WaqtType,
        status: PrayerStatus
    ): Result<Unit> {
        val current = prayerLogDao.getByDate(userId, dateKey)
            ?: PrayerLogEntity(dateKey = dateKey, userId = userId)
        val updated = current.withStatus(waqt, status).copy(
            updatedAtMillis = System.currentTimeMillis(),
            syncedToCloud = false
        )
        prayerLogDao.upsert(updated)
        return pushPrayerLogToCloud(userId, updated)
    }

    suspend fun updatePrayerNote(
        userId: String,
        dateKey: String,
        note: String
    ): Result<Unit> {
        val trimmed = note.take(500)
        val current = prayerLogDao.getByDate(userId, dateKey)
            ?: PrayerLogEntity(dateKey = dateKey, userId = userId)
        val updated = current.copy(
            note = trimmed,
            updatedAtMillis = System.currentTimeMillis(),
            syncedToCloud = false
        )
        prayerLogDao.upsert(updated)
        return pushPrayerLogToCloud(userId, updated)
    }

    private suspend fun pushPrayerLogToCloud(userId: String, entity: PrayerLogEntity): Result<Unit> {
        val doc = PrayerLogDoc(
            id = entity.dateKey,
            userId = userId,
            dateKey = entity.dateKey,
            fajrStatus = entity.fajrStatus,
            dhuhrStatus = entity.dhuhrStatus,
            asrStatus = entity.asrStatus,
            maghribStatus = entity.maghribStatus,
            ishaStatus = entity.ishaStatus,
            note = entity.note
        )
        val result = remoteSync.upsertPrayerLog(userId, doc)
        if (result.isSuccess) {
            prayerLogDao.upsert(entity.copy(syncedToCloud = true))
        }
        return result
    }

    suspend fun mergeRemotePrayerLogs(userId: String, remoteLogs: List<PrayerLogDoc>) {
        if (remoteLogs.isEmpty()) return
        val entities = remoteLogs.mapNotNull { doc ->
            if (doc.dateKey.isBlank()) return@mapNotNull null
            val remoteMillis = doc.updatedAt?.toDate()?.time ?: 0L
            val local = prayerLogDao.getByDate(userId, doc.dateKey)
            if (local == null || remoteMillis >= local.updatedAtMillis || local.syncedToCloud) {
                PrayerLogEntity(
                    dateKey = doc.dateKey,
                    userId = userId,
                    fajrStatus = doc.fajrStatus,
                    dhuhrStatus = doc.dhuhrStatus,
                    asrStatus = doc.asrStatus,
                    maghribStatus = doc.maghribStatus,
                    ishaStatus = doc.ishaStatus,
                    note = doc.note,
                    updatedAtMillis = if (remoteMillis > 0L) remoteMillis else System.currentTimeMillis(),
                    syncedToCloud = true
                )
            } else {
                null
            }
        }
        if (entities.isNotEmpty()) {
            prayerLogDao.upsertAll(entities)
        }
    }

    suspend fun toggleBookmark(
        userId: String,
        surah: Surah,
        ayahNumber: Int,
        arabicText: String,
        banglaText: String,
        currentlyBookmarked: Boolean
    ): Result<Unit> {
        val bookmarkId = "${surah.number}_$ayahNumber"
        return if (currentlyBookmarked) {
            bookmarkDao.deleteById(userId, bookmarkId)
            remoteSync.deleteBookmark(userId, bookmarkId)
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

    suspend fun mergeRemoteBookmarks(userId: String, remoteBookmarks: List<QuranBookmarkDoc>) {
        if (remoteBookmarks.isEmpty()) return
        val entities = remoteBookmarks.mapNotNull { doc ->
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
        val doc = UserPreferencesDoc(
            id = "user_settings",
            userId = userId,
            districtId = settings.districtId,
            asrMethod = settings.asrMethod.name,
            themeMode = settings.themeMode.name,
            accentTheme = settings.accentTheme.name,
            quranDarkMode = settings.quranDarkMode,
            quranFontSize = settings.quranFontSize.toLong(),
            prePrayerMinutes = settings.alertConfig.prePrayerMinutes.toLong()
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
            prePrayerMinutes = remote.prePrayerMinutes.toInt()
        )
    }

    suspend fun setWaqtAlertMode(waqt: WaqtType, mode: AdhanAlertMode) {
        preferences.setWaqtAlertMode(waqt, mode)
    }
}
