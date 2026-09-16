package com.novalpie.nativeapp.feature.reader.preferences

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ReaderPreferencesViewModelTest {
    private open class Repository : ReaderPreferenceRepository {
        var writes = 0
        override suspend fun list() = listOf(ReaderPreferenceProfile(1,"合成",false))
        override suspend fun load(name: String) = "{}"
        override suspend fun create(name: String, preferences: String, isDefault: Boolean) { writes++ }
        override suspend fun update(id: Long, preferences: String) { writes++ }
        override suspend fun delete(id: Long) { writes++ }
    }
    @Test fun refreshDoesNotEraseTheNameTypedWhileWaitingForNetwork() = runBlocking {
        val response = CompletableDeferred<List<ReaderPreferenceProfile>>()
        val repository = object : Repository() { override suspend fun list() = response.await() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ReaderPreferencesViewModel(repository, scope)
        try {
            model.refresh(); model.editName("新输入名称"); response.complete(emptyList())
            assertEquals("新输入名称",model.state.name)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun accountSwitchRejectsALateProfileListAndNoImplicitSaveOccurs() = runBlocking {
        val response = CompletableDeferred<List<ReaderPreferenceProfile>>()
        val repository = object : Repository() { override suspend fun list() = withContext(NonCancellable) { response.await() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ReaderPreferencesViewModel(repository, scope)
        try {
            model.refresh(); model.environmentChanged(); response.complete(listOf(ReaderPreferenceProfile(1,"旧账号",true)))
            assertTrue(model.state.profiles.isEmpty()); assertFalse(model.state.busy); assertEquals(0,repository.writes)
        } finally { model.close(); scope.cancel() }
    }
    @Test fun failedCreatePreservesNameAndDoesNotAutomaticallyRetry() = runBlocking {
        val repository = object : Repository() { override suspend fun create(name:String,preferences:String,isDefault:Boolean) { writes++; throw java.io.IOException("unknown result") } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val model = ReaderPreferencesViewModel(repository,scope)
        try {
            model.editName("我的草稿"); model.create("{}")
            assertEquals(1,repository.writes); assertEquals("我的草稿",model.state.name); assertFalse(model.state.busy)
        } finally { model.close(); scope.cancel() }
    }
}
