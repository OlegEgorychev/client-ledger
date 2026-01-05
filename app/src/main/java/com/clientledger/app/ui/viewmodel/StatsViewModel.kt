package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.DayIncome
import com.clientledger.app.data.dao.DayProfit
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

            val income = repository.getIncomeForDateRange(startDate, endDate)
            val expenses = repository.getExpensesForDateRange(startDate, endDate)
            val profit = income - expenses
            val workingDays = repository.getWorkingDaysCount(startDate, endDate)
            
            val mostProfitableByIncome = repository.getMostProfitableDayByIncome(startDate, endDate)
            val mostProfitableByProfit = repository.getMostProfitableDayByProfit(startDate, endDate)

            _uiState.value = _uiState.value.copy(
                income = income,
                expenses = expenses,
                profit = profit,
                workingDays = workingDays,
                mostProfitableDayByIncome = mostProfitableByIncome,
                mostProfitableDayByProfit = mostProfitableByProfit,
                isLoading = false
            )
        }
    }
}


