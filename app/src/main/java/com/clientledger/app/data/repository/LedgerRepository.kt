package com.clientledger.app.data.repository

import com.clientledger.app.data.dao.AppointmentDao
import com.clientledger.app.data.dao.ClientDao
import com.clientledger.app.data.dao.ClientIncome
import com.clientledger.app.data.dao.DayIncome
import com.clientledger.app.data.dao.DayProfit
import com.clientledger.app.data.dao.ExpenseDao
import com.clientledger.app.data.dao.SummaryStats
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class LedgerRepository(
    private val clientDao: ClientDao,
    private val appointmentDao: AppointmentDao,
    private val expenseDao: ExpenseDao
) {
    // Clients
    fun getAllClients(): Flow<List<ClientEntity>> = clientDao.getAllClients()
    
    suspend fun getClientById(id: Long): ClientEntity? = clientDao.getClientById(id)
    
    fun searchClients(query: String): Flow<List<ClientEntity>> = clientDao.searchClients(query)
    
    suspend fun getClientByPhone(phone: String): ClientEntity? = clientDao.getClientByPhone(phone)
    
    suspend fun findClientByName(firstName: String, lastName: String): ClientEntity? =
        clientDao.findClientByName(firstName, lastName)
    
    suspend fun insertClient(client: ClientEntity): Long = clientDao.insertClient(client)
    
    suspend fun updateClient(client: ClientEntity) = clientDao.updateClient(client)
    
    suspend fun deleteClient(client: ClientEntity) = clientDao.deleteClient(client)

    // Appointments
    fun getAppointmentsByDate(dateKey: String): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByDate(dateKey)
    
    suspend fun getAppointmentsByDateExcluding(dateKey: String, excludeId: Long?): List<AppointmentEntity> =
        appointmentDao.getAppointmentsByDateExcluding(dateKey, excludeId)
    
    fun getAppointmentsByDateRange(startDate: String, endDate: String): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByDateRange(startDate, endDate)
    
    fun getAppointmentsByClient(clientId: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.getAppointmentsByClient(clientId)
    
    suspend fun getAppointmentsCountByClient(clientId: Long): Int =
        appointmentDao.getAppointmentsCountByClient(clientId)
    
    suspend fun getTotalIncomeByClient(clientId: Long): Long =
        appointmentDao.getTotalIncomeByClient(clientId)
    
    suspend fun getAppointmentById(id: Long): AppointmentEntity? =
        appointmentDao.getAppointmentById(id)
    
    suspend fun insertAppointment(appointment: AppointmentEntity): Long =
        appointmentDao.insertAppointment(appointment)
    
    suspend fun updateAppointment(appointment: AppointmentEntity) =
        appointmentDao.updateAppointment(appointment)
    
    suspend fun deleteAppointment(appointment: AppointmentEntity) =
        appointmentDao.deleteAppointment(appointment)

    // Expenses
    fun getExpensesByDate(dateKey: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByDate(dateKey)
    
    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)
    
    suspend fun getExpenseById(id: Long): ExpenseEntity? = expenseDao.getExpenseById(id)
    
    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    
    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)
    
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    // Statistics
    suspend fun getIncomeForDateRange(startDate: String, endDate: String): Long =
        appointmentDao.getIncomeForDateRange(startDate, endDate)
    
    suspend fun getExpensesForDateRange(startDate: String, endDate: String): Long =
        expenseDao.getExpensesForDateRange(startDate, endDate)
    
    suspend fun getMostProfitableDayByIncome(startDate: String, endDate: String): DayIncome? =
        appointmentDao.getMostProfitableDayByIncome(startDate, endDate)
    
    suspend fun getMostProfitableDayByProfit(startDate: String, endDate: String): DayProfit? =
        appointmentDao.getMostProfitableDayByProfit(startDate, endDate)
    
    suspend fun getWorkingDaysCount(startDate: String, endDate: String): Int =
        appointmentDao.getWorkingDaysCount(startDate, endDate)
    
    suspend fun getWorkingDaysInRange(startDate: String, endDate: String): List<String> =
        appointmentDao.getWorkingDaysInRange(startDate, endDate)
    
    // Phase 1: New aggregation methods
    suspend fun getIncomeSeries(startDate: String, endDate: String): List<DayIncome> =
        appointmentDao.getIncomeSeries(startDate, endDate)
    
    suspend fun getIncomeByClient(startDate: String, endDate: String): List<ClientIncome> =
        appointmentDao.getIncomeByClient(startDate, endDate)
    
    suspend fun getSummaryStats(startDate: String, endDate: String): SummaryStats =
        appointmentDao.getSummaryStats(startDate, endDate)
    
    // Cancellation statistics
    suspend fun getCancellationsCount(startDate: String, endDate: String): Int =
        appointmentDao.getCancellationsCount(startDate, endDate)
    
    suspend fun getTotalAppointmentsCount(startDate: String, endDate: String): Int =
        appointmentDao.getTotalAppointmentsCount(startDate, endDate)
    
    suspend fun getCancellationsSeries(startDate: String, endDate: String): List<com.clientledger.app.data.dao.DayCancellations> =
        appointmentDao.getCancellationsSeries(startDate, endDate)
    
    // Clients Analytics
    suspend fun getClientsSummary(startDate: String, endDate: String): com.clientledger.app.data.dao.ClientsSummary =
        appointmentDao.getClientsSummary(startDate, endDate)
    
    suspend fun getTopClientsByIncome(startDate: String, endDate: String, limit: Int = 10): List<com.clientledger.app.data.dao.TopClient> =
        appointmentDao.getTopClientsByIncome(startDate, endDate, limit)
    
    // Visits Analytics
    suspend fun getVisitsSummary(startDate: String, endDate: String): com.clientledger.app.data.dao.VisitsSummary =
        appointmentDao.getVisitsSummary(startDate, endDate)
    
    suspend fun getVisitsSeries(startDate: String, endDate: String): List<com.clientledger.app.data.dao.DayVisits> =
        appointmentDao.getVisitsSeries(startDate, endDate)
    
    // Insights
    suspend fun getBusiestDay(startDate: String, endDate: String): com.clientledger.app.data.dao.BusiestDay? =
        appointmentDao.getBusiestDay(startDate, endDate)
    
    suspend fun getBiggestPayment(startDate: String, endDate: String): com.clientledger.app.data.dao.BiggestPayment? =
        appointmentDao.getBiggestPayment(startDate, endDate)
    
    suspend fun getMostFrequentClient(startDate: String, endDate: String): com.clientledger.app.data.dao.MostFrequentClient? =
        appointmentDao.getMostFrequentClient(startDate, endDate)
}


