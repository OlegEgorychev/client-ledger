package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.navigation.DayScheduleViewModelFactory
import com.clientledger.app.ui.viewmodel.DayScheduleViewModel
import com.clientledger.app.ui.viewmodel.AppointmentWithClient
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import java.time.LocalDate
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScheduleScreen(
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit,
    onAppointmentClick: (Long) -> Unit
) {
    val viewModel: DayScheduleViewModel = viewModel(
        factory = DayScheduleViewModelFactory(date, repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(DateUtils.formatDate(date)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
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
                        text = uiState.error ?: "Ошибка",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Повторить")
                    }
                }
            }
        } else {
            DayScheduleContent(
                appointments = uiState.appointments,
                onAppointmentClick = { appointmentId ->
                    onAppointmentClick(appointmentId)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun DayScheduleContent(
    appointments: List<AppointmentWithClient>,
    onAppointmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dpPerMinute = 1.2f
    val hourHeight = (60 * dpPerMinute).dp
    val totalHeight = hourHeight * 24
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Левая колонка с часами
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    TimeColumn(
                        hourHeight = hourHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Правая колонка с временной сеткой и записями
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight)
                    ) {
                        // Временная сетка
                        TimeGrid(
                            hourHeight = hourHeight,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Записи
                        appointments.forEach { appointmentWithClient ->
                            AppointmentCardOnTimeline(
                                appointment = appointmentWithClient.appointment,
                                clientName = appointmentWithClient.clientName,
                                hourHeight = hourHeight,
                                dpPerMinute = dpPerMinute,
                                onClick = { onAppointmentClick(appointmentWithClient.appointment.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeColumn(
    hourHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(24) { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = String.format("%02d:00", hour),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimeGrid(
    hourHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(24) { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight)
                    .background(
                        color = if (hour % 2 == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    )
            )
            // Линия между часами
            if (hour < 23) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun AppointmentCardOnTimeline(
    appointment: AppointmentEntity,
    clientName: String?,
    hourHeight: androidx.compose.ui.unit.Dp,
    dpPerMinute: Float,
    onClick: () -> Unit
) {
    val dateTime = DateUtils.dateTimeToLocalDateTime(appointment.startsAt)
    val startMinutes = dateTime.hour * 60 + dateTime.minute
    val durationMinutes = max(15, appointment.durationMinutes) // Используем сохраненную длительность, минимум 15 минут
    
    // Вычисляем позицию: минуты от начала дня * dpPerMinute
    val topOffset = (startMinutes * dpPerMinute).dp
    val cardHeight = (durationMinutes * dpPerMinute).dp
    
    val endTime = dateTime.plusMinutes(durationMinutes.toLong())
    val timeRange = "${String.format("%02d:%02d", dateTime.hour, dateTime.minute)} - " +
            "${String.format("%02d:%02d", endTime.hour, endTime.minute)}"

    // Обертываем в Box для точного позиционирования
    Box(
        modifier = Modifier
            .offset(y = topOffset)
            .fillMaxWidth()
            .height(cardHeight)
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = appointment.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                clientName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                Text(
                    text = MoneyUtils.formatCents(appointment.incomeCents),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

