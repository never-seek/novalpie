package com.novalpie.nativeapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderCustomThemeTest {
    @Test
    fun normalizesHexValuesAndTrimsPersistedFields() {
        val normalized = normalizeReaderCustomTheme(
            ReaderCustomTheme(
                id = " ocean ",
                name = " 海边夜读 ",
                backgroundHex = "#102030",
                textHex = "#f0f4ff",
                sidebarBackgroundHex = "invalid",
                sidebarTextHex = "#D9E8FF",
                accentHex = "#65a8ff",
                backgroundImageUri = " content://images/ocean.webp ",
            ),
        )

        assertEquals(
            ReaderCustomTheme(
                id = "ocean",
                name = "海边夜读",
                backgroundHex = "#102030",
                textHex = "#F0F4FF",
                sidebarBackgroundHex = "#FFFFFF",
                sidebarTextHex = "#D9E8FF",
                accentHex = "#65A8FF",
                backgroundImageUri = "content://images/ocean.webp",
            ),
            normalized,
        )
    }

    @Test
    fun blankThemeIdentityIsRejectedAndThemeKeysRoundTrip() {
        assertNull(normalizeReaderCustomTheme(ReaderCustomTheme(id = " ", name = "Theme")))
        assertEquals("custom:ocean", readerCustomThemeKey(" ocean "))
        assertEquals("ocean", readerCustomThemeIdFromKey("custom:ocean"))
        assertNull(readerCustomThemeIdFromKey("ocean"))
    }

    @Test
    fun duplicateIdsAreKeptOnlyOnce() {
        val themes = normalizeReaderCustomThemes(
            listOf(
                ReaderCustomTheme(id = "one", name = "One"),
                ReaderCustomTheme(id = "one", name = "Duplicate"),
                ReaderCustomTheme(id = "two", name = "Two"),
            ),
        )

        assertEquals(listOf("one", "two"), themes.map { it.id })
        assertEquals("One", themes.first().name)
    }
}
