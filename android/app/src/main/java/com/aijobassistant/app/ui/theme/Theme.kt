package com.aijobassistant.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom light Material 3 color scheme matching a clean professional design.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlueDark,

    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = AccentIndigoContainer,
    onSecondaryContainer = AccentIndigoLight,

    tertiary = StatusSuccess,
    onTertiary = Color.White,

    background = DarkBackground, // It's light now, keeping name for compatibility
    onBackground = TextPrimary,

    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,

    error = StatusDanger,
    onError = Color.White,
    errorContainer = StatusDangerContainer,
    onErrorContainer = StatusDanger,

    outline = BorderColor,
    outlineVariant = BorderSubtle,

    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = PrimaryBlueLight,

    scrim = Color(0x33000000)
)

@Composable
fun AIJobAssistantTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
