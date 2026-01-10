package com.clientledger.app

import android.app.Application
import com.clientledger.app.data.database.AppDatabase
import com.clientledger.app.data.preferences.AppPreferences
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LedgerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        LedgerRepository(
            database.clientDao(),
            database.appointmentDao(),
            database.expenseDao(),
            database.expenseItemDao(),
            database.serviceTagDao(),
            database.appointmentServiceDao()
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channel
        com.clientledger.app.util.NotificationHelper.createNotificationChannel(this)
        
        // Initialize reminder scheduling based on preferences
        applicationScope.launch {
            val appPreferences = AppPreferences(this@LedgerApplication)
            // Get current preference value and schedule
            try {
                val enabled = appPreferences.dailyReminderEnabled.first()
                ReminderScheduler.scheduleDailyReminder(this@LedgerApplication, enabled)
            } catch (e: Exception) {
                // If error, schedule will be set when user toggles in Settings
            }
        }
    }
}
