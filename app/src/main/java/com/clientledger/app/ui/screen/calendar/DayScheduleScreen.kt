package com.clientledger.app.ui.screen.calendar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
    onAppointmentClick: (Long) -> Unit,
    onDateChange: ((LocalDate) -> Unit)? = null
) {
    // Используем date как источник истины для selectedDate
    var selectedDate by remember { mutableStateOf(date) }
    
    // Синхронизируем selectedDate с date из route
    LaunchedEffect(date) {
        selectedDate = date
    }
    
    // ViewModel инициализируем с date из route
    val viewModel: DayScheduleViewModel = viewModel(
        factory = DayScheduleViewModelFactory(date, repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isToday = selectedDate == LocalDate.now()
    val today = LocalDate.now()
    // Всегда показываем кнопку "Назад" (экран открывается поверх вкладок)
    val showBackButton = true

    // Обновляем ViewModel при изменении selectedDate
    LaunchedEffect(selectedDate) {
        if (viewModel.uiState.value.date != selectedDate) {
            viewModel.updateDate(selectedDate)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Всегда показываем дату с кнопками навигации
                    if (isToday) {
                            // Для "Сегодня" - центрируем текст, кнопки по краям
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Текст "Сегодня" - абсолютно по центру
                                Text(
                                    text = "Сегодня, ${DateUtils.formatShortDate(selectedDate)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                
                                // Кнопки навигации по краям
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Левая группа: стрелка влево и иконка "перейти к сегодня"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Стрелка влево (предыдущий день)
                                        IconButton(
                                            onClick = {
                                                selectedDate = selectedDate.minusDays(1)
                                            }
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий день")
                                        }
                                        
                                        // Иконка "перейти к сегодня" - всегда присутствует, но невидима для сегодня
                                        IconButton(
                                            onClick = {
                                                selectedDate = today
                                            },
                                            enabled = false,
                                            modifier = Modifier.alpha(0f)
                                        ) {
                                            Icon(
                                                Icons.Default.Today,
                                                contentDescription = "Вернуться к сегодня",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    // Правая стрелка (следующий день)
                                    IconButton(
                                        onClick = {
                                            selectedDate = selectedDate.plusDays(1)
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Следующий день")
                                    }
                                }
                            }
                        } else {
                            // Для других дней - как было (Row с элементами слева направо)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Стрелка влево (предыдущий день)
                                IconButton(
                                    onClick = {
                                        selectedDate = selectedDate.minusDays(1)
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий день")
                                }
                                
                                // Иконка "перейти к сегодня" - всегда присутствует, но видна только если не сегодня
                                IconButton(
                                    onClick = {
                                        selectedDate = today
                                    },
                                    enabled = !isToday,
                                    modifier = Modifier.alpha(if (isToday) 0f else 1f)
                                ) {
                                    Icon(
                                        Icons.Default.Today,
                                        contentDescription = "Вернуться к сегодня",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Фиксированный отступ для стабильности layout
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Дата
                                Text(
                                    text = DateUtils.formatDateWithWeekday(selectedDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Стрелка вправо (следующий день)
                                IconButton(
                                    onClick = {
                                        selectedDate = selectedDate.plusDays(1)
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Следующий день")
                                }
                            }
                        }
                },
                navigationIcon = {
                    // Кнопка "Назад" не нужна, так как BottomBar всегда виден
                    // Пользователь может переключиться на другую вкладку
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
                isToday = isToday,
                viewModel = viewModel,
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
    isToday: Boolean,
    viewModel: DayScheduleViewModel,
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
    
    // Автопрокрутка к ближайшей записи при открытии экрана, обновлении записей или смене даты
    // Добавляем viewModel.uiState.value.date в зависимости, чтобы автоскролл срабатывал при смене даты
    val currentDate = viewModel.uiState.value.date
    LaunchedEffect(appointments.size, isToday, currentDate) {
        delay(150) // Небольшая задержка для завершения рендеринга
        if (appointments.isNotEmpty() || isToday) {
            val scrollOffset = viewModel.getScrollOffset(isToday, slotHeightPx)
            scrollState.scrollTo(scrollOffset.toInt())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Левая колонка с часами (уменьшенная ширина)
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    TimeColumn(
                        slotHeightPx = slotHeightPx,
                        hourHeight = hourHeight,
                        density = density,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Gutter (небольшой отступ между временем и сеткой)
            Spacer(modifier = Modifier.width(8.dp))

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

                        // Линия текущего времени (только для сегодня)
                        if (isToday) {
                            CurrentTimeIndicator(
                                slotHeightPx = slotHeightPx,
                                density = density,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Записи (сортируем по времени начала для правильного определения соседних)
                        val sortedAppointments = appointments.sortedBy { it.appointment.startsAt }
                        sortedAppointments.forEachIndexed { index, appointmentWithClient ->
                            // Вычисляем время окончания текущей записи (в миллисекундах)
                            val currentStartDateTime = DateUtils.dateTimeToLocalDateTime(appointmentWithClient.appointment.startsAt)
                            val currentEndDateTime = currentStartDateTime.plusMinutes(
                                max(15, appointmentWithClient.appointment.durationMinutes).toLong()
                            )
                            val currentEndTimeMillis = currentEndDateTime.toMillis()
                            
                            // Определяем, есть ли следующая запись, которая начинается сразу после этой
                            val hasNextAdjacent = index < sortedAppointments.size - 1 && 
                                sortedAppointments[index + 1].appointment.startsAt == currentEndTimeMillis
                            
                            // Определяем, есть ли предыдущая запись, которая заканчивается прямо перед этой
                            val hasPrevAdjacent = if (index > 0) {
                                val prevStartDateTime = DateUtils.dateTimeToLocalDateTime(sortedAppointments[index - 1].appointment.startsAt)
                                val prevEndDateTime = prevStartDateTime.plusMinutes(
                                    max(15, sortedAppointments[index - 1].appointment.durationMinutes).toLong()
                                )
                                val prevEndTimeMillis = prevEndDateTime.toMillis()
                                appointmentWithClient.appointment.startsAt == prevEndTimeMillis
                            } else {
                                false
                            }
                            
                            AppointmentCardOnTimeline(
                                appointment = appointmentWithClient.appointment,
                                clientName = appointmentWithClient.clientName,
                                slotHeightPx = slotHeightPx,
                                density = density,
                                hasNextAdjacent = hasNextAdjacent,
                                hasPrevAdjacent = hasPrevAdjacent,
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
    slotHeightPx: Float,
    hourHeight: androidx.compose.ui.unit.Dp,
    density: androidx.compose.ui.unit.Density,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodySmall
    val textMeasurer = rememberTextMeasurer()
    
    // Измеряем высоту текста для выравнивания базовой линии
    val sampleText = "00:00"
    val textLayoutResult = textMeasurer.measure(sampleText, textStyle)
    val textHeightPx = textLayoutResult.size.height.toFloat()
    val baselineOffsetPx = textLayoutResult.firstBaseline.toFloat()
    
    Box(
        modifier = modifier
            .height((SLOTS_PER_DAY * slotHeightPx).dp)
            .fillMaxWidth()
    ) {
        // Рисуем метки времени точно на часовых линиях
        repeat(HOURS_PER_DAY) { hour ->
            // Y-позиция часовой линии в пикселях (единый расчет с TimeGrid)
            val hourSlotIndex = hour * SLOTS_PER_HOUR
            val yPx = hourSlotIndex * slotHeightPx
            
            // Выравниваем базовую линию текста с часовой линией
            // offset = yPx - baselineOffsetPx (чтобы базовая линия была на yPx)
            val textOffsetPx = yPx - baselineOffsetPx
            val textOffsetDp = with(density) { textOffsetPx.toDp() }
            
            Text(
                text = String.format("%02d:00", hour),
                style = textStyle,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(y = textOffsetDp)
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            )
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

/**
 * Индикатор текущего времени на таймлайне
 */
@Composable
fun CurrentTimeIndicator(
    slotHeightPx: Float,
    density: androidx.compose.ui.unit.Density,
    modifier: Modifier = Modifier
) {
    val now = remember { LocalTime.now() }
    val nowMinutes = now.hour * 60 + now.minute
    val normalizedMinutes = (nowMinutes / 5) * 5 // Нормализуем к 5-минутным слотам
    val startSlot = normalizedMinutes / 5
    val yPx = startSlot * slotHeightPx
    
    // Анимация мигания для видимости
    val infiniteTransition = rememberInfiniteTransition(label = "time_indicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "time_indicator_alpha"
    )
    
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    
    Canvas(
        modifier = modifier
    ) {
        val lineColor = primaryColor.copy(alpha = alpha)
        
        drawLine(
            color = lineColor,
            start = Offset(0f, yPx),
            end = Offset(size.width, yPx),
            strokeWidth = 3f
        )
        
        // Кружок на линии для акцента
        drawCircle(
            color = lineColor,
            radius = 6.dp.toPx(),
            center = Offset(0f, yPx)
        )
    }
}

@Composable
fun AppointmentCardOnTimeline(
    appointment: AppointmentEntity,
    clientName: String?,
    slotHeightPx: Float,
    density: androidx.compose.ui.unit.Density,
    hasNextAdjacent: Boolean = false,
    hasPrevAdjacent: Boolean = false,
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

    // Определяем скругление углов в зависимости от соседних записей
    val cornerRadius = 8.dp
    val shape = when {
        hasPrevAdjacent && hasNextAdjacent -> RoundedCornerShape(0.dp) // Нет скругления, если есть соседи сверху и снизу
        hasPrevAdjacent -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        ) // Скругление только снизу
        hasNextAdjacent -> RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ) // Скругление только сверху
        else -> RoundedCornerShape(cornerRadius) // Полное скругление
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val isCanceled = appointment.status == com.clientledger.app.data.entity.AppointmentStatus.CANCELED.name
    val borderColor = colorScheme.outline.copy(alpha = 0.5f)
    val borderWidth = 1.dp
    
    // Обертываем в Box для точного позиционирования (используем IntOffset для точности)
    Box(
        modifier = Modifier
            .offset { IntOffset(0, topPx.toInt()) }
            .fillMaxWidth()
            .height(cardHeightDp)
    ) {
        // Разделитель между соседними записями (если есть следующая запись)
        if (hasNextAdjacent) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                color = borderColor,
                thickness = 1.dp
            )
        }
        
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = shape
                )
                .then(
                    if (isCanceled) {
                        Modifier.alpha(0.5f)
                    } else {
                        Modifier
                    }
                ),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (isCanceled) {
                    colorScheme.surfaceVariant
                } else {
                    colorScheme.primaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCanceled) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Отменено",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = appointment.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (isCanceled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCanceled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    }
                )
                clientName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCanceled) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        },
                        maxLines = 1
                    )
                }
                if (!isCanceled) {
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
}

