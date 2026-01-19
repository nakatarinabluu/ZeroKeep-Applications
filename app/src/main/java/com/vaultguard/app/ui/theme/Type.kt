package com.vaultguard.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =
        Typography(
                // Large Header (e.g. "Balance" or Main Title)
                headlineLarge =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                lineHeight = 40.sp,
                                letterSpacing = (-0.5).sp // Tighter tracking for large text
                        ),
                // Section Headers
                titleLarge =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                letterSpacing = 0.sp
                        ),
                // Card Titles (e.g. "Netflix")
                titleMedium =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.SemiBold, // Stronger emphasis
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                letterSpacing = 0.1.sp
                        ),
                // Body Text (Usernames/Descriptions)
                bodyMedium =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Medium, // Slightly heavier than Normal for
                                // readability on white
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.2.sp
                        ),
                // Buttons
                labelLarge =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.1.sp
                        ),
                // Small labels/Hints
                labelSmall =
                        TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.5.sp,
                                color =
                                        Color(
                                                0xFF94A3B8
                                        ) // Force subtle color by default if not overridden
                        )
        )
