package com.novalpie.nativeapp.feature.reader.replacement

import com.novalpie.nativeapp.model.ReaderReplacementRule
import com.novalpie.nativeapp.ui.ReaderReplacementRemoteSyncAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ReplacementRemoteWriterTest {
    @Test fun legacySourceChangeUsesThePreEditBaselineNotAlreadyUpdatedLocalStorage() {
        val previous = ReaderReplacementRule("personal:11", 1, "A", "B", websiteRuleId = 11)
        val saved = previous.copy(source = "新的A")
        val action = com.novalpie.nativeapp.ui.readerReplacementSaveSyncAction(
            readerReplacementSyncBaseline(previous, saved), saved,
        )
        assertTrue(action is ReaderReplacementRemoteSyncAction.Replace)
    }
    @Test fun checkpointBindsTheCorrectBookRuleWithoutLosingANewerLocalEdit() {
        val saved = ReaderReplacementRule("mine", 1, "A", "B")
        val current = saved.copy(replacement = "之后修改的C", order = 9)
        val remote = saved.copy(id = "personal:22", websiteRuleId = 22)
        val result = bindReaderReplacementAcknowledgement(listOf(current), saved, remote).single()
        assertEquals(22L, result.websiteRuleId)
        assertEquals("之后修改的C", result.replacement)
        assertEquals("B", result.websiteReplacement)
        assertEquals(9, result.order)
    }
    private val local = ReaderReplacementRule("local", 1, "新原文", "新译名", websiteRuleId = 11)
    private open class Repository : ReplacementRemoteRepository {
        val writes = mutableListOf<String>()
        override suspend fun personal(bookId: Long) = emptyList<ReaderReplacementRule>()
        override suspend fun create(rule: ReaderReplacementRule): ReaderReplacementRule { writes += "create"; return rule.copy(websiteRuleId = 22) }
        override suspend fun update(id: Long, replacement: String): ReaderReplacementRule = error("unused")
        override suspend fun delete(id: Long) { writes += "delete:$id" }
    }

    @Test fun failedCreateNeverDeletesTheExistingPublicContribution() = runTest {
        val repository = object : Repository() {
            override suspend fun create(rule: ReaderReplacementRule): ReaderReplacementRule { writes += "create"; throw IOException("synthetic failure") }
        }
        assertTrue(runCatching { ReplacementRemoteWriter(repository).execute(ReaderReplacementRemoteSyncAction.Replace(11, local)) {} }.isFailure)
        assertEquals(listOf("create"), repository.writes)
    }

    @Test fun newIdentityIsCheckpointedBeforeCleanupCanFail() = runTest {
        val checkpoints = mutableListOf<ReaderReplacementRule>()
        val repository = object : Repository() {
            override suspend fun delete(id: Long) {
                assertEquals(22L, checkpoints.lastOrNull()?.websiteRuleId)
                throw IOException("synthetic cleanup failure")
            }
        }
        assertTrue(runCatching { ReplacementRemoteWriter(repository).execute(ReaderReplacementRemoteSyncAction.Replace(11, local), checkpoints::add) }.isFailure)
        assertEquals(22L, checkpoints.single().websiteRuleId)
    }

    @Test fun rereadReconcilesAnUncertainPriorCreateInsteadOfDuplicatingIt() = runTest {
        val existing = local.copy(id = "personal:22", websiteRuleId = 22)
        val repository = object : Repository() { override suspend fun personal(bookId: Long) = listOf(existing) }
        val result = ReplacementRemoteWriter(repository).execute(ReaderReplacementRemoteSyncAction.Create(local.copy(websiteRuleId = null))) {}
        assertEquals(22L, result?.websiteRuleId)
        assertTrue(repository.writes.isEmpty())
    }
}
