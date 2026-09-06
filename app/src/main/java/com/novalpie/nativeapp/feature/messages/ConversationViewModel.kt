package com.novalpie.nativeapp.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.apiFailureMessage
import kotlinx.coroutines.*

/** Recipient-keyed composers survive refresh/navigation; writes are never automatically replayed. */
internal class ConversationViewModel(
    private val repository: MessagesRepository,
    scope: CoroutineScope? = null,
) : MessageFeature(scope) {
    private var revision = 0L
    private var readJob: Job? = null
    private data class Composer(val text: String = "", val editRevision: Long = 0, val sending: Boolean = false, val notice: String? = null)
    private val composers = mutableMapOf<Long, Composer>()
    var state by mutableStateOf(MessageConversationState())
        private set

    fun load(targetId: Long, name: String?) {
        if (targetId <= 0) return
        val same = state.targetUserId == targetId
        val composer = composers.getOrPut(targetId) { Composer() }
        val pages = if (same) state.page.coerceAtLeast(1) else 1
        val serial = ++revision
        val account = environment
        readJob?.cancel()
        state = MessageConversationState(
            targetUserId = targetId, targetName = name ?: state.targetName.takeIf { same },
            messages = if (same && state.messages is LoadResult.Success) state.messages else LoadResult.Loading,
            draft = composer.text, sending = composer.sending, actionMessage = composer.notice,
            refreshing = true, page = pages, hasMore = same && state.hasMore,
        )
        readJob = work.launch {
            var lastSize = 0
            val result = messageAttempt {
                buildList {
                    for (page in 1..pages) {
                        val loaded = repository.conversation(targetId, page, PAGE_SIZE)
                        lastSize = loaded.size
                        addAll(loaded)
                    }
                }
            }
            if (account != environment || serial != revision) return@launch
            state = result.fold(
                { state.copy(messages = LoadResult.Success(chronologicalMessages(it)), refreshing = false, hasMore = lastSize >= PAGE_SIZE) },
                { state.copy(messages = if (state.messages is LoadResult.Success) state.messages else LoadResult.Error(apiFailureMessage("私信对话", it)),
                    refreshing = false, actionMessage = apiFailureMessage("私信对话", it)) },
            )
        }
    }

    fun loadMore() {
        val messages = (state.messages as? LoadResult.Success)?.value ?: return
        if (state.refreshing || state.loadingMore || !state.hasMore) return
        val targetId = state.targetUserId
        val page = state.page + 1
        val serial = ++revision
        val account = environment
        state = state.copy(loadingMore = true, actionMessage = null)
        readJob = work.launch {
            val result = messageAttempt { repository.conversation(targetId, page, PAGE_SIZE) }
            if (account != environment || serial != revision) return@launch
            state = result.fold(
                { state.copy(messages = LoadResult.Success(chronologicalMessages(messages + it)), page = page,
                    loadingMore = false, hasMore = it.size >= PAGE_SIZE) },
                { state.copy(loadingMore = false, actionMessage = apiFailureMessage("更早的私信", it)) },
            )
        }
    }

    fun edit(value: String) {
        val target = state.targetUserId.takeIf { it > 0 } ?: return
        val old = composers.getOrPut(target) { Composer() }
        composers[target] = old.copy(text = value, editRevision = old.editRevision + 1, notice = null)
        publishComposer(target)
    }

    fun send(senderId: Long, senderName: String) {
        val targetId = state.targetUserId
        val submitted = composers[targetId] ?: return
        val content = submitted.text.trim()
        if (senderId <= 0 || targetId <= 0 || content.isBlank() || submitted.sending) return
        val account = environment
        composers[targetId] = submitted.copy(sending = true, notice = null)
        publishComposer(targetId)
        work.launch {
            val result = messageAttempt { repository.send(senderId, senderName, targetId, content).requireAcknowledged() }
            if (account != environment) return@launch
            val current = composers[targetId] ?: return@launch
            composers[targetId] = result.fold(
                { current.copy(text = if (current.editRevision == submitted.editRevision) "" else current.text,
                    sending = false, notice = it.message ?: "私信已发送") },
                { current.copy(sending = false, notice = messageWriteFailure("发送私信", it)) },
            )
            publishComposer(targetId)
            if (result.isSuccess && state.targetUserId == targetId) load(targetId, state.targetName)
        }
    }

    private fun publishComposer(target: Long) {
        if (state.targetUserId != target) return
        val composer = composers[target] ?: return
        state = state.copy(draft = composer.text, sending = composer.sending, actionMessage = composer.notice)
    }

    fun environmentChanged() {
        invalidateEnvironment(); revision++; composers.clear(); state = MessageConversationState()
    }

    companion object { private const val PAGE_SIZE = 100 }
}

internal fun chronologicalMessages(messages: List<DirectMessage>): List<DirectMessage> = messages
    .associateBy { it.id }.values.sortedWith(compareBy<DirectMessage> { it.createdAt.orEmpty().replace('T', ' ').take(19) }.thenBy { it.id })
