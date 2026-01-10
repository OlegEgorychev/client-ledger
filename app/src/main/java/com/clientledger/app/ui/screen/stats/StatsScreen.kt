package com.clientledger.app.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.ui.viewmodel.StatsPeriod
import com.clientledger.app.ui.viewmodel.StatsUiState
import com.clientledger.app.ui.viewmodel.StatsViewModel
import com.clientledger.app.ui.components.PieChart
import com.clientledger.app.ui.components.PieSegment
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.YearMonth

enum class StatsSection {
    INCOME, EXPENSES, CLIENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(),
    onIncomeClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onClientsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onVisitsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onReportsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onSettingsClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf(StatsSection.INCOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Section tabs
            TabRow(selectedTabIndex = selectedSection.ordinal) {
                Tab(
                    selected = selectedSection == StatsSection.INCOME,
                    onClick = { selectedSection = StatsSection.INCOME },
                    text = { Text("Доход") }
                )
                Tab(
                    selected = selectedSection == StatsSection.EXPENSES,
                    onClick = { selectedSection = StatsSection.EXPENSES },
                    text = { Text("Расход") }
                )
                Tab(
                    selected = selectedSection == StatsSection.CLIENTS,
                    onClick = { selectedSection = StatsSection.CLIENTS },
                    text = { Text("Клиенты") }
                )
            }
            
            // Period selection and content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            // Выбор периода
            Text(
                text = "Период",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(StatsPeriod.DAY to "День", StatsPeriod.MONTH to "Месяц", StatsPeriod.YEAR to "Год")
                    .forEach { (period, label) ->
                        FilterChip(
                            selected = uiState.period == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                        )
                    }
            }

            // Выбор даты/месяца/года
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (uiState.period) {
                StatsPeriod.DAY -> {
                    Text("Выбранный день: ${DateUtils.formatDate(uiState.selectedDate)}")
                    val today = LocalDate.now()
                    val isNextDayFuture = uiState.selectedDate.plusDays(1).isAfter(today)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setDate(uiState.selectedDate.minusDays(1)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Предыдущий")
                        }
                        Button(
                            onClick = { viewModel.setDate(uiState.selectedDate.plusDays(1)) },
                            modifier = Modifier.weight(1f),
                            enabled = !isNextDayFuture
                        ) {
                            Text("Следующий")
                        }
                    }
                }
                StatsPeriod.MONTH -> {
                    Text("Выбранный месяц: ${DateUtils.formatMonth(uiState.selectedYearMonth)}")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setYearMonth(uiState.selectedYearMonth.minusMonths(1)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Предыдущий")
                        }
                        Button(
                            onClick = { viewModel.setYearMonth(uiState.selectedYearMonth.plusMonths(1)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Следующий")
                        }
                    }
                }
                StatsPeriod.YEAR -> {
                    Text("Выбранный год: ${uiState.selectedYear}")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setYear(uiState.selectedYear - 1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Предыдущий")
                        }
                        Button(
                            onClick = { viewModel.setYear(uiState.selectedYear + 1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Следующий")
                        }
                    }
                }
            }
            }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Section content based on selected tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedSection) {
                            StatsSection.INCOME -> IncomeSection(
                                uiState = uiState,
                                viewModel = viewModel,
                                onIncomeClick = onIncomeClick
                            )
                            StatsSection.EXPENSES -> ExpensesSection(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                            StatsSection.CLIENTS -> ClientsSection(
                                uiState = uiState,
                                viewModel = viewModel,
                                onClientsClick = onClientsClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// Income Section
@Composable
private fun IncomeSection(
    uiState: StatsUiState,
    viewModel: StatsViewModel,
    onIncomeClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Total Income card
        val totalIncome = uiState.income
        val totalIncomeFormatted = MoneyUtils.formatCents(totalIncome)
        val percentChangeText = uiState.incomeComparison?.percentChange?.let { percent ->
            if (percent >= 0) "+${String.format("%.1f", percent)}%" else "${String.format("%.1f", percent)}%"
        }
        
        ClickableStatsCard(
            title = "Общий доход",
            value = totalIncomeFormatted,
            subtitle = "Средний чек: ${MoneyUtils.formatCents(uiState.averageCheck)}${percentChangeText?.let { " ($it)" } ?: ""}",
            color = MaterialTheme.colorScheme.tertiary,
            onClick = {
                onIncomeClick(
                    uiState.period,
                    uiState.selectedDate,
                    uiState.selectedYearMonth,
                    uiState.selectedYear
                )
            },
            comparison = uiState.incomeComparison
        )
        
        // Pie chart: Income by service tags
        if (uiState.incomeByTag.isNotEmpty()) {
            Card(
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Доходы по услугам",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Pie chart visualization
                    val totalTagIncome = uiState.incomeByTag.sumOf { it.totalIncome }
                    val chartColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.error,
                        Color(0xFF9C27B0), // Purple
                        Color(0xFF00BCD4), // Cyan
                        Color(0xFFFF9800), // Orange
                        Color(0xFF4CAF50), // Green
                        Color(0xFFE91E63), // Pink
                        Color(0xFF795548), // Brown
                    )
                    
                    val pieSegments = uiState.incomeByTag.mapIndexed { index, tagIncome ->
                        PieSegment(
                            label = tagIncome.tagName,
                            value = tagIncome.totalIncome,
                            color = chartColors[index % chartColors.size]
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pie chart
                        PieChart(
                            segments = pieSegments,
                            modifier = Modifier.size(150.dp),
                            size = 150.dp,
                            strokeWidth = 0.dp,
                            gapAngle = 2f
                        )
                        
                        // Legend with percentages
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.incomeByTag.forEachIndexed { index, tagIncome ->
                                val percentage = if (totalTagIncome > 0) {
                                    (tagIncome.totalIncome.toDouble() / totalTagIncome) * 100
                                } else 0.0
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Color indicator
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = chartColors[index % chartColors.size],
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tagIncome.tagName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tagIncome.appointmentCount} услуг",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = MoneyUtils.formatCents(tagIncome.totalIncome),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format("%.1f", percentage)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Pie chart: Income by month (only for YEAR mode)
        if (uiState.period == StatsPeriod.YEAR && uiState.incomeByMonth.isNotEmpty()) {
            Card(
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
                    Text(
                        text = "Доходы по месяцам",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Monthly breakdown list with percentages
                    val totalMonthlyIncome = uiState.incomeByMonth.sumOf { it.totalIncome }
                    uiState.incomeByMonth.forEach { monthIncome ->
                        val percentage = if (totalMonthlyIncome > 0) {
                            (monthIncome.totalIncome.toDouble() / totalMonthlyIncome) * 100
                        } else 0.0
                        
                        // Parse monthKey (YYYY-MM) to YearMonth for formatting
                        val yearMonth = try {
                            val parts = monthIncome.monthKey.split("-")
                            if (parts.size == 2) {
                                YearMonth.of(parts[0].toInt(), parts[1].toInt())
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = yearMonth?.let { DateUtils.formatMonth(it) } ?: monthIncome.monthKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = MoneyUtils.formatCents(monthIncome.totalIncome),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "(${String.format("%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // TODO: Add pie chart visualization using Vico or DonutChart
                }
            }
        }
        
        // Best day/month by income
        if (uiState.period != StatsPeriod.DAY) {
            uiState.mostProfitableDayByIncome?.let { day ->
                val dayIncome = day.totalIncome
                val percentage = if (totalIncome > 0) {
                    (dayIncome.toDouble() / totalIncome) * 100
                } else 0.0
                
                StatsCard(
                    title = "Лучший день по доходу",
                    value = "${DateUtils.formatDate(DateUtils.toLocalDate(day.dateKey))}\n${MoneyUtils.formatCents(dayIncome)}",
                    subtitle = "${String.format("%.1f", percentage)}% от общего дохода",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        
        // Average income indicators
        if (uiState.workingDays > 0) {
            val avgIncomePerDay = totalIncome / uiState.workingDays
            StatsCard(
                title = "Средний доход в день",
                value = MoneyUtils.formatCents(avgIncomePerDay),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Total visits (relevant for income section)
        StatsCard(
            title = "Всего визитов",
            value = uiState.totalVisits.toString(),
            subtitle = if (uiState.workingDays > 0) {
                val avgVisitsPerDay = String.format("%.1f", uiState.totalVisits.toFloat() / uiState.workingDays)
                "Среднее в день: $avgVisitsPerDay"
            } else null,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// Expenses Section
@Composable
private fun ExpensesSection(
    uiState: StatsUiState,
    viewModel: StatsViewModel
) {
    val scrollState = rememberScrollState()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Total Expenses card
        val totalExpenses = uiState.expenses
        StatsCard(
            title = "Общий расход",
            value = MoneyUtils.formatCents(totalExpenses),
            color = MaterialTheme.colorScheme.error
        )
        
        // Pie chart: Expenses by month (only for YEAR mode)
        if (uiState.period == StatsPeriod.YEAR && uiState.expensesByMonth.isNotEmpty()) {
            Card(
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
                    Text(
                        text = "Расходы по месяцам",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Monthly breakdown list with percentages
                    val totalMonthlyExpenses = uiState.expensesByMonth.sumOf { it.totalExpenses }
                    uiState.expensesByMonth.forEach { monthExpense ->
                        val percentage = if (totalMonthlyExpenses > 0) {
                            (monthExpense.totalExpenses.toDouble() / totalMonthlyExpenses) * 100
                        } else 0.0
                        
                        // Parse monthKey (YYYY-MM) to YearMonth for formatting
                        val yearMonth = try {
                            val parts = monthExpense.monthKey.split("-")
                            if (parts.size == 2) {
                                YearMonth.of(parts[0].toInt(), parts[1].toInt())
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = yearMonth?.let { DateUtils.formatMonth(it) } ?: monthExpense.monthKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = MoneyUtils.formatCents(monthExpense.totalExpenses),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "(${String.format("%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // TODO: Add pie chart visualization using Vico or DonutChart
                }
            }
        }
        
        // Pie chart: Expenses by tag
        if (uiState.expensesByTag.isNotEmpty()) {
            Card(
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Расходы по категориям",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Pie chart visualization
                    val totalTagExpenses = uiState.expensesByTag.sumOf { it.totalAmount }
                    val chartColors = listOf(
                        MaterialTheme.colorScheme.error,
                        Color(0xFFFF9800), // Orange
                        Color(0xFF9C27B0), // Purple
                        Color(0xFF00BCD4), // Cyan
                        Color(0xFF4CAF50), // Green
                        Color(0xFFE91E63), // Pink
                        Color(0xFF795548), // Brown
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary,
                    )
                    
                    val pieSegments = uiState.expensesByTag.mapIndexed { index, tagExpense ->
                        PieSegment(
                            label = tagExpense.tag.displayName,
                            value = tagExpense.totalAmount,
                            color = chartColors[index % chartColors.size]
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pie chart
                        PieChart(
                            segments = pieSegments,
                            modifier = Modifier.size(150.dp),
                            size = 150.dp,
                            strokeWidth = 0.dp,
                            gapAngle = 2f
                        )
                        
                        // Legend with percentages
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.expensesByTag.forEachIndexed { index, tagExpense ->
                                val percentage = if (totalTagExpenses > 0) {
                                    (tagExpense.totalAmount.toDouble() / totalTagExpenses) * 100
                                } else 0.0
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Color indicator
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = chartColors[index % chartColors.size],
                                                shape = CircleShape
                                            )
                                    )
                                    
                                    Text(
                                        text = tagExpense.tag.displayName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = MoneyUtils.formatCents(tagExpense.totalAmount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "${String.format("%.1f", percentage)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Average expenses indicators
        if (uiState.workingDays > 0) {
            val avgExpensesPerDay = totalExpenses / uiState.workingDays
            StatsCard(
                title = "Средний расход в день",
                value = MoneyUtils.formatCents(avgExpensesPerDay),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
        
        // Most expensive day (if available for month/year periods)
        // TODO: Add most expensive day/month indicators
    }
}

// Clients Section
@Composable
private fun ClientsSection(
    uiState: StatsUiState,
    viewModel: StatsViewModel,
    onClientsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Total Clients card
        ClickableStatsCard(
            title = "Всего клиентов",
            value = uiState.totalClients.toString(),
            subtitle = "Средний чек: ${MoneyUtils.formatCents(uiState.averageCheck)}",
            color = MaterialTheme.colorScheme.tertiary,
            onClick = {
                onClientsClick(
                    uiState.period,
                    uiState.selectedDate,
                    uiState.selectedYearMonth,
                    uiState.selectedYear
                )
            },
            comparison = uiState.clientsComparison
        )
        
        // Top clients by income with percentages
        if (uiState.incomeByClient.isNotEmpty()) {
            Card(
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
                    Text(
                        text = "Клиенты по доходу",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Client list with visit count, total paid, percentage
                    val topClients = uiState.incomeByClient.take(10) // Top 10
                    val othersClients = uiState.incomeByClient.drop(10)
                    val totalClientIncome = uiState.incomeByClient.sumOf { it.totalIncome }
                    val top10Income = topClients.sumOf { it.totalIncome }
                    val othersIncome = othersClients.sumOf { it.totalIncome }
                    val top10Percentage = if (totalClientIncome > 0 && uiState.income > 0) {
                        (top10Income.toDouble() / uiState.income) * 100
                    } else 0.0
                    
                    // Top 10 clients table
                    topClients.forEachIndexed { index, clientIncome ->
                        val percentage = if (totalClientIncome > 0) {
                            (clientIncome.totalIncome.toDouble() / totalClientIncome) * 100
                        } else 0.0
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${clientIncome.clientName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Визитов: ${clientIncome.visitCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = MoneyUtils.formatCents(clientIncome.totalIncome),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "(${String.format("%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < topClients.size - 1 || othersClients.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    
                    // Others group (if more than 10 clients)
                    if (othersClients.isNotEmpty()) {
                        val othersPercentage = if (totalClientIncome > 0) {
                            (othersIncome.toDouble() / totalClientIncome) * 100
                        } else 0.0
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Остальные (${othersClients.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = MoneyUtils.formatCents(othersIncome),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "(${String.format("%.1f", othersPercentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Summary: Top 10 share
                    if (topClients.isNotEmpty() && uiState.income > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Топ-10 клиентов: ${String.format("%.1f", top10Percentage)}% от общего дохода",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // TODO: Add pie chart visualization using Vico or DonutChart
                }
            }
        }
        
        // Average check indicator
        if (uiState.totalVisits > 0) {
            StatsCard(
                title = "Средний чек",
                value = MoneyUtils.formatCents(uiState.averageCheck),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Cancellations info (relevant for clients section)
        if (uiState.totalCancellations > 0) {
            StatsCard(
                title = "Отказы",
                value = "${uiState.totalCancellations}",
                subtitle = "Процент отказов: ${String.format("%.1f%%", uiState.cancellationRate)}",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun StatsCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    subtitle: String? = null
) {
    Card(
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ClickableStatsCard(
    title: String,
    value: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    comparison: com.clientledger.app.ui.viewmodel.PeriodComparison? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Comparison delta
            comparison?.let { comp ->
                val deltaColor = if (comp.delta >= 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                }
                val deltaText = if (comp.percentChange != null) {
                    MoneyUtils.formatDeltaWithPercent(comp.delta, comp.percentChange)
                } else {
                    "${MoneyUtils.formatDelta(comp.delta)} (—)"
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = deltaColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


