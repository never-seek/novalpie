package com.novalpie.nativeapp.feature.workspace

import com.novalpie.nativeapp.model.WorkspaceLocalApiConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

internal interface TranslationSource {
    suspend fun chapters(bookId: Long): List<TranslationChapter>
    suspend fun prepare(bookId: Long, chapterId: Long): TranslationPreparation
    suspend fun submit(bookId: Long, chapterId: Long, content: String, title: String, result: TranslationResult, model: String, elapsedMs: Long): Boolean
}

internal class TranslationRunner(private val source: TranslationSource, private val model: TranslationModel, private val store: TranslationTaskStore) {
    suspend fun run(task: TranslationTask, config: WorkspaceLocalApiConfig, awaitReady: suspend () -> Unit, publish: suspend (TranslationTask) -> Unit): TranslationTask {
        require(config.id == task.configId)
        var current = task
        // The coordinator persists and publishes atomically before this callback returns.
        suspend fun emit(next: TranslationTask) { publish(next); current = next }
        awaitReady()
        if (task.phase in setOf(TranslationPhase.Submitting, TranslationPhase.SubmissionUncertain)) {
            emit(current.copy(phase = TranslationPhase.SubmissionUncertain, message = "上次提交结果仍未确认，请在网站核对后处理；没有重复提交"))
            return current
        }
        emit(current.copy(phase = TranslationPhase.Preparing, message = "正在读取待翻译章节"))
        val chapters = source.chapters(task.bookId).filter { it.status in setOf("pending", "failed", "user_translate") }
        require(chapters.map { it.id }.distinct().size == chapters.size) { "待翻章节ID重复，未开始" }
        val previousComplete = task.completed
        emit(current.copy(completed = previousComplete, total = previousComplete.size + chapters.count { it.id !in previousComplete }))
        require(chapters.isNotEmpty() || previousComplete.isNotEmpty()) { "没有可翻译章节；可能已完成、进入其他队列或源站未提供原稿" }
        for (chapter in chapters.filterNot { it.id in current.completed }) {
            awaitReady()
            emit(current.copy(phase = TranslationPhase.Preparing, currentChapterId = chapter.id, currentTitle = chapter.title, finishedChunks = 0, totalChunks = 0))
            val prepared = source.prepare(task.bookId, chapter.id)
            require(prepared.bookId == task.bookId && prepared.chapterId == chapter.id && prepared.chunks.isNotEmpty()) { "准备结果身份或分块为空" }
            require(prepared.title.isNotBlank()) { "标题为空，未开始模型调用" }
            require(prepared.chunks.map { it.index }.distinct().size == prepared.chunks.size) { "源分块编号重复" }
            val ordered = prepared.chunks.sortedBy { it.index }
            emit(current.copy(phase = TranslationPhase.Translating, totalChunks = ordered.size, message = "正在翻译章节"))
            val started = System.nanoTime()
            val updateMutex = Mutex()
            val nextChunk = AtomicInteger()
            val completedResults = arrayOfNulls<TranslationResult>(ordered.size)
            // Bound coroutine count as well as sockets. A single worker consumes source order.
            coroutineScope { List(minOf(ordered.size, config.concurrency.coerceIn(1, 8))) { launch(Dispatchers.IO) {
                while (true) {
                awaitReady()
                val slot = nextChunk.getAndIncrement()
                val chunk = ordered.getOrNull(slot) ?: break
                val digest = digest(chunk.content + "\n" + JSONObject(chunk.glossary).toString() + "\n" + config.model + "\n" + config.endpoint)
                val saved = store.checkpoint(task, chapter.id, "chunk-${chunk.index}")
                val result = if (saved?.optString("digest") == digest) TranslationResult(saved.getString("text"), saved.optLong("tokens"), saved.optString("additions", "[]"), saved.optString("removals", "[]"))
                    else model.translate(config, chunk.content, chunk.glossary).also { translated ->
                        store.checkpoint(task, chapter.id, "chunk-${chunk.index}", JSONObject().put("digest", digest).put("text", translated.text).put("tokens", translated.tokens).put("additions", translated.additions).put("removals", translated.removals))
                    }
                updateMutex.withLock { emit(current.copy(finishedChunks = current.finishedChunks + 1)) }
                completedResults[slot] = result
                }
            } }.joinAll() }
            val results = completedResults.map { requireNotNull(it) { "翻译分块结果缺失，未提交" } }
            awaitReady()
            val titleDigest = digest(prepared.title + config.model + config.endpoint)
            val savedTitle = withContext(Dispatchers.IO) { store.checkpoint(task, chapter.id, "title") }
            val title = if (savedTitle?.optString("digest") == titleDigest) savedTitle.getString("text") else model.translate(config, prepared.title, emptyMap()).text.also {
                withContext(Dispatchers.IO) { store.checkpoint(task, chapter.id, "title", JSONObject().put("digest", titleDigest).put("text", it)) }
            }
            val content = results.joinToString("\n") { it.text }
            require(content.isNotBlank()) { "整章译文为空，未提交" }
            val additions = JSONArray(); val removals = JSONArray()
            results.forEach { result ->
                JSONArray(result.additions).let { a -> for (i in 0 until a.length()) additions.put(a.get(i)) }
                JSONArray(result.removals).let { a -> for (i in 0 until a.length()) removals.put(a.get(i)) }
            }
            val total = TranslationResult(content, results.sumOf { it.tokens }, additions.toString(), removals.toString())
            withContext(Dispatchers.IO) { store.checkpoint(task, chapter.id, "submission", JSONObject().put("content", content).put("title", title).put("tokens", total.tokens).put("additions", additions).put("removals", removals)) }
            awaitReady()
            emit(current.copy(phase = TranslationPhase.Submitting, message = "正在提交完整章节"))
            // Any transport interruption after this checkpoint stays uncertain; no blind POST replay.
            awaitReady()
            val accepted = source.submit(task.bookId, chapter.id, content, title, total, config.model, (System.nanoTime() - started) / 1_000_000)
            if (!accepted) { emit(current.copy(phase = TranslationPhase.Failed, message = "服务器明确拒绝提交，结果保留在任务检查点")); return current }
            emit(current.copy(phase = TranslationPhase.Translating, completed = current.completed + chapter.id, message = "章节已提交"))
        }
        emit(current.copy(phase = TranslationPhase.Completed, message = "全部待翻章节已提交确认"))
        return current
    }
    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
