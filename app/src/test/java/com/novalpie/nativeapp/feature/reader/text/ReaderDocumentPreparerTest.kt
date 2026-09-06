package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReaderDocumentPreparerTest {
    private fun chapter(id: Long) = ReaderChapterContent(id, "第$id 章", ReaderContent("第$id 章", "测试$id", "test"))
    @Test fun parsingHappensOffCallerThreadAndAppendingReusesPriorDocuments() = runBlocking {
        val caller = Thread.currentThread()
        val parsed = mutableListOf<String>()
        val preparer = ReaderDocumentPreparer(parse = { content ->
            assertNotSame("不能在调用/UI线程解析", caller, Thread.currentThread())
            parsed += content.content
            listOf(ReaderContentBlock.Text(content.content))
        })
        val options = ReaderUiOptions(showComments = true)
        preparer.prepare(1, listOf(chapter(1)), emptyList(), ReaderReplacementState(novelId = 1), options)
        val second = preparer.prepare(1, listOf(chapter(1), chapter(2)), emptyList(), ReaderReplacementState(novelId = 1), options)
        assertEquals(listOf("测试1", "测试2"), parsed)
        assertEquals(2, second.chapters.size)
        assertEquals(listOf(1L, 2L), second.chapters.map { it.chapter.chapterId })
    }

    @Test fun onlyEffectiveRuleChangesReparseWhileChromeChangesReuseBlocks() = runBlocking {
        var parsed = 0
        val preparer = ReaderDocumentPreparer(parse = { parsed++; listOf(ReaderContentBlock.Text(it.content)) })
        val source = listOf(chapter(1))
        val settings = ReaderUiOptions()
        val rules = ReaderReplacementState(novelId = 1)
        preparer.prepare(1, source, emptyList(), rules, settings)
        preparer.prepare(1, source, emptyList(), rules.copy(actionMessage = "无正文变化", revision = 8), settings.copy(showComments = false, showImages = false))
        assertEquals(1, parsed)
        val next = rules.copy(personalRules = listOf(ReaderReplacementRule("r", 1, "测试", "替换")))
        preparer.prepare(1, source, listOf(Chapter(1, "第1章", 1)), next, settings)
        assertEquals(2, parsed)
    }

    @Test fun boundedCacheCannotKeepEveryVisitedChapterForever() = runBlocking {
        val preparer = ReaderDocumentPreparer(maxEntries = 2, parse = { listOf(ReaderContentBlock.Text(it.content)) })
        for (id in 1L..12L) preparer.prepare(1, listOf(chapter(id)), emptyList(), ReaderReplacementState(novelId = 1), ReaderUiOptions())
        assertTrue(preparer.cachedEntries <= 2)
    }
}
