package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentEditScreen(
    appointmentId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    viewModel: CalendarViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var clientNameText by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<Long?>(null) }
    var startHour by remember { mutableStateOf(12) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(13) }
    var endMinute by remember { mutableStateOf(0) }
    var incomeRubles by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    
    var filteredClients by remember { mutableStateOf<List<ClientEntity>>(emptyList()) }
    var showClientMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Поиск клиентов при вводе текста или при фокусе на поле
    LaunchedEffect(clientNameText, isFocused) {
        if (clientNameText.isNotBlank()) {
            repository.searchClients(clientNameText).collect { clients ->
                filteredClients = clients
                if (isFocused) {
                    showClientMenu = clients.isNotEmpty()
                }
            }
        } else {
            // При пустом поле показываем весь список клиентов по алфавиту
            repository.getAllClients().collect { clients ->
                filteredClients = clients
                if (isFocused) {
                    showClientMenu = clients.isNotEmpty()
                }
            }
        }
    }

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            scope.launch {
                val appointment = repository.getAppointmentById(appointmentId)
                appointment?.let {
                    title = it.title
                    selectedClientId = it.clientId
                    val client = repository.getClientById(it.clientId)
                    client?.let {
                        clientNameText = "${it.lastName} ${it.firstName}"
                    }
                    val dateTime = DateUtils.dateTimeToLocalDateTime(it.startsAt)
                    startHour = dateTime.hour
                    startMinute = dateTime.minute
                    val duration = it.durationMinutes
                    val endDateTime = dateTime.plusMinutes(duration.toLong())
                    endHour = endDateTime.hour
                    endMinute = endDateTime.minute
                    incomeRubles = MoneyUtils.centsToRubles(it.incomeCents).toString()
                    isPaid = it.isPaid
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (appointmentId == null) "Новая запись" else "Редактировать запись") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Стрижка") }
            )

            // Выбор клиента
            Text("Клиент *")
            ExposedDropdownMenuBox(
                expanded = showClientMenu,
                onExpandedChange = { showClientMenu = it }
            ) {
                OutlinedTextField(
                    value = clientNameText,
                    onValueChange = { text ->
                        clientNameText = text
                        selectedClientId = null
                        // showClientMenu будет обновляться автоматически через LaunchedEffect
                    },
                    label = { Text("Имя или фамилия клиента") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    interactionSource = interactionSource,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClientMenu)
                    },
                    placeholder = { Text("Введите имя или фамилию") }
                )
                ExposedDropdownMenu(
                    expanded = showClientMenu,
                    onDismissRequest = { showClientMenu = false }
                ) {
                    filteredClients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text("${client.lastName} ${client.firstName}") },
                            onClick = {
                                clientNameText = "${client.lastName} ${client.firstName}"
                                selectedClientId = client.id
                                showClientMenu = false
                            }
                        )
                    }
                }
            }

            // Начало сеанса
            Text("Начало сеанса", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startHour.toString().padStart(2, '0'),
                    onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) startHour = h } },
                    label = { Text("Часы") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = startMinute.toString().padStart(2, '0'),
                    onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) startMinute = m } },
                    label = { Text("Минуты") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Конец сеанса
            Text("Конец сеанса", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = endHour.toString().padStart(2, '0'),
                    onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) endHour = h } },
                    label = { Text("Часы") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endMinute.toString().padStart(2, '0'),
                    onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) endMinute = m } },
                    label = { Text("Минуты") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = incomeRubles,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == ',' }) incomeRubles = it },
                label = { Text("Сумма (рубли) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("1500") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPaid,
                    onCheckedChange = { isPaid = it }
                )
                Text("Оплачено")
            }

            Button(
                onClick = {
                    if (title.isBlank() || clientNameText.isBlank() || incomeRubles.isBlank()) {
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        
                        // Определяем ID клиента: если выбран из списка - используем его, иначе создаем нового
                        var clientId = selectedClientId
                        if (clientId == null) {
                            // Создаем нового клиента
                            // Парсим имя: если два слова - первое фамилия, второе имя; иначе - все в имя
                            val nameParts = clientNameText.trim().split("\\s+".toRegex())
                            val firstName: String
                            val lastName: String
                            if (nameParts.size >= 2) {
                                lastName = nameParts[0]
                                firstName = nameParts.subList(1, nameParts.size).joinToString(" ")
                            } else {
                                lastName = clientNameText.trim()
                                firstName = clientNameText.trim()
                            }
                            
                            val now = System.currentTimeMillis()
                            // Генерируем временный телефон на основе timestamp для уникальности
                            val tempPhone = "+7${now % 10000000000}"
                            val newClient = ClientEntity(
                                firstName = firstName,
                                lastName = lastName,
                                gender = "male", // По умолчанию
                                phone = tempPhone,
                                createdAt = now,
                                updatedAt = now
                            )
                            clientId = repository.insertClient(newClient)
                        }
                        
                        val incomeCents = MoneyUtils.rublesToCents(incomeRubles.replace(',', '.').toDoubleOrNull() ?: 0.0)
                        val startsAt = date.atTime(LocalTime.of(startHour, startMinute)).toMillis()
                        
                        // Вычисляем длительность в минутах
                        val startTotalMinutes = startHour * 60 + startMinute
                        val endTotalMinutes = endHour * 60 + endMinute
                        val durationMinutes = if (endTotalMinutes >= startTotalMinutes) {
                            endTotalMinutes - startTotalMinutes
                        } else {
                            // Если конец меньше начала, считаем что это следующий день (24 часа - начало + конец)
                            (24 * 60 - startTotalMinutes) + endTotalMinutes
                        }
                        
                        val appointment = if (appointmentId == null) {
                            AppointmentEntity(
                                clientId = clientId!!,
                                title = title,
                                startsAt = startsAt,
                                dateKey = date.toDateKey(),
                                durationMinutes = max(15, durationMinutes), // Минимум 15 минут
                                incomeCents = incomeCents,
                                isPaid = isPaid,
                                createdAt = System.currentTimeMillis()
                            )
                        } else {
                            AppointmentEntity(
                                id = appointmentId,
                                clientId = clientId!!,
                                title = title,
                                startsAt = startsAt,
                                dateKey = date.toDateKey(),
                                durationMinutes = max(15, durationMinutes), // Минимум 15 минут
                                incomeCents = incomeCents,
                                isPaid = isPaid,
                                createdAt = System.currentTimeMillis()
                            )
                        }

                        if (appointmentId == null) {
                            viewModel.insertAppointment(appointment)
                        } else {
                            viewModel.updateAppointment(appointment)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}


