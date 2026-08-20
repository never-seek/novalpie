package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.FavoritesCacheMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesSettingsStoreTest {
    private lateinit var store: FavoritesSettingsStore

    @Before
    fun setUp() {
        store = FavoritesSettingsStore(ApplicationProvider.getApplicationContext())
        store.save(PersistedFavoritesSettings())
    }

    @Test
    fun savesAndRestoresCollectionPresentationChoices() {
        val expected = PersistedFavoritesSettings(
            cacheMode = FavoritesCacheMode.All,
            tab = "history",
            layout = "list",
            gridColumns = 4,
            displayMode = "all",
            selectedDisplayGroupId = 8L,
            currentPage = 3,
            sortField = "last_read_time",
            sortOrder = "asc",
            searchQuery = "saved query"
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }

    @Test
    fun invalidPersistedValuesFallBackToWebsiteDefaults() {
        store.save(
            PersistedFavoritesSettings(
                tab = "unknown",
                layout = "cards",
                displayMode = "folder",
                sortField = "favorite_count",
                sortOrder = "sideways"
            )
        )

        assertEquals(PersistedFavoritesSettings(), store.load())
    }

    @Test
    fun gridColumnsAreRestrictedToTheReadableChoices() {
        store.save(PersistedFavoritesSettings(gridColumns = 3))
        assertEquals(3, store.load().gridColumns)

        store.save(PersistedFavoritesSettings(gridColumns = 6))
        assertEquals(DEFAULT_GRID_COLUMNS, store.load().gridColumns)
    }

    @Test
    fun noSearchModeKeepsPresentationButNeverRestoresTheSearchField() {
        store.save(
            PersistedFavoritesSettings(
                cacheMode = FavoritesCacheMode.NoSearch,
                tab = "history",
                layout = "list",
                displayMode = "all",
                selectedDisplayGroupId = 12L,
                currentPage = 4,
                sortField = "updated_at",
                sortOrder = "asc",
                searchQuery = "must not persist"
            )
        )

        assertEquals(
            PersistedFavoritesSettings(
                cacheMode = FavoritesCacheMode.NoSearch,
                tab = "history",
                layout = "list",
                displayMode = "all",
                selectedDisplayGroupId = 12L,
                currentPage = 4,
                sortField = "updated_at",
                sortOrder = "asc"
            ),
            store.load()
        )
    }

    @Test
    fun disabledCacheRetainsOnlyThePolicyForTheNextLaunch() {
        store.save(
            PersistedFavoritesSettings(
                tab = "history",
                layout = "list",
                selectedDisplayGroupId = 12L,
                currentPage = 4,
                searchQuery = "stale"
            )
        )

        store.save(
            PersistedFavoritesSettings(
                cacheMode = FavoritesCacheMode.None,
                tab = "history",
                layout = "list",
                selectedDisplayGroupId = 12L,
                currentPage = 4,
                searchQuery = "stale"
            )
        )

        assertEquals(PersistedFavoritesSettings(cacheMode = FavoritesCacheMode.None), store.load())
        assertEquals(FavoritesCacheMode.NoSearch, FavoritesCacheMode.None.next())
        assertEquals(FavoritesCacheMode.All, FavoritesCacheMode.NoSearch.next())
        assertEquals(FavoritesCacheMode.None, FavoritesCacheMode.All.next())
    }

    @Test
    fun clearCacheResetsPresentationWhilePreservingTheSelectedMode() {
        store.save(
            PersistedFavoritesSettings(
                cacheMode = FavoritesCacheMode.NoSearch,
                tab = "history",
                layout = "list",
                selectedDisplayGroupId = 12L,
                currentPage = 4
            )
        )

        store.clearCachedPresentationValues()

        assertEquals(PersistedFavoritesSettings(cacheMode = FavoritesCacheMode.NoSearch), store.load())
    }
}
