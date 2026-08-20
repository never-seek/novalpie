package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressLabelTest {
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
