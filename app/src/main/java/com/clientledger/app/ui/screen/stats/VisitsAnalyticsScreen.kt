package com.clientledger.app.ui.screen.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.ui.navigation.VisitsAnalyticsViewModelFactory
import com.clientledger.app.ui.viewmodel.StatsPeriod
import com.clientledger.app.ui.viewmodel.VisitsAnalyticsViewModel
import com.clientledger.app.util.*
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsAnalyticsScreen(
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int,
    repository: com.clientledger.app.data.repository.LedgerRepository,
    onBack: () -> Unit
) {
    val viewModel: VisitsAnalyticsViewModel = viewModel(
        factory = VisitsAnalyticsViewModelFactory(repository, period, selectedDate, selectedYearMonth, selectedYear)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Визиты • ${
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
                // Summary metrics
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
                            text = "Обзор",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                title = "Всего визитов",
                                value = "${uiState.totalVisits}",
                                comparison = uiState.visitsComparison,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Завершено",
                                value = "${uiState.completedVisits}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                title = "Отказы",
                                value = "${uiState.canceledVisits}",
                                comparison = uiState.cancellationsComparison?.let { comp ->
                                    com.clientledger.app.ui.viewmodel.PeriodComparison(
                                        current = comp.current.toLong(),
                                        previous = comp.previous.toLong(),
                                        delta = comp.delta.toLong(),
                                        percentChange = comp.percentChange
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Процент отказов",
                                value = "${String.format("%.1f", uiState.cancellationRate)}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        if (uiState.period != StatsPeriod.DAY) {
                            MetricCard(
                                title = "Среднее в день",
                                value = String.format("%.1f", uiState.avgVisitsPerDay),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Busiest day insight
                uiState.busiestDay?.let { day ->
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
                                text = "Самый загруженный день",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = DateUtils.formatDate(day.dateKey.toLocalDate()),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${day.visitsCount} визит${if (day.visitsCount % 10 == 1 && day.visitsCount % 100 != 11) "" else if (day.visitsCount % 10 in 2..4 && day.visitsCount % 100 !in 12..14) "а" else "ов"}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                // Visits series chart placeholder
                if (uiState.visitsSeries.isNotEmpty()) {
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
                                text = "Визиты по времени",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            // Simple list view for now (chart can be added later)
                            uiState.visitsSeries.take(10).forEach { dayVisit ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = DateUtils.formatShortDate(dayVisit.dateKey.toLocalDate()),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${dayVisit.visitsCount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

