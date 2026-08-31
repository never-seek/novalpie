package com.novalpie.nativeapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.model.FavoriteStatus
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderContent
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test

class ReaderChapterCommentsInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun collapsedChapterCommentsExpandInsideTheSelectableReaderArticle() {
        composeRule.setContent {
            MaterialTheme {
                val inlineInteractionActive = remember { AtomicBoolean(false) }
                SelectionContainer {
                    LazyColumn {
                        item {
                            ReaderChapterCommentsSection(
                                chapterId = 7L,
                                commentState = ReaderChapterCommentState(
                                    comments = LoadResult.Success(emptyList()),
                                ),
                                defaultCollapsed = true,
                                inlineCommentInteractionActive = inlineInteractionActive,
                                onRetry = {},
                                onDraftChange = {},
                                onSubmit = {},
                                onReply = {},
                                onCancelReply = {},
                                onLike = {},
                                onDislike = {},
                                onEmoji = {},
                                onAward = {},
                                onOpenUser = {},
                                onOpenLink = {},
                                onOpenWeb = {},
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("展开评论 (0)").performClick()

        composeRule.onNodeWithText("收起评论").assertIsDisplayed()
        composeRule.onNodeWithText("写评论").assertIsDisplayed()
    }

    @Test
    fun collapsedChapterCommentsExpandThroughTheRealReaderGestureLayer() {
        renderFullReader()

        composeRule.onNodeWithText("展开评论 (0)").performTouchInput { click() }

        composeRule.onNodeWithText("收起评论").assertIsDisplayed()
        composeRule.onNodeWithText("写评论").assertIsDisplayed()
    }

    @Test
    fun traditionalModeConvertsTheReaderSettingsSidebar() {
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalChineseVariant provides ChineseVariant.Traditional) {
                    ReaderSettingsControls(
                        options = ReaderUiOptions(),
                        category = ReaderSettingsCategory.Font,
                        textColor = Color.Black,
                        metaColor = Color.DarkGray,
                        onDecreaseFont = {},
                        onIncreaseFont = {},
                        onCycleTheme = {},
                        onOptionsChange = {},
                        onReset = {},
                        onClearCurrentBookCache = {},
                        clearingChapterCache = false,
                        chapterCacheMessage = null,
                    )
                }
            }
        }

        composeRule.onNodeWithText("字體設置").assertIsDisplayed()
    }

    @Test
    fun traditionalModeConvertsTheReaderRadialToolbar() {
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalChineseVariant provides ChineseVariant.Traditional) {
                    ReaderRadialMenu(
                        state = ReaderState(),
                        chapters = emptyList(),
                        favoriteStatus = LoadResult.Success(FavoriteStatus(isFavorited = false)),
                        ttsState = ReaderTtsState.Stopped,
                        showTts = false,
                        onPrevious = {},
                        onNext = {},
                        onCatalog = {},
                        onTts = {},
                        onFavorite = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("閱讀工具").assertIsDisplayed()
    }

    @Test
    fun typographySettingsUseReaderWidthNamesInsteadOfRawDpValues() {
        composeRule.setContent {
            MaterialTheme {
                ReaderSettingsControls(
                    options = ReaderUiOptions(contentWidthDp = 800),
                    category = ReaderSettingsCategory.Typography,
                    textColor = Color.Black,
                    metaColor = Color.DarkGray,
                    onDecreaseFont = {},
                    onIncreaseFont = {},
                    onCycleTheme = {},
                    onOptionsChange = {},
                    onReset = {},
                    onClearCurrentBookCache = {},
                    clearingChapterCache = false,
                    chapterCacheMessage = null,
                )
            }
        }

        composeRule.onNodeWithText("舒适宽度").assertIsDisplayed()
        composeRule.onAllNodesWithText("800 dp").assertCountEquals(0)
    }

    private fun renderFullReader() {
        val content = ReaderContent(
            title = "测试章节",
            content = "<p>这是用于点击回归的短正文。</p>",
            source = "test",
        )
        composeRule.setContent {
            MaterialTheme {
                ReaderScreen(
                    state = ReaderState(
                        bookId = 7L,
                        bookTitle = "测试书籍",
                        chapterId = 8L,
                        content = LoadResult.Success(content),
                        chapterContents = listOf(ReaderChapterContent(8L, "测试章节", content)),
                        chapters = LoadResult.Success(listOf(Chapter(8L, "测试章节", 1))),
                        chapterCommentStates = mapOf(
                            8L to ReaderChapterCommentState(comments = LoadResult.Success(emptyList())),
                        ),
                    ),
                    options = ReaderUiOptions(showTts = false),
                    readerFullscreen = false,
                    onReaderFullscreenChange = {},
                    ttsSettings = ReaderTtsSettings(),
                    catalogQuery = "",
                    onCatalogQueryChange = {},
                    onDecreaseFont = {},
                    onIncreaseFont = {},
                    onCycleTheme = {},
                    onReaderOptionsChange = {},
                    onReaderTtsSettingsChange = {},
                    onResetReaderOptions = {},
                    onClearReaderChapterCache = {},
                    onRetry = {},
                    onRetryChapterComments = {},
                    onRetryCatalog = {},
                    onOpenReader = { _, _ -> },
                    onOpenReaderAtPosition = { _, _, _ -> },
                    onLoadNextChapter = {},
                    onVisibleChapterChanged = { _, _ -> },
                    onToggleFavorite = {},
                    onBack = {},
                    onCommentDraftChange = { _, _ -> },
                    onSubmitComment = {},
                    onReplyComment = { _, _ -> },
                    onCancelCommentReply = {},
                    onCommentLike = { _, _ -> },
                    onCommentDislike = { _, _ -> },
                    onCommentEmoji = { _, _ -> },
                    onCommentAward = { _, _ -> },
                    onOpenUser = {},
                    onOpenLink = {},
                    onOpenWeb = {},
                    onPreviewImage = { _, _ -> },
                )
            }
        }
    }
}
