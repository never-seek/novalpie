package com.novalpie.nativeapp.feature.download

import com.novalpie.nativeapp.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class TaskExportImageReconcilerTest {
    @get:Rule val temp = TemporaryFolder()
    private val url = "https://images.test/one.webp"
    @Test fun transientSourceReadGetsOnlyOneRetryAndConfirmedPermissionsNeverRetry() = runBlocking {
        var reads = 0
        val checker = TaskExportImageReconciler(temp.newFolder()) {
            reads++
            if (reads == 1) throw NovalPieApiException(500, "/api/chapters/1/content", "fixture")
            listOf(url)
        }
        val body = "[图片: $url]\n[图片: $url]"
        assertEquals(1, Regex("\\[图片").findAll(checker.reconcile(1, body)).count())
        assertEquals(2, reads)
        var rejected = 0
        val forbidden = TaskExportImageReconciler(temp.newFolder()) { rejected++; throw NovalPieApiException(403, "/api/chapters/1/content", "fixture") }
        assertTrue(runCatching { forbidden.reconcile(1, body) }.isFailure)
        assertEquals(1, rejected)
    }
    @Test fun correctedArchiveKeepsOneVerifiedOccurrenceAndDoesNotRefetchQuotaOnRetry() = runBlocking {
        var sourceReads = 0
        val reconciler = TaskExportImageReconciler(temp.newFolder()) { sourceReads++; listOf(url) }
        val original = "正文前\n[图片: $url]\n正文后\n[图片: $url]"
        val bytes = byteArrayOf(82,73,70,70,1,0,0,0,87,69,66,80)
        repeat(2) {
            val out = ByteArrayOutputStream()
            NativeEpubArchiveWriter.write(out, NativeEpubMetadata("测试", "作者"), StringReader("第1章 章节\n$original"),
                openAsset = { NativeEpubAsset("image/webp", bytes.inputStream()) },
                reconcileSourceImages = reconciler::reconcile)
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip -> while (true) {
                val entry = zip.nextEntry ?: break; entries[entry.name] = zip.readBytes(); zip.closeEntry()
            } }
            val image = entries.filterKeys { it.startsWith("OEBPS/images/") }.values.single()
            assertArrayEquals(bytes, image)
            val body = entries.getValue("OEBPS/chapter-1.xhtml").toString(Charsets.UTF_8)
            assertEquals(1, Regex("<img ").findAll(body).count())
            assertTrue(body.indexOf("正文前") < body.indexOf("<img "))
            assertTrue(body.indexOf("<img ") < body.indexOf("正文后"))
        }
        assertEquals(1, sourceReads)
    }
    @Test fun chaptersWithoutDuplicatedMarkersDoNotNeedExtraSourceRequests() = runBlocking {
        val reconciler = TaskExportImageReconciler(temp.newFolder()) { error("No extra requests") }
        val body = "正文\n[图片: $url]"
        assertEquals(body, reconciler.reconcile(1, body))
    }
    @Test fun precheckIsParallelButBoundedAndResumesFromSavedChapterSnapshots() = runBlocking {
        val active = java.util.concurrent.atomic.AtomicInteger()
        val maximum = java.util.concurrent.atomic.AtomicInteger()
        val reads = java.util.concurrent.atomic.AtomicInteger()
        val reconciler = TaskExportImageReconciler(temp.newFolder()) {
            reads.incrementAndGet()
            val now = active.incrementAndGet(); maximum.updateAndGet { maxOf(it, now) }
            try { kotlinx.coroutines.delay(30); listOf(url) } finally { active.decrementAndGet() }
        }
        val source = (1..20).joinToString("\n") { "第${it}章\n[图片: $url]\n正文\n[图片: $url]" }
        reconciler.prepare(StringReader(source), 999)
        assertTrue(maximum.get() in 2..4)
        assertEquals(20, reads.get())
        reconciler.prepare(StringReader(source), 999)
        assertEquals(20, reads.get())
    }
}
