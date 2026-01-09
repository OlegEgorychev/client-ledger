package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.toDateKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
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
    val expenses: List<ExpenseEntity> = emptyList(),
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
                // Combine appointments and expenses flows
                combine(
                    repository.getAppointmentsByDate(dateKey),
                    repository.getExpensesByDate(dateKey)
                ) { appointments, expenses ->
                    // Загружаем имена клиентов для каждой записи
                    val appointmentsWithClients = appointments.map { appointment ->
                        val client = repository.getClientById(appointment.clientId)
                        val clientName = client?.let { "${it.firstName} ${it.lastName}" }
                        AppointmentWithClient(
                            appointment = appointment,
                            clientName = clientName
                        )
                    }.sortedBy { it.appointment.startsAt } // Сортировка по времени начала
                    
                    // Сортируем expenses по времени (spentAt)
                    val sortedExpenses = expenses.sortedBy { it.spentAt }
                    
                    _uiState.value = _uiState.value.copy(
                        appointments = appointmentsWithClients,
                        expenses = sortedExpenses,
                        isLoading = false
                    )
                }.collect { }
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
     * @return offset в пикселях для прокрутки (относительно начала рабочего дня 09:00)
     */
    fun getScrollOffset(isToday: Boolean, slotHeightPx: Float): Float {
        val appointments = _uiState.value.appointments
        val expenses = _uiState.value.expenses
        val allEvents = (appointments.map { it.appointment.startsAt } + expenses.map { it.spentAt })
            .sorted() // Combine and sort all event start times
        
        // Filter events within working hours (09:00-21:00)
        val workingDayStartMinutes = 9 * 60 // 09:00 = 540 minutes
        val workingDayEndMinutes = 21 * 60 // 21:00 = 1260 minutes
        
        val inRangeEvents = allEvents.filter { eventTimeMillis ->
            val dateTime = com.clientledger.app.util.DateUtils.dateTimeToLocalDateTime(eventTimeMillis)
            val eventMinutes = dateTime.hour * 60 + dateTime.minute
            eventMinutes in workingDayStartMinutes..workingDayEndMinutes
        }
        
        if (inRangeEvents.isNotEmpty()) {
            // Find nearest event
            val nearestEventTimeMillis = if (isToday) {
                val nowMillis = System.currentTimeMillis()
                inRangeEvents.firstOrNull { it >= nowMillis } ?: inRangeEvents.last()
            } else {
                inRangeEvents.first()
            }
            
            val dateTime = com.clientledger.app.util.DateUtils.dateTimeToLocalDateTime(nearestEventTimeMillis)
            val startMinutes = dateTime.hour * 60 + dateTime.minute
            // Calculate position relative to working hours start (09:00)
            val relativeStartMinutes = startMinutes - workingDayStartMinutes
            val normalizedStartMinutes = ((relativeStartMinutes / 5) * 5).coerceAtLeast(0) // Нормализуем к 5-минутным слотам
            val startSlot = normalizedStartMinutes / 5
            // Прокручиваем немного выше записи (на 2 часа назад)
            val scrollSlot = maxOf(0, startSlot - 24) // 24 слота = 2 часа
            return scrollSlot * slotHeightPx
        }
        
        // Если записей нет, прокручиваем к текущему времени (для сегодня) или к началу рабочего дня (09:00) (для другого дня)
        if (isToday) {
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            
            // Only scroll if current time is within working hours
            if (nowMinutes in workingDayStartMinutes..workingDayEndMinutes) {
                val relativeMinutes = nowMinutes - workingDayStartMinutes
                val normalizedMinutes = ((relativeMinutes / 5) * 5).coerceAtLeast(0)
                val scrollSlot = maxOf(0, (normalizedMinutes / 5) - 24) // 2 часа назад
                return scrollSlot * slotHeightPx
            }
        }
        
        // Default: scroll to start of working day (09:00) with a bit of offset
        return 0f * slotHeightPx // Start at top (09:00)
    }
}

