package com.github.adocker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Docker-inspired color palette
private val DockerBlue = Color(0xFF0DB7ED)
private val DockerDarkBlue = Color(0xFF384D54)
private val DockerNavy = Color(0xFF2496ED)

// Extended color palette for better visual hierarchy
private val DockerLightBlue = Color(0xFF4FC3F7)
private val DockerTeal = Color(0xFF26C6DA)
private val SuccessGreen = Color(0xFF4CAF50)
private val WarningOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFF44336)

private val DarkColorScheme = darkColorScheme(
    primary = DockerBlue,
    onPrimary = Color(0xFF003544),
    primaryContainer = DockerDarkBlue,
    onPrimaryContainer = DockerLightBlue,
    secondary = DockerTeal,
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF1A3A4A),
    onSecondaryContainer = DockerLightBlue,
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF00504C),
    onTertiaryContainer = Color(0xFF70F3E8),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    surfaceTint = DockerBlue,
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = DockerNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8F9),
    onPrimaryContainer = Color(0xFF001E2D),
    secondary = Color(0xFF4A6270),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE7F7),
    onSecondaryContainer = Color(0xFF051F2B),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA0F0E5),
    onTertiaryContainer = Color(0xFF002019),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceTint = DockerNavy,
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
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

            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Set status bar icon appearance (light or dark icons)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Material Design 3 Typography Scale
val Typography = Typography(
    // Display styles - Large, attention-grabbing text
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    // Headline styles - High-emphasis text
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // Title styles - Medium-emphasis text
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body styles - Used for longer text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Label styles - Buttons, tabs, labels
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
