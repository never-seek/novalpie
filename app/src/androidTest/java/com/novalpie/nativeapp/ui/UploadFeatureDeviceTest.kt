package com.novalpie.nativeapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.feature.upload.*
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production form and feature, synthetic submissions only; never writes a real user's book. */
class UploadFeatureDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ReaderTestActivity>()
    @Test fun statusAndUncertainRetryAreUsableInTheActualAndroidForm() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val requests = mutableListOf<UploadSubmission>()
        val repository = object : UploadRepository {
            override suspend fun document(uri: String) = error("No file access in this UI case")
            override suspend fun parse(document: UploadDocument) = error("No parser in this UI case")
            override suspend fun submit(submission: UploadSubmission): UploadActionResult {
                requests += submission
                if (requests.size == 1) throw java.io.IOException("synthetic response interruption")
                return UploadActionResult(true, "受控上传回执", 900013)
            }
        }
        val model = UploadBookViewModel(repository, scope)
        try {
            compose.runOnIdle { model.adopt(UploadBookState(
                draft = UploadBookDraft(title = "Beta7受控测试书", author = "合成作者", isAdult = true, chapterCount = 1),
                selectedFile = UploadDocument("content://unused/synthetic.epub", "synthetic.epub", 100),
                chapters = LoadResult.Success(listOf(UploadChapter("第一章", "合成内容", 1))), serverFilePath = "synthetic/owned.epub")) }
            compose.setContent { MaterialTheme { UploadBookScreen(model.state, true, {}, model::select, model::draft,
                { model.submit() }, model::clear, {}, {}, { model.submit(confirmUncertainRetry = true) }) } }
            compose.onNodeWithText("已完结").performScrollTo().performClick()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("确认上传 1 章"))
            compose.onNodeWithText("确认上传 1 章").performClick()
            compose.waitUntil(5000) { model.state.submissionUncertain }
            compose.runOnIdle {
                assertEquals(1, requests.size)
                assertEquals("19 完结", requests.single().request.spans)
                assertEquals("synthetic/owned.epub", requests.single().request.epubFilePath)
                assertEquals("Beta7受控测试书", model.state.draft.title)
            }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("核对后重试"))
            compose.onNodeWithText("核对后重试").performClick()
            compose.onNodeWithText("返回核对").performClick()
            compose.runOnIdle { assertEquals(1, requests.size) }
            compose.onNodeWithText("核对后重试").performClick()
            compose.onNodeWithText("已核对，仍需重试").performClick()
            compose.waitUntil(5000) { model.state.submitResult is LoadResult.Success }
            compose.runOnIdle { assertEquals(2, requests.size); assertFalse(model.state.processing) }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("书籍 ID：900013"))
            compose.onNodeWithText("书籍 ID：900013").assertIsDisplayed()
        } finally { model.close(); scope.cancel() }
    }
}
