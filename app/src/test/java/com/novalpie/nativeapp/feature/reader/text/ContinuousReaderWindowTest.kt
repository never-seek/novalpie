package com.novalpie.nativeapp.feature.reader.text

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import org.junit.Assert.*
import org.junit.Test

class ContinuousReaderWindowTest {
    private fun chapter(id: Long) = ReaderChapterContent(id, "第$id 章", ReaderContent("第$id 章", "原文$id", "synthetic"))
    @Test fun readingFortyChaptersDoesNotRetainFortyRawBodies() {
        var window = listOf(chapter(1))
        for (id in 2L..40L) {
            window = boundedReaderChapterWindow(window + chapter(id), id - 1)
            assertTrue("不能随阅读章数无限增长", window.size <= READER_CONTINUOUS_WINDOW_SIZE)
            assertTrue(window.any { it.chapterId == id - 1 })
            assertEquals(id, window.last().chapterId)
        }
    }
    @Test fun backwardsRecoveryKeepsTheNewPreviousChapterWithoutDroppingTheVisibleAnchor() {
        val window = boundedReaderChapterWindow((1L..12L).map(::chapter), 3, direction = -1)
        assertEquals((1L..8L).toList(), window.map { it.chapterId })
        assertTrue(window.any { it.chapterId == 3L })
    }
    @Test fun userWhoScrolledBackDuringPrefetchCannotLoseTheirVisibleChapter() {
        val window = boundedReaderChapterWindow((1L..12L).map(::chapter), 2)
        assertTrue(window.any { it.chapterId == 2L })
        assertEquals(8, window.size)
    }
    @Test fun evictedCommentsReleasePayloadButKeepTheUsersDraftAndReplyTarget() {
        val draft = ReaderChapterCommentState(draft = "尚未发送", replyingToCommentId = 7, replyingToName = "测试用户",
            comments = LoadResult.Success(emptyList()), bookReferences = mapOf(5L to LoadResult.Loading))
        val states = mapOf(1L to draft, 2L to ReaderChapterCommentState(comments = LoadResult.Success(emptyList())),
            3L to ReaderChapterCommentState(actionLoading = true), 4L to ReaderChapterCommentState())
        val kept = retainedReaderCommentWindow(states, setOf(4))
        assertEquals(setOf(1L, 3L, 4L), kept.keys)
        assertEquals("尚未发送", kept[1]?.draft)
        assertEquals(7L, kept[1]?.replyingToCommentId)
        assertEquals("测试用户", kept[1]?.replyingToName)
        assertTrue(kept[1]?.bookReferences?.isEmpty() == true)
        assertEquals(LoadResult.Idle, kept[1]?.comments)
        assertTrue(kept[3]?.actionLoading == true)
    }
    @Test fun stableParagraphKeyKeepsItsChapterAndLocalOffsetAfterPrependAndEviction() {
        val options = ReaderUiOptions(showComments = true)
        val before = readerBodyLayoutForContents((1L..8L).map(::chapter), options)
        val after = readerBodyLayoutForContents((4L..11L).map(::chapter), options).withPreviousChapterControl()
        val key = "reader-text-5-0"
        val first = readerViewportAnchorForBodyKey(before, key, 19)
        val next = readerViewportAnchorForBodyKey(after, key, 19)
        assertNotNull(first)
        assertEquals(first, next)
        assertNotEquals(readerBodyItemIndexForViewportAnchor(before, first!!), readerBodyItemIndexForViewportAnchor(after, next!!))
        assertNull(readerViewportAnchorForBodyKey(after, "reader-window-previous", 0))
        assertNull(readerViewportAnchorForBodyItem(after, 0, 0))
    }
}
