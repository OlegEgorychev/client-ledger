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
import java.time.LocalTime
import java.time.YearMonth

/**
 * Типы полей формы для индикации ошибок
 */
enum class AppointmentFieldType {
    TITLE,
    CLIENT,
    INCOME,
    DATE,
    START_TIME,
    END_TIME,
    TIME_RANGE,
    TIME_OVERLAP
}

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val dayAppointments: List<AppointmentEntity> = emptyList(),
    val dayExpenses: List<ExpenseEntity> = emptyList(),
    val clients: List<ClientEntity> = emptyList(),
    val workingDays: Set<String> = emptySet(), // dateKey для дней с записями
    val isSaving: Boolean = false,
    val savingMessage: String = ""
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

    /**
     * Валидация времени начала и конца сеанса
     * @return true если время корректно (конец позже начала), false если некорректно
     */
    fun validateTimeRange(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val startTime = LocalTime.of(startHour, startMinute)
        val endTime = LocalTime.of(endHour, endMinute)
        return endTime.isAfter(startTime)
    }

    /**
     * Валидация формы записи (без проверки пересечений, т.к. она требует suspend)
     * @return Map с ошибками валидации (ключ - название поля, значение - текст ошибки)
     */
    fun validateAppointmentForm(
        title: String,
        clientNameText: String,
        incomeRubles: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        // Название услуги
        if (title.isBlank()) {
            errors["title"] = "Введите название услуги."
        }
        
        // Клиент
        if (clientNameText.isBlank()) {
            errors["client"] = "Выберите клиента."
        }
        
        // Сумма
        val incomeValue = incomeRubles.replace(',', '.').toDoubleOrNull()
        if (incomeRubles.isBlank()) {
            errors["income"] = "Введите сумму больше 0."
        } else if (incomeValue == null || incomeValue <= 0) {
            errors["income"] = "Введите сумму больше 0."
        }
        
        // Время начала (всегда должно быть выбрано, но проверяем валидность)
        // Время окончания (всегда должно быть выбрано, но проверяем валидность)
        // Валидация диапазона времени
        if (!validateTimeRange(startHour, startMinute, endHour, endMinute)) {
            errors["timeRange"] = "Время окончания не может быть раньше времени начала."
        }
        
        return errors
    }
    
    /**
     * Проверка валидности формы
     * @return true если форма валидна, false если есть ошибки
     */
    fun isAppointmentFormValid(
        title: String,
        clientNameText: String,
        incomeRubles: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        return validateAppointmentForm(title, clientNameText, incomeRubles, startHour, startMinute, endHour, endMinute).isEmpty()
    }

    /**
     * Проверка пересечения времени с существующими записями
     * @param dateKey дата в формате "YYYY-MM-DD"
     * @param startHour час начала
     * @param startMinute минута начала
     * @param endHour час окончания
     * @param endMinute минута окончания
     * @param excludeAppointmentId ID записи, которую исключаем из проверки (при редактировании)
     * @return true если есть пересечение, false если пересечений нет
     */
    suspend fun checkTimeOverlap(
        dateKey: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        excludeAppointmentId: Long? = null
    ): Boolean {
        // Получаем все записи за день, исключая редактируемую
        val existingAppointments = repository.getAppointmentsByDateExcluding(dateKey, excludeAppointmentId)
        
        // Вычисляем время начала и окончания новой записи в миллисекундах
        val date = dateKey.toLocalDate()
        val startTime = date.atTime(startHour, startMinute).toMillis()
        val endTime = date.atTime(endHour, endMinute).toMillis()
        
        // Проверяем пересечение с каждой существующей записью
        // Правило пересечения: startA < endB AND endA > startB
        for (appointment in existingAppointments) {
            val existingStart = appointment.startsAt
            val existingEnd = appointment.startsAt + (appointment.durationMinutes * 60 * 1000L)
            
            // Проверяем пересечение: startA < endB AND endA > startB
            if (startTime < existingEnd && endTime > existingStart) {
                return true // Найдено пересечение
            }
        }
        
        return false // Пересечений нет
    }

    /**
     * Получить причину блокировки сохранения (первая по приоритету ошибка)
     * @return текст причины или null, если форма валидна
     */
    fun getSaveDisabledReason(
        title: String,
        clientNameText: String,
        selectedClientId: Long?,
        incomeRubles: String,
        dateSelected: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        isTimeRangeValid: Boolean,
        hasTimeOverlap: Boolean
    ): String? {
        // 1. Название услуги
        if (title.isBlank()) {
            return "Укажите название услуги."
        }
        
        // 2. Клиент
        // Клиент валиден, если либо выбран из списка, либо введен текст вручную
        if (clientNameText.isBlank()) {
            return "Выберите клиента."
        }
        
        // 2.1. Проверка дубля клиента (если клиент не выбран из списка)
        // Эта проверка выполняется в UI через LaunchedEffect, здесь только валидация
        
        // 3. Сумма
        val incomeValue = incomeRubles.replace(',', '.').toDoubleOrNull()
        if (incomeRubles.isBlank() || incomeValue == null || incomeValue <= 0) {
            return "Введите сумму услуги."
        }
        
        // 4. Дата
        if (!dateSelected) {
            return "Выберите дату."
        }
        
        // 5. Время начала (всегда выбрано, так как есть значения по умолчанию)
        // Пропускаем проверку, так как startHour и startMinute всегда имеют значения
        
        // 6. Время окончания (всегда выбрано, так как есть значения по умолчанию)
        // Пропускаем проверку, так как endHour и endMinute всегда имеют значения
        
        // 7. Валидация диапазона времени
        if (!isTimeRangeValid) {
            return "Время окончания должно быть позже времени начала."
        }
        
        // 8. Пересечение времени
        if (hasTimeOverlap) {
            return "Выбранное время пересекается с другой записью."
        }
        
        return null // Форма валидна
    }

    /**
     * Получить тип проблемного поля (первое по приоритету)
     * @return тип поля или null, если форма валидна
     */
    fun getInvalidField(
        title: String,
        clientNameText: String,
        selectedClientId: Long?,
        incomeRubles: String,
        dateSelected: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        isTimeRangeValid: Boolean,
        hasTimeOverlap: Boolean
    ): AppointmentFieldType? {
        // 1. Название услуги
        if (title.isBlank()) {
            return AppointmentFieldType.TITLE
        }
        
        // 2. Клиент
        // Клиент валиден, если либо выбран из списка, либо введен текст вручную
        if (clientNameText.isBlank()) {
            return AppointmentFieldType.CLIENT
        }
        
        // 3. Сумма
        val incomeValue = incomeRubles.replace(',', '.').toDoubleOrNull()
        if (incomeRubles.isBlank() || incomeValue == null || incomeValue <= 0) {
            return AppointmentFieldType.INCOME
        }
        
        // 4. Дата
        if (!dateSelected) {
            return AppointmentFieldType.DATE
        }
        
        // 5. Время начала (всегда выбрано, так как есть значения по умолчанию)
        // Пропускаем проверку, так как startHour и startMinute всегда имеют значения
        
        // 6. Время окончания (всегда выбрано, так как есть значения по умолчанию)
        // Пропускаем проверку, так как endHour и endMinute всегда имеют значения
        
        // 7. Валидация диапазона времени
        if (!isTimeRangeValid) {
            return AppointmentFieldType.TIME_RANGE
        }
        
        // 8. Пересечение времени
        if (hasTimeOverlap) {
            return AppointmentFieldType.TIME_OVERLAP
        }
        
        return null // Форма валидна
    }

    /**
     * Проверка, можно ли сохранить форму
     * @return true если форма валидна и можно сохранить
     */
    fun isSaveEnabled(
        title: String,
        clientNameText: String,
        selectedClientId: Long?,
        incomeRubles: String,
        dateSelected: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        isTimeRangeValid: Boolean,
        hasTimeOverlap: Boolean
    ): Boolean {
        return getSaveDisabledReason(
            title, clientNameText, selectedClientId, incomeRubles,
            dateSelected, startHour, startMinute, endHour, endMinute,
            isTimeRangeValid, hasTimeOverlap
        ) == null
    }

    /**
     * Проверить, существует ли клиент с таким именем (для предотвращения дублей)
     * @param clientNameText введенный текст (имя и фамилия)
     * @return Pair<Boolean, Long?> где первый элемент - true если клиент существует, второй - ID клиента (если найден)
     */
    suspend fun checkClientExists(clientNameText: String): Pair<Boolean, Long?> {
        // Нормализуем строку (trim, убрать двойные пробелы)
        val normalizedText = clientNameText.trim().replace("\\s+".toRegex(), " ")
        
        if (normalizedText.isBlank()) {
            return Pair(false, null)
        }

        // Разделяем на имя и фамилию
        val nameParts = normalizedText.split(" ", limit = 2)
        val firstName = nameParts[0]
        val lastName = if (nameParts.size >= 2) nameParts[1] else ""

        // Проверяем, существует ли клиент с таким именем и фамилией (case-insensitive)
        val existingClient = repository.findClientByName(firstName, lastName)
        
        return if (existingClient != null) {
            Pair(true, existingClient.id)
        } else {
            Pair(false, null)
        }
    }

    /**
     * Разрешить ID клиента: найти существующего или создать нового
     * @param clientNameText введенный текст (имя и фамилия)
     * @param selectedClientId выбранный ID клиента (если был выбран из списка)
     * @return Pair<Long, Boolean> где первый элемент - ID клиента, второй - true если клиент был создан
     */
    suspend fun resolveClientId(
        clientNameText: String,
        selectedClientId: Long?
    ): Pair<Long, Boolean> {
        // Если клиент уже выбран из списка, используем его
        if (selectedClientId != null) {
            return Pair(selectedClientId, false)
        }

        // Нормализуем строку (trim, убрать двойные пробелы)
        val normalizedText = clientNameText.trim().replace("\\s+".toRegex(), " ")
        
        if (normalizedText.isBlank()) {
            throw IllegalArgumentException("Имя клиента не может быть пустым")
        }

        // Разделяем на имя и фамилию
        val nameParts = normalizedText.split(" ", limit = 2)
        val firstName = nameParts[0]
        val lastName = if (nameParts.size >= 2) nameParts[1] else ""

        // Проверяем, существует ли клиент с таким именем и фамилией (case-insensitive)
        val existingClient = repository.findClientByName(firstName, lastName)
        
        if (existingClient != null) {
            // Клиент уже существует, используем его
            return Pair(existingClient.id, false)
        }

        // Создаем нового клиента
        val now = System.currentTimeMillis()
        val tempPhone = "+7${now % 10000000000}"
        val newClient = ClientEntity(
            firstName = firstName,
            lastName = lastName,
            gender = "male", // По умолчанию
            phone = tempPhone,
            createdAt = now,
            updatedAt = now
        )
        
        val newClientId = repository.insertClient(newClient)
        return Pair(newClientId, true) // true означает, что клиент был создан
    }

    /**
     * Начать процесс сохранения
     * @param willCreateNewClient будет ли создан новый клиент
     */
    fun startSaving(willCreateNewClient: Boolean = false) {
        if (_uiState.value.isSaving) {
            return // Уже идет сохранение, игнорируем повторный вызов
        }
        _uiState.value = _uiState.value.copy(
            isSaving = true,
            savingMessage = if (willCreateNewClient) {
                "Создаём клиента и сохраняем запись…"
            } else {
                "Сохраняем запись…"
            }
        )
    }

    /**
     * Завершить процесс сохранения
     */
    fun finishSaving() {
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            savingMessage = ""
        )
    }
}


