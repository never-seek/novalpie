package com.novalpie.nativeapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemePaletteTest {
    @Test
    fun lightPaletteUsesSourceWebsiteBlueGrayTokens() {
        val palette = lightThemeTokens()

        assertEquals(0xFFF2F2F2, palette.background)
        assertEquals(0xFFFFFFFF, palette.surface)
        assertEquals(0xFF3182ED, palette.primary)
        assertEquals(0xFFEDF0F2, palette.secondaryContainer)
        assertEquals(0xFF45525E, palette.onSurface)
        assertEquals(0xFFCED4DA, palette.outline)
        assertNotEquals(palette.primary, palette.secondary)
    }

    @Test
    fun darkPaletteKeepsWebsiteContrastAndBlueAccent() {
        val palette = darkThemeTokens()

        assertEquals(0xFF191C1F, palette.background)
        assertEquals(0xFF23262A, palette.surface)
        assertEquals(0xFF4D9DFF, palette.primary)
        assertEquals(0xFF2A2F34, palette.secondaryContainer)
        assertNotEquals(palette.primary, palette.secondary)
    }
}
