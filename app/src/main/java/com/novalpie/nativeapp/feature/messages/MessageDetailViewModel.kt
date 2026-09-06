package com.novalpie.nativeapp.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*

internal class MessageDetailViewModel(
    private val repository: MessagesRepository,
    scope: CoroutineScope? = null,
    private val onDeleted: (Long) -> Unit = {},
    private val onChanged: () -> Unit = {},
) : MessageFeature(scope) {
    var state by mutableStateOf(MessageDetailState())
        private set
    private var revision = 0L
    private var readJob: Job? = null
    private val mutations = mutableSetOf<Long>()

    fun load(id: Long, notice: String? = null) {
        if (id <= 0) return
        val serial = ++revision
        val account = environment
        readJob?.cancel()
        state = MessageDetailState(messageId = id, detail = LoadResult.Loading, actionLoading = id in mutations, actionMessage = notice)
        readJob = work.launch {
            val result = messageAttempt { repository.detail(id) }
            if (serial == revision && account == environment) state = state.copy(detail = result.messageLoadResult("消息详情"))
        }
    }
    fun markRead() {
        val current = (state.detail as? LoadResult.Success)?.value ?: return
        if (!current.isRead) mutate("标记已读") { repository.markRead(listOf(current.id)) }
    }
    fun toggleStar() {
        val current = (state.detail as? LoadResult.Success)?.value ?: return
        mutate(if (current.isStarred) "取消星标" else "添加星标") { repository.star(current.id, !current.isStarred) }
    }
    fun delete() {
        val id = (state.detail as? LoadResult.Success)?.value?.id ?: return
        mutate("删除消息", deleted = true) { repository.delete(listOf(id)) }
    }
    private fun mutate(label: String, deleted: Boolean = false, action: suspend () -> MessageActionResult) {
        val id = state.messageId.takeIf { it > 0 } ?: return
        if (!mutations.add(id)) return
        val account = environment
        state = state.copy(actionLoading = true, actionMessage = null)
        work.launch {
            val result = messageAttempt { action().requireAcknowledged() }
            if (account != environment) return@launch
            mutations.remove(id)
            if (state.messageId == id) state = state.copy(actionLoading = false)
            result.fold(
                { acknowledgement ->
                    if (deleted) onDeleted(id)
                    else if (state.messageId == id) load(id, acknowledgement.message ?: "${label}已同步")
                    onChanged()
                },
                { if (state.messageId == id) state = state.copy(actionMessage = messageWriteFailure(label, it)) },
            )
        }
    }
    fun environmentChanged() { invalidateEnvironment(); revision++; mutations.clear(); state = MessageDetailState() }
}
