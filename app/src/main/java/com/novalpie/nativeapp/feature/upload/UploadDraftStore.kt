package com.novalpie.nativeapp.feature.upload

import android.util.AtomicFile
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.UUID

/** Account/book isolated records. No auth token is stored. Chapter bytes are immutable generations. */
internal class UploadDraftStore(rootDirectory: File) {
    private val root = rootDirectory.canonicalFile
    private data class Chapters(val values: WeakReference<List<UploadChapter>>, val file: String, val bytes: Long, val sha: String)
    private val cached = object : LinkedHashMap<String, Chapters>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Chapters>?) = size > 16
    }
    private fun directory(account: Long, book: Long?): File {
        require(account > 0 && (book == null || book > 0))
        val parent = File(root, account.toString()).canonicalFile.also { require(it.parentFile == root) }
        return File(parent, book?.toString() ?: "new").canonicalFile.also { require(it.parentFile == parent) }
    }
    private fun metadata(dir: File): JSONObject? {
        val file = File(dir, "draft.json")
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        return JSONObject(AtomicFile(file).readFully().toString(Charsets.UTF_8))
    }
    @Synchronized fun save(account: Long, state: UploadBookState) {
        val dir = directory(account, state.existingNovelId)
        check(dir.isDirectory || dir.mkdirs()) { "无法创建上传草稿目录" }
        val previous = metadata(dir)
        val values = (state.chapters as? LoadResult.Success)?.value
        val content = values?.let { chapters ->
            cached[dir.path]?.takeIf { it.values.get() === chapters && File(dir, it.file).length() == it.bytes }
                ?: writeChapters(dir, chapters).also { cached[dir.path] = it }
        }
        if (state.submitResult == LoadResult.Loading && content != null) {
            val sha = MessageDigest.getInstance("SHA-256")
            chapterFile(dir, content.file).inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val count = input.read(buffer); if (count < 0) break; if (count > 0) sha.update(buffer, 0, count) }
            }
            check(sha.digest().hex() == content.sha) { "上传草稿章节校验失败，本次未发送上传请求" }
        }
        val draft = state.draft
        val form = JSONObject().put("title", draft.title).put("originalTitle", draft.titleTranslation).put("author", draft.author)
            .put("description", draft.description).put("language", draft.language).put("spans", draft.spans).put("adult", draft.isAdult)
            .put("source", draft.source).put("sourceUrl", draft.sourceUrl).put("tags", draft.tagsText).put("submitType", draft.submitType).put("cover", draft.coverUrl)
        val doc = state.selectedFile?.let { JSONObject().put("uri", it.uri).put("name", it.displayName).put("size", it.sizeBytes).put("mime", it.mimeType ?: JSONObject.NULL) }
        val success = (state.submitResult as? LoadResult.Success)?.value
        val status = when {
            state.submitResult == LoadResult.Loading || state.submissionUncertain -> "Uncertain"
            success?.success == true -> "Completed"
            else -> "Draft"
        }
        val data = JSONObject().put("schema", 1).put("account", account).put("book", state.existingNovelId ?: JSONObject.NULL)
            .put("draft", form).put("document", doc ?: JSONObject.NULL).put("serverFile", state.serverFilePath ?: JSONObject.NULL)
            .put("status", status).put("resultBook", success?.novelId ?: JSONObject.NULL).put("message", state.actionMessage ?: JSONObject.NULL)
            .put("wasParsing", state.processing && state.submitResult != LoadResult.Loading)
            .put("chapterFile", content?.file ?: JSONObject.NULL).put("chapterBytes", content?.bytes ?: 0).put("chapterSha", content?.sha ?: "")
        state.batchCheckpoint?.let { batch -> data.put("batch", JSONObject().put("signature", batch.signature)
            .put("novelId", batch.novelId ?: JSONObject.NULL).put("next", batch.nextBatch).put("total", batch.totalBatches).put("inFlight", batch.inFlight)) }
        val atomic = AtomicFile(File(dir, "draft.json")); val bytes = data.toString().toByteArray(Charsets.UTF_8)
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) } catch (failure: Exception) { atomic.failWrite(output); throw failure }
        check(atomic.readFully().contentEquals(bytes)) { "上传草稿尚未完整保存，未发送请求" }
        previous?.text("chapterFile")?.takeIf { it != content?.file }?.let { name -> chapterFile(dir, name).delete() }
    }
    @Synchronized fun load(account: Long, book: Long?): UploadBookState? {
        val dir = directory(account, book)
        val data = metadata(dir) ?: return null
        check(data.getInt("schema") == 1 && data.getLong("account") == account && data.optLong("book").takeIf { it > 0 } == book) { "上传草稿身份不符，原文件保留" }
        val chapterName = data.text("chapterFile")
        val chapters = chapterName?.let { name ->
            val file = chapterFile(dir, name)
            check(file.isFile && file.length() == data.getLong("chapterBytes")) { "上传草稿章节缺失或不完整，原文件保留" }
            readChapters(file, data.getString("chapterSha")).also { values -> cached[dir.path] = Chapters(WeakReference(values), name, file.length(), data.getString("chapterSha")) }
        }
        val f = data.getJSONObject("draft")
        val draft = UploadBookDraft(f.getString("title"), f.getString("originalTitle"), f.getString("author"), f.getString("description"),
            f.getString("language"), f.getString("spans"), f.getBoolean("adult"), f.getString("source"), f.getString("sourceUrl"),
            f.getString("tags"), f.getString("submitType"), f.getString("cover"), chapters?.size ?: 0)
        val document = data.optJSONObject("document")?.let { UploadDocument(it.getString("uri"), it.getString("name"), it.getLong("size"), it.text("mime")) }
        val batch = data.optJSONObject("batch")?.let { UploadBatchCheckpoint(it.getString("signature"), it.optLong("novelId").takeIf { id -> id > 0 },
            it.getInt("next"), it.getInt("total"), it.getBoolean("inFlight")) }
        val uncertain = batch?.inFlight ?: (data.getString("status") == "Uncertain")
        val message = when {
            batch != null && data.getString("status") != "Completed" -> "书籍 ${batch.novelId ?: "尚未确认"}：已确认 ${batch.nextBatch}/${batch.totalBatches} 批；" + if (batch.inFlight) "当前批结果未知，请核对目录，没有自动重发" else "可从已确认批次继续上传"
            uncertain -> "已恢复上传草稿；上次提交结果未确认，请先核对作品和目录，没有自动重发"
            data.getString("status") == "Completed" -> "上次上传已完成；请选择新文件或清空草稿后再创建新的上传"
            data.optBoolean("wasParsing") -> "已恢复填写内容；上次文件解析未完成，请重新选择原 EPUB"
            else -> "已恢复本账号的上传草稿"
        }
        val result: LoadResult<UploadActionResult> = when {
            uncertain -> LoadResult.Error(message)
            data.getString("status") == "Completed" -> LoadResult.Success(UploadActionResult(true, data.text("message"), data.optLong("resultBook").takeIf { it > 0 }))
            else -> LoadResult.Idle
        }
        return UploadBookState(book, draft, document, chapters?.let { LoadResult.Success(it) } ?: LoadResult.Idle,
            data.text("serverFile"), submitResult = result, actionMessage = message, submissionUncertain = uncertain, batchCheckpoint = batch)
    }
    @Synchronized fun discard(account: Long, book: Long?) {
        val dir = directory(account, book)
        check(dir.parentFile?.parentFile == root) { "上传草稿清理范围无效" }
        if (dir.exists() && !dir.deleteRecursively()) throw IOException("无法清除此上传草稿")
        cached.remove(dir.path)
    }
    private fun chapterFile(dir: File, name: String): File {
        require(name.matches(Regex("chapters-[0-9a-f-]{36}\\.bin"))) { "上传草稿章节路径无效" }
        return File(dir, name).canonicalFile.also { require(it.parentFile == dir) }
    }
    private fun writeChapters(dir: File, values: List<UploadChapter>): Chapters {
        val file = chapterFile(dir, "chapters-${UUID.randomUUID()}.bin")
        val sha = MessageDigest.getInstance("SHA-256")
        try {
            FileOutputStream(file).use { stream ->
                val output = DataOutputStream(DigestOutputStream(stream, sha).buffered())
                output.writeInt(1); output.writeInt(values.size)
                values.forEach { chapter ->
                    output.text(chapter.title); output.text(chapter.content); output.writeInt(chapter.chapterNumber); output.writeInt(chapter.hierarchyLevel)
                    output.writeInt(chapter.sectionPath.size); chapter.sectionPath.forEach { output.text(it) }
                    output.text(chapter.rawPath.orEmpty()); output.writeInt(chapter.spineIndex ?: -1)
                }
                output.flush(); stream.fd.sync()
            }
            return Chapters(WeakReference(values), file.name, file.length(), sha.digest().hex())
        } catch (failure: Exception) { file.delete(); throw failure }
    }
    private fun readChapters(file: File, expectedSha: String): List<UploadChapter> {
        val sha = MessageDigest.getInstance("SHA-256")
        var readBytes = 0L
        DataInputStream(DigestInputStream(file.inputStream(), sha).buffered()).use { input ->
            check(input.readInt() == 1) { "不支持的上传草稿章节版本" }
            val count = input.readInt(); check(count in 0..100000) { "上传草稿章节数异常" }
            fun text(): String {
                val size = input.readInt(); readBytes += size
                check(size in 0..(24 * 1024 * 1024) && readBytes <= 128L * 1024 * 1024) { "上传草稿章节数据过大或已损坏" }
                return ByteArray(size).also { input.readFully(it) }.toString(Charsets.UTF_8)
            }
            val result = List(count) {
                val title = text(); val body = text(); val number = input.readInt(); val depth = input.readInt()
                val levels = input.readInt(); check(levels in 0..100) { "上传草稿目录层级异常" }
                val parents = List(levels) { text() }; val path = text().ifEmpty { null }; val spine = input.readInt().takeIf { it >= 0 }
                UploadChapter(title, body, number, depth, parents, path, spine)
            }
            check(input.read() == -1 && sha.digest().hex() == expectedSha) { "上传草稿章节校验失败，原文件保留" }
            return result
        }
    }
    private fun DataOutputStream.text(value: String) { val bytes = value.toByteArray(Charsets.UTF_8); writeInt(bytes.size); write(bytes) }
    private fun JSONObject.text(key: String) = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
}
