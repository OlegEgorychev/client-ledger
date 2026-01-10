package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.ClientIncome
import com.clientledger.app.data.dao.DayIncome
import com.clientledger.app.data.dao.DayProfit
import com.clientledger.app.data.dao.MonthExpense
import com.clientledger.app.data.dao.MonthIncome
import com.clientledger.app.data.dao.SummaryStats
import com.clientledger.app.data.dao.TagExpense
import com.clientledger.app.data.dao.TagIncome
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class StatsPeriod {
    DAY, MONTH, YEAR
}

data class PeriodComparison(
    val current: Long,
    val previous: Long,
    val delta: Long,
    val percentChange: Double? // null if previous = 0
)

data class CancellationComparison(
    val current: Int,
    val previous: Int,
    val delta: Int,
    val percentChange: Double? // null if previous = 0
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedYear: Int = LocalDate.now().year,
    val income: Long = 0,
    val expenses: Long = 0,
    val profit: Long = 0,
    val workingDays: Int = 0,
    val mostProfitableDayByIncome: DayIncome? = null,
    val mostProfitableDayByProfit: DayProfit? = null,
    // Phase 1: New fields
    val totalVisits: Int = 0,
    val totalClients: Int = 0,
    val averageCheck: Long = 0,
    // Period comparisons
    val incomeComparison: PeriodComparison? = null,
    val visitsComparison: PeriodComparison? = null,
    val clientsComparison: PeriodComparison? = null,
    val averageCheckComparison: PeriodComparison? = null,
    // Cancellation stats
    val totalCancellations: Int = 0,
    val cancellationRate: Double = 0.0, // percentage (0-100)
    val cancellationsComparison: CancellationComparison? = null,
    // Chart data
    val incomeSeries: List<DayIncome> = emptyList(), // For line chart
    val incomeByMonth: List<MonthIncome> = emptyList(), // For pie chart (year mode)
    val incomeByClient: List<ClientIncome> = emptyList(), // For pie chart (top clients)
    val incomeByTag: List<TagIncome> = emptyList(), // For pie chart (income by service tags)
    val expensesByMonth: List<MonthExpense> = emptyList(), // For pie chart (expenses by month)
    val expensesByTag: List<TagExpense> = emptyList(), // For pie chart (expenses by tag)
    val isLoading: Boolean = false
)

data class IncomeDetailState(
    val period: StatsPeriod,
    val selectedDate: LocalDate,
    val selectedYearMonth: YearMonth,
    val selectedYear: Int,
    val totalIncome: Long,
    val incomeSeries: List<DayIncome>,
    val incomeByClient: List<ClientIncome>,
    val incomeComparison: PeriodComparison? = null,
    val bestDay: DayIncome? = null,
    val bestClient: ClientIncome? = null,
    // Cancellation stats
    val totalCancellations: Int = 0,
    val cancellationRate: Double = 0.0,
    val cancellationsComparison: CancellationComparison? = null,
    val isLoading: Boolean = false
)

class StatsViewModel(private val repository: LedgerRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun setPeriod(period: StatsPeriod) {
        _uiState.value = _uiState.value.copy(period = period)
        loadStats()
    }

    fun setDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadStats()
    }

    fun setYearMonth(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(selectedYearMonth = yearMonth)
        loadStats()
    }

    fun setYear(year: Int) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        loadStats()
    }

    private fun loadStats() {
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

            val summary = repository.getSummaryStats(startDate, endDate)
            val prevSummary = repository.getSummaryStats(prevStartDate, prevEndDate)
            
            val income = summary.totalIncome
            val expenses = repository.getExpensesForDateRange(startDate, endDate)
            val profit = income - expenses
            val workingDays = repository.getWorkingDaysCount(startDate, endDate)
            
            val mostProfitableByIncome = repository.getMostProfitableDayByIncome(startDate, endDate)
            val mostProfitableByProfit = repository.getMostProfitableDayByProfit(startDate, endDate)
            
            // Calculate average check
            val averageCheck = if (summary.totalVisits > 0) {
                income / summary.totalVisits
            } else {
                0L
            }
            
            val prevAverageCheck = if (prevSummary.totalVisits > 0) {
                prevSummary.totalIncome / prevSummary.totalVisits
            } else {
                0L
            }
            
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
            
            // Cancellation stats
            val totalCancellations = repository.getCancellationsCount(startDate, endDate)
            val totalAppointments = repository.getTotalAppointmentsCount(startDate, endDate)
            val cancellationRate = if (totalAppointments > 0) {
                (totalCancellations.toDouble() / totalAppointments) * 100
            } else {
                0.0
            }
            
            val prevTotalCancellations = repository.getCancellationsCount(prevStartDate, prevEndDate)
            val prevTotalAppointments = repository.getTotalAppointmentsCount(prevStartDate, prevEndDate)
            
            val cancellationsComparison = CancellationComparison(
                current = totalCancellations,
                previous = prevTotalCancellations,
                delta = totalCancellations - prevTotalCancellations,
                percentChange = if (prevTotalCancellations != 0) {
                    ((totalCancellations - prevTotalCancellations).toDouble() / prevTotalCancellations) * 100
                } else {
                    null
                }
            )

            // Load chart data
            val incomeSeries = repository.getIncomeSeries(startDate, endDate)
            val incomeByMonth = if (_uiState.value.period == StatsPeriod.YEAR) {
                repository.getIncomeByMonth(startDate, endDate)
            } else {
                emptyList()
            }
            val incomeByClient = repository.getIncomeByClient(startDate, endDate)
            val incomeByTag = repository.getIncomeByTag(startDate, endDate)
            val expensesByMonth = if (_uiState.value.period == StatsPeriod.YEAR) {
                repository.getExpensesByMonth(startDate, endDate)
            } else {
                emptyList()
            }
            val expensesByTag = repository.getExpensesByTag(startDate, endDate)

            _uiState.value = _uiState.value.copy(
                income = income,
                expenses = expenses,
                profit = profit,
                workingDays = workingDays,
                mostProfitableDayByIncome = mostProfitableByIncome,
                mostProfitableDayByProfit = mostProfitableByProfit,
                totalVisits = summary.totalVisits,
                totalClients = summary.totalClients,
                averageCheck = averageCheck,
                incomeComparison = calculateComparison(income, prevSummary.totalIncome),
                visitsComparison = calculateComparison(summary.totalVisits.toLong(), prevSummary.totalVisits.toLong()),
                clientsComparison = calculateComparison(summary.totalClients.toLong(), prevSummary.totalClients.toLong()),
                averageCheckComparison = calculateComparison(averageCheck, prevAverageCheck),
                totalCancellations = totalCancellations,
                cancellationRate = cancellationRate,
                cancellationsComparison = cancellationsComparison,
                // Chart data
                incomeSeries = incomeSeries,
                incomeByMonth = incomeByMonth,
                incomeByClient = incomeByClient,
                incomeByTag = incomeByTag,
                expensesByMonth = expensesByMonth,
                expensesByTag = expensesByTag,
                isLoading = false
            )
        }
    }
}


