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
            .put("exportPipeline", 6)
            .put("rules",rulesDigest(task)).put("bytes",file.length()).put("sha256",digest(file,control))
            .put("integrityIssues",DownloadIntegrityJson.encode(task.integrityIssues))
        receipt.writeText(data.toString())
    }

    /** Called only after reusable() validated the exact bytes. New GETs cannot repair old bytes. */
    fun integrityIssuesForReuse(task:DownloadTask):List<DownloadIntegrityIssue> {
        val data=JSONObject(receipt.readText())
        return if(data.has("integrityIssues")) DownloadIntegrityJson.decode(data.getJSONArray("integrityIssues"))
            else task.integrityIssues // Existing pipeline-6 packages retain their task disclosure.
    }
    suspend fun reusable(task:DownloadTask,file:File,sourceDigest:String,control:NativeDownloadControl):Boolean {
        if(!file.isFile||file.length()==0L||!receipt.isFile)return false
        val data=runCatching{JSONObject(receipt.readText())}.getOrNull() ?: return false
        return data.optInt("exportPipeline")==6 && data.optString("task")==task.id&&data.optString("format")==task.format.name&&data.optString("source")==sourceDigest&&
            data.optString("rules")==rulesDigest(task)&&data.optLong("bytes")==file.length()&&data.optString("sha256")==digest(file,control)
    }

    /** Fingerprint was computed by record()/reusable(); publisher verifies emitted bytes against it. */
    fun publicationFingerprint(task:DownloadTask,file:File,sourceDigest:String):PublicationFingerprint {
        val data=JSONObject(receipt.readText())
        require(data.optString("task")==task.id&&data.optString("format")==task.format.name&&data.optString("source")==sourceDigest&&
            data.optString("rules")==rulesDigest(task)&&data.optLong("bytes")==file.length()) {"下载打包检查点与发布文件不一致"}
        return PublicationFingerprint(data.getLong("bytes"),data.getString("sha256")).also {require(it.sha256.matches(Regex("[a-f0-9]{64}")))}
    }

    /** One-time adoption for a pre-checkpoint package that reached Saving before failing. */
    suspend fun adoptVerifiedLegacy(task:DownloadTask,file:File,sourceDigest:String,control:NativeDownloadControl):Boolean {
        // Old image-bearing archives were checked only against export markers, not the reader.
        // Rebuild them from retained source/assets; text-only CRC adoption remains safe.
        if (task.applyReplacement || task.totalAssets > 0 || task.completedAssets > 0) return false
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
                    if(entry.name.startsWith("OEBPS/images/"))return false // Even an old cover's original format/alpha is unproven.
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
