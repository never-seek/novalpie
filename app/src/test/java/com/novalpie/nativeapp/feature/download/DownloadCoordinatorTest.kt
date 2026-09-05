package com.novalpie.nativeapp.feature.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadCoordinatorTest {
    private fun task()=DownloadTask("task",1,2,"测试书",DownloadFormat.Epub)
    @Test fun engineRunsOutsideRouteAndCheckpointsBeforeAnyAuthorizationWrite()=runTest {
        val saved=mutableListOf<DownloadTask>()
        val finish=CompletableDeferred<Unit>()
        val coordinator=DownloadCoordinator(backgroundScope,{saved+=it},DownloadTaskRunner{current,_,update->
            assertEquals(DownloadPhase.Queued,saved.last().phase)
            update(current.copy(phase=DownloadPhase.Authorizing,authorizationAttempted=true))
            finish.await()
            current.copy(phase=DownloadPhase.Completed,destinationUri="content://downloads/1")
        })
        coordinator.start(task())
        runCurrent()
        assertTrue(saved.any{it.authorizationAttempted})
        assertEquals(DownloadPhase.Authorizing,coordinator.state.value.task?.phase)
        finish.complete(Unit)
        runCurrent()
        assertEquals(DownloadPhase.Completed,coordinator.state.value.task?.phase)
    }
    @Test fun lateFailureCannotOverwriteANewerTaskAndPausedTaskKeepsAuthorization()=runTest {
        val finish=CompletableDeferred<Unit>()
        val coordinator=DownloadCoordinator(backgroundScope,{},DownloadTaskRunner{current,control,update->
            update(current.copy(phase=DownloadPhase.Transferring,authorizationFile="ticket"))
            finish.await();control.awaitIfPaused();current.copy(phase=DownloadPhase.Completed)
        })
        coordinator.start(task());runCurrent()
        coordinator.pause()
        assertEquals(DownloadPhase.Paused,coordinator.state.value.task?.phase)
        assertEquals("ticket",coordinator.state.value.task?.authorizationFile)
        coordinator.cancel();runCurrent()
        assertEquals(DownloadPhase.Cancelled,coordinator.state.value.task?.phase)
        finish.complete(Unit);runCurrent()
        assertEquals(DownloadPhase.Cancelled,coordinator.state.value.task?.phase)
    }
}
