package com.clientledger.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.clientledger.app.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Calendar

/**
 * Компонент для выбора даты рождения через DatePicker
 * 
 * @param value Дата в формате yyyy-MM-dd или пустая строка
 * @param onValueChange Callback с датой в формате yyyy-MM-dd или пустой строкой
 * @param label Метка поля
 * @param isRequired Обязательно ли поле
 * @param isError Показывать ли ошибку
 * @param supportingText Текст поддержки (для ошибок)
 * @param modifier Modifier для поля
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Дата рождения",
    isRequired: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    // Получаем context в composable контексте
    val context = LocalContext.current
    
    // Парсим текущую дату
    val currentDate = remember(value) {
        try {
            if (value.isNotBlank()) {
                LocalDate.parse(value)
            } else {
                null
            }
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    // Отображаемое значение в формате dd MM yyyy
    val displayValue = remember(value) {
        if (value.isNotBlank()) {
            DateUtils.formatBirthDate(value)
        } else {
            ""
        }
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { /* Read-only */ },
            label = { Text(if (isRequired) "$label *" else label) },
            placeholder = { Text("дд мм гггг") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Выбрать дату",
                    tint = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить дату",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    null
                }
            }
        )
        // Накладываем прозрачный clickable слой поверх поля
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .clickable {
                    showDatePickerDialog(context, value, onValueChange)
                }
        )
    }
}

/**
 * Вспомогательная функция для открытия DatePickerDialog
 * Вынесена отдельно, чтобы избежать проблем с composable контекстом
 */
private fun showDatePickerDialog(
    context: android.content.Context,
    value: String,
    onValueChange: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    
    // Парсим дату напрямую из value
    try {
        if (value.isNotBlank()) {
            val date = LocalDate.parse(value)
            calendar.set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
    } catch (e: Exception) {
        // Используем текущую дату по умолчанию
    }
    
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            onValueChange(selectedDate.toString()) // yyyy-MM-dd
        },
        year,
        month,
        day
    )
    
    // Устанавливаем максимальную дату на сегодня (нельзя выбрать будущее)
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
    
    datePickerDialog.show()
}


