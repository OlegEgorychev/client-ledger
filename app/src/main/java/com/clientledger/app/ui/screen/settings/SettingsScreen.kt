package com.clientledger.app.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clientledger.app.data.preferences.AppPreferences
import com.clientledger.app.util.ReminderScheduler
import com.clientledger.app.util.NotificationHelper
import com.clientledger.app.data.dao.AppointmentWithClient
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dailyReminderEnabled by appPreferences.dailyReminderEnabled.collectAsStateWithLifecycle(initialValue = false)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ежедневное напоминание о завтрашнем расписании",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DEBUG: Уведомления приходят каждые 3 минуты для тестирования",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Получать напоминание с расписанием на следующий день",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dailyReminderEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                appPreferences.setDailyReminderEnabled(enabled)
                                // Schedule/cancel reminder work
                                ReminderScheduler.scheduleDailyReminder(
                                    context = context,
                                    enabled = enabled
                                )
                            }
                        }
                    )
                }
            }
            
            // DEBUG: Test notification button
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Тестирование уведомлений",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Нажмите кнопку ниже, чтобы проверить, работает ли уведомление",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            // Create notification channel
                            NotificationHelper.createNotificationChannel(context)
                            
                            // Show test notification
                            NotificationHelper.showDailyReminder(
                                context = context,
                                appointments = emptyList(), // Empty list for test
                                tomorrowDate = LocalDate.now().plusDays(1)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Показать тестовое уведомление")
                    }
                }
            }
        }
    }
}
