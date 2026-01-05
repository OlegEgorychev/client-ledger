package com.clientledger.app.ui.screen.stats

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
    viewModel: StatsViewModel = viewModel()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Выбор периода
            Text(
                text = "Период",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            when (uiState.period) {
                StatsPeriod.DAY -> {
                    // TODO: DatePicker для дня
                    Text("Выбранный день: ${DateUtils.formatDate(uiState.selectedDate)}")
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

            Divider()

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Статистика
                StatsCard(
                    title = "Доход",
                    value = MoneyUtils.formatCents(uiState.income),
                    color = MaterialTheme.colorScheme.primary
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

@Composable
fun StatsCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
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
        }
    }
}


