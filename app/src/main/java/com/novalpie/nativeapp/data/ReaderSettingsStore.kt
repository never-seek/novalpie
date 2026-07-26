package com.novalpie.nativeapp.data

import android.content.Context

class ReaderSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_reader_settings", Context.MODE_PRIVATE)

    fun loadFontSizeSp(): Int {
        return prefs.getInt(KEY_FONT_SIZE_SP, DEFAULT_FONT_SIZE_SP).coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
    }

    fun loadTheme(): String {
        val value = prefs.getString(KEY_THEME, DEFAULT_THEME).orEmpty()
        return if (value in SUPPORTED_THEMES) value else DEFAULT_THEME
    }

    fun saveFontSizeSp(value: Int) {
        prefs.edit().putInt(KEY_FONT_SIZE_SP, value.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)).apply()
    }

    fun saveTheme(value: String) {
        prefs.edit().putString(KEY_THEME, if (value in SUPPORTED_THEMES) value else DEFAULT_THEME).apply()
    }

    companion object {
        const val MIN_FONT_SIZE_SP = 14
        const val MAX_FONT_SIZE_SP = 28
        const val DEFAULT_FONT_SIZE_SP = 18
        const val DEFAULT_THEME = "system"
        private val SUPPORTED_THEMES = setOf("system", "sepia", "dark")
        private const val KEY_FONT_SIZE_SP = "font_size_sp"
        private const val KEY_THEME = "theme"
    }
}
