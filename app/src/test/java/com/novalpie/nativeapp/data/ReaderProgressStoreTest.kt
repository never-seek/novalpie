package com.novalpie.nativeapp.data

import androidx.test.core.app.ApplicationProvider
import com.novalpie.nativeapp.model.FavoriteEntry
import com.novalpie.nativeapp.model.NovelCard
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
            chapterCountAtLastRead = 42,
        )

        val progress = store.load(bookId = 101)

        assertEquals(5, progress?.chapterNumber)
        assertEquals(42, progress?.chapterCountAtLastRead)
        assertEquals(5, store.loadRecent(limit = 1).single().chapterNumber)
        assertEquals(42, store.loadRecent(limit = 1).single().chapterCountAtLastRead)
    }

    @Test
    fun doesNotCarryACompletedCatalogueBaselineToAnotherChapterWithoutAConfirmedDirectory() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "A-1",
            chapterNumber = 42,
            chapterCountAtLastRead = 42,
        )
        store.save(
            bookId = 101,
            chapterId = 1002,
            chapterTitle = "A-2",
            chapterNumber = 43,
        )

        val progress = store.load(bookId = 101)

        assertEquals(43, progress?.chapterNumber)
        assertNull(progress?.chapterCountAtLastRead)
    }

    @Test
    fun clearingOneBookKeepsOtherReadingProgress() {
        store.save(101, 1001, "A-1", bookTitle = "A Book")
        store.save(202, 2002, "B-2", bookTitle = "B Book")

        store.clear(bookId = 202)

        assertNull(store.load(bookId = 202))
        assertEquals(1001L, store.load(bookId = 101)?.chapterId)
        assertEquals(listOf(101L), store.loadRecent(limit = 5).map { it.bookId })
    }

    @Test
    fun backfillsCompletedCatalogueForLegacyProgressWhenSourceAlsoReportsCompleted() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "Final",
            bookTitle = "Completed Book",
            chapterNumber = 84,
        )

        val changed = store.backfillCompletedFavoriteCatalogues(
            listOf(
                FavoriteEntry(
                    book = NovelCard(id = 101, title = "Completed Book"),
                    lastChapter = 84,
                    chapterCount = 84,
                )
            )
        )

        assertEquals(1, changed)
        assertEquals(84, store.load(bookId = 101)?.chapterCountAtLastRead)
    }

    @Test
    fun backfillsCompletedCatalogueWhenNativeReaderReachedTheEndAndSourceHasNoProgress() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "Final",
            bookTitle = "Completed Book",
            chapterNumber = 84,
        )

        val changed = store.backfillCompletedFavoriteCatalogues(
            listOf(
                FavoriteEntry(
                    book = NovelCard(id = 101, title = "Completed Book"),
                    chapterCount = 84,
                )
            )
        )

        assertEquals(1, changed)
        assertEquals(84, store.load(bookId = 101)?.chapterCountAtLastRead)
    }

    @Test
    fun backfillsCompletedCatalogueWhenSourceUsesZeroForNoWebReadingProgress() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "Final",
            bookTitle = "Completed Book",
            chapterNumber = 84,
        )

        val changed = store.backfillCompletedFavoriteCatalogues(
            listOf(
                FavoriteEntry(
                    book = NovelCard(id = 101, title = "Completed Book"),
                    lastChapter = 0,
                    chapterCount = 84,
                )
            )
        )

        assertEquals(1, changed)
        assertEquals(84, store.load(bookId = 101)?.chapterCountAtLastRead)
    }

    @Test
    fun backfillsCatalogueWhenNativeReaderCompletedDespiteStaleEarlierWebProgress() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "Final",
            bookTitle = "Completed Book",
            chapterNumber = 84,
        )

        val changed = store.backfillCompletedFavoriteCatalogues(
            listOf(
                FavoriteEntry(
                    book = NovelCard(id = 101, title = "Completed Book"),
                    lastChapter = 83,
                    chapterCount = 84,
                )
            )
        )

        assertEquals(1, changed)
        assertEquals(84, store.load(bookId = 101)?.chapterCountAtLastRead)
    }

    @Test
    fun doesNotBackfillCatalogueForAnOrdinarilyPausedFavorite() {
        store.save(
            bookId = 101,
            chapterId = 1001,
            chapterTitle = "Paused",
            bookTitle = "Paused Book",
            chapterNumber = 83,
        )

        val changed = store.backfillCompletedFavoriteCatalogues(
            listOf(
                FavoriteEntry(
                    book = NovelCard(id = 101, title = "Paused Book"),
                    lastChapter = 83,
                    chapterCount = 84,
                )
            )
        )

        assertEquals(0, changed)
        assertNull(store.load(bookId = 101)?.chapterCountAtLastRead)
    }
}
