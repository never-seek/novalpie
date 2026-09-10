package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.UploadActionResult
import com.novalpie.nativeapp.model.UploadBatchCheckpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

internal class UploadBatchFailure(val checkpoint: UploadBatchCheckpoint, cause: Exception) : IOException(
    "${checkpoint.novelId?.let { "书籍 $it：" }.orEmpty()}已确认 ${checkpoint.nextBatch}/${checkpoint.totalBatches} 批；" +
        (if (checkpoint.inFlight) "当前批结果未确认，请核对目录，未自动重发。" else "后续批次尚未完成，可继续已确认的断点。") +
        " ${cause.message.orEmpty()}", cause,
)

/** Await durable state before every write; a late/missing acknowledgement cannot advance it. */
internal suspend fun runUploadBatches(
    signature: String,
    total: Int,
    existingBookId: Long?,
    resume: UploadBatchCheckpoint?,
    checkpoint: suspend (UploadBatchCheckpoint) -> Unit,
    send: suspend (index: Int, novelId: Long?) -> UploadActionResult,
): UploadActionResult {
    require(total > 0)
    var progress = resume ?: UploadBatchCheckpoint(signature, existingBookId, 0, total, false)
    require(progress.signature == signature && progress.totalBatches == total) { "上传内容已变化，不能沿用旧批次；请保留旧任务并核对作品" }
    require(progress.nextBatch in 0..total && (progress.nextBatch == 0 || (progress.novelId ?: 0) > 0)) { "上传批次检查点损坏，未发送请求" }
    require(existingBookId == null || progress.novelId == existingBookId) { "上传目标已变化，未发送请求" }
    if (progress.inFlight) throw UploadBatchFailure(progress, IOException("上次请求可能已经写入，不能安全重复此批"))
    var result = UploadActionResult(true, novelId = progress.novelId)
    try {
        for (index in progress.nextBatch until total) {
            currentCoroutineContext().ensureActive()
            val pending = progress.copy(inFlight = true)
            // A storage failure here leaves the previous confirmed boundary, with zero new POSTs.
            checkpoint(pending)
            progress = pending
            currentCoroutineContext().ensureActive()
            result = send(index, progress.novelId)
            if (!result.success) {
                val rejected = progress.copy(inFlight = false)
                checkpoint(rejected)
                progress = rejected
                if (total == 1 && index == 0) return result
                throw IOException(result.message ?: "服务器拒绝此上传批次")
            }
            val id = result.novelId ?: progress.novelId
            check((id ?: 0) > 0) { "上传回执缺少新书 ID，请先核对作品列表" }
            check(progress.novelId == null || id == progress.novelId) { "上传回执的书籍身份不符，请先核对目录" }
            progress = progress.copy(novelId = id)
            val completed = progress.copy(novelId = id, nextBatch = index + 1, inFlight = false)
            checkpoint(completed)
            progress = completed
            result = result.copy(novelId = id)
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: Exception) { throw UploadBatchFailure(progress, failure) }
    return result
}
