package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.navigation.AppointmentDetailsViewModelFactory
import com.clientledger.app.ui.viewmodel.AppointmentDetailsViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(
    appointmentId: Long,
    repository: LedgerRepository,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val viewModel: AppointmentDetailsViewModel = viewModel(
        factory = AppointmentDetailsViewModelFactory(appointmentId, repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Обновляем данные при возврате на экран (например, после редактирования)
    LaunchedEffect(appointmentId) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали записи") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Ошибка: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            uiState.appointment != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppointmentInfoCard(
                        appointment = uiState.appointment!!,
                        client = uiState.client
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEdit(appointmentId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Редактировать")
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Вы уверены, что хотите удалить эту запись? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteAppointment()
                            showDeleteDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun AppointmentInfoCard(
    appointment: com.clientledger.app.data.entity.AppointmentEntity,
    client: com.clientledger.app.data.entity.ClientEntity?
) {
    val dateTime = DateUtils.dateTimeToLocalDateTime(appointment.startsAt)
    val endDateTime = dateTime.plusMinutes(appointment.durationMinutes.toLong())
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Название услуги
            InfoRow(
                label = "Название услуги",
                value = appointment.title,
                isTitle = true
            )

            HorizontalDivider()

            // Клиент
            client?.let {
                InfoRow(
                    label = "Клиент",
                    value = "${it.lastName} ${it.firstName}"
                )
                if (it.phone.isNotBlank() && !it.phone.startsWith("temp_") && !it.phone.startsWith("+7") && it.phone.length < 15) {
                    InfoRow(
                        label = "Телефон",
                        value = it.phone
                    )
                }
                it.telegram?.let { telegram ->
                    if (telegram.isNotBlank()) {
                        InfoRow(
                            label = "Telegram",
                            value = telegram
                        )
                    }
                }
            }

            HorizontalDivider()

            // Дата и время
            InfoRow(
                label = "Дата",
                value = DateUtils.formatDate(dateTime.toLocalDate())
            )
            InfoRow(
                label = "Время",
                value = "${String.format("%02d:%02d", dateTime.hour, dateTime.minute)} - " +
                        "${String.format("%02d:%02d", endDateTime.hour, endDateTime.minute)}"
            )

            HorizontalDivider()

            // Сумма и статус оплаты
            InfoRow(
                label = "Сумма",
                value = MoneyUtils.formatCents(appointment.incomeCents),
                isTitle = true
            )
            InfoRow(
                label = "Статус оплаты",
                value = if (appointment.isPaid) "Оплачено" else "Не оплачено"
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isTitle: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isTitle) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (isTitle) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

