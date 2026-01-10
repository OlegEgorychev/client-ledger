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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
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
import com.clientledger.app.data.entity.ExpenseEntity
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
private const val WORKING_HOURS_START = 9 // Рабочие часы начинаются с 09:00
private const val WORKING_HOURS_END = 21 // Рабочие часы заканчиваются в 21:00 (inclusive)
private const val WORKING_HOURS_COUNT = WORKING_HOURS_END - WORKING_HOURS_START + 1 // 13 часов (09:00-21:00 inclusive)
private const val SLOTS_PER_WORKING_DAY = WORKING_HOURS_COUNT * SLOTS_PER_HOUR // 156 слотов (13 часов * 12 слотов)
private const val SLOT_HEIGHT_DP = 10f // Высота одного 5-минутного слота в dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScheduleScreen(
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit,
    onAppointmentClick: (Long) -> Unit,
    onExpenseClick: (Long) -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit,
    onDateChange: ((LocalDate) -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
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
                                    color = MaterialTheme.colorScheme.onSurface,
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
                },
                actions = {
                    if (onSettingsClick != null) {
                        IconButton(onClick = { onSettingsClick() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            var showAddChooser by remember { mutableStateOf(false) }
            
            FloatingActionButton(
                onClick = { showAddChooser = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить"
                )
            }
            
            if (showAddChooser) {
                AddChooserDialog(
                    onDismiss = { showAddChooser = false },
                    onAddAppointment = onAddAppointment,
                    onAddExpense = onAddExpense
                )
            }
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
                expenses = uiState.expenses,
                isToday = isToday,
                viewModel = viewModel,
                onAppointmentClick = { appointmentId ->
                    onAppointmentClick(appointmentId)
                },
                onExpenseClick = { expenseId ->
                    onExpenseClick(expenseId)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

// Timeline block data classes for overlap detection
sealed class TimelineBlock {
    abstract val topPx: Float
    abstract val heightPx: Float
    abstract val widthFraction: Float // 0.0 to 1.0
    abstract val offsetFraction: Float // 0.0 to 1.0 (left offset)
    
    data class AppointmentBlock(
        val appointment: AppointmentWithClient,
        override val topPx: Float,
        override val heightPx: Float,
        val hasNextAdjacent: Boolean,
        val hasPrevAdjacent: Boolean,
        override val widthFraction: Float = 1f,
        override val offsetFraction: Float = 0f
    ) : TimelineBlock()
    
    data class ExpenseBlock(
        val expense: ExpenseEntity,
        override val topPx: Float,
        override val heightPx: Float,
        override val widthFraction: Float = 1f,
        override val offsetFraction: Float = 0f
    ) : TimelineBlock()
}

// Fixed duration for expense blocks (15 minutes)
private const val EXPENSE_DURATION_MINUTES = 15

// Build timeline blocks with overlap detection for side-by-side layout
fun buildTimelineBlocks(
    appointments: List<AppointmentWithClient>,
    expenses: List<ExpenseEntity>,
    slotHeightPx: Float
): List<TimelineBlock> {
    val blocks = mutableListOf<TimelineBlock>()
    
    // Build appointment blocks (position relative to working hours start 09:00)
    appointments.forEachIndexed { index, appointmentWithClient ->
        val dateTime = DateUtils.dateTimeToLocalDateTime(appointmentWithClient.appointment.startsAt)
        val startMinutes = dateTime.hour * MINUTES_PER_HOUR + dateTime.minute
        // Calculate position relative to working hours start (09:00)
        val workingDayStartMinutes = WORKING_HOURS_START * 60 // 09:00 = 540 minutes
        val relativeStartMinutes = startMinutes - workingDayStartMinutes
        val normalizedStartMinutes = ((relativeStartMinutes / STEP_MINUTES) * STEP_MINUTES).coerceAtLeast(0)
        val durationMinutes = max(15, appointmentWithClient.appointment.durationMinutes)
        val normalizedDurationMinutes = ((durationMinutes + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES
        val startSlot = normalizedStartMinutes / STEP_MINUTES
        val durationSlots = normalizedDurationMinutes / STEP_MINUTES
        val topPx = startSlot * slotHeightPx
        val heightPx = durationSlots * slotHeightPx
        val endTimeMillis = dateTime.plusMinutes(normalizedDurationMinutes.toLong()).toMillis()
        
        val hasNextAdjacent = index < appointments.size - 1 && 
            appointments[index + 1].appointment.startsAt == endTimeMillis
        
        val hasPrevAdjacent = if (index > 0) {
            val prevDateTime = DateUtils.dateTimeToLocalDateTime(appointments[index - 1].appointment.startsAt)
            val prevDuration = max(15, appointments[index - 1].appointment.durationMinutes)
            val prevNormalizedDuration = ((prevDuration + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES
            val prevEndTimeMillis = prevDateTime.plusMinutes(prevNormalizedDuration.toLong()).toMillis()
            appointmentWithClient.appointment.startsAt == prevEndTimeMillis
        } else {
            false
        }
        
        blocks.add(
            TimelineBlock.AppointmentBlock(
                appointment = appointmentWithClient,
                topPx = topPx,
                heightPx = heightPx,
                hasNextAdjacent = hasNextAdjacent,
                hasPrevAdjacent = hasPrevAdjacent
            )
        )
    }
    
    // Build expense blocks (position relative to working hours start 09:00)
    expenses.forEach { expense ->
        val dateTime = DateUtils.dateTimeToLocalDateTime(expense.spentAt)
        val startMinutes = dateTime.hour * MINUTES_PER_HOUR + dateTime.minute
        // Calculate position relative to working hours start (09:00)
        val workingDayStartMinutes = WORKING_HOURS_START * 60 // 09:00 = 540 minutes
        val relativeStartMinutes = startMinutes - workingDayStartMinutes
        val normalizedStartMinutes = ((relativeStartMinutes / STEP_MINUTES) * STEP_MINUTES).coerceAtLeast(0)
        val normalizedDurationMinutes = ((EXPENSE_DURATION_MINUTES + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES
        val startSlot = normalizedStartMinutes / STEP_MINUTES
        val durationSlots = normalizedDurationMinutes / STEP_MINUTES
        val topPx = startSlot * slotHeightPx
        val heightPx = durationSlots * slotHeightPx
        
        blocks.add(
            TimelineBlock.ExpenseBlock(
                expense = expense,
                topPx = topPx,
                heightPx = heightPx
            )
        )
    }
    
    // Detect overlaps and adjust layout for side-by-side
    val sortedBlocks = blocks.sortedBy { it.topPx }
    
    // For each block, find overlapping blocks and apply side-by-side layout
    val result = mutableListOf<TimelineBlock>()
    sortedBlocks.forEach { block ->
        // Find all blocks that overlap with this block
        val overlappingBlocks = sortedBlocks.filter { otherBlock ->
            block != otherBlock && overlaps(block, otherBlock)
        }
        
        // Check if we have appointment+expense overlap for side-by-side layout
        val hasAppointmentOverlap = overlappingBlocks.any { it is TimelineBlock.AppointmentBlock }
        val hasExpenseOverlap = overlappingBlocks.any { it is TimelineBlock.ExpenseBlock }
        val isAppointmentExpenseOverlap = (block is TimelineBlock.AppointmentBlock && hasExpenseOverlap) ||
                (block is TimelineBlock.ExpenseBlock && hasAppointmentOverlap)
        
        if (isAppointmentExpenseOverlap && overlappingBlocks.size == 1) {
            // Side-by-side layout: appointment on left, expense on right
            if (block is TimelineBlock.AppointmentBlock) {
                // Appointment: left side (50% width)
                result.add(
                    TimelineBlock.AppointmentBlock(
                        appointment = block.appointment,
                        topPx = block.topPx,
                        heightPx = block.heightPx,
                        hasNextAdjacent = block.hasNextAdjacent,
                        hasPrevAdjacent = block.hasPrevAdjacent,
                        widthFraction = 0.5f,
                        offsetFraction = 0f
                    )
                )
            } else if (block is TimelineBlock.ExpenseBlock) {
                // Expense: right side (50% width)
                result.add(
                    TimelineBlock.ExpenseBlock(
                        expense = block.expense,
                        topPx = block.topPx,
                        heightPx = block.heightPx,
                        widthFraction = 0.5f,
                        offsetFraction = 0.5f
                    )
                )
            }
        } else {
            // No overlaps or multiple overlaps: use default layout
            result.add(block)
        }
    }
    
    return result
}

// Check if two timeline blocks overlap
fun overlaps(block1: TimelineBlock, block2: TimelineBlock): Boolean {
    val block1End = block1.topPx + block1.heightPx
    val block2End = block2.topPx + block2.heightPx
    return !(block1End <= block2.topPx || block2End <= block1.topPx)
}

@Composable
fun DayScheduleContent(
    appointments: List<AppointmentWithClient>,
    expenses: List<ExpenseEntity>,
    isToday: Boolean,
    viewModel: DayScheduleViewModel,
    onAppointmentClick: (Long) -> Unit,
    onExpenseClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Высота одного 5-минутного слота в dp
    val slotHeightDp = SLOT_HEIGHT_DP.dp
    // Высота одного 5-минутного слота в пикселях
    val slotHeightPx = with(density) { slotHeightDp.toPx() }
    // Высота одного часа (12 слотов по 5 минут)
    val hourHeight = (SLOTS_PER_HOUR * SLOT_HEIGHT_DP).dp
    // Общая высота рабочего дня (09:00-21:00, 156 слотов по 5 минут)
    val totalHeight = (SLOTS_PER_WORKING_DAY * SLOT_HEIGHT_DP).dp
    val scrollState = rememberScrollState()
    
    // Filter appointments and expenses: in-range (09:00-21:00 inclusive) vs out-of-range
    val (inRangeAppointments, outOfRangeAppointments) = appointments.partition { appointment ->
        val dateTime = DateUtils.dateTimeToLocalDateTime(appointment.appointment.startsAt)
        val hour = dateTime.hour
        val minute = dateTime.minute
        // In range if hour is between 9 and 20, or exactly 9:00 or 21:00 (21:00 inclusive means minute == 0)
        hour in WORKING_HOURS_START until WORKING_HOURS_END || 
        (hour == WORKING_HOURS_START) || 
        (hour == WORKING_HOURS_END && minute == 0)
    }
    
    val (inRangeExpenses, outOfRangeExpenses) = expenses.partition { expense ->
        val dateTime = DateUtils.dateTimeToLocalDateTime(expense.spentAt)
        val hour = dateTime.hour
        val minute = dateTime.minute
        // In range if hour is between 9 and 20, or exactly 9:00 or 21:00 (21:00 inclusive means minute == 0)
        hour in WORKING_HOURS_START until WORKING_HOURS_END || 
        (hour == WORKING_HOURS_START) || 
        (hour == WORKING_HOURS_END && minute == 0)
    }
    
    // Автопрокрутка к ближайшей записи при открытии экрана, обновлении записей или смене даты
    // Добавляем viewModel.uiState.value.date в зависимости, чтобы автоскролл срабатывал при смене даты
    val currentDate = viewModel.uiState.value.date
    LaunchedEffect(appointments.size, expenses.size, isToday, currentDate) {
        delay(150) // Небольшая задержка для завершения рендеринга
        if (appointments.isNotEmpty() || expenses.isNotEmpty() || isToday) {
            val scrollOffset = viewModel.getScrollOffset(isToday, slotHeightPx)
            scrollState.scrollTo(scrollOffset.toInt())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Out-of-range section (items before 09:00 or after 21:00)
        if (outOfRangeAppointments.isNotEmpty() || outOfRangeExpenses.isNotEmpty()) {
            OutOfRangeSection(
                outOfRangeAppointments = outOfRangeAppointments,
                outOfRangeExpenses = outOfRangeExpenses,
                onAppointmentClick = onAppointmentClick,
                onExpenseClick = onExpenseClick,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        
        // Main timeline (09:00-21:00)
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Левая колонка с часами (уменьшенная ширина)
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(totalHeight)
                ) {
                    TimeColumn(
                        slotHeightPx = slotHeightPx,
                        hourHeight = hourHeight,
                        density = density,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Gutter (небольшой отступ между временем и сеткой)
                Spacer(modifier = Modifier.width(8.dp))

                // Правая колонка с временной сеткой и записями
                Box(
                    modifier = Modifier
                        .weight(1f)
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

                    // Build timeline blocks with overlap detection (only in-range items)
                    val sortedAppointments = inRangeAppointments.sortedBy { it.appointment.startsAt }
                    val sortedExpenses = inRangeExpenses.sortedBy { it.spentAt }
                    
                    val timelineBlocks = buildTimelineBlocks(
                        appointments = sortedAppointments,
                        expenses = sortedExpenses,
                        slotHeightPx = slotHeightPx
                    )
                    
                    // Render timeline blocks
                    timelineBlocks.forEach { block ->
                        when (block) {
                            is TimelineBlock.AppointmentBlock -> {
                                AppointmentCardOnTimeline(
                                    appointment = block.appointment.appointment,
                                    clientName = block.appointment.clientName,
                                    slotHeightPx = slotHeightPx,
                                    density = density,
                                    hasNextAdjacent = block.hasNextAdjacent,
                                    hasPrevAdjacent = block.hasPrevAdjacent,
                                    widthFraction = block.widthFraction,
                                    offsetFraction = block.offsetFraction,
                                    onClick = { onAppointmentClick(block.appointment.appointment.id) }
                                )
                            }
                            is TimelineBlock.ExpenseBlock -> {
                                ExpenseCardOnTimeline(
                                    expense = block.expense,
                                    slotHeightPx = slotHeightPx,
                                    density = density,
                                    widthFraction = block.widthFraction,
                                    offsetFraction = block.offsetFraction,
                                    onClick = { onExpenseClick(block.expense.id) }
                                )
                            }
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
            .height((SLOTS_PER_WORKING_DAY * slotHeightPx).dp)
            .fillMaxWidth()
    ) {
        // Рисуем метки времени точно на часовых линиях (только рабочие часы 09:00-21:00)
        repeat(WORKING_HOURS_COUNT) { hourIndex ->
            val hour = WORKING_HOURS_START + hourIndex
            // Y-позиция часовой линии в пикселях (относительно начала рабочего дня 09:00)
            val hourSlotIndex = hourIndex * SLOTS_PER_HOUR
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
                // Рисуем линии времени (только рабочие часы 09:00-21:00)
                repeat(SLOTS_PER_WORKING_DAY + 1) { slotIndex ->
                    val y = slotIndex * slotHeightPx
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
        // Фон с чередованием цветов для часов (только рабочие часы)
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(WORKING_HOURS_COUNT) { hourIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight)
                        .background(
                            color = if (hourIndex % 2 == 0) {
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
    
    // Only show indicator if current time is within working hours (09:00-21:00)
    val isInWorkingHours = now.hour in WORKING_HOURS_START until WORKING_HOURS_END ||
            (now.hour == WORKING_HOURS_START && now.minute == 0) ||
            (now.hour == WORKING_HOURS_END && now.minute == 0)
    
    if (!isInWorkingHours) {
        return // Don't render indicator outside working hours
    }
    
    // Calculate position relative to working hours start (09:00)
    val workingDayStartMinutes = WORKING_HOURS_START * 60 // 09:00 = 540 minutes
    val relativeMinutes = nowMinutes - workingDayStartMinutes
    val normalizedMinutes = ((relativeMinutes / 5) * 5).coerceAtLeast(0) // Нормализуем к 5-минутным слотам
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
    widthFraction: Float = 1f,
    offsetFraction: Float = 0f,
    onClick: () -> Unit
) {
    val dateTime = DateUtils.dateTimeToLocalDateTime(appointment.startsAt)
    
    // Время начала записи в минутах от начала дня (00:00)
    val startMinutes = dateTime.hour * MINUTES_PER_HOUR + dateTime.minute
    
    // Calculate position relative to working hours start (09:00)
    val workingDayStartMinutes = WORKING_HOURS_START * 60 // 09:00 = 540 minutes
    val relativeStartMinutes = startMinutes - workingDayStartMinutes
    
    // Нормализуем время начала к ближайшему 5-минутному слоту (округление вниз)
    val normalizedStartMinutes = ((relativeStartMinutes / STEP_MINUTES) * STEP_MINUTES).coerceAtLeast(0)
    
    // Длительность записи (минимум 15 минут)
    val durationMinutes = max(15, appointment.durationMinutes)
    // Нормализуем длительность к ближайшему 5-минутному слоту (округление вверх)
    val normalizedDurationMinutes = ((durationMinutes + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES
    
    // Вычисляем позицию на основе 5-минутных слотов относительно 09:00
    val startSlot = normalizedStartMinutes / STEP_MINUTES
    val durationSlots = normalizedDurationMinutes / STEP_MINUTES
    
    // Позиция и высота в пикселях (точные значения, относительно 09:00)
    val topPx = startSlot * slotHeightPx
    val heightPx = durationSlots * slotHeightPx
    
    // Конвертируем обратно в dp для использования в Modifier
    val topOffsetDp = with(density) { topPx.toDp() }
    val cardHeightDp = with(density) { heightPx.toDp() }
    
    // Время для отображения (используем реальное время из appointment)
    val displayStartTime = dateTime.toLocalTime()
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
        // Apply width and offset for side-by-side layout using Row
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left offset spacer
            if (offsetFraction > 0f) {
                Spacer(modifier = Modifier.fillMaxWidth(offsetFraction))
            }
            
            // Card with adjusted width
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight()
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCardOnTimeline(
    expense: ExpenseEntity,
    slotHeightPx: Float,
    density: androidx.compose.ui.unit.Density,
    widthFraction: Float = 1f,
    offsetFraction: Float = 0f,
    onClick: () -> Unit
) {
    val dateTime = DateUtils.dateTimeToLocalDateTime(expense.spentAt)
    
    // Время начала расхода в минутах от начала дня (00:00)
    val startMinutes = dateTime.hour * MINUTES_PER_HOUR + dateTime.minute
    
    // Calculate position relative to working hours start (09:00)
    val workingDayStartMinutes = WORKING_HOURS_START * 60 // 09:00 = 540 minutes
    val relativeStartMinutes = startMinutes - workingDayStartMinutes
    
    // Нормализуем время начала к ближайшему 5-минутному слоту (округление вниз)
    val normalizedStartMinutes = ((relativeStartMinutes / STEP_MINUTES) * STEP_MINUTES).coerceAtLeast(0)
    
    // Фиксированная длительность расхода (15 минут)
    val normalizedDurationMinutes = ((EXPENSE_DURATION_MINUTES + STEP_MINUTES - 1) / STEP_MINUTES) * STEP_MINUTES
    
    // Вычисляем позицию на основе 5-минутных слотов относительно 09:00
    val startSlot = normalizedStartMinutes / STEP_MINUTES
    val durationSlots = normalizedDurationMinutes / STEP_MINUTES
    
    // Позиция и высота в пикселях (относительно 09:00)
    val topPx = startSlot * slotHeightPx
    val heightPx = durationSlots * slotHeightPx
    
    // Конвертируем обратно в dp для использования в Modifier
    val topOffsetDp = with(density) { topPx.toDp() }
    val cardHeightDp = with(density) { heightPx.toDp() }
    
    // Время для отображения (используем реальное время из expense)
    val displayTime = dateTime.toLocalTime()
    val timeText = String.format("%02d:%02d", displayTime.hour, displayTime.minute)
    
    val colorScheme = MaterialTheme.colorScheme
    val cornerRadius = 8.dp
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = colorScheme.error.copy(alpha = 0.5f)
    val borderWidth = 1.dp
    
    // Обертываем в Box для точного позиционирования
    Box(
        modifier = Modifier
            .offset { IntOffset(0, topPx.toInt()) }
            .fillMaxWidth()
            .height(cardHeightDp)
    ) {
        // Apply width and offset for side-by-side layout using Row
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left offset spacer
            if (offsetFraction > 0f) {
                Spacer(modifier = Modifier.fillMaxWidth(offsetFraction))
            }
            
            // Card with adjusted width
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight()
            ) {
                Card(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = shape
                        ),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.errorContainer.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Расход",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = MoneyUtils.formatCents(expense.totalAmountCents),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OutOfRangeSection(
    outOfRangeAppointments: List<AppointmentWithClient>,
    outOfRangeExpenses: List<ExpenseEntity>,
    onAppointmentClick: (Long) -> Unit,
    onExpenseClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (outOfRangeAppointments.isEmpty() && outOfRangeExpenses.isEmpty()) {
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Вне 09:00–21:00",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Out-of-range appointments
        outOfRangeAppointments.forEach { appointmentWithClient ->
            val dateTime = DateUtils.dateTimeToLocalDateTime(appointmentWithClient.appointment.startsAt)
            val timeText = String.format("%02d:%02d", dateTime.hour, dateTime.minute)
            
            Card(
                onClick = { onAppointmentClick(appointmentWithClient.appointment.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appointmentWithClient.appointment.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        appointmentWithClient.clientName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = MoneyUtils.formatCents(appointmentWithClient.appointment.incomeCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Out-of-range expenses
        outOfRangeExpenses.forEach { expense ->
            val dateTime = DateUtils.dateTimeToLocalDateTime(expense.spentAt)
            val timeText = String.format("%02d:%02d", dateTime.hour, dateTime.minute)
            
            Card(
                onClick = { onExpenseClick(expense.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Расход",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = MoneyUtils.formatCents(expense.totalAmountCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

