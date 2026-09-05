package com.novalpie.nativeapp.feature.download

import com.novalpie.nativeapp.data.NativeDownloadControl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun interface DownloadTaskRunner {
    suspend fun run(task:DownloadTask,control:NativeDownloadControl,checkpoint:suspend (DownloadTask)->Unit):DownloadTask
}
internal data class DownloadUiState(val task:DownloadTask?=null,val busy:Boolean=false,val message:String?=null)

/** One app-owned worker; progress callbacks are bound to its generation, never a page instance. */
internal class DownloadCoordinator(
    private val scope:CoroutineScope,
    private val persist:suspend (DownloadTask)->Unit,
    private val runner:DownloadTaskRunner,
) {
    private val mutable=MutableStateFlow(DownloadUiState())
    val state=mutable.asStateFlow()
    private var job:Job?=null
    private var control:NativeDownloadControl?=null
    private var generation=0L
    private var phaseBeforePause=DownloadPhase.Transferring

    fun start(task:DownloadTask) {
        if(mutable.value.busy)return
        val id=++generation
        val gate=NativeDownloadControl()
        control=gate
        mutable.value=DownloadUiState(task,true)
        job=scope.launch {
            try {
                persist(task)
                val completed=runner.run(task,gate) { update ->
                    if(id==generation) {
                        val value=if(gate.isPaused && update.phase !in setOf(DownloadPhase.Completed,DownloadPhase.Failed))update.copy(phase=DownloadPhase.Paused)else update
                        persist(value)
                        mutable.value=DownloadUiState(value,true)
                    }
                }
                if(id==generation){persist(completed);mutable.value=DownloadUiState(completed,false)}
            } catch(cancelled:CancellationException) {
                if(id==generation)recordFailure(id,DownloadPhase.Cancelled,"下载已取消")
                throw cancelled
            } catch(failure:Exception) {
                recordFailure(id,DownloadPhase.Failed,failure.message ?: "下载失败，可重试")
            } finally {
                if(id==generation){job=null;control=null}
            }
        }
    }

    private suspend fun recordFailure(id:Long,phase:DownloadPhase,message:String) {
        if(id!=generation)return
        val task=mutable.value.task ?: return
        val failed=task.copy(phase=if(task.phase==DownloadPhase.Authorizing&&task.authorizationFile==null)DownloadPhase.AuthorizationUncertain else phase,
            failure=message,updatedAt=System.currentTimeMillis())
        withContext(NonCancellable){runCatching{persist(failed)}}
        if(id==generation)mutable.value=DownloadUiState(failed,false,message)
    }

    fun pause() {
        if(!mutable.value.busy)return
        val task=mutable.value.task ?: return
        if(task.phase==DownloadPhase.Paused)return
        phaseBeforePause=task.phase
        control?.pause()
        val next=task.copy(phase=DownloadPhase.Paused)
        mutable.value=mutable.value.copy(task=next,message="下载已暂停")
        scope.launch{persist(next)}
    }

    fun resume() {
        val task=mutable.value.task ?: return
        if(!mutable.value.busy || task.phase!=DownloadPhase.Paused)return
        control?.resume()
        val next=task.copy(phase=phaseBeforePause)
        mutable.value=mutable.value.copy(task=next,message=null)
        scope.launch{persist(next)}
    }

    fun cancel() {
        if(!mutable.value.busy)return
        job?.cancel()
        control?.resume()
    }

    fun restore(task:DownloadTask) {if(!mutable.value.busy)mutable.value=DownloadUiState(task,false)}
}
