package com.clientledger.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.clientledger.app.ui.screen.calendar.AppointmentEditScreen
import com.clientledger.app.ui.screen.calendar.PersonalEventEditScreen
import com.clientledger.app.ui.screen.calendar.ExpenseEditScreen
import com.clientledger.app.ui.screen.settings.SettingsScreen
import com.clientledger.app.ui.screen.reminders.SmsReminderScreen
import com.clientledger.app.data.preferences.AppPreferences
import androidx.compose.ui.platform.LocalContext
import com.clientledger.app.util.toDateKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
    themePreferences: com.clientledger.app.data.preferences.ThemePreferences,
    notificationIntentData: com.clientledger.app.MainActivity.NotificationIntentData? = null,
    onIntentHandled: () -> Unit = {},
    onClientClick: (Long) -> Unit,
    onAddClient: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onAppointmentClick: (Long) -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
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
    
    // Handle navigation from notification intent
    // Track processed intents to avoid multiple navigations
    var processedIntentKey by remember { mutableStateOf<String?>(null) }
    
    // Create a stable key for LaunchedEffect
    val intentKey = remember(notificationIntentData) {
        notificationIntentData?.let { "${it.navigateToDay}_${it.smsRemindersAction}_${it.tomorrowDate}" } ?: null
    }
    
    LaunchedEffect(intentKey) {
        val data = notificationIntentData ?: return@LaunchedEffect
        if (intentKey == null || processedIntentKey == intentKey) return@LaunchedEffect
        
        android.util.Log.d("MainScreen", "🚀 Processing notification intent: $data, key: $intentKey")
        
        // Wait for NavHost to be ready
        kotlinx.coroutines.delay(500)
        
        try {
            if (data.navigateToDay != null) {
                android.util.Log.d("MainScreen", "Navigating to day: ${data.navigateToDay}")
                navController.navigate("day/${data.navigateToDay}") {
                    popUpTo("calendar") { inclusive = false }
                    launchSingleTop = true
                }
                processedIntentKey = intentKey
                onIntentHandled()
            } else if (data.smsRemindersAction && data.tomorrowDate != null) {
                android.util.Log.d("MainScreen", "🚀 SMS REMINDERS: Navigating to sms_reminders/${data.tomorrowDate}")
                val targetRoute = "sms_reminders/${data.tomorrowDate}"
                
                // Navigate directly with simple options
                try {
                    navController.navigate(targetRoute) {
                        popUpTo("calendar") { inclusive = false }
                        launchSingleTop = true
                    }
                    android.util.Log.d("MainScreen", "✅ SMS reminders navigation call completed")
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "Navigation exception, trying alternative", e)
                    // Fallback: try without popUpTo
                    try {
                        navController.navigate(targetRoute) {
                            launchSingleTop = true
                        }
                    } catch (e2: Exception) {
                        android.util.Log.e("MainScreen", "Alternative navigation also failed", e2)
                    }
                }
                
                processedIntentKey = intentKey
                onIntentHandled()
            } else {
                android.util.Log.w("MainScreen", "Intent data incomplete")
                processedIntentKey = intentKey
                onIntentHandled()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainScreen", "❌ Error during navigation", e)
            e.printStackTrace()
            processedIntentKey = intentKey
            onIntentHandled()
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
                    val isSelected = when {
                        currentRoute == destination.route -> true
                        // Treat Day screen as part of Calendar tab
                        destination == MainScreenDestination.Calendar &&
                            (currentRoute?.startsWith("day/") == true || currentRoute?.startsWith("sms_reminders/") == true) -> true
                        else -> false
                    }
                    
                    NavigationBarItem(
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
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
                        onExpenseClick = { expenseId ->
                            navController.navigate("expense_edit/$expenseId/${localDate.toString()}")
                        },
                        onAddAppointment = {
                            navController.navigate("appointment_edit/null/${localDate.toString()}")
                        },
                        onAddExpense = {
                            navController.navigate("expense_edit/null/${localDate.toString()}")
                        },
                        onAddPersonalEvent = { day ->
                            navController.navigate("personal_event_edit/null/${day}")
                        },
                        onPersonalEventClick = { eventId ->
                            navController.navigate("personal_event_edit/$eventId/${localDate}")
                        },
                        onDateChange = null, // Переключение дней происходит внутри экрана через состояние
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
            }
            
            composable(MainScreenDestination.Clients.route) {
                ClientsScreen(
                    onClientClick = onClientClick,
                    onAddClient = onAddClient,
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    viewModel = viewModel(
                        factory = ClientsViewModelFactory(repository)
                    )
                )
            }

            composable(MainScreenDestination.Calendar.route) {
                CalendarScreen(
                    onDateLongClick = { date ->
                        // Long press opens Day Schedule
                        navController.navigate("day/${date.toString()}")
                    },
                    onAddAppointment = { date ->
                        // Use selected date from calendar, fallback to today if null
                        navController.navigate("appointment_edit/null/${date.toString()}")
                    },
                    onAddExpense = { date ->
                        // Use selected date from calendar, fallback to today if null
                        navController.navigate("expense_edit/null/${date.toString()}")
                    },
                    onAddPersonalEvent = { date ->
                        navController.navigate("personal_event_edit/null/${date}")
                    },
                    onIncomeDetailClick = { period, date, yearMonth, year ->
                        navController.navigate("income_detail/$period/${date.toString()}/${yearMonth.year}-${yearMonth.monthValue}/$year")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    repository = repository,
                    themePreferences = themePreferences,
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
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
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
                    onBack = { navController.popBackStack() },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
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
            
            // Appointment Edit Screen
            composable("appointment_edit/{appointmentId}/{date}") { backStackEntry ->
                val appointmentId = backStackEntry.arguments?.getString("appointmentId")?.toLongOrNull()
                val dateStr = backStackEntry.arguments?.getString("date")
                dateStr?.let { date ->
                    val localDate = LocalDate.parse(date)
                    AppointmentEditScreen(
                        appointmentId = appointmentId,
                        date = localDate,
                        repository = repository,
                        viewModel = viewModel(factory = CalendarViewModelFactory(repository)),
                        onBack = { navController.popBackStack() },
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
            }
            
            // Expense Edit Screen
            composable("expense_edit/{expenseId}/{date}") { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
                val dateStr = backStackEntry.arguments?.getString("date")
                dateStr?.let { date ->
                    val localDate = LocalDate.parse(date)
                    ExpenseEditScreen(
                        expenseId = expenseId,
                        date = localDate,
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
            }
            
            // Personal Event Edit Screen (личное дело)
            composable("personal_event_edit/{eventId}/{date}") { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull()
                val dateStr = backStackEntry.arguments?.getString("date")
                dateStr?.let { date ->
                    PersonalEventEditScreen(
                        eventId = eventId,
                        date = LocalDate.parse(date),
                        repository = repository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Settings Screen
            composable("settings") {
                SettingsScreen(
                    appPreferences = appPreferences,
                    themePreferences = themePreferences,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // SMS Reminders Screen
            composable("sms_reminders/{tomorrowDate}") { backStackEntry ->
                android.util.Log.d("MainScreen", "🎯 SMS Reminders composable called! tomorrowDate argument: ${backStackEntry.arguments?.getString("tomorrowDate")}")
                val tomorrowDateStr = backStackEntry.arguments?.getString("tomorrowDate")
                tomorrowDateStr?.let { dateStr ->
                    android.util.Log.d("MainScreen", "Parsing tomorrowDate: $dateStr")
                    val tomorrowDate = try {
                        LocalDate.parse(dateStr)
                    } catch (e: Exception) {
                        android.util.Log.e("MainScreen", "Error parsing date: $dateStr", e)
                        LocalDate.now().plusDays(1) // Fallback to tomorrow
                    }
                    android.util.Log.d("MainScreen", "Parsed tomorrowDate: $tomorrowDate")
                    
                    val scope = rememberCoroutineScope()
                    var appointments by remember { mutableStateOf<List<com.clientledger.app.data.dao.AppointmentWithClient>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(true) }
                    
                    LaunchedEffect(tomorrowDate) {
                        android.util.Log.d("MainScreen", "Loading appointments for date: ${tomorrowDate.toDateKey()}")
                        scope.launch {
                            isLoading = true
                            appointments = repository.getTomorrowAppointmentsWithClient(tomorrowDate.toDateKey())
                            isLoading = false
                            android.util.Log.d("MainScreen", "Loaded ${appointments.size} appointments for SMS reminders")
                        }
                    }
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        SmsReminderScreen(
                            appointments = appointments,
                            tomorrowDate = tomorrowDate,
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
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


