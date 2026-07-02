package com.aijobassistant.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette matching the new app icon's blue and indigo, with a clean UI.
 */

// Background
val DarkBackground = Color(0xFF0B1121)
val CardBackground = Color(0xFF151E32)
val CardBackgroundTranslucent = Color(0xB3151E32) // ~70% opacity
val SurfaceElevated = Color(0xFF1E2B45)

// Primary (Blue)
val PrimaryBlue = Color(0xFF0F69DB)
val PrimaryBlueHover = Color(0xFF0C54B0)
val PrimaryBlueLight = Color(0xFF4D94EB)
val PrimaryBlueDark = Color(0xFF0A4796)
val PrimaryBlueContainer = Color(0x260F69DB) // 15% opacity

// Secondary (Indigo)
val AccentIndigo = Color(0xFF2D34A9)
val AccentIndigoLight = Color(0xFF4F57C4)
val AccentIndigoContainer = Color(0x262D34A9) // 15% opacity

// Gradient endpoints
val GradientStart = PrimaryBlue
val GradientEnd = AccentIndigo

// Status Colors
val StatusSuccess = Color(0xFF10B981)
val StatusSuccessContainer = Color(0x1A10B981) // 10% opacity
val StatusWarning = Color(0xFFF59E0B)
val StatusWarningContainer = Color(0x1AF59E0B)
val StatusDanger = Color(0xFFEF4444)
val StatusDangerContainer = Color(0x1AEF4444)
val StatusPending = Color(0xFFFBBF24)

// Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Borders
val BorderColor = Color(0x1AFFFFFF) // 10% white
val BorderSubtle = Color(0x0DFFFFFF) // 5% white

// Blob / decorative
val BlobPrimary = Color(0x4D0F69DB) // 30% opacity
val BlobAccent = Color(0x332D34A9)   // 20% opacity
