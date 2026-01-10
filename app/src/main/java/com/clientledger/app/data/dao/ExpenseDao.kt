package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.entity.ExpenseTag
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
    
    @Query("SELECT * FROM expenses ORDER BY spentAt")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query(
        """
        SELECT COALESCE(SUM(totalAmountCents), 0) FROM expenses 
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        """
    )
    suspend fun getExpensesForDateRange(startDate: String, endDate: String): Long

    @Query(
        """
        SELECT DISTINCT dateKey FROM expenses 
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        """
    )
    suspend fun getExpenseDaysInRange(startDate: String, endDate: String): List<String>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Expenses by month for pie chart
    @Query(
        """
        SELECT 
            SUBSTR(dateKey, 1, 7) as monthKey,
            COALESCE(SUM(totalAmountCents), 0) as totalExpenses
        FROM expenses 
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        GROUP BY monthKey
        ORDER BY monthKey ASC
        """
    )
    suspend fun getExpensesByMonth(startDate: String, endDate: String): List<MonthExpense>

    // Expenses by tag for pie chart
    // Note: tag is stored as string in Room, so we select it as string and convert to enum in Repository
    @Query(
        """
        SELECT 
            ei.tag as tagName,
            COALESCE(SUM(ei.amountCents), 0) as totalAmount
        FROM expense_items ei
        INNER JOIN expenses e ON ei.expenseId = e.id
        WHERE e.dateKey >= :startDate AND e.dateKey <= :endDate
        GROUP BY ei.tag
        ORDER BY totalAmount DESC
        """
    )
    suspend fun getExpensesByTagRaw(startDate: String, endDate: String): List<TagExpenseRaw>
}

// Data classes for expense aggregation
data class MonthExpense(
    val monthKey: String, // "YYYY-MM" format
    val totalExpenses: Long
)

data class TagExpense(
    val tag: ExpenseTag,
    val totalAmount: Long
)

// Raw data class for query result (tag as string)
data class TagExpenseRaw(
    val tagName: String,
    val totalAmount: Long
)

