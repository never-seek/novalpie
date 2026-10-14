package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

class ReaderTouchDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<com.novalpie.nativeapp.audit.ReaderTestActivity>()
    private val paragraph="Reader body QA has enough text to span the complete native reading width."
    private val body=ReaderContent("Chapter10",(1..18).joinToString(""){"<p>$paragraph Paragraph $it.</p>"},"fixture")
    private var requests=0
    private var ui by mutableStateOf(ReaderState(bookId=99990,chapterId=10,bookTitle="QA",content=LoadResult.Success(body),
        chapterContents=listOf(ReaderChapterContent(10,"Chapter10",body)),
        chapters=LoadResult.Success(listOf(Chapter(10,"Chapter10",1),Chapter(20,"Chapter20",2))),
        nextChapterError="manual fixture",visibleChapterId=10))
    private val options=ReaderUiOptions(useInfiniteScroll=true,showTts=false,showComments=false,pageTurnEffect="none")
    private fun install(){compose.setContent{MaterialTheme{ ReaderScreen(
        state=ui,
        options=options, readerFullscreen=false,onReaderFullscreenChange={},ttsSettings=ReaderTtsSettings(),
        replacementState=ReaderReplacementState(novelId=99990),catalogQuery="",onCatalogQueryChange={},
        onDecreaseFont={},onIncreaseFont={},onCycleTheme={},onReaderOptionsChange={},onReaderTtsSettingsChange={},
        onReaderReplacementSourceChange={},onSaveReaderReplacementRule={},onDeleteReaderReplacementRule={},
        onSetSharedReaderReplacementRuleVisible={_,_->},onCloneSharedReaderReplacementRule={},onReaderSharedRulesEnabledChange={},
        onDefaultReaderSharedRulesEnabledChange={},onResetReaderSharedRulesOverride={},onResetReaderOptions={},onClearReaderChapterCache={},
        onRetry={},onRetryChapterComments={},onRetryCatalog={},onOpenReader={_,_->},onOpenReaderAtPosition={_,_,_->},
        onLoadNextChapter={_->requests++},onLoadPreviousChapter={},
        onVisibleChapterChanged={_,_->},onViewportAnchorChanged={},onToggleFavorite={},onBack={},
        onCommentDraftChange={_,_->},onSubmitComment={},onReplyComment={_,_->},onCancelCommentReply={},onCommentLike={_,_->},
        onCommentDislike={_,_->},onCommentEmoji={_,_->},onCommentAward={_,_->},onOpenUser={},onOpenLink={},onOpenWeb={},onPreviewImage={_,_->},
        recordPlaybackProgress=false
    )}}}
    private fun ready() {
        install()
        compose.waitUntil(15000){runCatching{compose.onNodeWithTag("reader-continuous-list").fetchSemanticsNode().config[ReaderWindowChapterIds]==listOf(10L)}.getOrDefault(false)}
        compose.waitForIdle()
    }
    @Test fun touchingRetryButtonMustNotAlsoOpenReaderMenu() {
        ready()
        compose.runOnIdle {ui=ui.copy(nextChapterError=null)}
        compose.onNodeWithTag("reader-continuous-list").performScrollToNode(hasText("继续下滑自动加载 · 立即加载下一章"))
        compose.waitForIdle()
        compose.runOnIdle {requests=0} // Ignore the independent automatic sentinel request.
        compose.onNodeWithText("继续下滑自动加载 · 立即加载下一章").performTouchInput {click()}
        compose.waitForIdle()
        assertEquals(1,requests)
        compose.onNodeWithText("设置").assertDoesNotExist()
    }
    @Test fun bodyTapTogglesMenuWithoutRequiringDoubleTap() {
        ready()
        compose.onNodeWithText("$paragraph Paragraph 1.").performTouchInput {click()}
        compose.onNodeWithText("设置").assertExists()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("$paragraph Paragraph 1.").performTouchInput {click()}
        compose.onNodeWithText("设置").assertDoesNotExist()
    }
    @Test fun nextChapterLoadingDoesNotSummonSidebar() {
        ready()
        compose.runOnIdle {ui=ui.copy(chapterId=20,visibleChapterId=20,content=LoadResult.Loading,chapterContents=emptyList())}
        compose.waitForIdle()
        compose.onNodeWithText("设置").assertDoesNotExist()
    }
}
