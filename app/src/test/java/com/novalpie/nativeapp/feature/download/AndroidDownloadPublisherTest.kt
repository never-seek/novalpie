package com.novalpie.nativeapp.feature.download

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class AndroidDownloadPublisherTest {
    @Test fun localFallbackMovesOnlyTheCompletedTaskFileAndLabelsTheActualDestination() {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val id=UUID.randomUUID().toString()
        val task=DownloadTask(id,1,2,"本地保存测试",DownloadFormat.Epub)
        val root=File(app.noBackupFilesDir,"download-work/$id").apply{mkdirs()}
        val complete=File(root,"result.epub").apply{writeText("complete bytes")}
        val untouched=File(root,"source.txt").apply{writeText("source bytes")}
        // AndroidX FileProvider's '/' containment check is not Windows-path compatible in
        // Robolectric. Test the filesystem contract here; the device test uses the real provider.
        val publisher=AndroidDownloadPublisher(app){destination->
            assertEquals(File(app.filesDir,"native-downloads").canonicalFile,destination.parentFile)
            Uri.parse("content://${app.packageName}.downloads/local_downloads/${destination.name}")
        }
        val uri=Uri.parse(publisher.keepCompletedFileLocally(task,complete,"$id.epub"))
        assertFalse(complete.exists())
        assertTrue(untouched.exists())
        assertTrue(uri.toString().contains("/local_downloads/"))
        val saved=File(app.filesDir,"native-downloads/$id.epub")
        assertEquals("complete bytes",saved.readText())
        assertTrue(downloadStatusText(DownloadUiState(task.copy(phase=DownloadPhase.Completed,destinationUri=uri.toString()))).contains("卸载"))
        assertThrows(IllegalArgumentException::class.java){publisher.keepCompletedFileLocally(task,root.parentFile!!,"outside.epub")}
        saved.delete();untouched.delete();root.delete()
    }
}
