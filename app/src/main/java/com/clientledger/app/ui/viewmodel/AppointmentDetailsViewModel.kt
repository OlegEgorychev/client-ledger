package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppointmentDetailsUiState(
    val appointment: AppointmentEntity? = null,
    val client: ClientEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AppointmentDetailsViewModel(
    private val appointmentId: Long,
    private val repository: LedgerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentDetailsUiState(isLoading = true))
    val uiState: StateFlow<AppointmentDetailsUiState> = _uiState.asStateFlow()

    init {
        loadAppointment()
    }

    private fun loadAppointment() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val appointment = repository.getAppointmentById(appointmentId)
                if (appointment != null) {
                    val client = repository.getClientById(appointment.clientId)
                    _uiState.value = _uiState.value.copy(
                        appointment = appointment,
                        client = client,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Запись не найдена"
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
        loadAppointment()
    }

    suspend fun deleteAppointment() {
        val appointment = _uiState.value.appointment
        if (appointment != null) {
            repository.deleteAppointment(appointment)
        }
    }
}

