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
        require(chapters.all { it.id > 0 } && chapters.map { it.id }.distinct().size == chapters.size) { "待翻章节ID无效或重复，未开始" }
        val previousComplete = task.completed
        if (task.targets == null && (task.total > 0 || previousComplete.isNotEmpty() || task.currentChapterId != null)) {
            emit(current.copy(phase = TranslationPhase.Failed, message = "旧翻译任务未记录完整章节范围，无法确认是否漏章；请核对网站后重新创建，旧检查点保留"))
            return current
        }
        val targets = task.targets ?: chapters
        require(targets.isNotEmpty()) { "没有可翻译章节；可能已完成、进入其他队列或源站未提供原稿" }
        require(previousComplete.all { id -> targets.any { it.id == id } }) { "翻译任务完成记录与原定章节不符，未开始" }
        // Freeze the initial scope durably before the first billable model request. A resumed
        // task must neither absorb newly arrived chapters nor claim vanished candidates completed.
        emit(current.copy(targets = targets, completed = previousComplete, total = targets.size))
        val pending = targets.filterNot { it.id in previousComplete }
        val available = chapters.associateBy { it.id }
        val missing = pending.filterNot { it.id in available }
        if (missing.isNotEmpty()) {
            emit(current.copy(phase = TranslationPhase.Failed, currentChapterId = missing.first().id, currentTitle = missing.first().title,
                message = "${missing.size}个原定待翻章节状态未确认；可能进入其他队列或被源站移除，请核对后重试，没有当作完成"))
            return current
        }
        for (chapter in pending) {
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
                val digest = inputDigest(chunk.content, chunk.glossary, config)
                val saved = store.checkpoint(task, chapter.id, "chunk-${chunk.index}")
                val result = if (saved?.optString("digest") == digest) readVerifiedResult(saved, chunk.content, chunk.glossary)
                    else validateResult(model.translate(config, chunk.content, chunk.glossary), chunk.content, chunk.glossary).also { translated ->
                        store.checkpoint(task, chapter.id, "chunk-${chunk.index}", resultCheckpoint(digest, translated))
                    }
                updateMutex.withLock { emit(current.copy(finishedChunks = current.finishedChunks + 1)) }
                completedResults[slot] = result
                }
            } }.joinAll() }
            val results = completedResults.map { requireNotNull(it) { "翻译分块结果缺失，未提交" } }
            awaitReady()
            val titleDigest = inputDigest(prepared.title, emptyMap(), config)
            val savedTitle = withContext(Dispatchers.IO) { store.checkpoint(task, chapter.id, "title") }
            val title = if (savedTitle?.optString("digest") == titleDigest) readVerifiedResult(savedTitle, prepared.title, emptyMap()).text
            else validateResult(model.translate(config, prepared.title, emptyMap()), prepared.title, emptyMap()).also {
                withContext(Dispatchers.IO) { store.checkpoint(task, chapter.id, "title", resultCheckpoint(titleDigest, it)) }
            }.text
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
    private fun resultCheckpoint(sourceDigest: String, result: TranslationResult) = JSONObject()
        .put("digest", sourceDigest).put("text", result.text).put("tokens", result.tokens)
        .put("additions", result.additions).put("removals", result.removals).put("resultSha256", resultDigest(result))

    private fun inputDigest(text: String, glossary: Map<String, String>, config: WorkspaceLocalApiConfig) = digest(
        JSONArray().put(text).put(JSONArray().apply { glossary.toSortedMap().forEach { (source, target) ->
            put(JSONArray().put(source).put(target))
        } }).put(config.model).put(config.endpoint).toString(),
    )

    private fun resultDigest(result: TranslationResult) = digest(JSONArray().put(result.text).put(result.tokens)
        .put(result.additions).put(result.removals).toString())

    private fun readVerifiedResult(saved: JSONObject, original: String, glossary: Map<String, String>): TranslationResult {
        try {
            val result = TranslationResult(saved.getString("text"), saved.getLong("tokens"), saved.getString("additions"), saved.getString("removals"))
            check(saved.optString("resultSha256") == resultDigest(result))
            return validateResult(result, original, glossary)
        } catch (_: Exception) {
            // Do not hide storage corruption behind another billable model invocation.
            error("翻译检查点不完整或已损坏；未提交、未重新调用模型，请核对后移除此失败任务重新创建")
        }
    }

    private fun validateResult(result: TranslationResult, original: String, glossary: Map<String, String>): TranslationResult {
        require(result.tokens >= 0) { "翻译用量格式异常，未提交" }
        decodeTranslationResponse(JSONObject().put("translation", result.text).put("table_add", JSONArray(result.additions))
            .put("table_remove", JSONArray(result.removals)).toString(), original)
        val removals = JSONArray(result.removals)
        for (index in 0 until removals.length()) require(glossary.containsKey(removals.getString(index))) { "翻译移除项不在当前词表，未提交" }
        return result
    }

    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
