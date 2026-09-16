package com.novalpie.nativeapp.feature.books

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.BookAccessPolicyDraft
import com.novalpie.nativeapp.ui.BookEditDraft
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookManagementViewModelTest {
    private val allowed = BookEditPermissions(title = true, authorName = true, photoUrl = true)
    private fun info(id: Long, title: String = "server$id", policy: ManagedBookAccessPolicy? = ManagedBookAccessPolicy()) =
        BookEditInfo(id, title, authorName = "server author", accessPolicy = policy)

    private inner class Repository : BookManagementRepository {
        var read: suspend (Long) -> BookEditInfo = { this@BookManagementViewModelTest.info(it) }
        var permission: suspend (Long) -> BookEditPermissions = { allowed }
        var write: suspend () -> BookEditResult = { BookEditResult(true) }
        var policyWrite: suspend () -> ForumActionResult = { ForumActionResult(true) }
        var transferWrite: suspend () -> ManagedBookTransferResult = { ManagedBookTransferResult(true) }
        var coverWrite: suspend () -> String = { "new-cover" }
        val writes = mutableListOf<Pair<Long, BookEditRequest>>()
        val policies = mutableListOf<Pair<Long, ManagedBookAccessPolicy>>()
        val transfers = mutableListOf<Pair<Long, String>>()
        val covers = mutableListOf<Pair<Long, String>>()
        override suspend fun info(id: Long) = read(id)
        override suspend fun permissions(id: Long) = permission(id)
        override suspend fun save(id: Long, request: BookEditRequest): BookEditResult {
            writes += id to request; return write()
        }
        override suspend fun savePolicy(id: Long, policy: ManagedBookAccessPolicy): ForumActionResult {
            policies += id to policy; return policyWrite()
        }
        override suspend fun transfer(id: Long, identifier: String): ManagedBookTransferResult {
            transfers += id to identifier; return transferWrite()
        }
        override suspend fun uploadCover(id: Long, uri: String): String {
            covers += id to uri; return coverWrite()
        }
    }

    @Test fun confirmedSavedFieldsCanFollowLaterServerChanges() = runTest {
        val repo = Repository()
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(1); runCurrent()
        model.updateDraft(model.state.draft.copy(title = "saved title"))
        model.save(); runCurrent()
        repo.read = { info(it, "edited on website") }
        model.load(1); runCurrent()
        assertEquals("edited on website", model.state.draft.title)
    }

    @Test fun confirmedPolicyCanFollowLaterServerChanges() = runTest {
        val repo = Repository()
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(1); runCurrent()
        model.updatePolicyDraft(model.state.accessPolicyDraft.copy(readThresholdType = "points_min", readThresholdValue = "30"))
        model.savePolicy(); runCurrent()
        repo.read = { info(it, policy = ManagedBookAccessPolicy(readThresholdType = "points_min", readThresholdValue = 40)) }
        model.load(1); runCurrent()
        assertEquals("40", model.state.accessPolicyDraft.readThresholdValue)
    }

    @Test fun retryAndLateLoadsMergeOnlyUntouchedFields() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<BookEditInfo>()
        repo.read = { withContext(NonCancellable) { pending.await() } }
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(42); runCurrent()
        model.updateDraft(model.state.draft.copy(title = "my title"))
        model.updatePolicyDraft(model.state.accessPolicyDraft.copy(readThresholdType = "points_min", readThresholdValue = "30"))
        model.updateTransferIdentifier("private target")
        pending.complete(info(42, policy = ManagedBookAccessPolicy(allowDownload = false))); runCurrent()
        assertEquals("my title", model.state.draft.title)
        assertEquals("server author", model.state.draft.authorName)
        assertEquals("30", model.state.accessPolicyDraft.readThresholdValue)
        assertFalse(model.state.accessPolicyDraft.allowDownload)
        repo.read = { info(it, "retry title") }
        model.load(42); runCurrent()
        assertEquals("my title", model.state.draft.title)
        assertEquals("30", model.state.accessPolicyDraft.readThresholdValue)
        assertEquals("private target", model.state.transferIdentifier)
    }

    @Test fun aToBToARejectsOldLoadAndPermissions() = runTest {
        val repo = Repository()
        val old = CompletableDeferred<BookEditInfo>()
        val permission = CompletableDeferred<BookEditPermissions>()
        repo.read = { withContext(NonCancellable) { old.await() } }
        repo.permission = { withContext(NonCancellable) { permission.await() } }
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(1); runCurrent()
        repo.read = { info(it, "new A") }; repo.permission = { allowed }
        model.load(2); runCurrent(); model.load(1); runCurrent()
        old.complete(info(1, "old A")); permission.complete(BookEditPermissions()); runCurrent()
        assertEquals("new A", model.state.draft.title)
        assertEquals(allowed, (model.state.permissions as LoadResult.Success).value)
    }

    @Test fun aToBToARejectsOldSaveAndDetailRefresh() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<BookEditResult>()
        repo.write = { withContext(NonCancellable) { pending.await() } }
        val refreshed = mutableListOf<Long>()
        val model = BookManagementViewModel(repo, backgroundScope, onSaved = { refreshed += it })
        model.load(1); runCurrent(); model.updateDraft(model.state.draft.copy(title = "old draft"))
        model.save(); runCurrent()
        model.load(2); runCurrent(); model.load(1); runCurrent()
        pending.complete(BookEditResult(true)); runCurrent()
        assertEquals("server1", model.state.draft.title)
        assertFalse(model.state.saving)
        assertTrue(refreshed.isEmpty())
        assertFalse(model.state.actionMessage.orEmpty().contains("成功"))
    }

    @Test fun partialSaveRetainsFailedAndNewerFieldsAndRefreshesOnlyOnce() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<BookEditResult>()
        repo.write = { pending.await() }
        val refreshed = mutableListOf<Long>()
        val model = BookManagementViewModel(repo, backgroundScope, onSaved = { refreshed += it })
        model.load(1); runCurrent()
        model.updateDraft(model.state.draft.copy(title = "submitted", authorName = "failed author"))
        model.save(); model.save(); model.uploadCover("ignored"); runCurrent()
        assertEquals(1, repo.writes.size)
        assertTrue(repo.covers.isEmpty())
        assertEquals("submitted", repo.writes.single().second.title)
        model.updateDraft(model.state.draft.copy(title = "newer draft"))
        pending.complete(BookEditResult(true, failedFields = listOf("author_name"), errors = listOf("author rejected"))); runCurrent()
        assertEquals("newer draft", model.state.draft.title)
        assertEquals("failed author", model.state.draft.authorName)
        assertTrue(model.state.actionMessage.orEmpty().contains("author_name"))
        assertTrue(model.state.actionMessage.orEmpty().contains("author rejected"))
        assertFalse(model.state.saving)
        assertEquals(listOf(1L), refreshed)
        model.load(1); runCurrent()
        assertEquals("failed author", model.state.draft.authorName)
        assertEquals("newer draft", model.state.draft.title)
    }

    @Test fun deniedUnknownAndMissingPolicyNeverWrite() = runTest {
        val repo = Repository()
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(1)
        model.updateDraft(BookEditDraft(title = "draft", authorName = "author"))
        model.updateTransferIdentifier("target")
        model.save(); model.savePolicy(); model.transfer(); model.uploadCover("cover")
        repo.permission = { BookEditPermissions() }; runCurrent()
        model.save(); model.savePolicy(); model.transfer(); model.uploadCover("cover"); runCurrent()
        assertTrue(repo.writes.isEmpty()); assertTrue(repo.policies.isEmpty())
        assertTrue(repo.transfers.isEmpty()); assertTrue(repo.covers.isEmpty())
        repo.permission = { allowed }; repo.read = { info(it, policy = null) }
        model.load(1); runCurrent(); model.savePolicy(); runCurrent()
        assertTrue(repo.policies.isEmpty())
        assertTrue(model.state.actionMessage.orEmpty().contains("门槛"))
        assertEquals("draft", model.state.draft.title)
    }

    @Test fun failuresReleaseBusyWithoutErasingAnyInput() = runTest {
        val repo = Repository()
        repo.write = { throw IllegalStateException("save rejected") }
        repo.policyWrite = { ForumActionResult(false, "policy rejected") }
        repo.transferWrite = { ManagedBookTransferResult(false, "transfer rejected") }
        repo.coverWrite = { throw IllegalStateException("cover rejected") }
        val refreshed = mutableListOf<Long>()
        val model = BookManagementViewModel(repo, backgroundScope, onSaved = { refreshed += it })
        model.load(1); runCurrent()
        model.updateDraft(model.state.draft.copy(title = "draft", photoUrl = "my cover"))
        model.updatePolicyDraft(BookAccessPolicyDraft(readThresholdType = "points_min", readThresholdValue = "30"))
        model.updateTransferIdentifier("target")
        model.save(); runCurrent(); assertFalse(model.state.saving)
        model.savePolicy(); runCurrent(); assertFalse(model.state.savingAccessPolicy)
        assertTrue(model.state.actionMessage.orEmpty().contains("policy rejected"))
        model.transfer(); runCurrent(); assertFalse(model.state.transferringBook)
        assertTrue(model.state.actionMessage.orEmpty().contains("transfer rejected"))
        model.uploadCover("uri"); runCurrent(); assertFalse(model.state.uploadingCover)
        repo.read = { throw IllegalStateException("info failed") }
        repo.permission = { throw IllegalStateException("permission failed") }
        model.load(1); runCurrent()
        assertTrue(model.state.info is LoadResult.Error); assertTrue(model.state.permissions is LoadResult.Error)
        assertEquals("draft", model.state.draft.title)
        assertEquals("my cover", model.state.draft.photoUrl)
        assertEquals("30", model.state.accessPolicyDraft.readThresholdValue)
        assertEquals("target", model.state.transferIdentifier)
        assertTrue(refreshed.isEmpty())
    }

    @Test fun accountChangeClearsImmediatelyAndDiscardsLateResults() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<ManagedBookTransferResult>()
        repo.transferWrite = { withContext(NonCancellable) { pending.await() } }
        var environment = 0L
        val model = BookManagementViewModel(repo, backgroundScope, environmentRevision = { environment })
        model.load(1); runCurrent(); model.updateTransferIdentifier("private")
        model.transfer(); runCurrent()
        environment++; model.environmentChanged(accountChanged = true)
        assertEquals(0L, model.state.bookId)
        assertEquals("", model.state.draft.title)
        assertEquals("", model.state.transferIdentifier)
        assertEquals(LoadResult.Idle, model.state.permissions)
        model.load(1); runCurrent(); model.updateTransferIdentifier("new account target")
        pending.complete(ManagedBookTransferResult(true)); runCurrent()
        assertEquals("new account target", model.state.transferIdentifier)
        assertNull(model.state.actionMessage)
    }

    @Test fun proxyOnlyChangeInvalidatesWritePreservesEditsAndDoesNotResubmit() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<BookEditResult>()
        repo.write = { withContext(NonCancellable) { pending.await() } }
        var environment = 0L
        val refreshed = mutableListOf<Long>()
        val model = BookManagementViewModel(repo, backgroundScope, { environment }, { refreshed += it })
        model.load(1); runCurrent(); model.updateDraft(model.state.draft.copy(title = "private draft"))
        model.updateTransferIdentifier("target")
        model.updatePolicyDraft(model.state.accessPolicyDraft.copy(readThresholdValue = "30", readThresholdType = "points_min"))
        model.save(); runCurrent()
        environment++; model.environmentChanged(accountChanged = false)
        assertFalse(model.state.saving)
        assertTrue(model.state.actionMessage.orEmpty().contains("未自动重发"))
        pending.complete(BookEditResult(true)); runCurrent()
        assertEquals("private draft", model.state.draft.title)
        assertEquals("30", model.state.accessPolicyDraft.readThresholdValue)
        assertEquals("target", model.state.transferIdentifier)
        model.load(1); runCurrent()
        assertEquals("private draft", model.state.draft.title)
        assertEquals(1, repo.writes.size); assertTrue(refreshed.isEmpty())
    }

    @Test fun revisionGateRejectsCompletionEvenBeforeEnvironmentCallback() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<BookEditResult>()
        repo.write = { withContext(NonCancellable) { pending.await() } }
        var environment = 0L
        val refreshed = mutableListOf<Long>()
        val model = BookManagementViewModel(repo, backgroundScope, { environment }, { refreshed += it })
        model.load(1); runCurrent(); model.save(); runCurrent()
        environment++; pending.complete(BookEditResult(true)); runCurrent()
        assertTrue(refreshed.isEmpty())
        assertFalse(model.state.actionMessage.orEmpty().contains("成功"))
        model.environmentChanged(false); assertFalse(model.state.saving)
    }

    @Test fun staleCoverCannotAffectWrongBookOrReopenedIdentity() = runTest {
        val repo = Repository()
        val pending = CompletableDeferred<String>()
        repo.coverWrite = { withContext(NonCancellable) { pending.await() } }
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(1); runCurrent(); model.uploadCover("old-uri"); runCurrent()
        model.load(2); runCurrent(); pending.complete("old-cover"); runCurrent()
        assertEquals(2L, model.state.bookId); assertEquals("", model.state.draft.photoUrl)
        model.load(1); runCurrent(); assertEquals("", model.state.draft.photoUrl)
        assertFalse(model.state.uploadingCover)
    }

    @Test fun successfulCoverAndTransferDoNotOverwriteNewerInputs() = runTest {
        val repo = Repository()
        val cover = CompletableDeferred<String>()
        val transfer = CompletableDeferred<ManagedBookTransferResult>()
        repo.coverWrite = { cover.await() }; repo.transferWrite = { transfer.await() }
        val model = BookManagementViewModel(repo, backgroundScope)
        model.load(1); runCurrent(); model.uploadCover("uri"); runCurrent()
        model.updateDraft(model.state.draft.copy(photoUrl = "newer cover"))
        cover.complete("uploaded cover"); runCurrent()
        assertEquals("newer cover", model.state.draft.photoUrl)
        model.updateTransferIdentifier("submitted target"); model.transfer(); runCurrent()
        model.updateTransferIdentifier("newer target")
        transfer.complete(ManagedBookTransferResult(true)); runCurrent()
        assertEquals("newer target", model.state.transferIdentifier)
        assertFalse(model.state.transferringBook)
    }

    @Test fun cancellationIsNotSuccessAndReleasesBusy() = runTest {
        val repo = Repository()
        repo.write = { throw CancellationException("cancelled") }
        val refreshed = mutableListOf<Long>()
        val model = BookManagementViewModel(repo, backgroundScope, onSaved = { refreshed += it })
        model.load(1); runCurrent(); model.updateDraft(model.state.draft.copy(title = "keep"))
        model.save(); runCurrent()
        assertFalse(model.state.saving); assertEquals("keep", model.state.draft.title)
        assertTrue(refreshed.isEmpty()); assertFalse(model.state.actionMessage.orEmpty().contains("成功"))
    }
}
