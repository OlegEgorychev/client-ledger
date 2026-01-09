package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.ServiceTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceTagDao {
    @Query("SELECT * FROM service_tags WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllActiveTags(): Flow<List<ServiceTagEntity>>

    @Query("SELECT * FROM service_tags ORDER BY sortOrder ASC, name ASC")
    fun getAllTags(): Flow<List<ServiceTagEntity>>

    @Query("SELECT * FROM service_tags WHERE id = :id")
    suspend fun getTagById(id: Long): ServiceTagEntity?

    @Query("SELECT * FROM service_tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): ServiceTagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: ServiceTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<ServiceTagEntity>)

    @Update
    suspend fun updateTag(tag: ServiceTagEntity)

    @Delete
    suspend fun deleteTag(tag: ServiceTagEntity)

    @Query("UPDATE service_tags SET isActive = :isActive WHERE id = :id")
    suspend fun setTagActive(id: Long, isActive: Boolean)
}
