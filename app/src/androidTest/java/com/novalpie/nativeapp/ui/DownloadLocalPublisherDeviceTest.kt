package com.novalpie.nativeapp.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.novalpie.nativeapp.feature.download.*
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DownloadLocalPublisherDeviceTest {
    @Test fun realFileProviderOpensAndDeletesOnlyTheCompletedLocalTestDownload() {
        val app=InstrumentationRegistry.getInstrumentation().targetContext
        val task=DownloadTask("beta7-local-${UUID.randomUUID()}",1,2,"本机保存验证",DownloadFormat.Txt)
        val root=File(app.noBackupFilesDir,"download-work/${task.id}").apply{mkdirs()}
        val content="本机保存原始字节验证"
        val file=File(root,"result.txt").apply{writeText(content)}
        var uri:Uri?=null
        try {
            uri=Uri.parse(AndroidDownloadPublisher(app).keepCompletedFileLocally(task,file,"${task.id}.txt"))
            assertEquals(content,app.contentResolver.openInputStream(uri)!!.use{it.reader().readText()})
            assertTrue(uri.toString().contains("/local_downloads/"))
            assertFalse(file.exists())
        } finally {
            uri?.let{assertEquals(1,app.contentResolver.delete(it,null,null))}
            file.delete();root.delete()
        }
    }
}
