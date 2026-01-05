package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val dayAppointments: List<AppointmentEntity> = emptyList(),
    val dayExpenses: List<ExpenseEntity> = emptyList(),
    val clients: List<ClientEntity> = emptyList(),
    val workingDays: Set<String> = emptySet() // dateKey для дней с записями
)

class CalendarViewModel(private val repository: LedgerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadClients()
        loadWorkingDays(_uiState.value.currentMonth)
    }

    fun changeMonth(months: Int) {
        val newMonth = _uiState.value.currentMonth.plusMonths(months.toLong())
        _uiState.value = _uiState.value.copy(currentMonth = newMonth)
        loadWorkingDays(newMonth)
    }

    private fun loadWorkingDays(month: YearMonth) {
        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        val startDateKey = firstDay.toDateKey()
        val endDateKey = lastDay.toDateKey()

        viewModelScope.launch {
            val workingDaysList = repository.getWorkingDaysInRange(startDateKey, endDateKey)
            _uiState.value = _uiState.value.copy(workingDays = workingDaysList.toSet())
        }
    }

    fun refreshWorkingDays() {
        loadWorkingDays(_uiState.value.currentMonth)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDayData(date)
    }

    fun clearSelectedDate() {
        _uiState.value = _uiState.value.copy(selectedDate = null)
    }

    private fun loadDayData(date: LocalDate) {
        val dateKey = date.toDateKey()
        viewModelScope.launch {
            repository.getAppointmentsByDate(dateKey).collect { appointments ->
                _uiState.value = _uiState.value.copy(dayAppointments = appointments)
            }
        }
        viewModelScope.launch {
            repository.getExpensesByDate(dateKey).collect { expenses ->
                _uiState.value = _uiState.value.copy(dayExpenses = expenses)
            }
        }
    }

    private fun loadClients() {
        viewModelScope.launch {
            repository.getAllClients().collect { clients ->
                _uiState.value = _uiState.value.copy(clients = clients)
            }
        }
    }

    suspend fun insertAppointment(appointment: AppointmentEntity) {
        repository.insertAppointment(appointment)
    }

    suspend fun updateAppointment(appointment: AppointmentEntity) {
        repository.updateAppointment(appointment)
    }

    suspend fun deleteAppointment(appointment: AppointmentEntity) {
        repository.deleteAppointment(appointment)
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        repository.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        repository.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        repository.deleteExpense(expense)
    }
}


