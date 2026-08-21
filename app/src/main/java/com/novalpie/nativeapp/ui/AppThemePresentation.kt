package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.AppThemeMode

internal fun AppThemeMode.resolvesDark(systemDark: Boolean): Boolean = when (this) {
    AppThemeMode.System -> systemDark
    AppThemeMode.Light -> false
    AppThemeMode.Dark -> true
}

/** The source drawer offers a direct light/dark toggle rather than exposing its system default. */
internal fun sourceThemeToggleLabel(isDark: Boolean): String = if (isDark) "浅色" else "深色"

internal fun AppThemeMode.displayLabel(): String = when (this) {
    AppThemeMode.System -> "跟随系统"
    AppThemeMode.Light -> "浅色"
    AppThemeMode.Dark -> "深色"
}
