package com.clientledger.app

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.navigation.MainScreen
import com.clientledger.app.ui.screen.SplashScreen
import com.clientledger.app.ui.screen.calendar.AppointmentDetailsScreen
import com.clientledger.app.ui.screen.calendar.AppointmentEditScreen
import com.clientledger.app.ui.screen.calendar.DayDetailScreen
import com.clientledger.app.ui.screen.calendar.DayScheduleScreen
import com.clientledger.app.ui.screen.calendar.ExpenseEditScreen
import com.clientledger.app.ui.screen.clients.ClientDetailScreen
import com.clientledger.app.ui.screen.clients.ClientEditScreen
import com.clientledger.app.ui.theme.ClientLedgerTheme
import com.clientledger.app.ui.theme.ThemeMode
import com.clientledger.app.data.preferences.ThemePreferences
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.ui.viewmodel.ClientsViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.Intent

class MainActivity : ComponentActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private val _notificationIntentData = MutableStateFlow<NotificationIntentData?>(null)
    val notificationIntentData: StateFlow<NotificationIntentData?> = _notificationIntentData.asStateFlow()
    
    data class NotificationIntentData(
        val navigateToDay: String?,
        val smsRemindersAction: Boolean,
        val tomorrowDate: String?
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
        
        // Handle initial intent
        handleIntent(intent)
        
        val app = application as LedgerApplication
        val repository = app.repository
        val themePreferences = ThemePreferences(this)
        
        // Initialize default service tags on first launch
        lifecycleScope.launch {
            repository.initializeDefaultTags()
        }

        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            val currentIntentData by notificationIntentData.collectAsStateWithLifecycle()
            
            ClientLedgerTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        repository = repository, 
                        themePreferences = themePreferences,
                        notificationIntentData = currentIntentData,
                        onIntentHandled = { 
                            _notificationIntentData.value = null
                        }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "🔔 onNewIntent called! action: ${intent?.action}, extras: ${intent?.extras?.keySet()}")
        setIntent(intent)
        handleIntent(intent)
        android.util.Log.d("MainActivity", "onNewIntent: NotificationIntentData after handleIntent: ${_notificationIntentData.value}")
    }
    
    override fun onResume() {
        super.onResume()
        // Also check intent in onResume in case activity was already running
        android.util.Log.d("MainActivity", "onResume called, checking intent: action=${intent.action}")
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            android.util.Log.d("MainActivity", "handleIntent: intent is null")
            return
        }
        
        val navigateToDay = intent.getStringExtra("navigate_to_day")
        val smsRemindersAction = intent.action == com.clientledger.app.util.NotificationHelper.ACTION_SMS_REMINDERS
        val tomorrowDate = intent.getStringExtra("tomorrow_date")
        
        android.util.Log.d("MainActivity", "handleIntent: action=${intent.action}, navigateToDay=$navigateToDay, smsRemindersAction=$smsRemindersAction, tomorrowDate=$tomorrowDate")
        
        // Only update if we have relevant data
        if (navigateToDay != null || (smsRemindersAction && tomorrowDate != null)) {
            val newData = NotificationIntentData(
                navigateToDay = navigateToDay,
                smsRemindersAction = smsRemindersAction,
                tomorrowDate = tomorrowDate
            )
            android.util.Log.d("MainActivity", "📝 Updating NotificationIntentData: $newData")
            _notificationIntentData.value = newData
            
            // Force a small delay to ensure StateFlow update is processed
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.util.Log.d("MainActivity", "✅ NotificationIntentData after update: ${_notificationIntentData.value}")
            }, 100)
        } else {
            android.util.Log.d("MainActivity", "⚠️ No relevant data in intent, skipping update. navigateToDay=$navigateToDay, smsRemindersAction=$smsRemindersAction, tomorrowDate=$tomorrowDate")
        }
    }
}

