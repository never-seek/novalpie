package com.novalpie.nativeapp.feature.reader.preferences

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.json.JSONObject

internal data class ReaderPreferencesState(val profiles: List<ReaderPreferenceProfile> = emptyList(), val busy: Boolean = false,
    val name: String = "", val message: String? = null, val loaded: String? = null)
internal class ReaderPreferencesViewModel(private val repository: ReaderPreferenceRepository, scope: CoroutineScope? = null) : ViewModel() {
    private val parent = scope ?: viewModelScope
    private val work = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    private var generation = 0L
    var state by mutableStateOf(ReaderPreferencesState()); private set
    fun editName(name: String) { state = state.copy(name = name) }
    private fun runAction(action: suspend () -> Unit) {
        if (state.busy) return
        val ticket = generation
        state = state.copy(busy = true, message = null)
        work.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (ticket == generation) state = state.copy(message = failure.message ?: "配置操作失败，请重试") }
            finally { if (ticket == generation) state = state.copy(busy = false) }
        }
    }
    private suspend fun checkedList(): List<ReaderPreferenceProfile> = repository.list().also { currentCoroutineContext().ensureActive() }
    fun refresh() = runAction { val profiles = checkedList(); state = state.copy(profiles = profiles) }
    fun load(profile: ReaderPreferenceProfile) = runAction {
        val data = repository.load(profile.name); currentCoroutineContext().ensureActive()
        state = state.copy(loaded = data, message = "已读取“${profile.name}”，请确认应用到本机")
    }
    fun consumeLoaded() { state = state.copy(loaded = null) }
    fun create(preferences: String) {
        val name = state.name.trim()
        if (name.isEmpty()) { state = state.copy(message = "请输入配置名称"); return }
        runAction { repository.create(name, preferences, false); currentCoroutineContext().ensureActive()
            val profiles = checkedList()
            state = state.copy(profiles = profiles, name = if (state.name.trim() == name) "" else state.name, message = "配置已保存") }
    }
    fun update(profile: ReaderPreferenceProfile, preferences: String) = runAction {
        val old = JSONObject(repository.load(profile.name)); currentCoroutineContext().ensureActive()
        val overlay = JSONObject(preferences); overlay.keys().forEach { key -> old.put(key, overlay.get(key)) }
        repository.update(profile.id, old.toString()); currentCoroutineContext().ensureActive()
        val profiles = checkedList(); state = state.copy(profiles = profiles, message = "配置已更新")
    }
    fun makeDefault(profile: ReaderPreferenceProfile) = runAction {
        repository.update(profile.id, JSONObject().put("is_default", true).toString()); currentCoroutineContext().ensureActive()
        val profiles = checkedList(); state = state.copy(profiles = profiles, message = "默认配置已更新")
    }
    fun delete(profile: ReaderPreferenceProfile) = runAction {
        repository.delete(profile.id); currentCoroutineContext().ensureActive()
        val profiles = checkedList(); state = state.copy(profiles = profiles, message = "配置已删除")
    }
    fun environmentChanged() { generation++; work.coroutineContext.cancelChildren(); state = ReaderPreferencesState() }
    fun close() = work.cancel()
    override fun onCleared() { close() }
}
