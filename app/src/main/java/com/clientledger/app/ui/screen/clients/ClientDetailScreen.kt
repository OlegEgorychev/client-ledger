package com.clientledger.app.ui.screen.clients

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch
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
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
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
                    totalIncome = totalIncome
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
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
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
    totalIncome: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow("Имя", client.firstName)
            InfoRow("Фамилия", client.lastName)
            InfoRow("Пол", when (client.gender) {
                "male" -> "Муж"
                "female" -> "Жен"
                else -> "Муж" // Fallback для старых данных
            })
            client.birthDate?.let { 
                InfoRow("Дата рождения", DateUtils.formatDateShort(it))
            }
            InfoRow("Телефон", client.phone?.takeIf { it.isNotBlank() } ?: "Не указан")
            client.telegram?.let { InfoRow("Telegram", it) }
            client.notes?.let { InfoRow("Заметки", it) }
            
            // Разделитель
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Статистика
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            InfoRow("Количество визитов", appointmentsCount.toString())
            InfoRow("Всего оплачено", MoneyUtils.formatCents(totalIncome))
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


