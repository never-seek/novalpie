package com.novalpie.nativeapp.feature.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novalpie.nativeapp.model.*
import kotlinx.coroutines.*

internal interface FavoriteDestinationRepository {
    suspend fun groups(): List<FavoriteGroup>
    suspend fun create(name: String): FavoriteGroup
    suspend fun add(bookId: Long, groupId: Long): FavoriteStatus
}

internal data class FavoriteDestinationState(
    val bookId: Long = 0,
    val visible: Boolean = false,
    val groups: LoadResult<List<FavoriteGroup>> = LoadResult.Idle,
    val creating: Boolean = false,
    val name: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

/** Destination is selected before the add POST; a failed write never becomes local success. */
internal class FavoriteDestinationViewModel(
    private val repository: FavoriteDestinationRepository,
    private val scope: CoroutineScope,
    private val onSaved: (Long, FavoriteStatus) -> Unit,
    private val onGroups: (List<FavoriteGroup>) -> Unit = {},
) {
    var state by mutableStateOf(FavoriteDestinationState())
        private set
    private var epoch = 0L
    private var read: Job? = null

    fun open(bookId: Long) {
        if (state.busy || bookId <= 0) return
        epoch++
        state = FavoriteDestinationState(bookId = bookId, visible = true)
        refresh()
    }

    fun refresh() {
        if (!state.visible || state.busy) return
        val owner = epoch
        read?.cancel()
        state = state.copy(groups = LoadResult.Loading, error = null)
        read = scope.launch {
            try {
                val groups = repository.groups()
                if (owner != epoch || !isActive) return@launch
                state = state.copy(groups = LoadResult.Success(groups))
                onGroups(groups)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                if (owner == epoch) state = state.copy(groups = LoadResult.Error(failure.message ?: "收藏夹加载失败"))
            }
        }
    }

    fun dismiss() { if (!state.busy) invalidate() }
    fun invalidate() { epoch++; read?.cancel(); state = FavoriteDestinationState() }
    fun startCreate() { if (!state.busy) state = state.copy(creating = true, error = null) }
    fun cancelCreate() { if (!state.busy) state = state.copy(creating = false, error = null) }
    fun updateName(value: String) { if (!state.busy) state = state.copy(name = value, error = null) }

    fun choose(groupId: Long) {
        if (!state.visible || state.busy || groupId < 0) return
        if (groupId != 0L && (state.groups as? LoadResult.Success)?.value?.none { it.id == groupId } != false) return
        submit { groupId }
    }

    fun createAndChoose() {
        val name = state.name.trim()
        if (!state.visible || state.busy || name.isBlank()) return
        val owner = epoch
        submit {
            val group = repository.create(name)
            if (owner != epoch) throw CancellationException("收藏夹所属账号已改变")
            val id = group.id?.takeIf { it > 0 } ?: error("收藏夹已提交，但未取得编号，请刷新列表确认，不要重复创建")
            val groups = ((state.groups as? LoadResult.Success)?.value.orEmpty() + group).distinctBy { it.id }
            state = state.copy(groups = LoadResult.Success(groups), creating = false, name = "")
            onGroups(groups)
            id
        }
    }

    private fun submit(destination: suspend () -> Long) {
        val owner = epoch
        val book = state.bookId
        read?.cancel() // A stale group list must not hide a just-created destination on failure.
        state = state.copy(busy = true, error = null)
        scope.launch {
            try {
                val group = destination()
                if (owner != epoch) return@launch
                val status = repository.add(book, group)
                if (owner != epoch) return@launch
                check(status.isFavorited) { "服务器尚未确认收藏，请刷新后重试" }
                onSaved(book, status)
                invalidate()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                if (owner == epoch) state = state.copy(
                    busy = false,
                    groups = if (state.groups == LoadResult.Loading) LoadResult.Error("请重新加载收藏夹") else state.groups,
                    error = failure.message ?: "收藏失败，请刷新确认后重试",
                )
            }
        }
    }
}
