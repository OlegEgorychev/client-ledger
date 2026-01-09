package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.AppointmentServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentServiceDao {
    @Query("SELECT * FROM appointment_services WHERE appointmentId = :appointmentId ORDER BY sortOrder ASC")
    fun getServicesForAppointment(appointmentId: Long): Flow<List<AppointmentServiceEntity>>

    @Query("SELECT * FROM appointment_services WHERE appointmentId = :appointmentId ORDER BY sortOrder ASC")
    suspend fun getServicesForAppointmentSync(appointmentId: Long): List<AppointmentServiceEntity>

    @Query("SELECT * FROM appointment_services WHERE serviceTagId = :tagId")
    suspend fun getAppointmentsForTag(tagId: Long): List<AppointmentServiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: AppointmentServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<AppointmentServiceEntity>)

    @Delete
    suspend fun deleteService(service: AppointmentServiceEntity)

    @Query("DELETE FROM appointment_services WHERE appointmentId = :appointmentId")
    suspend fun deleteServicesForAppointment(appointmentId: Long)

    // Statistics: Income by tag for a period
    @Query(
        """
        SELECT 
            st.id as tagId,
            st.name as tagName,
            COALESCE(SUM(aps.priceForThisTag), 0) as totalIncome,
            COUNT(aps.id) as appointmentCount
        FROM service_tags st
        LEFT JOIN appointment_services aps ON st.id = aps.serviceTagId
        LEFT JOIN appointments a ON aps.appointmentId = a.id
        WHERE a.dateKey >= :startDate 
            AND a.dateKey <= :endDate 
            AND a.isPaid = 1 
            AND a.status != 'CANCELED'
            AND st.isActive = 1
        GROUP BY st.id, st.name
        HAVING totalIncome > 0
        ORDER BY totalIncome DESC
        """
    )
    suspend fun getIncomeByTag(startDate: String, endDate: String): List<TagIncome>

    // Top tags by income
    @Query(
        """
        SELECT 
            st.id as tagId,
            st.name as tagName,
            COALESCE(SUM(aps.priceForThisTag), 0) as totalIncome,
            COUNT(aps.id) as appointmentCount
        FROM service_tags st
        LEFT JOIN appointment_services aps ON st.id = aps.serviceTagId
        LEFT JOIN appointments a ON aps.appointmentId = a.id
        WHERE a.dateKey >= :startDate 
            AND a.dateKey <= :endDate 
            AND a.isPaid = 1 
            AND a.status != 'CANCELED'
            AND st.isActive = 1
        GROUP BY st.id, st.name
        HAVING totalIncome > 0
        ORDER BY totalIncome DESC
        LIMIT :limit
        """
    )
    suspend fun getTopTags(startDate: String, endDate: String, limit: Int = 10): List<TagIncome>
}

data class TagIncome(
    val tagId: Long,
    val tagName: String,
    val totalIncome: Long, // in cents
    val appointmentCount: Int
)
