package com.novalpie.nativeapp.feature.upload

import com.novalpie.nativeapp.ui.UploadBookState
import kotlinx.coroutines.Job
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

internal interface UploadDraftPersistence {
    suspend fun load(accountId: Long, bookId: Long?): UploadBookState?
    suspend fun checkpoint(accountId: Long, state: UploadBookState)
    fun schedule(accountId: Long, state: UploadBookState, onFailure: () -> Unit): Job
    suspend fun discard(accountId: Long, bookId: Long?) { checkpoint(accountId, UploadBookState(existingNovelId = bookId)) }
}

/** Application-scope ordered disk writes. Newer snapshots cannot be overwritten by delayed jobs. */
internal class StoredUploadDrafts(private val store: UploadDraftStore, private val scope: CoroutineScope) : UploadDraftPersistence {
    private val mutex = Mutex()
    private val revisions = AtomicLong()
    private val saved = mutableMapOf<Pair<Long, Long?>, Long>()
    override suspend fun load(accountId: Long, bookId: Long?) = mutex.withLock { withContext(Dispatchers.IO) { store.load(accountId, bookId) } }
    override suspend fun discard(accountId: Long, bookId: Long?) = mutex.withLock {
        withContext(Dispatchers.IO) { store.discard(accountId, bookId) }
        saved[accountId to bookId] = revisions.incrementAndGet()
    }
    override fun schedule(accountId: Long, state: UploadBookState, onFailure: () -> Unit): Job {
        val revision = revisions.incrementAndGet()
        return scope.launch {
            try { persist(accountId, state, revision) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onFailure() }
        }
    }
    override suspend fun checkpoint(accountId: Long, state: UploadBookState) = persist(accountId, state, revisions.incrementAndGet())
    private suspend fun persist(accountId: Long, state: UploadBookState, revision: Long) = mutex.withLock {
        val key = accountId to state.existingNovelId
        if ((saved[key] ?: 0) >= revision) return@withLock
        withContext(Dispatchers.IO) { store.save(accountId, state) }
        saved[key] = revision
    }
}
