package com.vaultguard.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue,
    secondary = KeyholeCyan,
    tertiary = TextGray,
    background = DeepNavyBackground,
    surface = DeepNavySurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite,
    error = DangerRed
)

// We want to force Dark Theme or at least make Light Theme look similar (Strong)
// But for now, let's define a "Light" that is just slightly lighter version of Strong matches
private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    secondary = KeyholeCyan,
    tertiary = TextGray,
    background = Color(0xFFF5F5F7), // Keep the "Swiss" light for contrast if system is light?
    // User asked for "Simple but Strong" - usually means Dark Mode. 
    // Let's make "Light" theme also dark-ish or just respect system.
    // Given the user liked the "Obsidian" vibe, let's stick to Dark defaults.
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun ZeroKeepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // DISABLE Dynamic Color to enforce OUR Brand
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //     val context = LocalContext.current
        //     if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // FORCE DARK THEME for that "Strong" look everywhere? 
        // User complained theme "inside apps is different". 
        // Best way to unify is enforce ONE theme.
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Match background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
