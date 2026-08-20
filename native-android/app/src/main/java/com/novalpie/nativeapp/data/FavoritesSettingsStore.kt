package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.FavoritesCacheMode

/** Local presentation choices mirror the source favourites page without becoming server mutations. */
data class PersistedFavoritesSettings(
    val cacheMode: FavoritesCacheMode = FavoritesCacheMode.All,
    val tab: String = "favorites",
    val layout: String = "grid",
    val gridColumns: Int = DEFAULT_GRID_COLUMNS,
    val displayMode: String = "default",
    val selectedDisplayGroupId: Long? = null,
    val currentPage: Int = 1,
    val sortField: String = "created_at",
    val sortOrder: String = "desc",
    val searchQuery: String = ""
)

class FavoritesSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PersistedFavoritesSettings {
        val cacheMode = FavoritesCacheMode.fromPersisted(prefs.getString(KEY_CACHE_MODE, null))
        // The source stores the mode separately from the cache payload. With cache disabled,
        // stale values must never be restored after a process restart.
        if (cacheMode == FavoritesCacheMode.None) {
            return PersistedFavoritesSettings(cacheMode = cacheMode)
        }

        return PersistedFavoritesSettings(
            cacheMode = cacheMode,
            tab = prefs.getString(KEY_TAB, null).orEmpty().takeIf { it in TABS } ?: "favorites",
            layout = prefs.getString(KEY_LAYOUT, null).orEmpty().takeIf { it in LAYOUTS } ?: "grid",
            gridColumns = normalizeGridColumns(prefs.getInt(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)),
            displayMode = prefs.getString(KEY_DISPLAY_MODE, null).orEmpty().takeIf { it in DISPLAY_MODES } ?: "default",
            selectedDisplayGroupId = prefs.getLongOrNull(KEY_SELECTED_DISPLAY_GROUP_ID)?.takeIf { it > 0L },
            currentPage = prefs.getInt(KEY_CURRENT_PAGE, 1).coerceIn(1, MAX_CACHED_PAGE),
            sortField = prefs.getString(KEY_SORT_FIELD, null).orEmpty().takeIf { it in SORT_FIELDS } ?: "created_at",
            sortOrder = prefs.getString(KEY_SORT_ORDER, null).orEmpty().takeIf { it in SORT_ORDERS } ?: "desc",
            searchQuery = if (cacheMode == FavoritesCacheMode.All) {
                prefs.getString(KEY_SEARCH_QUERY, "").orEmpty().take(MAX_SEARCH_QUERY_LENGTH)
            } else {
                ""
            }
        )
    }

    fun save(settings: PersistedFavoritesSettings) {
        val editor = prefs.edit()
            .putString(KEY_CACHE_MODE, settings.cacheMode.persistedValue)

        if (settings.cacheMode == FavoritesCacheMode.None) {
            editor.removeCachedPresentationValues().apply()
            return
        }

        editor
            .putString(KEY_TAB, settings.tab.takeIf { it in TABS } ?: "favorites")
            .putString(KEY_LAYOUT, settings.layout.takeIf { it in LAYOUTS } ?: "grid")
            .putInt(KEY_GRID_COLUMNS, normalizeGridColumns(settings.gridColumns))
            .putString(KEY_DISPLAY_MODE, settings.displayMode.takeIf { it in DISPLAY_MODES } ?: "default")
            .putNullableLong(KEY_SELECTED_DISPLAY_GROUP_ID, settings.selectedDisplayGroupId?.takeIf { it > 0L })
            .putInt(KEY_CURRENT_PAGE, settings.currentPage.coerceIn(1, MAX_CACHED_PAGE))
            .putString(KEY_SORT_FIELD, settings.sortField.takeIf { it in SORT_FIELDS } ?: "created_at")
            .putString(KEY_SORT_ORDER, settings.sortOrder.takeIf { it in SORT_ORDERS } ?: "desc")
        if (settings.cacheMode == FavoritesCacheMode.All) {
            editor.putString(KEY_SEARCH_QUERY, settings.searchQuery.take(MAX_SEARCH_QUERY_LENGTH))
        } else {
            editor.remove(KEY_SEARCH_QUERY)
        }
        editor.apply()
    }

    /** Matches the source clear-cache action while preserving the selected cache policy. */
    fun clearCachedPresentationValues() {
        prefs.edit().removeCachedPresentationValues().apply()
    }

    private fun android.content.SharedPreferences.Editor.putNullableLong(key: String, value: Long?) = apply {
        if (value == null) remove(key) else putLong(key, value)
    }

    private fun android.content.SharedPreferences.Editor.removeCachedPresentationValues() = apply {
        remove(KEY_TAB)
        remove(KEY_LAYOUT)
        remove(KEY_GRID_COLUMNS)
        remove(KEY_DISPLAY_MODE)
        remove(KEY_SELECTED_DISPLAY_GROUP_ID)
        remove(KEY_CURRENT_PAGE)
        remove(KEY_SORT_FIELD)
        remove(KEY_SORT_ORDER)
        remove(KEY_SEARCH_QUERY)
    }

    private fun android.content.SharedPreferences.getLongOrNull(key: String): Long? =
        if (contains(key)) getLong(key, 0L) else null

    companion object {
        private const val PREFERENCES_NAME = "novalpie_native_favorites_settings"
        private const val KEY_CACHE_MODE = "favorites_cache_mode"
        private const val KEY_TAB = "tab"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_SELECTED_DISPLAY_GROUP_ID = "selected_display_group_id"
        private const val KEY_CURRENT_PAGE = "current_page"
        private const val KEY_SORT_FIELD = "sort_field"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_SEARCH_QUERY = "search_query"

        private const val MAX_CACHED_PAGE = 10_000
        private const val MAX_SEARCH_QUERY_LENGTH = 500

        private val TABS = setOf("favorites", "history")
        private val LAYOUTS = setOf("grid", "list")
        private val DISPLAY_MODES = setOf("default", "all", "unclassified")
        private val SORT_FIELDS = setOf("created_at", "last_read_time", "updated_at")
        private val SORT_ORDERS = setOf("asc", "desc")
    }
}

const val DEFAULT_GRID_COLUMNS: Int = 2

/** The product deliberately exposes only the three readable source-style choices. */
fun normalizeGridColumns(value: Int): Int = when (value) {
    2, 3, 4 -> value
    else -> DEFAULT_GRID_COLUMNS
}
