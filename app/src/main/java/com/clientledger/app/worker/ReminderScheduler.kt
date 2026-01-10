package com.clientledger.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily_reminder_work"

    /**
     * Schedules a daily reminder at 21:00.
     * WorkManager will execute this periodically, but we use constraints to ensure
     * it triggers around 21:00. Note: PeriodicWorkRequest has a minimum interval of 15 minutes,
     * so we schedule it to run daily with a time window constraint.
     */
    fun scheduleDailyReminder(context: Context) {
        // Calculate delay until next 21:00
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 21)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // If 21:00 has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= currentTimeMillis) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        
        val delayUntil2100 = calendar.timeInMillis - currentTimeMillis
        val initialDelayHours = TimeUnit.MILLISECONDS.toHours(delayUntil2100)

        // Create periodic work request (daily)
        // Note: PeriodicWorkRequest minimum interval is 15 minutes, but we want daily
        // We'll use a longer interval and handle the timing in the worker itself
        val dailyReminderWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS  // Run every 24 hours
        )
            .setInitialDelay(delayUntil2100, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyReminderWork
        )
    }

    /**
     * Cancels the scheduled daily reminder.
     */
    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
