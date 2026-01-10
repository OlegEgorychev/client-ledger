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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.ui.viewmodel.IncomeDetailState
import com.clientledger.app.ui.viewmodel.IncomeDetailViewModel
import com.clientledger.app.ui.viewmodel.StatsPeriod
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDetailScreen(
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int,
    repository: com.clientledger.app.data.repository.LedgerRepository,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)? = null
) {
    val viewModel: IncomeDetailViewModel = viewModel(
        factory = IncomeDetailViewModelFactory(repository, period, selectedDate, selectedYearMonth, selectedYear)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Доход • ${
                            when (period) {
                                StatsPeriod.DAY -> DateUtils.formatDate(selectedDate)
                                StatsPeriod.MONTH -> DateUtils.formatMonth(selectedYearMonth)
                                StatsPeriod.YEAR -> selectedYear.toString()
                            }
                        }"
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Total Income - Big number with comparison
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
                            .padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Общий доход",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = MoneyUtils.formatCents(uiState.totalIncome),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Comparison to previous period
                        uiState.incomeComparison?.let { comp ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val deltaColor = if (comp.delta >= 0) {
                                MaterialTheme.colorScheme.primary
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = deltaColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Top Insights
                if (uiState.bestDay != null || uiState.bestClient != null || uiState.incomeComparison != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Основные показатели",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            uiState.bestDay?.let { day ->
                                InsightCard(
                                    title = "Лучший день",
                                    value = "${DateUtils.formatDate(day.dateKey.toLocalDate())}\n${MoneyUtils.formatCents(day.totalIncome)}"
                                )
                            }
                            
                            uiState.bestClient?.let { client ->
                                val percentage = if (uiState.totalIncome > 0) {
                                    (client.totalIncome.toFloat() / uiState.totalIncome * 100)
                                } else {
                                    0f
                                }
                                InsightCard(
                                    title = "Лучший клиент",
                                    value = "${client.clientName}\n${MoneyUtils.formatCents(client.totalIncome)} (${MoneyUtils.formatPercent(percentage.toDouble())})"
                                )
                            }
                            
                            uiState.incomeComparison?.let { comp ->
                                if (comp.percentChange != null) {
                                    InsightCard(
                                        title = "Рост дохода",
                                        value = MoneyUtils.formatDeltaWithPercent(comp.delta, comp.percentChange)
                                    )
                                }
                            }
                        }
                    }
                }

                // Donut Chart: Client contribution
                if (uiState.incomeByClient.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Вклад клиентов",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            var selectedClientIndex by remember { mutableStateOf<Int?>(null) }
                            
                            val topClients = uiState.incomeByClient.take(8) // Top 8 for donut chart
                            val othersIncome = uiState.incomeByClient.drop(8).sumOf { it.totalIncome }
                            
                            IncomeDonutChart(
                                data = topClients,
                                totalIncome = uiState.totalIncome,
                                selectedIndex = selectedClientIndex,
                                onSegmentClick = { index ->
                                    selectedClientIndex = if (selectedClientIndex == index) null else index
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )

                            // Client list under donut chart
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            topClients.forEachIndexed { index, client ->
                                val isSelected = selectedClientIndex == index
                                ClientIncomeRow(
                                    clientName = client.clientName,
                                    amount = client.totalIncome,
                                    percentage = if (uiState.totalIncome > 0) {
                                        (client.totalIncome.toFloat() / uiState.totalIncome * 100)
                                    } else {
                                        0f
                                    },
                                    visitCount = client.visitCount,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedClientIndex = if (isSelected) null else index
                                    }
                                )
                            }
                            
                            if (othersIncome > 0) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ClientIncomeRow(
                                    clientName = "Остальные",
                                    amount = othersIncome,
                                    percentage = if (uiState.totalIncome > 0) {
                                        (othersIncome.toFloat() / uiState.totalIncome * 100)
                                    } else {
                                        0f
                                    },
                                    visitCount = 0
                                )
                            }
                        }
                    }
                } else {
                    // Empty state
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Нет данных",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "В выбранном периоде нет оплаченных записей",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Bottom padding
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun IncomeLineChart(
    data: List<com.clientledger.app.data.dao.DayIncome>,
    period: StatsPeriod,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет данных",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    // Prepare chart data - convert to rubles
    val entries = remember(data) {
        data.mapIndexed { index, dayIncome ->
            entryOf(index.toFloat(), dayIncome.totalIncome / 100f)
        }
    }
    
    val modelProducer = remember(entries) {
        ChartEntryModelProducer(entries)
    }
    
    // Format X-axis labels based on period
    val xAxisFormatter = remember(period, data) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index >= 0 && index < data.size) {
                val dateKey = data[index].dateKey
                when (period) {
                    StatsPeriod.DAY -> {
                        val date = dateKey.toLocalDate()
                        date.dayOfMonth.toString()
                    }
                    StatsPeriod.MONTH -> {
                        val date = dateKey.toLocalDate()
                        date.dayOfMonth.toString()
                    }
                    StatsPeriod.YEAR -> {
                        val date = dateKey.toLocalDate()
                        DateUtils.formatShortDate(date)
                    }
                }
            } else {
                ""
            }
        }
    }
    
    // Format Y-axis labels
    val yAxisFormatter = remember {
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            "${value.toInt()} ₽"
        }
    }
    
    ProvideChartStyle(
        chartStyle = currentChartStyle
    ) {
        Chart(
            chart = lineChart(
                spacing = 8.dp,
                lines = listOf(
                    lineSpec(
                        lineColor = MaterialTheme.colorScheme.primary
                    )
                )
            ),
            chartModelProducer = modelProducer,
            modifier = modifier,
            startAxis = rememberStartAxis(
                valueFormatter = yAxisFormatter,
                guideline = null
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = xAxisFormatter,
                guideline = null
            ),
            marker = null,
            isZoomEnabled = false
        )
    }
}

