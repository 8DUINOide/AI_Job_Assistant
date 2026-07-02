package com.aijobassistant.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom dark Material 3 color scheme matching the web dashboard's glassmorphic design.
 * The app is dark-mode only to maintain the premium feel of the original web interface.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlueLight,

    secondary = AccentIndigo,
    onSecondary = TextPrimary,
    secondaryContainer = AccentIndigoContainer,
    onSecondaryContainer = AccentIndigoLight,

    tertiary = StatusSuccess,
    onTertiary = TextPrimary,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,

    error = StatusDanger,
    onError = TextPrimary,
    errorContainer = StatusDangerContainer,
    onErrorContainer = StatusDanger,

    outline = BorderColor,
    outlineVariant = BorderSubtle,

    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = PrimaryBlueDark,

    scrim = Color(0x80000000)
)

@Composable
fun AIJobAssistantTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
