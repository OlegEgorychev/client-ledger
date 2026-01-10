package com.clientledger.app.util

import android.content.Context
import androidx.work.*
import com.clientledger.app.work.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME_PRODUCTION = "daily_reminder_work_production"
    private const val WORK_NAME_DEBUG = "daily_reminder_work_debug"
    
    /**
     * Schedules production daily reminder at specified time for tomorrow's schedule
     */
    fun scheduleDailyReminder(context: Context, enabled: Boolean, hour: Int = 21, minute: Int = 0) {
        val workManager = WorkManager.getInstance(context)
        
        if (enabled) {
            android.util.Log.d("ReminderScheduler", "Scheduling production reminder work (daily at $hour:${String.format("%02d", minute)})")
            
            // Cancel existing production work first
            workManager.cancelUniqueWork(WORK_NAME_PRODUCTION)
            android.util.Log.d("ReminderScheduler", "Cancelled existing production work")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            // Calculate time until target time today (or tomorrow if it's past target time)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            
            // Validate and clamp hour and minute
            val targetHour = hour.coerceIn(0, 23)
            val targetMinute = minute.coerceIn(0, 59)
            
            calendar.set(Calendar.HOUR_OF_DAY, targetHour)
            calendar.set(Calendar.MINUTE, targetMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // If it's already past target time, schedule for tomorrow
            if (currentHour > targetHour || (currentHour == targetHour && currentMinute >= targetMinute)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val delayMillis = calendar.timeInMillis - System.currentTimeMillis()
            
            // Schedule daily work at target time
            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag("daily_reminder_production")
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_PRODUCTION,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            android.util.Log.d("ReminderScheduler", "Production reminder scheduled: daily at $targetHour:${String.format("%02d", targetMinute)}, first in ${delayMillis / 1000 / 60} minutes")
        } else {
            // Cancel production work if disabled
            android.util.Log.d("ReminderScheduler", "Cancelling production reminder work")
            workManager.cancelUniqueWork(WORK_NAME_PRODUCTION)
        }
    }
    
    /**
     * Schedules debug reminder every 3 minutes for testing
     */
    fun scheduleDebugReminder(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        
        if (enabled) {
            android.util.Log.d("ReminderScheduler", "Scheduling debug reminder work (every 3 minutes)")
            
            // Cancel existing debug work first
            workManager.cancelUniqueWork(WORK_NAME_DEBUG)
            android.util.Log.d("ReminderScheduler", "Cancelled existing debug work")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            // For debugging: repeat every 3 minutes
            val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(3, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
                .setConstraints(constraints)
                .addTag("daily_reminder_debug")
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_DEBUG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            android.util.Log.d("ReminderScheduler", "Debug reminder scheduled: every 3 minutes, starting in 1 minute")
            
            // Also schedule immediate one-time work for testing
            val oneTimeWork = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS) // Show notification in 30 seconds
                .setConstraints(constraints)
                .addTag("daily_reminder_test")
                .build()
            workManager.enqueue(oneTimeWork)
            android.util.Log.d("ReminderScheduler", "One-time test work scheduled: in 30 seconds")
        } else {
            // Cancel debug work if disabled
            android.util.Log.d("ReminderScheduler", "Cancelling debug reminder work")
            workManager.cancelUniqueWork(WORK_NAME_DEBUG)
        }
    }
}
