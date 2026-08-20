package com.novalpie.nativeapp.data

import android.content.Context

/** Local presentation settings for the owner-only uploaded-book management shelf. */
data class PersistedProfileBooksSettings(
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
)

class ProfileBooksSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PersistedProfileBooksSettings = PersistedProfileBooksSettings(
        gridColumns = normalizeGridColumns(
            preferences.getInt(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS),
        ),
    )

    fun save(settings: PersistedProfileBooksSettings) {
        preferences.edit()
            .putInt(KEY_GRID_COLUMNS, normalizeGridColumns(settings.gridColumns))
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "novalpie_native_profile_books_settings"
        private const val KEY_GRID_COLUMNS = "grid_columns"
    }
}
