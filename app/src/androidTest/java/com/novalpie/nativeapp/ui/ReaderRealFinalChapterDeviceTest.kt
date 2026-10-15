package com.novalpie.nativeapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.MainActivity
import com.novalpie.nativeapp.model.*
import org.junit.*
import org.junit.Assert.*
import org.json.JSONObject
import java.io.File
import android.graphics.Bitmap
import androidx.compose.ui.semantics.getOrNull

/** Actual user-identified book; reads only, never submits translation/comments or edits content. */
class ReaderRealFinalChapterDeviceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun romanticFantasyNtrLastChapterShowsTerminalStateWithoutManualRetry() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveNovalPie")=="1")
        val vm=ViewModelProvider(compose.activity)[NovalPieViewModel::class.java]
        val app=InstrumentationRegistry.getInstrumentation().targetContext
        val report=JSONObject().put("bookId",358153).put("chapterId",11725813)
        compose.runOnIdle {vm.openReader(358153,11725813)}
        compose.waitUntil(150_000){vm.readerState.content is LoadResult.Success&&vm.readerState.chapters is LoadResult.Success}
        compose.runOnIdle {
            val chapters=(vm.readerState.chapters as LoadResult.Success).value
            assertEquals(11725813L,chapters.last().id)
            report.put("catalogSize",chapters.size)
        }
        // Source success precedes off-main parsing/layout; do not race the single loading item.
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("reader-continuous-list").fetchSemanticsNodes().any {
                it.config.getOrNull(ReaderWindowChapterIds)?.contains(11725813L)==true
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("本章结束"))
        compose.waitUntil(30_000){vm.readerState.nextChapterExhausted}
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("已读完全部章节"))
        compose.onNodeWithText("已读完全部章节").assertExists()
        compose.onNodeWithText("继续下滑加载下一章 · 点此重试").assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(vm.readerState.catalogRefreshing);assertFalse(vm.readerState.nextChapterWaitingForCatalog)
            report.put("endConfirmedAutomatically",true).put("retryClicked",false)
            File(app.getExternalFilesDir(null),"feedback-real-final-reader.json").writeText(report.toString(2))
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let {bitmap->
            File(app.getExternalFilesDir(null),"feedback-real-final-reader.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
}
