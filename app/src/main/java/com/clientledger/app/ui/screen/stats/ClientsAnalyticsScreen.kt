package com.clientledger.app.ui.screen.stats

import androidx.compose.foundation.background
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
import com.clientledger.app.ui.components.DonutChart
import com.clientledger.app.ui.components.DonutSegment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.dao.TopClient
import com.clientledger.app.ui.navigation.ClientsAnalyticsViewModelFactory
import com.clientledger.app.ui.viewmodel.ClientsAnalyticsViewModel
import com.clientledger.app.ui.viewmodel.StatsPeriod
import com.clientledger.app.util.*
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsAnalyticsScreen(
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int,
    repository: com.clientledger.app.data.repository.LedgerRepository,
    onBack: () -> Unit,
    onClientClick: ((Long) -> Unit)? = null
) {
    val viewModel: ClientsAnalyticsViewModel = viewModel(
        factory = ClientsAnalyticsViewModelFactory(repository, period, selectedDate, selectedYearMonth, selectedYear)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Клиенты • ${
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
                                title = "Всего клиентов",
                                value = "${uiState.uniqueClients}",
                                comparison = uiState.uniqueClientsComparison,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Средний доход",
                                value = MoneyUtils.formatCents(uiState.avgIncomePerClient),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // New vs Returning clients donut chart
                if (uiState.uniqueClients > 0) {
                    var selectedSegmentIndex by remember { mutableStateOf(-1) }
                    val totalClients = uiState.uniqueClients.toLong()
                    val newClients = uiState.newClients.toLong()
                    val returningClients = uiState.returningClients.toLong()
                    
                    val segments = listOf(
                        DonutSegment(
                            label = "Новые",
                            value = newClients,
                            color = MaterialTheme.colorScheme.tertiary
                        ),
                        DonutSegment(
                            label = "Возвращающиеся",
                            value = returningClients,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Новые vs Возвращающиеся",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            DonutChart(
                                segments = segments,
                                centerText = totalClients.toString(),
                                selectedIndex = selectedSegmentIndex,
                                onSegmentClick = { index ->
                                    selectedSegmentIndex = if (selectedSegmentIndex == index) -1 else index
                                },
                                modifier = Modifier.size(200.dp)
                            )
                            
                            // List with percentages
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                segments.forEachIndexed { index, segment ->
                                    val percentage = if (totalClients > 0) {
                                        (segment.value.toFloat() / totalClients * 100)
                                    } else {
                                        0f
                                    }
                                    
                                    val isSelected = selectedSegmentIndex == index
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedSegmentIndex = if (isSelected) -1 else index
                                            }
                                            .padding(vertical = 8.dp, horizontal = 12.dp)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(
                                                        color = segment.color.copy(alpha = 0.1f),
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(
                                                        color = segment.color,
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                            )
                                            Text(
                                                text = segment.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${segment.value}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${String.format("%.1f", percentage)}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Best client insight
                uiState.bestClient?.let { client ->
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
                                text = "Лучший клиент",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = client.clientName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = MoneyUtils.formatCents(client.totalIncome),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${String.format("%.1f", client.percentage)}%",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                
                // Top clients list
                if (uiState.topClients.isNotEmpty()) {
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
                                text = "Топ клиентов по доходу",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            uiState.topClients.forEach { client ->
                                ClientRow(
                                    client = client,
                                    onClick = onClientClick?.let { { it(client.clientId) } }
                                )
                                if (client != uiState.topClients.last()) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    comparison: com.clientledger.app.ui.viewmodel.PeriodComparison? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            comparison?.let { comp ->
                val deltaText = if (comp.percentChange != null) {
                    "${if (comp.delta >= 0) "+" else ""}${comp.delta} (${String.format("%+.1f%%", comp.percentChange)})"
                } else {
                    "${if (comp.delta >= 0) "+" else ""}${comp.delta}"
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (comp.delta >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
fun ClientRow(
    client: TopClient,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.padding(vertical = 8.dp)
                } else {
                    Modifier.padding(vertical = 8.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = client.clientName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${client.visitCount} визит${if (client.visitCount % 10 == 1 && client.visitCount % 100 != 11) "" else if (client.visitCount % 10 in 2..4 && client.visitCount % 100 !in 12..14) "а" else "ов"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = MoneyUtils.formatCents(client.totalIncome),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format("%.1f", client.percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

