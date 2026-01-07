package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.ClientsSummary
import com.clientledger.app.data.dao.TopClient
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ClientsAnalyticsState(
    val period: StatsPeriod,
    val selectedDate: LocalDate,
    val selectedYearMonth: YearMonth,
    val selectedYear: Int,
    val uniqueClients: Int = 0,
    val newClients: Int = 0,
    val returningClients: Int = 0,
    val avgIncomePerClient: Long = 0,
    val topClients: List<TopClient> = emptyList(),
    val bestClient: TopClient? = null,
    // Comparisons with previous period
    val uniqueClientsComparison: PeriodComparison? = null,
    val newClientsComparison: PeriodComparison? = null,
    val returningClientsComparison: PeriodComparison? = null,
    val isLoading: Boolean = false
)

class ClientsAnalyticsViewModel(
    private val repository: LedgerRepository,
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ClientsAnalyticsState(
            period = period,
            selectedDate = selectedDate,
            selectedYearMonth = selectedYearMonth,
            selectedYear = selectedYear,
            isLoading = true
        )
    )
    val uiState: StateFlow<ClientsAnalyticsState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun refresh() {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val (startDate, endDate) = when (_uiState.value.period) {
                StatsPeriod.DAY -> {
                    val date = _uiState.value.selectedDate
                    Pair(date.toDateKey(), date.toDateKey())
                }
                StatsPeriod.MONTH -> {
                    val month = _uiState.value.selectedYearMonth
                    Pair(
                        DateUtils.getStartOfMonth(month).toDateKey(),
                        DateUtils.getEndOfMonth(month).toDateKey()
                    )
                }
                StatsPeriod.YEAR -> {
                    val year = _uiState.value.selectedYear
                    Pair(
                        DateUtils.getStartOfYear(year).toDateKey(),
                        DateUtils.getEndOfYear(year).toDateKey()
                    )
                }
            }

            // Calculate previous period dates
            val (prevStartDate, prevEndDate) = when (_uiState.value.period) {
                StatsPeriod.DAY -> {
                    val prevDate = _uiState.value.selectedDate.minusDays(1)
                    Pair(prevDate.toDateKey(), prevDate.toDateKey())
                }
                StatsPeriod.MONTH -> {
                    val prevMonth = _uiState.value.selectedYearMonth.minusMonths(1)
                    Pair(
                        DateUtils.getStartOfMonth(prevMonth).toDateKey(),
                        DateUtils.getEndOfMonth(prevMonth).toDateKey()
                    )
                }
                StatsPeriod.YEAR -> {
                    val prevYear = _uiState.value.selectedYear - 1
                    Pair(
                        DateUtils.getStartOfYear(prevYear).toDateKey(),
                        DateUtils.getEndOfYear(prevYear).toDateKey()
                    )
                }
            }
            
            val clientsSummary = repository.getClientsSummary(startDate, endDate)
            val prevClientsSummary = repository.getClientsSummary(prevStartDate, prevEndDate)
            
            val topClients = repository.getTopClientsByIncome(startDate, endDate, 10)
            val totalIncome = repository.getIncomeForDateRange(startDate, endDate)
            
            // Calculate percentages for top clients
            val topClientsWithPercentage = if (totalIncome > 0) {
                topClients.map { client ->
                    client.copy(percentage = (client.totalIncome.toFloat() / totalIncome * 100))
                }
            } else {
                topClients
            }
            
            val avgIncomePerClient = if (clientsSummary.uniqueClients > 0) {
                totalIncome / clientsSummary.uniqueClients
            } else {
                0L
            }
            
            val bestClient = topClientsWithPercentage.maxByOrNull { it.totalIncome }
            
            // Calculate comparisons
            fun calculateComparison(current: Long, previous: Long): PeriodComparison {
                val delta = current - previous
                val percentChange = if (previous != 0L) {
                    ((current - previous).toDouble() / previous) * 100
                } else {
                    null
                }
                return PeriodComparison(current, previous, delta, percentChange)
            }

            _uiState.value = _uiState.value.copy(
                uniqueClients = clientsSummary.uniqueClients,
                newClients = clientsSummary.newClients,
                returningClients = clientsSummary.returningClients,
                avgIncomePerClient = avgIncomePerClient,
                topClients = topClientsWithPercentage,
                bestClient = bestClient,
                uniqueClientsComparison = calculateComparison(
                    clientsSummary.uniqueClients.toLong(),
                    prevClientsSummary.uniqueClients.toLong()
                ),
                newClientsComparison = calculateComparison(
                    clientsSummary.newClients.toLong(),
                    prevClientsSummary.newClients.toLong()
                ),
                returningClientsComparison = calculateComparison(
                    clientsSummary.returningClients.toLong(),
                    prevClientsSummary.returningClients.toLong()
                ),
                isLoading = false
            )
        }
    }
}

