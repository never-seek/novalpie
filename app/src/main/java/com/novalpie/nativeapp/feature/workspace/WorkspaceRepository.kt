package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.data.WorkspaceLocalStore
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.WorkspaceApiDraft
import com.novalpie.nativeapp.ui.WorkspaceCookieDraft

internal interface WorkspaceRepository {
    suspend fun apis(): List<WorkspaceApiConfig>
    suspend fun cookieStatus(): WorkspaceCookieStatus
    suspend fun cookies(): WorkspaceCookieConfigs
    suspend fun health(): WorkspaceHealth
    suspend fun saveApi(draft: WorkspaceApiDraft): WorkspaceActionResult
    suspend fun deleteApi(id: Long): WorkspaceActionResult
    suspend fun toggleApi(id: Long): WorkspaceActionResult
    suspend fun saveCookie(draft: WorkspaceCookieDraft): WorkspaceActionResult
    suspend fun deleteCookie(id: Long): WorkspaceActionResult
    suspend fun toggleCookie(id: Long, enabled: Boolean): WorkspaceActionResult
}
internal interface WorkspaceLocalRepository {
    fun apis(): List<WorkspaceLocalApiConfig>
    fun save(config: WorkspaceLocalApiConfig)
    fun delete(id: Long)
    fun jobs(): List<WorkspaceTranslationJob>
    fun hasLegacyData(): Boolean = false
    fun restoreLegacyData(): Boolean = false
}
internal class StoredWorkspaceLocalRepository(private val store: WorkspaceLocalStore) : WorkspaceLocalRepository {
    override fun apis() = store.loadApis()
    override fun save(config: WorkspaceLocalApiConfig) = store.upsertApi(config)
    override fun delete(id: Long) = store.deleteApi(id)
    override fun jobs() = store.loadJobs()
    override fun hasLegacyData() = store.hasUnassignedLegacyData()
    override fun restoreLegacyData() = store.restoreLegacyData()
}
internal class WebsiteWorkspaceRepository(private val api: NovalPieApi) : WorkspaceRepository {
    override suspend fun apis() = api.workspaceApiConfigs()
    override suspend fun cookieStatus() = api.workspaceCookieStatus()
    override suspend fun cookies() = api.workspaceCookieConfigs()
    override suspend fun health() = api.workspaceHealth()
    override suspend fun saveApi(draft: WorkspaceApiDraft): WorkspaceActionResult = when {
        draft.shareToServer && draft.serverId != null -> api.updateWorkspaceApi(draft.serverId, draft.name, draft.model, draft.endpoint, draft.apiKey, draft.concurrency.toInt())
        draft.shareToServer -> {
            val existing = api.workspaceApiConfigs().filter { it.name == draft.name.trim() && it.model == draft.model.trim() && it.endpoint.trimEnd('/') == draft.endpoint.trim().trimEnd('/') }
            val identical = existing.firstOrNull { it.apiKey == draft.apiKey.trim() }
            if (identical != null) WorkspaceActionResult(true, id = identical.id)
            else {
                check(existing.isEmpty()) { "服务器已有同名配置，请刷新后编辑，未重复创建" }
                api.createWorkspaceApi(draft.name, draft.model, draft.endpoint, draft.apiKey, draft.concurrency.toInt())
            }
        }
        draft.serverId != null -> api.deleteWorkspaceApi(draft.serverId)
        else -> WorkspaceActionResult(true)
    }
    override suspend fun deleteApi(id: Long) = api.deleteWorkspaceApi(id)
    override suspend fun toggleApi(id: Long) = api.toggleWorkspaceApi(id)
    override suspend fun saveCookie(draft: WorkspaceCookieDraft) = if (draft.id == null) api.createWorkspaceCookie(draft.configKey, draft.description, draft.cookieRaw, draft.proxyIp, draft.isActive)
        else api.updateWorkspaceCookie(draft.id, draft.description, draft.cookieRaw.takeIf(String::isNotBlank), draft.proxyIp, draft.isActive)
    override suspend fun deleteCookie(id: Long) = api.deleteWorkspaceCookie(id)
    override suspend fun toggleCookie(id: Long, enabled: Boolean) = api.setWorkspaceCookieActive(id, enabled)
}
