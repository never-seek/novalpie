package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.core.AppContainer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Existing app session, GET only. Does not translate, submit, export raw prose or inspect API keys. */
class WorkspaceTranslationReadLiveDeviceTest {
    @Test fun currentSourceReturnsTypedCandidatesAndAnIdentityCheckedPreparationWhenAvailable() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val bookId = args.getString("bookId")?.toLongOrNull()?.takeIf { it > 0 } ?: error("Explicit bookId required")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer.from(context)
        val candidates = container.api.translationChapterCandidates(bookId)
        assertEquals(candidates.size, candidates.map { it.id }.distinct().size)
        val candidate = candidates.firstOrNull { it.status in setOf("pending", "failed", "user_translate") }
        val report = JSONObject().put("bookId", bookId).put("candidates", candidates.size)
            .put("writeRequests", 0).put("modelCalls", 0).put("prepared", false)
        if (candidate != null) {
            val preparation = container.api.prepareWorkspaceTranslation(bookId, candidate.id)
            assertEquals(bookId, preparation.bookId); assertEquals(candidate.id, preparation.chapterId)
            assertTrue(preparation.chunks.isNotEmpty())
            report.put("prepared", true).put("chapterId", candidate.id).put("chunkCount", preparation.chunks.size)
        }
        val folder = File(context.getExternalFilesDir(null), "beta7-qa").apply { mkdirs() }
        File(folder, "workspace-translation-read.json").writeText(report.toString(2))
    }
}
