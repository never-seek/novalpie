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
}
