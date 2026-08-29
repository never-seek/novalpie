package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchSettingsStoreTest {
    private lateinit var store: SearchSettingsStore

    @Before
    fun setUp() {
        store = SearchSettingsStore(ApplicationProvider.getApplicationContext())
        store.clear()
    }

    @Test
    fun savesAndLoadsSearchSettings() {
        val expected = PersistedSearchSettings(
            sortBy = "updated_at",
            sortOrder = "asc",
            scope = "tags",
            matchType = "ai",
            adultFilter = "adult_only",
            source = "novelPia",
            wordCountRange = "100000..500000",
            requiredTags = listOf("同人", "奇幻"),
            blockedTags = listOf("后宫"),
            advancedSyntaxEnabled = true,
            viewMode = "list"
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }

    @Test
    fun returnsDefaultsWhenNothingWasSaved() {
        assertEquals(PersistedSearchSettings(), store.load())
    }

    @Test
    fun defaultsMatchTheWebsiteSearchContract() {
        val defaults = store.load()

        assertEquals("favorite_count", defaults.sortBy)
        assertEquals("desc", defaults.sortOrder)
        assertEquals("fuzzy_strict", defaults.matchType)
        assertEquals("all", defaults.adultFilter)
        assertEquals(emptyList<String>(), defaults.requiredTags)
        assertEquals(emptyList<String>(), defaults.blockedTags)
        assertEquals(false, defaults.advancedSyntaxEnabled)
        assertEquals("grid", defaults.viewMode)
        assertEquals(true, defaults.cacheEnabled)
    }

    @Test
    fun disablingCacheDropsSavedValuesButRetainsTheLocalPolicy() {
        store.save(PersistedSearchSettings(sortBy = "updated_at", requiredTags = listOf("奇幻")))

        store.setCacheEnabled(false)

        assertEquals(PersistedSearchSettings(cacheEnabled = false), store.load())
        store.setCacheEnabled(true)
        assertEquals(PersistedSearchSettings(), store.load())
    }

    @Test
    fun preservesAnExistingAllContentSelectionAfterTheWebsiteDefaultChanges() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("novalpie_native_search_settings", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("adult_filter", "all")
            .apply()

        assertEquals("all", store.load().adultFilter)
    }

    @Test
    fun preservesAnExistingSafeSelection() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("novalpie_native_search_settings", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("adult_filter", "unrestricted")
            .apply()

        assertEquals("unrestricted", store.load().adultFilter)
    }

    @Test
    fun clearingCachedSettingsKeepsTheCurrentCachePolicy() {
        store.save(PersistedSearchSettings(sortBy = "updated_at", viewMode = "list"))

        store.clearCachedSettings()

        assertEquals(PersistedSearchSettings(), store.load())
    }
}
