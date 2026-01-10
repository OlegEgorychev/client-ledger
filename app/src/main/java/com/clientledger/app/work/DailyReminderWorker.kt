package com.clientledger.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clientledger.app.LedgerApplication
import com.clientledger.app.util.NotificationHelper
import com.clientledger.app.util.toDateKey
import java.time.LocalDate

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            android.util.Log.d("DailyReminderWorker", "Worker started")
            
            val app = applicationContext as LedgerApplication
            val repository = app.repository
            
            // Get tomorrow's date
            val tomorrow = LocalDate.now().plusDays(1)
            val tomorrowDateKey = tomorrow.toDateKey()
            
            android.util.Log.d("DailyReminderWorker", "Fetching appointments for date: $tomorrowDateKey")
            
            // Fetch tomorrow's appointments with client info
            val appointments = repository.getTomorrowAppointmentsWithClient(tomorrowDateKey)
            
            android.util.Log.d("DailyReminderWorker", "Found ${appointments.size} appointments")
            
            // Create notification channel if needed (always recreate to ensure proper settings)
            NotificationHelper.createNotificationChannel(applicationContext)
            
            // Show notification
            NotificationHelper.showDailyReminder(
                context = applicationContext,
                appointments = appointments,
                tomorrowDate = tomorrow
            )
            
            android.util.Log.d("DailyReminderWorker", "Notification shown successfully")
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("DailyReminderWorker", "Error in worker", e)
            Result.retry()
        }
    }
}
