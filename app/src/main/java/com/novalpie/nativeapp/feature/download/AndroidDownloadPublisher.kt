package com.novalpie.nativeapp.feature.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.novalpie.nativeapp.data.copyNativeDownloadFilePausable
import java.io.File
import java.io.IOException
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException

/** Publish only a finished file, and remove only the MediaStore row created by this call on failure. */
internal class AndroidDownloadPublisher(
    private val context:Context,
    private val localUri:(File)->android.net.Uri={FileProvider.getUriForFile(context,"${context.packageName}.downloads",it)},
) {
    suspend fun publish(task:DownloadTask,file:File,paused:suspend()->Unit):String {
        require(file.isFile&&file.length()>0){"下载结果为空"}
        val extension=if(task.format==DownloadFormat.Epub)"epub" else "txt"
        val mime=if(task.format==DownloadFormat.Epub)"application/epub+zip" else "text/plain"
        val title=task.title.replace(Regex("[^A-Za-z0-9\\p{L}\\p{N}._-]"),"_").trim('_').take(72).ifBlank{"novalpie"}
        val name="${title}_${task.bookId}_${task.createdAt}.$extension"
        if(Build.VERSION.SDK_INT>=29) {
            val resolver=context.contentResolver
            val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME,name);put(MediaStore.Downloads.MIME_TYPE,mime)
                put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);put(MediaStore.Downloads.IS_PENDING,1)
            }) ?: throw IOException("无法创建下载文件，检查剩余空间")
            try {
                resolver.openOutputStream(uri,"w")?.use {copyNativeDownloadFilePausable(file,it,paused)} ?: throw IOException("无法保存下载文件")
                // MediaStore's SIZE column can remain zero until the pending row is published.
                // Validate the actual descriptor, not that asynchronously updated database cache.
                val actual=resolver.openFileDescriptor(uri,"r")?.use{it.statSize}
                if(actual==null||actual<0L) {
                    val copied=resolver.openInputStream(uri)?.use {input->val buffer=ByteArray(64*1024);var bytes=0L;while(true){val n=input.read(buffer);if(n<0)break;bytes+=n};bytes}
                    if(copied!=file.length())throw IOException("下载保存不完整：预期${file.length()}，实际$copied")
                } else if(actual!=file.length())throw IOException("下载保存不完整：预期${file.length()}，实际$actual")
                val committed=resolver.update(uri,ContentValues().apply{put(MediaStore.Downloads.IS_PENDING,0)},null,null)
                if(committed<=0)throw IOException("下载文件发布失败")
                return uri.toString()
            } catch(failure:Throwable){
                runCatching{resolver.delete(uri,null,null)}
                if(failure is CancellationException)throw failure
                if(failure !is IOException)throw failure
                // Some OEM/emulator public FUSE providers reject very large files even though
                // the app-private archive is already complete. Keep that exact verified file;
                // history/notification explicitly label this as App-local, not system Downloads.
                return keepCompletedFileLocally(task,file,name)
            }
        }
        val root=context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: throw IOException("无法打开下载目录")
        root.mkdirs()
        val target=File(root,name)
        val part=File(root,".$name.part")
        try {
            part.outputStream().use{copyNativeDownloadFilePausable(file,it,paused)}
            if(target.exists())throw IOException("同名下载文件已存在，请从下载记录打开")
            if(!part.renameTo(target))throw IOException("无法发布下载文件")
            return target.toURI().toString()
        } finally {part.delete()}
    }

    internal fun keepCompletedFileLocally(task:DownloadTask,file:File,name:String):String {
        val allowedWork=File(context.noBackupFilesDir,"download-work/${task.id}").canonicalFile
        require(file.canonicalFile.parentFile==allowedWork){"只有本任务的完整打包文件可转存"}
        val root=File(context.filesDir,"native-downloads").canonicalFile
        if(!root.isDirectory&&!root.mkdirs())throw IOException("公共目录保存失败，本机下载目录也不可写；完整文件已保留待重试")
        val destination=File(root,name).canonicalFile
        require(destination.parentFile==root)
        if(destination.exists())throw IOException("本机已存在同名下载，完整临时文件已保留")
        val uri=localUri(destination)
        if(!file.renameTo(destination))throw IOException("无法转存本机下载；完整临时文件已保留待重试")
        return uri.toString()
    }
}
