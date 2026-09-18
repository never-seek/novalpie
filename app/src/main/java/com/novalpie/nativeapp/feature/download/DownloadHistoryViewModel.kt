package com.novalpie.nativeapp.feature.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class DownloadHistoryEntry(val task:DownloadTask,val running:Boolean)
internal data class DownloadHistoryState(val entries:List<DownloadHistoryEntry> = emptyList(),val loading:Boolean=true,val message:String?=null)

internal fun downloadHistoryForAccount(accountId:Long,stored:List<DownloadTask>,active:DownloadUiState):List<DownloadHistoryEntry> {
    val tasks=stored.filter{it.accountId==accountId}.associateBy{it.id}.toMutableMap()
    active.task?.takeIf{it.accountId==accountId}?.let{tasks[it.id]=it}
    return tasks.values.sortedByDescending{it.updatedAt}.map{DownloadHistoryEntry(it,active.busy&&active.task?.id==it.id)}
}

internal fun downloadCanResume(task:DownloadTask,running:Boolean):Boolean = !running &&
    task.phase in setOf(DownloadPhase.Queued,DownloadPhase.Paused,DownloadPhase.NeedsRetry,DownloadPhase.Failed,DownloadPhase.Cancelled) &&
    (task.authorizationFile!=null || !task.authorizationAttempted)

internal fun downloadCanOpen(task:DownloadTask):Boolean=task.phase==DownloadPhase.Completed&&!task.destinationUri.isNullOrBlank()

/** A route may disappear while the coordinator keeps working; the history is a fresh disk view. */
internal class DownloadHistoryViewModel(
    private val accountId:Long,
    private val store:DownloadTaskStore,
    private val coordinator:DownloadCoordinator,
    private val workDirectory: java.io.File? = null,
    private val start:(DownloadTask)->Unit,
):ViewModel() {
    constructor(
        accountId: Long,
        store: DownloadTaskStore,
        coordinator: DownloadCoordinator,
        start: (DownloadTask) -> Unit,
    ) : this(accountId, store, coordinator, null, start)

    var state by mutableStateOf(DownloadHistoryState())
        private set
    init {
        viewModelScope.launch {
            combine(store.revisions,coordinator.state){_,live->live}.collectLatest{live->
                try {
                    val recovered=withContext(Dispatchers.IO){store.recover(accountId)}
                    state=state.copy(loading=false,entries=downloadHistoryForAccount(accountId,recovered.tasks,live),
                        message=if(recovered.unreadableFiles.isEmpty())null else "${recovered.unreadableFiles.size}条任务记录无法读取，原记录已保留")
                }catch(cancelled:CancellationException){throw cancelled}
                catch(failure:Exception){state=state.copy(loading=false,message=failure.message ?: "下载记录读取失败")}
            }
        }
    }
    fun continueTask(task:DownloadTask) {
        if(task.accountId!=accountId||coordinator.state.value.busy||!downloadCanResume(task,false))return
        try{start(task.copy(phase=DownloadPhase.Queued,failure=null));state=state.copy(message="正在从检查点继续")}
        catch(failure:Exception){state=state.copy(message=failure.message ?: "无法恢复下载")}
    }
    fun togglePause(task:DownloadTask) {
        if(task.accountId!=accountId||coordinator.state.value.task?.id!=task.id)return
        if(coordinator.state.value.task?.phase==DownloadPhase.Paused)coordinator.resume()else coordinator.pause()
    }
    fun cancel(task:DownloadTask) {
        if(task.accountId==accountId&&coordinator.state.value.task?.id==task.id)coordinator.cancel()
    }
    fun deleteTask(task:DownloadTask) {
        if(task.accountId!=accountId)return
        if(coordinator.state.value.busy&&coordinator.state.value.task?.id==task.id)return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                store.delete(task.id)
                workDirectory?.let { dir ->
                    val work = java.io.File(dir, task.id).canonicalFile
                    if (work.parentFile == dir.canonicalFile) work.deleteRecursively()
                }
            }
            coordinator.dismiss(task.id)
            val recovered = withContext(Dispatchers.IO) { store.recover(accountId) }
            state = state.copy(entries = downloadHistoryForAccount(accountId, recovered.tasks, coordinator.state.value))
        }
    }
    fun feedback(message:String){state=state.copy(message=message)}
    fun dispose(){viewModelScope.cancel()}
}
