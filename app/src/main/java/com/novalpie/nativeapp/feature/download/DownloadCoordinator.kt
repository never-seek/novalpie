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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val persistenceMutex=Mutex()
    private val commandContext=scope.coroutineContext.minusKey(Job)
    private var interruptedPhase=DownloadPhase.Cancelled
    private var interruptedMessage="下载已取消"

    fun start(task:DownloadTask) {
        if(mutable.value.busy)return
        val id=++generation
        val gate=NativeDownloadControl()
        control=gate
        interruptedPhase=DownloadPhase.Cancelled
        interruptedMessage="下载已取消"
        mutable.value=DownloadUiState(task,true)
        job=scope.launch {
            try {
                persistLatest(id)
                val completed=runner.run(task,gate) { update ->
                    withContext(commandContext) {
                        if(id==generation) {
                            phaseBeforePause=update.phase
                            val value=if(gate.isPaused && update.phase !in setOf(DownloadPhase.Completed,DownloadPhase.Failed))update.copy(phase=DownloadPhase.Paused)else update
                            mutable.value=DownloadUiState(value,true)
                            persistLatest(id)
                        }
                    }
                }
                if(id==generation){mutable.value=DownloadUiState(completed,false);persistLatest(id)}
            } catch(cancelled:CancellationException) {
                if(id==generation)recordFailure(id,interruptedPhase,interruptedMessage)
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
        val failed=task.copy(phase=if(task.authorizationAttempted&&task.authorizationFile==null)DownloadPhase.AuthorizationUncertain else phase,
            failure=message,updatedAt=System.currentTimeMillis())
        mutable.value=DownloadUiState(failed,false,message)
        withContext(NonCancellable){runCatching{persistLatest(id)}}
    }

    private suspend fun persistLatest(id:Long)=persistenceMutex.withLock {
        if(id==generation)mutable.value.task?.let {persist(it)}
    }

    fun pause() {
        if(!mutable.value.busy)return
        val task=mutable.value.task ?: return
        if(task.phase==DownloadPhase.Paused)return
        phaseBeforePause=task.phase
        control?.pause()
        val next=task.copy(phase=DownloadPhase.Paused)
        mutable.value=mutable.value.copy(task=next,message="下载已暂停")
        val id=generation
        scope.launch{runCatching{persistLatest(id)}}
    }

    fun resume() {
        val task=mutable.value.task ?: return
        if(!mutable.value.busy || task.phase!=DownloadPhase.Paused)return
        control?.resume()
        val next=task.copy(phase=phaseBeforePause)
        mutable.value=mutable.value.copy(task=next,message=null)
        val id=generation
        scope.launch{runCatching{persistLatest(id)}}
    }

    fun cancel() {
        if(!mutable.value.busy)return
        job?.cancel()
        control?.resume()
    }

    /** OS time budget exhaustion is resumable interruption, not a user cancellation. */
    fun interrupt() {
        if(!mutable.value.busy)return
        interruptedPhase=DownloadPhase.NeedsRetry
        interruptedMessage="系统暂停了后台下载，可从检查点继续"
        job?.cancel()
        control?.resume()
    }

    fun restore(task:DownloadTask) {if(!mutable.value.busy)mutable.value=DownloadUiState(task,false)}
}
