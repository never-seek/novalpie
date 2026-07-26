package com.novalpie.nativeapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal data class ThemeTokens(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val onBackground: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val outline: Long
)

internal fun lightThemeTokens(): ThemeTokens = ThemeTokens(
    // Mirrors the source site's current blue-gray mobile palette.
    primary = 0xFF3182ED,
    onPrimary = 0xFFFFFFFF,
    primaryContainer = 0xFFE7F1FF,
    onPrimaryContainer = 0xFF146DE1,
    secondary = 0xFF7D8A97,
    secondaryContainer = 0xFFEDF0F2,
    onSecondaryContainer = 0xFF45525E,
    background = 0xFFF2F2F2,
    surface = 0xFFFFFFFF,
    surfaceVariant = 0xFFF5F7FA,
    onBackground = 0xFF1F2933,
    onSurface = 0xFF45525E,
    onSurfaceVariant = 0xFF7D8A97,
    outline = 0xFFCED4DA
)

internal fun darkThemeTokens(): ThemeTokens = ThemeTokens(
    primary = 0xFF4D9DFF,
    onPrimary = 0xFFFFFFFF,
    primaryContainer = 0xFF001D3D,
    onPrimaryContainer = 0xFFB8D6FF,
    secondary = 0xFF8E99A4,
    secondaryContainer = 0xFF2A2F34,
    onSecondaryContainer = 0xFFDCE0E5,
    background = 0xFF191C1F,
    surface = 0xFF23262A,
    surfaceVariant = 0xFF2A2F34,
    onBackground = 0xFFF0F2F5,
    onSurface = 0xFFF0F2F5,
    onSurfaceVariant = 0xFFB8C1CA,
    outline = 0xFF4A545E
)

private val lightTokens = lightThemeTokens()
private val darkTokens = darkThemeTokens()

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(lightTokens.primary),
    onPrimary = Color(lightTokens.onPrimary),
    primaryContainer = Color(lightTokens.primaryContainer),
    onPrimaryContainer = Color(lightTokens.onPrimaryContainer),
    secondary = Color(lightTokens.secondary),
    secondaryContainer = Color(lightTokens.secondaryContainer),
    onSecondaryContainer = Color(lightTokens.onSecondaryContainer),
    background = Color(lightTokens.background),
    surface = Color(lightTokens.surface),
    surfaceVariant = Color(lightTokens.surfaceVariant),
    onBackground = Color(lightTokens.onBackground),
    onSurface = Color(lightTokens.onSurface),
    onSurfaceVariant = Color(lightTokens.onSurfaceVariant),
    outline = Color(lightTokens.outline)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(darkTokens.primary),
    onPrimary = Color(darkTokens.onPrimary),
    primaryContainer = Color(darkTokens.primaryContainer),
    onPrimaryContainer = Color(darkTokens.onPrimaryContainer),
    secondary = Color(darkTokens.secondary),
    secondaryContainer = Color(darkTokens.secondaryContainer),
    onSecondaryContainer = Color(darkTokens.onSecondaryContainer),
    background = Color(darkTokens.background),
    surface = Color(darkTokens.surface),
    surfaceVariant = Color(darkTokens.surfaceVariant),
    onBackground = Color(darkTokens.onBackground),
    onSurface = Color(darkTokens.onSurface),
    onSurfaceVariant = Color(darkTokens.onSurfaceVariant),
    outline = Color(darkTokens.outline)
)

private val NovalPieShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun NovalPieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = NovalPieShapes,
        content = content
    )
}
