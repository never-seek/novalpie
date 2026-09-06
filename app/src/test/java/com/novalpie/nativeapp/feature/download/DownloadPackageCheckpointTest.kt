package com.novalpie.nativeapp.feature.download

import com.novalpie.nativeapp.data.NativeDownloadControl
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadPackageCheckpointTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun onlyTheSameFrozenInputAndUnchangedBytesCanReuseTheCompletedPackage()=runBlocking {
        val dir=temp.newFolder()
        val file=java.io.File(dir,"result.txt").apply{writeText("complete result")}
        val gate=NativeDownloadControl()
        val task=DownloadTask("checkpoint",1,2,"测试",DownloadFormat.Txt)
        val checkpoint=DownloadPackageCheckpoint(dir)
        checkpoint.record(task,file,"source-one",gate)
        assertTrue(checkpoint.reusable(task,file,"source-one",gate))
        assertFalse(checkpoint.reusable(task,file,"source-two",gate))
        assertFalse(checkpoint.reusable(task.copy(replacementSnapshot="changed"),file,"source-one",gate))
        file.writeText("changed content")
        assertFalse(checkpoint.reusable(task,file,"source-one",gate))
    }

    @Test fun legacyCompleteArchiveRequiresACorrectCentralDirectoryAndFullCrc()=runBlocking {
        val dir=temp.newFolder();val file=java.io.File(dir,"result.epub")
        java.util.zip.ZipOutputStream(file.outputStream()).use{zip->
            for(name in listOf("mimetype","OEBPS/content.opf","OEBPS/nav.xhtml","OEBPS/chapter-1.xhtml")) {
                zip.putNextEntry(java.util.zip.ZipEntry(name));zip.write("fixture".toByteArray());zip.closeEntry()
            }
        }
        val task=DownloadTask("legacy",1,2,"测试",DownloadFormat.Epub,completedChapters=1,totalChapters=1)
        val checkpoint=DownloadPackageCheckpoint(dir);val control=NativeDownloadControl()
        assertTrue(checkpoint.adoptVerifiedLegacy(task,file,"source",control))
        assertTrue(checkpoint.reusable(task,file,"source",control))
        val incomplete=java.io.File(temp.newFolder(),"result.epub").apply{writeText("truncated zip")}
        assertFalse(DownloadPackageCheckpoint(incomplete.parentFile!!).adoptVerifiedLegacy(task,incomplete,"source",control))
    }
}
