package com.clientledger.app.ui.screen.calendar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.clientledger.app.data.dao.AppointmentWithClient
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.screen.reminders.formatServiceTagsForSms
import com.clientledger.app.ui.screen.reminders.sendSmsForAppointment
import com.clientledger.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentNotificationScreen(
    appointmentId: Long,
    repository: LedgerRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var appointmentWithClient by remember { mutableStateOf<AppointmentWithClient?>(null) }
    var isLoading by remember { mutableStateOf(true) }
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
    
    // Load appointment data
    LaunchedEffect(appointmentId) {
        scope.launch {
            isLoading = true
            try {
                val appointment = repository.getAppointmentById(appointmentId)
                if (appointment != null) {
                    val client = repository.getClientById(appointment.clientId)
                    if (client != null) {
                        appointmentWithClient = AppointmentWithClient(
                            appointmentId = appointment.id,
                            clientId = client.id,
                            clientName = "${client.firstName} ${client.lastName}",
                            clientPhone = client.phone,
                            startsAt = appointment.startsAt,
                            dateKey = appointment.dateKey,
                            durationMinutes = appointment.durationMinutes,
                            title = appointment.title
                        )
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Ошибка загрузки данных: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    val appointmentDate = appointmentWithClient?.let {
        LocalDate.parse(it.dateKey)
    } ?: LocalDate.now()
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Уведомление клиенту") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            appointmentWithClient == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Запись не найдена",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                val appointment = appointmentWithClient!!
                val hasPhone = !appointment.clientPhone.isNullOrBlank()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Appointment info card
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = appointment.clientName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = appointment.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (appointment.clientPhone != null) {
                                Text(
                                    text = appointment.clientPhone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Нет номера телефона",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Message template editor
                    Card(
                        modifier = Modifier.fillMaxWidth()
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
                    
                    // Send button
                    Button(
                        onClick = {
                            if (!hasPhone) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Нет номера телефона для отправки",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                return@Button
                            }
                            
                            scope.launch {
                                sendSmsForAppointment(
                                    context = context,
                                    repository = repository,
                                    appointment = appointment,
                                    tomorrowDate = appointmentDate,
                                    messageTemplate = messageTemplate.text,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasPhone
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отправить SMS")
                    }
                }
            }
        }
    }
}
