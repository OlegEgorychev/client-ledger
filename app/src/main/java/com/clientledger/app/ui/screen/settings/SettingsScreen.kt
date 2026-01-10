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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clientledger.app.LedgerApplication
import com.clientledger.app.data.backup.BackupRepository
import com.clientledger.app.data.backup.BackupScheduler
import com.clientledger.app.data.backup.GoogleDriveBackupService
import com.clientledger.app.data.preferences.AppPreferences
import com.clientledger.app.data.preferences.ThemePreferences
import com.clientledger.app.ui.theme.ThemeMode
import com.clientledger.app.util.ReminderScheduler
import com.clientledger.app.util.NotificationHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlinx.coroutines.flow.combine
import java.io.File
import android.net.Uri
import android.content.ContentResolver
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult

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
            
            // Section 3: Backup & Restore
            val app = context.applicationContext as? com.clientledger.app.LedgerApplication
            val backupScheduler = remember(app) { app?.backupScheduler }
            val repository = remember(app) { app?.repository }
            val googleDriveService = remember(app) { app?.googleDriveBackupService }
            
            var lastBackupTimestamp by remember { mutableStateOf<String?>(null) }
            var showRestoreConfirmDialog by remember { mutableStateOf(false) }
            var restoreError by remember { mutableStateOf<String?>(null) }
            var isBackingUp by remember { mutableStateOf(false) }
            var isRestoring by remember { mutableStateOf(false) }
            var isGoogleSignedIn by remember { mutableStateOf(false) }
            var googleAccountEmail by remember { mutableStateOf<String?>(null) }
            
            // Check Google sign-in status
            LaunchedEffect(googleDriveService) {
                googleDriveService?.let { service ->
                    isGoogleSignedIn = service.isSignedIn()
                    googleAccountEmail = service.getSignedInAccount()?.email
                }
            }
            
            // Google Sign-In launcher
            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                googleDriveService?.let { service ->
                    try {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        val account = task.getResult(ApiException::class.java)
                        isGoogleSignedIn = true
                        googleAccountEmail = account.email
                        Toast.makeText(context, "Вход выполнен: ${account.email}", Toast.LENGTH_SHORT).show()
                        
                        // Check if database is empty and offer to restore from Drive
                        scope.launch {
                            val isDatabaseEmpty = repository?.isDatabaseEmpty() == true
                            if (isDatabaseEmpty && repository != null && backupScheduler != null) {
                                // Check if there are backups in Drive
                                val backupsResult = service.listBackupsFromDrive()
                                backupsResult.onSuccess { backups ->
                                    if (backups.isNotEmpty()) {
                                        // Show dialog offering restore - will be handled by the restore function
                                        android.app.AlertDialog.Builder(context)
                                            .setTitle("Восстановить из Google Drive?")
                                            .setMessage("База данных пуста. Обнаружен бэкап в Google Drive (${backups[0].fileName}). Восстановить данные?")
                                            .setPositiveButton("Восстановить") { _, _ ->
                                                scope.launch {
                                                    // Download and restore
                                                    val downloadResult = service.downloadLatestBackupFromDrive()
                                                    downloadResult.onSuccess { backupFile ->
                                                        try {
                                                            val backupRepository = BackupRepository(context, repository)
                                                            val backupResult = backupRepository.readBackup(backupFile)
                                                            backupResult.onSuccess { payload ->
                                                                val restoreResult = backupRepository.restoreFromBackup(payload)
                                                                restoreResult.onSuccess {
                                                                    Toast.makeText(context, "Данные успешно восстановлены из Google Drive", Toast.LENGTH_SHORT).show()
                                                                    lastBackupTimestamp = backupScheduler.getLatestBackupTimestamp()
                                                                }.onFailure { error ->
                                                                    Toast.makeText(context, "Ошибка восстановления: ${error.message}", Toast.LENGTH_LONG).show()
                                                                }
                                                            }.onFailure { error ->
                                                                Toast.makeText(context, "Ошибка чтения бэкапа: ${error.message}", Toast.LENGTH_LONG).show()
                                                            }
                                                        } finally {
                                                            backupFile.delete()
                                                        }
                                                    }.onFailure { error ->
                                                        Toast.makeText(context, "Не удалось загрузить бэкап: ${error.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            .setNegativeButton("Отмена", null)
                                            .show()
                                    }
                                }
                            }
                        }
                    } catch (e: ApiException) {
                        isGoogleSignedIn = false
                        googleAccountEmail = null
                        val errorMessage = when (e.statusCode) {
                            ConnectionResult.NETWORK_ERROR -> "Ошибка сети"
                            12501 -> "Вход отменён" // GoogleSignInStatusCodes.SIGN_IN_CANCELLED
                            12500 -> "Ошибка входа" // GoogleSignInStatusCodes.SIGN_IN_FAILED
                            7 -> "Ошибка сети" // ConnectionResult.NETWORK_ERROR
                            8 -> "Ошибка внутренней службы" // ConnectionResult.INTERNAL_ERROR
                            else -> "Ошибка входа: ${e.message}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        isGoogleSignedIn = false
                        googleAccountEmail = null
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            // Load last backup timestamp
            LaunchedEffect(backupScheduler) {
                backupScheduler?.let {
                    lastBackupTimestamp = it.getLatestBackupTimestamp()
                }
            }
            
            // Observe last backup timestamp changes
            val lastBackupTimestampFlow = remember(backupScheduler) {
                backupScheduler?.lastBackupTimestamp
            }
            lastBackupTimestampFlow?.let { flow ->
                LaunchedEffect(flow) {
                    flow.collect { timestamp ->
                        lastBackupTimestamp = timestamp
                    }
                }
            }
            
            SettingsSectionCard(title = "Резервное копирование") {
                // Google Drive status
                googleDriveService?.let { service ->
                    if (isGoogleSignedIn) {
                        SettingsTextRow(
                            title = "Google Drive",
                            value = googleAccountEmail ?: "Авторизован"
                        )
                        
                        // Check backups in Drive button
                        var checkingBackups by remember { mutableStateOf(false) }
                        var backupsCount by remember { mutableStateOf<Int?>(null) }
                        
                        SettingsButtonRow(
                            title = "Проверить бэкапы в Drive",
                            description = if (backupsCount != null) "Найдено бэкапов: $backupsCount" else "Проверить наличие бэкапов в Google Drive",
                            buttonText = if (checkingBackups) "Проверка..." else "Проверить",
                            enabled = !checkingBackups && !isRestoring && !isBackingUp,
                            onClick = {
                                scope.launch {
                                    checkingBackups = true
                                    restoreError = null
                                    try {
                                        val backupsResult = service.listBackupsFromDrive()
                                        backupsResult.onSuccess { backups ->
                                            backupsCount = backups.size
                                            if (backups.isEmpty()) {
                                                restoreError = "Бэкапы в Google Drive не найдены. Убедитесь, что бэкапы загружаются успешно."
                                            } else {
                                                val latestBackup = backups[0]
                                                val dateStr = java.time.Instant.ofEpochMilli(latestBackup.createdTime)
                                                    .atZone(java.time.ZoneId.systemDefault())
                                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                                                Toast.makeText(context, "Найдено бэкапов: ${backups.size}. Последний: $dateStr", Toast.LENGTH_LONG).show()
                                                restoreError = null
                                            }
                                        }.onFailure { error ->
                                            restoreError = "Ошибка проверки: ${error.message}"
                                            backupsCount = null
                                        }
                                    } catch (e: Exception) {
                                        restoreError = "Ошибка: ${e.message}"
                                        backupsCount = null
                                    } finally {
                                        checkingBackups = false
                                    }
                                }
                            }
                        )
                        
                        SettingsButtonRow(
                            title = "Выйти из Google",
                            description = "Отключить автоматическую загрузку в Google Drive",
                            buttonText = "Выйти",
                            enabled = true,
                            onClick = {
                                scope.launch {
                                    service.signOut()
                                    isGoogleSignedIn = false
                                    googleAccountEmail = null
                                    backupsCount = null
                                    Toast.makeText(context, "Выход выполнен", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        val signInClient = service.getGoogleSignInClient()
                        SettingsButtonRow(
                            title = "Войти в Google Drive",
                            description = "Автоматически сохранять бэкапы в Google Drive. Требуется настройка OAuth Client ID в Google Cloud Console.",
                            buttonText = "Войти",
                            enabled = signInClient != null,
                            onClick = {
                                signInClient?.let {
                                    googleSignInLauncher.launch(it.signInIntent)
                                }
                            }
                        )
                    }
                }
                
                // Last backup timestamp
                lastBackupTimestamp?.let { timestamp ->
                    SettingsTextRow(
                        title = "Последний бэкап",
                        value = try {
                            java.time.Instant.parse(timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        } catch (e: Exception) {
                            timestamp
                        }
                    )
                }
                
                // Export backup button
                SettingsButtonRow(
                    title = "Экспорт бэкапа",
                    description = "Сохранить текущую копию данных для передачи на другое устройство",
                    buttonText = "Экспортировать",
                    enabled = !isBackingUp && repository != null && backupScheduler != null,
                    onClick = {
                        scope.launch {
                            isBackingUp = true
                            restoreError = null
                            try {
                                val result = backupScheduler?.performBackupNow()
                                result?.onSuccess { backupInfo ->
                                    try {
                                        // Share the backup file using FileProvider
                                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            backupInfo.file
                                        )
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Backup ${backupInfo.fileName}")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Экспортировать бэкап"))
                                        restoreError = null
                                    } catch (e: Exception) {
                                        restoreError = "Не удалось поделиться файлом: ${e.message}"
                                    }
                                }?.onFailure { error ->
                                    restoreError = "Не удалось создать бэкап: ${error.message}"
                                }
                            } catch (e: Exception) {
                                restoreError = "Ошибка: ${e.message}"
                            } finally {
                                isBackingUp = false
                            }
                        }
                    }
                )
                
                // Restore from Google Drive button (only if signed in)
                if (isGoogleSignedIn && googleDriveService != null && repository != null && backupScheduler != null) {
                    SettingsButtonRow(
                        title = "Восстановить из Google Drive",
                        description = "Загрузить последний бэкап из Google Drive (заменит текущие данные)",
                        buttonText = "Восстановить",
                        enabled = !isRestoring && !isBackingUp,
                        onClick = {
                            scope.launch {
                                isRestoring = true
                                restoreError = null
                                try {
                                    // Download latest backup from Drive
                                    val downloadResult = googleDriveService!!.downloadLatestBackupFromDrive()
                                    downloadResult.onSuccess { backupFile ->
                                        try {
                                            // Read backup from downloaded file
                                            val backupRepository = BackupRepository(context, repository!!)
                                            val backupResult = backupRepository.readBackup(backupFile)
                                            backupResult.onSuccess { payload ->
                                                // Confirm restore
                                                android.app.AlertDialog.Builder(context)
                                                    .setTitle("Восстановление данных")
                                                    .setMessage("Это действие заменит все текущие данные данными из бэкапа. Продолжить?")
                                                    .setPositiveButton("Восстановить") { _, _ ->
                                                        scope.launch {
                                                            val restoreResult = backupRepository.restoreFromBackup(payload)
                                                            restoreResult.onSuccess {
                                                                Toast.makeText(context, "Данные успешно восстановлены из Google Drive", Toast.LENGTH_SHORT).show()
                                                                restoreError = null
                                                                // Refresh backup timestamp
                                                                lastBackupTimestamp = backupScheduler!!.getLatestBackupTimestamp()
                                                            }.onFailure { error ->
                                                                restoreError = "Ошибка восстановления: ${error.message ?: "Неизвестная ошибка"}"
                                                            }
                                                            isRestoring = false
                                                        }
                                                    }
                                                    .setNegativeButton("Отмена") { _, _ ->
                                                        isRestoring = false
                                                    }
                                                    .setOnCancelListener {
                                                        isRestoring = false
                                                    }
                                                    .show()
                                            }.onFailure { error ->
                                                restoreError = "Не удалось прочитать бэкап: ${error.message ?: "Неизвестная ошибка"}"
                                                isRestoring = false
                                            }
                                        } finally {
                                            // Clean up downloaded file
                                            backupFile.delete()
                                        }
                                    }.onFailure { error ->
                                        restoreError = "Не удалось загрузить бэкап из Google Drive: ${error.message ?: "Неизвестная ошибка"}"
                                        isRestoring = false
                                    }
                                } catch (e: Exception) {
                                    restoreError = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                                    isRestoring = false
                                }
                            }
                        }
                    )
                }
                
                // Restore backup button
                SettingsButtonRow(
                    title = "Восстановить из файла",
                    description = "Загрузить данные из сохранённого бэкапа (заменит текущие данные)",
                    buttonText = "Восстановить",
                    enabled = !isRestoring && !isBackingUp && repository != null && backupScheduler != null,
                    onClick = {
                        showRestoreConfirmDialog = true
                    }
                )
                
                // Show error if any
                restoreError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // File picker launcher for restore
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null && repository != null && app != null) {
                    scope.launch {
                        isRestoring = true
                        restoreError = null
                        try {
                            // Read backup from selected file
                            val backupRepository = BackupRepository(context, repository)
                            val tempFile = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.json")
                            
                            try {
                                // Copy content from URI to temp file
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    tempFile.outputStream().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                } ?: throw IllegalStateException("Не удалось прочитать файл")
                                
                                // Read backup from temp file
                                val backupResult = backupRepository.readBackup(tempFile)
                                backupResult.onSuccess { payload ->
                                    // Perform restore
                                    val restoreResult = backupRepository.restoreFromBackup(payload)
                                    restoreResult.onSuccess {
                                        Toast.makeText(context, "Данные успешно восстановлены", Toast.LENGTH_SHORT).show()
                                        restoreError = null
                                        // Refresh backup timestamp
                                        lastBackupTimestamp = backupScheduler?.getLatestBackupTimestamp()
                                    }.onFailure { error ->
                                        restoreError = "Ошибка восстановления: ${error.message ?: "Неизвестная ошибка"}"
                                    }
                                }.onFailure { error ->
                                    restoreError = "Не удалось прочитать бэкап: ${error.message ?: "Неизвестная ошибка"}"
                                }
                            } finally {
                                // Clean up temp file
                                tempFile.delete()
                                isRestoring = false
                            }
                        } catch (e: Exception) {
                            restoreError = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                            isRestoring = false
                        }
                    }
                }
            }
            
            // Restore confirmation dialog
            if (showRestoreConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreConfirmDialog = false },
                    title = { Text("Восстановление данных") },
                    text = { Text("Это действие заменит все текущие данные данными из бэкапа. Продолжить?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRestoreConfirmDialog = false
                                // Launch file picker
                                filePickerLauncher.launch("application/json")
                            }
                        ) {
                            Text("Выбрать файл")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreConfirmDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
            
            // Section 4: Отладка (Debug) - only in debug builds
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
                    
                    // Manual backup trigger (debug only)
                    if (backupScheduler != null) {
                        SettingsButtonRow(
                            title = "Создать бэкап сейчас",
                            description = "DEBUG: Принудительно создать бэкап (для тестирования)",
                            buttonText = "Создать бэкап",
                            enabled = !isBackingUp,
                            onClick = {
                                scope.launch {
                                    isBackingUp = true
                                    restoreError = null
                                    try {
                                        val result = backupScheduler.performBackupNow()
                                        result.onSuccess {
                                            restoreError = "Бэкап создан успешно!"
                                        }.onFailure { error ->
                                            restoreError = "Ошибка: ${error.message}"
                                        }
                                    } catch (e: Exception) {
                                        restoreError = "Ошибка: ${e.message}"
                                    } finally {
                                        isBackingUp = false
                                    }
                                }
                            }
                        )
                        
                        // Show last backup timestamp (debug)
                        lastBackupTimestamp?.let { timestamp ->
                            SettingsTextRow(
                                title = "Время последнего бэкапа",
                                value = timestamp
                            )
                        }
                    }
                    
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
            
            // Section 5: О приложении (About) - Version at bottom
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