package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderProgressStoreTest {
    private lateinit var store: ReaderProgressStore

    @Before
    fun setUp() {
        store = ReaderProgressStore(ApplicationProvider.getApplicationContext())
        store.clear()
    }

    @Test
    fun loadsProgressForRequestedBookWithoutLosingOtherBooks() {
        store.save(101, 1001, "A-1")
        store.save(202, 2002, "B-2")

        val first = store.load(bookId = 101)
        val second = store.load(bookId = 202)

        assertEquals(1001L, first?.chapterId)
        assertEquals("A-1", first?.chapterTitle)
        assertEquals(2002L, second?.chapterId)
        assertEquals("B-2", second?.chapterTitle)
    }

    @Test
    fun returnsNullForUnknownBookProgress() {
        store.save(101, 1001, "A-1")

        assertNull(store.load(bookId = 303))
    }

    @Test
    fun loadsRecentProgressesInMostRecentOrderWithLimit() {
        store.save(101, 1001, "A-1")
        store.save(202, 2002, "B-2")
        store.save(303, 3003, "C-3")

        val recent = store.loadRecent(limit = 2)

        assertEquals(listOf(303L, 202L), recent.map { it.bookId })
        assertEquals(listOf(3003L, 2002L), recent.map { it.chapterId })
    }

    @Test
    fun savingExistingBookMovesItToRecentFront() {
        store.save(101, 1001, "A-1")
        store.save(202, 2002, "B-2")
        store.save(101, 1002, "A-2")

        val recent = store.loadRecent(limit = 5)

        assertEquals(listOf(101L, 202L), recent.map { it.bookId })
        assertEquals(1002L, recent.first().chapterId)
        assertEquals("A-2", recent.first().chapterTitle)
    }

    @Test
    fun persistsBookTitleAlongsideTheChapterProgress() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "A-1",
            bookTitle = "A Book",
        )

        val progress = store.load(bookId = 101)

        assertEquals("A Book", progress?.bookTitle)
        assertEquals("A Book", store.loadRecent(limit = 1).single().bookTitle)
    }

    @Test
    fun persistsChapterNumberAlongsideChapterProgress() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "A-1",
            chapterNumber = 5,
        )

        val progress = store.load(bookId = 101)

        assertEquals(5, progress?.chapterNumber)
        assertEquals(5, store.loadRecent(limit = 1).single().chapterNumber)
    }
}
