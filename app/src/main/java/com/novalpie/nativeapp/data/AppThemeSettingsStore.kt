package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.AppThemeMode

class AppThemeSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadMode(): AppThemeMode = AppThemeMode.fromPersisted(prefs.getString(KEY_MODE, null))

    fun saveMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.persistedValue).apply()
    }

    companion object {
        internal const val PREFERENCES_NAME = "novalpie_native_app_theme"
        private const val KEY_MODE = "theme_mode"
    }
}
