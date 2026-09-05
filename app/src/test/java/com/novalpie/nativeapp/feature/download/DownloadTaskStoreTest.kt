package com.novalpie.nativeapp.feature.download

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DownloadTaskStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun task(id: String = "test-task") = DownloadTask(
        id=id,accountId=100,bookId=20,title="受控测试书",format=DownloadFormat.Epub,
        applyReplacement=true,replacementSnapshot="{\"test\":\"frozen\"}",requestedConcurrency=9999,
    )

    @Test fun processRecoveryPreservesAuthorizationSnapshotAndExplicitlyRequiresRetry() {
        val root=temporary.newFolder()
        val store=DownloadTaskStore(root)
        store.save(task().copy(phase=DownloadPhase.Transferring,authorizationFile="authorized.txt",completedChapters=15,completedAssets=32))
        val recovered=DownloadTaskStore(root).recover(100).tasks.single()
        assertEquals(DownloadPhase.NeedsRetry,recovered.phase)
        assertEquals("authorized.txt",recovered.authorizationFile)
        assertEquals("{\"test\":\"frozen\"}",recovered.replacementSnapshot)
        assertEquals(32,recovered.completedAssets)
        assertEquals(15,recovered.completedChapters)
        assertFalse(recovered.mayAuthorizeAgain)
    }

    @Test fun authorizationInterruptedBeforeReceiptNeverBlindlyChargesAgain() {
        val store=DownloadTaskStore(temporary.newFolder())
        store.save(task().copy(phase=DownloadPhase.Authorizing,authorizationAttempted=true))
        val recovered=store.recover(100).tasks.single()
        assertEquals(DownloadPhase.AuthorizationUncertain,recovered.phase)
        assertFalse(recovered.mayAuthorizeAgain)
    }

    @Test fun completedAndPausedTasksKeepTheirStateAndOtherAccountsAreNotExposed() {
        val store=DownloadTaskStore(temporary.newFolder())
        store.save(task("done").copy(phase=DownloadPhase.Completed,destinationUri="content://downloads/123"))
        store.save(task("paused").copy(phase=DownloadPhase.Paused))
        store.save(task("other").copy(accountId=200))
        val recovered=store.recover(100).tasks
        assertEquals(setOf("done","paused"),recovered.map { it.id }.toSet())
        assertEquals(DownloadPhase.Completed,recovered.first{it.id=="done"}.phase)
        assertEquals(DownloadPhase.Paused,recovered.first{it.id=="paused"}.phase)
    }

    @Test fun untrustedTaskNamesCannotEscapeTheOwnedDirectory() {
        val store=DownloadTaskStore(temporary.newFolder())
        listOf("../outside","..","a/b","a\\b","C:drive").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { store.save(task(invalid)) }
        }
    }

    @Test fun corruptCheckpointsAreReportedAndNotSilentlyOverwritten() {
        val root=temporary.newFolder()
        File(root,"broken.json").writeText("truncated {")
        val result=DownloadTaskStore(root).recover(100)
        assertTrue(result.tasks.isEmpty())
        assertEquals(listOf("broken.json"),result.unreadableFiles)
        assertEquals("truncated {",File(root,"broken.json").readText())
    }

    @Test fun userConcurrencyIsPreservedWhileActualWorkersRemainBounded() {
        val task=task()
        assertEquals(9999,task.requestedConcurrency)
        assertEquals(8,effectiveDownloadConcurrency(9999,64L*1024*1024))
        assertEquals(16,effectiveDownloadConcurrency(9999,512L*1024*1024))
        assertEquals(1,effectiveDownloadConcurrency(0,1024))
    }
}
