package com.novalpie.nativeapp.feature.search

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.PersistedSearchSettings
import com.novalpie.nativeapp.data.SearchHistoryStore
import com.novalpie.nativeapp.data.SearchSettingsStore
import com.novalpie.nativeapp.model.NovelTag
import com.novalpie.nativeapp.model.SearchPage
import com.novalpie.nativeapp.ui.ResolvedSearchRequest
import com.novalpie.nativeapp.ui.SearchOptions
import com.novalpie.nativeapp.ui.SearchViewMode

/** Search owns its response envelope; callers never infer a next page from a card count alone. */
internal interface SearchRepository {
    suspend fun page(request: ResolvedSearchRequest, page: Int): SearchPage
    suspend fun tags(): List<NovelTag>
}

internal class WebsiteSearchRepository(private val api: NovalPieApi) : SearchRepository {
    override suspend fun page(request: ResolvedSearchRequest, page: Int): SearchPage = api.searchPage(
        keyword = request.keyword, page = page, limit = 60,
        sortBy = request.sortBy, sortOrder = request.sortOrder, scope = request.scope,
        matchType = request.matchType, adultFilter = request.adultFilter, source = request.source,
        minWordCount = request.minWordCount, maxWordCount = request.maxWordCount,
        requiredTags = request.requiredTags, blockedTags = request.blockedTags,
        tagsAny = request.tagsAny, tagsExpression = request.tagsExpression, blockedTerms = request.blockedTerms,
        platform = request.platform, novelType = request.type, status = request.status,
    )
    override suspend fun tags(): List<NovelTag> = api.allTags()
}

/** The existing preference keys remain the migration contract. No new profile or history reset. */
internal interface SearchPreferences {
    fun settings(): PersistedSearchSettings
    fun save(settings: PersistedSearchSettings)
    fun history(): List<String>
    fun remember(keyword: String)
    fun clearHistory()
    fun clearOptions()
}

internal class StoredSearchPreferences(
    private val settings: SearchSettingsStore,
    private val history: SearchHistoryStore,
) : SearchPreferences {
    override fun settings() = settings.load()
    override fun save(settings: PersistedSearchSettings) = this.settings.save(settings)
    override fun history() = history.load()
    override fun remember(keyword: String) = history.saveKeyword(keyword)
    override fun clearHistory() = history.clear()
    override fun clearOptions() = settings.clearCachedSettings()
}

internal fun PersistedSearchSettings.toSearchOptions() = SearchOptions(
    sortBy, sortOrder, scope, matchType, adultFilter, source, wordCountRange,
    requiredTags, blockedTags, advancedSyntaxEnabled,
    if (viewMode == "list") SearchViewMode.List else SearchViewMode.Grid, cacheEnabled,
)

internal fun SearchOptions.persisted() = PersistedSearchSettings(
    sortBy, sortOrder, scope, matchType, adultFilter, source, wordCountRange,
    requiredTags, blockedTags, advancedSyntaxEnabled,
    if (viewMode == SearchViewMode.List) "list" else "grid", cacheEnabled,
)
