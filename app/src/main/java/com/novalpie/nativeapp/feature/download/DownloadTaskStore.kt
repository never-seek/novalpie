package com.novalpie.nativeapp.feature.download

import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class DownloadFormat { Epub, Txt }
internal enum class DownloadPhase {
    Queued, Authorizing, AuthorizationUncertain, Transferring, Packaging, Saving, Paused, NeedsRetry, Failed, Cancelled, Completed,
}

/** Durable task metadata. Store under noBackupFilesDir; never print authorization/snapshot values. */
internal data class DownloadTask(
    val id: String,
    val accountId: Long,
    val bookId: Long,
    val title: String,
    val format: DownloadFormat,
    val applyReplacement: Boolean = false,
    val replacementSnapshot: String? = null,
    val requestedConcurrency: Int = 8,
    val phase: DownloadPhase = DownloadPhase.Queued,
    val authorizationAttempted: Boolean = false,
    val authorizationFile: String? = null,
    val completedChapters: Int = 0,
    val completedAssets: Int = 0,
    val destinationUri: String? = null,
    val failure: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val totalChapters:Int=0,
    val totalAssets:Int=0,
    val failedAssets:Int=0,
) {
    val mayAuthorizeAgain: Boolean get() = !authorizationAttempted && authorizationFile == null &&
        phase !in setOf(DownloadPhase.AuthorizationUncertain,DownloadPhase.Completed,DownloadPhase.Cancelled)

    override fun toString(): String = "DownloadTask(id=$id,bookId=$bookId,format=$format,phase=$phase)"
}

internal data class RecoveredDownloads(val tasks: List<DownloadTask>, val unreadableFiles: List<String>)

/** Synchronous disk contract; callers use an IO dispatcher, never Compose or a viewport callback. */
internal class DownloadTaskStore(directory: File) {
    private val root = directory.canonicalFile
    private val changes=MutableStateFlow(0L)
    val revisions=changes.asStateFlow()
    init { if (!root.isDirectory && !root.mkdirs()) throw IOException("无法创建下载任务目录") }

    @Synchronized fun save(task: DownloadTask) {
        require(task.accountId > 0 && task.bookId > 0)
        require(task.completedChapters >= 0 && task.completedAssets >= 0)
        val file = ownedFile(task.id)
        val data = JSONObject()
            .put("schema", 1).put("id", task.id).put("account_id", task.accountId).put("book_id", task.bookId)
            .put("title", task.title).put("format", task.format.name).put("apply_replacement", task.applyReplacement)
            .put("replacement_snapshot", task.replacementSnapshot ?: JSONObject.NULL)
            .put("concurrency", task.requestedConcurrency).put("phase", task.phase.name)
            .put("authorization_attempted", task.authorizationAttempted)
            .put("authorization_file", task.authorizationFile ?: JSONObject.NULL)
            .put("completed_chapters", task.completedChapters).put("completed_assets", task.completedAssets)
            .put("total_chapters",task.totalChapters).put("total_assets",task.totalAssets).put("failed_assets",task.failedAssets)
            .put("destination_uri", task.destinationUri ?: JSONObject.NULL).put("failure", task.failure ?: JSONObject.NULL)
            .put("created_at", task.createdAt).put("updated_at", task.updatedAt).toString().toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(data); atomic.finishWrite(output);changes.value++ }
        catch (failure: Throwable) { atomic.failWrite(output); throw failure }
    }

    @Synchronized fun recover(accountId: Long): RecoveredDownloads {
        val tasks = mutableListOf<DownloadTask>()
        val errors = mutableListOf<String>()
        // AtomicFile can leave only .bak after an interrupted replacement on older Android.
        // Open the logical base through AtomicFile so its recovery runs; .new is uncommitted.
        root.listFiles()?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
            ?.map { File(root,it.name.removeSuffix(".bak")) }?.distinctBy {it.name}
            ?.sortedBy { it.name }?.forEach { file ->
            try {
                require(file.canonicalFile.parentFile == root)
                val value = JSONObject(String(AtomicFile(file).readFully(), Charsets.UTF_8))
                require(value.getInt("schema") == 1)
                val id = value.getString("id")
                require(ownedFile(id) == file.canonicalFile)
                if (value.getLong("account_id") != accountId) return@forEach
                val storedPhase = DownloadPhase.valueOf(value.getString("phase"))
                val ticket = value.nullableString("authorization_file")
                val phase = when (storedPhase) {
                    DownloadPhase.Authorizing -> if (ticket == null) DownloadPhase.AuthorizationUncertain else DownloadPhase.NeedsRetry
                    DownloadPhase.Transferring, DownloadPhase.Packaging, DownloadPhase.Saving -> DownloadPhase.NeedsRetry
                    else -> storedPhase
                }
                tasks += DownloadTask(
                    id=id,accountId=accountId,bookId=value.getLong("book_id"),title=value.getString("title"),
                    format=DownloadFormat.valueOf(value.getString("format")),applyReplacement=value.getBoolean("apply_replacement"),
                    replacementSnapshot=value.nullableString("replacement_snapshot"),requestedConcurrency=value.getInt("concurrency"),
                    phase=phase,authorizationAttempted=value.optBoolean("authorization_attempted") || ticket != null || phase == DownloadPhase.AuthorizationUncertain,
                    authorizationFile=ticket,completedChapters=value.getInt("completed_chapters"),completedAssets=value.getInt("completed_assets"),
                    destinationUri=value.nullableString("destination_uri"),failure=value.nullableString("failure"),
                    createdAt=value.getLong("created_at"),updatedAt=value.getLong("updated_at"),
                    totalChapters=value.optInt("total_chapters"),totalAssets=value.optInt("total_assets"),failedAssets=value.optInt("failed_assets"),
                )
            } catch (_: Exception) {
                // A corrupt/newer record stays on disk for recovery, never becomes an empty task.
                errors += file.name
            }
        }
        return RecoveredDownloads(tasks.toList(),errors.toList())
    }

    private fun ownedFile(id: String): File {
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,95}"))) { "无效下载任务标识" }
        return File(root,"$id.json").canonicalFile.also { require(it.parentFile == root) }
    }

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
}

/** Keep the requested value visible; resource safety is a separate scheduler concern. */
internal fun effectiveDownloadConcurrency(requested: Int, workerMemoryBudgetBytes: Long): Int =
    minOf(requested.coerceAtLeast(1),16,(workerMemoryBudgetBytes / (8L*1024*1024)).coerceIn(1,16).toInt())
