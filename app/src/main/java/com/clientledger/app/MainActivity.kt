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
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.ui.viewmodel.ClientsViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as LedgerApplication
        val repository = app.repository

        setContent {
            ClientLedgerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(repository)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(repository: LedgerRepository) {
    val navController = rememberNavController()

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
                    repository = repository
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
                ExpenseEditScreenWrapper(
                    expenseId = expenseId,
                    date = localDate,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("expense_edit/{expenseId}") { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull()
            ExpenseEditScreenWrapper(
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

@Composable
fun ExpenseEditScreenWrapper(
    expenseId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    onBack: () -> Unit,
    viewModel: CalendarViewModel = viewModel(
        factory = com.clientledger.app.ui.navigation.CalendarViewModelFactory(repository)
    )
) {
    ExpenseEditScreen(
        expenseId = expenseId,
        date = date,
        repository = repository,
        viewModel = viewModel,
        onBack = onBack
    )
}


