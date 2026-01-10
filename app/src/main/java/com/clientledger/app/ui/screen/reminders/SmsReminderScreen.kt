package com.clientledger.app.ui.screen.reminders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.clientledger.app.data.dao.AppointmentWithClient
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.toDateKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.content.ActivityNotFoundException
import android.content.Context
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicInteger

/**
 * Formats service tags for SMS message according to rules:
 * - 1 tag: "— стрижка"
 * - 2-3 tags: "— стрижка, окрашивание"
 * - >3 tags: "— стрижка, окрашивание и др."
 * - 0 tags: returns empty string
 */
suspend fun formatServiceTagsForSms(
    repository: LedgerRepository,
    appointmentId: Long
): String {
    val services = repository.getServicesForAppointmentSync(appointmentId)
    if (services.isEmpty()) {
        return ""
    }
    
    val tagNames = services
        .mapNotNull { service ->
            repository.getTagById(service.serviceTagId)?.name
        }
        .sorted() // Sort alphabetically for consistency
    
    return when (tagNames.size) {
        0 -> ""
        1 -> "— ${tagNames[0]}"
        2, 3 -> "— ${tagNames.joinToString(", ")}"
        else -> "— ${tagNames.take(2).joinToString(", ")} и др."
    }
}

/**
 * Formats a list of service tag names for SMS (used when tags are already loaded)
 */
fun formatServiceTagNamesForSms(tagNames: List<String>): String {
    if (tagNames.isEmpty()) {
        return ""
    }
    
    val sorted = tagNames.sorted()
    return when (sorted.size) {
        0 -> ""
        1 -> "— ${sorted[0]}"
        2, 3 -> "— ${sorted.joinToString(", ")}"
        else -> "— ${sorted.take(2).joinToString(", ")} и др."
    }
}

/**
 * Sends SMS for a single appointment
 * @param suppressSnackbar If true, doesn't show individual snackbar (for bulk actions)
 * @return true if SMS app was opened successfully, false otherwise
 */
