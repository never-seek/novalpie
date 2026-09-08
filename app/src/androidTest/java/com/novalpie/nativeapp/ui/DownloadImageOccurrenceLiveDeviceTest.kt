package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.feature.download.DownloadFormat
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Authorized TXT stream only: compares source image occurrences with real chapter JSON, no images downloaded. */
class DownloadImageOccurrenceLiveDeviceTest {
    @Test fun compareDuplicatedExportMarkersWithTheChapterReaderSource() = runBlocking {
        val bookId = InstrumentationRegistry.getArguments().getString("bookId")?.toLongOrNull() ?: error("Explicit book required")
        require(bookId == 350192L) { "Only the previously authorized large QA book is in scope" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer.from(context)
        val account = container.api.currentUser().id ?: error("Existing session required")
        val temp = File(context.cacheDir, "beta7-image-occurrence-${UUID.randomUUID()}.txt")
        val packageFile = File(context.cacheDir, temp.name + ".epub")
        val packageRoot = File(context.cacheDir, temp.name + "-work").apply { mkdirs() }
        var authorizations = 0
        try {
            val previous = container.downloadStore.recover(account).tasks.firstOrNull { it.bookId == bookId && it.format == DownloadFormat.Epub && it.authorizationFile != null }
            var ticket = previous?.authorizationFile
            if (ticket == null) { authorizations++; ticket = container.api.requestEpubDownload(bookId).fileName }
            try {
                temp.outputStream().use { output -> container.api.streamDownloadFile(ticket!!) { input -> input.copyTo(output, 65536) } }
            } catch (expired: NovalPieApiException) {
                // Explicitly authorized QA only. A source 410 is a confirmed expiry, not an
                // uncertain authorization response. Never add this as blind production retry.
                if (expired.statusCode !in setOf(404, 410) || authorizations != 0) throw expired
                authorizations++
                ticket = container.api.requestEpubDownload(bookId).fileName
                temp.outputStream().use { output -> container.api.streamDownloadFile(ticket!!) { input -> input.copyTo(output, 65536) } }
            }
            assertTrue(temp.length() > 0)
            val catalog = container.api.chapters(bookId)
            data class Candidate(val number: Int, val urls: List<String>, val adjacent: Int, val body: String)
            val candidates = mutableListOf<Candidate>()
            val markers = Regex("\\[图片(?:[:：]|\\s)*?(.*?)\\]")
            var total = 0; var adjacentTotal = 0; var chapters = 0
            val discard = object : java.io.Writer() { override fun write(cbuf: CharArray, off: Int, len: Int) {} ; override fun flush() {}; override fun close() {} }
            temp.reader(Charsets.UTF_8).use { reader -> NativeEpubArchiveWriter.writeTransformedTxt(discard, reader, { number, title, body ->
                chapters++
                val matches = markers.findAll(body).toList()
                val urls = matches.map { it.groupValues[1].trim().trimStart(':', '：').trim() }.filter { it.isNotBlank() }
                val adjacent = matches.zipWithNext().count { (a, b) -> a.groupValues[1].trim().isNotBlank() &&
                    a.groupValues[1].trim() == b.groupValues[1].trim() && body.substring(a.range.last + 1, b.range.first).isBlank() }
                total += urls.size; adjacentTotal += adjacent
                if (urls.size != urls.distinct().size && candidates.size < 4) candidates += Candidate(number, urls, adjacent, body)
                NativeDownloadChapterText(title, body)
            }) }
            val reports = JSONArray()
            val expectedByChapter = mutableMapOf<Int, List<String>>()
            for (candidate in candidates) {
                val chapter = catalog.firstOrNull { it.number == candidate.number } ?: catalog.getOrNull(candidate.number - 1) ?: error("Chapter identity not found")
                val content = container.api.chapterContent(chapter.id)
                val rendered = readerBlocksForContent(content).filterIsInstance<ReaderContentBlock.Image>().map { it.url }
                expectedByChapter[candidate.number] = rendered
                val corrected = com.novalpie.nativeapp.feature.download.reconcileExportImageOccurrences(candidate.body, rendered)
                assertEquals(rendered.size, markers.findAll(corrected.body).count { it.groupValues[1].trim().trimStart(':', '：').trim().isNotBlank() })
                reports.put(JSONObject().put("chapterNumber", candidate.number).put("chapterId", chapter.id)
                    .put("exportOccurrences", candidate.urls.size).put("exportDistinct", candidate.urls.distinct().size)
                    .put("adjacentExportDuplicates", candidate.adjacent).put("readerOccurrences", rendered.size)
                    .put("readerDistinct", rendered.distinct().size).put("serverIllustrations", content.illustrations.size)
                    .put("identicalSequence", rendered == candidate.urls).put("removedExtraCopies", corrected.removed)
                    .put("correctedOccurrences", rendered.size).put("correctionVerified", true))
            }
            val report = JSONObject().put("bookId", bookId).put("chapters", chapters).put("exportImageOccurrences", total)
                .put("adjacentExportDuplicates", adjacentTotal).put("newAuthorizations", authorizations).put("imageRequests", 0).put("samples", reports)
            if (InstrumentationRegistry.getArguments().getString("verifyFullImagePrecheck") == "true") {
                val checkFolder = File(packageRoot, "whole-quotas")
                val reads = java.util.concurrent.atomic.AtomicInteger()
                val precheckStarted = android.os.SystemClock.elapsedRealtime()
                val checker = com.novalpie.nativeapp.feature.download.TaskExportImageReconciler(checkFolder) { number ->
                    val chapter = catalog.firstOrNull { it.number == number }
                        ?: catalog.getOrNull(number - 1)?.takeIf { it.number == null } ?: error("Unknown chapter number $number")
                    reads.incrementAndGet()
                    readerBlocksForContent(container.api.chapterContent(chapter.id, showImages = true))
                        .filterIsInstance<ReaderContentBlock.Image>().map { it.originalUrl ?: it.url }
                }
                try {
                    val sourceOnly = withTimeout(15 * 60 * 1000L) { temp.reader(Charsets.UTF_8).use { checker.prepare(it, 4) } }
                    report.put("sourceOnlyOccurrences", sourceOnly)
                    report.put("fullPrecheck", true)
                } catch (failure: Exception) {
                    report.put("fullPrecheck", false).put("fullPrecheckError", failure.javaClass.simpleName + ": " + failure.message?.take(180))
                    throw failure
                } finally {
                    val entries = checkFolder.listFiles().orEmpty().filter { it.extension == "json" && !it.name.endsWith("-mismatch.json") }.map { JSONObject(it.readText()) }
                    val mismatches = checkFolder.listFiles().orEmpty().filter { it.name.endsWith("-mismatch.json") }.map { JSONObject(it.readText()) }
                    report.put("mismatches", JSONArray(mismatches))
                    report.put("precheckReads", reads.get()).put("precheckedChapters", entries.size)
                        .put("removedExtraOccurrences", entries.sumOf { it.getInt("removed") })
                        .put("precheckElapsedMs", android.os.SystemClock.elapsedRealtime() - precheckStarted)
                    val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
                    File(folder, "image-occurrence-live.json").writeText(report.toString(2))
                }
            }
            if (InstrumentationRegistry.getArguments().getString("verifyImageArchive") == "true") {
                val hashes = ConcurrentHashMap<String, String>()
                fun digest(file: File): String = file.inputStream().use { input ->
                    val sha = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(65536)
                    while (true) { val n = input.read(buffer); if (n < 0) break; sha.update(buffer, 0, n) }
                    sha.digest().joinToString("") { "%02x".format(it) }
                }
                val imageReconciler = com.novalpie.nativeapp.feature.download.TaskExportImageReconciler(File(packageRoot, "quotas")) { number -> expectedByChapter.getValue(number) }
                packageFile.outputStream().use { output -> NativeEpubArchiveWriter.write(output,
                    NativeEpubMetadata("Beta7图片重复验收片段", "受控验收"),
                    java.io.StringReader(candidates.joinToString("\n\n") { "第${it.number}章 验收片段\n${it.body}" }),
                    openAsset = { url ->
                        val file = File.createTempFile("asset-", ".bin", packageRoot)
                        var mime: String? = null
                        file.outputStream().use { out -> container.api.streamAsset(url) { input, type -> mime = type; input.copyTo(out, 65536) } }
                        hashes[url] = digest(file)
                        NativeEpubAsset(mime, file.inputStream(), onConsumed = { file.delete() })
                    }, reconcileSourceImages = imageReconciler::reconcile, stagingDirectory = File(packageRoot, "stage").apply { mkdirs() }) }
                var imageFiles = 0; var bodyReferences = 0; var chapterFiles = 0
                ZipInputStream(packageFile.inputStream()).use { zip -> while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name.startsWith("OEBPS/images/")) {
                        imageFiles++
                        val sha = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(65536)
                        while (true) { val n = zip.read(buffer); if (n < 0) break; sha.update(buffer, 0, n) }
                        assertTrue(sha.digest().joinToString("") { "%02x".format(it) } in hashes.values)
                    } else if (entry.name.matches(Regex("OEBPS/chapter-\\d+\\.xhtml"))) {
                        chapterFiles++; bodyReferences += Regex("<img ").findAll(zip.readBytes().toString(Charsets.UTF_8)).count()
                    }
                    zip.closeEntry()
                } }
                assertEquals(candidates.size, chapterFiles)
                assertEquals(expectedByChapter.values.sumOf { it.size }, imageFiles)
                assertEquals(imageFiles, bodyReferences)
                report.put("imageRequests", hashes.size).put("archiveVerified", true).put("archiveBytes", packageFile.length())
                    .put("archiveImageFiles", imageFiles).put("archiveImageReferences", bodyReferences).put("originalBytesVerified", true)
            }
            val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
            File(folder, "image-occurrence-live.json").writeText(report.toString(2))
        } finally {
            check(temp.canonicalFile.parentFile == context.cacheDir.canonicalFile && temp.name.startsWith("beta7-image-occurrence-"))
            temp.delete()
            packageFile.delete()
            check(packageRoot.canonicalFile.parentFile == context.cacheDir.canonicalFile && packageRoot.name.startsWith("beta7-image-occurrence-"))
            packageRoot.deleteRecursively()
        }
    }
}
