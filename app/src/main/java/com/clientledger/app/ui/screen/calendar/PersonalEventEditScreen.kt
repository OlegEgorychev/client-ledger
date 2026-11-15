package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clientledger.app.data.entity.AppointmentStatus
import com.clientledger.app.data.entity.PersonalEventEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.DateUtils
import com.clientledger.app.util.toDateKey
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Создание / редактирование личного дела (время занято не клиентом).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalEventEditScreen(
    eventId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(date) }
    var isAllDay by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var existingEvent by remember { mutableStateOf<PersonalEventEntity?>(null) }

    // По умолчанию: ближайший целый час, длительность 1 час
    val defaultStartHour = (LocalTime.now().hour + 1).coerceIn(6, 22)
    var startHour by remember { mutableStateOf(defaultStartHour) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(defaultStartHour + 1) }
    var endMinute by remember { mutableStateOf(0) }

    // Загрузка существующего дела
    LaunchedEffect(eventId) {
        if (eventId != null) {
            repository.getPersonalEventById(eventId)?.let { event ->
                existingEvent = event
                title = event.title
                note = event.note ?: ""
                selectedDate = LocalDate.parse(event.dateKey)
                isAllDay = event.isAllDay
                if (!event.isAllDay) {
                    startHour = event.startMinute / 60
                    startMinute = event.startMinute % 60
                    // 24:00 храним как 1440 — в выборе показываем 23:55
                    val end = event.endMinute.coerceAtMost(23 * 60 + 55)
                    endHour = end / 60
                    endMinute = end % 60
                }
            }
        }
    }

    val startTotal = startHour * 60 + startMinute
    val endTotal = endHour * 60 + endMinute
    val isTimeValid = isAllDay || endTotal > startTotal
    val isFormValid = title.isNotBlank() && isTimeValid

    // Информационное предупреждение: в это время уже есть записи клиентов
    var overlappingClientsCount by remember { mutableStateOf(0) }
    LaunchedEffect(selectedDate, isAllDay, startTotal, endTotal) {
        val appointments = repository.getAppointmentsByDateExcluding(selectedDate.toDateKey(), null)
            .filter { it.status != AppointmentStatus.CANCELED.name }
        overlappingClientsCount = appointments.count { appointment ->
            val start = DateUtils.dateTimeToLocalDateTime(appointment.startsAt)
                .let { it.hour * 60 + it.minute }
            val end = start + appointment.durationMinutes
            isAllDay || (startTotal < end && endTotal > start)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "Личное дело" else "Редактировать личное дело") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
                actions = {
                    if (eventId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Что за дело *") },
                placeholder = { Text("Например: врач, встреча, выходной") },
                singleLine = true,
                isError = errorMessage != null && title.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = DateUtils.formatDate(selectedDate),
                onValueChange = { },
                readOnly = true,
                label = { Text("Дата") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Выбрать дату")
                    }
                }
            )

            if (showDatePicker) {
                DatePickerDialog(
                    initialDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                        showDatePicker = false
                    },
                    onDismiss = { showDatePicker = false }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAllDay = !isAllDay },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Весь день",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }

            if (!isAllDay) {
                TimeRow(
                    label = "Начало",
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it }
                )
                TimeRow(
                    label = "Конец",
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it }
                )
                if (!isTimeValid) {
                    Text(
                        text = "Конец должен быть позже начала",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (overlappingClientsCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "В это время записано клиентов: $overlappingClientsCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (!isFormValid) {
                        errorMessage = if (title.isBlank()) "Введите название" else "Проверьте время"
                        return@Button
                    }
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val event = PersonalEventEntity(
                                id = eventId ?: 0L,
                                title = title.trim(),
                                dateKey = selectedDate.toDateKey(),
                                isAllDay = isAllDay,
                                startMinute = if (isAllDay) PersonalEventEntity.ALL_DAY_START else startTotal,
                                endMinute = if (isAllDay) PersonalEventEntity.ALL_DAY_END else endTotal,
                                note = note.trim().takeIf { it.isNotEmpty() },
                                createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
                            )
                            repository.savePersonalEvent(event)
                            onBack()
                        } catch (e: Exception) {
                            errorMessage = "Не удалось сохранить. Попробуйте ещё раз."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Сохранение…" else "Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить личное дело?") },
            text = { Text("«${title}» будет удалено.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        existingEvent?.let { repository.deletePersonalEvent(it) }
                        showDeleteDialog = false
                        onBack()
                    }
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    var showHourMenu by remember { mutableStateOf(false) }
    var showMinuteMenu by remember { mutableStateOf(false) }
    val hours = (0..23).toList()
    val minutes = (0..55 step 5).toList()

    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showHourMenu,
                onExpandedChange = { showHourMenu = !showHourMenu },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = String.format("%02d", hour),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Час") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showHourMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showHourMenu,
                    onDismissRequest = { showHourMenu = false }
                ) {
                    hours.forEach { h ->
                        DropdownMenuItem(
                            text = { Text(String.format("%02d", h)) },
                            onClick = {
                                onHourChange(h)
                                showHourMenu = false
                            }
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = showMinuteMenu,
                onExpandedChange = { showMinuteMenu = !showMinuteMenu },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = String.format("%02d", minute),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Минуты") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMinuteMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showMinuteMenu,
                    onDismissRequest = { showMinuteMenu = false }
                ) {
                    minutes.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(String.format("%02d", m)) },
                            onClick = {
                                onMinuteChange(m)
                                showMinuteMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
