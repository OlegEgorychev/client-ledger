package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.DayVisits
import com.clientledger.app.data.dao.VisitsSummary
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class VisitsAnalyticsState(
    val period: StatsPeriod,
    val selectedDate: LocalDate,
    val selectedYearMonth: YearMonth,
    val selectedYear: Int,
    val totalVisits: Int = 0,
    val completedVisits: Int = 0,
    val canceledVisits: Int = 0,
    val cancellationRate: Double = 0.0,
    val avgVisitsPerDay: Double = 0.0,
    val visitsSeries: List<DayVisits> = emptyList(),
    val cancellationsSeries: List<com.clientledger.app.data.dao.DayCancellations> = emptyList(),
    val busiestDay: com.clientledger.app.data.dao.BusiestDay? = null,
    // Comparisons with previous period
    val visitsComparison: PeriodComparison? = null,
    val cancellationsComparison: CancellationComparison? = null,
    val isLoading: Boolean = false
)

class VisitsAnalyticsViewModel(
    private val repository: LedgerRepository,
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        VisitsAnalyticsState(
            period = period,
            selectedDate = selectedDate,
            selectedYearMonth = selectedYearMonth,
            selectedYear = selectedYear,
            isLoading = true
        )
    )
    val uiState: StateFlow<VisitsAnalyticsState> = _uiState.asStateFlow()

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
            
            val visitsSummary = repository.getVisitsSummary(startDate, endDate)
            val prevVisitsSummary = repository.getVisitsSummary(prevStartDate, prevEndDate)
            
            val visitsSeries = repository.getVisitsSeries(startDate, endDate)
            val cancellationsSeries = repository.getCancellationsSeries(startDate, endDate)
            val busiestDay = repository.getBusiestDay(startDate, endDate)
            
            // Calculate cancellation rate
            val cancellationRate = if (visitsSummary.totalVisits > 0) {
                (visitsSummary.canceledVisits.toDouble() / visitsSummary.totalVisits) * 100
            } else {
                0.0
            }
            
            // Calculate avg visits per day
            val workingDays = repository.getWorkingDaysCount(startDate, endDate)
            val avgVisitsPerDay = if (workingDays > 0) {
                visitsSummary.completedVisits.toDouble() / workingDays
            } else {
                0.0
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
            
            val cancellationsComparison = CancellationComparison(
                current = visitsSummary.canceledVisits,
                previous = prevVisitsSummary.canceledVisits,
                delta = visitsSummary.canceledVisits - prevVisitsSummary.canceledVisits,
                percentChange = if (prevVisitsSummary.canceledVisits != 0) {
                    ((visitsSummary.canceledVisits - prevVisitsSummary.canceledVisits).toDouble() / prevVisitsSummary.canceledVisits) * 100
                } else {
                    null
                }
            )

            _uiState.value = _uiState.value.copy(
                totalVisits = visitsSummary.totalVisits,
                completedVisits = visitsSummary.completedVisits,
                canceledVisits = visitsSummary.canceledVisits,
                cancellationRate = cancellationRate,
                avgVisitsPerDay = avgVisitsPerDay,
                visitsSeries = visitsSeries,
                cancellationsSeries = cancellationsSeries,
                busiestDay = busiestDay,
                visitsComparison = calculateComparison(
                    visitsSummary.totalVisits.toLong(),
                    prevVisitsSummary.totalVisits.toLong()
                ),
                cancellationsComparison = cancellationsComparison,
                isLoading = false
            )
        }
    }
}

