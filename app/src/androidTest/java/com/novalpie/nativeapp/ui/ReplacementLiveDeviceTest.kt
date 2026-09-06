package com.novalpie.nativeapp.ui

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.audit.ReaderTestActivity
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.feature.download.*
import com.novalpie.nativeapp.feature.reader.replacement.ReplacementRemoteWriter
import com.novalpie.nativeapp.feature.reader.tts.buildSpeechChapter
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

/** Explicit opt-in: publishes ONE marked transient rule, removes it and only its own downloads. */
class ReplacementLiveDeviceTest {
    @Test fun aRealPublicRuleControlsReadingSpeechAndBothNativeExportFormats() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        require(args.getString("allowPublicRuleWrite") == "true") { "Public-rule write needs explicit opt-in" }
        val bookId = args.getString("bookId")?.toLongOrNull() ?: error("Explicit test book required")
        val chapterId = args.getString("chapterId")?.toLongOrNull() ?: error("Explicit test chapter required")
        require(bookId == 353686L) { "Only the user-scoped small QA book is allowed" }
        val context = instrumentation.targetContext
        val container = AppContainer.from(context)
        container.refreshEnvironmentFromStores()
        val account = container.api.currentUser().id ?: error("Existing authenticated app session is required")
        val book = container.api.bookDetail(bookId)
        val chapters = container.api.chapters(bookId)
        require(chapters.size in 2..30)
        val order = chapters.indexOfFirst { it.id == chapterId }.takeIf { it >= 0 }?.plus(1) ?: error("Chapter not in source directory")
        val content = container.api.chapterContent(chapterId)
        val personal = container.api.personalGlossaries(bookId)
        val taken = personal.map { it.source }.toSet()
        val source = readerBlocksForContent(content).filterIsInstance<ReaderContentBlock.Text>().asSequence()
            .flatMap { Regex("[\\p{IsHan}]{6,12}").findAll(it.value).map { found -> found.value } }
            .firstOrNull { it !in taken } ?: error("No safe unique source phrase; no rule was published")
        val marker = "【Beta7临时验收-${UUID.randomUUID().toString().take(8)}】"
        val testRule = ReaderReplacementRule("beta7-qa-${UUID.randomUUID()}", bookId, source, source + marker)
        val scenario = ActivityScenario.launch(ReaderTestActivity::class.java)
        val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        val cleanupCheckpoint = File(folder, "public-replacement-pending.json")
        val report = JSONObject().put("bookId", bookId).put("chapterId", chapterId).put("formats", JSONArray())
        val ownedUris = mutableListOf<Uri>()
        val createdIds = mutableSetOf<Long>()
        val started = android.os.SystemClock.elapsedRealtime()
        var succeeded = false
        cleanupCheckpoint.writeText(JSONObject().put("bookId", bookId).put("testMarker", marker).put("createdIds", JSONArray()).toString(2))
        try {
            ReplacementRemoteWriter(container.replacementRemoteRepository).execute(ReaderReplacementRemoteSyncAction.Create(testRule)) { rule ->
                rule.websiteRuleId?.let(createdIds::add)
                cleanupCheckpoint.writeText(JSONObject().put("bookId", bookId).put("testMarker", marker).put("createdIds", JSONArray(createdIds.toList())).toString(2))
            }
            assertEquals(1, createdIds.size)
            val id = createdIds.single()
            val shared = withTimeout(30000) {
                var found: List<ReaderReplacementRule>
                do {
                    found = container.api.sharedGlossaries(bookId)
                    if (found.none { it.id == "shared:$id" }) delay(500)
                } while (found.none { it.id == "shared:$id" })
                found
            }
            val public = shared.first { it.id == "shared:$id" }
            assertEquals(testRule.replacement, public.replacement)
            val state = ReaderReplacementState(novelId = bookId, sharedRules = LoadResult.Success(shared), sharedRulesEnabledOverride = true)
            val chapter = ReaderChapterContent(chapterId, content.title, content)
            val rendered = effectiveReaderChapterContent(chapter, order, state)
            assertTrue(rendered.content.content.contains(marker))
            assertFalse(effectiveReaderChapterContent(chapter, order, state.copy(sharedRulesEnabledOverride = false)).content.content.contains(marker))
            assertFalse(effectiveReaderChapterContent(chapter, order, state.copy(hiddenSharedRuleIds = setOf(public.id))).content.content.contains(marker))
            val spoken = buildSpeechChapter(bookId, chapterId, book.title, rendered.content, null, order, chapters.size, 1)
            assertTrue(spoken.segments.any { it.contains(marker) })
            assertEquals(content.content, container.api.chapterContent(chapterId).content)
            report.put("publicVisible", true).put("bookPolicyDisabled", true).put("singleRuleHidden", true).put("speechTextReplaced", true).put("sourceUnchanged", true)

            for (format in listOf(DownloadFormat.Txt, DownloadFormat.Epub)) for (replace in listOf(false, true)) {
                val task = DownloadTask("beta7-public-${UUID.randomUUID()}", account, bookId, book.title, format,
                    applyReplacement = replace, replacementSnapshot = if (replace) DownloadRulesSnapshot.encode(state) else null, requestedConcurrency = 8)
                scenario.onActivity { NativeDownloadService.start(it, task) }
                val complete = withTimeout(180000) { container.downloads.state.first { it.task?.id == task.id && !it.busy } }
                assertEquals(DownloadPhase.Completed, complete.task?.phase)
                val uri = Uri.parse(complete.task!!.destinationUri).also(ownedUris::add)
                var markerFound = false
                var xhtmlCount = 0
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    if (format == DownloadFormat.Txt) {
                        input.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.forEach { if (it.contains(marker)) markerFound = true } }
                    } else {
                        ZipInputStream(input).use { zip ->
                            while (true) {
                                val entry = zip.nextEntry ?: break
                                if (entry.name.endsWith(".xhtml")) {
                                    xhtmlCount++
                                    // This opt-in sample has 9 short text chapters; no giant package is read into memory.
                                    val text = zip.readBytes().toString(Charsets.UTF_8)
                                    if (text.contains(marker)) markerFound = true
                                }
                                zip.closeEntry()
                            }
                        }
                        assertTrue(xhtmlCount >= chapters.size)
                    }
                }
                assertEquals("导出替换模式必须与选择一致", replace, markerFound)
                report.getJSONArray("formats").put(JSONObject().put("format", format.name).put("applied", replace).put("markerFound", markerFound).put("xhtmlCount", xhtmlCount))
                assertTrue(context.contentResolver.delete(uri, null, null) > 0)
                ownedUris.remove(uri)
            }
            succeeded = true
        } finally {
            instrumentation.runOnMainSync { container.downloads.cancel() }
            ownedUris.forEach { context.contentResolver.delete(it, null, null) }
            // If a create reply was lost, reconcile only our exact unique marker before deletion.
            val owned = runCatching { container.api.personalGlossaries(bookId) }.getOrDefault(emptyList())
                .filter { it.replacement == testRule.replacement && it.source == source }
            owned.mapNotNullTo(createdIds) { it.websiteRuleId }
            var deleteAcknowledged = true
            for (id in createdIds) if (runCatching { container.api.deletePersonalGlossary(id) }.isFailure) deleteAcknowledged = false
            val remaining = runCatching { container.api.personalGlossaries(bookId) }.getOrNull()
            val cleaned = deleteAcknowledged && remaining != null && remaining.none { it.websiteRuleId in createdIds || it.replacement == testRule.replacement }
            report.put("ruleRemoved", cleaned).put("passed", succeeded && cleaned).put("elapsedMs", android.os.SystemClock.elapsedRealtime() - started)
            File(folder, "public-replacement.json").writeText(report.toString(2))
            if (cleaned) cleanupCheckpoint.delete()
            scenario.close()
            assertTrue("必须删除本次临时规则", cleaned)
        }
    }
}
