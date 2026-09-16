package com.novalpie.nativeapp.feature.books

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*

/** Owns one book identity, its private draft and all of its cancellable work (never the root scope). */
internal class BookManagementViewModel(
    private val repository: BookManagementRepository,
    scope: CoroutineScope? = null,
    private val environmentRevision: () -> Long = { 0L },
    private val onSaved: (Long) -> Unit = {},
) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    var state by mutableStateOf(BookManagementUiState())
        private set
    private var generation = 0L
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    // A touched field stays locally owned until this book identity ends, including failed saves.
    private val editedMetadata = mutableSetOf<String>()
    private val editedPolicy = mutableSetOf<String>()
    private var coverEditRevision = 0L
    private var transferEditRevision = 0L
    private data class Ticket(val bookId: Long, val generation: Long, val environment: Long)
    private fun ticket() = Ticket(state.bookId, generation, environmentRevision())
    private fun current(ticket: Ticket) = ticket.bookId == state.bookId && ticket.generation == generation &&
        ticket.environment == environmentRevision()

    private fun invalidate() {
        generation++ // Invalidate before cancellation: even non-cancellable repositories cannot publish.
        loadJob?.cancel(); loadJob = null
        mutationJob?.cancel(); mutationJob = null
    }

    fun load(id: Long) {
        if (id <= 0) return
        val previous = state.takeIf { it.bookId == id }
        invalidate()
        if (previous == null) {
            editedMetadata.clear(); editedPolicy.clear()
            coverEditRevision++; transferEditRevision++
        }
        state = (previous ?: BookManagementUiState(bookId = id)).copy(
            revision = generation, info = LoadResult.Loading, permissions = LoadResult.Loading,
            saving = false, uploadingCover = false, savingAccessPolicy = false, transferringBook = false,
            actionMessage = if (previous?.busy == true) interruptedMessage else previous?.actionMessage,
        )
        val ticket = ticket()
        loadJob = work.launch {
            supervisorScope {
                launch {
                    val result = fetch("加载书籍信息") {
                        repository.info(id).also { require(it.id == id) { "书籍信息标识不匹配" } }
                    }
                    ensureActive()
                    if (!current(ticket)) return@launch
                    val info = (result as? LoadResult.Success)?.value
                    state = state.copy(info = result,
                        draft = info?.let { mergeMetadata(bookEditDraft(it)) } ?: state.draft,
                        accessPolicyDraft = info?.accessPolicy?.let { mergePolicy(bookAccessPolicyDraft(it)) } ?: state.accessPolicyDraft)
                }
                launch {
                    val result = fetch("加载编辑权限") { repository.permissions(id) }
                    ensureActive()
                    if (current(ticket)) state = state.copy(permissions = result)
                }
            }
        }
    }

    fun updateDraft(draft: BookEditDraft) {
        if (state.bookId <= 0) return
        val old = metadataValues(state.draft)
        metadataValues(draft).forEach { (key, value) -> if (old[key] != value) editedMetadata += key }
        if (draft.photoUrl != state.draft.photoUrl) coverEditRevision++
        state = state.copy(draft = draft.copy(tags = draft.tags.toList()), actionMessage = null)
    }

    fun updatePolicyDraft(draft: BookAccessPolicyDraft) {
        if (state.bookId <= 0) return
        val old = policyValues(state.accessPolicyDraft)
        policyValues(draft).forEach { (key, value) -> if (old[key] != value) editedPolicy += key }
        state = state.copy(accessPolicyDraft = draft, actionMessage = null)
    }

    fun updateTransferIdentifier(identifier: String) {
        if (state.bookId <= 0) return
        if (identifier != state.transferIdentifier) transferEditRevision++
        state = state.copy(transferIdentifier = identifier, actionMessage = null)
    }

    private fun writable(): Boolean {
        if (state.bookId <= 0 || state.busy) return false
        if (!state.canManage) {
            state = state.copy(actionMessage = "尚未确认当前书籍的管理权限，请重新加载后再试")
            return false
        }
        return true
    }

    fun save() {
        if (!writable()) return
        validateBookEditDraft(state.draft)?.let { state = state.copy(actionMessage = it); return }
        val draft = state.draft
        val id = state.bookId
        state = state.copy(saving = true, actionMessage = "正在保存书籍信息…")
        mutate("保存书籍信息", {
            repository.save(id, BookEditRequest(draft.title, draft.titleTranslation, draft.authorName,
                draft.description, draft.source, draft.sourceUrl, draft.language, draft.status,
                draft.isAdult, draft.photoUrl, draft.tags))
        }) { saved ->
            if (saved.success && saved.failedFields.isEmpty() && saved.errors.isEmpty()) {
                val currentValues = metadataValues(state.draft)
                metadataValues(draft).forEach { (key, value) ->
                    if (currentValues[key] == value) editedMetadata.remove(key)
                }
            }
            val message = buildList {
                if (!saved.success) add(saved.message ?: "保存失败：服务器拒绝操作")
                else if (saved.failedFields.isNotEmpty()) add("部分信息保存失败：${saved.failedFields.joinToString(", ")}")
                else add(saved.message ?: "书籍信息保存成功")
                addAll(saved.errors)
            }.joinToString("\n")
            // Never replace a draft with the captured request, even after a confirmed full save.
            state = state.copy(actionMessage = message)
            if (saved.success) onSaved(id)
        }
    }

    fun savePolicy() {
        if (!writable()) return
        if ((state.info as? LoadResult.Success)?.value?.accessPolicy == null) {
            state = state.copy(actionMessage = "尚未获取当前读写门槛，不能用默认值覆盖，请重新加载书籍信息")
            return
        }
        validateBookAccessPolicyDraft(state.accessPolicyDraft)?.let { state = state.copy(actionMessage = it); return }
        val id = state.bookId
        val draft = state.accessPolicyDraft
        val policy = bookAccessPolicyFromDraft(draft)
        state = state.copy(savingAccessPolicy = true, actionMessage = "正在保存读写门槛…")
        mutate("保存读写门槛", { repository.savePolicy(id, policy) }) {
            if (it.success) {
                val currentValues = policyValues(state.accessPolicyDraft)
                policyValues(draft).forEach { (key, value) ->
                    if (currentValues[key] == value) editedPolicy.remove(key)
                }
            }
            state = state.copy(actionMessage = if (it.success) it.message ?: "读写门槛已保存"
                else "保存失败：${it.message ?: "服务器拒绝操作"}")
        }
    }

    fun transfer() {
        if (!writable()) return
        val identifier = state.transferIdentifier.trim()
        if (identifier.isBlank()) { state = state.copy(actionMessage = "请输入接收方 UID 或用户名"); return }
        val id = state.bookId
        val draftRevision = transferEditRevision
        state = state.copy(transferringBook = true, actionMessage = "正在提交书籍转让…")
        mutate("转让书籍", { repository.transfer(id, identifier) }) { result ->
            val target = result.targetUsername ?: result.targetUserId?.let { "UID $it" } ?: identifier
            state = state.copy(
                transferIdentifier = if (result.success && draftRevision == transferEditRevision) "" else state.transferIdentifier,
                actionMessage = if (result.success) result.message ?: "已提交转让给 $target"
                    else "转让失败：${result.message ?: "服务器拒绝操作"}",
            )
        }
    }

    fun uploadCover(uri: String) {
        if (uri.isBlank() || !writable()) return
        if ((state.permissions as? LoadResult.Success)?.value?.photoUrl != true) {
            state = state.copy(actionMessage = "当前账号无封面编辑权限"); return
        }
        val id = state.bookId
        val draftRevision = coverEditRevision
        state = state.copy(uploadingCover = true, actionMessage = "正在上传原始封面…")
        mutate("上传封面", { repository.uploadCover(id, uri) }) { url ->
            if (draftRevision == coverEditRevision) {
                updateDraft(state.draft.copy(photoUrl = url))
                state = state.copy(actionMessage = "封面已上传，保存信息后生效")
            } else state = state.copy(actionMessage = "封面已上传；保留了之后编辑的封面，请核对后保存")
        }
    }

    private fun <T> mutate(label: String, request: suspend () -> T, publish: (T) -> Unit) {
        val ticket = ticket()
        mutationJob = work.launch {
            try {
                ensureActive()
                if (!current(ticket)) return@launch
                val result = request()
                ensureActive() // A cancelled/non-cancellable response is never a success receipt.
                if (current(ticket)) publish(result)
            } catch (cancelled: CancellationException) {
                if (current(ticket)) state = state.copy(actionMessage = interruptedMessage)
                throw cancelled
            } catch (failure: Exception) {
                if (current(ticket)) state = state.copy(actionMessage = apiFailureMessage(label, failure))
            } finally {
                if (current(ticket)) state = state.copy(saving = false, uploadingCover = false,
                    savingAccessPolicy = false, transferringBook = false)
            }
        }
    }

    private suspend fun <T> fetch(label: String, request: suspend () -> T): LoadResult<T> = try {
        LoadResult.Success(request())
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (failure: Exception) { LoadResult.Error(apiFailureMessage(label, failure)) }

    /** Called synchronously by the environment owner, not by a delayed Compose snapshot collector. */
    fun environmentChanged(accountChanged: Boolean) {
        val wasBusy = state.busy
        invalidate()
        if (accountChanged) {
            editedMetadata.clear(); editedPolicy.clear()
            coverEditRevision++; transferEditRevision++
            state = BookManagementUiState(revision = generation)
        } else {
            val message = if (wasBusy) interruptedMessage else "请求环境已变更，请重新加载核对权限；草稿已保留"
            state = state.copy(revision = generation, saving = false, uploadingCover = false,
                savingAccessPolicy = false, transferringBook = false,
                info = LoadResult.Error(message), permissions = LoadResult.Error(message), actionMessage = message)
        }
    }

    fun close() { environmentChanged(accountChanged = true); work.cancel() }
    override fun onCleared() { close(); super.onCleared() }

    private fun mergeMetadata(server: BookEditDraft): BookEditDraft {
        val local = state.draft
        fun <T> field(key: String, local: T, remote: T): T = if (key in editedMetadata) local else remote
        return BookEditDraft(
            title = field("title", local.title, server.title),
            titleTranslation = field("titleTranslation", local.titleTranslation, server.titleTranslation),
            authorName = field("authorName", local.authorName, server.authorName),
            description = field("description", local.description, server.description),
            source = field("source", local.source, server.source), sourceUrl = field("sourceUrl", local.sourceUrl, server.sourceUrl),
            language = field("language", local.language, server.language), status = field("status", local.status, server.status),
            isAdult = field("isAdult", local.isAdult, server.isAdult), photoUrl = field("photoUrl", local.photoUrl, server.photoUrl),
            tags = field("tags", local.tags, server.tags).toList(), tagDraft = local.tagDraft,
        )
    }

    private fun mergePolicy(server: BookAccessPolicyDraft): BookAccessPolicyDraft {
        val local = state.accessPolicyDraft
        fun <T> field(key: String, local: T, remote: T): T = if (key in editedPolicy) local else remote
        return BookAccessPolicyDraft(
            field("allowDownload", local.allowDownload, server.allowDownload),
            field("downloadThresholdType", local.downloadThresholdType, server.downloadThresholdType),
            field("downloadThresholdValue", local.downloadThresholdValue, server.downloadThresholdValue),
            field("readThresholdType", local.readThresholdType, server.readThresholdType),
            field("readThresholdValue", local.readThresholdValue, server.readThresholdValue),
        )
    }

    private fun metadataValues(draft: BookEditDraft): Map<String, Any> = with(draft) { mapOf(
        "title" to title, "titleTranslation" to titleTranslation, "authorName" to authorName, "description" to description,
        "source" to source, "sourceUrl" to sourceUrl, "language" to language, "status" to status, "isAdult" to isAdult,
        "photoUrl" to photoUrl, "tags" to tags, "tagDraft" to tagDraft) }
    private fun policyValues(draft: BookAccessPolicyDraft): Map<String, Any> = with(draft) { mapOf(
        "allowDownload" to allowDownload, "downloadThresholdType" to downloadThresholdType, "downloadThresholdValue" to downloadThresholdValue,
        "readThresholdType" to readThresholdType, "readThresholdValue" to readThresholdValue) }

    private companion object {
        const val interruptedMessage = "操作已中断，网站可能已接收请求；草稿已保留，请先刷新核对，未自动重发"
    }
}
