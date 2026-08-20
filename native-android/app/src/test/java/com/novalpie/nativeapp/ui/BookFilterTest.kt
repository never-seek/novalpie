package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.NovelCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFilterTest {
    @Test
    fun bookMatchesQueryByStatus() {
        val book = NovelCard(id = 354491, title = "Native Book", status = "已完结")

        assertTrue(bookMatchesQuery(book, "完结"))
        assertTrue(bookMatchesQuery(book, "已完结"))
        assertFalse(bookMatchesQuery(book, "连载"))
    }

    @Test
    fun bookMatchesQueryKeepsExistingTitleAuthorTagAndIdMatching() {
        val book = NovelCard(
            id = 354491,
            title = "Native Book",
            author = "Author Name",
            tags = listOf("Fantasy")
        )

        assertTrue(bookMatchesQuery(book, "native"))
        assertTrue(bookMatchesQuery(book, "author"))
        assertTrue(bookMatchesQuery(book, "fantasy"))
        assertTrue(bookMatchesQuery(book, "354491"))
    }

    @Test
    fun bookMatchesQueryByWordCountAndUpdatedAt() {
        val book = NovelCard(
            id = 354491,
            title = "Native Book",
            wordCount = 1234567,
            updatedAt = "2026-07-02T08:30:00Z"
        )

        assertTrue(bookMatchesQuery(book, "1234567"))
        assertTrue(bookMatchesQuery(book, "1,234,567"))
        assertTrue(bookMatchesQuery(book, "2026-07-02"))
    }
}
