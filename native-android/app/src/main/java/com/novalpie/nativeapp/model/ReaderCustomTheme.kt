package com.novalpie.nativeapp.model

private val READER_THEME_HEX_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")

/** A user-owned reader theme matching the source reader's custom-theme fields. */
data class ReaderCustomTheme(
    val id: String,
    val name: String,
    val backgroundHex: String = "#FFFFFF",
    val textHex: String = "#202124",
    val sidebarBackgroundHex: String = "#FFFFFF",
    val sidebarTextHex: String = "#202124",
    val accentHex: String = "#2563EB",
    val backgroundImageUri: String? = null,
)

internal const val READER_CUSTOM_THEME_PREFIX = "custom:"

internal fun readerCustomThemeKey(id: String): String =
    READER_CUSTOM_THEME_PREFIX + id.trim()

internal fun readerCustomThemeIdFromKey(theme: String): String? =
    theme.removePrefix(READER_CUSTOM_THEME_PREFIX)
        .takeIf { theme.startsWith(READER_CUSTOM_THEME_PREFIX) && it.isNotBlank() }

internal fun normalizeReaderHex(value: String?, fallback: String): String =
    value?.trim()?.uppercase()?.takeIf { READER_THEME_HEX_PATTERN.matches(it) } ?: fallback

/** Keeps malformed or hand-edited preference values from making the reader unusable. */
internal fun normalizeReaderCustomTheme(theme: ReaderCustomTheme): ReaderCustomTheme? {
    val id = theme.id.trim().take(80).takeIf(String::isNotBlank) ?: return null
    val name = theme.name.trim().take(48).takeIf(String::isNotBlank) ?: return null
    return theme.copy(
        id = id,
        name = name,
        backgroundHex = normalizeReaderHex(theme.backgroundHex, "#FFFFFF"),
        textHex = normalizeReaderHex(theme.textHex, "#202124"),
        sidebarBackgroundHex = normalizeReaderHex(theme.sidebarBackgroundHex, "#FFFFFF"),
        sidebarTextHex = normalizeReaderHex(theme.sidebarTextHex, "#202124"),
        accentHex = normalizeReaderHex(theme.accentHex, "#2563EB"),
        backgroundImageUri = theme.backgroundImageUri
            ?.trim()
            ?.take(2048)
            ?.takeIf(String::isNotBlank),
    )
}

internal fun normalizeReaderCustomThemes(themes: List<ReaderCustomTheme>): List<ReaderCustomTheme> {
    val result = ArrayList<ReaderCustomTheme>(minOf(themes.size, 12))
    val seen = HashSet<String>()
    themes.forEach { theme ->
        val normalized = normalizeReaderCustomTheme(theme) ?: return@forEach
        if (seen.add(normalized.id)) result += normalized
    }
    return result.take(12)
}
