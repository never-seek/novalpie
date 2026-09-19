package com.novalpie.nativeapp.feature.download

import android.content.Context
import com.novalpie.nativeapp.data.*
import com.novalpie.nativeapp.model.LoadResult
import com.novalpie.nativeapp.ui.ReaderReplacementState
import com.novalpie.nativeapp.ui.mergeReaderReplacementPersonalRules
import com.novalpie.nativeapp.ui.toNativeDownloadText
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

    suspend fun run(
        task: DownloadTask,
        control: NativeDownloadControl,
        checkpoint: suspend (DownloadTask) -> Unit,
    ): DownloadTask = run(task, control, object : DownloadCheckpointCallback {
        override suspend fun onCheckpoint(task: DownloadTask, log: String?, awaitingDecision: Boolean) {
            checkpoint(task)
        }
    })

    override suspend fun run(task:DownloadTask,control:NativeDownloadControl,checkpoint:DownloadCheckpointCallback):DownloadTask = withContext(Dispatchers.IO) {
        require(task.id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,95}")))
        root.mkdirs()
        val work=File(root,task.id).canonicalFile
        require(work.parentFile==root.canonicalFile)
        if(!work.isDirectory&&!work.mkdirs())throw IOException("无法创建下载任务目录")
        val current=AtomicReference(task)
        // Locks are task-scoped; completing many downloads must not retain all historical URLs.
        val resourceLocks=ConcurrentHashMap<String,Mutex>()
        suspend fun save(update:DownloadTask, log: String? = null, awaitingDecision: Boolean = false){
            current.set(update)
            checkpoint(update, log, awaitingDecision)
        }
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
        val metadata=runCatching{api.bookDetail(task.bookId)}.getOrNull()
        var ticket=current.get().authorizationFile
        if(ticket==null) {
            if(!current.get().mayAuthorizeAgain)throw IOException("上次授权结果未确认，请核对下载记录后再新建任务，避免重复扣分")
            save(current.get().copy(phase=DownloadPhase.Authorizing,authorizationAttempted=true), "正在验证下载授权...")
            control.awaitIfPaused()
            val receipt=if(task.format==DownloadFormat.Epub)api.requestEpubDownload(task.bookId)else api.requestTxtDownload(task.bookId)
            ticket=receipt.fileName
            save(current.get().copy(authorizationFile=ticket,phase=DownloadPhase.Transferring), "下载授权成功，准备下载正文...")
        }
        val source=File(work,"source.txt")
        val sourceComplete=File(work,"source.complete")
        val sourceWasReused=source.isFile&&sourceComplete.isFile&&JSONObject(sourceComplete.readText()).optString("sha256")==fileDigest(source)
        if(!sourceWasReused) {
            save(current.get().copy(phase=DownloadPhase.Transferring), "正在下载小说正文...")
            val part=File.createTempFile("source-",".part",work)
            part.outputStream().use {output->api.streamDownloadFile(ticket!!){input->copyNativeDownloadStream(input,output,control::awaitIfPaused)}}
            if(part.length()==0L)throw IOException("下载内容为空")
            if(source.exists()&&!source.delete())throw IOException("无法覆盖已存在的正文检查点")
            if(!part.renameTo(source))throw IOException("无法保存下载正文检查点")
            sourceComplete.writeText(JSONObject().put("bytes",source.length()).put("sha256",fileDigest(source)).toString())
            save(current.get(), "正文下载完成，开始解析章节...")
        }
        control.awaitIfPaused()
        val transformed=current.get().replacementSnapshot?.let(DownloadRulesSnapshot::decode)
        val finished=File(work,if(task.format==DownloadFormat.Epub)"result.epub"else"result.txt")
        val packageCheckpoint=DownloadPackageCheckpoint(work)
        val sourceDigest=JSONObject(sourceComplete.readText()).getString("sha256")
        val reusePackage=packageCheckpoint.reusable(current.get(),finished,sourceDigest,control) ||
            (sourceWasReused&&packageCheckpoint.adoptVerifiedLegacy(current.get(),finished,sourceDigest,control))
        if(!reusePackage) {
        save(current.get().copy(phase=DownloadPhase.Packaging), "正在生成章节文本...")
        var lastProgress=NativeEpubExportProgress()
        if(task.format==DownloadFormat.Txt) {
            save(current.get(), "正在生成 TXT 文本...")
            finished.outputStream().use {output->
                if(transformed==null)source.inputStream().use{copyNativeDownloadStream(it,output,control::awaitIfPaused)}
                else source.reader(Charsets.UTF_8).use {reader->output.writer(Charsets.UTF_8).use {writer->
                    NativeEpubArchiveWriter.writeTransformedTxt(writer,reader,{number,title,body->
                        transformed.transform(number,title,body).toNativeDownloadText()
                    },control::awaitIfPaused)
                }}
            }
        } else {
            val assets=File(work,"assets").apply{mkdirs()}
            val staging=File(work,"staging").apply{mkdirs()}
            val reconcilerDir=File(work,"image-reconciliation").apply{mkdirs()}
            var imageCatalog: List<com.novalpie.nativeapp.model.Chapter>? = null
            val catalogLock = Mutex()
            val imageReconciler = TaskExportImageReconciler(reconcilerDir) { number ->
                control.awaitIfPaused()
                val catalog = catalogLock.withLock { imageCatalog ?: api.chapters(task.bookId).also { imageCatalog = it } }
                val matching = catalog.filter { it.number == number }
                val chapter = matching.singleOrNull() ?: catalog.getOrNull(number - 1) ?: catalog.firstOrNull { it.number == number }
                if (chapter == null) emptyList()
                else try {
                    com.novalpie.nativeapp.ui.readerBlocksForContent(api.chapterContent(chapter.id, showImages = true))
                        .filterIsInstance<com.novalpie.nativeapp.ui.ReaderContentBlock.Image>().map { it.originalUrl ?: it.url }
                } catch (_: Throwable) {
                    emptyList()
                }
            }
            val sourceOnlyImages = source.reader(Charsets.UTF_8).use { reader ->
                imageReconciler.prepare(reader, task.requestedConcurrency, control::awaitIfPaused)
            }
            save(current.get().copy(sourceOnlyImages = sourceOnlyImages), "开始生成 EPUB（下载插图并打包）...")
            val downloadSettings = DownloadSettingsStore(app).load()

            var continueToPublish = false
            while (!continueToPublish) {
                var lastSaved=0L
                val progressLock=Any()
                var passFailedImages = 0
                var passFailedCover = false
                save(current.get(), "正在处理图片并打包...")
                val bookDetail = metadata ?: runCatching { api.bookDetail(task.bookId) }.getOrNull()
                val primaryCover = bookDetail?.fullCoverUrl?.trim()?.takeIf { it.isNotBlank() }
                    ?: bookDetail?.coverUrl?.trim()?.takeIf { it.isNotBlank() }
                    ?: runCatching { api.bookCoverPhoto(task.bookId) }.getOrNull()

                val fallbackCovers = buildList {
                    bookDetail?.coverUrl?.trim()?.takeIf { it.isNotBlank() && it != primaryCover }?.let(::add)
                    bookDetail?.fullCoverUrl?.trim()?.takeIf { it.isNotBlank() && it != primaryCover }?.let(::add)
                    if (primaryCover == null) {
                        runCatching { api.bookCoverPhoto(task.bookId) }.getOrNull()?.let(::add)
                    }
                }.distinct()

                val resolvedTitle = bookDetail?.title?.trim()?.takeIf { it.isNotBlank() && it != "Untitled" } ?: task.title
                val resolvedAuthor = bookDetail?.author?.trim()?.takeIf { it.isNotBlank() } ?: "未知作者"
                val resolvedDescription = bookDetail?.description?.trim().orEmpty()
                val resolvedTags = bookDetail?.tags.orEmpty()
                val resolvedOriginalTitle = bookDetail?.originalTitle?.trim()?.takeIf { it.isNotBlank() && it != resolvedTitle }
                val resolvedStatus = bookDetail?.status?.trim()?.takeIf { it.isNotBlank() }
                val resolvedWordCount = bookDetail?.wordCount?.takeIf { it > 0 }
                val resolvedPlatform = bookDetail?.platform?.trim()?.takeIf { it.isNotBlank() }

                val epubMetadata = NativeEpubMetadata(
                    title = resolvedTitle,
                    author = resolvedAuthor,
                    description = resolvedDescription,
                    coverUrl = primaryCover,
                    fallbackCoverUrls = fallbackCovers,
                    tags = resolvedTags,
                    originalTitle = resolvedOriginalTitle,
                    status = resolvedStatus,
                    wordCount = resolvedWordCount,
                    platform = resolvedPlatform,
                )

                finished.outputStream().use {output->source.reader(Charsets.UTF_8).use {reader->
                    NativeEpubArchiveWriter.write(output, epubMetadata, reader,
                        openAsset={url->openResource(assets,url,control,resourceLocks)},
                        transformChapter={number,title,body->transformed?.transform(number,title,body)?.toNativeDownloadText() ?: NativeDownloadChapterText(title,body)},
                        reconcileSourceImages=imageReconciler::reconcile,
                        imageConcurrency=effectiveDownloadConcurrency(task.requestedConcurrency,Runtime.getRuntime().maxMemory()/4),
                        compressImages=downloadSettings.compressImages,
                        imageQuality=downloadSettings.imageQuality,
                        zipCompressionLevel=downloadSettings.zipCompressionLevel,
                        stagingDirectory=staging,allowMissingCover=true,awaitIfPaused=control::awaitIfPaused,
                        onProgress={progress->
                            synchronized(progressLock) {
                                lastProgress=progress
                                if (progress.coverFailed) passFailedCover = true
                                passFailedImages=progress.failedImages + (if (passFailedCover) 1 else 0)
                                val now=System.currentTimeMillis()
                                val update=current.get().copy(completedChapters=progress.completedChapters,completedAssets=progress.completedImages,
                                    totalChapters=progress.totalChapters,totalAssets=progress.totalImages,failedAssets=passFailedImages,updatedAt=now)
                                current.set(update)
                                if(progress.statusLog != null || now-lastSaved>400||progress.completedChapters!=current.get().completedChapters) {
                                    lastSaved=now
                                    runBlocking {checkpoint(update, progress.statusLog, false)}
                                }
                            }
                        })
                }}
                save(current.get().copy(completedChapters=lastProgress.completedChapters,completedAssets=lastProgress.completedImages,
                    totalChapters=lastProgress.totalChapters,totalAssets=lastProgress.totalImages,failedAssets=passFailedImages))

                if (passFailedImages == 0) {
                    continueToPublish = true
                } else {
                    save(current.get(), "图片处理完成，其中 $passFailedImages 张失败。请选择：换网络重试或直接打包。", awaitingDecision = true)
                    val decision = control.requestDecision().await()
                    if (decision == DownloadFailureDecision.PackageAnyway) {
                        save(current.get(), "已选择直接打包（缺图处保留占位）...", awaitingDecision = false)
                        continueToPublish = true
                    } else {
                        save(current.get(), "已选择换网络重试，正在重新请求失败插图...", awaitingDecision = false)
                        if (finished.exists()) finished.delete()
                    }
                }
            }
        }
        if(!finished.isFile||finished.length()==0L)throw IOException("下载生成结果为空")
        packageCheckpoint.record(current.get(),finished,sourceDigest,control)
        }
        save(current.get().copy(phase=DownloadPhase.Saving), "正在保存文件到系统存储...")
        val destination=publish(current.get(),finished,control::awaitIfPaused)
        val completed=current.get().copy(phase=DownloadPhase.Completed,destinationUri=destination,failure=null,updatedAt=System.currentTimeMillis())
        save(completed, "EPUB 打包完成，已保存！")
        // The canonical target was validated against the private task root above. No other task
        // or downloaded user file is eligible for cleanup here.
        if(work.canonicalFile.parentFile==root.canonicalFile)work.deleteRecursively()
        completed
    }

    internal suspend fun openResource(root:File,url:String,control:NativeDownloadControl,resourceLocks:ConcurrentHashMap<String,Mutex>):NativeEpubAsset {
        val key=MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString(""){"%02x".format(it)}
        return resourceLocks.getOrPut(root.absolutePath+key){Mutex()}.withLock {openResourceLocked(root,url,key,control)}
    }
    private suspend fun openResourceLocked(root:File,url:String,key:String,control:NativeDownloadControl):NativeEpubAsset {
        val data=File(root,"$key.bin")
        val metadata=File(root,"$key.json")
        val cached=metadata.readTextOrNull()?.let{runCatching{JSONObject(it)}.getOrNull()}
        if(data.isFile&&cached?.optLong("bytes")==data.length()&&cached.optString("sha256")==fileDigest(data)) {
            val inspected = runCatching { inspectNativeEpubFileType(data, cached.optString("mime").takeIf { it.isNotBlank() }) }
            if (inspected.isSuccess) return NativeEpubAsset(inspected.getOrNull(), data.inputStream())
            // A cached HTML error must not trap every retry. Refetch, keeping the old task file
            // untouched until a valid new original has been downloaded and inspected.
        }
        val part=File.createTempFile("$key-",".part",root)
        try {
            var mime:String?=null
            part.outputStream().use {output->api.streamAsset(url){input,type->mime=type;copyNativeDownloadStream(input,output,control::awaitIfPaused)}}
            if(part.length()==0L)throw IOException("插图响应为空")
            mime = inspectNativeEpubFileType(part, mime)
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
