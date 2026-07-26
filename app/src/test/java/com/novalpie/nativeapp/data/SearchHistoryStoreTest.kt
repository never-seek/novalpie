package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchHistoryStoreTest {
    private lateinit var store: SearchHistoryStore

    @Before
    fun setUp() {
        store = SearchHistoryStore(ApplicationProvider.getApplicationContext())
        store.clear()
    }

    @Test
    fun savesRecentKeywordsMostRecentFirst() {
        store.saveKeyword("alpha")
        store.saveKeyword("beta")
        store.saveKeyword("gamma")

        assertEquals(listOf("gamma", "beta", "alpha"), store.load())
    }

    @Test
    fun savingExistingKeywordMovesItToFront() {
        store.saveKeyword("alpha")
        store.saveKeyword("beta")
        store.saveKeyword("alpha")

        assertEquals(listOf("alpha", "beta"), store.load())
    }

    @Test
    fun ignoresBlankKeywordsAndLimitsHistorySize() {
        store.saveKeyword(" ")
        (1..12).forEach { store.saveKeyword("k$it") }

        assertEquals((12 downTo 3).map { "k$it" }, store.load())
    }

    @Test
    fun loadsLastKeywordFromMostRecentHistoryEntry() {
        assertEquals("", store.loadLastKeyword())

        store.saveKeyword("alpha")
        store.saveKeyword("beta")

        assertEquals("beta", store.loadLastKeyword())
    }
}
