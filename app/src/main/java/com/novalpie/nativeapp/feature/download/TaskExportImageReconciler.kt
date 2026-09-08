package com.novalpie.nativeapp.feature.download

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import com.novalpie.nativeapp.data.NativeEpubArchiveWriter
import com.novalpie.nativeapp.data.NativeDownloadChapterText

/** Only ambiguous duplicate chapters need a source read; task-owned snapshots are reused on retry. */
internal class TaskExportImageReconciler(private val directory: File,
    private val sourceImages: suspend (Int) -> List<String>) {
    suspend fun prepare(source: java.io.Reader, concurrency: Int, awaitReady: suspend () -> Unit = {}) = coroutineScope {
        val workers = concurrency.coerceIn(1, 4)
        val pending = Channel<Pair<Int, String>>(workers)
        val sourceOnly = java.util.concurrent.atomic.AtomicInteger()
        val jobs = List(workers) { launch(Dispatchers.IO) {
            for ((number, body) in pending) { awaitReady(); sourceOnly.addAndGet(checkChapter(number, body).sourceOnlyOccurrences) }
        } }
        try {
            val discard = object : java.io.Writer() {
                override fun write(cbuf: CharArray, off: Int, len: Int) = Unit
                override fun flush() = Unit
                override fun close() = Unit
            }
            NativeEpubArchiveWriter.writeTransformedTxt(discard, source, { number, title, body ->
                if (hasDuplicates(body)) pending.send(number to body)
                NativeDownloadChapterText(title, "")
            }, awaitReady)
        } finally { pending.close() }
        jobs.joinAll()
        sourceOnly.get()
    }

    private fun urls(body: String) = Regex("\\[图片(?:[:：]|\\s)*?(.*?)\\]").findAll(body)
        .mapNotNull { normalizedExportImageUrl(it.groupValues[1]) }.toList()
    private fun hasDuplicates(body: String): Boolean = urls(body).let { it.size != it.distinct().size }

    suspend fun reconcile(number: Int, body: String): String = checkChapter(number, body).body

    private suspend fun checkChapter(number: Int, body: String): ExportImageReconciliation {
        require(number > 0)
        val urls = urls(body)
        if (urls.size == urls.distinct().size) return ExportImageReconciliation(body, 0)
        val hash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }
        val record = File(directory, "$number.json")
        val cached = if (record.isFile) runCatching { JSONObject(record.readText()) }.getOrNull() else null
        val expected = if (cached?.optInt("schema") == 1 && cached.optString("bodyHash") == hash)
            cached.getJSONArray("images").let { a -> (0 until a.length()).map { a.getString(it) } }
        else try { sourceImages(number) } catch (failure: Exception) {
            if (failure !is com.novalpie.nativeapp.data.NovalPieApiException || failure.statusCode !in setOf(500, 502, 503, 504)) throw failure
            // Only a read-only chapter GET is repeated, once. Authorization/model/submit calls
            // are never inside this boundary, and cancellation interrupts the delay immediately.
            delay(700)
            sourceImages(number)
        }
        val result = try { reconcileExportImageOccurrences(body, expected) } catch (mismatch: IllegalArgumentException) {
            check(directory.isDirectory || directory.mkdirs()) { "无法保存插图校对检查点" }
            fun resource(value: String): String {
                if (value.startsWith("data:", true)) return value.substringBefore(',').take(100) + ",<inline length=${value.length}>"
                val normalized = normalizedExportImageUrl(value).orEmpty()
                return runCatching { java.net.URI(normalized).let { uri -> "${uri.scheme}://${uri.host}${uri.rawPath}" } }.getOrDefault("invalid scheme=" + value.takeWhile(Char::isLetter).take(12))
            }
            File(directory, "$number-mismatch.json").writeText(JSONObject().put("chapterNumber", number)
                .put("export", JSONArray(urls.map(::resource))).put("reader", JSONArray(expected.map(::resource))).toString())
            throw IllegalArgumentException("第${number}章插图无法校对：${mismatch.message}", mismatch)
        }
        check(directory.isDirectory || directory.mkdirs()) { "无法保存插图校对检查点" }
        val data = JSONObject().put("schema", 1).put("bodyHash", hash).put("images", JSONArray(expected))
            .put("exportOccurrences", urls.size).put("retainedOccurrences", urls.size - result.removed)
            .put("sourceOnlyOccurrences", result.sourceOnlyOccurrences).put("removed", result.removed).toString()
        record.writeText(data)
        check(record.readText() == data) { "插图校对检查点保存失败" }
        File(directory, "$number-mismatch.json").takeIf { it.isFile }?.delete()
        return result
    }

}
