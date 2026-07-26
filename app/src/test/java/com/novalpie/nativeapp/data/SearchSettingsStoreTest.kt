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
            wordCountRange = "100000..500000"
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }

    @Test
    fun returnsDefaultsWhenNothingWasSaved() {
        assertEquals(PersistedSearchSettings(), store.load())
    }
}
