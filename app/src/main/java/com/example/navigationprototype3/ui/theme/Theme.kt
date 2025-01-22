package com.example.navigationprototype3.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---------- SAMPLE COLOR PALETTE ---------- //

// Light color palette
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF50606F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4F5),
    onSecondaryContainer = Color(0xFF0C1D29),
    tertiary = Color(0xFF65587B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF201634),
    error = Color(0xFFBA1B1B),
    errorContainer = Color(0xFFFFDAD4),
    onError = Color.White,
    onErrorContainer = Color(0xFF410001),
    background = Color(0xFFFBFCFE),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDDE3EB),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    inverseOnSurface = Color(0xFFF0F1F3),
    inverseSurface = Color(0xFF2E3133),
    inversePrimary = Color(0xFF9FCCFF),
    surfaceTint = Color(0xFF0061A6),
)

// Dark color palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FCCFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFB7C8D8),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF394956),
    onSecondaryContainer = Color(0xFFD3E4F5),
    tertiary = Color(0xFFD0C1E8),
    onTertiary = Color(0xFF352B4B),
    tertiaryContainer = Color(0xFF4C4162),
    onTertiaryContainer = Color(0xFFEBDDFF),
    error = Color(0xFFFFB4A9),
    errorContainer = Color(0xFF930006),
    onError = Color(0xFF680003),
    onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF191C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    inverseOnSurface = Color(0xFF191C1E),
    inverseSurface = Color(0xFFE2E2E5),
    inversePrimary = Color(0xFF0061A6),
    surfaceTint = Color(0xFF9FCCFF),
)

// ---------- TYPOGRAPHY & SHAPES (Optional) ---------- //

private val AppTypography = androidx.compose.material3.Typography(
    /* you can override default text styles here, e.g.:
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    )*/
)

private val AppShapes = androidx.compose.material3.Shapes(
    /* you can customize shapes here */
)

// ---------- THEME COMPOSABLE ---------- //

@Composable
fun Navigationprototype3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
