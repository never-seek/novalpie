package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

/** Read-only diagnosis of the specific whole-book reconciliation mismatch; exports no prose. */
class ReaderImageShapeDeviceTest {
    @Test fun describeTheNonHttpImageInChapter1035WithoutExportingItsPayload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val api = AppContainer.from(context).api
        val chapter = api.chapters(350192).first { it.number == 1035 }
        val content = api.chapterContent(chapter.id)
        val report = JSONObject().put("bookId", 350192).put("chapterId", chapter.id).put("chapterNumber", 1035)
        report.put("emptySrcTags", Regex("""<img\b[^>]*?\ssrc\s*=\s*["']\s*["']""", RegexOption.IGNORE_CASE).findAll(content.content).count())
        val images = JSONArray()
        for (image in readerBlocksForContent(content).filterIsInstance<ReaderContentBlock.Image>()) {
            val url = image.originalUrl ?: image.url
            if (url.startsWith("data:", true)) {
                val entry = JSONObject().put("scheme", "data").put("header", url.substringBefore(',')).put("encodedLength", url.length)
                if (url.substringBefore(',').endsWith(";base64")) {
                    val bytes = android.util.Base64.decode(url.substringAfter(','), android.util.Base64.DEFAULT)
                    val size = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, size)
                    entry.put("width", size.outWidth).put("height", size.outHeight).put("mime", size.outMimeType)
                }
                images.put(entry)
            } else {
                val uri = android.net.Uri.parse(url)
                images.put(JSONObject().put("scheme", uri.scheme).put("host", uri.host)
                    .put("path", uri.encodedPath?.take(260)).put("length", url.length)
                    .put("hasWhitespace", url.any(Char::isWhitespace)).put("hasOriginal", image.originalUrl != null))
            }
        }
        report.put("images", images)
        val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        File(folder, "reader-image-shape.json").writeText(report.toString(2))
    }
}
