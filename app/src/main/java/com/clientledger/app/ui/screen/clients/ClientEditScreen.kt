package com.clientledger.app.ui.screen.clients

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var telegram by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId) {
        if (clientId != null) {
            scope.launch {
                val client = repository.getClientById(clientId)
                client?.let {
                    firstName = it.firstName
                    lastName = it.lastName
                    gender = it.gender
                    birthDate = it.birthDate ?: ""
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Имя *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Фамилия *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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

            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Дата рождения (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("2020-01-01") }
            )

            // Валидация телефона
            val (isPhoneValid, phoneValidationError) = remember(phone) {
                validatePhoneNumber(phone)
            }

            PhoneInput(
                value = phone,
                onValueChange = { 
                    phone = it
                    phoneError = null // Сбрасываем ошибку при изменении
                },
                isError = !isPhoneValid && phone.isNotBlank(),
                supportingText = if (!isPhoneValid && phone.isNotBlank()) {
                    phoneValidationError ?: "Введите корректный номер телефона"
                } else {
                    phoneError
                },
                onPasteError = { error ->
                    phoneError = error
                },
                modifier = Modifier.fillMaxWidth()
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

            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank()) {
                        error = "Заполните обязательные поля"
                        return@Button
                    }

                    // Валидация телефона
                    val (isPhoneValid, phoneValidationError) = validatePhoneNumber(phone)
                    if (!isPhoneValid && phone.isNotBlank()) {
                        phoneError = phoneValidationError ?: "Введите корректный номер телефона"
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        error = null
                        phoneError = null

                        try {
                            // Проверка уникальности телефона (только если телефон указан)
                            if (phone.isNotBlank()) {
                                val existing = repository.getClientByPhone(phone)
                                if (existing != null && existing.id != clientId) {
                                    phoneError = "Клиент с таким телефоном уже существует"
                                    isLoading = false
                                    return@launch
                                }
                            }

                            val now = System.currentTimeMillis()
                            val client = if (clientId == null) {
                                ClientEntity(
                                    firstName = firstName,
                                    lastName = lastName,
                                    gender = gender,
                                    birthDate = birthDate.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    telegram = telegram.ifBlank { null },
                                    notes = notes.ifBlank { null },
                                    createdAt = now,
                                    updatedAt = now
                                )
                            } else {
                                ClientEntity(
                                    id = clientId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    gender = gender,
                                    birthDate = birthDate.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    telegram = telegram.ifBlank { null },
                                    notes = notes.ifBlank { null },
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
                enabled = !isLoading && (phone.isBlank() || validatePhoneNumber(phone).first)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}


