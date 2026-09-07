package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.WorkspaceApiDraft
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {
    private class Local : WorkspaceLocalRepository {
        val configs = mutableListOf<WorkspaceLocalApiConfig>()
        override fun apis() = configs.toList()
        override fun save(config: WorkspaceLocalApiConfig) { configs.removeAll { it.id == config.id }; configs += config }
        override fun delete(id: Long) { configs.removeAll { it.id == id } }
        override fun jobs() = emptyList<WorkspaceTranslationJob>()
    }
    private open class Repository : WorkspaceRepository {
        override suspend fun apis() = emptyList<WorkspaceApiConfig>()
        override suspend fun cookieStatus() = WorkspaceCookieStatus(false)
        override suspend fun cookies() = WorkspaceCookieConfigs()
        override suspend fun health() = WorkspaceHealth()
        override suspend fun saveApi(draft: WorkspaceApiDraft) = WorkspaceActionResult(true, id = 1)
        override suspend fun deleteApi(id: Long) = WorkspaceActionResult(true)
        override suspend fun toggleApi(id: Long) = WorkspaceActionResult(true)
        override suspend fun saveCookie(draft: com.novalpie.nativeapp.ui.WorkspaceCookieDraft) = WorkspaceActionResult(true)
        override suspend fun deleteCookie(id: Long) = WorkspaceActionResult(true)
        override suspend fun toggleCookie(id: Long, enabled: Boolean) = WorkspaceActionResult(true)
    }
    private fun draft() = WorkspaceApiDraft(name = "合成配置", apiKey = "synthetic-only", shareToServer = true)
    @Test fun slowHealthCannotHoldTheApiAndCookiePanels() = runTest {
        val health = CompletableDeferred<WorkspaceHealth>()
        val model = WorkspaceViewModel(object : Repository() { override suspend fun health() = health.await() }, Local(), backgroundScope)
        model.load(); runCurrent()
        assertTrue(model.state.apiConfigs is LoadResult.Success)
        assertTrue(model.state.cookieStatus is LoadResult.Success)
        assertTrue(model.state.health is LoadResult.Loading)
        health.complete(WorkspaceHealth()); runCurrent()
    }
    @Test fun rejectedServerSaveDoesNotCreateAFalselySharedLocalConfig() = runTest {
        val local = Local()
        val model = WorkspaceViewModel(object : Repository() { override suspend fun saveApi(draft: WorkspaceApiDraft) = WorkspaceActionResult(false, "合成拒绝") }, local, backgroundScope)
        model.saveApi(draft()); runCurrent()
        assertTrue(local.configs.isEmpty())
        assertTrue(model.state.actionMessage.orEmpty().contains("合成拒绝"))
        assertEquals(draft(), model.state.failedApiDraft)
    }
    @Test fun oldAccountWriteCannotSaveIntoTheReplacementAccount() = runTest {
        val ack = CompletableDeferred<WorkspaceActionResult>()
        val local = Local()
        val model = WorkspaceViewModel(object : Repository() { override suspend fun saveApi(draft: WorkspaceApiDraft) = withContext(NonCancellable) { ack.await() } }, local, backgroundScope)
        model.saveApi(draft()); runCurrent(); model.environmentChanged()
        ack.complete(WorkspaceActionResult(true, id = 4)); runCurrent()
        assertTrue(local.configs.isEmpty())
    }
}
