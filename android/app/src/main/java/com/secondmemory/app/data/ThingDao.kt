package com.secondmemory.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ThingDao {
    @Query("SELECT * FROM things ORDER BY createdAt DESC")
    fun observeThings(): Flow<List<ThingEntity>>

    @Query("SELECT * FROM things ORDER BY createdAt DESC")
    suspend fun getThings(): List<ThingEntity>

    @Query("SELECT * FROM things WHERE id = :id")
    suspend fun getThing(id: String): ThingEntity?

    @Query("SELECT * FROM things WHERE id = :id")
    fun observeThing(id: String): Flow<ThingEntity?>

    @Query("SELECT COUNT(*) FROM things WHERE status IN ('INBOX','ACTIVE')")
    suspend fun activeCount(): Int

    @Query("SELECT * FROM things WHERE sourceUrl IS NOT NULL AND status != 'ARCHIVED'")
    suspend fun thingsWithUrl(): List<ThingEntity>

    @Query("SELECT COALESCE(MAX(notifId), 100) FROM things")
    suspend fun maxNotifId(): Int

    @Query("SELECT COALESCE(MIN(sortOrder), 0) FROM things")
    suspend fun minSortOrder(): Int

    @Query(
        """
        UPDATE things SET processingStatus = 'FAILED', processingError = 'stale'
        WHERE processingStatus = 'PROCESSING' AND updatedAt < :staleBefore
        """,
    )
    suspend fun failStaleProcessing(staleBefore: Long): Int

    @Query("SELECT * FROM things WHERE notifId = 0")
    suspend fun missingNotifIds(): List<ThingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ThingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ThingEntity>)

    @Query("DELETE FROM things WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM things")
    suspend fun deleteAll()

    @Query("SELECT imageUri FROM things WHERE imageUri IS NOT NULL")
    suspend fun allImageUris(): List<String>

    @Query("SELECT * FROM activities ORDER BY at DESC LIMIT 400")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(entity: ActivityEntity)

    @Query("DELETE FROM activities")
    suspend fun deleteActivities()

    @Transaction
    suspend fun replace(entity: ThingEntity) {
        upsert(entity)
    }
}
