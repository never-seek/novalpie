package com.novalpie.nativeapp.feature.download

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.novalpie.nativeapp.core.AppContainer
import com.novalpie.nativeapp.ui.Text
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DownloadHistoryPanel(accountId:Long,onOpenBook:(Long)->Unit) {
    val context=LocalContext.current
    val container=remember(context){AppContainer.from(context)}
    val revision by container.environment.revisions.collectAsState()
    val currentDownload by container.downloads.state.collectAsState()
    val model=remember(accountId,revision){DownloadHistoryViewModel(accountId,container.downloadStore,container.downloads){NativeDownloadService.start(context,it)}}
    DisposableEffect(model){onDispose{model.dispose()}}
    val state=model.state
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("下载记录",style=MaterialTheme.typography.titleLarge)
        Text("本机 EPUB / TXT · 已完成文件可打开或分享",style=MaterialTheme.typography.bodySmall)
        state.message?.let{Text(it,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(state.loading)CircularProgressIndicator()
        else if(state.entries.isEmpty())Text("暂无本机下载任务，从书籍详情选择 EPUB 或 TXT 下载")
        state.entries.forEach {entry->key(entry.task.id) {
            val task=entry.task
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(task.title,style=MaterialTheme.typography.titleMedium)
                    Text("${task.format.name.uppercase()} · ${if(task.applyReplacement)"应用替换"else"原文"} · 并发 ${task.requestedConcurrency}",style=MaterialTheme.typography.labelMedium)
                    Text(downloadStatusText(DownloadUiState(task,entry.running)),style=MaterialTheme.typography.bodySmall)
                    if(task.totalChapters>0||task.totalAssets>0)Text("章节 ${task.completedChapters}/${task.totalChapters} · 图片 ${task.completedAssets}/${task.totalAssets}",style=MaterialTheme.typography.labelSmall)
                    if(entry.running&&task.phase!=DownloadPhase.Paused)LinearProgressIndicator(Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick={onOpenBook(task.bookId)}){Text("书籍")}
                        if(entry.running) {
                            TextButton(onClick={model.togglePause(task)}){Text(if(task.phase==DownloadPhase.Paused)"继续"else"暂停")}
                            TextButton(onClick={model.cancel(task)}){Text("取消")}
                        } else if(downloadCanResume(task,false)) {
                            TextButton(enabled=!currentDownload.busy,onClick={model.continueTask(task)}){Text("继续下载")}
                        }
                        if(downloadCanOpen(task)) {
                            TextButton(onClick={runCatching{openCompletedDownload(context,task,false)}.onFailure{model.feedback("文件已移走或没有可用阅读器，请到系统下载目录查看")}}){Text("打开")}
                            TextButton(onClick={runCatching{openCompletedDownload(context,task,true)}.onFailure{model.feedback("文件已移走，无法分享；下载记录仍保留")}}){Text("分享")}
                        }
                    }
                }
            }
        }}
    }
}

/** Only this task's published URI is shared, never the private authorization/cache directory. */
internal fun openCompletedDownload(context:Context,task:DownloadTask,share:Boolean) {
    require(downloadCanOpen(task))
    val source=Uri.parse(task.destinationUri)
    val uri=when(source.scheme) {
        "content"->source.also{context.contentResolver.openFileDescriptor(it,"r")?.use{descriptor->require(descriptor.statSize!=0L)} ?: error("文件不存在")}
        "file"->{
            val root=context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.canonicalFile ?: error("下载目录不可用")
            val file=File(requireNotNull(source.path)).canonicalFile
            require(file.parentFile==root&&file.isFile)
            FileProvider.getUriForFile(context,"${context.packageName}.downloads",file)
        }
        else->error("下载地址无效")
    }
    val mime=if(task.format==DownloadFormat.Epub)"application/epub+zip"else"text/plain"
    val intent=Intent(if(share)Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
        if(share){type=mime;putExtra(Intent.EXTRA_STREAM,uri)}else setDataAndType(uri,mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData=ClipData.newRawUri(task.title,uri)
    }
    context.startActivity(Intent.createChooser(intent,if(share)"分享下载文件"else"选择阅读器"))
}