@Composable
fun AppNavigation(
    repository: LedgerRepository,
    themePreferences: ThemePreferences,
    notificationIntentData: MainActivity.NotificationIntentData?,
    onIntentHandled: () -> Unit
) {
    val navController = rememberNavController()
    
    // Navigate to main if we have notification intent data, but don't clear it yet
    // MainScreen will handle the actual navigation and call onIntentHandled
    LaunchedEffect(notificationIntentData) {
        if (notificationIntentData != null) {
            android.util.Log.d("AppNavigation", "Notification intent received, navigating to main screen: $notificationIntentData")
            
            // Ensure we're on main screen - MainScreen will handle the actual navigation
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
                launchSingleTop = true
            }
            
            // DON'T call onIntentHandled here - let MainScreen handle it after navigation completes
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        
        composable("main") {
            MainScreen(
                repository = repository,
                themePreferences = themePreferences,
                notificationIntentData = notificationIntentData,
                onIntentHandled = onIntentHandled,
                onClientClick = { clientId ->
                    navController.navigate("client_detail/$clientId")
                },
                onAddClient = {
                    navController.navigate("client_edit")
                },
                onDateClick = { date ->
                    // Навигация на день теперь происходит внутри MainScreen
                    // Этот колбэк больше не используется, но оставляем для совместимости
                },
                onAppointmentClick = { appointmentId ->
                    navController.navigate("appointment_details/$appointmentId")
                },
                onAddAppointment = {
                    navController.navigate("appointment_edit/null/${LocalDate.now().toString()}")
                },
                onAddExpense = {
                    navController.navigate("expense_edit/null")
                }
            )
        }

        composable("client_detail/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")?.toLongOrNull()
            clientId?.let { id ->
                ClientDetailScreen(
                    clientId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("client_edit/$id") },
                    repository = repository,
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }
        }

        composable("client_edit/{clientId}") { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")?.toLongOrNull()
            ClientEditScreen(
                clientId = clientId,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable("client_edit") {
            ClientEditScreen(
                clientId = null,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable("day_detail/{date}") { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date")
            dateStr?.let { date ->
                val localDate = LocalDate.parse(date)
                DayDetailScreenWrapper(
                    date = localDate,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onAddAppointment = {
                        navController.navigate("appointment_edit/null/$date") {
                            popUpTo(backStackEntry.destination.id) { inclusive = true }
                        }
                    },
                    onAddExpense = {
                        navController.navigate("expense_edit/null/$date") {
                            popUpTo(backStackEntry.destination.id) { inclusive = true }
                        }
                    },
                    onAppointmentClick = { appointmentId ->
                        navController.navigate("appointment_details/$appointmentId")
                    },
                    onExpenseClick = { expenseId ->
                        navController.navigate("expense_edit/$expenseId")
                    }
                )
            }
        }

        composable("appointment_edit/{appointmentId}/{date}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")?.toLongOrNull()
            val dateStr = backStackEntry.arguments?.getString("date")
            dateStr?.let { date ->
                val localDate = LocalDate.parse(date)
                AppointmentEditScreenWrapper(
                    appointmentId = appointmentId,
                    date = localDate,
                    repository = repository,
                onBack = {
                    // Просто возвращаемся назад - если редактировали из details, вернемся на details
                    navController.popBackStack()
                }
                )
            }
        }

        composable("appointment_details/{appointmentId}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")?.toLongOrNull()
            appointmentId?.let { id ->
                AppointmentDetailsScreen(
                    appointmentId = id,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onEdit = { appointmentId ->
                        // Получаем дату из appointment для навигации
                        navController.navigate("appointment_edit/$appointmentId")
                    }
                )
            }
        }

        composable("appointment_edit/{appointmentId}") { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")?.toLongOrNull()
            AppointmentEditScreenWrapper(
                appointmentId = appointmentId,
                date = LocalDate.now(),
                repository = repository,
                onBack = {
                    // Просто возвращаемся назад - если редактировали из details, вернемся на details
                    navController.popBackStack()
                }
            )
        }

        composable("expense_edit/{expenseId}/{date}") { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
            val dateStr = backStackEntry.arguments?.getString("date")
            dateStr?.let { date ->
                val localDate = LocalDate.parse(date)
                ExpenseEditScreen(
                    expenseId = expenseId,
                    date = localDate,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("expense_edit/{expenseId}") { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
            ExpenseEditScreen(
                expenseId = expenseId,
                date = LocalDate.now(),
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun DayDetailScreenWrapper(
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit,
    onAppointmentClick: (Long) -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: CalendarViewModel = viewModel(
        factory = com.clientledger.app.ui.navigation.CalendarViewModelFactory(repository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    androidx.compose.runtime.LaunchedEffect(date) {
        viewModel.selectDate(date)
    }

    DayDetailScreen(
        repository = repository,
        date = date,
        appointments = uiState.dayAppointments,
        expenses = uiState.dayExpenses,
        onBack = onBack,
        onAddAppointment = onAddAppointment,
        onAddExpense = onAddExpense,
        onAppointmentClick = { appointment ->
            onAppointmentClick(appointment.id)
        },
        onExpenseClick = { expense ->
            onExpenseClick(expense.id)
        }
    )
}

@Composable
fun AppointmentEditScreenWrapper(
    appointmentId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit,
    viewModel: CalendarViewModel = viewModel(
        factory = com.clientledger.app.ui.navigation.CalendarViewModelFactory(repository)
    )
) {
    AppointmentEditScreen(
        appointmentId = appointmentId,
        date = date,
        repository = repository,
        viewModel = viewModel,
        onBack = onBack
    )
}

