package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileBooksSettingsStoreTest {
    private lateinit var store: ProfileBooksSettingsStore

    @Before
    fun setUp() {
        store = ProfileBooksSettingsStore(ApplicationProvider.getApplicationContext())
        store.save(PersistedProfileBooksSettings())
    }

    @Test
    fun savesTheSelectedUploadedBooksColumnCount() {
        store.save(PersistedProfileBooksSettings(gridColumns = 4))
        assertEquals(4, store.load().gridColumns)
    }

    @Test
    fun invalidColumnCountsUseTheTwoColumnDefault() {
        store.save(PersistedProfileBooksSettings(gridColumns = 1))
        assertEquals(DEFAULT_GRID_COLUMNS, store.load().gridColumns)
    }
}
