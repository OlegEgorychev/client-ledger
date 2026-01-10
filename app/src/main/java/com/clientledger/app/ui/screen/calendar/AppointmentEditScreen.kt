package com.clientledger.app.ui.screen.calendar

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import java.time.format.DateTimeFormatter
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.AppointmentServiceEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ServiceTagEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.screen.reminders.formatServiceTagNamesForSms
import com.clientledger.app.ui.viewmodel.AppointmentFieldType
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppointmentEditScreen(
    appointmentId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    viewModel: CalendarViewModel,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Service tags: selected tag IDs and their prices
    var selectedServiceTagIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var serviceTagPrices by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) } // tagId -> price in cents
    var availableTags by remember { mutableStateOf<List<ServiceTagEntity>>(emptyList()) }
    var showTagSelector by remember { mutableStateOf(false) }
    var clientNameText by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<Long?>(null) }
    var startHour by remember { mutableStateOf(12) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(13) }
    var endMinute by remember { mutableStateOf(0) }
    
    var showStartHourMenu by remember { mutableStateOf(false) }
    var showStartMinuteMenu by remember { mutableStateOf(false) }
    var showEndHourMenu by remember { mutableStateOf(false) }
    var showEndMinuteMenu by remember { mutableStateOf(false) }
    
    // Списки для выбора времени (ограничение 9:00-22:00)
    val availableHours = (9..22).toList()
    val availableMinutes = (0..59 step 5).toList() // Шаг 5 минут: 00, 05, 10, 15, ..., 55
    var incomeRubles by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var hasTriedToSave by remember { mutableStateOf(false) }
    
    var filteredClients by remember { mutableStateOf<List<ClientEntity>>(emptyList()) }
    var showClientMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // Проверка дубля клиента
    var hasClientNameConflict by remember { mutableStateOf(false) }
    var existingClientId by remember { mutableStateOf<Long?>(null) }
    
    // Client phone for SMS button
    var clientPhone by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val app = remember { context.applicationContext as? com.clientledger.app.LedgerApplication }
    val backupScheduler = remember(app) { app?.backupScheduler }
    
    // Инициализируем дату и время при первом запуске
    LaunchedEffect(date) {
        if (appointmentId == null) {
            selectedDate = date
            // Устанавливаем время по умолчанию 12:00 для новых записей (в пределах 9:00-22:00)
            startHour = 12.coerceIn(9, 22)
            startMinute = 0
            // Время окончания - на час позже (13:00, но не позже 22:00)
            endHour = 13.coerceIn(9, 22)
            endMinute = 0
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Load available service tags
    LaunchedEffect(Unit) {
        repository.getAllActiveTags().collect { tags ->
            availableTags = tags
        }
    }
    
    // Проверка пересечений времени (suspend функция, вызывается через LaunchedEffect)
    var hasTimeOverlap by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedDate, startHour, startMinute, endHour, endMinute, appointmentId) {
        // Проверяем пересечения только если время валидно
        if (viewModel.validateTimeRange(startHour, startMinute, endHour, endMinute)) {
            val dateKey = selectedDate.toDateKey()
            hasTimeOverlap = viewModel.checkTimeOverlap(
                dateKey = dateKey,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                excludeAppointmentId = appointmentId
            )
        } else {
            hasTimeOverlap = false
        }
    }
    
    // Валидация времени
    val isTimeRangeValid = remember(startHour, startMinute, endHour, endMinute) {
        viewModel.validateTimeRange(startHour, startMinute, endHour, endMinute)
    }
    
    // Получаем причину блокировки сохранения и проблемное поле из ViewModel
    val saveDisabledReason = remember(
        selectedServiceTagIds, clientNameText, selectedClientId, incomeRubles, selectedDate,
        startHour, startMinute, endHour, endMinute, isTimeRangeValid, hasTimeOverlap
    ) {
        viewModel.getSaveDisabledReason(
            selectedServiceTagIds = selectedServiceTagIds,
            clientNameText = clientNameText,
            selectedClientId = selectedClientId,
            incomeRubles = incomeRubles,
            dateSelected = selectedDate != null,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            isTimeRangeValid = isTimeRangeValid,
            hasTimeOverlap = hasTimeOverlap
        )
    }
    
    val invalidField = remember(
        selectedServiceTagIds, clientNameText, selectedClientId, incomeRubles, selectedDate,
        startHour, startMinute, endHour, endMinute, isTimeRangeValid, hasTimeOverlap
    ) {
        viewModel.getInvalidField(
            selectedServiceTagIds = selectedServiceTagIds,
            clientNameText = clientNameText,
            selectedClientId = selectedClientId,
            incomeRubles = incomeRubles,
            dateSelected = selectedDate != null,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            isTimeRangeValid = isTimeRangeValid,
            hasTimeOverlap = hasTimeOverlap
        )
    }
    
    val isSaveEnabled = remember(
        selectedServiceTagIds, clientNameText, selectedClientId, incomeRubles, selectedDate,
        startHour, startMinute, endHour, endMinute, isTimeRangeValid, hasTimeOverlap,
        hasClientNameConflict, uiState.isSaving
    ) {
        // Блокируем сохранение, если идет процесс сохранения или есть конфликт имени клиента
        if (uiState.isSaving || hasClientNameConflict) {
            false
        } else {
            viewModel.isSaveEnabled(
                selectedServiceTagIds = selectedServiceTagIds,
                clientNameText = clientNameText,
                selectedClientId = selectedClientId,
                incomeRubles = incomeRubles,
                dateSelected = selectedDate != null,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                isTimeRangeValid = isTimeRangeValid,
                hasTimeOverlap = hasTimeOverlap
            )
        }
    }
    
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
    
    // Проверка дубля клиента при вводе текста (если клиент не выбран из списка)
    LaunchedEffect(clientNameText, selectedClientId) {
        if (clientNameText.isNotBlank() && selectedClientId == null) {
            scope.launch {
                val (exists, clientId) = viewModel.checkClientExists(clientNameText)
                hasClientNameConflict = exists
                existingClientId = clientId
            }
        } else {
            hasClientNameConflict = false
            existingClientId = null
        }
    }

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            scope.launch {
                val appointment = repository.getAppointmentById(appointmentId)
                appointment?.let {
                    // Load service tags for this appointment
                    val services = repository.getServicesForAppointmentSync(appointmentId)
                    selectedServiceTagIds = services.map { it.serviceTagId }
                    serviceTagPrices = services.associate { it.serviceTagId to it.priceForThisTag }
                    
                    selectedClientId = it.clientId
                    scope.launch {
                        val client = repository.getClientById(it.clientId)
                        client?.let {
                            clientNameText = "${it.firstName} ${it.lastName}"
                            clientPhone = it.phone
                        }
                    }
                    val dateTime = DateUtils.dateTimeToLocalDateTime(it.startsAt)
                    selectedDate = dateTime.toLocalDate()
                    // Ограничиваем время начала в пределах 9:00-22:00
                    val loadedStartHour = dateTime.hour.coerceIn(9, 22)
                    startHour = loadedStartHour
                    // Округляем минуты до ближайшего значения с шагом 5
                    // Если час 22, то только 0 минут
                    val loadedStartMinute = if (loadedStartHour == 22) {
                        0
                    } else {
                        (dateTime.minute / 5) * 5
                    }
                    startMinute = loadedStartMinute
                    val duration = it.durationMinutes
                    val endDateTime = dateTime.plusMinutes(duration.toLong())
                    // Ограничиваем время окончания в пределах 9:00-22:00
                    val loadedEndHour = endDateTime.hour.coerceIn(9, 22)
                    endHour = loadedEndHour
                    // Округляем минуты до ближайшего значения с шагом 5
                    // Если час 22, то только 0 минут
                    val loadedEndMinute = if (loadedEndHour == 22) {
                        0
                    } else {
                        (endDateTime.minute / 5) * 5
                    }
                    endMinute = loadedEndMinute
                    incomeRubles = MoneyUtils.centsToRubles(it.incomeCents).toString()
                    isPaid = it.isPaid
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (appointmentId == null) "Новая запись" else "Редактировать запись") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !uiState.isSaving
                    ) {
                        Text("Назад")
                    }
                },
                actions = {
                    if (onSettingsClick != null) {
                        IconButton(onClick = { onSettingsClick() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Блокируем все поля во время сохранения
                val isFormDisabled = uiState.isSaving
                
                // Service Tags Selector
                Text("Услуги *", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(isFormDisabled) {
                            if (!isFormDisabled) {
                                detectTapGestures {
                                    showTagSelector = true
                                }
                            }
                        }
                ) {
                    OutlinedTextField(
                        value = if (selectedServiceTagIds.isEmpty()) "" else selectedServiceTagIds.joinToString(", ") { tagId ->
                            availableTags.find { it.id == tagId }?.name ?: ""
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Выберите услуги") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                // Intercept all pointer events to prevent TextField from getting focus
                            },
                        enabled = false, // Disable to prevent focus
                        trailingIcon = {
                            IconButton(onClick = { if (!isFormDisabled) showTagSelector = true }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                                    contentDescription = "Выбрать услуги"
                                )
                            }
                        },
                        isError = invalidField == AppointmentFieldType.SERVICES,
                        colors = if (invalidField == AppointmentFieldType.SERVICES) {
                            OutlinedTextFieldDefaults.colors(
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                                disabledBorderColor = MaterialTheme.colorScheme.error,
                                disabledLabelColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        }
                    )
                }
                
                // Selected tags as chips with price inputs
                if (selectedServiceTagIds.isNotEmpty()) {
                    selectedServiceTagIds.forEach { tagId ->
                        val tag = availableTags.find { it.id == tagId }
                        tag?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tag name chip
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        selectedServiceTagIds = selectedServiceTagIds - tagId
                                        serviceTagPrices = serviceTagPrices - tagId
                                    },
                                    label = { Text(it.name) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                    )
                                )
                                
                                // Price input for this tag
                                val currentPrice = serviceTagPrices[tagId] ?: 0L
                                
                                // Compute priceRubles based on currentPrice (only whole rubles, no kopecks)
                                val priceRubles = remember(tagId, currentPrice) {
                                    if (currentPrice > 0) {
                                                // Convert cents to whole rubles (integer division)
                                                (currentPrice / 100L).toString()
                                    } else {
                                        ""
                                    }
                                }
                                
                                // Local state for text input - initialized from priceRubles
                                var priceText by remember(tagId) { 
                                    mutableStateOf(
                                        if (currentPrice > 0) {
                                                // Convert cents to whole rubles (integer division)
                                                (currentPrice / 100L).toString()
                                        } else {
                                            ""
                                        }
                                    ) 
                                }
                                
                                // Sync priceText with priceRubles when price changes externally (e.g., when loading existing appointment)
                                LaunchedEffect(tagId, priceRubles) {
                                    if (priceText != priceRubles) {
                                        priceText = priceRubles
                                    }
                                }
                                
                                // Check if price is empty (for error highlighting)
                                // Show error immediately when price is empty - when tag is first added, currentPrice will be 0 and priceText will be empty
                                val isPriceEmpty = priceText.isBlank() || currentPrice == 0L
                                
                                OutlinedTextField(
                                    value = priceText,
                                    onValueChange = { newValue ->
                                        if (newValue.all { c -> c.isDigit() }) {
                                            if (newValue.isEmpty()) {
                                                priceText = newValue
                                                serviceTagPrices = serviceTagPrices + (tagId to 0L)
                                            } else {
                                                val priceRubles = newValue.toLongOrNull() ?: 0L
                                                // Int max value in cents: 2,147,483,647
                                                // Max rubles that fit in Int cents: 21,474,836
                                                val maxRubles = 21474836L
                                            if (priceRubles <= maxRubles) {
                                                priceText = newValue
                                                val priceCents = priceRubles * 100L // Convert rubles to cents
                                                serviceTagPrices = serviceTagPrices + (tagId to priceCents)
                                            }
                                                // If exceeds max, don't update - prevents overflow
                                            }
                                        }
                                    },
                                    label = { Text("Цена *") },
                                    modifier = Modifier.width(120.dp),
                                    singleLine = true,
                                    placeholder = { Text("0") },
                                    enabled = !isFormDisabled,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = isPriceEmpty,
                                    colors = if (isPriceEmpty) {
                                        OutlinedTextFieldDefaults.colors(
                                            errorBorderColor = MaterialTheme.colorScheme.error,
                                            errorLabelColor = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        OutlinedTextFieldDefaults.colors()
                                    }
                                )
                            }
                        }
                    }
                    
                    // Total amount (sum of tag prices) - displayed as read-only field
                    val totalCents = selectedServiceTagIds.sumOf { tagId ->
                        serviceTagPrices[tagId] ?: 0L
                    }
                    // Convert cents to whole rubles (integer division) - avoid Double precision issues
                    val totalRubles = if (totalCents > 0) {
                        (totalCents / 100).toString()
                    } else {
                        "0"
                    }
                    
                    // Update incomeRubles when total changes
                    LaunchedEffect(totalCents) {
                        incomeRubles = totalRubles
                    }
                    
                    // Display total amount as read-only field
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = totalRubles,
                        onValueChange = { }, // Read-only
                        label = { Text("Сумма") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = false, // Make it read-only
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                
                // Tag selector dialog
                if (showTagSelector) {
                    ServiceTagSelectorDialog(
                        availableTags = availableTags,
                        selectedTagIds = selectedServiceTagIds,
                        onTagsSelected = { selectedIds ->
                            selectedServiceTagIds = selectedIds
                            // Remove prices for deselected tags (user must enter prices manually)
                            serviceTagPrices = serviceTagPrices.filterKeys { it in selectedIds }
                        },
                        onDismiss = { showTagSelector = false }
                    )
                }

            // Выбор даты
            Text("Дата записи *", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = DateUtils.formatDate(selectedDate),
                onValueChange = { }, // Только для чтения
                readOnly = true,
                label = { Text("Дата") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CalendarToday,
                            contentDescription = "Выбрать дату"
                        )
                    }
                },
                isError = invalidField == AppointmentFieldType.DATE,
                colors = if (invalidField == AppointmentFieldType.DATE) {
                    OutlinedTextFieldDefaults.colors(
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    OutlinedTextFieldDefaults.colors()
                }
            )

            // DatePicker Dialog
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

            // Выбор клиента
            Text("Клиент *", style = MaterialTheme.typography.labelSmall)
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
                    enabled = !isFormDisabled,
                    interactionSource = interactionSource,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClientMenu)
                    },
                    placeholder = { Text("Введите имя или фамилию") },
                    isError = invalidField == AppointmentFieldType.CLIENT || hasClientNameConflict,
                    colors = if (invalidField == AppointmentFieldType.CLIENT || hasClientNameConflict) {
                        OutlinedTextFieldDefaults.colors(
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        OutlinedTextFieldDefaults.colors()
                    },
                    supportingText = {
                        if (hasClientNameConflict) {
                            Text(
                                text = "Клиент с таким именем уже существует. Выберите его из списка.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (invalidField == AppointmentFieldType.CLIENT) {
                            Text(
                                text = "Выберите клиента.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = showClientMenu,
                    onDismissRequest = { showClientMenu = false }
                ) {
                    filteredClients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text("${client.firstName} ${client.lastName}") },
                            onClick = {
                                clientNameText = "${client.firstName} ${client.lastName}"
                                selectedClientId = client.id
                                clientPhone = client.phone
                                hasClientNameConflict = false
                                existingClientId = null
                                showClientMenu = false
                            }
                        )
                    }
                }
            }
            
            // Кнопка "Выбрать клиента" при обнаружении дубля
            if (hasClientNameConflict && existingClientId != null) {
                TextButton(
                    onClick = {
                        scope.launch {
                            val client = repository.getClientById(existingClientId!!)
                            client?.let {
                                clientNameText = "${it.firstName} ${it.lastName}"
                                selectedClientId = it.id
                                clientPhone = it.phone
                                hasClientNameConflict = false
                                existingClientId = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выбрать клиента")
                }
            }
            
            // Кнопка "Написать клиенту" - видна только при редактировании существующей записи и наличии телефона
            if (appointmentId != null && selectedClientId != null && !clientPhone.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val phone = clientPhone ?: return@launch
                            val dateTime = if (appointmentId != null) {
                                selectedDate.atTime(LocalTime.of(startHour, startMinute))
                            } else {
                                selectedDate.atTime(LocalTime.of(startHour, startMinute))
                            }
                            val formattedDate = dateTime.format(DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru")))
                            val formattedTime = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            
                            // Format service tags for this appointment
                            val serviceTags = if (appointmentId != null) {
                                // Load service tags from database
                                val services = repository.getServicesForAppointmentSync(appointmentId)
                                val tagNames = services
                                    .mapNotNull { service ->
                                        repository.getTagById(service.serviceTagId)?.name
                                    }
                                formatServiceTagNamesForSms(tagNames)
                            } else {
                                // For new appointments, use current selected tags
                                val tagNames = selectedServiceTagIds
                                    .mapNotNull { tagId -> availableTags.find { it.id == tagId }?.name }
                                formatServiceTagNamesForSms(tagNames)
                            }
                            
                            // Build message with improved format (same as SMS reminders template)
                            val message = buildString {
                                append("Здравствуйте!\n")
                                append("Напоминаю, что у вас запись $formattedDate в $formattedTime")
                                if (serviceTags.isNotEmpty()) {
                                    append(" $serviceTags") // serviceTags already includes "— ", so add space before
                                }
                                append(".\n")
                                append("Если планы изменились — пожалуйста, сообщите заранее.\n")
                                append("Спасибо!")
                            }
                            
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$phone")
                                putExtra("sms_body", message)
                            }
                            
                            try {
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    // Try alternative method
                                    val alternativeIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("sms:$phone")
                                        putExtra("sms_body", message)
                                    }
                                    context.startActivity(alternativeIntent)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AppointmentEditScreen", "Error opening SMS", e)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFormDisabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Написать клиенту")
                }
            }

            // Начало сеанса
            Text("Начало сеанса", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Выбор часов начала
                ExposedDropdownMenuBox(
                    expanded = showStartHourMenu,
                    onExpandedChange = { showStartHourMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = startHour.toString().padStart(2, '0'),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Часы") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStartHourMenu)
                        },
                        isError = invalidField == AppointmentFieldType.START_TIME || 
                                  invalidField == AppointmentFieldType.TIME_RANGE || 
                                  invalidField == AppointmentFieldType.TIME_OVERLAP,
                        colors = if (invalidField == AppointmentFieldType.START_TIME || 
                                    invalidField == AppointmentFieldType.TIME_RANGE || 
                                    invalidField == AppointmentFieldType.TIME_OVERLAP) {
                            OutlinedTextFieldDefaults.colors(
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showStartHourMenu,
                        onDismissRequest = { showStartHourMenu = false }
                    ) {
                        availableHours.forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(hour.toString().padStart(2, '0')) },
                                onClick = {
                                    startHour = hour
                                    // Если выбран час 22, устанавливаем минуты на 0 (22:00 - последняя допустимая точка)
                                    if (hour == 22 && startMinute > 0) {
                                        startMinute = 0
                                    }
                                    showStartHourMenu = false
                                    isLoading = false // Сбрасываем при изменении времени
                                }
                            )
                        }
                    }
                }
                
                // Выбор минут начала
                ExposedDropdownMenuBox(
                    expanded = showStartMinuteMenu,
                    onExpandedChange = { showStartMinuteMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = startMinute.toString().padStart(2, '0'),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Минуты") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStartMinuteMenu)
                        },
                        isError = invalidField == AppointmentFieldType.START_TIME || 
                                  invalidField == AppointmentFieldType.TIME_RANGE || 
                                  invalidField == AppointmentFieldType.TIME_OVERLAP,
                        colors = if (invalidField == AppointmentFieldType.START_TIME || 
                                    invalidField == AppointmentFieldType.TIME_RANGE || 
                                    invalidField == AppointmentFieldType.TIME_OVERLAP) {
                            OutlinedTextFieldDefaults.colors(
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showStartMinuteMenu,
                        onDismissRequest = { showStartMinuteMenu = false }
                    ) {
                        // Если выбран час 22, показываем только 0 минут (22:00 - последняя допустимая точка)
                        val allowedMinutes = if (startHour == 22) {
                            availableMinutes.filter { it == 0 }
                        } else {
                            availableMinutes
                        }
                        allowedMinutes.forEach { minute ->
                            DropdownMenuItem(
                                text = { Text(minute.toString().padStart(2, '0')) },
                                onClick = {
                                    startMinute = minute
                                    showStartMinuteMenu = false
                                    isLoading = false // Сбрасываем при изменении времени
                                }
                            )
                        }
                    }
                }
            }

            // Конец сеанса
            Text("Конец сеанса", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Выбор часов окончания
                ExposedDropdownMenuBox(
                    expanded = showEndHourMenu,
                    onExpandedChange = { showEndHourMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = endHour.toString().padStart(2, '0'),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Часы") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEndHourMenu)
                        },
                        isError = invalidField == AppointmentFieldType.END_TIME || 
                                  invalidField == AppointmentFieldType.TIME_RANGE || 
                                  invalidField == AppointmentFieldType.TIME_OVERLAP,
                        colors = if (invalidField == AppointmentFieldType.END_TIME || 
                                    invalidField == AppointmentFieldType.TIME_RANGE || 
                                    invalidField == AppointmentFieldType.TIME_OVERLAP) {
                            OutlinedTextFieldDefaults.colors(
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showEndHourMenu,
                        onDismissRequest = { showEndHourMenu = false }
                    ) {
                        availableHours.forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(hour.toString().padStart(2, '0')) },
                                onClick = {
                                    endHour = hour
                                    // Если выбран час 22, устанавливаем минуты на 0 (22:00 - последняя допустимая точка)
                                    if (hour == 22 && endMinute > 0) {
                                        endMinute = 0
                                    }
                                    showEndHourMenu = false
                                    isLoading = false // Сбрасываем при изменении времени
                                }
                            )
                        }
                    }
                }
                
                // Выбор минут окончания
                ExposedDropdownMenuBox(
                    expanded = showEndMinuteMenu,
                    onExpandedChange = { showEndMinuteMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = endMinute.toString().padStart(2, '0'),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Минуты") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEndMinuteMenu)
                        },
                        isError = invalidField == AppointmentFieldType.END_TIME || 
                                  invalidField == AppointmentFieldType.TIME_RANGE || 
                                  invalidField == AppointmentFieldType.TIME_OVERLAP,
                        colors = if (invalidField == AppointmentFieldType.END_TIME || 
                                    invalidField == AppointmentFieldType.TIME_RANGE || 
                                    invalidField == AppointmentFieldType.TIME_OVERLAP) {
                            OutlinedTextFieldDefaults.colors(
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showEndMinuteMenu,
                        onDismissRequest = { showEndMinuteMenu = false }
                    ) {
                        // Если выбран час 22, показываем только 0 минут (22:00 - последняя допустимая точка)
                        val allowedMinutes = if (endHour == 22) {
                            availableMinutes.filter { it == 0 }
                        } else {
                            availableMinutes
                        }
                        allowedMinutes.forEach { minute ->
                            DropdownMenuItem(
                                text = { Text(minute.toString().padStart(2, '0')) },
                                onClick = {
                                    endMinute = minute
                                    showEndMinuteMenu = false
                                    isLoading = false // Сбрасываем при изменении времени
                                }
                            )
                        }
                    }
                }
            }
            
            // Сообщение об ошибке времени (показываем под блоком выбора времени)
            if ((invalidField == AppointmentFieldType.TIME_RANGE || invalidField == AppointmentFieldType.TIME_OVERLAP) && saveDisabledReason != null) {
                Text(
                    text = saveDisabledReason,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Button(
                onClick = {
                    Log.d("AppointmentEdit", "Button onClick triggered")
                    Log.d("AppointmentEdit", "isLoading: $isLoading, isSaveEnabled: $isSaveEnabled")
                    Log.d("AppointmentEdit", "selectedTags: $selectedServiceTagIds, clientNameText: '$clientNameText', incomeRubles: '$incomeRubles'")
                    
                    // Проверка валидности формы
                    if (!isSaveEnabled || uiState.isSaving) {
                        Log.w("AppointmentEdit", "Validation failed: form is invalid or already saving")
                        Log.w("AppointmentEdit", "Save disabled reason: $saveDisabledReason, isSaving: ${uiState.isSaving}")
                        return@Button
                    }

                    Log.d("AppointmentEdit", "All validations passed, starting save")
                    scope.launch {
                        try {
                            // Проверяем, будет ли создан новый клиент
                            val willCreateNewClient = selectedClientId == null && 
                                !hasClientNameConflict && 
                                clientNameText.isNotBlank()
                            
                            // Начинаем процесс сохранения
                            viewModel.startSaving(willCreateNewClient = willCreateNewClient)
                            isLoading = true
                            
                            Log.d("AppointmentEdit", "Save button clicked - starting save process")
                            
                            // Разрешаем ID клиента: находим существующего или создаем нового
                            val (clientId, isNewClient) = viewModel.resolveClientId(
                                clientNameText = clientNameText,
                                selectedClientId = selectedClientId
                            )
                            
                            val clientDisplayName = clientNameText.trim()
                            
                            val incomeCents = MoneyUtils.rublesToCents(incomeRubles.replace(',', '.').toDoubleOrNull() ?: 0.0)
                            val startsAt = selectedDate.atTime(LocalTime.of(startHour, startMinute)).toMillis()
                            
                            // Вычисляем длительность в минутах
                            val startTotalMinutes = startHour * 60 + startMinute
                            val endTotalMinutes = endHour * 60 + endMinute
                            val durationMinutes = if (endTotalMinutes >= startTotalMinutes) {
                                endTotalMinutes - startTotalMinutes
                            } else {
                                // Если конец меньше начала, считаем что это следующий день (24 часа - начало + конец)
                                (24 * 60 - startTotalMinutes) + endTotalMinutes
                            }
                            
                            // Create services list from selected tags
                            val services = selectedServiceTagIds.mapIndexed { index, tagId ->
                                AppointmentServiceEntity(
                                    appointmentId = appointmentId ?: 0L, // Will be updated after appointment is saved
                                    serviceTagId = tagId,
                                    priceForThisTag = serviceTagPrices[tagId] ?: 0L,
                                    sortOrder = index
                                )
                            }
                            
                            // Generate title from selected tags (for backward compatibility)
                            val title = if (selectedServiceTagIds.isNotEmpty()) {
                                selectedServiceTagIds.joinToString(", ") { tagId ->
                                    availableTags.find { it.id == tagId }?.name ?: ""
                                }
                            } else {
                                "Другое"
                            }
                            
                            val appointment = if (appointmentId == null) {
                                AppointmentEntity(
                                    clientId = clientId,
                                    title = title, // Keep for backward compatibility
                                    startsAt = startsAt,
                                    dateKey = selectedDate.toDateKey(),
                                    durationMinutes = max(15, durationMinutes), // Минимум 15 минут
                                    incomeCents = incomeCents,
                                    isPaid = isPaid,
                                    createdAt = System.currentTimeMillis()
                                )
                            } else {
                                AppointmentEntity(
                                    id = appointmentId,
                                    clientId = clientId,
                                    title = title, // Keep for backward compatibility
                                    startsAt = startsAt,
                                    dateKey = selectedDate.toDateKey(),
                                    durationMinutes = max(15, durationMinutes), // Минимум 15 минут
                                    incomeCents = incomeCents,
                                    isPaid = isPaid,
                                    createdAt = System.currentTimeMillis()
                                )
                            }

                            Log.d("AppointmentEdit", "Saving appointment: ${if (appointmentId == null) "INSERT" else "UPDATE"}")
                            
                            // Save appointment with services
                            val savedAppointmentId = repository.saveAppointmentWithServices(appointment, services)
                            Log.d("AppointmentEdit", "Appointment saved successfully with ID: $savedAppointmentId")
                            
                            // Trigger automatic backup after successful save
                            backupScheduler?.scheduleBackup()
                            
                            // Обновляем рабочие дни после сохранения записи
                            viewModel.refreshWorkingDays()
                            
                            Log.d("AppointmentEdit", "Save completed, navigating back")
                            hasTriedToSave = false // Сбрасываем флаг при успешном сохранении
                            
                            // Показываем уведомление о создании нового клиента
                            if (isNewClient) {
                                snackbarHostState.showSnackbar(
                                    message = "Создан новый клиент: $clientDisplayName",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            
                            onBack()
                        } catch (e: Exception) {
                            Log.e("AppointmentEdit", "Error saving appointment", e)
                            snackbarHostState.showSnackbar(
                                message = "Не удалось сохранить. Попробуйте ещё раз.",
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            isLoading = false
                            viewModel.finishSaving()
                            Log.d("AppointmentEdit", "Save process finished, isLoading = false")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isSaveEnabled && !uiState.isSaving
            ) {
                if (isLoading || uiState.isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Сохранение…")
                    }
                } else {
                    Text("Сохранить")
                }
            }
            
            // Отображение подсказки, если кнопка неактивна
            if (!isSaveEnabled && saveDisabledReason != null) {
                Text(
                    text = saveDisabledReason,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Extra bottom padding for better accessibility with large font scales
            Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Overlay с индикатором загрузки во время сохранения
            if (uiState.isSaving) {
                var showLongSaveMessage by remember { mutableStateOf(false) }
                
                // Таймаут для долгих операций
                LaunchedEffect(uiState.isSaving) {
                    if (uiState.isSaving) {
                        kotlinx.coroutines.delay(10000) // 10 секунд
                        if (uiState.isSaving) {
                            showLongSaveMessage = true
                        }
                    } else {
                        showLongSaveMessage = false
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = uiState.savingMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (showLongSaveMessage) {
                            Text(
                                text = "Сохранение занимает больше времени, чем обычно…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Выберите дату",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Навигация по месяцам
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Предыдущий месяц"
                        )
                    }
                    Text(
                        text = DateUtils.formatMonth(currentMonth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Следующий месяц"
                        )
                    }
                }
                
                // Календарь
                DatePickerCalendar(
                    month = currentMonth,
                    selectedDate = selectedDate,
                    initialDate = initialDate,
                    onDateClick = { date ->
                        selectedDate = date
                        // Если выбранная дата в другом месяце, переключаем месяц
                        val dateMonth = YearMonth.from(date)
                        if (dateMonth != currentMonth) {
                            currentMonth = dateMonth
                        }
                    }
                )
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            onDateSelected(selectedDate)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    initialDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // Понедельник = 0
    val daysInMonth = month.lengthOfMonth()
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Заголовки дней недели
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Календарная сетка
        var currentDay = 1
        repeat(6) { week ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(7) { dayOfWeek ->
                    if (week == 0 && dayOfWeek < firstDayOfWeek) {
                        // Пустая ячейка перед первым днем месяца
                        Spacer(modifier = Modifier.weight(1f))
                    } else if (currentDay <= daysInMonth) {
                        val date = month.atDay(currentDay)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val isWeekend = dayOfWeek == 5 || dayOfWeek == 6
                        
                        DatePickerDayCell(
                            day = currentDay,
                            isSelected = isSelected,
                            isToday = isToday,
                            isWeekend = isWeekend,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                        currentDay++
                    } else {
                        // Пустая ячейка после последнего дня месяца
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Фон для выбранной даты
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        } else if (isToday && !isSelected) {
            // Фон для текущего дня (если не выбран)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    )
            )
        } else if (isWeekend && !isSelected && !isToday) {
            // Фон для выходных дней
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
        
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else if (isToday && !isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ServiceTagSelectorDialog(
    availableTags: List<ServiceTagEntity>,
    selectedTagIds: List<Long>,
    onTagsSelected: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedTagIds.toSet()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Выберите услуги",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableTags) { tag ->
                        val isSelected = currentSelection.contains(tag.id)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                currentSelection = if (isSelected) {
                                    currentSelection - tag.id
                                } else {
                                    currentSelection + tag.id
                                }
                            },
                            label = { Text(tag.name) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            onTagsSelected(currentSelection.toList())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}


