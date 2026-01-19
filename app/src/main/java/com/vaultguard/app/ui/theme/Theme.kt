package com.vaultguard.app.ui.theme

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

// We prioritize the Light Theme for the "Clean Fintech" look
// But we provide a functional Dark Mode mapping just in case
private val AppDarkColorScheme =
        darkColorScheme(
                primary = OceanBlue,
                onPrimary = Color.White,
                secondary = SkyBlue,
                onSecondary = Color.White,
                background = DarkBackground,
                surface = DarkSurface,
                onBackground = Color.White,
                onSurface = Color.White,
                error = AccentError
        )

private val AppLightColorScheme =
        lightColorScheme(
                primary = OceanBlue,
                onPrimary = Color.White,
                primaryContainer = SoftCloud, // Used for subtle backgrounds
                onPrimaryContainer = TextPrimary,
                secondary = SkyBlue,
                onSecondary = Color.White,
                secondaryContainer = LightSilver,
                background = SoftCloud,
                onBackground = TextPrimary,
                surface = PureWhite,
                onSurface = TextPrimary,
                surfaceVariant = LightSilver, // Inputs
                error = AccentError,
                onError = Color.White
        )

@Composable
fun ZeroKeepTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // We Default to FALSE for dynamic color to enforce our Brand Identity
        dynamicColor: Boolean = false,
        content: @Composable () -> Unit
) {
    // For now, let's stick to the Light Scheme mostly unless heavily requested otherwise,
    // or map dark theme to a very specific dark blue implementation.
    // To ensure the "Clean Fintech" vibe, we might want to default to light even on dark mode
    // phones
    // briefly, or use our custom dark palette.
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()

            // If background is light (SoftCloud), we need dark status bar icons.
            // If background is dark, we need light icons.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
