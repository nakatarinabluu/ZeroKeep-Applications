package com.vaultguard.app.ui.theme

import androidx.compose.ui.graphics.Color

// --- MODERN CLEAN FINTECH PALETTE ---

// Backgrounds
val SoftCloud = Color(0xFFF8FAFC) // Main Background (Slate-50)
val PureWhite = Color(0xFFFFFFFF) // Card Surfaces

// Primary Gradients & Accents
val BlueGradientStart = Color(0xFF3B82F6) // Bright Blue (Blue-500)
val BlueGradientEnd = Color(0xFF2563EB) // Deep Blue (Blue-600)
val OceanGradientStart = Color(0xFF0EA5E9) // Sky Blue
val OceanGradientEnd = Color(0xFF3B82F6) // Blue

// Typography
val TextPrimary = Color(0xFF1E293B) // Slate-800 (High Contrast)
val TextSecondary = Color(0xFF64748B) // Slate-500 (Soft Grey)
val TextTertiary = Color(0xFF94A3B8) // Slate-400

// Functional Colors
val AccentSuccess = Color(0xFF10B981) // Emerald-500
val AccentError = Color(0xFFEF4444) // Red-500
val AccentWarning = Color(0xFFF59E0B) // Amber-500

// Legacy Compatibility (Mapped)
val BrandPurple = BlueGradientEnd
val BrandBlue = OceanGradientStart
val BackgroundLight = SoftCloud
val SurfaceWhite = PureWhite
val LightSilver = Color(0xFFF1F5F9) // Slate-100 (Inputs/Dividers)

// Dark Mode Fallbacks (Deep Navy)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
