package com.novalpie.nativeapp.feature.download

import org.junit.Assert.*
import org.junit.Test

class DownloadOpenActionTest {
    @Test fun completedStateExposesTheRecordedUriForTheDetailPageOpenAction() {
        val task = DownloadTask("done", 1, 2, "测试", DownloadFormat.Epub,
            phase = DownloadPhase.Completed, destinationUri = "content://downloads/test")
        assertEquals("content://downloads/test", nativeDownloadCompletedState(task).completedUri)
        assertEquals(com.novalpie.nativeapp.ui.NativeBookDownloadFormat.Epub, nativeDownloadCompletedState(task).format)
    }
    @Test fun completedTaskIsOpenableOnlyWithARecordedDestination() {
        val base = DownloadTask("open", 1, 2, "测试", DownloadFormat.Epub)
        assertFalse(downloadCanOpen(base))
        assertTrue(downloadCanOpen(base.copy(phase = DownloadPhase.Completed, destinationUri = "content://downloads/test")))
        assertFalse(downloadCanOpen(base.copy(phase = DownloadPhase.Completed, destinationUri = "")))
    }
    @Test fun resumeDoesNotTreatUnconfirmedAuthorizationAsSafeToRepeat() {
        val task = DownloadTask("uncertain", 1, 2, "测试", DownloadFormat.Epub,
            phase = DownloadPhase.AuthorizationUncertain, authorizationAttempted = true)
        assertFalse(downloadCanResume(task, false))
    }
}
