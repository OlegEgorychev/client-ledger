package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.ExpenseItemEntity

@Dao
interface ExpenseItemDao {
    @Query("SELECT * FROM expense_items WHERE expenseId = :expenseId ORDER BY id")
    suspend fun getItemsForExpense(expenseId: Long): List<ExpenseItemEntity>
    
    @Query("SELECT * FROM expense_items WHERE expenseId = :expenseId ORDER BY id")
    fun getItemsForExpenseFlow(expenseId: Long): kotlinx.coroutines.flow.Flow<List<ExpenseItemEntity>>
    
    @Insert
    suspend fun insertExpenseItem(item: ExpenseItemEntity): Long
    
    @Insert
    suspend fun insertExpenseItems(items: List<ExpenseItemEntity>)
    
    @Delete
    suspend fun deleteExpenseItem(item: ExpenseItemEntity)
    
    @Query("DELETE FROM expense_items WHERE expenseId = :expenseId")
    suspend fun deleteItemsForExpense(expenseId: Long)
}
