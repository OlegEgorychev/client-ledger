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
    primaryContainer = Color(0xFFE3E7F1), // Slightly more visible indigo tint
    onPrimaryContainer = Color(0xFF1A237E), // Darker indigo for better contrast
    
    secondary = CoolBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EDF5), // More visible cyan tint
    onSecondaryContainer = Color(0xFF01579B), // Darker blue for better contrast
    
    tertiary = SoftGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1F5E1), // More visible green tint
    onTertiaryContainer = Color(0xFF2E7D32), // Darker green for better contrast
    
    error = MutedRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5), // More visible red tint
    onErrorContainer = Color(0xFFB71C1C), // Darker red for better contrast
    
    background = LightBackground,
    onBackground = Color(0xFF0D0D0D), // Very dark, almost black (high contrast, modern 2025)
    surface = LightSurface,
    onSurface = Color(0xFF0D0D0D), // Very dark for maximum readability
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF495057), // Darker gray for better secondary text contrast
    
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

@Composable
fun ClientLedgerTheme(
    darkTheme: Boolean = true, // Default to dark, will be overridden by themeMode
    themeMode: com.clientledger.app.ui.theme.ThemeMode? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        com.clientledger.app.ui.theme.ThemeMode.LIGHT -> false
        com.clientledger.app.ui.theme.ThemeMode.DARK -> true
        null -> darkTheme // Fallback to parameter if themeMode is null
    }
    
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
