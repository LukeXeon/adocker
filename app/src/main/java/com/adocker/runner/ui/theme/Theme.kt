package com.adocker.runner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Docker-inspired color palette
private val DockerBlue = Color(0xFF0DB7ED)
private val DockerDarkBlue = Color(0xFF384D54)
private val DockerNavy = Color(0xFF2496ED)

private val DarkColorScheme = darkColorScheme(
    primary = DockerBlue,
    onPrimary = Color.White,
    primaryContainer = DockerDarkBlue,
    onPrimaryContainer = Color.White,
    secondary = DockerNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A3A4A),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF4DB6AC),
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCACACA),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = DockerNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8F9),
    onPrimaryContainer = DockerDarkBlue,
    secondary = DockerBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F5FC),
    onSecondaryContainer = DockerDarkBlue,
    tertiary = Color(0xFF00897B),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1C1C),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun ADockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val Typography = Typography()
