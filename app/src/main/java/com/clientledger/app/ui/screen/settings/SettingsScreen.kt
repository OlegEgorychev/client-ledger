package com.clientledger.app.ui.screen.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clientledger.app.data.preferences.AppPreferences
import com.clientledger.app.data.preferences.ThemePreferences
import com.clientledger.app.ui.theme.ThemeMode
import com.clientledger.app.util.ReminderScheduler
import com.clientledger.app.util.NotificationHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    themePreferences: ThemePreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // State for notification toggles
    val dailyReminderEnabled by appPreferences.dailyReminderEnabled.collectAsStateWithLifecycle(initialValue = false)
    val debugReminderEnabled by appPreferences.debugReminderEnabled.collectAsStateWithLifecycle(initialValue = false)
    val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
    
    // State for reminder time
    val reminderHour by appPreferences.reminderHour.collectAsStateWithLifecycle(initialValue = 21)
    val reminderMinute by appPreferences.reminderMinute.collectAsStateWithLifecycle(initialValue = 0)
    
    // Local state for time pickers
    var showHourMenu by remember { mutableStateOf(false) }
    var showMinuteMenu by remember { mutableStateOf(false) }
    
    // Generate hour and minute lists
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }
    
    // Get app version
    val versionName = remember {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo?.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Внешний вид (Appearance)
            SettingsSectionCard(title = "Внешний вид") {
                // Theme selector
                ThemeSelectorRow(
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        scope.launch {
                            themePreferences.setThemeMode(newMode)
                        }
                    }
                )
            }
            
            // Section 2: Уведомления (Notifications)
            SettingsSectionCard(title = "Уведомления") {
                // Main user-facing toggle for reminder
                SettingsSwitchRow(
                    title = "Уведомления о завтрашнем расписании",
                    description = "Получать ежедневное напоминание с расписанием на следующий день",
                    checked = dailyReminderEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            appPreferences.setDailyReminderEnabled(enabled)
                            ReminderScheduler.scheduleDailyReminder(
                                context = context,
                                enabled = enabled,
                                hour = reminderHour,
                                minute = reminderMinute
                            )
                        }
                    }
                )
                
                // Time picker for reminder (shown when enabled)
                if (dailyReminderEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Время уведомления",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hour picker
                        ExposedDropdownMenuBox(
                            expanded = showHourMenu,
                            onExpandedChange = { showHourMenu = !showHourMenu },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = String.format("%02d", reminderHour),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Час") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showHourMenu) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showHourMenu,
                                onDismissRequest = { showHourMenu = false }
                            ) {
                                hours.forEach { hour ->
                                    DropdownMenuItem(
                                        text = { Text(String.format("%02d", hour)) },
                                        onClick = {
                                            scope.launch {
                                                appPreferences.setReminderTime(hour, reminderMinute)
                                                // Reschedule reminder with new time if enabled
                                                if (dailyReminderEnabled) {
                                                    ReminderScheduler.scheduleDailyReminder(
                                                        context = context,
                                                        enabled = true,
                                                        hour = hour,
                                                        minute = reminderMinute
                                                    )
                                                }
                                            }
                                            showHourMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Minute picker
                        ExposedDropdownMenuBox(
                            expanded = showMinuteMenu,
                            onExpandedChange = { showMinuteMenu = !showMinuteMenu },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = String.format("%02d", reminderMinute),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Минуты") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMinuteMenu) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showMinuteMenu,
                                onDismissRequest = { showMinuteMenu = false }
                            ) {
                                minutes.forEach { minute ->
                                    DropdownMenuItem(
                                        text = { Text(String.format("%02d", minute)) },
                                        onClick = {
                                            scope.launch {
                                                appPreferences.setReminderTime(reminderHour, minute)
                                                // Reschedule reminder with new time if enabled
                                                if (dailyReminderEnabled) {
                                                    ReminderScheduler.scheduleDailyReminder(
                                                        context = context,
                                                        enabled = true,
                                                        hour = reminderHour,
                                                        minute = minute
                                                    )
                                                }
                                            }
                                            showMinuteMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Section 3: Отладка (Debug) - only in debug builds
            val isDebug = remember {
                try {
                    val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
                    (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                } catch (e: Exception) {
                    false
                }
            }
            
            if (isDebug) {
                SettingsSectionCard(title = "Отладка") {
                    // Test notification button
                    SettingsButtonRow(
                        title = "Тестирование уведомлений",
                        description = "Показать тестовое уведомление для проверки работы",
                        buttonText = "Показать тестовое уведомление",
                        onClick = {
                            NotificationHelper.createNotificationChannel(context)
                            NotificationHelper.showDailyReminder(
                                context = context,
                                appointments = emptyList(),
                                tomorrowDate = LocalDate.now().plusDays(1)
                            )
                        }
                    )
                    
                    // Debug daily reminder toggle (separate from production toggle)
                    SettingsSwitchRow(
                        title = "Ежедневные напоминания о завтрашнем расписании",
                        description = "DEBUG: Уведомления приходят каждые 3 минуты для тестирования",
                        checked = debugReminderEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                appPreferences.setDebugReminderEnabled(enabled)
                                ReminderScheduler.scheduleDebugReminder(
                                    context = context,
                                    enabled = enabled
                                )
                            }
                        },
                        debugCaption = true
                    )
                }
            }
            
            // Section 4: О приложении (About) - Version at bottom
            SettingsSectionCard(title = "О приложении") {
                SettingsTextRow(
                    title = "Версия",
                    value = versionName
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Section title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Section content
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    debugCaption: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = if (debugCaption) FontStyle.Italic else FontStyle.Normal
                )
            }
            if (debugCaption) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "для тестов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SettingsButtonRow(
    title: String,
    description: String? = null,
    buttonText: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonText)
        }
    }
}

@Composable
private fun SettingsTextRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeSelectorRow(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Тема",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Light theme option
            FilterChip(
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                label = { Text("Светлая") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            )
            
            // Dark theme option
            FilterChip(
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) },
                label = { Text("Тёмная") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            )
        }
    }
}