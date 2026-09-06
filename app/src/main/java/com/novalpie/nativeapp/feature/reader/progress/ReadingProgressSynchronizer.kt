package com.novalpie.nativeapp.feature.reader.progress

import com.novalpie.nativeapp.model.ForumActionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ReadingProgressSyncFailure(val bookId: Long, val chapterId: Long, val message: String)

/**
 * Reading and background speech share one ordered writer. A slow acknowledgement cannot let an
 * older request finish after a newer one, and cannot hold any local paragraph save behind it.
 * Pending positions coalesce by book. A failed write is not automatically replayed.
 */
internal class ReadingProgressSynchronizer(
    private val scope: CoroutineScope,
    private val environmentRevision: () -> Long,
    private val write: suspend (Long, Long) -> ForumActionResult,
) {
    private data class Position(val environment: Long, val bookId: Long, val chapterId: Long)
    private val lock = Any()
    private val pending = linkedMapOf<Long, Position>()
    private val requested = mutableMapOf<Long, Position>()
    private var worker: Job? = null
    private val mutableFailure = MutableStateFlow<ReadingProgressSyncFailure?>(null)
    val failure = mutableFailure.asStateFlow()

    fun request(bookId: Long, chapterId: Long) {
        if (bookId <= 0 || chapterId <= 0) return
        synchronized(lock) {
            val position = Position(environmentRevision(), bookId, chapterId)
            if (requested[bookId] == position) return
            requested[bookId] = position
            pending[bookId] = position
            startWorker()
        }
    }

    private fun startWorker() {
        if (worker != null || pending.isEmpty() || !scope.isActive) return
        val job = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                val position = synchronized(lock) {
                    pending.entries.firstOrNull()?.let { entry -> pending.remove(entry.key) }
                } ?: break
                if (position.environment != environmentRevision()) continue
                try {
                    val result = write(position.bookId, position.chapterId)
                    check(result.success) { result.message ?: "服务器未保存阅读进度" }
                    if (position.environment == environmentRevision()) mutableFailure.value = null
                } catch (cancelled: CancellationException) {
                    // Account/proxy cancellation discards only that old environment, not a new
                    // pending request. Actual application-scope cancellation still terminates.
                    if (!isActive || position.environment == environmentRevision()) throw cancelled
                } catch (_: Exception) {
                    if (position.environment == environmentRevision()) mutableFailure.value = ReadingProgressSyncFailure(
                        position.bookId, position.chapterId, "本地进度已保存，网站同步未完成",
                    )
                }
            }
        }
        worker = job
        job.invokeOnCompletion {
            synchronized(lock) {
                if (worker === job) { worker = null; startWorker() }
            }
        }
        job.start()
    }
}
