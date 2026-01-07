package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE dateKey = :dateKey ORDER BY startsAt")
    fun getAppointmentsByDate(dateKey: String): Flow<List<AppointmentEntity>>

    @Query(
        """
        SELECT * FROM appointments 
        WHERE dateKey = :dateKey 
        AND (:excludeId IS NULL OR id != :excludeId)
        ORDER BY startsAt
        """
    )
    suspend fun getAppointmentsByDateExcluding(dateKey: String, excludeId: Long?): List<AppointmentEntity>

    @Query(
        """
        SELECT * FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate 
        ORDER BY startsAt
        """
    )
    fun getAppointmentsByDateRange(startDate: String, endDate: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE clientId = :clientId ORDER BY startsAt DESC")
    fun getAppointmentsByClient(clientId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT COUNT(*) FROM appointments WHERE clientId = :clientId")
    suspend fun getAppointmentsCountByClient(clientId: Long): Int

    @Query("SELECT COALESCE(SUM(incomeCents), 0) FROM appointments WHERE clientId = :clientId AND isPaid = 1 AND status != 'CANCELED'")
    suspend fun getTotalIncomeByClient(clientId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(incomeCents), 0) FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND isPaid = 1 AND status != 'CANCELED'
        """
    )
    suspend fun getIncomeForDateRange(startDate: String, endDate: String): Long

    @Query(
        """
        SELECT dateKey, SUM(incomeCents) as totalIncome 
        FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND isPaid = 1 AND incomeCents > 0 AND status != 'CANCELED'
        GROUP BY dateKey
        ORDER BY totalIncome DESC
        LIMIT 1
        """
    )
    suspend fun getMostProfitableDayByIncome(startDate: String, endDate: String): DayIncome?

    @Query(
        """
        SELECT a.dateKey, (COALESCE(SUM(a.incomeCents), 0) - COALESCE(SUM(e.amountCents), 0)) as profit
        FROM appointments a
        LEFT JOIN expenses e ON a.dateKey = e.dateKey
        WHERE a.dateKey >= :startDate AND a.dateKey <= :endDate AND a.isPaid = 1 AND a.status != 'CANCELED'
        GROUP BY a.dateKey
        ORDER BY profit DESC
        LIMIT 1
        """
    )
    suspend fun getMostProfitableDayByProfit(startDate: String, endDate: String): DayProfit?

    @Query(
        """
        SELECT COUNT(DISTINCT dateKey) FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND isPaid = 1 AND incomeCents > 0 AND status != 'CANCELED'
        """
    )
    suspend fun getWorkingDaysCount(startDate: String, endDate: String): Int

    @Query(
        """
        SELECT DISTINCT dateKey FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        """
    )
    suspend fun getWorkingDaysInRange(startDate: String, endDate: String): List<String>

    // Income series for line chart (income over time)
    @Query(
        """
        SELECT dateKey, COALESCE(SUM(incomeCents), 0) as totalIncome
        FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND isPaid = 1 AND status != 'CANCELED'
        GROUP BY dateKey
        ORDER BY dateKey ASC
        """
    )
    suspend fun getIncomeSeries(startDate: String, endDate: String): List<DayIncome>

    // Income by client for donut chart
    @Query(
        """
        SELECT 
            c.id as clientId,
            c.firstName || ' ' || c.lastName as clientName,
            COALESCE(SUM(a.incomeCents), 0) as totalIncome,
            COUNT(a.id) as visitCount
        FROM appointments a
        INNER JOIN clients c ON a.clientId = c.id
        WHERE a.dateKey >= :startDate AND a.dateKey <= :endDate AND a.isPaid = 1 AND a.status != 'CANCELED'
        GROUP BY c.id, c.firstName, c.lastName
        ORDER BY totalIncome DESC
        """
    )
    suspend fun getIncomeByClient(startDate: String, endDate: String): List<ClientIncome>

    // Summary statistics
    @Query(
        """
        SELECT 
            COALESCE(SUM(incomeCents), 0) as totalIncome,
            COUNT(*) as totalVisits,
            COUNT(DISTINCT clientId) as totalClients
        FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND isPaid = 1 AND status != 'CANCELED'
        """
    )
    suspend fun getSummaryStats(startDate: String, endDate: String): SummaryStats
    
    // Cancellation statistics
    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND status = 'CANCELED'
        """
    )
    suspend fun getCancellationsCount(startDate: String, endDate: String): Int
    
    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        """
    )
    suspend fun getTotalAppointmentsCount(startDate: String, endDate: String): Int
    
    // Cancellations series for analytics
    @Query(
        """
        SELECT dateKey, COUNT(*) as cancellationsCount
        FROM appointments 
        WHERE dateKey >= :startDate AND dateKey <= :endDate AND status = 'CANCELED'
        GROUP BY dateKey
        ORDER BY dateKey ASC
        """
    )
    suspend fun getCancellationsSeries(startDate: String, endDate: String): List<DayCancellations>

    @Insert
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)
}

data class DayIncome(
    val dateKey: String,
    val totalIncome: Long
)

data class DayProfit(
    val dateKey: String,
    val profit: Long
)

data class ClientIncome(
    val clientId: Long,
    val clientName: String,
    val totalIncome: Long,
    val visitCount: Int
)

data class SummaryStats(
    val totalIncome: Long,
    val totalVisits: Int,
    val totalClients: Int
)

data class DayCancellations(
    val dateKey: String,
    val cancellationsCount: Int
)


