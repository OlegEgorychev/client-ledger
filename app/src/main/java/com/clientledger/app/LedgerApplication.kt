package com.clientledger.app

import android.app.Application
import android.content.pm.PackageManager
import com.clientledger.app.data.backup.BackupScheduler
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
    
    val backupScheduler by lazy {
        val appVersion = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))?.versionName ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)?.versionName ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
        BackupScheduler(this, repository, appVersion)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channel
        com.clientledger.app.util.NotificationHelper.createNotificationChannel(this)
        
        // Initialize reminder scheduling based on preferences
        applicationScope.launch {
            val appPreferences = AppPreferences(this@LedgerApplication)
            // Get current preference values and schedule both modes
            try {
                val productionEnabled = appPreferences.dailyReminderEnabled.first()
                val reminderHour = appPreferences.reminderHour.first()
                val reminderMinute = appPreferences.reminderMinute.first()
                ReminderScheduler.scheduleDailyReminder(
                    this@LedgerApplication, 
                    productionEnabled,
                    hour = reminderHour,
                    minute = reminderMinute
                )
                
                val debugEnabled = appPreferences.debugReminderEnabled.first()
                ReminderScheduler.scheduleDebugReminder(this@LedgerApplication, debugEnabled)
            } catch (e: Exception) {
                // If error, schedule will be set when user toggles in Settings
            }
        }
    }
}
