package com.novalpie.nativeapp.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.apiFailureMessage
import com.novalpie.nativeapp.ui.mergeMessagePages
import kotlinx.coroutines.*

internal class MessageInboxViewModel(
    private val repository: MessagesRepository,
    scope: CoroutineScope? = null,
) : MessageFeature(scope) {
    var state by mutableStateOf(MessageCenterState())
        private set
    private var pageRevision = 0L
    private var statsRevision = 0L
    private var pageJob: Job? = null
    private var statsJob: Job? = null

    fun editKeyword(value: String) { state = state.copy(query = state.query.copy(keyword = value)) }
    fun submit() = loadQuery(state.query, retain = false)
    fun refresh() = loadQuery(state.appliedQuery ?: state.query, retain = true)
    fun filter(transform: (MessageQuery) -> MessageQuery) {
        val next = transform(state.query)
        if (state.query == next && state.appliedQuery == next) return
        state = state.copy(query = next)
        submit()
    }

    private fun loadQuery(requested: MessageQuery, retain: Boolean, notice: String? = null) {
        val query = requested.copy(keyword = requested.keyword.trim())
        val keep = retain && state.appliedQuery == query && state.messages is LoadResult.Success
        val pages = if (keep) state.pagination.page.coerceAtLeast(1) else 1
        val serial = ++pageRevision
        val account = environment
        pageJob?.cancel()
        state = state.copy(
            appliedQuery = query, messages = if (keep) state.messages else LoadResult.Loading,
            pagination = if (keep) state.pagination else MessagePagination(),
            selectedIds = if (keep) state.selectedIds else emptySet(),
            refreshing = true, loadingMore = false, actionMessage = notice,
        )
        pageJob = work.launch {
            val result = messageAttempt {
                val merged = mutableListOf<SiteMessage>()
                var pagination = MessagePagination()
                for (page in 1..pages) {
                    val loaded = repository.page(query, page, PAGE_SIZE)
                    merged += loaded.items
                    pagination = loaded.pagination
                    if (page >= pagination.totalPages) break
                }
                MessagePage(mergeMessagePages(emptyList(), merged), pagination)
            }
            if (account != environment || serial != pageRevision) return@launch
            state = result.fold(
                { loaded -> state.copy(messages = LoadResult.Success(loaded.items), pagination = loaded.pagination,
                    refreshing = false, selectedIds = state.selectedIds.intersect(loaded.items.map { it.id }.toSet())) },
                { failure -> state.copy(messages = if (keep) state.messages else LoadResult.Error(apiFailureMessage("消息列表", failure)),
                    refreshing = false, actionMessage = apiFailureMessage("消息列表", failure)) },
            )
        }
        refreshStats()
    }

    private fun refreshStats() {
        val serial = ++statsRevision
        val account = environment
        statsJob?.cancel()
        if (state.stats !is LoadResult.Success) state = state.copy(stats = LoadResult.Loading)
        statsJob = work.launch {
            val result = messageAttempt { repository.stats() }
            if (account == environment && serial == statsRevision) state = state.copy(stats = result.messageLoadResult("消息统计"))
        }
    }

    fun loadMore() {
        val current = (state.messages as? LoadResult.Success)?.value ?: return
        val pagination = state.pagination
        if (state.refreshing || state.loadingMore || pagination.page >= pagination.totalPages) return
        val query = state.appliedQuery ?: return
        val serial = ++pageRevision
        val account = environment
        state = state.copy(loadingMore = true, actionMessage = null)
        pageJob = work.launch {
            val result = messageAttempt { repository.page(query, pagination.page + 1, pagination.pageSize) }
            if (account != environment || serial != pageRevision) return@launch
            state = result.fold(
                { state.copy(messages = LoadResult.Success(mergeMessagePages(current, it.items)), pagination = it.pagination, loadingMore = false) },
                { state.copy(loadingMore = false, actionMessage = apiFailureMessage("加载更多消息", it)) },
            )
        }
    }

    fun toggleSelected(id: Long) {
        if ((state.messages as? LoadResult.Success)?.value?.any { it.id == id } != true) return
        state = state.copy(selectedIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id)
    }
    fun selectAll(select: Boolean) {
        state = state.copy(selectedIds = if (select) (state.messages as? LoadResult.Success)?.value.orEmpty().map { it.id }.toSet() else emptySet())
    }
    fun markSelectedRead() {
        val ids = state.selectedIds.toList()
        if (ids.isNotEmpty()) mutate("批量已读") { repository.markRead(ids) }
    }
    fun deleteSelected() {
        val ids = state.selectedIds.toList()
        if (ids.isNotEmpty()) mutate("批量删除") { repository.delete(ids) }
    }
    fun markAllRead() = mutate("全部已读", repository::markAllRead)
    fun star(message: SiteMessage) = mutate(if (message.isStarred) "取消星标" else "添加星标") { repository.star(message.id, !message.isStarred) }
    fun markRead(id: Long) { if (id > 0) mutate("标记已读") { repository.markRead(listOf(id)) } }

    private fun mutate(label: String, action: suspend () -> MessageActionResult) {
        if (state.actionLoading) return
        val account = environment
        state = state.copy(actionLoading = true, actionMessage = null)
        work.launch {
            val result = messageAttempt { action().requireAcknowledged() }
            if (account != environment) return@launch
            state = state.copy(actionLoading = false)
            result.fold(
                { loadQuery(state.appliedQuery ?: state.query, retain = true, notice = it.message ?: "${label}已同步") },
                { state = state.copy(actionMessage = messageWriteFailure(label, it)) },
            )
        }
    }

    fun environmentChanged() {
        invalidateEnvironment(); pageRevision++; statsRevision++; state = MessageCenterState()
    }

    companion object { private const val PAGE_SIZE = 20 }
}
