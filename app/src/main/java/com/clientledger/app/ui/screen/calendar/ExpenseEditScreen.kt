package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.clientledger.app.LedgerApplication
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.entity.ExpenseItemEntity
import com.clientledger.app.data.entity.ExpenseTag
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditScreen(
    expenseId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as? LedgerApplication }
    val backupScheduler = remember(app) { app?.backupScheduler }
    
    var selectedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Time selection for expense
    var expenseHour by remember { mutableStateOf(LocalTime.now().hour) }
    var expenseMinute by remember { mutableStateOf((LocalTime.now().minute / 5) * 5) } // Round to nearest 5 minutes
    var showHourMenu by remember { mutableStateOf(false) }
    var showMinuteMenu by remember { mutableStateOf(false) }
    
    var selectedTags by remember { mutableStateOf<Set<ExpenseTag>>(emptySet()) }
    var tagAmounts by remember { mutableStateOf<Map<ExpenseTag, String>>(emptyMap()) }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val allTags = ExpenseTag.values().toList()
    
    // Time picker lists (working hours 09:00-21:00)
    val hours = (9..21).toList()
    val minutes = (0..59 step 5).toList() // Step 5 minutes: 00, 05, 10, 15, ..., 55
    
    // BringIntoViewRequester for amount fields
    val amountBringIntoViewRequester = remember { BringIntoViewRequester() }
    
    // Initialize date and time from nav arg for new expenses
    LaunchedEffect(date) {
        if (expenseId == null) {
            selectedDate = date
            // Default to current time, clamped to working hours
            val now = LocalTime.now()
            expenseHour = now.hour.coerceIn(9, 21)
            expenseMinute = (now.minute / 5) * 5 // Round to nearest 5 minutes
        }
    }
    
    // Load existing expense if editing
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            scope.launch {
                val expense = repository.getExpenseById(expenseId)
                expense?.let {
                    selectedDate = LocalDate.parse(it.dateKey)
                    note = it.note ?: ""
                    
                    // Extract time from spentAt timestamp
                    val dateTime = DateUtils.dateTimeToLocalDateTime(it.spentAt)
                    expenseHour = dateTime.hour
                    expenseMinute = (dateTime.minute / 5) * 5 // Round to nearest 5 minutes
                    
                    val items = repository.getExpenseItems(expenseId)
                    selectedTags = items.map { item -> item.tag }.toSet()
                    tagAmounts = items.associate { item ->
                        item.tag to MoneyUtils.centsToRubles(item.amountCents).toString()
                    }
                }
            }
        }
    }
    
    // Calculate total
    val totalCents = selectedTags.sumOf { tag ->
        val amountText = tagAmounts[tag] ?: "0"
        MoneyUtils.rublesToCents(amountText.replace(',', '.').toDoubleOrNull() ?: 0.0)
    }
    
    // Validation
    val isFormValid = remember(selectedDate, selectedTags, tagAmounts) {
        selectedTags.isNotEmpty() && 
        selectedTags.all { tag ->
            val amountText = tagAmounts[tag] ?: ""
            val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
            amount > 0
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == null) "Новый расход" else "Редактировать расход") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
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
            // Date picker
            Text("Дата расхода *", style = MaterialTheme.typography.labelSmall)
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
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Выбрать дату"
                        )
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
            
            // Time picker
            Text("Время расхода *", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hour picker
                ExposedDropdownMenuBox(
                    expanded = showHourMenu,
                    onExpandedChange = { showHourMenu = !showHourMenu },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = String.format("%02d", expenseHour),
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
                        hours.forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(String.format("%02d", hour)) },
                                onClick = {
                                    expenseHour = hour
                                    showHourMenu = false
                                }
                            )
                        }
                    }
                }
                
                // Minute picker
                ExposedDropdownMenuBox(
                    expanded = showMinuteMenu,
                    onExpandedChange = { showMinuteMenu = !showMinuteMenu },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = String.format("%02d", expenseMinute),
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
                        minutes.forEach { minute ->
                            DropdownMenuItem(
                                text = { Text(String.format("%02d", minute)) },
                                onClick = {
                                    expenseMinute = minute
                                    showMinuteMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Tag selection
            Text("Теги расхода *", style = MaterialTheme.typography.labelSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTags.contains(tag),
                        onClick = {
                            selectedTags = if (selectedTags.contains(tag)) {
                                selectedTags - tag
                            } else {
                                selectedTags + tag
                            }
                            // Remove amount when tag is deselected
                            if (!selectedTags.contains(tag)) {
                                tagAmounts = tagAmounts - tag
                            }
                        },
                        label = { Text(tag.displayName) }
                    )
                }
            }
            
            if (selectedTags.isEmpty()) {
                Text(
                    text = "Выберите хотя бы один тег",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Per-tag amounts
            if (selectedTags.isNotEmpty()) {
                Text("Суммы по тегам *", style = MaterialTheme.typography.labelSmall)
                selectedTags.forEach { tag ->
                    val currentAmount = tagAmounts[tag] ?: ""
                    var amountText by remember(tag) { mutableStateOf(currentAmount) }
                    
                    // Sync with state
                    LaunchedEffect(tag) {
                        amountText = tagAmounts[tag] ?: ""
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tag.displayName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { newValue ->
                                if (newValue.all { c -> c.isDigit() || c == '.' || c == ',' }) {
                                    amountText = newValue
                                    tagAmounts = tagAmounts + (tag to newValue)
                                }
                            },
                            label = { Text("Сумма") },
                            modifier = Modifier
                                .width(120.dp)
                                .bringIntoViewRequester(amountBringIntoViewRequester)
                                .onFocusEvent { focusState ->
                                    if (focusState.isFocused) {
                                        scope.launch {
                                            delay(100)
                                            amountBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("0") },
                            isError = amountText.isNotBlank() && 
                                (amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) <= 0
                        )
                    }
                }
                
                // Total
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Итого",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = MoneyUtils.formatCents(totalCents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Note (optional)
            Text("Примечание", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                placeholder = { Text("Необязательное примечание") }
            )
            
            // Error message
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Save button
            Button(
                onClick = {
                    if (!isFormValid) {
                        errorMessage = "Заполните все обязательные поля"
                        return@Button
                    }
                    
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        
                        try {
                            // Use selected date and time for spentAt timestamp
                            val expenseTime = LocalTime.of(expenseHour, expenseMinute)
                            val expenseDateTime = selectedDate.atTime(expenseTime)
                            val spentAt = expenseDateTime.toMillis()
                            
                            val expense = if (expenseId == null) {
                                ExpenseEntity(
                                    spentAt = spentAt,
                                    dateKey = selectedDate.toDateKey(),
                                    totalAmountCents = totalCents,
                                    note = note.takeIf { it.isNotBlank() },
                                    createdAt = System.currentTimeMillis()
                                )
                            } else {
                                ExpenseEntity(
                                    id = expenseId,
                                    spentAt = spentAt,
                                    dateKey = selectedDate.toDateKey(),
                                    totalAmountCents = totalCents,
                                    note = note.takeIf { it.isNotBlank() },
                                    createdAt = System.currentTimeMillis()
                                )
                            }
                            
                            val items = selectedTags.map { tag ->
                                val amountText = tagAmounts[tag] ?: "0"
                                val amountCents = MoneyUtils.rublesToCents(
                                    amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                )
                                ExpenseItemEntity(
                                    expenseId = expenseId ?: 0L,
                                    tag = tag,
                                    amountCents = amountCents
                                )
                            }
                            
                            repository.saveExpenseWithItems(expense, items)
                            
                            // Trigger automatic backup after successful save
                            backupScheduler?.scheduleBackup()
                            
                            onBack()
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сохранения: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isFormValid
            ) {
                if (isLoading) {
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
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
