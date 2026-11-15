package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.PersonalEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalEventDao {
    @Query("SELECT * FROM personal_events WHERE id = :id")
    suspend fun getById(id: Long): PersonalEventEntity?

    @Query("SELECT * FROM personal_events WHERE dateKey = :dateKey ORDER BY isAllDay DESC, startMinute")
    fun getByDate(dateKey: String): Flow<List<PersonalEventEntity>>

    @Query("SELECT * FROM personal_events WHERE dateKey = :dateKey ORDER BY isAllDay DESC, startMinute")
    suspend fun getByDateOnce(dateKey: String): List<PersonalEventEntity>

    @Query(
        """
        SELECT DISTINCT dateKey FROM personal_events
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        """
    )
    suspend fun getDaysInRange(startDate: String, endDate: String): List<String>

    @Query("SELECT * FROM personal_events ORDER BY dateKey, startMinute")
    suspend fun getAll(): List<PersonalEventEntity>

    @Insert
    suspend fun insert(event: PersonalEventEntity): Long

    @Update
    suspend fun update(event: PersonalEventEntity)

    @Delete
    suspend fun delete(event: PersonalEventEntity)

    @Query("DELETE FROM personal_events")
    suspend fun deleteAll()
}
