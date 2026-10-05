package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AuraCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF80F5FF),
    secondary = AuraLime,
    onSecondary = Color(0xFF003820),
    secondaryContainer = Color(0xFF005230),
    onSecondaryContainer = Color(0xFF82FBAA),
    tertiary = AuraViolet,
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFE8DDFF),
    background = AuraDarkBg,
    onBackground = AuraTextPrimaryDark,
    surface = AuraDarkSurface,
    onSurface = AuraTextPrimaryDark,
    surfaceVariant = AuraDarkSurfaceVariant,
    onSurfaceVariant = AuraTextSecondaryDark,
    outline = AuraDarkBorder,
    error = AuraError
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006877),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA1EFFF),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Color(0xFF006C42),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF8CF8BD),
    onSecondaryContainer = Color(0xFF002111),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9DDFF),
    onTertiaryContainer = Color(0xFF22005D),
    background = AuraLightBg,
    onBackground = AuraTextPrimaryLight,
    surface = AuraLightSurface,
    onSurface = AuraTextPrimaryLight,
    surfaceVariant = AuraLightSurfaceVariant,
    onSurfaceVariant = AuraTextSecondaryLight,
    outline = AuraLightBorder,
    error = AuraError
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = true, // Default to futuristic dark fitness theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
