package com.secondmemory.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ThingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ThingEntity>)

    @Query("DELETE FROM things WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM things")
    suspend fun deleteAll()

    @Query("SELECT * FROM activities ORDER BY at DESC LIMIT 400")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(entity: ActivityEntity)

    @Query("DELETE FROM activities")
    suspend fun deleteActivities()
}
