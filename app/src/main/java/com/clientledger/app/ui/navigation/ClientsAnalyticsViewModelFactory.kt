package com.clientledger.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.viewmodel.ClientsAnalyticsViewModel
import com.clientledger.app.ui.viewmodel.StatsPeriod
import java.time.LocalDate
import java.time.YearMonth

class ClientsAnalyticsViewModelFactory(
    private val repository: LedgerRepository,
    private val period: StatsPeriod,
    private val selectedDate: LocalDate,
    private val selectedYearMonth: YearMonth,
    private val selectedYear: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientsAnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientsAnalyticsViewModel(repository, period, selectedDate, selectedYearMonth, selectedYear) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

