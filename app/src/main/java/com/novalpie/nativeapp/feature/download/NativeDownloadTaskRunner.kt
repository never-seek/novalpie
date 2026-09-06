package com.novalpie.nativeapp.feature.download

import android.content.Context
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.ReaderReplacementState
import com.novalpie.nativeapp.ui.mergeReaderReplacementPersonalRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** Native stream/archive execution with task-owned checkpoints and reusable original resources. */
internal class NativeDownloadTaskRunner(
    context:Context,
    private val api:NovalPieApi,
    private val publish:suspend (DownloadTask,File,suspend()->Unit)->String = AndroidDownloadPublisher(context)::publish,
):DownloadTaskRunner {
    private val app=context.applicationContext
    private val root=File(app.noBackupFilesDir,"download-work")

    override suspend fun run(task:DownloadTask,control:NativeDownloadControl,checkpoint:suspend(DownloadTask)->Unit):DownloadTask = withContext(Dispatchers.IO) {
        require(task.id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,95}")))
        root.mkdirs()
        val work=File(root,task.id).canonicalFile
        require(work.parentFile==root.canonicalFile)
        if(!work.isDirectory&&!work.mkdirs())throw IOException("无法创建下载任务目录")
        val current=AtomicReference(task)
        // Locks are task-scoped; completing many downloads must not retain all historical URLs.
        val resourceLocks=ConcurrentHashMap<String,Mutex>()
        suspend fun save(update:DownloadTask){current.set(update);checkpoint(update)}
        control.awaitIfPaused()
        if(task.applyReplacement&&task.replacementSnapshot==null) {
            val store=ReaderReplacementRulesStore(app)
            val personal=mergeReaderReplacementPersonalRules(store.loadPersonalRules(task.bookId),api.personalGlossaries(task.bookId))
            val enabled=store.loadSharedRulesEnabledOverride(task.bookId) ?: store.loadDefaultSharedRulesEnabled()
            val shared=if(enabled)api.sharedGlossaries(task.bookId) else emptyList()
            val snapshot=DownloadRulesSnapshot.encode(ReaderReplacementState(
                novelId=task.bookId,personalRules=personal,sharedRules=LoadResult.Success(shared),
                hiddenSharedRuleIds=store.loadHiddenSharedRuleIds(task.bookId),sharedRulesEnabledOverride=enabled))
            save(current.get().copy(replacementSnapshot=snapshot))
        }
        val metadata=api.bookDetail(task.bookId)
        var ticket=current.get().authorizationFile
        if(ticket==null) {
            if(!current.get().mayAuthorizeAgain)throw IOException("上次授权结果未确认，请核对下载记录后再新建任务，避免重复扣分")
            save(current.get().copy(phase=DownloadPhase.Authorizing,authorizationAttempted=true))
            control.awaitIfPaused()
            val receipt=if(task.format==DownloadFormat.Epub)api.requestEpubDownload(task.bookId)else api.requestTxtDownload(task.bookId)
            ticket=receipt.fileName
            save(current.get().copy(authorizationFile=ticket,phase=DownloadPhase.Transferring))
        }
        val source=File(work,"source.txt")
        val sourceComplete=File(work,"source.complete")
        val sourceReceipt=sourceComplete.readTextOrNull()?.let{runCatching{JSONObject(it)}.getOrNull()}
        val sourceWasReused=source.isFile&&sourceReceipt?.optLong("bytes")==source.length()&&sourceReceipt.optString("sha256")==fileDigest(source)
        if(!sourceWasReused) {
            val part=File(work,"source.part")
            part.outputStream().use {output->api.streamDownloadFile(ticket!!){input->copyNativeDownloadStream(input,output,control::awaitIfPaused)}}
            if(part.length()==0L)throw IOException("源站返回空下载文件")
            if(source.exists()&&!source.delete())throw IOException("无法重建下载正文")
            if(!part.renameTo(source))throw IOException("无法保存下载正文检查点")
            sourceComplete.writeText(JSONObject().put("bytes",source.length()).put("sha256",fileDigest(source)).toString())
        }
        control.awaitIfPaused()
        val transformed=current.get().replacementSnapshot?.let(DownloadRulesSnapshot::decode)
        val finished=File(work,if(task.format==DownloadFormat.Epub)"result.epub"else"result.txt")
        val packageCheckpoint=DownloadPackageCheckpoint(work)
        val sourceDigest=JSONObject(sourceComplete.readText()).getString("sha256")
        val reusePackage=packageCheckpoint.reusable(current.get(),finished,sourceDigest,control) ||
            (sourceWasReused&&packageCheckpoint.adoptVerifiedLegacy(current.get(),finished,sourceDigest,control))
        if(!reusePackage) {
        save(current.get().copy(phase=DownloadPhase.Packaging))
        var lastProgress=NativeEpubExportProgress()
        if(task.format==DownloadFormat.Txt) {
            finished.outputStream().use {output->
                if(transformed==null)source.inputStream().use{copyNativeDownloadStream(it,output,control::awaitIfPaused)}
                else source.reader(Charsets.UTF_8).use {reader->output.writer(Charsets.UTF_8).use {writer->
                    NativeEpubArchiveWriter.writeTransformedTxt(writer,reader,{number,title,body->
                        transformed.transform(number,title,body).let{NativeDownloadChapterText(it.title,it.body)}
                    },control::awaitIfPaused)
                }}
            }
        } else {
            val assets=File(work,"assets").apply{mkdirs()}
            val staging=File(work,"staging").apply{mkdirs()}
            var lastSaved=0L
            val progressLock=Any()
            finished.outputStream().use {output->source.reader(Charsets.UTF_8).use {reader->
                NativeEpubArchiveWriter.write(output,NativeEpubMetadata(task.title,metadata.author ?: "未知作者",metadata.description.orEmpty(),coverUrl=metadata.coverUrl),reader,
                    openAsset={url->openResource(assets,url,control,resourceLocks)},
                    transformChapter={number,title,body->transformed?.transform(number,title,body)?.let{NativeDownloadChapterText(it.title,it.body)} ?: NativeDownloadChapterText(title,body)},
                    imageConcurrency=effectiveDownloadConcurrency(task.requestedConcurrency,Runtime.getRuntime().maxMemory()/4),
                    stagingDirectory=staging,awaitIfPaused=control::awaitIfPaused,
                    onProgress={progress->
                        synchronized(progressLock) {
                            lastProgress=progress
                            val now=System.currentTimeMillis()
                            if(now-lastSaved>400||progress.completedChapters!=current.get().completedChapters) {
                                lastSaved=now
                                val update=current.get().copy(completedChapters=progress.completedChapters,completedAssets=progress.completedImages,
                                    totalChapters=progress.totalChapters,totalAssets=progress.totalImages,failedAssets=progress.failedImages,updatedAt=now)
                                current.set(update)
                                runBlocking {checkpoint(update)}
                            }
                        }
                    })
            }}
            save(current.get().copy(completedChapters=lastProgress.completedChapters,completedAssets=lastProgress.completedImages,
                totalChapters=lastProgress.totalChapters,totalAssets=lastProgress.totalImages,failedAssets=lastProgress.failedImages))
            if(lastProgress.failedImages>0)throw IOException("${lastProgress.failedImages}张插图失败，文件未发布；重试将复用已完成资源")
        }
        if(!finished.isFile||finished.length()==0L)throw IOException("下载生成结果为空")
        packageCheckpoint.record(current.get(),finished,sourceDigest,control)
        }
        save(current.get().copy(phase=DownloadPhase.Saving))
        val destination=publish(current.get(),finished,control::awaitIfPaused)
        val completed=current.get().copy(phase=DownloadPhase.Completed,destinationUri=destination,failure=null,updatedAt=System.currentTimeMillis())
        save(completed)
        // The canonical target was validated against the private task root above. No other task
        // or downloaded user file is eligible for cleanup here.
        if(work.canonicalFile.parentFile==root.canonicalFile)work.deleteRecursively()
        completed
    }

    private suspend fun openResource(root:File,url:String,control:NativeDownloadControl,resourceLocks:ConcurrentHashMap<String,Mutex>):NativeEpubAsset {
        val key=MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString(""){"%02x".format(it)}
        return resourceLocks.getOrPut(root.absolutePath+key){Mutex()}.withLock {openResourceLocked(root,url,key,control)}
    }
    private suspend fun openResourceLocked(root:File,url:String,key:String,control:NativeDownloadControl):NativeEpubAsset {
        val data=File(root,"$key.bin")
        val metadata=File(root,"$key.json")
        val cached=metadata.readTextOrNull()?.let{runCatching{JSONObject(it)}.getOrNull()}
        if(data.isFile&&cached?.optLong("bytes")==data.length()&&cached.optString("sha256")==fileDigest(data))return NativeEpubAsset(cached.optString("mime").takeIf{it.isNotBlank()},data.inputStream())
        val part=File.createTempFile("$key-",".part",root)
        try {
            var mime:String?=null
            part.outputStream().use {output->api.streamAsset(url){input,type->mime=type;copyNativeDownloadStream(input,output,control::awaitIfPaused)}}
            if(part.length()==0L)throw IOException("插图响应为空")
            if(data.exists()&&!data.delete())throw IOException("无法替换损坏插图检查点")
            if(!part.renameTo(data))throw IOException("无法保存插图检查点")
            metadata.writeText(JSONObject().put("bytes",data.length()).put("mime",mime.orEmpty()).put("sha256",fileDigest(data)).toString())
            return NativeEpubAsset(mime,data.inputStream())
        } finally {part.delete()}
    }
    private fun File.readTextOrNull():String?=if(isFile)runCatching{readText()}.getOrNull() else null
    private fun fileDigest(file:File):String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use{input->val buffer=ByteArray(64*1024);while(true){val n=input.read(buffer);if(n<0)break;if(n>0)digest.update(buffer,0,n)}}
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
}
