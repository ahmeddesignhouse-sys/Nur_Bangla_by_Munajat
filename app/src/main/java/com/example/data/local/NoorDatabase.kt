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
import kotlinx.coroutines.flow.Flow

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
interface QuranBookmarkDao {
    @Query("SELECT * FROM quran_bookmarks WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeAllForUser(userId: String): Flow<List<QuranBookmarkEntity>>

    @Query("SELECT * FROM quran_bookmarks WHERE userId = :userId ORDER BY createdAtMillis DESC")
    suspend fun getAllForUserOnce(userId: String): List<QuranBookmarkEntity>

    @Query("UPDATE quran_bookmarks SET userId = :targetUserId WHERE userId = :sourceUserId")
    suspend fun migrateUserBookmarks(sourceUserId: String, targetUserId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuranBookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<QuranBookmarkEntity>)

    @Query("DELETE FROM quran_bookmarks WHERE userId = :userId AND bookmarkId = :bookmarkId")
    suspend fun deleteById(userId: String, bookmarkId: String)
}

@Database(
    entities = [QuranBookmarkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NoorDatabase : RoomDatabase() {
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
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
