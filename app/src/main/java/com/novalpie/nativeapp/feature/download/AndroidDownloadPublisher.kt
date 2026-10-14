package com.novalpie.nativeapp.feature.download

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.novalpie.nativeapp.data.copyNativeDownloadFilePausable
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest

/** The durable reservation, not a matching human filename, proves ownership of a resumed publication. */
internal class AndroidDownloadPublisher(
    context:Context,
    private val milestone:(PublicationMilestone)->Unit={},
    private val localUri:(File)->Uri={FileProvider.getUriForFile(context,"${context.packageName}.downloads",it)},
) {
    private val context=context.applicationContext
    suspend fun publish(task:DownloadTask,file:File,paused:suspend()->Unit):String=publishVerified(task,file,null,paused)
    suspend fun publishVerified(task:DownloadTask,file:File,verified:PublicationFingerprint?,paused:suspend()->Unit):String=DownloadPublicationLock.withLock {
        val journal=DownloadPublicationJournal(context,task.id)
        var record=journal.prepare(task,file,if(Build.VERSION.SDK_INT>=29)PublicationKind.MediaStore else PublicationKind.ExternalFile,paused,verified=verified)
        milestone(PublicationMilestone.IntentSaved)
        recover(record,journal,paused)?.let{return@withLock it}
        when(record.kind) {
            PublicationKind.MediaStore->{
                if(Build.VERSION.SDK_INT<29)throw IOException("此系统无法访问原任务的 MediaStore 下载目录；记录已保留，不会另存重复文件")
                try {publishMedia(record,file,journal,paused)} catch(error:IOException) {
                // Never fall back when a completed/unknown row might already be publicly visible.
                val ownedIntent=journal.read(task) ?: throw IOException("系统发布事务检查点无法确认，原文件已保留",error)
                val row=findOwnedRow(ownedIntent)
                if(row!=null) {
                    if(!row.pending)throw IOException("文件发布状态已变化，原记录已保留；请重试确认，不会另存重复文件",error)
                    if(context.contentResolver.delete(row.uri,null,null)<=0)throw IOException("无法确认临时下载文件已清理，原记录已保留",error)
                }
                record=ownedIntent.copy(kind=PublicationKind.LocalFile,uri=null,published=false)
                journal.save(record)
                publishFile(record,file,journal,paused,move=true)
                }
            }
            PublicationKind.ExternalFile->publishFile(record,file,journal,paused,move=false)
            PublicationKind.LocalFile->publishFile(record,file,journal,paused,move=true)
        }
    }

    /** Called before any source/API work when a runner restarts after publication. */
    suspend fun recoverPublished(task:DownloadTask,paused:suspend()->Unit):String?=DownloadPublicationLock.withLock {
        val journal=DownloadPublicationJournal(context,task.id)
        val record=journal.read(task) ?: return@withLock null
        recover(record,journal,paused)
    }

    internal fun keepCompletedFileLocally(task:DownloadTask,file:File,name:String):String=runBlocking {
        val work=File(context.noBackupFilesDir,"download-work/${task.id}").canonicalFile
        require(file.canonicalFile.parentFile==work){"只有本任务的完整打包文件可转存"}
        DownloadPublicationLock.withLock {
            val journal=DownloadPublicationJournal(context,task.id)
            val record=journal.prepare(task,file,PublicationKind.LocalFile,{},name)
            require(record.kind==PublicationKind.LocalFile){"已发布任务不能改变保存位置"}
            recover(record,journal,{}) ?: publishFile(record,file,journal,{},move=true)
        }
    }

    private suspend fun recover(record:DownloadPublicationRecord,journal:DownloadPublicationJournal,paused:suspend()->Unit):String? {
        if(record.kind==PublicationKind.MediaStore) {
            if(Build.VERSION.SDK_INT<29)throw IOException("此系统无法访问原任务的 MediaStore 下载目录；记录已保留")
            val row=findOwnedRow(record)
            if(row==null) {
                if(record.published)throw IOException("此前已发布的下载文件不存在，发布记录已保留；请核对后新建任务")
                return null
            }
            val matches=try {mediaMatches(row.uri,record,paused)} catch(missing:java.io.FileNotFoundException) {
                // insert() can reserve a pending database row before any backing file is created.
                // This is an owned incomplete publication, not a reason to insert a second row.
                if(!row.pending||record.published)throw missing
                false
            }
            if(!matches) {
                if(record.published||!row.pending)throw IOException("已发布文件的内容已改变，原文件已保留；不会覆盖或重复发布")
                return null // Known owned pending row can be rewritten from the complete private result.
            }
            val bound=record.copy(uri=row.uri.toString())
            if(record.uri!=bound.uri)journal.save(bound)
            if(row.pending)commitMedia(row.uri,bound)
            val completed=bound.copy(published=true);journal.save(completed)
            return row.uri.toString()
        }
        val target=targetFile(record)
        if(!target.exists()) {
            if(record.published)throw IOException("此前已发布的下载文件不存在，原记录已保留")
            return null
        }
        if(!target.isFile||target.length()!=record.bytes||publicationFileDigest(target,paused)!=record.sha256)
            throw IOException("本任务目标文件已改变，原文件已保留；不会认领、覆盖或另存一份")
        val uri=fileUri(record,target)
        if(record.uri!=null&&record.uri!=uri)throw IOException("下载保存地址与本任务检查点不一致")
        journal.save(record.copy(uri=uri,published=true))
        return uri
    }

    private suspend fun publishFile(record:DownloadPublicationRecord,source:File,journal:DownloadPublicationJournal,paused:suspend()->Unit,move:Boolean):String {
        val target=targetFile(record)
        val directory=target.parentFile!!
        if(!directory.isDirectory&&!directory.mkdirs())throw IOException("无法创建下载保存目录；完整文件已保留待重试")
        val uri=fileUri(record,target)
        val bound=record.copy(uri=uri);journal.save(bound);milestone(PublicationMilestone.DestinationBound)
        if(target.exists())return recover(bound,journal,paused) ?: error("目标状态不一致")
        require(source.isFile&&source.length()>0){"下载临时文件不存在"}
        if(move) {
            val allowed=File(context.noBackupFilesDir,"download-work/${record.taskId}").canonicalFile
            require(source.canonicalFile.parentFile==allowed){"只有本任务完整文件可转存"}
            paused()
            if(!source.renameTo(target))throw IOException("无法转存本机下载；完整临时文件已保留")
            if(target.length()!=record.bytes||publicationFileDigest(target,paused)!=record.sha256)
                throw IOException("本机保存内容与完整下载不一致；文件已保留，不会覆盖")
        } else {
            val part=File(directory,".novalpie-${record.nonce}.part")
            try {
                val hash=MessageDigest.getInstance("SHA-256")
                part.outputStream().use {stream->
                    DigestOutputStream(stream,hash).use {output->copyNativeDownloadFilePausable(source,output,paused);stream.fd.sync()}
                }
                if(part.length()!=record.bytes||hash.digest().toHex()!=record.sha256)throw IOException("下载保存内容与原始包不一致")
                milestone(PublicationMilestone.BytesWritten)
                if(target.exists())throw IOException("本任务目标路径已被其他文件占用，原文件已保留")
                if(!part.renameTo(target))throw IOException("无法发布下载文件；完整临时文件已保留")
            } finally {part.delete()}
        }
        milestone(PublicationMilestone.DestinationCommitted)
        journal.save(bound.copy(published=true));milestone(PublicationMilestone.ReceiptSaved)
        return uri
    }

    @androidx.annotation.RequiresApi(29)
    private suspend fun publishMedia(record:DownloadPublicationRecord,source:File,journal:DownloadPublicationJournal,paused:suspend()->Unit):String {
        val resolver=context.contentResolver
        var row=findOwnedRow(record)
        if(row==null) {
            val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,record.reservationName)
                put(MediaStore.MediaColumns.MIME_TYPE,if(record.format==DownloadFormat.Epub)"application/epub+zip"else "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING,1)
            }) ?: throw IOException("无法创建系统下载文件")
            // Even abrupt death here is recoverable by the random reservation saved BEFORE insert.
            milestone(PublicationMilestone.RowInserted)
            journal.save(record.copy(uri=uri.toString()));milestone(PublicationMilestone.DestinationBound)
            row=findOwnedRow(record.copy(uri=uri.toString())) ?: throw IOException("无法确认本任务下载文件的归属")
        }
        if(!row.pending)return recover(record.copy(uri=row.uri.toString()),journal,paused) ?: error("目标状态不一致")
        val bound=record.copy(uri=row.uri.toString());journal.save(bound)
        resolver.openOutputStream(row.uri,"wt")?.use {output->copyNativeDownloadFilePausable(source,output,paused)}
            ?: throw IOException("无法保存系统下载文件")
        milestone(PublicationMilestone.BytesWritten)
        if(!mediaMatches(row.uri,bound,paused))throw IOException("系统下载保存不完整或内容与原始包不一致")
        commitMedia(row.uri,bound);milestone(PublicationMilestone.DestinationCommitted)
        journal.save(bound.copy(published=true));milestone(PublicationMilestone.ReceiptSaved)
        return row.uri.toString()
    }

    private fun commitMedia(uri:Uri,record:DownloadPublicationRecord) {
        val count=context.contentResolver.update(uri,ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,record.name)
            put(MediaStore.MediaColumns.IS_PENDING,0)
        },null,null)
        if(count<=0)throw IOException("系统下载文件发布失败，检查点已保留")
    }
    private data class Row(val uri:Uri,val pending:Boolean)
    @androidx.annotation.RequiresApi(29)
    private fun findOwnedRow(record:DownloadPublicationRecord):Row? {
        check(Build.VERSION.SDK_INT>=29)
        val resolver=context.contentResolver
        val known=record.uri?.let(Uri::parse)
        if(known!=null&&(known.scheme!="content"||known.authority!="media"||!known.path.orEmpty().contains("/downloads/")))
            throw IOException("系统下载地址无效，记录已保留")
        @Suppress("DEPRECATION") val query=MediaStore.setIncludePending(known ?: MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        val projection=arrayOf(MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.OWNER_PACKAGE_NAME,MediaStore.MediaColumns.IS_PENDING)
        val selection=if(known==null)"${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?"else null
        val arguments=if(known==null)arrayOf(record.reservationName,context.packageName)else null
        val found=mutableListOf<Row>()
        val cursorResult=resolver.query(query,projection,selection,arguments,null)
            ?: throw IOException("无法读取系统下载发布状态，检查点已保留；不会创建重复文件")
        cursorResult.use {cursor->
            while(cursor.moveToNext()) {
                val owner=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.OWNER_PACKAGE_NAME))
                val name=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                if(owner!=context.packageName||name !in setOf(record.name,record.reservationName))
                    throw IOException("下载文件不属于本任务，不会读取或修改")
                val id=cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val uri=known ?: ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,id)
                found.add(Row(uri,cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_PENDING))!=0))
            }
        }
        if(found.size>1)throw IOException("发现多个本任务发布候选，原文件已保留；不会自行覆盖或另存")
        return found.singleOrNull()
    }
    private suspend fun mediaMatches(uri:Uri,record:DownloadPublicationRecord,paused:suspend()->Unit):Boolean {
        val size=context.contentResolver.openFileDescriptor(uri,"r")?.use{it.statSize}
        if(size!=null&&size>=0&&size!=record.bytes)return false
        return context.contentResolver.openInputStream(uri)?.use {publicationStreamDigest(it,paused)==record.sha256} ?: false
    }
    private fun targetFile(record:DownloadPublicationRecord):File {
        requireSafePublicationName(record.name)
        val root=when(record.kind) {
            PublicationKind.LocalFile->File(context.filesDir,"native-downloads")
            PublicationKind.ExternalFile->context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: throw IOException("无法打开下载目录")
            else->error("不是文件目标")
        }.canonicalFile
        val target=File(root,record.name).canonicalFile
        require(target.parentFile==root){"下载目标超出本任务目录"}
        return target
    }
    private fun fileUri(record:DownloadPublicationRecord,file:File)=if(record.kind==PublicationKind.LocalFile)localUri(file).toString()else file.toURI().toString()
    private fun ByteArray.toHex()=joinToString(""){"%02x".format(it)}
}
