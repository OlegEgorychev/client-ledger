package com.clientledger.app.ui.screen.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddChooserDialog(
    onDismiss: () -> Unit,
    onAddAppointment: () -> Unit,
    onAddExpense: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onAddAppointment()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Новая запись")
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onAddExpense()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Расход")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
