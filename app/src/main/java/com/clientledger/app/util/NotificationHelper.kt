package com.clientledger.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clientledger.app.MainActivity
import com.clientledger.app.R
import com.clientledger.app.data.dao.AppointmentWithClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {
    const val CHANNEL_ID = "daily_reminder_channel"
    const val CHANNEL_NAME = "Ежедневные напоминания"
    const val NOTIFICATION_ID = 1001
    const val ACTION_SMS_REMINDERS = "com.clientledger.app.ACTION_SMS_REMINDERS"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if channel already exists
            var existingChannel: NotificationChannel? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            }
            
            // If channel exists but has wrong importance, delete and recreate
            if (existingChannel != null) {
                if (existingChannel.importance != NotificationManager.IMPORTANCE_HIGH) {
                    android.util.Log.d("NotificationHelper", "Deleting existing channel to recreate with HIGH importance")
                    notificationManager.deleteNotificationChannel(CHANNEL_ID)
                    existingChannel = null
                } else {
                    android.util.Log.d("NotificationHelper", "Channel already exists with HIGH importance")
                    return
                }
            }
            
            // Create new channel if it doesn't exist
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // High importance for heads-up notification
            ).apply {
                description = "Напоминания о расписании на завтра"
                enableVibration(true)
                enableLights(true)
                // Use default notification sound
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("NotificationHelper", "Notification channel created with HIGH importance")
        }
    }
    
    fun showDailyReminder(
        context: Context,
        appointments: List<AppointmentWithClient>,
        tomorrowDate: LocalDate
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        // Main content intent - opens Day Schedule for tomorrow
        val tomorrowDateStr = tomorrowDate.toString()
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to_day", tomorrowDateStr)
            setPackage(context.packageName)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // SMS reminders action intent
        val smsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = ACTION_SMS_REMINDERS
            putExtra("tomorrow_date", tomorrowDateStr)
            // Add package name to ensure intent is delivered
            setPackage(context.packageName)
        }
        val smsPendingIntent = PendingIntent.getActivity(
            context,
            1,
            smsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        android.util.Log.d("NotificationHelper", "SMS intent created: action=$ACTION_SMS_REMINDERS, tomorrowDate=$tomorrowDateStr, package=${context.packageName}")
        
        // Build notification content
        val title = "Завтра расписание"
        val body = if (appointments.isEmpty()) {
            "Завтра записей нет"
        } else {
            val count = appointments.size
            val firstFew = appointments.take(3).joinToString(", ") { appt ->
                val time = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(appt.startsAt),
                    ZoneId.systemDefault()
                )
                "${time.format(DateTimeFormatter.ofPattern("HH:mm"))} ${appt.clientName}"
            }
            "Записей: $count\n$firstFew"
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon for now
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for heads-up
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Explicit sound
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
        
        // Add SMS action button only if there are appointments
        if (appointments.isNotEmpty()) {
            builder.addAction(
                android.R.drawable.ic_menu_send,
                "Отправить SMS",
                smsPendingIntent
            )
        }
        
        // Check if notifications are enabled
        if (notificationManager.areNotificationsEnabled()) {
            try {
                val notification = builder.build()
                notificationManager.notify(NOTIFICATION_ID, notification)
                android.util.Log.d("NotificationHelper", "Notification sent successfully. ID: $NOTIFICATION_ID")
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Error showing notification", e)
            }
        } else {
            android.util.Log.e("NotificationHelper", "Notifications are disabled for this app!")
        }
    }
}
