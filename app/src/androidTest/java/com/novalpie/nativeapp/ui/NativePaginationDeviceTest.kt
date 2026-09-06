package com.novalpie.nativeapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import com.novalpie.nativeapp.feature.reader.pagination.NativePagedReader
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderViewportAnchor
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class NativePaginationDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<com.novalpie.nativeapp.audit.ReaderTestActivity>()

    @Test fun replacementMarkupStaysTextAndReflowDoesNotInventAnIllustrationPage() {
        val chapterId = System.currentTimeMillis()
        val original = ReaderContent(null, "<p>前面 <b>Alice</b> 后面</p>", "test")
        val rules = ReaderReplacementState(novelId = 900109, personalRules = listOf(
            com.novalpie.nativeapp.model.ReaderReplacementRule("fixture", 900109, "Alice", "**名字** ![文字](https://host.test/one.png)"),
        ))
        val derived = effectiveReaderChapterContent(com.novalpie.nativeapp.model.ReaderChapterContent(chapterId, null, original), 1, rules).content
        val anchor = AtomicReference<ReaderViewportAnchor?>()
        compose.setContent { MaterialTheme {
            NativePagedReader(900109, chapterId, original, derived, ReaderUiOptions(pageTurnMode = true, pageTurnEffect = "none", showComments = false),
                ReaderChapterEntryPosition.Start, null, FontFamily.Default, Color.Black, Color.White, false, false,
                onTap = { _, _ -> }, onBoundary = {}, registerTurn = {}, onAnchor = { anchor.set(it) }, onPreview = { _, _ -> }, modifier = Modifier.fillMaxSize()) {}
        } }
        compose.waitUntil(15000) { anchor.get() != null }
        compose.onNodeWithText("**名字**", substring = true).assertExists()
        compose.onNodeWithText("![文字](https://host.test/one.png)", substring = true).assertExists()
        compose.onAllNodesWithText("插图加载失败").assertCountEquals(0)
    }

    @Test fun failedPageIllustrationHasAnExplicitRetryInsteadOfAnEmptyPage() {
        val chapterId=System.currentTimeMillis()
        val content=ReaderContent(null,"<img src='http://127.0.0.1:1/beta7-missing-image.png'><p>图后正文</p>","test")
        compose.activityRule.scenario.onActivity {it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.setContent {MaterialTheme {
            NativePagedReader(900103,chapterId,content,content,
                ReaderUiOptions(pageTurnMode=true,pageTurnEffect="none",showComments=false),
                ReaderChapterEntryPosition.Start,null,FontFamily.Default,Color.Black,Color.White,false,false,
                onTap={_,_->},onBoundary={},registerTurn={},onAnchor={},onPreview={_,_->},modifier=Modifier.fillMaxSize()){}
        }}
        compose.waitUntil(15000){compose.onAllNodesWithText("插图加载失败").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("重试插图").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("插图加载失败").fetchSemanticsNodes().isNotEmpty()}
    }

    @Test fun paginatedTextHonorsTraditionalConversionAndWordSpacingWithRichSpans() {
        val chapterId=System.currentTimeMillis()
        val anchor=AtomicReference<ReaderViewportAnchor?>()
        val content=ReaderContent(null,"<p>龙书 <b>one two</b> 世界</p>","test")
        compose.activityRule.scenario.onActivity {it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.setContent {
            CompositionLocalProvider(LocalChineseVariant provides com.novalpie.nativeapp.model.ChineseVariant.Traditional) {
                MaterialTheme {
                    NativePagedReader(900101,chapterId,content,content,
                        ReaderUiOptions(pageTurnMode=true,pageTurnEffect="none",showComments=false,wordSpacing=6f),
                        ReaderChapterEntryPosition.Start,null,FontFamily.Default,Color.Black,Color.White,false,false,
                        onTap={_,_->},onBoundary={},registerTurn={},onAnchor={anchor.set(it)},onPreview={_,_->},
                        modifier=Modifier.fillMaxSize()){}
                }
            }
        }
        compose.waitUntil(15000){anchor.get()!=null}
        compose.onNodeWithText("龍書",substring=true).assertExists()
        compose.onNodeWithText("one\u200A\u200A\u200Atwo",substring=true).assertExists()
    }

    @Test fun followingSpeechDoesNotRequireHighlightAndReflowKeepsTheVisibleParagraph() {
        val chapterId=System.currentTimeMillis()
        val anchor=AtomicReference<ReaderViewportAnchor?>()
        val phrase="朗读到这一段时应自动来到末尾。"
        val content=ReaderContent(null,"<p>"+"前面是很长的正文，不能遮挡或漏字。".repeat(120)+"</p><p>$phrase</p>","test")
        var follow by mutableStateOf<String?>(null)
        var fontSize by mutableStateOf(20)
        compose.activityRule.scenario.onActivity {it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.setContent {
            MaterialTheme {
                NativePagedReader(900102,chapterId,content,content,
                    ReaderUiOptions(pageTurnMode=true,pageTurnEffect="none",showComments=false,fontSizeSp=fontSize),
                    ReaderChapterEntryPosition.Start,null,FontFamily.Default,Color.Black,Color.White,false,false,
                    onTap={_,_->},onBoundary={},registerTurn={},onAnchor={anchor.set(it)},onPreview={_,_->},
                    modifier=Modifier.fillMaxSize(),followText=follow,highlightText=null){}
            }
        }
        compose.waitUntil(15000){anchor.get()!=null}
        val first=anchor.get()
        compose.runOnIdle{follow=phrase}
        compose.waitUntil(10000){anchor.get()!=first}
        compose.onNodeWithText(phrase,substring=true).assertExists()
        compose.runOnIdle{fontSize=26}
        compose.waitForIdle()
        compose.onNodeWithText(phrase,substring=true).assertExists()
    }

    @Test fun realMeasuredPagesAdvanceReturnAndCrossTheBoundaryOnce() {
        val anchor=AtomicReference<ReaderViewportAnchor?>()
        val boundaries=CopyOnWriteArrayList<Int>()
        val chapterId=System.currentTimeMillis()
        val content=ReaderContent("分页测试","<p>这是一段必须跨越多页的中文长段落。🙂".plus("真实行边界不能截断，也不能跳过或重复。".repeat(90))+"</p><p>最后一段完整可见。</p>","test")
        compose.activityRule.scenario.onActivity {it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.setContent {
            MaterialTheme {
                var turn by remember {mutableStateOf<((Int)->Unit)?>(null)}
                Column(Modifier.fillMaxSize()) {
                    Row {
                        TextButton(onClick={turn?.invoke(-1)}){Text("测试上一页")}
                        TextButton(onClick={turn?.invoke(1)}){Text("测试下一页")}
                    }
                    NativePagedReader(900100,chapterId,content,content,
                        ReaderUiOptions(pageTurnMode=true,useInfiniteScroll=false,pageTurnEffect="none",showComments=false),
                        ReaderChapterEntryPosition.Start,null,FontFamily.Default,Color.Black,Color.White,true,true,
                        onTap={_,_->},onBoundary={boundaries.add(it)},registerTurn={turn=it},onAnchor={anchor.set(it)},onPreview={_,_->},
                        modifier=Modifier.weight(1f)){}
                }
            }
        }
        compose.waitUntil(15000){anchor.get()!=null}
        val first=anchor.get()
        compose.onNodeWithText("测试下一页").performClick()
        compose.waitUntil(5000){anchor.get()!=first}
        val second=anchor.get()
        compose.onNodeWithText("测试上一页").performClick()
        compose.waitUntil(5000){anchor.get()==first}
        assertEquals(first,anchor.get())
        compose.onNodeWithText("测试下一页").performClick()
        compose.waitUntil(5000){anchor.get()==second}
        compose.onRoot().performTouchInput{swipeLeft()}
        compose.waitUntil(5000){anchor.get()!=second}
        compose.onRoot().performTouchInput{swipeRight()}
        compose.waitUntil(5000){anchor.get()==second}
        repeat(70) {
            if(boundaries.isEmpty())compose.onNodeWithText("测试下一页").performClick()
        }
        assertEquals(listOf(1),boundaries)
        repeat(3){compose.onNodeWithText("测试下一页").performClick()}
        assertEquals("同一章尾不能重复发起跨章",listOf(1),boundaries)
    }
}
