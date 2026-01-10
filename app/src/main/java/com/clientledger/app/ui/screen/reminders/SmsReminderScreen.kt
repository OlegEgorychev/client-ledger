package com.clientledger.app.ui.screen.reminders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.clientledger.app.data.dao.AppointmentWithClient
import com.clientledger.app.util.toDateKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.content.ActivityNotFoundException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsReminderScreen(
    appointments: List<AppointmentWithClient>,
    tomorrowDate: LocalDate,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var messageTemplate by remember {
        mutableStateOf(TextFieldValue("Напоминание: завтра в {time} у вас запись. До встречи!"))
    }
    
    // Map of appointment ID to selected state (default: all selected)
    var selectedAppointments by remember {
        mutableStateOf(appointments.map { it.appointmentId }.toSet())
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SMS напоминания") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            if (appointments.isNotEmpty()) {
                BottomAppBar {
                    Button(
                        onClick = {
                            val selected = appointments.filter { it.appointmentId in selectedAppointments }
                            android.util.Log.d("SmsReminderScreen", "Отправить нажата. Выбрано записей: ${selected.size}")
                            
                            var successCount = 0
                            var errorCount = 0
                            
                            selected.forEachIndexed { index, appointment ->
                                val phone = appointment.clientPhone
                                android.util.Log.d("SmsReminderScreen", "Обработка записи $index: ${appointment.clientName}, телефон: $phone")
                                
                                if (!phone.isNullOrBlank()) {
                                    // Нормализуем номер телефона (убираем все пробелы и дефисы)
                                    val normalizedPhone = phone.trim().replace("\\s+".toRegex(), "").replace("-", "")
                                    
                                    val time = LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(appointment.startsAt),
                                        ZoneId.systemDefault()
                                    )
                                    val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                                    val formattedDate = tomorrowDate.format(DateTimeFormatter.ofPattern("d MMMM", java.util.Locale("ru")))
                                    
                                    val message = messageTemplate.text
                                        .replace("{time}", formattedTime)
                                        .replace("{date}", formattedDate)
                                    
                                    android.util.Log.d("SmsReminderScreen", "Создание SMS Intent для $normalizedPhone (оригинал: $phone)")
                                    android.util.Log.d("SmsReminderScreen", "Сообщение: $message")
                                    
                                    try {
                                        // Способ 1: smsto: схема (стандартный способ)
                                        val uri = Uri.parse("smsto:$normalizedPhone")
                                        android.util.Log.d("SmsReminderScreen", "Способ 1 - URI: $uri")
                                        
                                        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                                            putExtra("sms_body", message)
                                        }
                                        
                                        android.util.Log.d("SmsReminderScreen", "Intent создан: action=${intent.action}, data=${intent.data}")
                                        
                                        // Проверяем наличие приложений для обработки SMS
                                        // Пробуем без MATCH_DEFAULT_ONLY и с ним
                                        val resolveList1 = context.packageManager.queryIntentActivities(intent, 0)
                                        val resolveList2 = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                                        android.util.Log.d("SmsReminderScreen", "Найдено приложений для SMS (без флагов): ${resolveList1.size}, (с MATCH_DEFAULT_ONLY): ${resolveList2.size}")
                                        
                                        var intentLaunched = false
                                        
                                        // Выводим все найденные приложения
                                        resolveList1.forEach { resolveInfo ->
                                            android.util.Log.d("SmsReminderScreen", "  - ${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}")
                                        }
                                        
                                        // Пытаемся запустить даже если список пуст - иногда это работает
                                        android.util.Log.d("SmsReminderScreen", "✅ Попытка запуска SMS приложения (способ 1) для $normalizedPhone")
                                        try {
                                            context.startActivity(intent)
                                            successCount++
                                            intentLaunched = true
                                            android.util.Log.d("SmsReminderScreen", "✅ startActivity вызван успешно!")
                                        } catch (e: ActivityNotFoundException) {
                                            android.util.Log.w("SmsReminderScreen", "⚠️ ActivityNotFoundException (способ 1): ${e.message}")
                                            // Продолжаем к следующему способу
                                        } catch (e: Exception) {
                                            android.util.Log.e("SmsReminderScreen", "❌ Другая ошибка при запуске (способ 1): ${e.message}", e)
                                        }
                                        
                                        // Способ 2: если первый способ не сработал, пробуем альтернативный (sms: схема)
                                        if (!intentLaunched) {
                                            android.util.Log.d("SmsReminderScreen", "Попытка способа 2: sms: схема")
                                            try {
                                                val alternativeIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("sms:$normalizedPhone")
                                                    putExtra("sms_body", message)
                                                }
                                                
                                                android.util.Log.d("SmsReminderScreen", "✅ Попытка запуска (способ 2)")
                                                context.startActivity(alternativeIntent)
                                                successCount++
                                                intentLaunched = true
                                                android.util.Log.d("SmsReminderScreen", "✅ Способ 2 сработал")
                                            } catch (e: ActivityNotFoundException) {
                                                android.util.Log.w("SmsReminderScreen", "⚠️ ActivityNotFoundException (способ 2): ${e.message}")
                                            } catch (e: Exception) {
                                                android.util.Log.e("SmsReminderScreen", "❌ Другая ошибка при способе 2: ${e.message}", e)
                                            }
                                        }
                                        
                                        // Способ 3: пытаемся через Intent.createChooser
                                        if (!intentLaunched) {
                                            android.util.Log.d("SmsReminderScreen", "Попытка способа 3: createChooser")
                                            try {
                                                val chooserIntent = Intent.createChooser(intent, "Выберите приложение для SMS")
                                                context.startActivity(chooserIntent)
                                                successCount++
                                                intentLaunched = true
                                                android.util.Log.d("SmsReminderScreen", "✅ Способ 3 (chooser) сработал")
                                            } catch (e: Exception) {
                                                android.util.Log.e("SmsReminderScreen", "❌ Ошибка при способе 3: ${e.message}", e)
                                            }
                                        }
                                        
                                        if (!intentLaunched) {
                                            android.util.Log.e("SmsReminderScreen", "❌ Все способы не сработали - не найдено приложение для отправки SMS")
                                            errorCount++
                                        }
                                    } catch (e: ActivityNotFoundException) {
                                        android.util.Log.e("SmsReminderScreen", "❌ ActivityNotFoundException: нет приложения для SMS", e)
                                        errorCount++
                                        e.printStackTrace()
                                    } catch (e: Exception) {
                                        android.util.Log.e("SmsReminderScreen", "❌ Неожиданная ошибка при запуске SMS", e)
                                        errorCount++
                                        e.printStackTrace()
                                    }
                                } else {
                                    android.util.Log.w("SmsReminderScreen", "Пропуск записи ${appointment.clientName}: нет телефона")
                                    errorCount++
                                }
                            }
                            
                            // Показываем результат пользователю
                            scope.launch {
                                if (successCount > 0) {
                                    snackbarHostState.showSnackbar(
                                        message = "Открыто SMS приложений: $successCount",
                                        duration = SnackbarDuration.Short
                                    )
                                } else if (errorCount > 0) {
                                    snackbarHostState.showSnackbar(
                                        message = "Ошибка: не удалось открыть SMS приложение",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = selectedAppointments.isNotEmpty()
                    ) {
                        Text("Отправить")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (appointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "На завтра нет записей",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Message template editor
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Шаблон сообщения",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Используйте {time} для времени, {date} для даты",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = messageTemplate,
                            onValueChange = { messageTemplate = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            label = { Text("Текст сообщения") }
                        )
                    }
                }
                
                // Appointments list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appointments) { appointment ->
                        val time = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(appointment.startsAt),
                            ZoneId.systemDefault()
                        )
                        val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                        val isSelected = appointment.appointmentId in selectedAppointments
                        val hasPhone = !appointment.clientPhone.isNullOrBlank()
                        
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appointment.clientName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$formattedTime - ${appointment.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (appointment.clientPhone != null) {
                                        Text(
                                            text = appointment.clientPhone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "Телефон не указан",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (hasPhone) {
                                            selectedAppointments = if (checked) {
                                                selectedAppointments + appointment.appointmentId
                                            } else {
                                                selectedAppointments - appointment.appointmentId
                                            }
                                        }
                                    },
                                    enabled = hasPhone
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
