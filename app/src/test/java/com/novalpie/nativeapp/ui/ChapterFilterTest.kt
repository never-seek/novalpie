package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterFilterTest {
    @Test
    fun chapterMatchesQueryByTitleNumberAndId() {
        val chapter = Chapter(id = 9001, title = "第一章", number = 12)

        assertTrue(chapterMatchesQuery(chapter, "第一"))
        assertTrue(chapterMatchesQuery(chapter, "12"))
        assertTrue(chapterMatchesQuery(chapter, "9001"))
    }

    @Test
    fun chapterMatchesQueryByWordCountAndUpdatedAt() {
        val chapter = Chapter(
            id = 9001,
            title = "第一章",
            wordCount = 12345,
            updatedAt = "2026-07-02T08:30:00Z"
        )

        assertTrue(chapterMatchesQuery(chapter, "12345"))
        assertTrue(chapterMatchesQuery(chapter, "12,345"))
        assertTrue(chapterMatchesQuery(chapter, "2026-07-02"))
    }
}
