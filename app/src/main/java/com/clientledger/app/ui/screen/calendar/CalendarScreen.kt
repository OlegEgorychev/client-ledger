package com.clientledger.app.ui.screen.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDateClick: (LocalDate) -> Unit,
    onAddAppointment: () -> Unit,
    onAddAppointmentForDate: ((LocalDate) -> Unit)? = null,
    onAddExpense: () -> Unit,
    onIncomeDetailClick: ((com.clientledger.app.ui.viewmodel.StatsPeriod, LocalDate, YearMonth, Int) -> Unit)? = null,
    repository: com.clientledger.app.data.repository.LedgerRepository? = null,
    themePreferences: com.clientledger.app.data.preferences.ThemePreferences? = null,
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
            var showAddChooser by remember { mutableStateOf(false) }
            
            FloatingActionButton(onClick = { showAddChooser = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // Reserve space for FAB so bottom widgets don't overlap with the "+" button
                .padding(bottom = 88.dp)
                // Make the whole screen scrollable for small heights / large fontScale
                .verticalScroll(scrollState)
        ) {
            // Навигация по месяцам
            MonthHeader(
                month = uiState.currentMonth,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) }
            )

            // Календарная сетка в Card для лучшей читаемости
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                CalendarGrid(
                    month = uiState.currentMonth,
                    workingDays = uiState.workingDays,
                    selectedDate = uiState.selectedDate,
                    onDateClick = { date ->
                        viewModel.selectDate(date)
                        onDateClick(date)
                    },
                    onDateLongClick = { date ->
                        // Long press opens New Appointment screen with pre-selected date
                        onAddAppointmentForDate?.invoke(date) ?: onAddAppointment()
                    }
                )
            }
            
            // Profit widgets (expandable with income/expense details)
            Spacer(modifier = Modifier.height(16.dp))
            if (repository != null && onIncomeDetailClick != null) {
                ExpandableProfitWidgets(
                    repository = repository,
                    onIncomeDetailClick = onIncomeDetailClick
                )
            }
            
            // Theme selector (between stats widgets and version)
            // TODO: Uncomment to restore theme switcher
            // if (themePreferences != null) {
            //     Spacer(modifier = Modifier.height(16.dp))
            //     ThemeSelector(themePreferences = themePreferences)
            // }
            
            // Version info at the bottom
            Spacer(modifier = Modifier.height(8.dp))
            VersionInfo(repository = repository, viewModel = viewModel)
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
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Предыдущий месяц",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = DateUtils.formatMonth(month),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Следующий месяц",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CalendarGrid(
    month: YearMonth,
    workingDays: Set<String>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // Понедельник = 0
    val daysInMonth = month.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
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
                        val isSelected = selectedDate == date
                        val dateKey = date.toDateKey()
                        val hasAppointments = workingDays.contains(dateKey)
                        // dayOfWeek: 0 = понедельник, 5 = суббота, 6 = воскресенье
                        val isWeekend = dayOfWeek == 5 || dayOfWeek == 6
                        
                        CalendarDayCell(
                            day = currentDay,
                            isToday = isToday,
                            isSelected = isSelected,
                            hasAppointments = hasAppointments,
                            isWeekend = isWeekend,
                            onClick = { onDateClick(date) },
                            onLongClick = { onDateLongClick(date) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasAppointments: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Single clear selection marker: filled circle for selected date.
        // If not selected, optionally outline "today" (subtle, secondary).
        val circleModifier = Modifier.fillMaxSize(0.7f)
        when {
            isSelected -> {
                Box(
                    modifier = circleModifier.background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
                )
            }
            isToday -> {
                Box(
                    modifier = circleModifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50)
                    )
                )
            }
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            // Dot indicator only for "has appointments/events".
            // If selected day has no appointments, do not show the dot.
            if (hasAppointments) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            },
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
    repository: LedgerRepository,
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
                        repository = repository,
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
    val isCanceled = appointment.status == com.clientledger.app.data.entity.AppointmentStatus.CANCELED.name
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = appointment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCanceled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
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
                    color = MaterialTheme.colorScheme.onSurface
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
    repository: LedgerRepository,
    onClick: () -> Unit
) {
    var expenseItems by remember { mutableStateOf<List<com.clientledger.app.data.entity.ExpenseItemEntity>>(emptyList()) }
    
    LaunchedEffect(expense.id) {
        expenseItems = repository.getExpenseItems(expense.id)
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (expenseItems.isNotEmpty()) {
                        Text(
                            text = expenseItems.joinToString(", ") { it.tag.displayName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Расход",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = DateUtils.formatDate(LocalDate.parse(expense.dateKey)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = MoneyUtils.formatCents(expense.totalAmountCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            expense.note?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StatisticsWidgets(
    repository: LedgerRepository,
    onTodayClick: () -> Unit,
    onMonthClick: () -> Unit,
    onYearClick: () -> Unit
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val currentYear = today.year
    
    // Today income
    var todayIncome by remember { mutableStateOf(0L) }
    
    // Month income
    var monthIncome by remember { mutableStateOf(0L) }
    
    // Year income
    var yearIncome by remember { mutableStateOf(0L) }
    
    // Refresh when appointments change - observe appointments flow
    val todayAppointments = repository.getAppointmentsByDate(today.toDateKey())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    val monthAppointments = repository.getAppointmentsByDateRange(
        DateUtils.getStartOfMonth(currentMonth).toDateKey(),
        DateUtils.getEndOfMonth(currentMonth).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val yearAppointments = repository.getAppointmentsByDateRange(
        DateUtils.getStartOfYear(currentYear).toDateKey(),
        DateUtils.getEndOfYear(currentYear).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Load today income when date or appointments change
    LaunchedEffect(today, todayAppointments.value) {
        val dateKey = today.toDateKey()
        todayIncome = repository.getIncomeForDateRange(dateKey, dateKey)
    }
    
    // Load month income when month or appointments change
    LaunchedEffect(currentMonth, monthAppointments.value) {
        val startDate = DateUtils.getStartOfMonth(currentMonth).toDateKey()
        val endDate = DateUtils.getEndOfMonth(currentMonth).toDateKey()
        monthIncome = repository.getIncomeForDateRange(startDate, endDate)
    }
    
    // Load year income when year or appointments change
    LaunchedEffect(currentYear, yearAppointments.value) {
        val startDate = DateUtils.getStartOfYear(currentYear).toDateKey()
        val endDate = DateUtils.getEndOfYear(currentYear).toDateKey()
        yearIncome = repository.getIncomeForDateRange(startDate, endDate)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Today widget
        StatisticsWidget(
            title = "Доход сегодня",
            value = MoneyUtils.formatCents(todayIncome),
            onClick = onTodayClick,
            modifier = Modifier.weight(1f)
        )
        
        // Month widget
        StatisticsWidget(
            title = "Доход месяц",
            value = MoneyUtils.formatCents(monthIncome),
            onClick = onMonthClick,
            modifier = Modifier.weight(1f)
        )
        
        // Year widget
        StatisticsWidget(
            title = "Доход год",
            value = MoneyUtils.formatCents(yearIncome),
            onClick = onYearClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatisticsWidget(
    title: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ExpandableProfitWidgets(
    repository: LedgerRepository,
    onIncomeDetailClick: (com.clientledger.app.ui.viewmodel.StatsPeriod, LocalDate, YearMonth, Int) -> Unit
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val currentYear = today.year
    
    // Expanded card state (only one can be expanded at a time)
    var expandedCard by remember { mutableStateOf<ProfitPeriod?>(null) }
    
    // Today data
    var todayIncome by remember { mutableStateOf(0L) }
    var todayExpenses by remember { mutableStateOf(0L) }
    var todayNet by remember { mutableStateOf(0L) }
    
    // Month data
    var monthIncome by remember { mutableStateOf(0L) }
    var monthExpenses by remember { mutableStateOf(0L) }
    var monthNet by remember { mutableStateOf(0L) }
    
    // Year data
    var yearIncome by remember { mutableStateOf(0L) }
    var yearExpenses by remember { mutableStateOf(0L) }
    var yearNet by remember { mutableStateOf(0L) }
    
    // Observe data changes
    val todayAppointments = repository.getAppointmentsByDate(today.toDateKey())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val todayExpensesFlow = repository.getExpensesByDate(today.toDateKey())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    val monthAppointments = repository.getAppointmentsByDateRange(
        DateUtils.getStartOfMonth(currentMonth).toDateKey(),
        DateUtils.getEndOfMonth(currentMonth).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    val monthExpensesFlow = repository.getExpensesByDateRange(
        DateUtils.getStartOfMonth(currentMonth).toDateKey(),
        DateUtils.getEndOfMonth(currentMonth).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val yearAppointments = repository.getAppointmentsByDateRange(
        DateUtils.getStartOfYear(currentYear).toDateKey(),
        DateUtils.getEndOfYear(currentYear).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    val yearExpensesFlow = repository.getExpensesByDateRange(
        DateUtils.getStartOfYear(currentYear).toDateKey(),
        DateUtils.getEndOfYear(currentYear).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val scope = rememberCoroutineScope()
    
    // Load today data
    LaunchedEffect(today, todayAppointments.value, todayExpensesFlow.value) {
        scope.launch {
            todayIncome = repository.getIncomeForDay(today)
            todayExpenses = repository.getExpensesForDay(today)
            todayNet = repository.getNetProfitForDay(today)
        }
    }
    
    // Load month data
    LaunchedEffect(currentMonth, monthAppointments.value, monthExpensesFlow.value) {
        scope.launch {
            monthIncome = repository.getIncomeForMonth(currentYear, currentMonth.monthValue)
            monthExpenses = repository.getExpensesForMonth(currentYear, currentMonth.monthValue)
            monthNet = repository.getNetProfitForMonth(currentYear, currentMonth.monthValue)
        }
    }
    
    // Load year data
    LaunchedEffect(currentYear, yearAppointments.value, yearExpensesFlow.value) {
        scope.launch {
            yearIncome = repository.getIncomeForYear(currentYear)
            yearExpenses = repository.getExpensesForYear(currentYear)
            yearNet = repository.getNetProfitForYear(currentYear)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Today widget
        ExpandableProfitWidget(
            period = ProfitPeriod.TODAY,
            caption = "Сегодня",
            profit = todayNet,
            income = todayIncome,
            expenses = todayExpenses,
            isExpanded = expandedCard == ProfitPeriod.TODAY,
            onToggle = {
                expandedCard = if (expandedCard == ProfitPeriod.TODAY) null else ProfitPeriod.TODAY
            },
            onIncomeClick = {
                onIncomeDetailClick(
                    com.clientledger.app.ui.viewmodel.StatsPeriod.DAY,
                    today,
                    YearMonth.now(),
                    today.year
                )
            },
            modifier = Modifier.weight(1f)
        )
        
        // Month widget
        ExpandableProfitWidget(
            period = ProfitPeriod.MONTH,
            caption = "Месяц",
            profit = monthNet,
            income = monthIncome,
            expenses = monthExpenses,
            isExpanded = expandedCard == ProfitPeriod.MONTH,
            onToggle = {
                expandedCard = if (expandedCard == ProfitPeriod.MONTH) null else ProfitPeriod.MONTH
            },
            onIncomeClick = {
                onIncomeDetailClick(
                    com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH,
                    LocalDate.now(),
                    currentMonth,
                    currentMonth.year
                )
            },
            modifier = Modifier.weight(1f)
        )
        
        // Year widget
        ExpandableProfitWidget(
            period = ProfitPeriod.YEAR,
            caption = "Год",
            profit = yearNet,
            income = yearIncome,
            expenses = yearExpenses,
            isExpanded = expandedCard == ProfitPeriod.YEAR,
            onToggle = {
                expandedCard = if (expandedCard == ProfitPeriod.YEAR) null else ProfitPeriod.YEAR
            },
            onIncomeClick = {
                onIncomeDetailClick(
                    com.clientledger.app.ui.viewmodel.StatsPeriod.YEAR,
                    LocalDate.now(),
                    YearMonth.now(),
                    currentYear
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

enum class ProfitPeriod {
    TODAY, MONTH, YEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableProfitWidget(
    period: ProfitPeriod,
    caption: String,
    profit: Long,
    income: Long,
    expenses: Long,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profitColor = when {
        profit > 0 -> MaterialTheme.colorScheme.primary
        profit < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = modifier
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Line 1: Caption (Сегодня / Месяц / Год)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Line 2: Label (Прибыль)
            Text(
                text = "Прибыль",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Line 3: Value (amount) - dominant
            Text(
                text = MoneyUtils.formatCentsSigned(profit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = profitColor
            )
            
            // Expanded content (income/expense details)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    // Income detail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Доход:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MoneyUtils.formatCents(income),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable(onClick = onIncomeClick)
                        )
                    }
                    
                    // Expense detail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Расход:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MoneyUtils.formatCents(expenses),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseWidgets(
    repository: LedgerRepository
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val currentYear = today.year
    
    // Today expenses
    var todayExpenses by remember { mutableStateOf(0L) }
    
    // Month expenses
    var monthExpenses by remember { mutableStateOf(0L) }
    
    // Year expenses
    var yearExpenses by remember { mutableStateOf(0L) }
    
    // Refresh when expenses change - observe expenses flow
    val todayExpensesFlow = repository.getExpensesByDate(today.toDateKey())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    val monthExpensesFlow = repository.getExpensesByDateRange(
        DateUtils.getStartOfMonth(currentMonth).toDateKey(),
        DateUtils.getEndOfMonth(currentMonth).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val yearExpensesFlow = repository.getExpensesByDateRange(
        DateUtils.getStartOfYear(currentYear).toDateKey(),
        DateUtils.getEndOfYear(currentYear).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val scope = rememberCoroutineScope()
    
    // Load today expenses when date or expenses change
    LaunchedEffect(today, todayExpensesFlow.value) {
        scope.launch {
            todayExpenses = repository.getExpensesForDay(today)
        }
    }
    
    // Load month expenses when month or expenses change
    LaunchedEffect(currentMonth, monthExpensesFlow.value) {
        scope.launch {
            monthExpenses = repository.getExpensesForMonth(currentYear, currentMonth.monthValue)
        }
    }
    
    // Load year expenses when year or expenses change
    LaunchedEffect(currentYear, yearExpensesFlow.value) {
        scope.launch {
            yearExpenses = repository.getExpensesForYear(currentYear)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Today widget
        StatisticsWidget(
            title = "Расход сегодня",
            value = MoneyUtils.formatCents(todayExpenses),
            onClick = { },
            valueColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        
        // Month widget
        StatisticsWidget(
            title = "Расход месяц",
            value = MoneyUtils.formatCents(monthExpenses),
            onClick = { },
            valueColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        
        // Year widget
        StatisticsWidget(
            title = "Расход год",
            value = MoneyUtils.formatCents(yearExpenses),
            onClick = { },
            valueColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NetProfitWidgets(
    repository: LedgerRepository
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val currentYear = today.year
    
    // Today net profit
    var todayNet by remember { mutableStateOf(0L) }
    
    // Month net profit
    var monthNet by remember { mutableStateOf(0L) }
    
    // Year net profit
    var yearNet by remember { mutableStateOf(0L) }
    
    // Refresh when appointments or expenses change
    val todayAppointments = repository.getAppointmentsByDate(today.toDateKey())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val todayExpenses = repository.getExpensesByDate(today.toDateKey())
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    val monthAppointments = repository.getAppointmentsByDateRange(
        DateUtils.getStartOfMonth(currentMonth).toDateKey(),
        DateUtils.getEndOfMonth(currentMonth).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    val monthExpenses = repository.getExpensesByDateRange(
        DateUtils.getStartOfMonth(currentMonth).toDateKey(),
        DateUtils.getEndOfMonth(currentMonth).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val yearAppointments = repository.getAppointmentsByDateRange(
        DateUtils.getStartOfYear(currentYear).toDateKey(),
        DateUtils.getEndOfYear(currentYear).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    val yearExpenses = repository.getExpensesByDateRange(
        DateUtils.getStartOfYear(currentYear).toDateKey(),
        DateUtils.getEndOfYear(currentYear).toDateKey()
    ).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val scope = rememberCoroutineScope()
    
    // Load today net profit when date or data change
    LaunchedEffect(today, todayAppointments.value, todayExpenses.value) {
        scope.launch {
            todayNet = repository.getNetProfitForDay(today)
        }
    }
    
    // Load month net profit when month or data change
    LaunchedEffect(currentMonth, monthAppointments.value, monthExpenses.value) {
        scope.launch {
            monthNet = repository.getNetProfitForMonth(currentYear, currentMonth.monthValue)
        }
    }
    
    // Load year net profit when year or data change
    LaunchedEffect(currentYear, yearAppointments.value, yearExpenses.value) {
        scope.launch {
            yearNet = repository.getNetProfitForYear(currentYear)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Today widget
        StatisticsWidget(
            title = "Прибыль сегодня",
            value = MoneyUtils.formatCentsSigned(todayNet),
            onClick = { },
            valueColor = when {
                todayNet > 0 -> MaterialTheme.colorScheme.primary
                todayNet < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        
        // Month widget
        StatisticsWidget(
            title = "Прибыль месяц",
            value = MoneyUtils.formatCentsSigned(monthNet),
            onClick = { },
            valueColor = when {
                monthNet > 0 -> MaterialTheme.colorScheme.primary
                monthNet < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        
        // Year widget
        StatisticsWidget(
            title = "Прибыль год",
            value = MoneyUtils.formatCentsSigned(yearNet),
            onClick = { },
            valueColor = when {
                yearNet > 0 -> MaterialTheme.colorScheme.primary
                yearNet < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ThemeSelector(
    themePreferences: com.clientledger.app.data.preferences.ThemePreferences
) {
    val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = com.clientledger.app.ui.theme.ThemeMode.LIGHT)
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Тема",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Light theme option
            FilterChip(
                selected = themeMode == com.clientledger.app.ui.theme.ThemeMode.LIGHT,
                onClick = {
                    scope.launch {
                        themePreferences.setThemeMode(com.clientledger.app.ui.theme.ThemeMode.LIGHT)
                    }
                },
                label = { Text("Светлая") },
                modifier = Modifier.weight(1f)
            )
            
            // Dark theme option
            FilterChip(
                selected = themeMode == com.clientledger.app.ui.theme.ThemeMode.DARK,
                onClick = {
                    scope.launch {
                        themePreferences.setThemeMode(com.clientledger.app.ui.theme.ThemeMode.DARK)
                    }
                },
                label = { Text("Тёмная") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VersionInfo(
    repository: com.clientledger.app.data.repository.LedgerRepository? = null,
    viewModel: CalendarViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTestDataDialog by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) } // 0.0 to 1.0
    var isHolding by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Track progress when holding
    LaunchedEffect(isHolding) {
        if (isHolding) {
            holdProgress = 0f
            val holdDuration = 5000L // 5 seconds
            val updateInterval = 50L // Update every 50ms
            val steps = (holdDuration / updateInterval).toInt()
            
            repeat(steps) {
                delay(updateInterval)
                if (isHolding) {
                    holdProgress = (it + 1).toFloat() / steps
                } else {
                    return@LaunchedEffect
                }
            }
            
            if (isHolding && holdProgress >= 1f) {
                showTestDataDialog = true
                isHolding = false
                holdProgress = 0f
            }
        } else {
            holdProgress = 0f
        }
    }
    
    val versionName = remember {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo?.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    // Check if debug build (simple check - in production this would use BuildConfig.DEBUG)
    val isDebug = try {
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (e: Exception) {
        false
    }
    
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .then(
                    if (isDebug && repository != null) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isHolding = true
                                    val progressJob = scope.launch {
                                        val holdDuration = 5000L // 5 seconds
                                        val updateInterval = 50L // Update every 50ms
                                        val steps = (holdDuration / updateInterval).toInt()
                                        
                                        repeat(steps) {
                                            delay(updateInterval)
                                            if (isHolding) {
                                                holdProgress = (it + 1).toFloat() / steps
                                            }
                                        }
                                        
                                        if (isHolding && holdProgress >= 1f) {
                                            showTestDataDialog = true
                                        }
                                    }
                                    
                                    // Wait for release (this will block until finger is lifted)
                                    tryAwaitRelease()
                                    
                                    isHolding = false
                                    holdProgress = 0f
                                    progressJob.cancel()
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = "версия $versionName",
                style = MaterialTheme.typography.labelSmall,
                color = if (isHolding) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
            
            // Progress indicator during hold
            if (isHolding && isDebug && repository != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
    
    // Test data dialog (DEBUG ONLY)
    if (showTestDataDialog && isDebug && repository != null) {
        AlertDialog(
            onDismissRequest = { showTestDataDialog = false },
            title = { Text("Test Data (DEBUG)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Generate test data for statistics validation:")
                    Text("• 30 clients (Client 1-30)")
                    Text("• December 1-31, 2025 (full month)")
                    Text("• January 1-7, 2026")
                    Text("")
                    Text("Warning: This will add test data to the database.")
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val result = repository.generateTestData()
                                    // Refresh calendar to show new data
                                    viewModel?.refreshWorkingDays()
                                    snackbarHostState.showSnackbar(
                                        "Generated: ${result.clientsCount} clients, ${result.appointmentsCount} appointments. Switch to Dec 2025 or Jan 2026 to see them."
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                                showTestDataDialog = false
                            }
                        }
                    ) {
                        Text("Generate")
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    repository.clearAllTestData()
                                    snackbarHostState.showSnackbar("Test data cleared")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                                showTestDataDialog = false
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear All")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Snackbar for messages
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )
}