suspend fun sendSmsForAppointment(
    context: Context,
    repository: LedgerRepository,
    appointment: AppointmentWithClient,
    tomorrowDate: LocalDate,
    messageTemplate: String,
    snackbarHostState: SnackbarHostState,
    suppressSnackbar: Boolean = false
): Boolean {
    val phone = appointment.clientPhone
    if (phone.isNullOrBlank()) {
        if (!suppressSnackbar) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar(
                    message = "Нет номера телефона для ${appointment.clientName}",
                    duration = SnackbarDuration.Short
                )
            }
        }
        return false
    }
    
    // Normalize phone number
    val normalizedPhone = phone.trim().replace("\\s+".toRegex(), "").replace("-", "")
    
    // Format time and date
    val time = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(appointment.startsAt),
        ZoneId.systemDefault()
    )
    val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val formattedDate = tomorrowDate.format(DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru")))
    
    // Format service tags
    val serviceTags = formatServiceTagsForSms(repository, appointment.appointmentId)
    
    // Build final message
    var finalMessage = messageTemplate
        .replace("{time}", formattedTime)
        .replace("{date}", formattedDate)
    
    if (serviceTags.isEmpty()) {
        finalMessage = finalMessage.replace("{service}", "")
    } else {
        finalMessage = finalMessage.replace("{service}", " $serviceTags")
    }
    
    android.util.Log.d("SmsReminderScreen", "Отправка SMS для ${appointment.clientName}, телефон: $normalizedPhone")
    android.util.Log.d("SmsReminderScreen", "Сообщение: $finalMessage")
    
    try {
        // Try standard smsto: scheme
        val uri = Uri.parse("smsto:$normalizedPhone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", finalMessage)
        }
        
        var intentLaunched = false
        
        // Try to launch SMS app
        try {
            context.startActivity(intent)
            intentLaunched = true
            android.util.Log.d("SmsReminderScreen", "✅ SMS приложение открыто успешно")
        } catch (e: ActivityNotFoundException) {
            android.util.Log.w("SmsReminderScreen", "⚠️ ActivityNotFoundException, пробуем альтернативный способ")
        } catch (e: Exception) {
            android.util.Log.e("SmsReminderScreen", "❌ Ошибка при запуске SMS", e)
        }
        
        // Try alternative method if first failed
        if (!intentLaunched) {
            try {
                val alternativeIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$normalizedPhone")
                    putExtra("sms_body", finalMessage)
                }
                context.startActivity(alternativeIntent)
                intentLaunched = true
                android.util.Log.d("SmsReminderScreen", "✅ Альтернативный способ сработал")
            } catch (e: Exception) {
                android.util.Log.e("SmsReminderScreen", "❌ Альтернативный способ также не сработал", e)
            }
        }
        
        // Show feedback only if not suppressed (for bulk actions)
        if (!suppressSnackbar) {
            withContext(Dispatchers.Main) {
                if (intentLaunched) {
                    snackbarHostState.showSnackbar(
                        message = "Открыто SMS приложение для ${appointment.clientName}",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        message = "Не удалось открыть SMS приложение",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
        
        return intentLaunched
    } catch (e: Exception) {
        android.util.Log.e("SmsReminderScreen", "❌ Неожиданная ошибка", e)
        if (!suppressSnackbar) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar(
                    message = "Ошибка: ${e.message ?: "Неизвестная ошибка"}",
                    duration = SnackbarDuration.Long
                )
            }
        }
        return false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsReminderScreen(
    appointments: List<AppointmentWithClient>,
    tomorrowDate: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var messageTemplate by remember {
        mutableStateOf(
            TextFieldValue(
                "Здравствуйте!\n" +
                "Напоминаю, что у вас запись {date} в {time}{service}.\n" +
                "Если планы изменились — пожалуйста, сообщите заранее.\n" +
                "Спасибо!"
            )
        )
    }
    
    // Map of appointment ID to selected state (for bulk action only)
    var selectedAppointments by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    
    // Track if bulk mode is enabled
    var showBulkMode by remember {
        mutableStateOf(false)
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SMS напоминания") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            if (appointments.isNotEmpty() && showBulkMode && appointments.size > 1) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBulkMode = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                val selected = appointments.filter { it.appointmentId in selectedAppointments }
                                android.util.Log.d("SmsReminderScreen", "Отправить всем нажата. Выбрано записей: ${selected.size}")
                                
                                val successCount = AtomicInteger(0)
                                val errorCount = AtomicInteger(0)
                                
                            scope.launch {
                                selected.forEachIndexed { index, appointment ->
                                    // Use the same sendSmsForAppointment function for consistency
                                    // Create a temporary snackbar state to track individual results
                                    // For bulk action, we'll show a summary at the end
                                    
                                    val phone = appointment.clientPhone
                                    if (!phone.isNullOrBlank()) {
                                        // Send SMS for this appointment
                                        // We skip showing individual snackbars for bulk action
                                        val success = try {
                                            sendSmsForAppointment(
                                                context = context,
                                                repository = repository,
                                                appointment = appointment,
                                                tomorrowDate = tomorrowDate,
                                                messageTemplate = messageTemplate.text,
                                                snackbarHostState = snackbarHostState,
                                                suppressSnackbar = true // Suppress individual snackbars for bulk action
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e("SmsReminderScreen", "Ошибка при отправке SMS для ${appointment.clientName}", e)
                                            false
                                        }
                                        
                                        if (success) {
                                            successCount.incrementAndGet()
                                        } else {
                                            errorCount.incrementAndGet()
                                        }
                                        
                                        // Небольшая задержка между открытием SMS приложений (только если не последнее)
                                        if (index < selected.size - 1) {
                                            kotlinx.coroutines.delay(500)
                                        }
                                    } else {
                                        android.util.Log.w("SmsReminderScreen", "Пропуск записи ${appointment.clientName}: нет телефона")
                                        errorCount.incrementAndGet()
                                    }
                                }
                                
                                // Show summary after all appointments are processed
                                if (successCount.get() > 0) {
                                    snackbarHostState.showSnackbar(
                                        message = "Открыто SMS приложений: ${successCount.get()}",
                                        duration = SnackbarDuration.Short
                                    )
                                } else if (errorCount.get() > 0) {
                                    snackbarHostState.showSnackbar(
                                        message = "Не удалось открыть SMS приложения",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                                
                                // Reset bulk mode after sending
                                showBulkMode = false
                                selectedAppointments = emptySet()
                            }
                            },
                            modifier = Modifier.weight(2f),
                            enabled = selectedAppointments.isNotEmpty()
                        ) {
                            Text("Отправить всем (${selectedAppointments.size})")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (appointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "На завтра нет записей",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Message template editor
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Шаблон сообщения",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Используйте {time} для времени, {date} для даты, {service} для услуг",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = messageTemplate,
                            onValueChange = { messageTemplate = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            label = { Text("Текст сообщения") }
                        )
                    }
                }
                
                // Bulk mode toggle button (only if multiple appointments)
                if (appointments.size > 1 && !showBulkMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showBulkMode = true }) {
                            Text("Отправить всем")
                        }
                    }
                }
                
                // Appointments list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appointments) { appointment ->
                        val time = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(appointment.startsAt),
                            ZoneId.systemDefault()
                        )
                        val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                        val hasPhone = !appointment.clientPhone.isNullOrBlank()
                        val isSelected = appointment.appointmentId in selectedAppointments
                        
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appointment.clientName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$formattedTime - ${appointment.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (appointment.clientPhone != null) {
                                            Text(
                                                text = appointment.clientPhone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Text(
                                                text = "Нет номера",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                fontStyle = FontStyle.Italic
                                            )
                                        }
                                    }
                                    
                                    // Per-appointment Send button (primary action)
                                    if (!showBulkMode) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    sendSmsForAppointment(
                                                        context = context,
                                                        repository = repository,
                                                        appointment = appointment,
                                                        tomorrowDate = tomorrowDate,
                                                        messageTemplate = messageTemplate.text,
                                                        snackbarHostState = snackbarHostState
                                                    )
                                                }
                                            },
                                            enabled = hasPhone,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text("Отправить")
                                        }
                                    } else {
                                        // Bulk mode: show checkbox instead
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (hasPhone) {
                                                    selectedAppointments = if (checked) {
                                                        selectedAppointments + appointment.appointmentId
                                                    } else {
                                                        selectedAppointments - appointment.appointmentId
                                                    }
                                                }
                                            },
                                            enabled = hasPhone,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
