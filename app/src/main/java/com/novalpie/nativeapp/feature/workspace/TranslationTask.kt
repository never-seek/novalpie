package com.novalpie.nativeapp.feature.workspace

import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

internal enum class TranslationPhase { Queued, Preparing, Translating, Submitting, SubmissionUncertain, Paused, Failed, Completed, Cancelled }
internal data class TranslationChapter(val id: Long, val title: String, val number: Int, val status: String)
internal data class TranslationChunk(val index: Int, val content: String, val glossary: Map<String, String>)
internal data class TranslationPreparation(val bookId: Long, val chapterId: Long, val title: String, val chunks: List<TranslationChunk>)
internal data class TranslationTask(
    val id: String = UUID.randomUUID().toString(), val accountId: Long, val bookId: Long, val title: String,
    val configId: Long, val phase: TranslationPhase = TranslationPhase.Queued,
    val completed: Set<Long> = emptySet(), val total: Int = 0, val currentChapterId: Long? = null,
    val currentTitle: String? = null, val finishedChunks: Int = 0, val totalChunks: Int = 0,
    val message: String? = null, val updatedAt: Long = System.currentTimeMillis(),
    val targets: List<TranslationChapter>? = null,
) {
    override fun toString() = "TranslationTask(id=$id,bookId=$bookId,phase=$phase)"
}

/** Each task owns one directory. Records/chunk results never contain API keys or browser sessions. */
internal class TranslationTaskStore(rootDirectory: File) {
    private val root = rootDirectory.canonicalFile.apply { check(isDirectory || mkdirs()) { "无法创建任务目录" } }
    fun directory(id: String): File {
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,80}")))
        return File(root, id).canonicalFile.also { require(it.parentFile == root); check(it.isDirectory || it.mkdirs()) { "无法创建任务目录" } }
    }
    @Synchronized fun save(task: TranslationTask) {
        require(task.accountId > 0 && task.bookId > 0 && task.configId > 0)
        task.targets?.let { targets ->
            require(targets.all { it.id > 0 } && targets.map { it.id }.distinct().size == targets.size)
            require(task.completed.all { id -> targets.any { it.id == id } })
        }
        write(File(directory(task.id), "task.json"), JSONObject().put("schema", 2).put("id", task.id).put("account", task.accountId)
            .put("book", task.bookId).put("title", task.title).put("config", task.configId).put("phase", task.phase.name)
            .put("completed", JSONArray(task.completed.toList())).put("total", task.total).put("chapter", task.currentChapterId ?: JSONObject.NULL)
            .put("chapterTitle", task.currentTitle ?: JSONObject.NULL).put("chunks", task.finishedChunks).put("totalChunks", task.totalChunks)
            .put("message", task.message ?: JSONObject.NULL).put("updated", System.currentTimeMillis())
            .put("targets", task.targets?.let { chapters -> JSONArray().apply { chapters.forEach { chapter ->
                put(JSONObject().put("id", chapter.id).put("title", chapter.title).put("number", chapter.number).put("status", chapter.status))
            } } } ?: JSONObject.NULL))
    }
    @Synchronized fun load(accountId: Long): List<TranslationTask> = root.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { dir ->
        run {
            require(dir.canonicalFile.parentFile == root)
            val json = read(File(dir, "task.json")) ?: return@run null
            require(json.getInt("schema") in 1..2 && json.getString("id") == dir.name)
            if (json.getLong("account") != accountId) return@run null
            val stored = TranslationPhase.valueOf(json.getString("phase"))
            TranslationTask(id = dir.name, accountId = accountId, bookId = json.getLong("book"), title = json.getString("title"), configId = json.getLong("config"),
                phase = when (stored) { TranslationPhase.Submitting -> TranslationPhase.SubmissionUncertain; TranslationPhase.Queued, TranslationPhase.Preparing, TranslationPhase.Translating -> TranslationPhase.Paused; else -> stored },
                completed = json.getJSONArray("completed").let { a -> (0 until a.length()).map { a.getLong(it) }.toSet() }, total = json.optInt("total"),
                currentChapterId = json.optLong("chapter").takeIf { it > 0 }, currentTitle = json.text("chapterTitle"),
                finishedChunks = json.optInt("chunks"), totalChunks = json.optInt("totalChunks"), message = json.text("message"), updatedAt = json.optLong("updated"),
                targets = if (json.isNull("targets")) null else json.getJSONArray("targets").let { a -> (0 until a.length()).map { index ->
                    a.getJSONObject(index).let { item -> TranslationChapter(item.getLong("id"), item.getString("title"), item.getInt("number"), item.getString("status")) }
                }.also { targets -> require(targets.all { it.id > 0 } && targets.map { it.id }.distinct().size == targets.size) } })
        }
    }.sortedByDescending { it.updatedAt }
    @Synchronized fun checkpoint(task: TranslationTask, chapterId: Long, name: String, data: JSONObject) {
        require(chapterId > 0 && name.matches(Regex("[a-z0-9_-]+")))
        write(File(directory(task.id), "$chapterId-$name.json"), data)
    }
    @Synchronized fun checkpoint(task: TranslationTask, chapterId: Long, name: String): JSONObject? {
        require(chapterId > 0 && name.matches(Regex("[a-z0-9_-]+")))
        return read(File(directory(task.id), "$chapterId-$name.json"))
    }
    @Synchronized fun remove(id: String) { val dir = directory(id); require(dir.parentFile == root); check(dir.deleteRecursively()) { "无法清理此任务" } }
    private fun write(file: File, data: JSONObject) {
        val bytes = data.toString().toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(file); val out = atomic.startWrite()
        try { out.write(bytes); atomic.finishWrite(out) } catch (failure: Throwable) { atomic.failWrite(out); throw failure }
        // AtomicFile reports a failed rename by logging, not throwing. Do not acknowledge a phase
        // or send a billable POST until the committed bytes can actually be read back.
        atomic.openRead().use { input ->
            val buffer = ByteArray(65536)
            var offset = 0
            while (offset < bytes.size) {
                val count = input.read(buffer, 0, minOf(buffer.size, bytes.size - offset))
                check(count > 0 && (0 until count).all { buffer[it] == bytes[offset + it] }) { "翻译检查点写入未完成，任务已停止" }
                offset += count
            }
            check(input.read() == -1) { "翻译检查点写入不完整，任务已停止" }
        }
    }
    private fun read(file: File): JSONObject? = if (!file.exists() && !File(file.path + ".bak").exists()) null else JSONObject(AtomicFile(file).readFully().toString(Charsets.UTF_8))
    private fun JSONObject.text(key: String) = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
}
