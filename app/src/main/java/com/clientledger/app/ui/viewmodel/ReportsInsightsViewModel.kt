package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.*
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ReportsInsightsState(
    val period: StatsPeriod,
    val selectedDate: LocalDate,
    val selectedYearMonth: YearMonth,
    val selectedYear: Int,
    val bestIncomeDay: DayIncome? = null,
    val bestClient: ClientIncome? = null,
    val biggestPayment: BiggestPayment? = null,
    val mostFrequentClient: MostFrequentClient? = null,
    val busiestDay: BusiestDay? = null,
    val highestCancellationDay: DayCancellations? = null,
    val incomeGrowth: PeriodComparison? = null,
    val isLoading: Boolean = false
)

class ReportsInsightsViewModel(
    private val repository: LedgerRepository,
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ReportsInsightsState(
            period = period,
            selectedDate = selectedDate,
            selectedYearMonth = selectedYearMonth,
            selectedYear = selectedYear,
            isLoading = true
        )
    )
    val uiState: StateFlow<ReportsInsightsState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    fun refresh() {
        loadInsights()
    }

    private fun loadInsights() {
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
            
            // Load all insights
            val bestIncomeDay = repository.getMostProfitableDayByIncome(startDate, endDate)
            val incomeByClient = repository.getIncomeByClient(startDate, endDate)
            val bestClient = incomeByClient.maxByOrNull { it.totalIncome }
            val biggestPayment = repository.getBiggestPayment(startDate, endDate)
            val mostFrequentClient = repository.getMostFrequentClient(startDate, endDate)
            val busiestDay = repository.getBusiestDay(startDate, endDate)
            val cancellationsSeries = repository.getCancellationsSeries(startDate, endDate)
            val highestCancellationDay = cancellationsSeries.maxByOrNull { it.cancellationsCount }
            
            // Calculate income growth
            val currentIncome = repository.getIncomeForDateRange(startDate, endDate)
            val prevIncome = repository.getIncomeForDateRange(prevStartDate, prevEndDate)
            val incomeGrowth = PeriodComparison(
                current = currentIncome,
                previous = prevIncome,
                delta = currentIncome - prevIncome,
                percentChange = if (prevIncome != 0L) {
                    ((currentIncome - prevIncome).toDouble() / prevIncome) * 100
                } else {
                    null
                }
            )

            _uiState.value = _uiState.value.copy(
                bestIncomeDay = bestIncomeDay,
                bestClient = bestClient,
                biggestPayment = biggestPayment,
                mostFrequentClient = mostFrequentClient,
                busiestDay = busiestDay,
                highestCancellationDay = highestCancellationDay,
                incomeGrowth = incomeGrowth,
                isLoading = false
            )
        }
    }
}

