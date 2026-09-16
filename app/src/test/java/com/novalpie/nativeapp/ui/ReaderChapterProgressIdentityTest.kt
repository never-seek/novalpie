package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderChapterProgressIdentityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrollProgressFollowsChapterBWhenReaderScreenSurvivesNavigation() {
        var state by mutableStateOf(chapterState(10))
        compose.setContent { MaterialTheme { ReaderFixture(state) } }
        waitForBody("A段0")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        compose.onNodeWithTag("reader-continuous-list").performTouchInput { down(center); advanceEventTime(100); up() }
        compose.waitForIdle()
        val firstProgress = progress()
        assertTrue("A must report within-chapter progress: $firstProgress", firstProgress > 0f && firstProgress < 50f)

        compose.runOnIdle { state = chapterState(20) }
        waitForBody("B段0")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        compose.onNodeWithTag("reader-continuous-list").performTouchInput { down(center); advanceEventTime(100); up() }
        compose.waitForIdle()
        val secondProgress = progress()
        assertTrue("B must advance beyond its 50% book-entry position, got $secondProgress", secondProgress > 50f)
        assertEquals(firstProgress + 50f, secondProgress, 1f)
    }

    private fun waitForBody(text: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun progress(): Float {
        val values = compose.onAllNodes(hasText("%", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().flatMap { it.config[SemanticsProperties.Text] }
            .mapNotNull { it.text.removeSuffix("%").toFloatOrNull() }
        assertEquals("Expected one visible footer percentage", 1, values.size)
        return values.single()
    }

    private fun chapterState(id: Long): ReaderState {
        val label = if (id == 10L) "A" else "B"
        return ReaderState(bookId = 990001, chapterId = id,
            content = LoadResult.Success(ReaderContent("${label}章", (0..39).joinToString("") { "<p>${label}段$it 正文正文正文。</p>" }, "fixture")),
            chapters = LoadResult.Success(listOf(Chapter(10, "A章"), Chapter(20, "B章"))),
            comments = LoadResult.Success(emptyList()))
    }

    @Composable private fun ReaderFixture(state: ReaderState) {
        ReaderScreen(state = state,
            options = ReaderUiOptions(pageTurnMode = false, useInfiniteScroll = false, showTts = false,
                showImages = false, showComments = false, showHeader = false, showFooter = true),
            readerFullscreen = false, onReaderFullscreenChange = {}, ttsSettings = ReaderTtsSettings(),
            replacementState = ReaderReplacementState(), catalogQuery = "", onCatalogQueryChange = {},
            onDecreaseFont = {}, onIncreaseFont = {}, onCycleTheme = {}, onReaderOptionsChange = {},
            onReaderTtsSettingsChange = {}, onReaderReplacementSourceChange = {}, onSaveReaderReplacementRule = {},
            onDeleteReaderReplacementRule = {}, onSetSharedReaderReplacementRuleVisible = { _, _ -> },
            onCloneSharedReaderReplacementRule = {}, onReaderSharedRulesEnabledChange = {},
            onDefaultReaderSharedRulesEnabledChange = {}, onResetReaderSharedRulesOverride = {},
            onResetReaderOptions = {}, onClearReaderChapterCache = {}, onRetry = {}, onRetryChapterComments = {},
            onRetryCatalog = {}, onOpenReader = { _, _ -> }, onOpenReaderAtPosition = { _, _, _ -> },
            onLoadNextChapter = {}, onVisibleChapterChanged = { _, _ -> }, onViewportAnchorChanged = {},
            onToggleFavorite = {}, onBack = {}, onCommentDraftChange = { _, _ -> }, onSubmitComment = {},
            onReplyComment = { _, _ -> }, onCancelCommentReply = {}, onCommentLike = { _, _ -> },
            onCommentDislike = { _, _ -> }, onCommentEmoji = { _, _ -> }, onCommentAward = { _, _ -> },
            onOpenUser = {}, onOpenLink = {}, onOpenWeb = {}, onPreviewImage = { _, _ -> },
            recordPlaybackProgress = false)
    }
}
