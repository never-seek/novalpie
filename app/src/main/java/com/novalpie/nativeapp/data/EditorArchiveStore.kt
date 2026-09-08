package com.novalpie.nativeapp.data

import android.content.Context
import android.util.AtomicFile
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

class EditorArchiveStore(
    context: Context,
    directoryName: String = "epub-editor-archives",
    private val beforeMetadataCommit: () -> Unit = {},
) {
    private val directory = File(context.filesDir, directoryName).canonicalFile.also {
        require(it.parentFile == context.filesDir.canonicalFile) { "存档目录超出应用范围" }
    }

    @Synchronized fun save(archive: EditorArchive) {
        ensureDirectory()
        val metadataTarget = archiveFile(archive.id)
        val previous = readMetadata(metadataTarget)
        val previousBody = previous?.let { bodyFile(archive.id, it) }
        val body = File(directory, "${metadataTarget.nameWithoutExtension}.body-${UUID.randomUUID()}.txt")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            // Write a new immutable body first. Never delete the last committed body to save it.
            FileOutputStream(body).use { output ->
                val writer = DigestOutputStream(output, digest).bufferedWriter(Charsets.UTF_8)
                writer.write(archive.textContent); writer.flush(); output.fd.sync()
            }
            val metadata = toJson(archive).put("schema", 2).put("textFile", body.name)
                .put("textSha256", digest.digest().hex()).put("textBytes", body.length())
            beforeMetadataCommit()
            val bytes = metadata.toString().toByteArray(Charsets.UTF_8)
            val atomic = AtomicFile(metadataTarget)
            val stream = atomic.startWrite()
            try { stream.write(bytes); atomic.finishWrite(stream) }
            catch (failure: Exception) { atomic.failWrite(stream); throw failure }
            // AtomicFile can only log a failed rename; acknowledge after verifying committed bytes.
            check(atomic.readFully().contentEquals(bytes)) { "存档提交未完成，旧存档保留" }
            if (previousBody != null && previousBody != body && previous?.has("textFile") == true) {
                previousBody.delete() // A no-longer-referenced generation of this archive only.
            }
        } catch (failure: Exception) {
            val committed = runCatching { readMetadata(metadataTarget)?.optString("textFile") == body.name }.getOrNull()
            if (committed == false) body.delete()
            throw IOException("保存存档未完成，已有存档与编辑内容不会被清空", failure)
        }
    }

    @Synchronized fun list(): List<EditorArchive> {
        if (!directory.exists()) return emptyList()
        return metadataFiles().map { file ->
            runCatching { fromJson(requireNotNull(readMetadata(file)), "") }.getOrElse {
                EditorArchive(file.nameWithoutExtension, "存档信息损坏（原文件保留）", file.lastModified(), "")
            }
        }
            .sortedByDescending(EditorArchive::timestamp)
    }

    @Synchronized fun load(id: String): EditorArchive? {
        val file = archiveFile(id)
        val metadata = readMetadata(file) ?: return null
        val body = bodyFile(id, metadata)
        if (!body.isFile) throw IOException("存档正文缺失，未替换当前编辑内容；原文件保留")
        val digest = MessageDigest.getInstance("SHA-256")
        val text = java.security.DigestInputStream(body.inputStream(), digest).reader(Charsets.UTF_8).use { it.readText() }
        if (metadata.has("textFile") && (metadata.optLong("textBytes", -1) != body.length() ||
                    metadata.optString("textSha256") != digest.digest().hex())) {
            throw IOException("存档正文校验失败，可能已损坏；未替换当前编辑内容，原文件保留")
        }
        return try { fromJson(metadata, text) } catch (failure: Exception) { throw IOException("存档信息损坏，原文件保留", failure) }
    }

    @Synchronized fun delete(id: String) {
        val file = archiveFile(id)
        val body = runCatching { readMetadata(file)?.let { bodyFile(id, it) } }.getOrNull()
        listOf(file, File(file.path + ".bak"), File(file.path + ".new"), File(file.path + ".tmp")).forEach {
            if (it.exists() && !it.delete()) throw IOException("删除存档信息失败，正文保留")
        }
        val owned = listOfNotNull(body, archiveTextFile(id)) + directory.listFiles().orEmpty().filter {
            val prefix = file.nameWithoutExtension + ".body-"
            it.name.startsWith(prefix) && it.name.removePrefix(prefix).matches(Regex("[0-9a-f-]{36}\\.txt"))
        }
        owned.distinct().forEach { if (it.exists() && !it.delete()) throw IOException("删除存档正文失败") }
    }

    @Synchronized fun clear() {
        metadataFiles().forEach { delete(it.nameWithoutExtension) }
        // Only this store's uncommitted body generations, not arbitrary files in the directory.
        directory.listFiles().orEmpty().filter { it.name.matches(Regex("[A-Za-z0-9._-]+\\.body-[0-9a-f-]{36}\\.txt")) }.forEach {
            if (!it.delete()) throw IOException("清理未完成的存档正文失败")
        }
    }

    private fun metadataFiles(): List<File> = directory.listFiles().orEmpty().filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
        .map { File(directory, it.name.removeSuffix(".bak")) }.distinct()

    private fun readMetadata(file: File): JSONObject? {
        if (!file.exists() && !File(file.path + ".bak").exists()) return null
        return try { JSONObject(AtomicFile(file).readFully().toString(Charsets.UTF_8)) }
        catch (failure: Exception) { throw IOException("存档信息读取失败，原文件保留", failure) }
    }

    private fun bodyFile(id: String, metadata: JSONObject): File {
        if (!metadata.has("textFile")) return archiveTextFile(id)
        val name = metadata.getString("textFile")
        val prefix = archiveFile(id).nameWithoutExtension + ".body-"
        require(name.startsWith(prefix) && name.removePrefix(prefix).matches(Regex("[0-9a-f-]{36}\\.txt"))) { "存档正文路径无效" }
        return File(directory, name).canonicalFile.also { require(it.parentFile == directory) }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    private fun ensureDirectory() {
        if (!directory.exists() && !directory.mkdirs()) throw IOException("无法创建存档目录")
    }

    private fun archiveFile(id: String): File {
        val safeId = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        require(safeId.isNotBlank()) { "存档 ID 不能为空" }
        return File(directory, "$safeId.json")
    }

    private fun archiveTextFile(id: String): File = File(directory, archiveFile(id).nameWithoutExtension + ".txt")

    private fun toJson(archive: EditorArchive): JSONObject = JSONObject()
        .put("id", archive.id)
        .put("name", archive.name)
        .put("timestamp", archive.timestamp)
        .put("fileName", archive.fileName)
        .put("chapterCount", archive.chapterCount)
        .put("totalWords", archive.totalWords)
        .put("metadata", JSONObject()
            .put("title", archive.metadata.title)
            .put("author", archive.metadata.author)
            .put("description", archive.metadata.description)
            .put("language", archive.metadata.language)
            .put("tags", archive.metadata.tags)
            .put("isAdult", archive.metadata.isAdult)
            .put("source", archive.metadata.source)
            .put("sourceUrl", archive.metadata.sourceUrl))

    private fun fromJson(source: JSONObject, textContent: String): EditorArchive {
        val metadata = source.optJSONObject("metadata") ?: JSONObject()
        return EditorArchive(
            id = source.getString("id"),
            name = source.optString("name", "存档"),
            timestamp = source.optLong("timestamp"),
            textContent = textContent,
            metadata = EditorBookMetadata(
                title = metadata.optString("title"),
                author = metadata.optString("author"),
                description = metadata.optString("description"),
                language = metadata.optString("language", "zh"),
                tags = metadata.optString("tags"),
                isAdult = metadata.optBoolean("isAdult"),
                source = metadata.optString("source"),
                sourceUrl = metadata.optString("sourceUrl")
            ),
            fileName = source.optString("fileName").takeIf(String::isNotBlank),
            chapterCount = source.optInt("chapterCount"),
            totalWords = source.optInt("totalWords")
        )
    }
}
