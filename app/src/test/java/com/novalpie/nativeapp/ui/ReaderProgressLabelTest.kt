package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderProgressLabelTest {
    @Test fun unavailableCatalogDoesNotMasqueradeAsUnreadProgress() {
        assertNull(readerBookProgressFraction(99, emptyList(), .8f))
    }
    @Test fun chapterMissingFromTheCatalogHasUnknownBookProgress() {
        assertNull(readerBookProgressFraction(99, listOf(Chapter(id = 10, title = "一")), .8f))
    }
    @Test fun fullBookProgressIncludesTheCurrentChapterFraction() {
        val chapters = listOf(Chapter(id = 10, title = "一"), Chapter(id = 20, title = "二"))
        assertEquals(.25f, readerBookProgressFraction(10, chapters, .5f)!!, .0001f)
        assertEquals(1f, readerBookProgressFraction(20, chapters, 1f)!!, .0001f)
        assertEquals(.5f, readerBookProgressFraction(20, chapters, Float.NaN)!!, .0001f)
    }
    @Test fun scrollProgressExcludesTitleAndTreatsChapterCommentsAsCompletedBody() {
        assertEquals(0f, readerScrollProgress(0, 10, 30, 1, 5), .001f)
        assertEquals(.125f, readerScrollProgress(1, 50, 100, 1, 5), .001f)
        assertEquals(1f, readerScrollProgress(5, 0, 100, 1, 5), .001f)
    }
    @Test
    fun labelsCurrentChapterPositionWhenChapterExists() {
        val chapters = listOf(
            Chapter(id = 10, title = "第一章"),
            Chapter(id = 20, title = "第二章"),
            Chapter(id = 30, title = "第三章")
        )

        assertEquals("2章-\"第二章\"", readerChapterProgressLabel(20, chapters))
        assertEquals(1f / 3f, readerBookProgressFraction(20, chapters))
    }

    @Test
    fun labelsUnmatchedChapterWithoutLosingTotal() {
        val chapters = listOf(
            Chapter(id = 10, title = "第一章"),
            Chapter(id = 20, title = "第二章")
        )

        assertEquals("当前章节 99 · 目录共 2 章", readerChapterProgressLabel(99, chapters))
    }

    @Test
    fun labelsEmptyCatalog() {
        assertEquals("当前章节 99 · 目录未加载", readerChapterProgressLabel(99, emptyList()))
    }
}
