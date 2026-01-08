package com.clientledger.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit
) {
    // Gradient background: deep blue → indigo → light cyan
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A237E),  // Deep blue
            Color(0xFF3949AB),  // Indigo
            Color(0xFF00ACC1)   // Light cyan
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // App icon (simplified calendar icon with growth indicator)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF3949AB),  // Indigo
                                Color(0xFF5C6BC0)   // Light indigo
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Growth indicator (small upward trend)
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF4DD0E1) // Cyan accent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App name
            Text(
                text = "ClientLedger",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "Учёт клиентов, доходов и времени",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Optional: Fade-in animation
        LaunchedEffect(Unit) {
            delay(1500)
            onNavigateToMain()
        }
    }
}
