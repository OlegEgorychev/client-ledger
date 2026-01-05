package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.repository.LedgerRepository
import com.clientledger.app.ui.viewmodel.CalendarViewModel
import com.clientledger.app.util.*
import com.clientledger.app.util.MoneyUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditScreen(
    expenseId: Long?,
    date: LocalDate,
    repository: LedgerRepository,
    viewModel: CalendarViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(12) }
    var minute by remember { mutableStateOf(0) }
    var amountRubles by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            scope.launch {
                val expense = repository.getExpenseById(expenseId)
                expense?.let {
                    title = it.title
                    val dateTime = DateUtils.dateTimeToLocalDateTime(it.spentAt)
                    hour = dateTime.hour
                    minute = dateTime.minute
                    amountRubles = MoneyUtils.centsToRubles(it.amountCents).toString()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == null) "Новый расход" else "Редактировать расход") },
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Название *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Такси") }
            )

            // Время
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hour.toString().padStart(2, '0'),
                    onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) hour = h } },
                    label = { Text("Часы") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minute.toString().padStart(2, '0'),
                    onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) minute = m } },
                    label = { Text("Минуты") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = amountRubles,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == ',' }) amountRubles = it },
                label = { Text("Сумма (рубли) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("500") }
            )

            Button(
                onClick = {
                    if (title.isBlank() || amountRubles.isBlank()) {
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        val amountCents = MoneyUtils.rublesToCents(amountRubles.replace(',', '.').toDoubleOrNull() ?: 0.0)
                        val spentAt = date.atTime(LocalTime.of(hour, minute)).toMillis()
                        
                        val expense = if (expenseId == null) {
                            ExpenseEntity(
                                title = title,
                                spentAt = spentAt,
                                dateKey = date.toDateKey(),
                                amountCents = amountCents,
                                createdAt = System.currentTimeMillis()
                            )
                        } else {
                            ExpenseEntity(
                                id = expenseId,
                                title = title,
                                spentAt = spentAt,
                                dateKey = date.toDateKey(),
                                amountCents = amountCents,
                                createdAt = System.currentTimeMillis()
                            )
                        }

                        if (expenseId == null) {
                            viewModel.insertExpense(expense)
                        } else {
                            viewModel.updateExpense(expense)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
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


