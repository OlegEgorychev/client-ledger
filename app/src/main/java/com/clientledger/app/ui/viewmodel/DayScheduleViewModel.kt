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
import java.time.LocalDateTime
import java.time.LocalTime

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
    private var date: LocalDate,
    private val repository: LedgerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DayScheduleUiState(date = date)
    )
    val uiState: StateFlow<DayScheduleUiState> = _uiState.asStateFlow()

    init {
        loadAppointments()
    }

    /**
     * Обновить дату и перезагрузить записи
     */
    fun updateDate(newDate: LocalDate) {
        if (date != newDate) {
            date = newDate
            _uiState.value = _uiState.value.copy(date = newDate)
            loadAppointments()
        }
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
                        val clientName = client?.let { "${it.firstName} ${it.lastName}" }
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
    
    /**
     * Найти ближайшую запись для автопрокрутки
     * @param isToday true если это сегодняшний день
     * @return индекс записи для прокрутки или null
     */
    fun findNearestAppointmentSlot(isToday: Boolean): Int? {
        val appointments = _uiState.value.appointments
        if (appointments.isEmpty()) {
            return null
        }
        
        if (isToday) {
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            
            // Ищем запись, которая сейчас идет
            val currentAppointment = appointments.firstOrNull { appointment ->
                val dateTime = com.clientledger.app.util.DateUtils.dateTimeToLocalDateTime(appointment.appointment.startsAt)
                val startMinutes = dateTime.hour * 60 + dateTime.minute
                val endMinutes = startMinutes + appointment.appointment.durationMinutes
                nowMinutes >= startMinutes && nowMinutes < endMinutes
            }
            
            if (currentAppointment != null) {
                return appointments.indexOf(currentAppointment)
            }
            
            // Ищем следующую ближайшую будущую запись
            val nextAppointment = appointments.firstOrNull { appointment ->
                val dateTime = com.clientledger.app.util.DateUtils.dateTimeToLocalDateTime(appointment.appointment.startsAt)
                val startMinutes = dateTime.hour * 60 + dateTime.minute
                startMinutes > nowMinutes
            }
            
            if (nextAppointment != null) {
                return appointments.indexOf(nextAppointment)
            }
            
            // Если будущих нет, возвращаем последнюю запись
            return appointments.size - 1
        } else {
            // Для не сегодняшнего дня - первая запись
            return 0
        }
    }
    
    /**
     * Получить offset для прокрутки к ближайшей записи или текущему времени
     * @param isToday true если это сегодняшний день
     * @param slotHeightPx высота одного слота в пикселях
     * @return offset в пикселях для прокрутки
     */
    fun getScrollOffset(isToday: Boolean, slotHeightPx: Float): Float {
        val appointments = _uiState.value.appointments
        val slotIndex = findNearestAppointmentSlot(isToday)
        
        if (slotIndex != null && slotIndex < appointments.size) {
            val appointment = appointments[slotIndex]
            val dateTime = com.clientledger.app.util.DateUtils.dateTimeToLocalDateTime(appointment.appointment.startsAt)
            val startMinutes = dateTime.hour * 60 + dateTime.minute
            val normalizedStartMinutes = (startMinutes / 5) * 5 // Нормализуем к 5-минутным слотам
            val startSlot = normalizedStartMinutes / 5
            // Прокручиваем немного выше записи (на 2 часа назад)
            val scrollSlot = maxOf(0, startSlot - 24) // 24 слота = 2 часа
            return scrollSlot * slotHeightPx
        }
        
        // Если записей нет, прокручиваем к текущему времени (для сегодня) или к 08:00 (для другого дня)
        if (isToday) {
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            val normalizedMinutes = (nowMinutes / 5) * 5
            val scrollSlot = maxOf(0, (normalizedMinutes / 5) - 24) // 2 часа назад
            return scrollSlot * slotHeightPx
        } else {
            // Для другого дня - к 08:00 (96 слотов = 8 часов * 12 слотов)
            return 96f * slotHeightPx
        }
    }
}

