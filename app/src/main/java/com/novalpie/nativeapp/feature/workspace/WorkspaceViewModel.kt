package com.novalpie.nativeapp.feature.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*

/** Native workspace configuration: independent reads, frozen writes, no secret logging. */
internal class WorkspaceViewModel(private val repository: WorkspaceRepository, private val local: WorkspaceLocalRepository, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private var revision = 0L
    private var environment = 0L
    private var reads: Job? = null
    private var nextLocalId = System.currentTimeMillis()
    var state by mutableStateOf(WorkspaceState(localApis = local.apis(), jobs = local.jobs(), hasUnassignedLegacyData = local.hasLegacyData()))
        private set
    fun present(transform: (WorkspaceState) -> WorkspaceState) { state = transform(state) }
    fun load(message: String? = null) {
        val serial = ++revision
        reads?.cancel()
        state = state.copy(apiConfigs = LoadResult.Loading, cookieStatus = LoadResult.Loading, cookieConfigs = LoadResult.Loading, health = LoadResult.Loading,
            localApis = local.apis(), jobs = local.jobs(), hasUnassignedLegacyData = local.hasLegacyData(), actionMessage = message ?: state.actionMessage)
        reads = work.launch { supervisorScope {
            launch { val result = attempt { repository.apis() }; if (serial == revision) state = state.copy(apiConfigs = result.asLoad("API 配置")) }
            launch { val result = attempt { repository.cookieStatus() }; if (serial == revision) state = state.copy(cookieStatus = result.asLoad("Cookie 状态")) }
            launch { val result = attempt { repository.cookies() }; if (serial == revision) state = state.copy(cookieConfigs = result.asLoad("Cookie 配置")) }
            launch { val result = attempt { repository.health() }; if (serial == revision) state = state.copy(health = result.asLoad("工作区状态")) }
        } }
    }
    fun saveApi(draft: WorkspaceApiDraft) {
        if (state.actionLoading) return
        validateWorkspaceApiDraft(draft)?.let { state = state.copy(actionMessage = it); return }
        val account = environment
        val id = draft.id ?: ++nextLocalId
        state = state.copy(actionLoading = true, actionMessage = null, failedApiDraft = null)
        work.launch {
            val result = attempt { repository.saveApi(draft).also { check(it.success) { it.message ?: "服务器未保存配置" } } }
            if (account != environment) return@launch
            val saved = result.mapCatching { action ->
                val serverId = if (draft.shareToServer) action.id ?: draft.serverId ?: error("服务器未返回配置ID，请刷新确认后重试，未自动重复发布") else null
                local.save(WorkspaceLocalApiConfig(id, draft.name.trim(), draft.model.trim(), draft.endpoint.trim(), draft.apiKey.trim(), draft.concurrency.toInt(), draft.shareToServer, serverId))
                action
            }
            state = state.copy(localApis = local.apis(), actionLoading = false,
                failedApiDraft = if (saved.isFailure) draft else null,
                actionMessage = saved.fold({ "API 配置已保存" }, { failureMessage("保存 API 配置", it, listOf(draft.apiKey)) }))
            if (saved.isSuccess) load()
        }
    }

    fun saveCookie(draft: WorkspaceCookieDraft) {
        validateWorkspaceCookieDraft(draft)?.let { state = state.copy(actionMessage = it); return }
        action("Cookie 配置已保存", secrets = listOf(draft.cookieRaw), failedCookie = draft) { repository.saveCookie(draft) }
    }
    fun deleteLocal(config: WorkspaceLocalApiConfig) {
        action("API 配置已删除", after = { local.delete(config.id) }) {
            config.serverId?.let { repository.deleteApi(it) } ?: WorkspaceActionResult(true)
        }
    }
    fun deleteServer(id: Long) = action("共享 API 已删除", after = {
        local.apis().filter { it.serverId == id }.forEach { local.save(it.copy(sharedToServer = false, serverId = null)) }
    }) { repository.deleteApi(id) }
    fun toggleApi(id: Long) = action("API 状态已更新") { repository.toggleApi(id) }
    fun deleteCookie(id: Long) = action("Cookie 配置已删除") { repository.deleteCookie(id) }
    fun toggleCookie(id: Long, enabled: Boolean) = action("Cookie 状态已更新") { repository.toggleCookie(id, enabled) }
    fun dismissFailedDrafts() { state = state.copy(failedApiDraft = null, failedCookieDraft = null) }
    fun restoreLegacy() { if (local.restoreLegacyData()) load("已恢复本机旧配置，未共享或启动任务") }

    private fun action(label: String, secrets: List<String> = emptyList(), failedCookie: WorkspaceCookieDraft? = null,
        after: () -> Unit = {}, operation: suspend () -> WorkspaceActionResult) {
        if (state.actionLoading) return
        val account = environment
        state = state.copy(actionLoading = true, actionMessage = null, failedCookieDraft = null)
        work.launch {
            val result = attempt { operation().also { check(it.success) { it.message ?: "服务器拒绝操作" } } }
            if (account != environment) return@launch
            val saved = result.mapCatching { after(); it }
            state = state.copy(actionLoading = false, localApis = local.apis(), failedCookieDraft = if (saved.isFailure) failedCookie else null,
                actionMessage = saved.fold({ label }, { failureMessage(label, it, secrets) }))
            if (saved.isSuccess) load()
        }
    }
    private fun failureMessage(label: String, failure: Throwable, secrets: List<String>): String {
        var message = apiFailureMessage(label, failure)
        (secrets + local.apis().map { it.apiKey }).filter(String::isNotBlank).forEach { secret -> message = message.replace(secret, "[已隐藏]") }
        return message
    }
    fun environmentChanged() { environment++; revision++; work.coroutineContext.cancelChildren(); state = WorkspaceState() }
    private suspend fun <T> attempt(load: suspend () -> T): Result<T> = try { Result.success(load()) } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { Result.failure(failure) }
    private fun <T> Result<T>.asLoad(label: String): LoadResult<T> = fold({ LoadResult.Success(it) }, { LoadResult.Error(apiFailureMessage(label, it)) })
    fun close() { environment++; revision++; work.cancel() }
}
