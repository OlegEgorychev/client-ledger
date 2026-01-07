package com.clientledger.app.ui.screen.stats

import androidx.compose.foundation.clickable
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
import com.clientledger.app.ui.navigation.ReportsInsightsViewModelFactory
import com.clientledger.app.ui.viewmodel.ReportsInsightsViewModel
import com.clientledger.app.ui.viewmodel.StatsPeriod
import com.clientledger.app.util.*
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsInsightsScreen(
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int,
    repository: com.clientledger.app.data.repository.LedgerRepository,
    onBack: () -> Unit,
    onDayClick: ((LocalDate) -> Unit)? = null,
    onClientClick: ((Long) -> Unit)? = null
) {
    val viewModel: ReportsInsightsViewModel = viewModel(
        factory = ReportsInsightsViewModelFactory(repository, period, selectedDate, selectedYearMonth, selectedYear)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Отчёты • ${
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ключевые показатели",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Income growth
                uiState.incomeGrowth?.let { growth ->
                    InsightCard(
                        title = "Рост дохода",
                        value = MoneyUtils.formatDeltaWithPercent(growth.delta, growth.percentChange ?: 0.0),
                        subtitle = "по сравнению с предыдущим периодом",
                        onClick = null
                    )
                }
                
                // Best income day
                uiState.bestIncomeDay?.let { day ->
                    InsightCard(
                        title = "Лучший день по доходу",
                        value = DateUtils.formatDate(day.dateKey.toLocalDate()),
                        subtitle = MoneyUtils.formatCents(day.totalIncome),
                        onClick = onDayClick?.let { { it(day.dateKey.toLocalDate()) } }
                    )
                }
                
                // Best client
                uiState.bestClient?.let { client ->
                    InsightCard(
                        title = "Лучший клиент",
                        value = client.clientName,
                        subtitle = MoneyUtils.formatCents(client.totalIncome),
                        onClick = onClientClick?.let { { it(client.clientId) } }
                    )
                }
                
                // Biggest payment
                uiState.biggestPayment?.let { payment ->
                    InsightCard(
                        title = "Наибольший платёж",
                        value = MoneyUtils.formatCents(payment.amount),
                        subtitle = "${payment.clientName} • ${DateUtils.formatShortDate(payment.dateKey.toLocalDate())}",
                        onClick = onClientClick?.let { { it(payment.clientId) } }
                    )
                }
                
                // Most frequent client
                uiState.mostFrequentClient?.let { client ->
                    InsightCard(
                        title = "Самый частый клиент",
                        value = client.clientName,
                        subtitle = "${client.visitCount} визит${if (client.visitCount % 10 == 1 && client.visitCount % 100 != 11) "" else if (client.visitCount % 10 in 2..4 && client.visitCount % 100 !in 12..14) "а" else "ов"}",
                        onClick = onClientClick?.let { { it(client.clientId) } }
                    )
                }
                
                // Busiest day
                uiState.busiestDay?.let { day ->
                    InsightCard(
                        title = "Самый загруженный день",
                        value = DateUtils.formatDate(day.dateKey.toLocalDate()),
                        subtitle = "${day.visitsCount} визит${if (day.visitsCount % 10 == 1 && day.visitsCount % 100 != 11) "" else if (day.visitsCount % 10 in 2..4 && day.visitsCount % 100 !in 12..14) "а" else "ов"}",
                        onClick = onDayClick?.let { { it(day.dateKey.toLocalDate()) } }
                    )
                }
                
                // Highest cancellation day
                uiState.highestCancellationDay?.let { day ->
                    InsightCard(
                        title = "День с наибольшим числом отказов",
                        value = DateUtils.formatDate(day.dateKey.toLocalDate()),
                        subtitle = "${day.cancellationsCount} отказ${if (day.cancellationsCount % 10 == 1 && day.cancellationsCount % 100 != 11) "" else if (day.cancellationsCount % 10 in 2..4 && day.cancellationsCount % 100 !in 12..14) "а" else "ов"}",
                        onClick = onDayClick?.let { { it(day.dateKey.toLocalDate()) } }
                    )
                }
            }
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

