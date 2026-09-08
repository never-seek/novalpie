package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Feedback1936 source comparison: GETs only, no progress/cache write, no remote media download. */
class SourceImageFeedbackReadDeviceTest {
    @Test fun inspectCurrentSourceAroundTheReportedEightySecondChapter() = runBlocking {
        val bookId = InstrumentationRegistry.getArguments().getString("bookId")?.toLongOrNull()
        require(bookId == 360516L) { "This read-only sample must be explicitly selected" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer.from(context)
        container.refreshEnvironmentFromStores()
        val chapters = container.api.chapters(bookId)
        val candidates = chapters.filter { chapter -> chapter.number in 81..83 || Regex("(?:EP\\s*|第)0?82(?:[.．章\\s]|$)", RegexOption.IGNORE_CASE).containsMatchIn(chapter.title) }
            .distinctBy { it.id }.take(6)
        assertTrue("源目录未找到报告章节，不能凭列表下标猜", candidates.isNotEmpty())
        val samples = JSONArray()
        for (chapter in candidates) {
            val content = container.api.chapterContent(chapter.id, showImages = true)
            val blocks = readerBlocksForContent(content)
            val images = blocks.filterIsInstance<ReaderContentBlock.Image>()
            samples.put(JSONObject().put("chapterId", chapter.id).put("number", chapter.number)
                .put("title", chapter.title).put("catalogImageCount", chapter.imageCount ?: JSONObject.NULL)
                .put("updatedAt", chapter.updatedAt ?: JSONObject.NULL).put("sourceIllustrations", content.illustrations.size)
                .put("renderedImageOccurrences", images.size).put("validImageUrls", images.all { it.url.startsWith("https://") || it.url.startsWith("http://") })
                .put("markupHasImageTag", content.content.contains("<img", ignoreCase = true)).put("sourceTextChars", content.content.length))
        }
        val report = JSONObject().put("bookId", bookId).put("feedbackId", 1936).put("readVerified", true)
            .put("reportedBugFixed", JSONObject.NULL).put("samples", samples).put("mediaDownloads", 0).put("writes", 0)
        val folder = File(context.cacheDir, "beta7-source-feedback").apply { mkdirs() }
        File(folder, "images-1936.json").writeText(report.toString(2))
        Unit
    }
}
