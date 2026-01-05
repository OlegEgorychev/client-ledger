package com.clientledger.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clientledger.app.util.Countries
import com.clientledger.app.util.Country

/**
 * Компонент для ввода телефонного номера с валидацией и форматированием
 * 
 * @param value Текущее значение телефона в формате E.164 (например, "+79161234567")
 * @param onValueChange Callback при изменении значения
 * @param isError Показывать ли ошибку
 * @param supportingText Текст ошибки или подсказки
 * @param enabled Включен ли компонент
 * @param modifier Modifier для компонента
 * @param onPasteError Callback при ошибке вставки (опционально)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInput(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onPasteError: ((String) -> Unit)? = null
) {
    var pasteError by remember { mutableStateOf<String?>(null) }
    // Инициализация: парсим существующий номер или используем дефолт
    val initialCountry = remember(value) {
        if (value.isBlank()) {
            Countries.defaultCountry
        } else {
            Countries.findCountryByPhoneNumber(value) ?: Countries.defaultCountry
        }
    }
    
    val initialLocalNumber = remember(value) {
        if (value.isBlank()) {
            ""
        } else {
            val country = Countries.findCountryByPhoneNumber(value) ?: Countries.defaultCountry
            Countries.extractNumberWithoutCode(value, country)
        }
    }

    var selectedCountry by remember { mutableStateOf(initialCountry) }
    var expanded by remember { mutableStateOf(false) }
    var localNumber by remember { mutableStateOf(initialLocalNumber) }

    // Синхронизируем с внешним value только если оно изменилось извне
    LaunchedEffect(value) {
        if (value.isBlank()) {
            if (localNumber.isNotBlank() || selectedCountry != Countries.defaultCountry) {
                selectedCountry = Countries.defaultCountry
                localNumber = ""
            }
        } else {
            val country = Countries.findCountryByPhoneNumber(value) ?: Countries.defaultCountry
            val numberWithoutCode = Countries.extractNumberWithoutCode(value, country)
            val currentFullNumber = if (localNumber.isNotBlank()) {
                "${selectedCountry.code}${localNumber}"
            } else {
                ""
            }
            
            // Обновляем только если внешнее значение отличается от текущего
            if (value != currentFullNumber) {
                selectedCountry = country
                localNumber = numberWithoutCode
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val numberTextFieldInteractionSource = remember { MutableInteractionSource() }
    
    // Определяем цвет рамки
    val borderColor = if (isError || pasteError != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }
    
    Column(modifier = modifier) {
        // Единый контейнер для телефона
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp)
                ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Блок выбора страны (слева)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.width(120.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .menuAnchor()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { expanded = !expanded }
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedCountry.flag,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = selectedCountry.code,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Выбрать страну",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Countries.allCountries.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(country.flag, style = MaterialTheme.typography.bodyLarge)
                                        Text(country.name)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            country.code,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCountry = country
                                    expanded = false
                                    // При смене страны очищаем номер
                                    localNumber = ""
                                    // Обновляем полный номер (пустой)
                                    onValueChange("")
                                }
                            )
                        }
                    }
                }
                
                // Разделитель
                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // Поле ввода номера (справа)
                TextField(
                    value = localNumber,
                    onValueChange = { newValue ->
                    // Проверяем, не является ли это вставкой
                    // Вставка определяется как: большое изменение длины или наличие нецифровых символов (кроме уже введенных)
                    val containsNonDigits = newValue.any { !it.isDigit() }
                    val lengthDiff = newValue.length - localNumber.length
                    val isPaste = lengthDiff > 1 || (containsNonDigits && newValue.length > localNumber.length)
                    
                    if (isPaste) {
                        // Обработка вставки - обрабатываем весь текст
                        val (processedNumber, errorMessage) = processPastedPhoneNumber(newValue, selectedCountry)
                        
                        if (errorMessage != null) {
                            // Ошибка при вставке - не применяем, но показываем ошибку
                            pasteError = errorMessage
                            onPasteError?.invoke(errorMessage)
                            return@TextField
                        } else {
                            pasteError = null
                        }
                        
                        if (processedNumber != null) {
                            // Парсим обработанный номер
                            val country = Countries.findCountryByPhoneNumber(processedNumber) ?: selectedCountry
                            val numberWithoutCode = Countries.extractNumberWithoutCode(processedNumber, country)
                            
                            // Проверяем валидность
                            val (isValid, _) = validatePhoneNumber(processedNumber)
                            if (isValid || processedNumber.isBlank()) {
                                selectedCountry = country
                                localNumber = numberWithoutCode
                                onValueChange(processedNumber)
                            } else {
                                // Номер невалиден - не применяем
                                return@TextField
                            }
                        } else {
                            // Не удалось обработать - не применяем
                            return@TextField
                        }
                        return@TextField
                    }
                    
                    // Обычный ввод - разрешаем только цифры
                    val digitsOnly = newValue.filter { it.isDigit() }
                    
                    // Сбрасываем ошибку вставки при обычном вводе
                    if (pasteError != null) {
                        pasteError = null
                    }
                    
                    // Для России: проверяем, что первый символ - '9'
                    if (selectedCountry.code == "+7" && digitsOnly.isNotEmpty()) {
                        if (digitsOnly.first() != '9') {
                            // Игнорируем ввод, если не начинается с 9
                            return@TextField
                        }
                    }
                    
                    // Ограничиваем длину
                    val maxLength = selectedCountry.maxLength
                    if (digitsOnly.length <= maxLength) {
                        localNumber = digitsOnly
                        // Обновляем полный номер
                        val fullNumber = if (digitsOnly.isNotBlank()) {
                            "${selectedCountry.code}${digitsOnly}"
                        } else {
                            ""
                        }
                        onValueChange(fullNumber)
                    }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(focusRequester)
                        .padding(horizontal = 4.dp),
                    placeholder = { 
                        Text(
                            text = "9123456789",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    interactionSource = numberTextFieldInteractionSource
                )
            }
        }
        
        // Место для ошибки (всегда зарезервировано)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val errorText = pasteError ?: supportingText
            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp)
                )
            } else {
                // Пустое место для стабильности
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Обработка вставки телефонного номера с преобразованием форматов
 * @param pastedText Вставленный текст
 * @param country Текущая выбранная страна
 * @return Pair<String?, String?> где первый элемент - обработанный номер в формате E.164, второй - сообщение об ошибке
 */
