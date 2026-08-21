package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderAdjacentChapterTest {
    private val chapters = listOf(
        Chapter(id = 10, title = "第一章"),
        Chapter(id = 20, title = "第二章"),
        Chapter(id = 30, title = "第三章")
    )

    @Test
    fun findsPreviousAndNextForCurrentChapter() {
        val adjacent = adjacentReaderChapters(currentChapterId = 20, chapters = chapters)

        assertEquals(10L, adjacent.previous?.id)
        assertEquals(30L, adjacent.next?.id)
    }

    @Test
    fun firstChapterHasNoPrevious() {
        val adjacent = adjacentReaderChapters(currentChapterId = 10, chapters = chapters)

        assertNull(adjacent.previous)
        assertEquals(20L, adjacent.next?.id)
    }

    @Test
    fun unmatchedChapterDoesNotFallBackToCatalogEdges() {
        val adjacent = adjacentReaderChapters(currentChapterId = 99, chapters = chapters)

        assertNull(adjacent.previous)
        assertNull(adjacent.next)
    }

    @Test
    fun infiniteScrollSelectsTheFirstUnloadedContiguousChapter() {
        val next = nextReaderChapterForInfiniteScroll(
            currentChapterId = 10,
            chapters = chapters,
            loadedChapterIds = setOf(10L, 20L),
        )

        assertEquals(30L, next?.id)
    }

    @Test
    fun infiniteScrollDoesNotReloadAnAlreadyLoadedChapter() {
        val next = nextReaderChapterForInfiniteScroll(
            currentChapterId = 20,
            chapters = chapters,
            loadedChapterIds = setOf(10L, 20L, 30L),
        )

        assertNull(next)
    }

    @Test
    fun infiniteScrollMarksTheEndOnlyAfterTheWholeContiguousWindowIsLoaded() {
        val nextBeforeLast = nextReaderChapterForInfiniteScroll(
            currentChapterId = 10,
            chapters = chapters,
            loadedChapterIds = setOf(10L, 20L),
        )
        val nextAtLast = nextReaderChapterForInfiniteScroll(
            currentChapterId = 10,
            chapters = chapters,
            loadedChapterIds = setOf(10L, 20L, 30L),
        )

        assertEquals(30L, nextBeforeLast?.id)
        assertNull(nextAtLast)
    }

    @Test
    fun incompleteCatalogDoesNotLookLikeTheEndOfTheBook() {
        assertEquals(true, readerCatalogIsIncomplete(currentChapterId = 10L, chapters = emptyList()))
        assertEquals(true, readerCatalogIsIncomplete(currentChapterId = 10L, chapters = chapters.drop(1)))
        assertEquals(false, readerCatalogIsIncomplete(currentChapterId = 10L, chapters = chapters))
    }

    @Test
    fun pageTurnReturnsToTheEndOfThePreviousChapterButStartsOtherChapterOpensAtTop() {
        assertEquals(
            ReaderChapterEntryPosition.End,
            readerChapterEntryPositionForPageBoundary(ReaderPageBoundaryTarget.PreviousChapter),
        )
        assertEquals(
            ReaderChapterEntryPosition.Start,
            readerChapterEntryPositionForPageBoundary(ReaderPageBoundaryTarget.NextChapter),
        )
        assertEquals(
            ReaderChapterEntryPosition.Start,
            readerChapterEntryPositionForPageBoundary(ReaderPageBoundaryTarget.None),
        )
        assertEquals(0, readerChapterEntryScrollIndex(ReaderChapterEntryPosition.Start, itemCount = 7))
        assertEquals(6, readerChapterEntryScrollIndex(ReaderChapterEntryPosition.End, itemCount = 7))
        assertEquals(0, readerChapterEntryScrollIndex(ReaderChapterEntryPosition.End, itemCount = 0))
    }
}
