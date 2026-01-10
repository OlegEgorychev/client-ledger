package com.clientledger.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clientledger.app.data.database.AppDatabase
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.notification.NotificationManager
import com.clientledger.app.util.toDateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = LedgerRepository(
                database.clientDao(),
                database.appointmentDao(),
                database.expenseDao(),
                database.expenseItemDao(),
                database.serviceTagDao(),
                database.appointmentServiceDao()
            )

            // Get tomorrow's date
            val tomorrow = LocalDate.now().plusDays(1)
            val tomorrowDateKey = tomorrow.toDateKey()

            // Fetch tomorrow's appointments (excluding canceled)
            val appointments = repository.getTomorrowAppointmentsExcludingCanceled(tomorrowDateKey)

            // Load client names for appointments (need to fetch clients)
            val appointmentsWithClients = appointments.mapNotNull { appointment ->
                val client = repository.getClientById(appointment.clientId)
                client?.let { 
                    appointment to "${it.firstName} ${it.lastName}"
                }
            }

            // Build appointments list with client names in title
            val appointmentsForNotification = appointmentsWithClients.map { (appointment, clientName) ->
                appointment.copy(title = clientName)
            }

            // Show notification
            NotificationManager.showDailyReminderNotification(
                context = applicationContext,
                tomorrowDate = tomorrow,
                appointments = appointmentsForNotification,
                onNotificationClick = {
                    // Handled by pending intent in notification
                },
                onSendSmsClick = {
                    // Handled by pending intent in notification
                }
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
