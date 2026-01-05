package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDateClick: (LocalDate) -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Обновляем рабочие дни при каждом появлении экрана
    LaunchedEffect(uiState.currentMonth) {
        viewModel.refreshWorkingDays()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Календарь") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAppointment) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Навигация по месяцам
            MonthHeader(
                month = uiState.currentMonth,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) }
            )

            // Календарная сетка
            CalendarGrid(
                month = uiState.currentMonth,
                workingDays = uiState.workingDays,
                onDateClick = { date ->
                    viewModel.selectDate(date)
                    onDateClick(date)
                }
            )
        }
    }
}

@Composable
fun MonthHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий месяц")
        }
        Text(
            text = DateUtils.formatMonth(month),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Следующий месяц")
        }
    }
}

@Composable
fun CalendarGrid(
    month: YearMonth,
    workingDays: Set<String>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()
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
                        val isToday = date == LocalDate.now()
                        val dateKey = date.toDateKey()
                        val hasAppointments = workingDays.contains(dateKey)
                        // dayOfWeek: 0 = понедельник, 5 = суббота, 6 = воскресенье
                        val isWeekend = dayOfWeek == 5 || dayOfWeek == 6
                        
                        CalendarDayCell(
                            day = currentDay,
                            isToday = isToday,
                            hasAppointments = hasAppointments,
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
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    hasAppointments: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Фон для выходных дней (легкая заливка)
        if (isWeekend && !isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
        
        // Фон для текущего дня (круг)
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.7f)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        
        // Текст с числом дня
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            // Индикатор рабочих дней (точка)
            if (hasAppointments) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (isToday) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    appointments: List<AppointmentEntity>,
    expenses: List<ExpenseEntity>,
    onBack: () -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(DateUtils.formatDate(date)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Запись клиента") },
                    onClick = {
                        showMenu = false
                        onAddAppointment()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Расход") },
                    onClick = {
                        showMenu = false
                        onAddExpense()
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Записи (доходы)
            Text(
                text = "Записи клиентов (Доходы)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (appointments.isEmpty()) {
                Text("Нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                appointments.forEach { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        onClick = { onAppointmentClick(appointment) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Divider()

            // Расходы
            Text(
                text = "Расходы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (expenses.isEmpty()) {
                Text("Нет расходов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                expenses.forEach { expense ->
                    ExpenseCard(
                        expense = expense,
                        onClick = { onExpenseClick(expense) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentCard(
    appointment: AppointmentEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = appointment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = DateUtils.formatDateTime(DateUtils.dateTimeToLocalDateTime(appointment.startsAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = MoneyUtils.formatCents(appointment.incomeCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (appointment.isPaid) {
                    Badge {
                        Text("Оплачено")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCard(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DateUtils.formatDateTime(DateUtils.dateTimeToLocalDateTime(expense.spentAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = MoneyUtils.formatCents(expense.amountCents),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


