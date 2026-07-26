package com.novalpie.nativeapp.data

import android.content.Context

data class PersistedSearchSettings(
    val sortBy: String = "relevance",
    val sortOrder: String = "desc",
    val scope: String = "all",
    val matchType: String = "ai",
    val adultFilter: String = "all",
    val source: String = "",
    val wordCountRange: String = ""
)

class SearchSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_search_settings", Context.MODE_PRIVATE)

    fun load(): PersistedSearchSettings =
        PersistedSearchSettings(
            sortBy = prefs.getString(KEY_SORT_BY, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().sortBy,
            sortOrder = prefs.getString(KEY_SORT_ORDER, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().sortOrder,
            scope = prefs.getString(KEY_SCOPE, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().scope,
            matchType = prefs.getString(KEY_MATCH_TYPE, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().matchType,
            adultFilter = prefs.getString(KEY_ADULT_FILTER, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().adultFilter,
            source = prefs.getString(KEY_SOURCE, null) ?: PersistedSearchSettings().source,
            wordCountRange = prefs.getString(KEY_WORD_COUNT_RANGE, null) ?: PersistedSearchSettings().wordCountRange
        )

    fun save(settings: PersistedSearchSettings) {
        prefs.edit()
            .putString(KEY_SORT_BY, settings.sortBy)
            .putString(KEY_SORT_ORDER, settings.sortOrder)
            .putString(KEY_SCOPE, settings.scope)
            .putString(KEY_MATCH_TYPE, settings.matchType)
            .putString(KEY_ADULT_FILTER, settings.adultFilter)
            .putString(KEY_SOURCE, settings.source)
            .putString(KEY_WORD_COUNT_RANGE, settings.wordCountRange)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_SCOPE = "scope"
        private const val KEY_MATCH_TYPE = "match_type"
        private const val KEY_ADULT_FILTER = "adult_filter"
        private const val KEY_SOURCE = "source"
        private const val KEY_WORD_COUNT_RANGE = "word_count_range"
    }
}
