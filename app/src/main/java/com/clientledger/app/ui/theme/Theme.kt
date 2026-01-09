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
    primary = SoftIndigoPrimary, // #4F5DFF - main accent
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAFF), // Light indigo tint for containers
    onPrimaryContainer = SoftIndigoPrimary, // Primary color for text on container
    
    secondary = SoftIndigoSecondary, // #6B7280 - secondary UI elements
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3F4F6), // Light gray tint
    onSecondaryContainer = SoftIndigoTextSecondary, // Secondary text color
    
    tertiary = SoftIndigoSuccess, // #22C55E - success/positive states
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5), // Light green tint
    onTertiaryContainer = Color(0xFF15803D), // Darker green for contrast
    
    error = SoftIndigoError, // #EF4444 - error/destructive actions
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), // Light red tint
    onErrorContainer = Color(0xFF991B1B), // Darker red for contrast
    
    background = LightBackground, // #F6F7FB - soft indigo-tinted background
    onBackground = SoftIndigoTextPrimary, // #111827 - main text
    surface = LightSurface, // #FFFFFF - white for cards
    onSurface = SoftIndigoTextPrimary, // #111827 - main text on surface
    surfaceVariant = LightSurfaceVariant, // Slightly darker for separation
    onSurfaceVariant = SoftIndigoTextSecondary, // #4B5563 - secondary text
    
    outline = LightOutline, // #E5E7EB - soft outline
    outlineVariant = LightOutlineVariant // #E5E7EB - outline variant
)

@Composable
fun ClientLedgerTheme(
    darkTheme: Boolean = false, // Default to light, will be overridden by themeMode
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
