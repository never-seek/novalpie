package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderSessionStoreTest {
    private lateinit var store: ReaderSessionStore

    @Before
    fun setUp() {
        store = ReaderSessionStore(ApplicationProvider.getApplicationContext())
        store.clear()
    }

    @Test
    fun restoresTheLastActiveReaderRoute() {
        store.save(bookId = 354491, chapterId = 6992449)

        assertEquals(354491L, store.load()?.bookId)
        assertEquals(6992449L, store.load()?.chapterId)
    }

    @Test
    fun clearRemovesTheActiveReaderRoute() {
        store.save(bookId = 354491, chapterId = 6992449)
        store.clear()

        assertNull(store.load())
    }

    @Test
    fun ignoresInvalidRouteIds() {
        store.save(bookId = 0, chapterId = 6992449)
        store.save(bookId = 354491, chapterId = 0)

        assertNull(store.load())
    }
}
