package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemePresentationTest {
    @Test
    fun resolvesSystemAndExplicitModesWithoutLosingTheSystemDefault() {
        assertEquals(true, AppThemeMode.System.resolvesDark(systemDark = true))
        assertEquals(false, AppThemeMode.System.resolvesDark(systemDark = false))
        assertEquals(false, AppThemeMode.Light.resolvesDark(systemDark = true))
        assertEquals(true, AppThemeMode.Dark.resolvesDark(systemDark = false))
    }

    @Test
    fun sourceDrawerLabelOffersTheOppositeAppearance() {
        assertEquals("深色", sourceThemeToggleLabel(isDark = false))
        assertEquals("浅色", sourceThemeToggleLabel(isDark = true))
    }
}
