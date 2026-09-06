package com.novalpie.nativeapp.feature.download

import org.junit.Assert.*
import org.junit.Test

class DownloadHistoryTest {
    private fun task(id:String,phase:DownloadPhase,account:Long=1)=DownloadTask(id,account,2,id,DownloadFormat.Epub,phase=phase)

    @Test fun activeInProcessTaskOverridesRecoveryPhaseAndAnotherAccountNeverLeaks() {
        val persisted=listOf(task("active",DownloadPhase.NeedsRetry),task("finished",DownloadPhase.Completed),task("other",DownloadPhase.Paused,2))
        val live=DownloadUiState(task("active",DownloadPhase.Transferring),busy=true)
        val result=downloadHistoryForAccount(1,persisted,live)
        assertEquals(setOf("active","finished"),result.map{it.task.id}.toSet())
        assertEquals(DownloadPhase.Transferring,result.first{it.task.id=="active"}.task.phase)
        assertTrue(result.first{it.task.id=="active"}.running)
    }

    @Test fun recoveredPausedTaskHasContinueButUncertainAuthorizationNeverHasBlindRetry() {
        assertTrue(downloadCanResume(task("paused",DownloadPhase.Paused),false))
        assertFalse(downloadCanResume(task("unknown",DownloadPhase.AuthorizationUncertain),false))
        assertFalse(downloadCanResume(task("receipt-lost",DownloadPhase.Failed).copy(authorizationAttempted=true),false))
        assertFalse(downloadCanResume(task("finished",DownloadPhase.Completed),false))
        assertFalse(downloadCanResume(task("running",DownloadPhase.Paused),true))
    }

    @Test fun completedFileCanOnlyOpenWhenARealDestinationWasPublished() {
        assertFalse(downloadCanOpen(task("done",DownloadPhase.Completed)))
        assertFalse(downloadCanOpen(task("failed",DownloadPhase.Failed).copy(destinationUri="content://downloads/1")))
        assertTrue(downloadCanOpen(task("done",DownloadPhase.Completed).copy(destinationUri="content://downloads/1")))
    }
}
