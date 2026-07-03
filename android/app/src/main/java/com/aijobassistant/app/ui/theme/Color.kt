package com.aijobassistant.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette matching the new app icon's blue and indigo, with a clean UI.
 */

// Background
val DarkBackground = Color(0xFFF8FAFC) // Now Light Background
val CardBackground = Color(0xFFFFFFFF)
val CardBackgroundTranslucent = Color(0xE6FFFFFF) // ~90% opacity white
val SurfaceElevated = Color(0xFFF1F5F9)

// Primary (Emerald Green - variable names kept for backward compatibility)
val PrimaryBlue = Color(0xFF059669) // Emerald 600 - Success, growth
val PrimaryBlueHover = Color(0xFF047857) // Emerald 700
val PrimaryBlueLight = Color(0xFF34D399) // Emerald 400
val PrimaryBlueDark = Color(0xFF065F46) // Emerald 800
val PrimaryBlueContainer = Color(0x26059669) // 15% opacity

// Secondary (Indigo)
val AccentIndigo = Color(0xFF475569) // Slate 600
val AccentIndigoLight = Color(0xFF64748B) // Slate 500
val AccentIndigoContainer = Color(0x26475569) // 15% opacity

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
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF334155)
val TextMuted = Color(0xFF64748B)

// Borders
val BorderColor = Color(0x1A000000) // 10% black
val BorderSubtle = Color(0x0D000000) // 5% black

// Blob / decorative
val BlobPrimary = Color(0x4D0F69DB) // 30% opacity
val BlobAccent = Color(0x332D34A9)   // 20% opacity
