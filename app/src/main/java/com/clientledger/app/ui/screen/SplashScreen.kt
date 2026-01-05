package com.clientledger.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Текст о тестовой версии в нижней части экрана
        Text(
            text = """Тестовая версия приложения.
Не предназначено для коммерческого использования.
Разработано исключительно для личного использования мамой Егорычева Олега.
Все права защищены.""",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 32.dp)
        )
        
        // Автоматический переход через 1.5 секунды
        LaunchedEffect(Unit) {
            delay(1500)
            onNavigateToMain()
        }
    }
}

