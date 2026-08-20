package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCoverPrefetchTest {
    @Test
    fun warmsTheInitialResponsiveGridRowsWithTheSameThumbnailUrlsAsBookCards() {
        val books = listOf(
            NovelCard(1, "one", coverUrl = "thumb-1", fullCoverUrl = "full-1"),
            NovelCard(2, "two", fullCoverUrl = "full-2"),
            NovelCard(3, "three", coverUrl = "thumb-3"),
            NovelCard(4, "four", coverUrl = "thumb-3", fullCoverUrl = "full-4"),
            NovelCard(5, "five", coverUrl = "thumb-5"),
            NovelCard(6, "six", coverUrl = "thumb-6"),
            NovelCard(7, "seven", coverUrl = "thumb-7"),
            NovelCard(8, "eight", coverUrl = "thumb-8"),
        )

        assertEquals(
            listOf("thumb-1", "full-2", "thumb-3", "thumb-5"),
            searchInitialCoverPreloadUrls(books),
        )
        assertEquals(emptyList<String>(), searchInitialCoverPreloadUrls(books, preloadCount = 0))
    }

    @Test
    fun derivesPreloadBudgetFromTheCurrentResponsiveColumnCount() {
        assertEquals(4, searchCoverPreloadCount(columnCount = 2, rowCount = 2))
        assertEquals(8, searchCoverPreloadCount(columnCount = 4, rowCount = 2))
        assertEquals(8, searchCoverPreloadCount(columnCount = 6, rowCount = 2))

        val books = (1L..12L).map { id -> NovelCard(id, "book-$id", coverUrl = "cover-$id") }
        assertEquals(
            (1L..8L).map { id -> "cover-$id" },
            searchInitialCoverPreloadUrls(books, columnCount = 6),
        )
    }

    @Test
    fun followsTheLastVisibleBookAndUsesTheThumbnailCacheKey() {
        val books = listOf(
            NovelCard(1, "one", coverUrl = "thumb-1"),
            NovelCard(2, "two", coverUrl = "thumb-2"),
            NovelCard(3, "three", coverUrl = "thumb-3"),
            NovelCard(4, "four", fullCoverUrl = "full-4"),
            NovelCard(5, "five", coverUrl = "thumb-5", fullCoverUrl = "full-5"),
            NovelCard(6, "six", fullCoverUrl = "full-6"),
            NovelCard(7, "seven", coverUrl = "thumb-5"),
            NovelCard(8, "eight", coverUrl = "thumb-8")
        )

        assertEquals(
            listOf("full-4", "thumb-5", "full-6", "thumb-8"),
            searchCoverPreloadUrlsAfterVisible(
                books = books,
                visibleBookIds = listOf(2, 3),
                preloadCount = 8
            )
        )
    }

    @Test
    fun defaultLookAheadIsCappedToOneSmallBatch() {
        val books = (1L..10L).map { id -> NovelCard(id, "book-$id", coverUrl = "cover-$id") }

        assertEquals(
            listOf("cover-3", "cover-4", "cover-5", "cover-6"),
            searchCoverPreloadUrlsAfterVisible(
                books = books,
                visibleBookIds = listOf(1, 2)
            )
        )
        assertEquals(
            emptyList<String>(),
            searchCoverPreloadUrlsAfterVisible(books, visibleBookIds = emptyList())
        )
    }

    @Test
    fun doesNotCompeteWithFirstVisibleSearchCovers() {
        val books = (1L..6L).map { id -> NovelCard(id, "book-$id", coverUrl = "cover-$id") }

        assertEquals(
            emptyList<String>(),
            searchCoverPreloadUrlsAfterVisible(
                books = books,
                visibleBookIds = listOf(1, 2),
                allowSpeculativePreload = false
            )
        )
    }
}
