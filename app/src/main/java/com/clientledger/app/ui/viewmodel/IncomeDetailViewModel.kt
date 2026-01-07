package com.clientledger.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clientledger.app.data.dao.ClientIncome
import com.clientledger.app.data.dao.DayIncome
import com.clientledger.app.data.repository.LedgerRepository
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

            val totalIncome = repository.getIncomeForDateRange(startDate, endDate)
            val incomeSeries = repository.getIncomeSeries(startDate, endDate)
            val incomeByClient = repository.getIncomeByClient(startDate, endDate)

            _uiState.value = _uiState.value.copy(
                totalIncome = totalIncome,
                incomeSeries = incomeSeries,
                incomeByClient = incomeByClient,
                isLoading = false
            )
        }
    }
}

