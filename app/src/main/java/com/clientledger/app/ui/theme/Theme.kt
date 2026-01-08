package com.clientledger.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = LightIndigo,
    onPrimaryContainer = Color.White,
    
    secondary = Cyan,
    onSecondary = Color.White,
    secondaryContainer = LightCyan,
    onSecondaryContainer = DeepBlue,
    
    tertiary = SoftGreen,
    onTertiary = Color.White,
    tertiaryContainer = LightGreen,
    onTertiaryContainer = DeepBlue,
    
    error = MutedRed,
    onError = Color.White,
    errorContainer = LightRed,
    onErrorContainer = Color.White,
    
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B8C8),
    
    outline = Color(0xFF3A4560),
    outlineVariant = Color(0xFF2A3450)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = LightIndigo,
    onPrimaryContainer = Color.White,
    
    secondary = CoolBlue,
    onSecondary = Color.White,
    secondaryContainer = LightCyan,
    onSecondaryContainer = DeepBlue,
    
    tertiary = SoftGreen,
    onTertiary = Color.White,
    tertiaryContainer = LightGreen,
    onTertiaryContainer = DeepBlue,
    
    error = MutedRed,
    onError = Color.White,
    errorContainer = LightRed,
    onErrorContainer = Color.White,
    
    background = Color(0xFFF5F7FA),
    onBackground = DeepBlue,
    surface = Color.White,
    onSurface = DeepBlue,
    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = Indigo,
    
    outline = Color(0xFF9FA8DA),
    outlineVariant = Color(0xFFC5CAE9)
)

@Composable
fun ClientLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
