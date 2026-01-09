package com.clientledger.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clientledger.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class ThemePreferences(private val context: Context) {
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.DARK // Default to dark
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}


