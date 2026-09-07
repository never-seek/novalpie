package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.feature.reader.text.boundedReaderChapterWindow
import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Real Compose keyed-list prepend/eviction. No network, progress writes or user settings changed. */
class ContinuousReaderWindowDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    private fun chapter(id: Long) = ReaderChapterContent(id, "第$id 章", ReaderContent("第$id 章",
        (1..18).joinToString("") { "<p>第$id 章第$it 段，连续阅读应保持内容和位置，向前向后都不能漏段。</p>" }, "test"))

    @Test fun replacingWindowKeepsVisibleParagraphAndPreviousButtonRestoresEarlierBody() {
        val catalog = (1L..20L).map { Chapter(it, "第$it 章", it.toInt()) }
        val anchor = AtomicReference<ReaderViewportAnchor?>()
        var sources by mutableStateOf((1L..8L).map(::chapter))
        var visible by mutableStateOf(1L)
        var previousRequests = 0
        compose.setContent { MaterialTheme {
            ReaderScreen(
                state = ReaderState(bookId = 900151, chapterId = 1, bookTitle = "合成连续阅读", content = LoadResult.Success(chapter(1).content),
                    chapterContents = sources, chapters = LoadResult.Success(catalog), visibleChapterId = visible),
                options = ReaderUiOptions(useInfiniteScroll = true, showTts = false, showComments = false, pageTurnEffect = "none"),
                readerFullscreen = false, onReaderFullscreenChange = {}, ttsSettings = ReaderTtsSettings(),
                replacementState = ReaderReplacementState(novelId = 900151), catalogQuery = "", onCatalogQueryChange = {},
                onDecreaseFont = {}, onIncreaseFont = {}, onCycleTheme = {}, onReaderOptionsChange = {}, onReaderTtsSettingsChange = {},
                onReaderReplacementSourceChange = {}, onSaveReaderReplacementRule = {}, onDeleteReaderReplacementRule = {},
                onSetSharedReaderReplacementRuleVisible = { _, _ -> }, onCloneSharedReaderReplacementRule = {},
                onReaderSharedRulesEnabledChange = {}, onDefaultReaderSharedRulesEnabledChange = {}, onResetReaderSharedRulesOverride = {},
                onResetReaderOptions = {}, onClearReaderChapterCache = {}, onRetry = {}, onRetryChapterComments = {}, onRetryCatalog = {},
                onOpenReader = { _, _ -> }, onOpenReaderAtPosition = { _, _, _ -> }, onLoadNextChapter = {},
                onLoadPreviousChapter = {
                    previousRequests++
                    val previous = chapter(sources.first().chapterId - 1)
                    sources = boundedReaderChapterWindow(listOf(previous) + sources, visible, direction = -1)
                },
                onVisibleChapterChanged = { id, _ -> visible = id }, onViewportAnchorChanged = { anchor.set(it) },
                onToggleFavorite = {}, onBack = {}, onCommentDraftChange = { _, _ -> }, onSubmitComment = {}, onReplyComment = { _, _ -> },
                onCancelCommentReply = {}, onCommentLike = { _, _ -> }, onCommentDislike = { _, _ -> }, onCommentEmoji = { _, _ -> },
                onCommentAward = { _, _ -> }, onOpenUser = {}, onOpenLink = {}, onOpenWeb = {}, onPreviewImage = { _, _ -> },
                recordPlaybackProgress = false,
            )
        } }
        compose.waitUntil(15000) { anchor.get() != null }
        val list = compose.onNodeWithTag("reader-continuous-list")
        // ScrollToNode only guarantees that the target is somewhere on screen; the preceding
        // chapter may still own the viewport top. This case requires an exact chapter-local anchor.
        val target = readerBodyItemIndexForViewportAnchor(readerBodyLayoutForContents(sources,
            ReaderUiOptions(showComments = false)), ReaderViewportAnchor(6, 8, 0))!!
        list.performScrollToIndex(target)
        compose.waitUntil(5000) { anchor.get()?.chapterId == 6L }
        val before = anchor.get()!!
        compose.runOnIdle { sources = (4L..11L).map(::chapter) }
        compose.waitUntil(15000) { list.fetchSemanticsNode().config[ReaderWindowChapterIds] == (4L..11L).toList() }
        compose.waitForIdle()
        assertEquals("更新窗口不能跳到别章", before.chapterId, anchor.get()?.chapterId)
        assertEquals("更新窗口不能跳到别段", before.itemIndexWithinChapter, anchor.get()?.itemIndexWithinChapter)
        list.performScrollToIndex(0)
        compose.onNodeWithText("加载上一章").performClick()
        compose.waitUntil(15000) { sources.first().chapterId == 3L && previousRequests == 1 }
        compose.waitUntil(15000) { list.fetchSemanticsNode().config[ReaderWindowChapterIds].firstOrNull() == 3L }
        list.performScrollToNode(hasText("第3 章第1 段", substring = true))
        compose.onNodeWithText("第3 章第1 段", substring = true).assertIsDisplayed()
        assertTrue(sources.size <= 8)
    }
}
