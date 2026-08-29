package com.novalpie.nativeapp.data

import android.content.Context

data class PersistedSearchSettings(
    val sortBy: String = "favorite_count",
    val sortOrder: String = "desc",
    val scope: String = "all",
    val matchType: String = "fuzzy_strict",
    /** The live source starts with all content visible; users can narrow this explicitly. */
    val adultFilter: String = "all",
    val source: String = "",
    val wordCountRange: String = "",
    val requiredTags: List<String> = emptyList(),
    val blockedTags: List<String> = emptyList(),
    val advancedSyntaxEnabled: Boolean = false,
    /** Mirrors the source's local `novel_search_settings.viewMode` value. */
    val viewMode: String = "grid",
    /** Mirrors the source toolbar's local "do not cache search settings" switch. */
    val cacheEnabled: Boolean = true
)

class SearchSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("novalpie_native_search_settings", Context.MODE_PRIVATE)

    fun load(): PersistedSearchSettings {
        val cacheEnabled = prefs.getBoolean(KEY_CACHE_ENABLED, true)
        if (!cacheEnabled) return PersistedSearchSettings(cacheEnabled = false)
        val storedAdultFilter = prefs.getString(KEY_ADULT_FILTER, null)?.takeIf { it.isNotBlank() }
        return PersistedSearchSettings(
            sortBy = prefs.getString(KEY_SORT_BY, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().sortBy,
            sortOrder = prefs.getString(KEY_SORT_ORDER, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().sortOrder,
            scope = prefs.getString(KEY_SCOPE, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().scope,
            matchType = prefs.getString(KEY_MATCH_TYPE, null)?.takeIf { it.isNotBlank() } ?: PersistedSearchSettings().matchType,
            // A stored value is an explicit user choice, including "all" from previous builds.
            // Only fresh / cleared settings inherit the current website default.
            adultFilter = storedAdultFilter ?: PersistedSearchSettings().adultFilter,
            source = prefs.getString(KEY_SOURCE, null) ?: PersistedSearchSettings().source,
            wordCountRange = prefs.getString(KEY_WORD_COUNT_RANGE, null) ?: PersistedSearchSettings().wordCountRange,
            requiredTags = loadTagSet(KEY_REQUIRED_TAGS),
            blockedTags = loadTagSet(KEY_BLOCKED_TAGS),
            advancedSyntaxEnabled = prefs.getBoolean(KEY_ADVANCED_SYNTAX, false),
            viewMode = prefs.getString(KEY_VIEW_MODE, null)
                ?.takeIf { it == VIEW_MODE_GRID || it == VIEW_MODE_LIST }
                ?: PersistedSearchSettings().viewMode,
            cacheEnabled = true
        )
    }

    fun save(settings: PersistedSearchSettings) {
        if (!settings.cacheEnabled) {
            disableCache()
            return
        }
        prefs.edit()
            .putBoolean(KEY_CACHE_ENABLED, true)
            .putString(KEY_SORT_BY, settings.sortBy)
            .putString(KEY_SORT_ORDER, settings.sortOrder)
            .putString(KEY_SCOPE, settings.scope)
            .putString(KEY_MATCH_TYPE, settings.matchType)
            .putString(KEY_ADULT_FILTER, settings.adultFilter)
            .putString(KEY_SOURCE, settings.source)
            .putString(KEY_WORD_COUNT_RANGE, settings.wordCountRange)
            .putStringSet(KEY_REQUIRED_TAGS, settings.requiredTags.toStoredTagSet())
            .putStringSet(KEY_BLOCKED_TAGS, settings.blockedTags.toStoredTagSet())
            .putBoolean(KEY_ADVANCED_SYNTAX, settings.advancedSyntaxEnabled)
            .putString(
                KEY_VIEW_MODE,
                settings.viewMode.takeIf { it == VIEW_MODE_GRID || it == VIEW_MODE_LIST } ?: VIEW_MODE_GRID
            )
            .apply()
    }

    fun setCacheEnabled(enabled: Boolean) {
        if (!enabled) {
            disableCache()
            return
        }
        prefs.edit().putBoolean(KEY_CACHE_ENABLED, true).apply()
    }

    /** Drops stored values but retains the source-equivalent cache policy toggle. */
    fun clearCachedSettings() {
        prefs.edit()
            .remove(KEY_SORT_BY)
            .remove(KEY_SORT_ORDER)
            .remove(KEY_SCOPE)
            .remove(KEY_MATCH_TYPE)
            .remove(KEY_ADULT_FILTER)
            .remove(KEY_SOURCE)
            .remove(KEY_WORD_COUNT_RANGE)
            .remove(KEY_REQUIRED_TAGS)
            .remove(KEY_BLOCKED_TAGS)
            .remove(KEY_ADVANCED_SYNTAX)
            .remove(KEY_VIEW_MODE)
            .apply()
    }

    private fun disableCache() {
        prefs.edit()
            .clear()
            .putBoolean(KEY_CACHE_ENABLED, false)
            .apply()
    }

    private fun loadTagSet(key: String): List<String> =
        prefs.getStringSet(key, emptySet())
            .orEmpty()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .sorted()

    private fun List<String>.toStoredTagSet(): Set<String> =
        asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()

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
        private const val KEY_REQUIRED_TAGS = "required_tags"
        private const val KEY_BLOCKED_TAGS = "blocked_tags"
        private const val KEY_ADVANCED_SYNTAX = "advanced_syntax"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_CACHE_ENABLED = "cache_enabled"
        private const val VIEW_MODE_GRID = "grid"
        private const val VIEW_MODE_LIST = "list"
    }
}
