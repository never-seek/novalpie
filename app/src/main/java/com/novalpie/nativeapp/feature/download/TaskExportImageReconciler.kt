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
internal class TaskExportImageReconciler(private val directory:File,
    private val onIssue:(DownloadIntegrityIssue)->Unit={},
    private val sourceImages: suspend (Int) -> List<String>) {

    init {
        ensureDirectory()
    }

    private fun ensureDirectory(): Boolean {
        if (directory.isDirectory) return true
        directory.mkdirs()
        return directory.isDirectory
    }

    suspend fun prepare(source: java.io.Reader, concurrency: Int, awaitReady: suspend () -> Unit = {}) = coroutineScope {
        ensureDirectory()
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
            },awaitReady,preambleDirectory=directory)
        } finally { pending.close() }
        jobs.joinAll()
        sourceOnly.get()
    }

    private fun urls(body: String) = Regex("\\[图片(?:[:：]|\\s)*?(.*?)\\]").findAll(body)
        .mapNotNull { normalizedExportImageUrl(it.groupValues[1]) }.toList()
    private fun hasDuplicates(body: String): Boolean = urls(body).let { it.size != it.distinct().size }

    suspend fun reconcile(number: Int, body: String): String = checkChapter(number, body).body

    /** Retry an unknown/mismatched source without trusting a stale successful quota snapshot. */
    fun retryUnverified() {
        directory.listFiles().orEmpty().filter {it.isFile&&it.name.matches(Regex("[0-9]+-mismatch\\.json"))}.forEach {m->
            val number=m.name.substringBefore('-');File(directory,"$number.json").delete();m.delete()
        }
    }

    private suspend fun checkChapter(number: Int, body: String): ExportImageReconciliation {
        require(number > 0)
        val urls = urls(body)
        if (urls.size == urls.distinct().size) return ExportImageReconciliation(body, 0)
        val hash = MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }
        val record = File(directory, "$number.json")
        val cached = if (record.isFile) runCatching { JSONObject(record.readText()) }.getOrNull() else null
        val cachedExpected=runCatching {
            if(cached?.optInt("schema")==1&&cached.optString("bodyHash")==hash)
                cached.getJSONArray("images").let {a->(0 until a.length()).map{a.getString(it)}} else null
        }.getOrNull()
        val expected=try {cachedExpected ?: try { sourceImages(number) } catch (failure: Exception) {
            if (failure !is com.novalpie.nativeapp.data.NovalPieApiException || failure.statusCode !in setOf(500, 502, 503, 504)) throw failure
            // Only a read-only chapter GET is repeated, once. Authorization/model/submit calls
            // are never inside this boundary, and cancellation interrupts the delay immediately.
            delay(700)
            sourceImages(number)
        }}catch(cancelled:CancellationException){throw cancelled}
        catch(failure:Exception) {
            // A GET failure is unknown, not proof that the reader legitimately has zero pictures.
            val issue=DownloadIntegrityIssue(DownloadIntegrityCode.ImageSourceUnavailable,number,hash)
            onIssue(issue)
            if(ensureDirectory())runCatching {File(directory,"$number-mismatch.json").writeText(JSONObject().put("chapterNumber",number).put("bodyHash",hash).put("issue",issue.code.name).toString())}
            if(failure is com.novalpie.nativeapp.data.NovalPieApiException&&failure.statusCode in setOf(401,403))throw failure
            return ExportImageReconciliation(body,0,0)
        }
        val result = try { reconcileExportImageOccurrences(body, expected) } catch (mismatch: IllegalArgumentException) {
            onIssue(DownloadIntegrityIssue(DownloadIntegrityCode.ImageSourceMismatch,number,hash))
            if (ensureDirectory()) {
                fun resource(value: String): String {
                    if (value.startsWith("data:", true)) return value.substringBefore(',').take(100) + ",<inline length=${value.length}>"
                    val normalized = normalizedExportImageUrl(value).orEmpty()
                    return runCatching { java.net.URI(normalized).let { uri -> "${uri.scheme}://${uri.host}${uri.rawPath}" } }.getOrDefault("invalid scheme=" + value.takeWhile(Char::isLetter).take(12))
                }
                runCatching {
                    File(directory, "$number-mismatch.json").writeText(JSONObject().put("chapterNumber", number)
                        .put("export", JSONArray(urls.map(::resource))).put("reader", JSONArray(expected.map(::resource)))
                        .put("reason", mismatch.message).toString())
                }
            }
            return ExportImageReconciliation(body, 0, 0)
        }
        if (ensureDirectory()) {
            val data = JSONObject().put("schema", 1).put("bodyHash", hash).put("images", JSONArray(expected))
                .put("exportOccurrences", urls.size).put("retainedOccurrences", urls.size - result.removed)
                .put("sourceOnlyOccurrences", result.sourceOnlyOccurrences).put("removed", result.removed).toString()
            runCatching {
                record.writeText(data)
                File(directory, "$number-mismatch.json").takeIf { it.isFile }?.delete()
            }
        }
        return result
    }

}
