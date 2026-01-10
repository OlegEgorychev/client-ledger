package com.clientledger.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

private val DAILY_REMINDER_ENABLED_KEY = booleanPreferencesKey("daily_reminder_enabled")
private val DEBUG_REMINDER_ENABLED_KEY = booleanPreferencesKey("debug_reminder_enabled")
private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")
private val BACKUP_COUNTER_KEY = intPreferencesKey("backup_counter")

class AppPreferences(private val context: Context) {
    val dailyReminderEnabled: Flow<Boolean> = context.appDataStore.data.map { preferences ->
        preferences[DAILY_REMINDER_ENABLED_KEY] ?: false // Default: OFF
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[DAILY_REMINDER_ENABLED_KEY] = enabled
        }
    }
    
    val debugReminderEnabled: Flow<Boolean> = context.appDataStore.data.map { preferences ->
        preferences[DEBUG_REMINDER_ENABLED_KEY] ?: false // Default: OFF
    }

    suspend fun setDebugReminderEnabled(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[DEBUG_REMINDER_ENABLED_KEY] = enabled
        }
    }
    
    // Reminder time (default: 21:00)
    val reminderHour: Flow<Int> = context.appDataStore.data.map { preferences ->
        preferences[REMINDER_HOUR_KEY] ?: 21 // Default: 21:00
    }
    
    val reminderMinute: Flow<Int> = context.appDataStore.data.map { preferences ->
        preferences[REMINDER_MINUTE_KEY] ?: 0 // Default: 0 minutes
    }
    
    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.appDataStore.edit { preferences ->
            preferences[REMINDER_HOUR_KEY] = hour.coerceIn(0, 23)
            preferences[REMINDER_MINUTE_KEY] = minute.coerceIn(0, 59)
        }
    }
    
    // Backup counter for numbering backups
    suspend fun getNextBackupNumber(): Int {
        var nextNumber = 0
        context.appDataStore.edit { preferences ->
            val current = preferences[BACKUP_COUNTER_KEY] ?: 0
            nextNumber = current + 1
            preferences[BACKUP_COUNTER_KEY] = nextNumber
        }
        return nextNumber
    }
    
    suspend fun getCurrentBackupNumber(): Int {
        return context.appDataStore.data.map { preferences ->
            preferences[BACKUP_COUNTER_KEY] ?: 0
        }.first()
    }
}
