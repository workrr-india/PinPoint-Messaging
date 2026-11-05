package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BbmTealLight,
    onPrimary = Color.Black,
    primaryContainer = BbmTealPrimary,
    onPrimaryContainer = Color.White,
    secondary = PinCyanAccent,
    onSecondary = Color.Black,
    tertiary = StatusReadGreen,
    background = Color(0xFF0F172A),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF1E293B),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF243042),
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateNavyBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BbmTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F7),
    onPrimaryContainer = BbmTealDark,
    secondary = BbmTealLight,
    onSecondary = Color.White,
    tertiary = StatusReadGreen,
    background = Color(0xFFECEFF1),
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFE2E8F0)
)

@Composable
fun PinPointTheme(
    darkTheme: Boolean = false, // Default to iconic BBM light theme matching screenshot
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
