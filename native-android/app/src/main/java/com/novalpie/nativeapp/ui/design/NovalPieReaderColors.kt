package com.novalpie.nativeapp.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.novalpie.nativeapp.model.ReaderCustomTheme
import com.novalpie.nativeapp.model.normalizeReaderHex
import com.novalpie.nativeapp.model.readerCustomThemeIdFromKey

/**
 * The reading surface's own palette.
 *
 * This is the one place in the app where a fixed colour is the correct answer rather than a
 * theme role. 护眼 and 深色 are paper simulations the reader chooses explicitly and expects to
 * look the same on every device, so deriving them from the Material scheme would defeat the
 * setting: picking 护眼 on a phone in dark mode has to produce sepia paper, not a dark surface
 * tinted slightly warm.
 *
 * They lived as raw `Color(0xFF…)` literals inside `NovalPieApp.kt`, which meant the app's only
 * hand-written colours were also the only ones no test could reach. `ColorContrastTest` measures
 * them here alongside the two Material palettes.
 *
 * 系统 is the exception and deliberately so: it is the "don't simulate anything" option, and it
 * follows the app theme.
 */
internal data class NovalPieReaderPalette(
    /** The page. */
    val background: Color,
    /** Chapter title and body text. */
    val text: Color,
    /** Captions, illustration labels, the source line — secondary copy on the page. */
    val meta: Color,
    /** Sidebar colors are independently configurable in the source custom-theme editor. */
    val sidebarBackground: Color = background,
    val sidebarText: Color = text,
    val accent: Color = Color(0xFF2563EB),
    val backgroundImageUri: String? = null,
)

/** 护眼 — warm paper. `meta` on `background` measures 4.88:1, so captions clear AA too. */
internal val NovalPieSepiaReaderPalette = NovalPieReaderPalette(
    background = Color(0xFFF4ECD8),
    text = Color(0xFF30271B),
    meta = Color(0xFF76634B),
    sidebarBackground = Color(0xFFF4ECD8),
    sidebarText = Color(0xFF30271B),
)

/** 深色 — near-black paper for night reading. Deliberately darker than the app's dark surface. */
internal val NovalPieDarkReaderPalette = NovalPieReaderPalette(
    background = Color(0xFF111111),
    text = Color(0xFFECECEC),
    meta = Color(0xFFAAAAAA),
    sidebarBackground = Color(0xFF111111),
    sidebarText = Color(0xFFECECEC),
)

/** The remaining source themes are fixed paper presets rather than app-theme aliases. */
internal val NovalPieLightReaderPalette = NovalPieReaderPalette(
    background = Color(0xFFFFFFFF),
    text = Color(0xFF202124),
    meta = Color(0xFF5F6368),
    sidebarBackground = Color(0xFFFFFFFF),
    sidebarText = Color(0xFF202124),
)

internal val NovalPieGreenReaderPalette = NovalPieReaderPalette(
    background = Color(0xFFE8F5E8),
    text = Color(0xFF2D5A2D),
    meta = Color(0xFF4F744F),
    sidebarBackground = Color(0xFFE8F5E8),
    sidebarText = Color(0xFF2D5A2D),
)

internal val NovalPieGrayReaderPalette = NovalPieReaderPalette(
    background = Color(0xFFF3F4F6),
    text = Color(0xFF374151),
    meta = Color(0xFF6B7280),
    sidebarBackground = Color(0xFFF3F4F6),
    sidebarText = Color(0xFF374151),
)

internal val NovalPieHighContrastReaderPalette = NovalPieReaderPalette(
    background = Color(0xFF000000),
    text = Color(0xFFFFFFFF),
    meta = Color(0xFFE5E7EB),
    sidebarBackground = Color(0xFF000000),
    sidebarText = Color(0xFFFFFFFF),
)

/**
 * Resolves a persisted `ReaderUiOptions.theme` string to a palette.
 *
 * Unknown values fall through to 系统 rather than failing, because the value is read back from
 * `ReaderSettingsStore` and an older or hand-edited preference must not break reading.
 */
@Composable
internal fun novalPieReaderPalette(
    theme: String,
    customThemes: List<ReaderCustomTheme> = emptyList(),
): NovalPieReaderPalette {
    val customId = readerCustomThemeIdFromKey(theme)
    val custom = customId?.let { id -> customThemes.firstOrNull { it.id == id } }
    if (custom != null) {
        val materialAccent = MaterialTheme.colorScheme.primary
        return NovalPieReaderPalette(
            background = readerColorFromHex(custom.backgroundHex, MaterialTheme.colorScheme.surface),
            text = readerColorFromHex(custom.textHex, MaterialTheme.colorScheme.onSurface),
            meta = readerColorFromHex(custom.textHex, MaterialTheme.colorScheme.onSurface)
                .copy(alpha = 0.68f),
            sidebarBackground = readerColorFromHex(custom.sidebarBackgroundHex, MaterialTheme.colorScheme.surface),
            sidebarText = readerColorFromHex(custom.sidebarTextHex, MaterialTheme.colorScheme.onSurface),
            accent = readerColorFromHex(custom.accentHex, materialAccent),
            backgroundImageUri = custom.backgroundImageUri,
        )
    }
    return when (theme) {
    "light" -> NovalPieLightReaderPalette
    "sepia" -> NovalPieSepiaReaderPalette
    "green" -> NovalPieGreenReaderPalette
    "gray" -> NovalPieGrayReaderPalette
    "dark" -> NovalPieDarkReaderPalette
    "high_contrast" -> NovalPieHighContrastReaderPalette
        else -> NovalPieReaderPalette(
            background = MaterialTheme.colorScheme.surface,
            text = MaterialTheme.colorScheme.onSurface,
            meta = MaterialTheme.colorScheme.onSurfaceVariant,
            sidebarBackground = MaterialTheme.colorScheme.surface,
            sidebarText = MaterialTheme.colorScheme.onSurface,
            accent = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun readerColorFromHex(value: String, fallback: Color): Color {
    val normalized = normalizeReaderHex(value, "")
    if (normalized.isBlank()) return fallback
    return normalized.removePrefix("#").toLongOrNull(16)?.let { rgb ->
        Color(0xFF000000L or rgb)
    } ?: fallback
}
