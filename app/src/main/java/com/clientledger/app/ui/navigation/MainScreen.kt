package com.clientledger.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.screen.calendar.CalendarScreen
import com.clientledger.app.ui.screen.calendar.DayScheduleScreen
import com.clientledger.app.ui.screen.clients.ClientsScreen
import com.clientledger.app.ui.screen.stats.IncomeDetailScreen
import com.clientledger.app.ui.screen.stats.StatsScreen
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.ui.viewmodel.ClientsViewModel
import com.clientledger.app.ui.viewmodel.StatsViewModel
import java.time.LocalDate

sealed class MainScreenDestination(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Calendar : MainScreenDestination("calendar", "Календарь", Icons.Default.CalendarToday)
    object Clients : MainScreenDestination("clients", "Клиенты", Icons.Default.Person)
    object Stats : MainScreenDestination("stats", "Статистика", Icons.Default.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: LedgerRepository,
    onClientClick: (Long) -> Unit,
    onAddClient: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onAppointmentClick: (Long) -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit
) {
    // Единый NavController для всех экранов (вкладки + день)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Навигация на день через navController (внутри того же NavHost)
    val internalOnDateClick: (LocalDate) -> Unit = { date ->
        navController.navigate("day/${date.toString()}") {
            // Просто добавляем день в стек поверх календаря
            // Календарь остается в стеке и будет восстановлен при возврате
            launchSingleTop = true
            restoreState = true
        }
    }

    val destinations = listOf(
        MainScreenDestination.Calendar,
        MainScreenDestination.Clients,
        MainScreenDestination.Stats
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) },
                        selected = when {
                            currentRoute == destination.route -> true
                            // Treat Day screen as part of Calendar tab
                            destination == MainScreenDestination.Calendar &&
                                (currentRoute?.startsWith("day/") == true) -> true
                            else -> false
                        },
                        onClick = {
                            // Если уже на этой вкладке, ничего не делаем
                            if (currentRoute != destination.route) {
                                // Пытаемся вернуться на вкладку, если она в стеке
                                val popped = navController.popBackStack(destination.route, inclusive = false)
                                
                                if (!popped) {
                                    // Вкладки нет в стеке, навигируем на неё
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = MainScreenDestination.Calendar.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Экран дня для выбранной даты
            composable("day/{dateIso}") { backStackEntry ->
                val dateStr = backStackEntry.arguments?.getString("dateIso")
                dateStr?.let { date ->
                    val localDate = LocalDate.parse(date)
                    DayScheduleScreen(
                        date = localDate,
                        repository = repository,
                        onBack = { /* Не нужен, так как BottomBar всегда виден */ },
                        onAppointmentClick = onAppointmentClick,
                        onDateChange = null // Переключение дней происходит внутри экрана через состояние
                    )
                }
            }
            
            composable(MainScreenDestination.Clients.route) {
                ClientsScreen(
                    onClientClick = onClientClick,
                    onAddClient = onAddClient,
                    viewModel = viewModel(
                        factory = ClientsViewModelFactory(repository)
                    )
                )
            }

            composable(MainScreenDestination.Calendar.route) {
                CalendarScreen(
                    onDateClick = internalOnDateClick,
                    onAddAppointment = onAddAppointment,
                    onAddExpense = onAddExpense,
                    onIncomeDetailClick = { period, date, yearMonth, year ->
                        navController.navigate("income_detail/$period/${date.toString()}/${yearMonth.year}-${yearMonth.monthValue}/$year")
                    },
                    repository = repository,
                    viewModel = viewModel(
                        factory = CalendarViewModelFactory(repository)
                    )
                )
            }

            composable(MainScreenDestination.Stats.route) {
                StatsScreen(
                    viewModel = viewModel(
                        factory = StatsViewModelFactory(repository)
                    ),
                    onIncomeClick = { period, date, yearMonth, year ->
                        navController.navigate("income_detail/$period/${date.toString()}/${yearMonth.year}-${yearMonth.monthValue}/$year")
                    },
                    onClientsClick = { period, date, yearMonth, year ->
                        navController.navigate("clients_analytics/$period/${date.toString()}/${yearMonth.year}-${yearMonth.monthValue}/$year")
                    },
                    onVisitsClick = { period, date, yearMonth, year ->
                        navController.navigate("visits_analytics/$period/${date.toString()}/${yearMonth.year}-${yearMonth.monthValue}/$year")
                    },
                    onReportsClick = { period, date, yearMonth, year ->
                        navController.navigate("reports_insights/$period/${date.toString()}/${yearMonth.year}-${yearMonth.monthValue}/$year")
                    }
                )
            }
            
            // Income Detail Screen
            composable("income_detail/{period}/{date}/{yearMonth}/{year}") { backStackEntry ->
                val periodStr = backStackEntry.arguments?.getString("period")
                val dateStr = backStackEntry.arguments?.getString("date")
                val yearMonthStr = backStackEntry.arguments?.getString("yearMonth")
                val yearStr = backStackEntry.arguments?.getString("year")
                
                val period = when (periodStr) {
                    "DAY" -> com.clientledger.app.ui.viewmodel.StatsPeriod.DAY
                    "MONTH" -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                    "YEAR" -> com.clientledger.app.ui.viewmodel.StatsPeriod.YEAR
                    else -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                }
                
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val yearMonth = yearMonthStr?.let {
                    val parts = it.split("-")
                    if (parts.size == 2) {
                        java.time.YearMonth.of(parts[0].toInt(), parts[1].toInt())
                    } else {
                        java.time.YearMonth.now()
                    }
                } ?: java.time.YearMonth.now()
                val year = yearStr?.toIntOrNull() ?: LocalDate.now().year
                
                IncomeDetailScreen(
                    period = period,
                    selectedDate = date,
                    selectedYearMonth = yearMonth,
                    selectedYear = year,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Clients Analytics Screen
            composable("clients_analytics/{period}/{date}/{yearMonth}/{year}") { backStackEntry ->
                val periodStr = backStackEntry.arguments?.getString("period")
                val dateStr = backStackEntry.arguments?.getString("date")
                val yearMonthStr = backStackEntry.arguments?.getString("yearMonth")
                val yearStr = backStackEntry.arguments?.getString("year")
                
                val period = when (periodStr) {
                    "DAY" -> com.clientledger.app.ui.viewmodel.StatsPeriod.DAY
                    "MONTH" -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                    "YEAR" -> com.clientledger.app.ui.viewmodel.StatsPeriod.YEAR
                    else -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                }
                
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val yearMonth = yearMonthStr?.let {
                    val parts = it.split("-")
                    if (parts.size == 2) {
                        java.time.YearMonth.of(parts[0].toInt(), parts[1].toInt())
                    } else {
                        java.time.YearMonth.now()
                    }
                } ?: java.time.YearMonth.now()
                val year = yearStr?.toIntOrNull() ?: LocalDate.now().year
                
                com.clientledger.app.ui.screen.stats.ClientsAnalyticsScreen(
                    period = period,
                    selectedDate = date,
                    selectedYearMonth = yearMonth,
                    selectedYear = year,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onClientClick = onClientClick
                )
            }
            
            // Visits Analytics Screen
            composable("visits_analytics/{period}/{date}/{yearMonth}/{year}") { backStackEntry ->
                val periodStr = backStackEntry.arguments?.getString("period")
                val dateStr = backStackEntry.arguments?.getString("date")
                val yearMonthStr = backStackEntry.arguments?.getString("yearMonth")
                val yearStr = backStackEntry.arguments?.getString("year")
                
                val period = when (periodStr) {
                    "DAY" -> com.clientledger.app.ui.viewmodel.StatsPeriod.DAY
                    "MONTH" -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                    "YEAR" -> com.clientledger.app.ui.viewmodel.StatsPeriod.YEAR
                    else -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                }
                
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val yearMonth = yearMonthStr?.let {
                    val parts = it.split("-")
                    if (parts.size == 2) {
                        java.time.YearMonth.of(parts[0].toInt(), parts[1].toInt())
                    } else {
                        java.time.YearMonth.now()
                    }
                } ?: java.time.YearMonth.now()
                val year = yearStr?.toIntOrNull() ?: LocalDate.now().year
                
                com.clientledger.app.ui.screen.stats.VisitsAnalyticsScreen(
                    period = period,
                    selectedDate = date,
                    selectedYearMonth = yearMonth,
                    selectedYear = year,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Reports/Insights Screen
            composable("reports_insights/{period}/{date}/{yearMonth}/{year}") { backStackEntry ->
                val periodStr = backStackEntry.arguments?.getString("period")
                val dateStr = backStackEntry.arguments?.getString("date")
                val yearMonthStr = backStackEntry.arguments?.getString("yearMonth")
                val yearStr = backStackEntry.arguments?.getString("year")
                
                val period = when (periodStr) {
                    "DAY" -> com.clientledger.app.ui.viewmodel.StatsPeriod.DAY
                    "MONTH" -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                    "YEAR" -> com.clientledger.app.ui.viewmodel.StatsPeriod.YEAR
                    else -> com.clientledger.app.ui.viewmodel.StatsPeriod.MONTH
                }
                
                val date = dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now()
                val yearMonth = yearMonthStr?.let {
                    val parts = it.split("-")
                    if (parts.size == 2) {
                        java.time.YearMonth.of(parts[0].toInt(), parts[1].toInt())
                    } else {
                        java.time.YearMonth.now()
                    }
                } ?: java.time.YearMonth.now()
                val year = yearStr?.toIntOrNull() ?: LocalDate.now().year
                
                com.clientledger.app.ui.screen.stats.ReportsInsightsScreen(
                    period = period,
                    selectedDate = date,
                    selectedYearMonth = yearMonth,
                    selectedYear = year,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onDayClick = internalOnDateClick,
                    onClientClick = onClientClick
                )
            }
        }
    }
}

// ViewModel Factories
class ClientsViewModelFactory(private val repository: LedgerRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CalendarViewModelFactory(private val repository: LedgerRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class StatsViewModelFactory(private val repository: LedgerRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DayScheduleViewModelFactory(
    private val date: LocalDate,
    private val repository: LedgerRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.clientledger.app.ui.viewmodel.DayScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.clientledger.app.ui.viewmodel.DayScheduleViewModel(date, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AppointmentDetailsViewModelFactory(
    private val appointmentId: Long,
    private val repository: LedgerRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.clientledger.app.ui.viewmodel.AppointmentDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.clientledger.app.ui.viewmodel.AppointmentDetailsViewModel(appointmentId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


