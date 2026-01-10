package com.clientledger.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clientledger.app.MainActivity
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotificationManager {
    const val CHANNEL_ID_DAILY_REMINDER = "daily_reminder_channel"
    const val NOTIFICATION_ID_DAILY_REMINDER = 1001
    const val ACTION_SEND_SMS = "com.clientledger.app.SEND_SMS"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_DAILY_REMINDER,
                "Ежедневные напоминания",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о расписании на завтра"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDailyReminderNotification(
        context: Context,
        tomorrowDate: LocalDate,
        appointments: List<AppointmentEntity>,
        onNotificationClick: () -> Unit,
        onSendSmsClick: () -> Unit
    ) {
        createNotificationChannel(context)

        // Intent for opening Day Schedule when notification is tapped
        val dayScheduleIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_day", tomorrowDate.toString())
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            dayScheduleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Send SMS" action button
        val smsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = ACTION_SEND_SMS
            putExtra("navigate_to_sms_reminder", tomorrowDate.toString())
        }
        val smsPendingIntent = PendingIntent.getActivity(
            context,
            1,
            smsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification body
        val body = buildNotificationBody(appointments, tomorrowDate)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Завтра расписание")
            .setContentText(body.first)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.second))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .apply {
                // Only add "Отправить SMS" action if there are appointments
                if (appointments.isNotEmpty()) {
                    addAction(
                        android.R.drawable.ic_dialog_email,
                        "Отправить SMS",
                        smsPendingIntent
                    )
                }
            }
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    private fun buildNotificationBody(
        appointments: List<AppointmentEntity>,
        tomorrowDate: LocalDate
    ): Pair<String, String> {
        return if (appointments.isEmpty()) {
            Pair(
                "Завтра записей нет",
                "На ${DateUtils.formatDate(tomorrowDate)} записей нет."
            )
        } else {
            val count = appointments.size
            val shortText = "Записей: $count"
            
            val longText = buildString {
                append("Записей: $count\n")
                appointments.take(3).forEach { appointment ->
                    val time = DateUtils.dateTimeToLocalDateTime(appointment.startsAt)
                    val clientName = appointment.title // Using title as client name (could be improved)
                    append("${String.format("%02d:%02d", time.hour, time.minute)} $clientName\n")
                }
                if (appointments.size > 3) {
                    append("... и ещё ${appointments.size - 3}")
                }
            }
            
            Pair(shortText, longText)
        }
    }
}
