package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE dateKey = :dateKey ORDER BY spentAt")
    fun getExpensesByDate(dateKey: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses 
        WHERE dateKey >= :startDate AND dateKey <= :endDate 
        ORDER BY spentAt
        """
    )
    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(totalAmountCents), 0) FROM expenses 
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        """
    )
    suspend fun getExpensesForDateRange(startDate: String, endDate: String): Long

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}


