package com.clientledger.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_preferences")

private val DAILY_REMINDER_ENABLED_KEY = booleanPreferencesKey("daily_reminder_enabled")

class ReminderPreferences(private val context: Context) {
    val dailyReminderEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DAILY_REMINDER_ENABLED_KEY] ?: false
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REMINDER_ENABLED_KEY] = enabled
        }
    }
}
