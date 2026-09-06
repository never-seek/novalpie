package com.novalpie.nativeapp.feature.download

import com.novalpie.nativeapp.data.NativeDownloadControl
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipFile

/** Final archive survives a failed MediaStore publication; it never needs another authorization. */
internal class DownloadPackageCheckpoint(private val directory:File) {
    private val receipt=File(directory,"package.complete")
    suspend fun record(task:DownloadTask,file:File,sourceDigest:String,control:NativeDownloadControl) {
        val data=JSONObject().put("task",task.id).put("format",task.format.name).put("source",sourceDigest)
            .put("rules",rulesDigest(task)).put("bytes",file.length()).put("sha256",digest(file,control))
        receipt.writeText(data.toString())
    }
    suspend fun reusable(task:DownloadTask,file:File,sourceDigest:String,control:NativeDownloadControl):Boolean {
        if(!file.isFile||file.length()==0L||!receipt.isFile)return false
        val data=runCatching{JSONObject(receipt.readText())}.getOrNull() ?: return false
        return data.optString("task")==task.id&&data.optString("format")==task.format.name&&data.optString("source")==sourceDigest&&
            data.optString("rules")==rulesDigest(task)&&data.optLong("bytes")==file.length()&&data.optString("sha256")==digest(file,control)
    }

    /** One-time adoption for a pre-checkpoint package that reached Saving before failing. */
    suspend fun adoptVerifiedLegacy(task:DownloadTask,file:File,sourceDigest:String,control:NativeDownloadControl):Boolean {
        if(receipt.exists()||task.format!=DownloadFormat.Epub||task.completedChapters<=0||task.totalChapters!=task.completedChapters||
            task.failedAssets!=0||task.totalAssets!=task.completedAssets||!file.isFile)return false
        val valid=try {
            ZipFile(file).use {zip->
                if(listOf("mimetype","OEBPS/content.opf","OEBPS/nav.xhtml").any{zip.getEntry(it)==null})return false
                var chapters=0;var images=0
                val buffer=ByteArray(64*1024)
                val entries=zip.entries()
                while(entries.hasMoreElements()) {
                    control.awaitIfPaused()
                    val entry=entries.nextElement()
                    if(entry.name.matches(Regex("OEBPS/chapter-\\d+\\.xhtml")))chapters++
                    if(entry.name.startsWith("OEBPS/images/image-"))images++
                    if(entry.isDirectory)continue
                    val crc=CRC32();var bytes=0L
                    zip.getInputStream(entry).use{input->while(true){control.awaitIfPaused();val n=input.read(buffer);if(n<0)break;crc.update(buffer,0,n);bytes+=n}}
                    if(bytes!=entry.size||crc.value!=entry.crc)return false
                }
                chapters==task.completedChapters&&images==task.completedAssets
            }
        }catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(_:Exception){false}
        if(valid)record(task,file,sourceDigest,control)
        return valid
    }
    private fun rulesDigest(task:DownloadTask)=MessageDigest.getInstance("SHA-256")
        .digest(("${task.applyReplacement}:"+task.replacementSnapshot.orEmpty()).toByteArray()).joinToString(""){"%02x".format(it)}
    private suspend fun digest(file:File,control:NativeDownloadControl):String {
        val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(64*1024)
        file.inputStream().use{input->while(true){control.awaitIfPaused();val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)}}
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
}
