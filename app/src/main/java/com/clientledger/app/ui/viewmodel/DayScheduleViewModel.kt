package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.toDateKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppointmentWithClient(
    val appointment: AppointmentEntity,
    val clientName: String? = null
)

data class DayScheduleUiState(
    val date: LocalDate,
    val appointments: List<AppointmentWithClient> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DayScheduleViewModel(
    private val date: LocalDate,
    private val repository: LedgerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DayScheduleUiState(date = date)
    )
    val uiState: StateFlow<DayScheduleUiState> = _uiState.asStateFlow()

    init {
        loadAppointments()
    }

    private fun loadAppointments() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val dateKey = date.toDateKey()
        
        viewModelScope.launch {
            try {
                repository.getAppointmentsByDate(dateKey).collect { appointments ->
                    // Загружаем имена клиентов для каждой записи
                    val appointmentsWithClients = appointments.map { appointment ->
                        val client = repository.getClientById(appointment.clientId)
                        val clientName = client?.let { "${it.lastName} ${it.firstName}" }
                        AppointmentWithClient(
                            appointment = appointment,
                            clientName = clientName
                        )
                    }.sortedBy { it.appointment.startsAt } // Сортировка по времени начала
                    
                    _uiState.value = _uiState.value.copy(
                        appointments = appointmentsWithClients,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки данных"
                )
            }
        }
    }
    
    fun refresh() {
        loadAppointments()
    }
}