@Composable
fun IncomeDonutChart(
    data: List<com.clientledger.app.data.dao.ClientIncome>,
    totalIncome: Long,
    selectedIndex: Int?,
    onSegmentClick: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || totalIncome == 0L) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = MoneyUtils.formatCents(totalIncome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Нет данных",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }
    
    // Prepare chart data - convert to percentages
    val entries = remember(data, totalIncome) {
        data.mapIndexed { index, clientIncome ->
            entryOf(index.toFloat(), clientIncome.totalIncome.toFloat() / totalIncome * 100f)
        }
    }
    
    val model = remember(entries) {
        entryModelOf(entries)
    }
    
    // Generate colors for segments - use theme colors
    val colorScheme = MaterialTheme.colorScheme
    val colors = remember(data.size, colorScheme) {
        val colorList = mutableListOf<Color>()
        // Use theme colors for consistent styling
        val baseColors = listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.primaryContainer,
            colorScheme.secondaryContainer,
            colorScheme.tertiaryContainer,
            colorScheme.error,
            colorScheme.errorContainer
        )
        repeat(data.size) { index ->
            colorList.add(baseColors[index % baseColors.size])
        }
        colorList
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // For MVP: Simple visual representation
        // Real donut chart will be implemented with proper vico API
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = MoneyUtils.formatCents(totalIncome),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${data.size} клиентов",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Show top clients percentages
            if (data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                data.take(3).forEachIndexed { index, client ->
                    val percentage = (client.totalIncome.toFloat() / totalIncome * 100f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        colors.getOrNull(index) ?: MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = client.clientName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = MoneyUtils.formatPercent(percentage.toDouble()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    value: String
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ClientIncomeRow(
    clientName: String,
    amount: Long,
    percentage: Float,
    visitCount: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
            Column {
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                if (visitCount > 0) {
                    Text(
                        text = "$visitCount визит(ов)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.End
        ) {
            Text(
                text = MoneyUtils.formatCents(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = MoneyUtils.formatPercent(percentage.toDouble()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ViewModel Factory
class IncomeDetailViewModelFactory(
    private val repository: com.clientledger.app.data.repository.LedgerRepository,
    private val period: StatsPeriod,
    private val selectedDate: LocalDate,
    private val selectedYearMonth: YearMonth,
    private val selectedYear: Int
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeDetailViewModel::class.java)) {
            return IncomeDetailViewModel(
                repository,
                period,
                selectedDate,
                selectedYearMonth,
                selectedYear
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

