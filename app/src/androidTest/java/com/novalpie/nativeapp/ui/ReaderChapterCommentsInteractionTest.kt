package com.novalpie.nativeapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsActions
import com.novalpie.nativeapp.data.ReaderTtsSettings
import com.novalpie.nativeapp.model.Chapter
import com.novalpie.nativeapp.model.ChineseVariant
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.model.ReaderChapterContent
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderViewportAnchor
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.feature.reader.tts.SpeechStatus

class ReaderChapterCommentsInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @After fun removeOnlyLegacySyntheticSpeechProgress() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=com.novalpie.nativeapp.data.ReaderProgressStore(context)
        val fixture=store.load(7)
        if(fixture?.bookTitle=="测试书籍"&&fixture.chapterId==8L) {
            store.clear(7)
            context.getSharedPreferences("novalpie_native_reader_anchors",android.content.Context.MODE_PRIVATE)
                .edit().remove("book_7_chapter_8").apply()
        }
    }

    @Before fun matchHandsetActivityOrientation() {
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        composeRule.waitUntil(10000) {
            composeRule.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT
        }
        composeRule.waitForIdle()
    }

    @Test
    fun collapsedChapterCommentsExpandWithoutRequiringSelectableArticleText() {
        composeRule.setContent {
            MaterialTheme {
                val inlineInteractionActive = remember { AtomicBoolean(false) }
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

        composeRule.onNodeWithText("展开评论 (0)").performClick()

        composeRule.onNodeWithText("收起评论").assertIsDisplayed()
        composeRule.onNodeWithText("写评论").assertIsDisplayed()
    }

    @Test
    fun collapsedChapterCommentsExpandThroughTheRealReaderGestureLayer() {
        renderFullReader()

        composeRule.onNodeWithText("展开评论 (0)").performTouchInput { click() }

        composeRule.onNodeWithText("收起评论").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("写评论").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun visibleReaderTtsButtonStartsApplicationServiceAndSurvivesLeavingThePage() {
        val playback=AppContainer.from(composeRule.activity).playback
        renderFullReader(showTts=true)
        try {
            composeRule.onRoot().performTouchInput { click(center) }
            composeRule.waitForIdle()
            composeRule.onRoot(useUnmergedTree=true).printToLog("Beta7ReaderUiTest")
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
                java.io.File(composeRule.activity.cacheDir,"beta7-reader-tts-button.png").outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
                }
            }
            composeRule.onNodeWithText("听书",useUnmergedTree=true).performClick()
            composeRule.waitUntil(15000) { playback.state.value.status == SpeechStatus.Speaking }
            assertEquals(8L,playback.state.value.chapter?.chapterId)
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 3").close()
            android.os.SystemClock.sleep(500)
            assertEquals(SpeechStatus.Speaking,playback.state.value.status)
        } finally { InstrumentationRegistry.getInstrumentation().runOnMainSync {playback.stop()} }
    }

    @Test fun fullReaderPageModeUsesTheMeasuredPlanForBothTapDirections() {
        val anchor=java.util.concurrent.atomic.AtomicReference<com.novalpie.nativeapp.model.ReaderViewportAnchor?>()
        renderFullReader(paged=true,onAnchor={anchor.set(it)})
        composeRule.waitUntil(15000){anchor.get()!=null}
        val first=anchor.get()
        composeRule.onRoot().performTouchInput {click(androidx.compose.ui.geometry.Offset(width*.9f,height*.5f))}
        composeRule.waitUntil(5000){anchor.get()!=first}
        composeRule.onRoot().performTouchInput {click(androidx.compose.ui.geometry.Offset(width*.1f,height*.5f))}
        composeRule.waitUntil(5000){anchor.get()==first}
        assertEquals(first,anchor.get())
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
    fun articleLongPressDoesNotRestoreCopySelectionOrTheRemovedRadialMenu() {
        renderFullReader()
        composeRule.onNodeWithText("这是用于点击回归的短正文。", substring = true)
            .performTouchInput { longClick() }
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection)).assertCountEquals(0)
        composeRule.onAllNodesWithText("复制").assertCountEquals(0)
        composeRule.onAllNodesWithText("全选").assertCountEquals(0)
        composeRule.onAllNodesWithText("阅读工具").assertCountEquals(0)
    }

    @Test
    fun layoutSettingsUseReaderWidthNamesAndRemainReachableOnNarrowScreens() {
        composeRule.setContent {
            MaterialTheme {
                // The production ReaderSettingsSheet owns this scroll container. Test the same
                // contract so narrow screens can reach all settings instead of rendering an
                // unconstrained child that is inevitably clipped at the activity boundary.
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                ReaderSettingsControls(
                    options = ReaderUiOptions(contentWidthDp = 800),
                    category = ReaderSettingsCategory.Layout,
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

        composeRule.onNodeWithText("舒适宽度").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("800 dp").assertCountEquals(0)
    }

    private fun renderFullReader(showTts: Boolean = false,paged:Boolean=false,onAnchor:(ReaderViewportAnchor)->Unit={}) {
        val content = ReaderContent(
            title = "测试章节",
            content = "<p>这是用于点击回归的短正文。</p>" + if (showTts||paged) {
                (1..20).joinToString("") { "<p>后台朗读验证段落$it，离开页面之后也应该保持朗读，不重复或漏掉句子。</p>" }
            } else "",
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
                    options = ReaderUiOptions(showTts = showTts,pageTurnMode=paged,useInfiniteScroll=!paged,pageTurnEffect="none"),
                    readerFullscreen = false,
                    onReaderFullscreenChange = {},
                    ttsSettings = ReaderTtsSettings(),
                    replacementState = ReaderReplacementState(novelId = 7L),
                    catalogQuery = "",
                    onCatalogQueryChange = {},
                    onDecreaseFont = {},
                    onIncreaseFont = {},
                    onCycleTheme = {},
                    onReaderOptionsChange = {},
                    onReaderTtsSettingsChange = {},
                    onReaderReplacementSourceChange = {},
                    onSaveReaderReplacementRule = {},
                    onDeleteReaderReplacementRule = {},
                    onSetSharedReaderReplacementRuleVisible = { _, _ -> },
                    onCloneSharedReaderReplacementRule = {},
                    onReaderSharedRulesEnabledChange = {},
                    onDefaultReaderSharedRulesEnabledChange = {},
                    onResetReaderSharedRulesOverride = {},
                    onResetReaderOptions = {},
                    onClearReaderChapterCache = {},
                    onRetry = {},
                    onRetryChapterComments = {},
                    onRetryCatalog = {},
                    onOpenReader = { _, _ -> },
                    onOpenReaderAtPosition = { _, _, _ -> },
                    onLoadNextChapter = {},
                    onVisibleChapterChanged = { _, _ -> },
                    onViewportAnchorChanged = onAnchor,
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
                    recordPlaybackProgress=false,
                )
            }
        }
    }
}