fun processPastedPhoneNumber(pastedText: String, country: Country): Pair<String?, String?> {
    if (pastedText.isBlank()) {
        return Pair(null, null)
    }

    // Убираем все нецифровые символы кроме +
    val cleaned = pastedText.replace(Regex("[^+0-9]"), "")
    
    // Для России (+7) - проверяем и преобразуем
    if (country.code == "+7") {
        // Вариант 1: +79161234567 (уже правильный формат E.164)
        if (cleaned.startsWith("+7") && cleaned.length == 12) {
            val numberWithoutCode = cleaned.substring(2)
            if (numberWithoutCode.firstOrNull() == '9' && numberWithoutCode.length == 10) {
                return Pair(cleaned, null)
            } else {
                return Pair(null, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
            }
        }
        
        // Вариант 2: 89161234567 → +79161234567
        if (cleaned.startsWith("8") && cleaned.length == 11) {
            val numberWithout8 = cleaned.substring(1)
            if (numberWithout8.firstOrNull() == '9' && numberWithout8.length == 10) {
                return Pair("+7$numberWithout8", null)
            } else {
                return Pair(null, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
            }
        }
        
        // Вариант 3: 9161234567 → +79161234567 (10 цифр, начинается с 9)
        if (cleaned.startsWith("9") && cleaned.length == 10 && !cleaned.contains("+")) {
            return Pair("+7$cleaned", null)
        }
        
        // Вариант 4: 7495... или 8495... (не мобильный) - отклоняем
        if (cleaned.startsWith("7") && cleaned.length == 11 && !cleaned.contains("+")) {
            val numberWithout7 = cleaned.substring(1)
            if (numberWithout7.firstOrNull() != '9') {
                return Pair(null, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
            }
            // Если начинается с 9, преобразуем
            return Pair("+7$numberWithout7", null)
        }
        
        // Вариант 5: +7495... или +7849... (не мобильный) - отклоняем
        if (cleaned.startsWith("+7") && cleaned.length >= 12) {
            val numberWithoutCode = cleaned.substring(2)
            if (numberWithoutCode.firstOrNull() != '9') {
                return Pair(null, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
            }
            // Если начинается с 9, но длина неправильная
            if (numberWithoutCode.length != 10) {
                return Pair(null, "Введите корректный номер телефона")
            }
            return Pair(cleaned, null)
        }
        
        // Если не соответствует ни одному формату, но содержит +7
        if (cleaned.contains("+7")) {
            val afterPlus7 = cleaned.substring(cleaned.indexOf("+7") + 2)
            if (afterPlus7.isNotEmpty() && afterPlus7.firstOrNull() != '9') {
                return Pair(null, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
            }
        }
        
        // Если просто цифры без префикса, но не начинается с 9
        if (!cleaned.contains("+") && !cleaned.startsWith("8") && !cleaned.startsWith("7") && cleaned.isNotEmpty()) {
            if (cleaned.firstOrNull() != '9') {
                return Pair(null, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
            }
            // Если начинается с 9 и длина 10
            if (cleaned.length == 10) {
                return Pair("+7$cleaned", null)
            }
        }
    }
    
    // Для других стран - просто очищаем и добавляем код
    if (cleaned.startsWith("+")) {
        return Pair(cleaned, null)
    }
    
    // Если нет +, добавляем код страны
    return Pair("${country.code}$cleaned", null)
}

/**
 * Валидация телефонного номера
 * @param phoneNumber Номер в формате E.164
 * @return Pair<Boolean, String?> где Boolean - валиден ли номер, String - сообщение об ошибке
 */
fun validatePhoneNumber(phoneNumber: String?): Pair<Boolean, String?> {
    if (phoneNumber.isNullOrBlank()) {
        return Pair(true, null) // Пустой номер допустим (не обязательное поле)
    }

    if (!phoneNumber.startsWith("+")) {
        return Pair(false, "Номер должен начинаться с '+'")
    }

    val country = Countries.findCountryByPhoneNumber(phoneNumber)
    if (country == null) {
        return Pair(false, "Неизвестный код страны")
    }

    val numberWithoutCode = Countries.extractNumberWithoutCode(phoneNumber, country)
    
    // Специальная валидация для России - только мобильные номера
    if (country.code == "+7") {
        if (numberWithoutCode.isEmpty()) {
            return Pair(false, "Введите корректный номер телефона")
        }
        
        // Проверка: первый символ после +7 должен быть '9'
        if (numberWithoutCode.firstOrNull() != '9') {
            return Pair(false, "Введите мобильный номер РФ: +7 9XXXXXXXXX")
        }
        
        // Проверка длины: должно быть ровно 10 цифр
        if (numberWithoutCode.length != 10) {
            return Pair(false, "Введите корректный номер телефона")
        }
    } else {
        // Для других стран - стандартная валидация
        if (numberWithoutCode.length < country.minLength) {
            return Pair(false, "Номер слишком короткий. Минимум ${country.minLength} цифр")
        }

        if (numberWithoutCode.length > country.maxLength) {
            return Pair(false, "Номер слишком длинный. Максимум ${country.maxLength} цифр")
        }
    }

    // Проверяем, что после кода страны только цифры
    if (!numberWithoutCode.all { it.isDigit() }) {
        return Pair(false, "Номер должен содержать только цифры")
    }

    return Pair(true, null)
}

