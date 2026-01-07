package com.clientledger.app.ui.screen.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.ui.viewmodel.StatsPeriod
import com.clientledger.app.ui.viewmodel.StatsViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(),
    onIncomeClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onClientsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onVisitsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> },
    onReportsClick: (StatsPeriod, LocalDate, YearMonth, Int) -> Unit = { _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Статистика") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            modifier = Modifier.weight(1f)
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
                // Статистика - Phase 1: Clickable cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Total Income - clickable
                    ClickableStatsCard(
                        title = "Доход",
                        value = MoneyUtils.formatCents(uiState.income),
                        subtitle = "Средний чек: ${MoneyUtils.formatCents(uiState.averageCheck)}",
                        color = MaterialTheme.colorScheme.primary,
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

                    // Visits count - clickable
                    ClickableStatsCard(
                        title = "Визиты",
                        value = uiState.totalVisits.toString(),
                        subtitle = if (uiState.workingDays > 0) "Среднее в день: ${String.format("%.1f", uiState.totalVisits.toFloat() / uiState.workingDays)}" else "",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            onVisitsClick(
                                uiState.period,
                                uiState.selectedDate,
                                uiState.selectedYearMonth,
                                uiState.selectedYear
                            )
                        },
                        comparison = uiState.visitsComparison
                    )

                    // Clients count - clickable
                    ClickableStatsCard(
                        title = "Клиенты",
                        value = uiState.totalClients.toString(),
                        subtitle = "",
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
                    
                    // Cancellations
                    StatsCard(
                        title = "Отказы",
                        value = "${uiState.totalCancellations}",
                        subtitle = "Процент отказов: ${String.format("%.1f%%", uiState.cancellationRate)}",
                        color = MaterialTheme.colorScheme.error
                    )

                    StatsCard(
                        title = "Расход",
                        value = MoneyUtils.formatCents(uiState.expenses),
                        color = MaterialTheme.colorScheme.error
                    )

                    StatsCard(
                        title = "Прибыль",
                        value = MoneyUtils.formatCents(uiState.profit),
                        color = if (uiState.profit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    StatsCard(
                        title = "Рабочие дни",
                        value = uiState.workingDays.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    // Reports/Insights - clickable button
                    OutlinedButton(
                        onClick = {
                            onReportsClick(
                                uiState.period,
                                uiState.selectedDate,
                                uiState.selectedYearMonth,
                                uiState.selectedYear
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отчёты и инсайты")
                    }

                if (uiState.period != StatsPeriod.DAY) {
                    uiState.mostProfitableDayByIncome?.let { day ->
                        StatsCard(
                            title = "Самый прибыльный день (по доходу)",
                            value = "${DateUtils.formatDate(DateUtils.toLocalDate(day.dateKey))}\n${MoneyUtils.formatCents(day.totalIncome)}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    uiState.mostProfitableDayByProfit?.let { day ->
                        StatsCard(
                            title = "Самый прибыльный день (по прибыли)",
                            value = "${DateUtils.formatDate(DateUtils.toLocalDate(day.dateKey))}\n${MoneyUtils.formatCents(day.profit)}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                }
            }

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
        modifier = Modifier.fillMaxWidth()
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
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
                    style = MaterialTheme.typography.bodySmall,
                    color = deltaColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


