package com.clientledger.app.ui.screen.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.navigation.ClientsViewModelFactory
import com.clientledger.app.ui.viewmodel.ClientsViewModel
import com.clientledger.app.util.DateUtils
import com.clientledger.app.util.MoneyUtils
import com.clientledger.app.util.PhoneUtils
import com.clientledger.app.util.TelegramUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    repository: LedgerRepository,
    viewModel: ClientsViewModel = viewModel(
        factory = ClientsViewModelFactory(repository)
    )
) {
    var client by remember { mutableStateOf<ClientEntity?>(null) }
    var appointmentsCount by remember { mutableStateOf(0) }
    var totalIncome by remember { mutableStateOf(0L) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clientId) {
        scope.launch {
            client = repository.getClientById(clientId)
            appointmentsCount = repository.getAppointmentsCountByClient(clientId)
            totalIncome = repository.getTotalIncomeByClient(clientId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Карточка клиента",
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        client?.let { clientData ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                ClientInfoCard(
                    client = clientData,
                    appointmentsCount = appointmentsCount,
                    totalIncome = totalIncome,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onEdit(clientId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Редактировать")
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Удалить клиента?") },
                    text = { Text("Вы уверены, что хотите удалить этого клиента?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.deleteClient(clientData)
                                    onBack()
                                }
                            }
                        ) {
                            Text("Удалить", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ClientInfoCard(
    client: ClientEntity,
    appointmentsCount: Int,
    totalIncome: Long,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Client name - primary element
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Имя",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${client.firstName} ${client.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Contact info section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Контактная информация",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                InfoRow("Пол", when (client.gender) {
                    "male" -> "Муж"
                    "female" -> "Жен"
                    else -> "Муж" // Fallback для старых данных
                })
            PhoneInfoRow(
                phone = client.phone,
                onCallClick = { phone ->
                    val success = PhoneUtils.openDialer(context, phone)
                    if (!success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Не удалось открыть приложение телефона")
                        }
                    }
                }
            )
            TelegramInfoRow(
                telegram = client.telegram,
                onTelegramClick = { username ->
                    val success = TelegramUtils.openTelegramChat(context, username)
                    if (!success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Не удалось открыть Telegram")
                        }
                    }
                }
            )
                client.notes?.let { InfoRow("Заметки", it) }
            }
            
            // Divider before statistics
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Statistics section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Статистика",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                InfoRow("Количество визитов", appointmentsCount.toString())
                InfoRow("Всего оплачено", MoneyUtils.formatCents(totalIncome))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Composable
fun PhoneInfoRow(
    phone: String?,
    onCallClick: (String) -> Unit
) {
    val isValidPhone = PhoneUtils.isValidPhoneForCall(phone)
    val accentColor = MaterialTheme.colorScheme.secondary
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Телефон",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = if (isValidPhone) {
                Modifier
                    .clickable { onCallClick(phone!!) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            } else {
                Modifier
            },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isValidPhone) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Позвонить",
                    modifier = Modifier.size(20.dp),
                    tint = accentColor
                )
            }
            Text(
                text = phone?.takeIf { it.isNotBlank() } ?: "Не указан",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isValidPhone) {
                    androidx.compose.ui.text.font.FontWeight.SemiBold
                } else {
                    androidx.compose.ui.text.font.FontWeight.Medium
                },
                color = if (isValidPhone) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textDecoration = if (isValidPhone) {
                    TextDecoration.Underline
                } else {
                    TextDecoration.None
                }
            )
        }
    }
}

@Composable
fun TelegramInfoRow(
    telegram: String?,
    onTelegramClick: (String) -> Unit
) {
    val isValidTelegram = TelegramUtils.isValidUsername(telegram)
    val displayText = telegram?.takeIf { it.isNotBlank() } ?: "Не указан"
    val accentColor = MaterialTheme.colorScheme.secondary
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Telegram",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = if (isValidTelegram) {
                Modifier
                    .clickable { onTelegramClick(telegram!!) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            } else {
                Modifier
            },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isValidTelegram) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = "Открыть чат в Telegram",
                    modifier = Modifier.size(20.dp),
                    tint = accentColor
                )
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isValidTelegram) {
                    androidx.compose.ui.text.font.FontWeight.SemiBold
                } else {
                    androidx.compose.ui.text.font.FontWeight.Medium
                },
                color = if (isValidTelegram) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textDecoration = if (isValidTelegram) {
                    TextDecoration.Underline
                } else {
                    TextDecoration.None
                }
            )
        }
    }
}


