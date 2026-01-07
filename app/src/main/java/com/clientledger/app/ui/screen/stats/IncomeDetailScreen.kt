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
    onBack: () -> Unit
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
                // Total Income - Big number
                Card(
                    modifier = Modifier.fillMaxWidth()
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Line Chart: Income over time
                if (uiState.incomeSeries.isNotEmpty()) {
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
                                text = "Доход по времени",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            // Simple line chart placeholder - will be replaced with vico
                            IncomeLineChart(
                                data = uiState.incomeSeries,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
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
                            
                            // Donut chart placeholder - will be replaced with vico
                            IncomeDonutChart(
                                data = uiState.incomeByClient,
                                totalIncome = uiState.totalIncome,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )

                            // Client list under donut chart
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val topClients = uiState.incomeByClient.take(10)
                            val othersIncome = uiState.incomeByClient.drop(10).sumOf { it.totalIncome }
                            
                            topClients.forEach { client ->
                                ClientIncomeRow(
                                    clientName = client.clientName,
                                    amount = client.totalIncome,
                                    percentage = if (uiState.totalIncome > 0) {
                                        (client.totalIncome.toFloat() / uiState.totalIncome * 100)
                                    } else {
                                        0f
                                    },
                                    visitCount = client.visitCount
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
    modifier: Modifier = Modifier
) {
    // Placeholder - will implement with vico library
    Box(
        modifier = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "Line Chart\n(${data.size} data points)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun IncomeDonutChart(
    data: List<com.clientledger.app.data.dao.ClientIncome>,
    totalIncome: Long,
    modifier: Modifier = Modifier
) {
    // Placeholder - will implement with vico library
    Box(
        modifier = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "Donut Chart\n(${data.size} clients)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ClientIncomeRow(
    clientName: String,
    amount: Long,
    percentage: Float,
    visitCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clientName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (visitCount > 0) {
                Text(
                    text = "$visitCount визит(ов)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.End
        ) {
            Text(
                text = MoneyUtils.formatCents(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("%.1f%%", percentage),
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

