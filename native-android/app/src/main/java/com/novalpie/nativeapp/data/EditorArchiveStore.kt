package com.novalpie.nativeapp.data

import android.content.Context
import com.novalpie.nativeapp.model.EditorArchive
import com.novalpie.nativeapp.model.EditorBookMetadata
import java.io.File
import java.io.IOException
import org.json.JSONObject

class EditorArchiveStore(
    context: Context,
    directoryName: String = "epub-editor-archives"
) {
    private val directory = File(context.filesDir, directoryName)

    fun save(archive: EditorArchive) {
        ensureDirectory()
        val metadataTarget = archiveFile(archive.id)
        val textTarget = archiveTextFile(archive.id)
        val metadataTemporary = File(directory, metadataTarget.name + ".tmp")
        val textTemporary = File(directory, textTarget.name + ".tmp")
        metadataTemporary.writeText(toJson(archive).toString(), Charsets.UTF_8)
        textTemporary.writeText(archive.textContent, Charsets.UTF_8)
        if (metadataTarget.exists() && !metadataTarget.delete()) throw IOException("无法替换现有存档信息")
        if (textTarget.exists() && !textTarget.delete()) throw IOException("无法替换现有存档正文")
        if (!metadataTemporary.renameTo(metadataTarget) || !textTemporary.renameTo(textTarget)) {
            throw IOException("保存存档失败")
        }
    }

    fun list(): List<EditorArchive> {
        if (!directory.exists()) return emptyList()
        return directory.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .mapNotNull { file -> runCatching { fromJson(JSONObject(file.readText(Charsets.UTF_8)), "") }.getOrNull() }
            .sortedByDescending(EditorArchive::timestamp)
    }

    fun load(id: String): EditorArchive? {
        val file = archiveFile(id)
        if (!file.exists()) return null
        val text = archiveTextFile(id).takeIf(File::exists)?.readText(Charsets.UTF_8).orEmpty()
        return runCatching { fromJson(JSONObject(file.readText(Charsets.UTF_8)), text) }.getOrNull()
    }

    fun delete(id: String) {
        val file = archiveFile(id)
        if (file.exists() && !file.delete()) throw IOException("删除存档失败")
        val text = archiveTextFile(id)
        if (text.exists() && !text.delete()) throw IOException("删除存档正文失败")
    }

    fun clear() {
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.extension in setOf("json", "txt", "tmp")) file.delete()
        }
    }

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
