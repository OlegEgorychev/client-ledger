package com.clientledger.app.data.repository

import com.clientledger.app.data.dao.AppointmentDao
import com.clientledger.app.data.dao.AppointmentServiceDao
import com.clientledger.app.data.dao.ClientDao
import com.clientledger.app.data.dao.ClientIncome
import com.clientledger.app.data.dao.DayIncome
import com.clientledger.app.data.dao.DayProfit
import com.clientledger.app.data.dao.ExpenseDao
import com.clientledger.app.data.dao.ExpenseItemDao
import com.clientledger.app.data.dao.ServiceTagDao
import com.clientledger.app.data.dao.SummaryStats
import com.clientledger.app.data.dao.TagIncome
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.AppointmentServiceEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.entity.ExpenseItemEntity
import com.clientledger.app.data.entity.ExpenseTag
import com.clientledger.app.data.entity.ServiceTagEntity
import com.clientledger.app.data.testdata.TestDataGenerator
import com.clientledger.app.util.DateUtils
import com.clientledger.app.util.toDateKey
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class LedgerRepository(
    private val clientDao: ClientDao,
    private val appointmentDao: AppointmentDao,
    private val expenseDao: ExpenseDao,
    private val expenseItemDao: ExpenseItemDao,
    private val serviceTagDao: ServiceTagDao,
    private val appointmentServiceDao: AppointmentServiceDao
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
    
    suspend fun deleteAppointment(appointment: AppointmentEntity) {
        // Delete associated services first (CASCADE should handle this, but explicit for clarity)
        appointmentServiceDao.deleteServicesForAppointment(appointment.id)
        appointmentDao.deleteAppointment(appointment)
    }

    // Expenses
    fun getExpensesByDate(dateKey: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByDate(dateKey)
    
    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)
    
    suspend fun getExpenseById(id: Long): ExpenseEntity? = expenseDao.getExpenseById(id)
    
    suspend fun getExpenseItems(expenseId: Long): List<ExpenseItemEntity> =
        expenseItemDao.getItemsForExpense(expenseId)
    
    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    
    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)
    
    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseItemDao.deleteItemsForExpense(expense.id)
        expenseDao.deleteExpense(expense)
    }
    
    suspend fun saveExpenseWithItems(
        expense: ExpenseEntity,
        items: List<ExpenseItemEntity>
    ): Long {
        val expenseId = if (expense.id == 0L) {
            expenseDao.insertExpense(expense)
        } else {
            expenseItemDao.deleteItemsForExpense(expense.id)
            expenseDao.updateExpense(expense)
            expense.id
        }
        
        val itemsWithExpenseId = items.map { it.copy(expenseId = expenseId) }
        expenseItemDao.insertExpenseItems(itemsWithExpenseId)
        
        return expenseId
    }

    // Statistics
    suspend fun getIncomeForDateRange(startDate: String, endDate: String): Long =
        appointmentDao.getIncomeForDateRange(startDate, endDate)
    
    suspend fun getExpensesForDateRange(startDate: String, endDate: String): Long =
        expenseDao.getExpensesForDateRange(startDate, endDate)
    
    // Income sums by period
    suspend fun getIncomeForDay(date: LocalDate): Long {
        val dateKey = date.toDateKey()
        return getIncomeForDateRange(dateKey, dateKey)
    }
    
    suspend fun getIncomeForMonth(year: Int, month: Int): Long {
        val yearMonth = java.time.YearMonth.of(year, month)
        val startDate = DateUtils.getStartOfMonth(yearMonth).toDateKey()
        val endDate = DateUtils.getEndOfMonth(yearMonth).toDateKey()
        return getIncomeForDateRange(startDate, endDate)
    }
    
    suspend fun getIncomeForYear(year: Int): Long {
        val startDate = DateUtils.getStartOfYear(year).toDateKey()
        val endDate = DateUtils.getEndOfYear(year).toDateKey()
        return getIncomeForDateRange(startDate, endDate)
    }
    
    // Expense sums by period
    suspend fun getExpensesForDay(date: LocalDate): Long {
        val dateKey = date.toDateKey()
        return getExpensesForDateRange(dateKey, dateKey)
    }
    
    suspend fun getExpensesForMonth(year: Int, month: Int): Long {
        val yearMonth = java.time.YearMonth.of(year, month)
        val startDate = DateUtils.getStartOfMonth(yearMonth).toDateKey()
        val endDate = DateUtils.getEndOfMonth(yearMonth).toDateKey()
        return getExpensesForDateRange(startDate, endDate)
    }
    
    suspend fun getExpensesForYear(year: Int): Long {
        val startDate = DateUtils.getStartOfYear(year).toDateKey()
        val endDate = DateUtils.getEndOfYear(year).toDateKey()
        return getExpensesForDateRange(startDate, endDate)
    }
    
    // Net profit sums by period
    suspend fun getNetProfitForDay(date: LocalDate): Long {
        val income = getIncomeForDay(date)
        val expenses = getExpensesForDay(date)
        return income - expenses
    }
    
    suspend fun getNetProfitForMonth(year: Int, month: Int): Long {
        val income = getIncomeForMonth(year, month)
        val expenses = getExpensesForMonth(year, month)
        return income - expenses
    }
    
    suspend fun getNetProfitForYear(year: Int): Long {
        val income = getIncomeForYear(year)
        val expenses = getExpensesForYear(year)
        return income - expenses
    }
    
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
    suspend fun getClientsSummary(startDate: String, endDate: String): com.clientledger.app.data.dao.ClientsSummary {
        val summary = appointmentDao.getClientsSummary(startDate, endDate)
        val firstDates = appointmentDao.getFirstAppointmentDates()
        val firstDateMap = firstDates.associateBy { it.clientId }
        
        // Get all clients in period
        val clientsInPeriod = appointmentDao.getClientIdsInPeriod(startDate, endDate)
        
        var newClients = 0
        var returningClients = 0
        
        clientsInPeriod.forEach { clientId ->
            val firstDate = firstDateMap[clientId]?.firstDateKey
            // New client: first appointment is in the selected period
            // Returning client: first appointment was before the selected period
            if (firstDate == null || (firstDate >= startDate && firstDate <= endDate)) {
                newClients++
            } else if (firstDate < startDate) {
                returningClients++
            } else {
                // Edge case: firstDate > endDate (shouldn't happen if client is in period)
                newClients++
            }
        }
        
        return summary.copy(
            newClients = newClients,
            returningClients = returningClients
        )
    }
    
    
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
    
    // Test data management (DEBUG ONLY)
    suspend fun generateTestData(): TestDataGenerationResult {
        val testData = TestDataGenerator.generateAllTestData()
        
        // Insert clients and get their IDs
        val clientIdMap = mutableMapOf<Int, Long>()
        testData.clients.forEachIndexed { index, client ->
            val insertedId = clientDao.insertClient(client)
            clientIdMap[index + 1] = insertedId // Map client index (1-30) to actual ID
        }
        
        // Update appointment clientIds with actual inserted IDs
        val allAppointments = testData.decemberAppointments + testData.januaryAppointments
        var insertedCount = 0
        allAppointments.forEach { appointment ->
            // appointment.clientId is currently 1-30 (placeholder)
            // Map to actual inserted client ID
            val clientIndex = appointment.clientId.toInt()
            val actualClientId = clientIdMap[clientIndex] ?: appointment.clientId
            
            appointmentDao.insertAppointment(
                appointment.copy(clientId = actualClientId)
            )
            insertedCount++
        }
        
        return TestDataGenerationResult(
            clientsCount = testData.clients.size,
            appointmentsCount = insertedCount
        )
    }
    
    data class TestDataGenerationResult(
        val clientsCount: Int,
        val appointmentsCount: Int
    )
    
    suspend fun clearAllTestData() {
        appointmentDao.deleteAllTestAppointments()
        clientDao.deleteAllTestClients()
    }
    
    // Service Tags
    fun getAllActiveTags(): Flow<List<ServiceTagEntity>> = serviceTagDao.getAllActiveTags()
    
    fun getAllTags(): Flow<List<ServiceTagEntity>> = serviceTagDao.getAllTags()
    
    suspend fun getTagById(id: Long): ServiceTagEntity? = serviceTagDao.getTagById(id)
    
    suspend fun getTagByName(name: String): ServiceTagEntity? = serviceTagDao.getTagByName(name)
    
    suspend fun insertTag(tag: ServiceTagEntity): Long = serviceTagDao.insertTag(tag)
    
    suspend fun insertTags(tags: List<ServiceTagEntity>) = serviceTagDao.insertTags(tags)
    
    suspend fun updateTag(tag: ServiceTagEntity) = serviceTagDao.updateTag(tag)
    
    suspend fun deleteTag(tag: ServiceTagEntity) = serviceTagDao.deleteTag(tag)
    
    suspend fun setTagActive(id: Long, isActive: Boolean) = serviceTagDao.setTagActive(id, isActive)
    
    // Appointment Services (tags for appointments)
    fun getServicesForAppointment(appointmentId: Long): Flow<List<AppointmentServiceEntity>> =
        appointmentServiceDao.getServicesForAppointment(appointmentId)
    
    suspend fun getServicesForAppointmentSync(appointmentId: Long): List<AppointmentServiceEntity> =
        appointmentServiceDao.getServicesForAppointmentSync(appointmentId)
    
    suspend fun saveAppointmentWithServices(
        appointment: AppointmentEntity,
        services: List<AppointmentServiceEntity>
    ): Long {
        val appointmentId = if (appointment.id == 0L) {
            appointmentDao.insertAppointment(appointment)
        } else {
            // Delete existing services before updating
            appointmentServiceDao.deleteServicesForAppointment(appointment.id)
            appointmentDao.updateAppointment(appointment)
            appointment.id
        }
        
        // Insert new services with correct appointmentId
        val servicesWithAppointmentId = services.map { it.copy(appointmentId = appointmentId) }
        appointmentServiceDao.insertServices(servicesWithAppointmentId)
        
        return appointmentId
    }
    
    // Tag-based Statistics
    suspend fun getIncomeByTag(startDate: String, endDate: String): List<TagIncome> =
        appointmentServiceDao.getIncomeByTag(startDate, endDate)
    
    suspend fun getTopTags(startDate: String, endDate: String, limit: Int = 10): List<TagIncome> =
        appointmentServiceDao.getTopTags(startDate, endDate, limit)
    
    // Initialize default tags (for migration/first run)
    suspend fun initializeDefaultTags() {
        val defaultTags = listOf(
            ServiceTagEntity(name = "Стрижка", defaultPrice = 0, isActive = true, sortOrder = 1),
            ServiceTagEntity(name = "Прическа", defaultPrice = 0, isActive = true, sortOrder = 2),
            ServiceTagEntity(name = "Окрашивание", defaultPrice = 0, isActive = true, sortOrder = 3),
            ServiceTagEntity(name = "Мелирование", defaultPrice = 0, isActive = true, sortOrder = 4),
            ServiceTagEntity(name = "Тонирование", defaultPrice = 0, isActive = true, sortOrder = 5)
        )
        
        // Check each tag and insert only if it doesn't exist
        defaultTags.forEach { tag ->
            val existingTag = serviceTagDao.getTagByName(tag.name)
            if (existingTag == null) {
                serviceTagDao.insertTag(tag)
            }
        }
    }
}


