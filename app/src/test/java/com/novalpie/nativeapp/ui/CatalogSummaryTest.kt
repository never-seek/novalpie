package com.novalpie.nativeapp.ui

import com.novalpie.nativeapp.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSummaryTest {
    @Test
    fun summarizesUnfilteredCatalogWithCurrentChapterPosition() {
        val chapters = listOf(
            Chapter(id = 10, title = "第一章"),
            Chapter(id = 20, title = "第二章"),
            Chapter(id = 30, title = "第三章")
        )

        assertEquals(
            "共 3 章 · 当前第 2 章",
            catalogSummaryLabel(allChapters = chapters, visibleChapters = chapters, currentChapterId = 20)
        )
    }

    @Test
    fun summarizesFilteredCatalogAndKeepsCurrentChapterPositionInFullCatalog() {
        val chapters = listOf(
            Chapter(id = 10, title = "序章"),
            Chapter(id = 20, title = "训练"),
            Chapter(id = 30, title = "决战")
        )
        val visible = listOf(chapters[1], chapters[2])

        assertEquals(
            "共 3 章 · 已筛选 2 章 · 当前第 3 章",
            catalogSummaryLabel(allChapters = chapters, visibleChapters = visible, currentChapterId = 30)
        )
    }

    @Test
    fun summarizesEmptyCatalog() {
        assertEquals(
            "目录未加载",
            catalogSummaryLabel(allChapters = emptyList(), visibleChapters = emptyList(), currentChapterId = 30)
        )
    }
}
