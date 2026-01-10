package com.clientledger.app.util

import android.content.Context
import androidx.work.*
import com.clientledger.app.work.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily_reminder_work"
    
    fun scheduleDailyReminder(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        
        if (enabled) {
            android.util.Log.d("ReminderScheduler", "Scheduling reminder work")
            
            // Cancel existing work first
            workManager.cancelUniqueWork(WORK_NAME)
            android.util.Log.d("ReminderScheduler", "Cancelled existing work")
            
            // DEBUG: Schedule every 3 minutes for testing
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            // For debugging: repeat every 3 minutes
            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(3, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
                .setConstraints(constraints)
                .addTag("daily_reminder")
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            android.util.Log.d("ReminderScheduler", "Periodic work scheduled: every 3 minutes, starting in 1 minute")
            
            // Also schedule immediate one-time work for testing
            val oneTimeWork = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS) // Show notification in 30 seconds
                .setConstraints(constraints)
                .addTag("daily_reminder_test")
                .build()
            workManager.enqueue(oneTimeWork)
            android.util.Log.d("ReminderScheduler", "One-time test work scheduled: in 30 seconds")
        } else {
            // Cancel work if disabled
            android.util.Log.d("ReminderScheduler", "Cancelling reminder work")
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
