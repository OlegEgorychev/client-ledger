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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import java.time.LocalTime
import kotlin.math.max

// Константы для временной сетки
private const val STEP_MINUTES = 5 // Шаг временной сетки в минутах
private const val MINUTES_PER_HOUR = 60
private const val SLOTS_PER_HOUR = MINUTES_PER_HOUR / STEP_MINUTES // 12 слотов по 5 минут в часе
private const val HOURS_PER_DAY = 24
private const val SLOTS_PER_DAY = HOURS_PER_DAY * SLOTS_PER_HOUR // 288 слотов по 5 минут в дне
private const val SLOT_HEIGHT_DP = 10f // Высота одного 5-минутного слота в dp

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
    val density = LocalDensity.current
    
    // Высота одного 5-минутного слота в dp
    val slotHeightDp = SLOT_HEIGHT_DP.dp
    // Высота одного 5-минутного слота в пикселях
    val slotHeightPx = with(density) { slotHeightDp.toPx() }
    // Высота одного часа (12 слотов по 5 минут)
    val hourHeight = (SLOTS_PER_HOUR * SLOT_HEIGHT_DP).dp
    // Общая высота дня (288 слотов по 5 минут)
    val totalHeight = (SLOTS_PER_DAY * SLOT_HEIGHT_DP).dp
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
                        // Временная сетка (5-минутные блоки с линиями)
                        TimeGrid(
                            slotHeightDp = slotHeightDp,
                            slotHeightPx = slotHeightPx,
                            hourHeight = hourHeight,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Записи
                        appointments.forEach { appointmentWithClient ->
                            AppointmentCardOnTimeline(
                                appointment = appointmentWithClient.appointment,
                                clientName = appointmentWithClient.clientName,
                                slotHeightPx = slotHeightPx,
                                density = density,
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
    slotHeightDp: androidx.compose.ui.unit.Dp,
    slotHeightPx: Float,
    hourHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val hourLineColor = colorScheme.outline.copy(alpha = 0.6f)
    val regularLineColor = colorScheme.outlineVariant.copy(alpha = 0.3f)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = colorScheme.surface
            )
            .drawBehind {
                // Рисуем линии времени
                repeat(SLOTS_PER_DAY + 1) { slotIndex ->
                    val y = slotIndex * slotHeightPx
                    val hour = slotIndex / SLOTS_PER_HOUR
                    val slotInHour = slotIndex % SLOTS_PER_HOUR
                    
                    // Усиленная линия каждый час
                    val isHourLine = slotInHour == 0
                    val lineColor = if (isHourLine) hourLineColor else regularLineColor
                    val lineWidth = if (isHourLine) 2f else 1f
                    
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = lineWidth
                    )
                }
            }
    ) {
        // Фон с чередованием цветов для часов
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(HOURS_PER_DAY) { hour ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight)
                        .background(
                            color = if (hour % 2 == 0) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun AppointmentCardOnTimeline(
    appointment: AppointmentEntity,
    clientName: String?,
    slotHeightPx: Float,
    density: androidx.compose.ui.unit.Density,
    onClick: () -> Unit
) {
    val dateTime = DateUtils.dateTimeToLocalDateTime(appointment.startsAt)
    
    // Время начала записи в минутах от начала дня (00:00)
    val startMinutes = dateTime.hour * MINUTES_PER_HOUR + dateTime.minute
    
    // Нормализуем время начала к ближайшему 5-минутному слоту (округление вниз)
    val normalizedStartMinutes = (startMinutes / STEP_MINUTES) * STEP_MINUTES
    
    // Длительность записи (минимум 15 минут)
    val durationMinutes = max(15, appointment.durationMinutes)
    // Нормализуем длительность к ближайшему 5-минутному слоту (округление вверх)
    val normalizedDurationMinutes = ((durationMinutes + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES
    
    // Вычисляем позицию на основе 5-минутных слотов (используем целые числа)
    val startSlot = normalizedStartMinutes / STEP_MINUTES
    val durationSlots = normalizedDurationMinutes / STEP_MINUTES
    
    // Позиция и высота в пикселях (точные значения)
    val topPx = startSlot * slotHeightPx
    val heightPx = durationSlots * slotHeightPx
    
    // Конвертируем обратно в dp для использования в Modifier
    val topOffsetDp = with(density) { topPx.toDp() }
    val cardHeightDp = with(density) { heightPx.toDp() }
    
    // Время для отображения (используем нормализованное время)
    val displayStartHour = normalizedStartMinutes / MINUTES_PER_HOUR
    val displayStartMinute = normalizedStartMinutes % MINUTES_PER_HOUR
    val displayStartTime = dateTime.toLocalDate().atTime(
        LocalTime.of(displayStartHour, displayStartMinute)
    )
    val displayEndTime = displayStartTime.plusMinutes(normalizedDurationMinutes.toLong())
    
    val timeRange = "${String.format("%02d:%02d", displayStartTime.hour, displayStartTime.minute)} - " +
            "${String.format("%02d:%02d", displayEndTime.hour, displayEndTime.minute)}"

    // Обертываем в Box для точного позиционирования (используем IntOffset для точности)
    Box(
        modifier = Modifier
            .offset { IntOffset(0, topPx.toInt()) }
            .fillMaxWidth()
            .height(cardHeightDp)
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

