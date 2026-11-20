package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PrayerStatus
import com.example.data.model.WaqtType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey val dateKey: String,
    val userId: String,
    val fajrStatus: String = PrayerStatus.NONE.name,
    val dhuhrStatus: String = PrayerStatus.NONE.name,
    val asrStatus: String = PrayerStatus.NONE.name,
    val maghribStatus: String = PrayerStatus.NONE.name,
    val ishaStatus: String = PrayerStatus.NONE.name,
    val note: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = false
) {
    fun statusFor(waqt: WaqtType): PrayerStatus {
        val raw = when (waqt) {
            WaqtType.FAJR -> fajrStatus
            WaqtType.DHUHR -> dhuhrStatus
            WaqtType.ASR -> asrStatus
            WaqtType.MAGHRIB -> maghribStatus
            WaqtType.ISHA -> ishaStatus
        }
        return PrayerStatus.safeValueOf(raw)
    }

    fun withStatus(waqt: WaqtType, status: PrayerStatus): PrayerLogEntity = when (waqt) {
        WaqtType.FAJR -> copy(fajrStatus = status.name)
        WaqtType.DHUHR -> copy(dhuhrStatus = status.name)
        WaqtType.ASR -> copy(asrStatus = status.name)
        WaqtType.MAGHRIB -> copy(maghribStatus = status.name)
        WaqtType.ISHA -> copy(ishaStatus = status.name)
    }
}

@Entity(tableName = "quran_bookmarks")
data class QuranBookmarkEntity(
    @PrimaryKey val bookmarkId: String, // e.g., "1_1"
    val userId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNameBn: String,
    val arabicText: String,
    val banglaText: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE userId = :userId ORDER BY dateKey DESC")
    fun observeAllForUser(userId: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE userId = :userId AND dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(userId: String, dateKey: String): PrayerLogEntity?

    @Query("SELECT * FROM prayer_logs WHERE userId = :userId AND (note LIKE '%' || :query || '%' OR dateKey LIKE '%' || :query || '%') ORDER BY dateKey DESC")
    suspend fun searchLogs(userId: String, query: String): List<PrayerLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrayerLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PrayerLogEntity>)
}

@Dao
interface QuranBookmarkDao {
    @Query("SELECT * FROM quran_bookmarks WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeAllForUser(userId: String): Flow<List<QuranBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuranBookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<QuranBookmarkEntity>)

    @Query("DELETE FROM quran_bookmarks WHERE userId = :userId AND bookmarkId = :bookmarkId")
    suspend fun deleteById(userId: String, bookmarkId: String)
}

@Database(
    entities = [PrayerLogEntity::class, QuranBookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NoorDatabase : RoomDatabase() {
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun quranBookmarkDao(): QuranBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: NoorDatabase? = null

        fun getInstance(context: Context): NoorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoorDatabase::class.java,
                    "noor_bangla_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
