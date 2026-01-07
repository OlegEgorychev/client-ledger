package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.ClientIncome
import com.clientledger.app.data.dao.DayIncome
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.viewmodel.PeriodComparison
import com.clientledger.app.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class IncomeDetailViewModel(
    private val repository: LedgerRepository,
    period: StatsPeriod,
    selectedDate: LocalDate,
    selectedYearMonth: YearMonth,
    selectedYear: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        IncomeDetailState(
            period = period,
            selectedDate = selectedDate,
            selectedYearMonth = selectedYearMonth,
            selectedYear = selectedYear,
            totalIncome = 0,
            incomeSeries = emptyList(),
            incomeByClient = emptyList(),
            isLoading = true
        )
    )
    val uiState: StateFlow<IncomeDetailState> = _uiState.asStateFlow()

    init {
        loadIncomeDetails()
    }

    fun refresh() {
        loadIncomeDetails()
    }

    private fun loadIncomeDetails() {
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
                        com.clientledger.app.util.DateUtils.getStartOfMonth(prevMonth).toDateKey(),
                        com.clientledger.app.util.DateUtils.getEndOfMonth(prevMonth).toDateKey()
                    )
                }
                StatsPeriod.YEAR -> {
                    val prevYear = _uiState.value.selectedYear - 1
                    Pair(
                        com.clientledger.app.util.DateUtils.getStartOfYear(prevYear).toDateKey(),
                        com.clientledger.app.util.DateUtils.getEndOfYear(prevYear).toDateKey()
                    )
                }
            }
            
            val totalIncome = repository.getIncomeForDateRange(startDate, endDate)
            val prevIncome = repository.getIncomeForDateRange(prevStartDate, prevEndDate)
            val incomeSeries = repository.getIncomeSeries(startDate, endDate)
            val incomeByClient = repository.getIncomeByClient(startDate, endDate)
            
            // Calculate comparison
            val incomeComparison = PeriodComparison(
                current = totalIncome,
                previous = prevIncome,
                delta = totalIncome - prevIncome,
                percentChange = if (prevIncome != 0L) {
                    ((totalIncome - prevIncome).toDouble() / prevIncome) * 100
                } else {
                    null
                }
            )
            
            // Find best day and best client
            val bestDay = incomeSeries.maxByOrNull { it.totalIncome }
            val bestClient = incomeByClient.maxByOrNull { it.totalIncome }
            
            // Cancellation stats
            val totalCancellations = repository.getCancellationsCount(startDate, endDate)
            val totalAppointments = repository.getTotalAppointmentsCount(startDate, endDate)
            val cancellationRate = if (totalAppointments > 0) {
                (totalCancellations.toDouble() / totalAppointments) * 100
            } else {
                0.0
            }
            
            val prevTotalCancellations = repository.getCancellationsCount(prevStartDate, prevEndDate)
            
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

            _uiState.value = _uiState.value.copy(
                totalIncome = totalIncome,
                incomeSeries = incomeSeries,
                incomeByClient = incomeByClient,
                incomeComparison = incomeComparison,
                bestDay = bestDay,
                bestClient = bestClient,
                totalCancellations = totalCancellations,
                cancellationRate = cancellationRate,
                cancellationsComparison = cancellationsComparison,
                isLoading = false
            )
        }
    }
}

