package com.clientledger.app.ui.screen.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.components.PhoneInput
import com.clientledger.app.ui.components.validatePhoneNumber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    clientId: Long?,
    repository: LedgerRepository,
    onBack: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var telegram by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Validation states
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneValidationError by remember { mutableStateOf<String?>(null) }
    
    // Focus requesters for scrolling to invalid fields
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId) {
        if (clientId != null) {
            scope.launch {
                val client = repository.getClientById(clientId)
                client?.let {
                    firstName = it.firstName
                    lastName = it.lastName
                    gender = it.gender
                    phone = it.phone ?: ""
                    telegram = it.telegram ?: ""
                    notes = it.notes ?: ""
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (clientId == null) "Новый клиент" else "Редактировать клиента") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { 
                    firstName = it
                    nameError = null // Clear error on change
                },
                label = { Text("Имя *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(firstNameFocusRequester),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { 
                    lastName = it
                    nameError = null // Clear error on change
                },
                label = { Text("Фамилия *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(lastNameFocusRequester),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } }
            )

            // Пол
            Text("Пол")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val genders = listOf("male" to "Муж", "female" to "Жен")
                genders.forEach { (value, label) ->
                    FilterChip(
                        selected = gender == value,
                        onClick = { gender = value },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Валидация телефона (required field)
            val (isPhoneFormatValid, phoneFormatError) = remember(phone) {
                if (phone.isBlank()) {
                    Pair(false, "Телефон обязателен для заполнения")
                } else {
                    validatePhoneNumber(phone)
                }
            }
            
            val phoneIsValid = phone.isNotBlank() && isPhoneFormatValid
            val phoneErrorText = when {
                phone.isBlank() -> "Телефон обязателен для заполнения"
                !isPhoneFormatValid -> phoneFormatError ?: "Введите корректный номер телефона"
                phoneError != null -> phoneError
                else -> null
            }

            PhoneInput(
                value = phone,
                onValueChange = { 
                    phone = it
                    phoneError = null // Сбрасываем ошибку при изменении
                    phoneValidationError = null
                },
                isError = !phoneIsValid || phoneValidationError != null,
                supportingText = phoneErrorText,
                onPasteError = { error ->
                    phoneError = error
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneFocusRequester)
            )

            OutlinedTextField(
                value = telegram,
                onValueChange = { telegram = it },
                label = { Text("Telegram") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("@username") }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Bottom padding to ensure Save button is always reachable
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    // Reset errors
                    nameError = null
                    phoneValidationError = null
                    error = null
                    
                    // Validate Name (firstName + lastName combined, after trimming)
                    val trimmedFirstName = firstName.trim()
                    val trimmedLastName = lastName.trim()
                    val fullName = "$trimmedFirstName $trimmedLastName".trim()
                    
                    if (fullName.isBlank()) {
                        nameError = "Имя и фамилия обязательны для заполнения"
                        // Focus on first name field and scroll to it
                        firstNameFocusRequester.requestFocus()
                        scope.launch {
                            kotlinx.coroutines.delay(100) // Small delay to allow focus
                            scrollState.scrollTo(0)
                        }
                        return@Button
                    }
                    
                    // Validate Phone (required and must be valid format)
                    if (phone.isBlank()) {
                        phoneValidationError = "Телефон обязателен для заполнения"
                        phoneFocusRequester.requestFocus()
                        scope.launch {
                            kotlinx.coroutines.delay(100) // Small delay to allow focus
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                        return@Button
                    }
                    
                    val (isPhoneValid, phoneFormatError) = validatePhoneNumber(phone)
                    if (!isPhoneValid) {
                        phoneValidationError = phoneFormatError ?: "Введите корректный номер телефона"
                        phoneFocusRequester.requestFocus()
                        scope.launch {
                            kotlinx.coroutines.delay(100) // Small delay to allow focus
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        error = null
                        phoneError = null

                        try {
                            // Проверка уникальности телефона
                            val existing = repository.getClientByPhone(phone)
                            if (existing != null && existing.id != clientId) {
                                phoneError = "Клиент с таким телефоном уже существует"
                                phoneValidationError = "Клиент с таким телефоном уже существует"
                                isLoading = false
                                return@launch
                            }

                            val now = System.currentTimeMillis()
                            // Use trimmed names
                            val trimmedFirstName = firstName.trim()
                            val trimmedLastName = lastName.trim()
                            
                            val client = if (clientId == null) {
                                ClientEntity(
                                    firstName = trimmedFirstName,
                                    lastName = trimmedLastName,
                                    gender = gender,
                                    birthDate = null,
                                    phone = phone.trim().ifBlank { null }, // Phone is required, but keep nullable for consistency
                                    telegram = telegram.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null },
                                    createdAt = now,
                                    updatedAt = now
                                )
                            } else {
                                ClientEntity(
                                    id = clientId,
                                    firstName = trimmedFirstName,
                                    lastName = trimmedLastName,
                                    gender = gender,
                                    birthDate = null,
                                    phone = phone.trim().ifBlank { null },
                                    telegram = telegram.trim().ifBlank { null },
                                    notes = notes.trim().ifBlank { null },
                                    createdAt = 0, // Будет проигнорировано при обновлении
                                    updatedAt = now
                                )
                            }

                            if (clientId == null) {
                                repository.insertClient(client)
                            } else {
                                repository.updateClient(client)
                            }
                            onBack()
                        } catch (e: Exception) {
                            error = e.message ?: "Ошибка сохранения"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && run {
                    // Enable Save button only when Name and Phone are valid
                    val trimmedFirstName = firstName.trim()
                    val trimmedLastName = lastName.trim()
                    val fullName = "$trimmedFirstName $trimmedLastName".trim()
                    val nameIsValid = fullName.isNotBlank()
                    val phoneIsValid = phone.isNotBlank() && validatePhoneNumber(phone).first
                    nameIsValid && phoneIsValid
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Сохранить")
                }
            }
            
            // Extra bottom padding for better accessibility with large font scales
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


